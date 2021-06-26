/*
 * Implementation of LZX decoding,
 * a java port of LzxDecoder.cs from MonoGame 
 */

/* This file was derived from libmspack
 * (C) 2003-2004 Stuart Caie.
 * (C) 2011 Ali Scissons.
 *
 * The LZX method was created by Jonathan Forbes and Tomi Poutanen, adapted
 * by Microsoft Corporation.
 *
 * This source file is Dual licensed; meaning the end-user of this source file
 * may redistribute/modify it under the LGPL 2.1 or MS-PL licenses.
 */
// LGPL License
/* GNU LESSER GENERAL PUBLIC LICENSE version 2.1
 * LzxDecoder is free software; you can redistribute it and/or modify it under
 * the terms of the GNU Lesser General Public License (LGPL) version 2.1 
 */
// MS-PL License
/* 
 * MICROSOFT PUBLIC LICENSE
 * This source code is subject to the terms of the Microsoft Public License (Ms-PL). 
 *  
 * Redistribution and use in source and binary forms, with or without modification, 
 * is permitted provided that redistributions of the source code retain the above 
 * copyright notices and this file header. 
 *  
 * Additional copyright notices should be appended to the list above. 
 * 
 * For details, see <http://www.opensource.org/licenses/ms-pl.html>. 
 */
/*
 * This derived work is recognized by Stuart Caie and is authorized to adapt
 * any changes made to lzxd.c in his libmspack library and will still retain
 * this dual licensing scheme. Big thanks to Stuart Caie!
 * 
 * DETAILS
 * This file is a pure C# port of the lzxd.c file from libmspack, with minor
 * changes towards the decompression of XNB files. The original decompression
 * software of LZX encoded data was written by Suart Caie in his
 * libmspack/cabextract projects, which can be located at 
 * http://http://www.cabextract.org.uk/
 */

package com.github.antag99.aquarria.xnb;

import java.nio.ByteBuffer;
import java.util.Arrays;

public class LzxDecoder {

  private static final int MIN_MATCH = 2;

  // private static final int MAX_MATCH = 257;
  private static final int NUM_CHARS = 256;

  private static final int PRETREE_NUM_ELEMENTS = 20;

  private static final int ALIGNED_NUM_ELEMENTS = 8;

  private static final int NUM_PRIMARY_LENGTHS = 7;

  private static final int NUM_SECONDARY_LENGTHS = 249;

  private static final int PRETREE_MAXSYMBOLS = PRETREE_NUM_ELEMENTS;

  private static final int PRETREE_TABLEBITS = 6;

  private static final int MAINTREE_MAXSYMBOLS = NUM_CHARS + 50 * 8;

  private static final int MAINTREE_TABLEBITS = 12;

  private static final int LENGTH_MAXSYMBOLS = NUM_SECONDARY_LENGTHS + 1;

  private static final int LENGTH_TABLEBITS = 12;

  private static final int ALIGNED_MAXSYMBOLS = ALIGNED_NUM_ELEMENTS;

  private static final int ALIGNED_TABLEBITS = 7;

  private static final int[] positionBase;

  private static final int[] extraBits;

  static {
    extraBits = new int[52];
    for (int i = 0, j = 0; i <= 50; i += 2) {
      extraBits[i] = extraBits[i + 1] = (byte) j;
      if ((i != 0) && (j < 17))
        j++;
    }

    positionBase = new int[51];
    for (int i = 0, j = 0; i <= 50; i++) {
      positionBase[i] = j;
      j += 1 << extraBits[i];
    }
  }

  private enum LzxBlockType {
    Invalid, Verbatim, Aligned, Uncompressed;
  }

  /** LRU offset system */
  private int R0, R1, R2;

  /** Decoding window */
  private byte[] window;

  private int windowSize;

  private int windowPos;

  private int mainElementCount;

  /** Current block information */
  private LzxBlockType blockType;

  private int blockLength = 0;

  private int blockRemaining = 0;

  /** Intel CALL instruction optimization */
  private int intelFileSize;

