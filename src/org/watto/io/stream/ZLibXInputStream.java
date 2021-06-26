/*
 * Application:  Game Extractor
 * Author:       wattostudios
 * Website:      http://www.watto.org
 * Copyright:    Copyright (c) 2002-2020 wattostudios
 *
 * License Information:
 * This program is free software; you can redistribute it and/or modify it under the terms of the GNU General Public License
 * published by the Free Software Foundation; either version 2 of the License, or (at your option) any later versions. This
 * program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranties
 * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License at http://www.gnu.org for more
 * details. For further information on this application, refer to the authors' website.
 */

package org.watto.io.stream;

import java.io.File;
import java.io.InputStream;
import org.watto.io.FileManipulator;
import org.watto.io.Manipulator;
import org.watto.io.buffer.ByteBuffer;
import org.watto.io.converter.ByteConverter;
import org.watto.io.converter.ShortConverter;
import org.watto.io.stream.datatype.ZLibXTree;

/***********************************************************************************************
ZLibX decompression
Ported from C-code at https://bitbucket.org/jibsen/tinf and modified to suit ZLibX format
***********************************************************************************************/
public class ZLibXInputStream extends InputStream {

  /** special ordering of code length codes **/
  short clcidx[] = {
      16, 17, 18, 0, 8, 7, 9, 6,
      10, 5, 11, 4, 12, 3, 13, 2,
      14, 1, 15
  };

  /** extra bits and base tables for length codes **/
  int[] length_bits = new int[288]; // unsigned char
  long[] length_base = new long[288]; // unsigned short

  /** extra bits and base tables for distance codes **/
  int[] dist_bits = new int[288]; // unsigned char
  long[] dist_base = new long[288]; // unsigned short

  /** fixed length/symbol tree **/
  ZLibXTree staticLengthTree = new ZLibXTree();
  /** fixed distance tree **/
  ZLibXTree staticDistanceTree = new ZLibXTree();

  /** dynamic length/symbol tree **/
  ZLibXTree dynamicLengthTree = new ZLibXTree();
  /** dynamic distance tree **/
  ZLibXTree dynamicDistanceTree = new ZLibXTree();

  /** the reading source **/
  Manipulator sourceManipulator;

  /** the buffer containing the uncompressed data **/
  Manipulator destManipulator;

  /** the current byte, which has been broken up in to bits **/
  int currentByte = 0;
  /** The number of bits in the currentByte **/
  int bitCount = 0;

  /** the length of the decompressed data **/
  long decompLength = 0;

  /** the marked location in the source **/
  int mark = 0;

  /***********************************************************************************************
  Creates an <code>InputStream</code> for the <code>file</code>
  @param file the <code>File</code> to read from
  ***********************************************************************************************/
  public ZLibXInputStream(File file, long decompLength) {
    this.sourceManipulator = new FileManipulator(file, false);
    //this.destManipulator = new FileManipulator(new ByteBuffer((int) decompLength)); // moved to available();
    this.destManipulator = new FileManipulator(new ByteBuffer(0));
    this.decompLength = decompLength;

    // Initialize the inflator
    tinf_init();

    // We now do this bit as part of available();
    /*
    while (destManipulator.getOffset() < decompLength) {
      // Skip the first 2 bytes, as per reference in http://aluigi.altervista.org/papers/quickbms.txt
      // These 2 bytes are the data length for this compressed block
      sourceManipulator.skip(2);
    
      // Uncompress the block
      tinf_uncompress();
    }
    
    // Now that we've uncompressed all the data, we need to return the buffer to the beginning,
    // ready for the read() by the extract() method when trying to export this file to disk
    destManipulator.seek(0);
    */
  }

