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

package org.watto.ge.plugin.exporter;

import org.watto.datatype.Resource;
import org.watto.ge.plugin.ExporterPlugin;
import org.watto.io.FileManipulator;
import org.watto.io.converter.ByteConverter;

/**
**********************************************************************************************
LZH (LZHUF) Decompression
==
==
== UNTESTED / UNVERIFIED ==
==
==
**********************************************************************************************
**/
public class Exporter_LZH extends ExporterPlugin {

  //static Exporter_LZH instance = new Exporter_LZH();

  byte[] buffer = null;

  int bufferLength = 0;

  int bufferPos = 0;

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  //public static Exporter_LZH getInstance() {
  //  return instance;
  //}

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public Exporter_LZH() {
    setName("LZH (LZHUF) Decompression");
  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  @Override
  public boolean available() {
    return bufferPos < bufferLength;
  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  @Override
  public void close() {
    buffer = null;
  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  @Override
  public void open(Resource source) {
    bufferPos = 0;
    bufferLength = 0;

    try {
      int compLength = (int) source.getLength();
      int decompLength = (int) source.getDecompressedLength();

      FileManipulator fm = new FileManipulator(source.getSource(), false);
      fm.seek(source.getOffset());

      byte[] compBuffer = fm.readBytes(compLength);
      buffer = new byte[decompLength];

      unlzh(compBuffer, compLength, buffer, decompLength);

      fm.close();

      bufferLength = decompLength;

    }
    catch (Throwable t) {
    }
  }

  /**
  **********************************************************************************************
  NOT DONE
  **********************************************************************************************
  **/
  @Override
  public void pack(Resource source, FileManipulator destination) {
    try {

      ExporterPlugin exporter = source.getExporter();
      exporter.open(source);

      while (exporter.available()) {
        destination.writeByte(exporter.read());
      }

      exporter.close();

    }
    catch (Throwable t) {
      logError(t);
    }
  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  @Override
  public int read() {
    try {
      return buffer[bufferPos++];
    }
    catch (Throwable t) {
      bufferPos = bufferLength; // early exit
      return 0;
    }
  }

  int[] d_code = new int[] {
      0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
      0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
      0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
      0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
      0x01, 0x01, 0x01, 0x01, 0x01, 0x01, 0x01, 0x01,
      0x01, 0x01, 0x01, 0x01, 0x01, 0x01, 0x01, 0x01,
      0x02, 0x02, 0x02, 0x02, 0x02, 0x02, 0x02, 0x02,
      0x02, 0x02, 0x02, 0x02, 0x02, 0x02, 0x02, 0x02,
      0x03, 0x03, 0x03, 0x03, 0x03, 0x03, 0x03, 0x03,
      0x03, 0x03, 0x03, 0x03, 0x03, 0x03, 0x03, 0x03,
      0x04, 0x04, 0x04, 0x04, 0x04, 0x04, 0x04, 0x04,
      0x05, 0x05, 0x05, 0x05, 0x05, 0x05, 0x05, 0x05,
      0x06, 0x06, 0x06, 0x06, 0x06, 0x06, 0x06, 0x06,
      0x07, 0x07, 0x07, 0x07, 0x07, 0x07, 0x07, 0x07,
      0x08, 0x08, 0x08, 0x08, 0x08, 0x08, 0x08, 0x08,
      0x09, 0x09, 0x09, 0x09, 0x09, 0x09, 0x09, 0x09,
      0x0A, 0x0A, 0x0A, 0x0A, 0x0A, 0x0A, 0x0A, 0x0A,
      0x0B, 0x0B, 0x0B, 0x0B, 0x0B, 0x0B, 0x0B, 0x0B,
      0x0C, 0x0C, 0x0C, 0x0C, 0x0D, 0x0D, 0x0D, 0x0D,
      0x0E, 0x0E, 0x0E, 0x0E, 0x0F, 0x0F, 0x0F, 0x0F,
      0x10, 0x10, 0x10, 0x10, 0x11, 0x11, 0x11, 0x11,
      0x12, 0x12, 0x12, 0x12, 0x13, 0x13, 0x13, 0x13,
      0x14, 0x14, 0x14, 0x14, 0x15, 0x15, 0x15, 0x15,
      0x16, 0x16, 0x16, 0x16, 0x17, 0x17, 0x17, 0x17,
      0x18, 0x18, 0x19, 0x19, 0x1A, 0x1A, 0x1B, 0x1B,
      0x1C, 0x1C, 0x1D, 0x1D, 0x1E, 0x1E, 0x1F, 0x1F,
      0x20, 0x20, 0x21, 0x21, 0x22, 0x22, 0x23, 0x23,
      0x24, 0x24, 0x25, 0x25, 0x26, 0x26, 0x27, 0x27,
      0x28, 0x28, 0x29, 0x29, 0x2A, 0x2A, 0x2B, 0x2B,
      0x2C, 0x2C, 0x2D, 0x2D, 0x2E, 0x2E, 0x2F, 0x2F,
      0x30, 0x31, 0x32, 0x33, 0x34, 0x35, 0x36, 0x37,
      0x38, 0x39, 0x3A, 0x3B, 0x3C, 0x3D, 0x3E, 0x3F,
  };

  int[] d_len = new int[] {
      0x03, 0x03, 0x03, 0x03, 0x03, 0x03, 0x03, 0x03,
      0x03, 0x03, 0x03, 0x03, 0x03, 0x03, 0x03, 0x03,
      0x03, 0x03, 0x03, 0x03, 0x03, 0x03, 0x03, 0x03,
      0x03, 0x03, 0x03, 0x03, 0x03, 0x03, 0x03, 0x03,
      0x04, 0x04, 0x04, 0x04, 0x04, 0x04, 0x04, 0x04,
      0x04, 0x04, 0x04, 0x04, 0x04, 0x04, 0x04, 0x04,
      0x04, 0x04, 0x04, 0x04, 0x04, 0x04, 0x04, 0x04,
      0x04, 0x04, 0x04, 0x04, 0x04, 0x04, 0x04, 0x04,
      0x04, 0x04, 0x04, 0x04, 0x04, 0x04, 0x04, 0x04,
      0x04, 0x04, 0x04, 0x04, 0x04, 0x04, 0x04, 0x04,
      0x05, 0x05, 0x05, 0x05, 0x05, 0x05, 0x05, 0x05,
      0x05, 0x05, 0x05, 0x05, 0x05, 0x05, 0x05, 0x05,
      0x05, 0x05, 0x05, 0x05, 0x05, 0x05, 0x05, 0x05,
      0x05, 0x05, 0x05, 0x05, 0x05, 0x05, 0x05, 0x05,
      0x05, 0x05, 0x05, 0x05, 0x05, 0x05, 0x05, 0x05,
      0x05, 0x05, 0x05, 0x05, 0x05, 0x05, 0x05, 0x05,
      0x05, 0x05, 0x05, 0x05, 0x05, 0x05, 0x05, 0x05,
      0x05, 0x05, 0x05, 0x05, 0x05, 0x05, 0x05, 0x05,
      0x06, 0x06, 0x06, 0x06, 0x06, 0x06, 0x06, 0x06,
      0x06, 0x06, 0x06, 0x06, 0x06, 0x06, 0x06, 0x06,
      0x06, 0x06, 0x06, 0x06, 0x06, 0x06, 0x06, 0x06,
      0x06, 0x06, 0x06, 0x06, 0x06, 0x06, 0x06, 0x06,
      0x06, 0x06, 0x06, 0x06, 0x06, 0x06, 0x06, 0x06,
      0x06, 0x06, 0x06, 0x06, 0x06, 0x06, 0x06, 0x06,
      0x07, 0x07, 0x07, 0x07, 0x07, 0x07, 0x07, 0x07,
      0x07, 0x07, 0x07, 0x07, 0x07, 0x07, 0x07, 0x07,
      0x07, 0x07, 0x07, 0x07, 0x07, 0x07, 0x07, 0x07,
      0x07, 0x07, 0x07, 0x07, 0x07, 0x07, 0x07, 0x07,
      0x07, 0x07, 0x07, 0x07, 0x07, 0x07, 0x07, 0x07,
      0x07, 0x07, 0x07, 0x07, 0x07, 0x07, 0x07, 0x07,
      0x08, 0x08, 0x08, 0x08, 0x08, 0x08, 0x08, 0x08,
      0x08, 0x08, 0x08, 0x08, 0x08, 0x08, 0x08, 0x08,
  };

  byte[] infile = null;

  int infilel = 0;

  byte[] outfile = null;

  int outfilel = 0;

  int inPos = 0;

  int outPos = 0;

  int xgetc() {
    //if(infile >= infilel) {
    if (inPos >= infilel) {
      return (-1);
    }
    return (ByteConverter.unsign(infile[inPos++]));
  }

  int xputc(int chr) {
    //if(outfile >= outfilel) {
    if (outPos >= outfilel) {
      return (-1);
    }
    outfile[outPos++] = (byte) chr;
    return (chr);
  }

  long textsize = 0;

  /********** LZSS compression **********/

  int N = 4096; /* buffer size */

  int F = 60; /* lookahead buffer size */

  int THRESHOLD = 2;

  int NIL = N; /* leaf of tree */

  int[] text_buf = new int[N + F - 1];

  /* Huffman coding */

  int N_CHAR = (256 - THRESHOLD + F);

  /* kinds of characters (character code = 0..N_CHAR-1) */
  int T = (N_CHAR * 2 - 1); /* size of table */

  int R = (T - 1); /* position of root */

  int MAX_FREQ = 0x8000; /* updates tree when the */

  int[] freq = new int[T + 1]; /* frequency table */

  int[] prnt = new int[T + N_CHAR]; /* pointers to parent nodes, except for the */
  /* elements [T..T + N_CHAR - 1] which are used to get */
  /* the positions of leaves corresponding to the codes. */

  int[] son = new int[T]; /* pointers to child nodes (son[], son[] + 1) */

  int getbuf = 0;

  int getlen = 0;

  int putbuf = 0;

  int putlen = 0;

  int GetBit() /* get one bit */
  {
    int i;

    while (getlen <= 8) {
      if ((int) (i = xgetc()) < 0) {//if ((int)(i = xgetc(infile)) < 0) {
        return (0); //i = 0;
      }
      getbuf |= i << (8 - getlen);
      getlen += 8;
    }
    i = getbuf;
    getbuf <<= 1;
    getlen--;
    return (int) ((i & 0x8000) >> 15);
  }

  int GetByte() /* get one byte */
  {
    int i;

    while (getlen <= 8) {
      if ((int) (i = xgetc()) < 0) {//if ((int)(i = xgetc(infile)) < 0) {
        return (0); //i = 0;
      }
      getbuf |= i << (8 - getlen);
      getlen += 8;
    }
    i = getbuf;
    getbuf <<= 8;
    getlen -= 8;
    return (int) ((i & 0xff00) >> 8);
  }

  /* initialization of tree */

  void StartHuff() {
    int i, j;

    for (i = 0; i < N_CHAR; i++) {
      freq[i] = 1;
      son[i] = i + T;
      prnt[i + T] = i;
    }
    i = 0;
    j = N_CHAR;
    while (j <= R) {
      freq[j] = freq[i] + freq[i + 1];
      son[j] = i;
      prnt[i] = prnt[i + 1] = j;
      i += 2;
      j++;
    }
    freq[T] = 0xffff;
    prnt[R] = 0;
  }

  /* reconstruction of tree */

  void reconst() {
    int i, j, k;
    int f, l;

    /* collect leaf nodes in the first half of the table */
    /* and replace the freq by (freq + 1) / 2. */
    j = 0;
    for (i = 0; i < T; i++) {
      if (son[i] >= T) {
        freq[j] = (freq[i] + 1) / 2;
        son[j] = son[i];
        j++;
      }
    }
    /* begin constructing tree by connecting sons */
    for (i = 0, j = N_CHAR; j < T; i += 2, j++) {
      k = i + 1;
      f = freq[j] = freq[i] + freq[k];
      for (k = j - 1; f < freq[k]; k--)
        ;
      k++;
      l = (j - k) * 2;

      //memmove(&freq[k + 1], &freq[k], l);
      for (int a = 0; a < l; a++) {
        freq[k + 1 + a] = freq[k + a];
      }

      freq[k] = f;

      //memmove(&son[k + 1], &son[k], l);
      for (int a = 0; a < l; a++) {
        son[k + 1 + a] = son[k + a];
      }

      son[k] = i;
    }
    /* connect prnt */
    for (i = 0; i < T; i++) {
      if ((k = son[i]) >= T) {
        prnt[k] = i;
      }
      else {
        prnt[k] = prnt[k + 1] = i;
      }
    }
  }

  /* increment frequency of given code by one, and update tree */

  void update(int c) {
    int i, j, k, l;

    if (freq[R] == MAX_FREQ) {
      reconst();
    }
    c = prnt[c + T];
    do {
      k = ++freq[c];

      /* if the order is disturbed, exchange nodes */
      if ((int) k > freq[l = c + 1]) {
        while ((int) k > freq[++l])
          ;
        l--;
        freq[c] = freq[l];
        freq[l] = k;

        i = son[c];
        prnt[i] = l;
        if (i < T)
          prnt[i + 1] = l;

        j = son[l];
        son[l] = i;

        prnt[j] = c;
        if (j < T)
          prnt[j + 1] = c;
        son[c] = j;

        c = l;
      }
    }
    while ((c = prnt[c]) != 0); /* repeat up to root */
  }

  int code;

  int len;

  int DecodeChar() {
    int c;

    c = son[R];

    /* travel from root to leaf, */
    /* choosing the smaller child node (son[]) if the read bit is 0, */
    /* the bigger (son[]+1} if 1 */
    while (c < T) {
      c += GetBit();
      c = son[c];
    }
    c -= T;
    update(c);
    return (int) c;
  }

  int DecodePosition() {
    int i, j, c;

    /* recover upper 6 bits from table */
    i = GetByte();
    c = (int) d_code[i] << 6;
    j = d_len[i];

    /* read lower 6 bits verbatim */
    j -= 2;
    while (j-- > 0) {
      i = (i << 1) + GetBit();
    }
    return (int) (c | (i & 0x3f));
  }

  int unlzh(byte[] in, int insz, byte[] out, int outsz) {
    int i, j, k, r, c;
    long count;

    infile = in;
    infilel = insz;
    outfile = out;
    outfilel = outsz;

    inPos = 0;
    outPos = 0;

    /*textsize = (xgetc(infile));
    textsize |= (xgetc(infile) << 8);
    textsize |= (xgetc(infile) << 16);
    textsize |= (xgetc(infile) << 24);
    if (textsize == 0)
        return(-1);*/
    textsize = outsz;

    StartHuff();
    for (i = 0; i < N - F; i++)
      text_buf[i] = 0x20;
    r = N - F;
    for (count = 0; count < textsize;) {
      c = DecodeChar();
      if (c < 256) {
        if (xputc(c) == -1) {//if (xputc(c, outfile) == -1) {
          return (-1);
        }
        text_buf[r++] = (int) c;
        r &= (N - 1);
        count++;
      }
      else {
        i = (r - DecodePosition() - 1) & (N - 1);
        j = c - 255 + THRESHOLD;
        for (k = 0; k < j; k++) {
          c = text_buf[(i + k) & (N - 1)];
          if (xputc(c) == -1) {//if (xputc(c, outfile) == -1) {
            return (-1);
          }
          text_buf[r++] = (int) c;
          r &= (N - 1);
          count++;
        }
      }
    }
    return (outPos);
  }

}