  private int intelCurrentPosition;

  private boolean intelStarted;

  /** Decoding tables */
  private HuffTable preTree;

  private HuffTable mainTree;

  private HuffTable lengthTree;

  private HuffTable alignedTree;

  // private int actualSize = 0;
  private int framesRead = 0;

  /** Whether the file header should be read */
  private boolean readHeader = true;

  public LzxDecoder() {
    preTree = new HuffTable(PRETREE_MAXSYMBOLS, PRETREE_TABLEBITS);
    mainTree = new HuffTable(MAINTREE_MAXSYMBOLS, MAINTREE_TABLEBITS);
    lengthTree = new HuffTable(LENGTH_MAXSYMBOLS, LENGTH_TABLEBITS);
    alignedTree = new HuffTable(ALIGNED_MAXSYMBOLS, ALIGNED_TABLEBITS);
  }

  private void reset() {
    R0 = R1 = R2 = 1;
    readHeader = true;
    windowSize = 1 << 16;
    // actualSize = windowSize;
    window = new byte[windowSize];
    for (int i = 0; i < windowSize; i++)
      window[i] = (byte) 0xDC;
    windowPos = 0;
    mainElementCount = NUM_CHARS + (16 << 4);
    readHeader = true;
    framesRead = 0;
    blockRemaining = 0;
    blockType = LzxBlockType.Invalid;
    intelCurrentPosition = 0;
    intelStarted = false;

    preTree.reset();
    mainTree.reset();
    lengthTree.reset();
    alignedTree.reset();
  }

  public void decompress(ByteBuffer input, int inputLength, ByteBuffer output, int outputLength) {

    reset();

    int endPosition = input.position() + inputLength;
    // the size of the input (compressed data)
    int blockSize;
    // the size of the output (decompressed data)
    int frameSize;
    int pos = input.position();

    while (input.position() < endPosition) {

      // System.out.println("pos=" + pos);

      // seek to the correct position
      // input.rewind();
      input.position(pos);

      // System.out.println("input.position= " + input.position());

      int hi, lo;
      hi = input.get() & 0xFF;
      lo = input.get() & 0xFF;
      blockSize = (hi << 8) | lo;
      // all blocks by default will output 32Kb of data, so thus
      // is our frame size
      frameSize = 0x8000;
      // ... unless this block is special, that it outputs a different
      // amount of data. this blocks header is identified by a 0xFF byte
      if (hi == 0xFF) {
        // that means the lo byte was the hi byte
        hi = lo;
        lo = input.get() & 0xFF;
        // ... which combined to a different output/frame size for this
        // particular block
        frameSize = (hi << 8) | lo;
        // now get our block size
        hi = input.get() & 0xFF;
        lo = input.get() & 0xFF;
        blockSize = (hi << 8) | lo;
        pos += 5;
      }
      else {
        pos += 2;
      }

      // System.out.println("FrameSize=" + frameSize);
      // System.out.println("#BlockSize=" + blockSize);

      // either says there is nothing to decode
      if (blockSize == 0 || frameSize == 0) {
        // System.out.println("Done decompressing");
        break;
      }

      decompressBlock(input, blockSize, output, frameSize);
      pos += blockSize;
    }

  }