  /***********************************************************************************************
  Creates an <code>InputStream</code> for the <code>Manipulator</code>
  @param manipulator the <code>Manipulator</code> to read from
  ***********************************************************************************************/
  public ZLibXInputStream(Manipulator manipulator, long decompLength) {
    this.sourceManipulator = manipulator;
    //this.destManipulator = new FileManipulator(new ByteBuffer((int) decompLength)); // moved to available();
    this.destManipulator = new FileManipulator(new ByteBuffer(0));
    this.decompLength = decompLength;

    // Initialize the inflator
    tinf_init();

    // We now do this bit as part of available();
    /*
    while (destManipulator.getOffset() < decompLength) {
      System.out.println(">>> STARTING. Read offset = " + sourceManipulator.getOffset());
      // Skip the first 2 bytes, as per reference in http://aluigi.altervista.org/papers/quickbms.txt
      // These 2 bytes are the data length for this compressed block
      int blockLength = ShortConverter.unsign(sourceManipulator.readShort());
      System.out.println(">>> Block length = " + blockLength + ", next block should start at " + (sourceManipulator.getOffset() + blockLength));
    
      // Uncompress the block
      tinf_uncompress();
    }
    
    System.out.println(">>> Done. Read offset = " + sourceManipulator.getOffset());
    
    // Now that we've uncompressed all the data, we need to return the buffer to the beginning,
    // ready for the read() by the extract() method when trying to export this file to disk
    destManipulator.seek(0);
    */
  }

  /***********************************************************************************************
  Gets the number of bytes left to read from the <code>Manipulator</code> source
  @return the remaining length
  ***********************************************************************************************/
  @Override
  public int available() {

    if ((int) destManipulator.getRemainingLength() <= 0) {
      if (decompLength > 0) {
        // we've run out of data in this block, need to read the next compressed block

        //System.out.println(">>> STARTING. Read offset = " + sourceManipulator.getOffset());

        // Skip the first 2 bytes, as per reference in http://aluigi.altervista.org/papers/quickbms.txt
        // These 2 bytes are the data length for this compressed block
        int blockLength = ShortConverter.unsign(sourceManipulator.readShort());
        long nextBlockOffset = sourceManipulator.getOffset() + blockLength;
        //System.out.println(">>> Block length = " + blockLength + ", next block should start at " + nextBlockOffset);

        destManipulator = new FileManipulator(new ByteBuffer(blockLength));

        // Uncompress the block
        boolean resetBitCounter = true;
        while (sourceManipulator.getOffset() < nextBlockOffset) {
          //System.out.println(">>> Decompressing at offset " + sourceManipulator.getOffset());
          int returnCode = tinf_uncompress(resetBitCounter);
          if (returnCode != 0) {
            // error somewhere - terminate the decompression here!
            decompLength = 0; // pretend the file has been fully decompressed
            return 0;
          }

          resetBitCounter = false; // so remaining iterations through the while() will continue from the existing bit, not reset it
        }

        // Now that we've uncompressed all the data, we need to return the buffer to the beginning,
        // ready for the read() by the extract() method when trying to export this file to disk
        destManipulator.seek(0);

        decompLength -= (blockLength + 2);
      }
    }

    return (int) destManipulator.getRemainingLength();
  }

  /***********************************************************************************************
  Closes the <code>Manipulator</code> source
  ***********************************************************************************************/
  @Override
  public void close() {
    destManipulator.close();
  }

  /***********************************************************************************************
  Gets the length of the <code>Manipulator</code> source
  @return the length of the source
  ***********************************************************************************************/
  public long getLength() {
    return destManipulator.getLength();
  }

  /***********************************************************************************************
  Gets the underlying <code>Manipulator</code> source
  @return the <code>Manipulator</code> source
  ***********************************************************************************************/
  public Manipulator getManipulator() {
    return destManipulator;
  }

  /***********************************************************************************************
  Gets the read offset in the <code>Manipulator</code> source
  @return the reading offset
  ***********************************************************************************************/
  public long getOffset() {
    return destManipulator.getOffset();
  }