  private void decompressBlock(ByteBuffer input, int inputLength, ByteBuffer output, int outputLength) {

    int startpos = input.position();
    int endpos = startpos + inputLength;

    LzxBuffer buffer = new LzxBuffer(input);

    if (readHeader) {
      if (buffer.readBits(1) == 1) {
        // Intel optimization header
        int hi = buffer.readBits(16);
        int lo = buffer.readBits(16);
        intelFileSize = (hi << 16) | lo;

        // System.out.println("Intel file size: " + intelFileSize);
      }

      readHeader = false;
    }

    int window_posn = windowPos;
    int window_size = windowSize;
    int R0 = this.R0;
    int R1 = this.R1;
    int R2 = this.R2;

    int togo = outputLength;
    int this_run, main_element, match_length, match_offset, length_footer, extra, verbatim_bits;
    int rundest, runsrc, copy_length, aligned_bits;

    // System.out.println("window_posn=" + window_posn);
    // System.out.println("window_size=" + window_size);
    // System.out.println("R0=" + R0);
    // System.out.println("R1=" + R1);
    // System.out.println("R2=" + R2);

    while (togo > 0) {
      // System.out.println("Togo: " + togo);

      if (blockRemaining == 0) {
        // System.out.println("Current block type: " + blockType);
        if (blockType == LzxBlockType.Uncompressed) {

          // realign bitstream to word
          if ((blockLength & 1) == 1) {
            input.get();
          }
          buffer.reset();
        }

        int nextBlockType = buffer.readBits(3);
        if (nextBlockType > 3) {
          throw new RuntimeException("Invalid block type: " + nextBlockType);
        }

        blockType = LzxBlockType.values()[nextBlockType];

        // System.out.println("New block type: " + blockType);

        int a = buffer.readBits(16);
        int b = buffer.readBits(8);

        blockLength = (a << 8) | b;
        blockRemaining = blockLength;

        // System.out.println("Block length: " + blockLength);

        switch (blockType) {
          case Aligned:
            for (int i = 0, j = 0; i < 8; ++i) {
              j = buffer.readBits(3);
              alignedTree.getLength()[i] = (byte) j;
              // System.out.println("I= " + i + ", J=" + j);
            }
            alignedTree.makeDecodeTable();
            /*
             * Rest of aligned header is the same as verbatim,
             * fall through case.
             */
          case Verbatim:
            readLengths(mainTree.getLength(), 0, 256, buffer);
            readLengths(mainTree.getLength(), 256, mainElementCount, buffer);
            mainTree.makeDecodeTable();
            if (mainTree.getLength()[0xE8] != 0)
              intelStarted = true;

            readLengths(lengthTree.getLength(), 0, NUM_SECONDARY_LENGTHS, buffer);
            lengthTree.makeDecodeTable();
            break;
          case Uncompressed:
            intelStarted = true; /* because we can't assume otherwise */
            buffer.ensureBits(16); /* get up to 16 pad bits into the buffer */
            if (buffer.getRemainingBits() > 16) {
              input.position(input.position() - 2); /* and align the bitstream! */
            }
            byte hi,
                mh,
                ml,
                lo;
            lo = input.get();
            ml = input.get();
            mh = input.get();
            hi = input.get();
            R0 = (int) (lo | ml << 8 | mh << 16 | hi << 24);
            lo = input.get();
            ml = input.get();
            mh = input.get();
            hi = input.get();
            R1 = (int) (lo | ml << 8 | mh << 16 | hi << 24);
            lo = input.get();
            ml = input.get();
            mh = input.get();
            hi = input.get();
            R2 = (int) (lo | ml << 8 | mh << 16 | hi << 24);
            break;
          default:
            throw new RuntimeException("Unknown block type " + blockType);
        }
      }

      /* buffer exhaustion check */
      if (input.position() > (startpos + inputLength)) {
        /*
         * it's possible to have a file where the next run is less than
         * 16 bits in size. In this case, the READ_HUFFSYM() macro used
         * in building the tables will exhaust the buffer, so we should
         * allow for this, but not allow those accidentally read bits to
         * be used (so we check that there are at least 16 bits
         * remaining - in this boundary case they aren't really part of
         * the compressed data)
         */
        // System.out.println("WTF");

        if (input.position() > (startpos + inputLength + 2) || buffer.getRemainingBits() < 16)
          throw new RuntimeException();
      }

      while ((this_run = (int) blockRemaining) > 0 && togo > 0) {
        if (this_run > togo)
          this_run = togo;
        togo -= this_run;
        blockRemaining -= this_run;

        /* apply 2^x-1 mask */
        window_posn &= window_size - 1;

        // System.out.println("this_run= " + this_run);
        // System.out.println("togo= " + togo);
        // System.out.println("blockRemaining= " + blockRemaining);
        // System.out.println("window_posn= " + window_posn);
        // System.out.println("window_size= " + window_size);

        /* runs can't straddle the window wraparound */
        if ((window_posn + this_run) > window_size)
          throw new RuntimeException("(window_posn + this_run) > window_size");

        switch (blockType) {
          case Verbatim:
            while (this_run > 0) {
              main_element = mainTree.readHuffSym(buffer);
              // main_element = (int)ReadHuffSym(m_state.MAINTREE_table, m_state.MAINTREE_len,
              // LzxConstants.MAINTREE_MAXSYMBOLS, LzxConstants.MAINTREE_TABLEBITS,
              // bitbuf);
              if (main_element < NUM_CHARS) {
                /* literal: 0 to NUM_CHARS-1 */
                window[window_posn++] = (byte) main_element;
                this_run--;
              }
              else {
                /* match: NUM_CHARS + ((slot<<3) | length_header (3 bits)) */
                main_element -= NUM_CHARS;

                match_length = main_element & NUM_PRIMARY_LENGTHS;
                if (match_length == NUM_PRIMARY_LENGTHS) {
                  // length_footer = (int)ReadHuffSym(m_state.LENGTH_table, m_state.LENGTH_len,
                  // LzxConstants.LENGTH_MAXSYMBOLS, LzxConstants.LENGTH_TABLEBITS,
                  // bitbuf);
                  length_footer = lengthTree.readHuffSym(buffer);
                  match_length += length_footer;
                }
                match_length += MIN_MATCH;

                match_offset = main_element >> 3;

                if (match_offset > 2) {
                  /* not repeated offset */
                  if (match_offset != 3) {
                    extra = extraBits[match_offset];
                    verbatim_bits = (int) buffer.readBits(extra);
                    match_offset = (int) positionBase[match_offset] - 2 + verbatim_bits;
                  }
                  else {
                    match_offset = 1;
                  }

                  /* update repeated offset LRU queue */
                  R2 = R1;
                  R1 = R0;
                  R0 = match_offset;
                }
                else if (match_offset == 0) {
                  match_offset = (int) R0;
                }
                else if (match_offset == 1) {
                  match_offset = (int) R1;
                  R1 = R0;
                  R0 = match_offset;
                }
                else /* match_offset == 2 */
                {
                  match_offset = (int) R2;
                  R2 = R0;
                  R0 = match_offset;
                }

                rundest = (int) window_posn;
                this_run -= match_length;

                /* copy any wrapped around source data */
                if (window_posn >= match_offset) {
                  /* no wrap */
                  runsrc = rundest - match_offset;
                }
                else {
                  runsrc = rundest + ((int) window_size - match_offset);
                  copy_length = match_offset - (int) window_posn;
                  if (copy_length < match_length) {
                    match_length -= copy_length;
                    window_posn += copy_length;
                    while (copy_length-- > 0)
                      window[rundest++] = window[runsrc++];
                    runsrc = 0;
                  }
                }
                window_posn += match_length;

                /* copy match data - no worries about destination wraps */
                while (match_length-- > 0)
                  window[rundest++] = window[runsrc++];
              }
            }
            break;

          case Aligned:
            while (this_run > 0) {
              // main_element = (int)ReadHuffSym(m_state.MAINTREE_table, m_state.MAINTREE_len,
              // LzxConstants.MAINTREE_MAXSYMBOLS, LzxConstants.MAINTREE_TABLEBITS,
              // bitbuf);
              main_element = mainTree.readHuffSym(buffer);

              // System.err.println("main_element= " + main_element);
              // System.err.println("this_run= " + this_run);

              if (main_element < NUM_CHARS) {
                /* literal 0 to NUM_CHARS-1 */
                window[window_posn++] = (byte) main_element;
                this_run--;
              }
              else {
                /* match: NUM_CHARS + ((slot<<3) | length_header (3 bits)) */
                main_element -= NUM_CHARS;

                match_length = main_element & NUM_PRIMARY_LENGTHS;
                // System.err.println("match_length= " + match_length);
                if (match_length == NUM_PRIMARY_LENGTHS) {
                  // length_footer = (int)ReadHuffSym(m_state.LENGTH_table, m_state.LENGTH_len,
                  // LzxConstants.LENGTH_MAXSYMBOLS, LzxConstants.LENGTH_TABLEBITS,
                  // bitbuf);
                  length_footer = lengthTree.readHuffSym(buffer);

                  // System.err.println("length_footer= " + length_footer);

                  match_length += length_footer;
                }
                match_length += MIN_MATCH;

                match_offset = main_element >> 3;

                // System.err.println("match_offset= " + match_offset);

                if (match_offset > 2) {
                  /* not repeated offset */
                  extra = extraBits[match_offset];
                  match_offset = (int) positionBase[match_offset] - 2;
                  if (extra > 3) {
                    /* verbatim and aligned bits */
                    extra -= 3;
                    verbatim_bits = (int) buffer.readBits(extra);
                    match_offset += (verbatim_bits << 3);
                    // aligned_bits = (int)ReadHuffSym(m_state.ALIGNED_table, m_state.ALIGNED_len,
                    // LzxConstants.ALIGNED_MAXSYMBOLS, LzxConstants.ALIGNED_TABLEBITS,
                    // bitbuf);
                    aligned_bits = alignedTree.readHuffSym(buffer);
                    match_offset += aligned_bits;
                  }
                  else if (extra == 3) {
                    /* aligned bits only */
                    // aligned_bits = (int)ReadHuffSym(m_state.ALIGNED_table, m_state.ALIGNED_len,
                    // LzxConstants.ALIGNED_MAXSYMBOLS, LzxConstants.ALIGNED_TABLEBITS,
                    // bitbuf);
                    aligned_bits = alignedTree.readHuffSym(buffer);
                    match_offset += aligned_bits;
                  }
                  else if (extra > 0) /* extra==1, extra==2 */
                  {
                    /* verbatim bits only */
                    verbatim_bits = buffer.readBits(extra);
                    match_offset += verbatim_bits;
                  }
                  else /* extra == 0 */
                  {
                    /* ??? */
                    match_offset = 1;
                  }

                  /* update repeated offset LRU queue */
                  R2 = R1;
                  R1 = R0;
                  R0 = match_offset;
                }
                else if (match_offset == 0) {
                  match_offset = (int) R0;
                }
                else if (match_offset == 1) {
                  match_offset = (int) R1;
                  R1 = R0;
                  R0 = match_offset;
                }
                else /* match_offset == 2 */
                {
                  match_offset = (int) R2;
                  R2 = R0;
                  R0 = match_offset;
                }

                rundest = (int) window_posn;
                this_run -= match_length;

                /* copy any wrapped around source data */
                if (window_posn >= match_offset) {
                  /* no wrap */
                  runsrc = rundest - match_offset;
                }
                else {
                  runsrc = rundest + ((int) window_size - match_offset);
                  copy_length = match_offset - (int) window_posn;
                  if (copy_length < match_length) {
                    match_length -= copy_length;
                    window_posn += copy_length;
                    while (copy_length-- > 0) {
                      window[rundest++] = window[runsrc++];
                    }
                    runsrc = 0;
                  }
                }
                window_posn += match_length;

                /* copy match data - no worries about destination wraps */
                while (match_length-- > 0) {
                  window[rundest++] = window[runsrc++];
                }
              }
            }
            break;

          case Uncompressed:
            if ((input.position() + this_run) > endpos)
              throw new RuntimeException("(input.position() + this_run) > endpos");

            // byte[] temp_buffer = new byte[this_run];
            // inData.Read(temp_buffer, 0, this_run);
            // temp_buffer.CopyTo(window, (int)window_posn);

            // input.get(window, window_posn, window.length - window_posn);
            input.get(window, window_posn, this_run);
            window_posn += this_run;
            break;

          default:
            throw new RuntimeException("Invalid block type: " + blockType);
        }
      }
    }

    if (togo != 0)
      throw new RuntimeException("togo != 0");

    int start_window_pos = (int) window_posn;

    if (start_window_pos == 0) {
      start_window_pos = (int) window_size;
    }

    start_window_pos -= outputLength;

    // System.out.println("start_window_pos= " + start_window_pos);
    // System.out.println("outputLength= " + outputLength);
    // System.out.println("input.position= " + input.position());

    output.put(window, start_window_pos, outputLength);
    // outData.Write(window, start_window_pos, outLen);

    this.windowPos = window_posn;
    this.R0 = R0;
    this.R1 = R1;
    this.R2 = R2;

    // TODO finish intel E8 decoding
    /* intel E8 decoding */
    if ((framesRead++ < 32768) && intelFileSize != 0) {
      if (outputLength <= 6 || !intelStarted) {
        intelCurrentPosition += outputLength;
      }
      else {
        int dataend = outputLength - 10;
        int curpos = intelCurrentPosition;

        intelCurrentPosition = (int) curpos + outputLength;

        while (output.position() < dataend) {
          if (output.get() != 0xE8) {
            curpos++;
            continue;
          }
        }
      }
      // TODO: Is this an error?
      // return -1;
    }
    // return 0;
  }