  /***********************************************************************************************
  Marks the current offset in the <code>Manipulator</code> source
  @param readLimit the number of bytes to read before the mark becomes invalid
  ***********************************************************************************************/
  @Override
  public void mark(int readLimit) {
    try {
      mark = (int) destManipulator.getOffset();
    }
    catch (Throwable t) {
      mark = 0;
    }
  }

  /***********************************************************************************************
  Whether marking is supported by this <code>InputStream</code>
  @return true
  ***********************************************************************************************/
  @Override
  public boolean markSupported() {
    return true;
  }

  /***********************************************************************************************
  Reads a <code>byte</code> from the <code>Manipulator</code> source
  @return the <code>byte</code> value
  ***********************************************************************************************/
  @Override
  public int read() {
    return ByteConverter.unsign(destManipulator.readByte());
  }

  /***********************************************************************************************
  Reads a number of <code>byte</code>s from the <code>Manipulator</code> source
  @param byteArray the array to read the <code>byte</code>s in to
  @return the number of bytes that were read
  ***********************************************************************************************/
  @Override
  public int read(byte[] byteArray) {
    return destManipulator.getBuffer().read(byteArray);
  }

  /***********************************************************************************************
  Reads a number of <code>byte</code>s from the <code>Manipulator</code> source
  @param byteArray the array to read the <code>byte</code>s in to
  @param offset the offset in the <code>byteArray</code> to store the read values
  @param length the number of <code>byte</code>s to read
  @return the number of bytes that were actually read
  ***********************************************************************************************/
  @Override
  public int read(byte[] byteArray, int offset, int length) {
    return destManipulator.getBuffer().read(byteArray, offset, length);
  }

  /***********************************************************************************************
  Moves to the <code>mark</code>ed position in the <code>Manipulator</code> source, and resets the
  <code>mark</code>ed position
  ***********************************************************************************************/
  @Override
  public void reset() {
    try {
      destManipulator.seek(mark);
    }
    catch (Throwable t) {
    }

    mark = 0;
  }

  /***********************************************************************************************
  Moves to the <code>offset</code> in the <code>Manipulator</code> source
  @param offset the offset to seek to
  ***********************************************************************************************/
  public void seek(long offset) {
    destManipulator.seek(offset);
  }

  /***********************************************************************************************
  Skips over a number of bytes in the <code>Manipulator</code> source
  @param byteCount the number of bytes to skip
  @return the number of skipped bytes
  ***********************************************************************************************/
  @Override
  public long skip(long byteCount) {
    return destManipulator.skip(byteCount);
  }

  /***********************************************************************************************
  build extra bits and base tables
  bits = unsigned char
  base = unsigned short
  ***********************************************************************************************/
  public void tinf_build_bits_base(int[] bits, long[] base, int delta, int first) {
    // build bits table
    for (int i = 0; i < delta; ++i) {
      bits[i] = 0;
    }

    for (int i = 0; i < 288 - delta; ++i) {
      bits[i + delta] = (i / delta);
    }

    // build base table
    long sum = first;
    for (int i = 0; i < 288; ++i) {
      base[i] = sum;
      sum += 1l << bits[i];
    }
  }

  /***********************************************************************************************
  build the fixed huffman trees
  ***********************************************************************************************/
  public void tinf_build_fixed_trees(ZLibXTree lt, ZLibXTree dt) {
    // build fixed length tree
    for (int i = 0; i < 7; ++i) {
      lt.setTableValue(i, 0);
    }

    lt.setTableValue(7, 24);
    lt.setTableValue(8, 152);
    lt.setTableValue(9, 112);

    for (int i = 0; i < 24; ++i) {
      lt.setTransValue(i, 256 + i);
    }
    for (int i = 0; i < 144; ++i) {
      lt.setTransValue(24 + i, i);
    }
    for (int i = 0; i < 8; ++i) {
      lt.setTransValue(24 + 144 + i, 280 + i);
    }
    for (int i = 0; i < 112; ++i) {
      lt.setTransValue(24 + 144 + 8 + i, 144 + i);
    }

    // build fixed distance tree
    for (int i = 0; i < 5; ++i) {
      dt.setTableValue(i, 0);
    }

    dt.setTableValue(5, 32);

    for (int i = 0; i < 32; ++i) {
      dt.setTransValue(i, i);
    }
  }