  private void readLengths(byte[] lens, int first, int last, LzxBuffer buffer) {
    int x, y;
    int z;

    // hufftbl pointer here?

    for (x = 0; x < 20; x++) {
      y = buffer.readBits(4);
      preTree.getLength()[x] = (byte) y;
    }
    preTree.makeDecodeTable();
    // MakeDecodeTable(LzxConstants.PRETREE_MAXSYMBOLS, LzxConstants.PRETREE_TABLEBITS,
    // m_state.PRETREE_len, m_state.PRETREE_table);

    for (x = first; x < last;) {
      z = preTree.readHuffSym(buffer);
      if (z == 17) {
        y = buffer.readBits(4);
        y += 4;
        while (y-- != 0)
          lens[x++] = 0;
      }
      else if (z == 18) {
        y = buffer.readBits(5);
        y += 20;
        while (y-- != 0)
          lens[x++] = 0;
      }
      else if (z == 19) {
        y = buffer.readBits(1);
        y += 4;
        z = preTree.readHuffSym(buffer);
        z = lens[x] - z;
        if (z < 0)
          z += 17;
        while (y-- != 0)
          lens[x++] = (byte) z;
      }
      else {
        z = lens[x] - z;
        if (z < 0)
          z += 17;
        lens[x++] = (byte) z;
      }
    }
  }
}