  /***********************************************************************************************
  given an array of code lengths, build a tree
  lengths = const unsigned char
  num = unsigned int
  ***********************************************************************************************/
  public void tinf_build_tree(ZLibXTree t, int[] lengths, int startPos, int num) {

    // clear code length count table
    for (int i = 0; i < 16; ++i) {
      t.setTableValue(i, 0);
    }

    // scan symbol lengths, and sum code length counts
    for (int i = 0; i < num; ++i) {
      // t->table[lengths[i]]++
      int position = lengths[startPos + i];
      t.setTableValue(position, t.getTableValue(position) + 1);
    }

    t.setTableValue(0, 0);

    // compute offset table for distribution sort
    int[] offs = new int[16];
    for (int sum = 0, i = 0; i < 16; ++i) {
      offs[i] = sum;
      sum += t.getTableValue(i);
    }

    // create code->symbol translation table (symbols sorted by code)
    for (int i = 0; i < num; ++i) {
      if (lengths[startPos + i] != 0) {
        t.setTransValue(offs[lengths[startPos + i]]++, i);
      }
    }
  }

  /***********************************************************************************************
  given a data stream and a tree, decode a symbol
  ***********************************************************************************************/
  public int tinf_decode_symbol(ZLibXTree t) {
    int len = 0;
    int sum = 0;
    int cur = 0;

    // get more bits while code value is above sum
    do {
      cur = 2 * cur + tinf_getbit();

      ++len;

      sum += t.getTableValue(len);
      cur -= t.getTableValue(len);
    }
    while (cur >= 0);

    return t.getTransValue(sum + cur);
  }

  /***********************************************************************************************
  given a data stream, decode dynamic trees from it
  ***********************************************************************************************/
  public void tinf_decode_trees(ZLibXTree lt, ZLibXTree dt) {
    // get 5 bits HLIT (257-286)
    int hlit = tinf_read_bits(5, 257);

    // get 5 bits HDIST (1-32)
    int hdist = tinf_read_bits(5, 1);

    // get 4 bits HCLEN (4-19)
    int hclen = tinf_read_bits(4, 4);

    //System.out.println("    tinf_decode_trees: Decoding Tree: hlit = " + hlit + ", hdist = " + hdist + ", hclen = " + hclen);

    int[] lengths = new int[288 + 32]; // unsigned char
    for (int i = 0; i < 19; ++i) {
      lengths[i] = 0;
    }

    // read code lengths for code length alphabet
    for (int i = 0; i < hclen; ++i) {
      // get 3 bits code length (0-7)
      int clen = tinf_read_bits(3, 0); // unsigned int

      lengths[clcidx[i]] = clen;
    }

    // build code length tree
    //System.out.println("    tinf_decode_trees: Building Code Length Tree");
    ZLibXTree code_tree = new ZLibXTree();
    tinf_build_tree(code_tree, lengths, 0, 19);

    // decode code lengths for the dynamic trees
    for (int num = 0; num < hlit + hdist;) {
      int sym = tinf_decode_symbol(code_tree);

      if (sym == 16) {
        // copy previous code length 3-6 times (read 2 bits)
        int prev = lengths[num - 1]; // unsigned char
        for (int length = tinf_read_bits(2, 3); length != 0; --length) {
          lengths[num++] = prev;
        }
      }
      else if (sym == 17) {
        // repeat code length 0 for 3-10 times (read 3 bits)
        for (int length = tinf_read_bits(3, 3); length != 0; --length) {
          lengths[num++] = 0;
        }
      }
      else if (sym == 18) {
        // repeat code length 0 for 11-138 times (read 7 bits)
        for (int length = tinf_read_bits(7, 11); length != 0; --length) {
          lengths[num++] = 0;
        }
      }
      else {
        // values 0-15 represent the actual code lengths
        lengths[num++] = sym;
      }
    }

    /* build dynamic trees */
    //System.out.println("    tinf_decode_trees: Building Dynamic Tree");
    tinf_build_tree(lt, lengths, 0, hlit);  // from 0 to hlit
    tinf_build_tree(dt, lengths, hlit, hdist); // from hlit to hdist
  }