class HuffTable {

  private short[] table;

  private byte[] length;

  private int maxSymbols;

  private int tableBits;

  private static final int SAFETY = 64;

  public HuffTable(int maxSymbols, int tableBits) {
    this.maxSymbols = maxSymbols;
    this.tableBits = tableBits;
    table = new short[(1 << tableBits) + (maxSymbols << 1)];
    length = new byte[maxSymbols + SAFETY];
  }

  public int getMaxSymbols() {
    return maxSymbols;
  }

  public void makeDecodeTable() {
    short sym;
    int leaf;
    byte bit_num = 1;
    int fill;
    int pos = 0; /* the current position in the decode table */
    int table_mask = (1 << (int) tableBits);
    int bit_mask = table_mask >>> 1; /* don't do 0 length codes */
    int next_symbol = bit_mask; /* base of allocation for long codes */

    /* fill entries for codes short enough for a direct mapping */
    while (bit_num <= tableBits) {
      for (sym = 0; sym < maxSymbols; sym++) {
        if (length[sym] == bit_num) {
          leaf = pos;

          if ((pos += bit_mask) > table_mask)
            return;// 1; /* table overrun */

          /* fill all possible lookups of this symbol with the symbol itself */
          fill = bit_mask;
          while (fill-- > 0)
            table[leaf++] = sym;
        }
      }
      bit_mask >>>= 1;
      bit_num++;
    }

    /* if there are any codes longer than tableBits */
    if (pos != table_mask) {
      /* clear the remainder of the table */
      for (sym = (short) pos; sym < table_mask; sym++)
        table[sym] = 0;

      /* give ourselves room for codes to grow by up to 16 more bits */
      pos <<= 16;
      table_mask <<= 16;
      bit_mask = 1 << 15;

      while (bit_num <= 16) {
        for (sym = 0; sym < maxSymbols; sym++) {
          if (length[sym] == bit_num) {
            leaf = pos >>> 16;
            for (fill = 0; fill < bit_num - tableBits; fill++) {
              /* if this path hasn't been taken yet, 'allocate' two entries */
              if (table[leaf] == 0) {
                table[(next_symbol << 1)] = 0;
                table[(next_symbol << 1) + 1] = 0;
                table[leaf] = (short) (next_symbol++);
              }
              /* follow the path and select either left or right for next bit */
              leaf = (table[leaf] << 1);
              if (((pos >>> (int) (15 - fill)) & 1) == 1)
                leaf++;
            }
            table[leaf] = sym;

            if ((pos += bit_mask) > table_mask)
              return;// 1;
          }
        }
        bit_mask >>>= 1;
        bit_num++;
      }
    }

    /* full talbe? */
    if (pos == table_mask)
      return;// 0;

    /* either erroneous table, or all elements are 0 - let's find out. */
    for (sym = 0; sym < maxSymbols; sym++)
      if (length[sym] != 0)
        return;// 1;
  }

  public int readHuffSym(LzxBuffer buffer) {
    int i, j;
    buffer.ensureBits(16);
    if ((i = table[buffer.peekBits((byte) tableBits)]) >= maxSymbols) {
      j = (int) (1 << (int) ((4 * 8) - tableBits));
      do {
        j >>>= 1;
        i <<= 1;
        i |= (buffer.getBitBuffer() & j) != 0 ? (int) 1 : 0;

        if (j == 0)
          throw new RuntimeException(); // return 0;

      }
      while ((i = table[i]) >= maxSymbols);
    }
    j = length[i];
    buffer.removeBits((byte) j);

    return i;
  }

  public void reset() {
    Arrays.fill(table, (short) 0);
    Arrays.fill(length, (byte) 0);
  }

  public short[] getTable() {
    return table;
  }

  public byte[] getLength() {
    return length;
  }
}


class LzxBuffer {

  private ByteBuffer byteBuffer;

  private int remainingBits = 0;

  private int bitBuffer = 0;

  public LzxBuffer(ByteBuffer buffer) {
    this.byteBuffer = buffer;
  }

  public void reset() {
    remainingBits = 0;
    bitBuffer = 0;
  }

  public void ensureBits(int bitCount) {
    if (bitCount < 0 || bitCount > 32) {
      throw new IllegalArgumentException(Integer.toString(bitCount));
    }

    while (remainingBits < bitCount) {
      int lo = byteBuffer.get() & 0xff;
      int hi = byteBuffer.get() & 0xff;
      bitBuffer |= ((hi << 8) | lo) << (4 * 8 - 16 - remainingBits);
      remainingBits += 16;
    }
  }

  public int peekBits(int bitCount) {
    if (bitCount > remainingBits) {
      throw new IllegalArgumentException("Not enough bits: required "
          + bitCount + " has " + remainingBits);
    }

    return (bitBuffer >>> (32 - bitCount));
  }

  public void removeBits(int bitCount) {
    bitBuffer <<= bitCount;
    remainingBits -= bitCount;
  }

  public int readBits(int bitCount) {
    int result = 0;

    if (bitCount > 0) {
      ensureBits(bitCount);
      result = peekBits(bitCount);
      removeBits(bitCount);
    }

    return result;
  }

  public int getBitBuffer() {
    return bitBuffer;
  }

  public int getRemainingBits() {
    return remainingBits;
  }
}