  /***********************************************************************************************
  Read a single bit from the source
  ***********************************************************************************************/
  public int tinf_getbit() {
    int bit;

    // check if tag is empty
    if (bitCount == 0) {
      // load next tag
      currentByte = ByteConverter.unsign(sourceManipulator.readByte());
      bitCount = 8;
    }

    // shift bit out of tag
    bit = (currentByte & 0x01);
    currentByte >>= 1;

    bitCount--;

    return bit;
  }

  /***********************************************************************************************
  given a stream and two trees, inflate a block of data
  ***********************************************************************************************/
  public int tinf_inflate_block_data(ZLibXTree lt, ZLibXTree dt) {
    while (true) {
      int sym = tinf_decode_symbol(lt);

      // check for end of block
      if (sym == 256) {
        //System.out.println("    tinf_inflate_block_data: End of Block");
        return 0; // break out of the loop
      }

      if (sym < 256) {
        //System.out.println("    tinf_inflate_block_data: Raw Symbol = " + sym);
        destManipulator.writeByte((byte) sym);//*d->dest++ = sym;
      }
      else {

        sym -= 257;

        // possibly get more bits from length code
        int length = tinf_read_bits(length_bits[sym], (int) length_base[sym]);

        int dist = tinf_decode_symbol(dt);

        // possibly get more bits from distance code
        int offs = tinf_read_bits(dist_bits[dist], (int) dist_base[dist]);

        //System.out.println("    tinf_inflate_block_data: Copy from previous: Length = " + length + ", Dist = " + dist + ", Offset = " + offs);

        // copy match
        for (int i = 0; i < length; ++i) {
          //d->dest[i] = d->dest[i - offs];
          long existingOffset = destManipulator.getOffset();

          // go to the earlier offset and read a byte
          destManipulator.seek(existingOffset - offs);
          byte byteValue = destManipulator.readByte();

          // go back to the current offset, and write the byte
          destManipulator.seek(existingOffset);
          destManipulator.writeByte(byteValue);
        }

        //d->dest += length;
      }
    }
  }

  /***********************************************************************************************
  inflate a block of data compressed with dynamic huffman trees
  ***********************************************************************************************/

  public int tinf_inflate_dynamic_block() {
    // decode trees from stream
    tinf_decode_trees(dynamicLengthTree, dynamicDistanceTree);

    // decode block using decoded trees
    return tinf_inflate_block_data(dynamicLengthTree, dynamicDistanceTree);
  }

  /***********************************************************************************************
  inflate a block of data compressed with fixed huffman trees
  ***********************************************************************************************/
  public int tinf_inflate_fixed_block() {
    // decode block using fixed trees
    return tinf_inflate_block_data(staticLengthTree, staticDistanceTree);
  }

  /***********************************************************************************************
  inflate an uncompressed block of data
  ***********************************************************************************************/
  public int tinf_inflate_uncompressed_block() {

    // get length
    int byte1 = ByteConverter.unsign(sourceManipulator.readByte());
    int byte2 = ByteConverter.unsign(sourceManipulator.readByte());

    int length = byte2;
    length = 256 * length + byte1;

    //System.out.println("    tinf_inflate_uncompressed_block: Length = " + length);

    // get the
    int byte3 = ByteConverter.unsign(sourceManipulator.readByte());
    int byte4 = ByteConverter.unsign(sourceManipulator.readByte());

    // get one's complement of length
    int invlength = byte4;
    invlength = 256 * invlength + byte3;

    //System.out.println("    tinf_inflate_uncompressed_block: Inverse Length = " + invlength);

    // check length
    if (length != (~invlength & 0x0000ffff)) {
      //System.out.println("        ERROR >> tinf_inflate_uncompressed_block: length and invlength don't match");
      return -3;
    }

    // copy block (do it individually, not en masse, so we can duplicate parts written by earlier iterations of this for loop)
    for (int i = length; i != 0; --i) {
      destManipulator.writeByte(sourceManipulator.readByte());
    }

    // make sure we start next block on a byte boundary
    bitCount = 0;

    return 0;
  }

  /***********************************************************************************************
  initialize global (static) data
  ***********************************************************************************************/
  public void tinf_init() {
    // build fixed huffman trees
    tinf_build_fixed_trees(staticLengthTree, staticDistanceTree);

    // build extra bits and base tables
    tinf_build_bits_base(length_bits, length_base, 4, 3);
    tinf_build_bits_base(dist_bits, dist_base, 2, 1);

    // fix a special case
    length_bits[28] = 0;
    length_base[28] = 258;
  }

  /***********************************************************************************************
  Reads a number of bits from the source, and returns the value. Adds the base value if provided
  ***********************************************************************************************/
  public int tinf_read_bits(int numBitsToRead, int base) {

    if (base < 0) {
      //System.out.println("      tinf_read_bits: Base is less than zero: " + base);
    }

    int valueOfBits = 0;

    // read num bits
    if (numBitsToRead != 0) {
      int limit = 1 << numBitsToRead;

      for (long mask = 1; mask < limit; mask *= 2) {
        if (tinf_getbit() != 0) {
          valueOfBits += mask;
        }
      }
    }

    return valueOfBits + base;
  }

  /***********************************************************************************************
  inflate stream from source to dest
  ***********************************************************************************************/
  public int tinf_uncompress() {
    return tinf_uncompress(true);
  }

  /***********************************************************************************************
  inflate stream from source to dest
  ***********************************************************************************************/
  @SuppressWarnings("unused")
  public int tinf_uncompress(boolean resetBitCounter) {
    if (resetBitCounter) {
      bitCount = 0;
    }

    // Read through the full Source and inflate (decompress) the data

    // ZLIBX differs from ZLIB in that ZLib continues looping over and over, whereas ZLIBX only reads the single
    // block before returning. Each block is preceeded by the length of the compressed block as a 2-byte short,
    // and if there is still more data to read, you read the next 2-byte header and run uncompress() again.
    /*
    int bFinal = 1;
    while (bFinal != 0) {
      // Read the Final Block flag (1 bit)
      bFinal = tinf_getbit();
      if (bFinal == 0) {
        return 0;
      }
      */
    int bFinal = tinf_getbit();

    // Read the Block Type flag (2 bits)
    int btype = tinf_read_bits(2, 0);

    // Decompress the block
    int result = -3;
    if (btype == 0) {
      // Uncompressed Block
      //System.out.println("tinf_uncompress: Read Uncompressed Block. Read offset: " + sourceManipulator.getOffset());
      result = tinf_inflate_uncompressed_block();
    }
    else if (btype == 1) {
      // Compressed Block with Fixed Huffman Trees
      //System.out.println("tinf_uncompress: Read Fixed Huffman Block");
      result = tinf_inflate_fixed_block();
    }
    else if (btype == 2) {
      // Compressed Block with Dynamic Huffman Trees
      //System.out.println("tinf_uncompress: Read Dynamic Huffman Block");
      result = tinf_inflate_dynamic_block();
    }

    if (result != 0) {
      //ErrorLogger.log("ZLibXInputStream: tinf_uncompress: failure somewhere");
      return -3;
    }
    /*
    }
    */

    return 0;
  }
}