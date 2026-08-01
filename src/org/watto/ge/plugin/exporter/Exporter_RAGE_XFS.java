/*
 * Application:  Game Extractor
 * Author:       wattostudios
 * Website:      http://www.watto.org
 * Copyright:    Copyright (c) 2002-2026 wattostudios
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
import org.watto.io.converter.ShortConverter;

public class Exporter_RAGE_XFS extends ExporterPlugin {

  static Exporter_RAGE_XFS instance = new Exporter_RAGE_XFS();

  byte[] decompBuffer = null;

  int decompPos = 0;

  int decompLength = 0;

  /**
  **********************************************************************************************
  Ref: https://github.com/LittleBigBug/QuickBMS/blob/5315ffe664b88dc09ae783ad17d9dfd252b1c927/src/compression/rage_xfs.c#L158
  **********************************************************************************************
  **/
  public static Exporter_RAGE_XFS getInstance() {
    return instance;
  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public Exporter_RAGE_XFS() {
    setName("RAGE XFS Compression");
  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  @Override
  public boolean available() {
    if (decompPos < decompLength) {
      return true;
    }
    else {
      return false;
    }
  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  @Override
  public void close() {
    decompBuffer = null;
    decompPos = 0;
    decompLength = 0;
  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  @Override
  public String getDescription() {
    return "This exporter decompresses RAGE XFS files when exporting\n\n" + super.getDescription();
  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  @Override
  public void open(Resource source) {
    try {
      FileManipulator fm = new FileManipulator(source.getSource(), false);
      fm.seek(source.getOffset());

      decompPos = 0;
      decompLength = (int) source.getDecompressedLength();

      decompBuffer = new byte[(int) decompLength];

      int compLength = (int) source.getLength();
      byte[] compBuffer = fm.readBytes(compLength);

      rage_xfs_decompress(compBuffer, compLength, decompBuffer, decompLength);
      decompPos = 0;

      fm.close();
    }
    catch (Throwable t) {
    }
  }

  /**
  **********************************************************************************************
  NOT IMPLEMENTED
  **********************************************************************************************
  **/
  @Override
  public void pack(Resource source, FileManipulator destination) {

  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  @Override
  public int read() {
    try {
      byte currentByte = decompBuffer[decompPos];
      decompPos++;
      return currentByte;
    }
    catch (Throwable t) {
      return 0;
    }
  }

  int bitsLeft = 0;
  int bitBuffer = 0; // byte
  byte[] inb;
  byte[] outb;
  int inbPos = 0;
  int outbPos = 0;
  int inl;
  int outl;
  int[] countTable1 = new int[512];
  int[] countTable2 = new int[512];
  int[] lengthTable1 = new int[512];
  int[] lengthTable2 = new int[512];
  int[] offsetTable1 = new int[32];
  int[] offsetTable2 = new int[32];

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  int getBit() {
    if (bitsLeft == 0) {
      if (inbPos >= inl) {
        return 0;
      }
      bitBuffer = ByteConverter.unsign(inb[inbPos]);
      inbPos++;
      bitsLeft = 8;
    }
    int t = (bitBuffer & 0x80) >> 7;
    bitBuffer <<= 1;
    bitsLeft--;
    return t;
  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  int loadBits(int count) {
    int value = 0;
    while (count > 0) {
      value = (value << 1) | getBit();
      count--;
    }
    return value;
  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  void loadTable(int[] table1, int[] table2) {
    if (inbPos >= inl) {
      return;
    }

    int table1Pos = 0;
    int table2Pos = 0;

    int count = ByteConverter.unsign(inb[inbPos]) * 2;
    inbPos++;

    int len1 = loadBits(3) + 1;
    int len2 = loadBits(3) + 1;
    while (count > 0) {
      int value;
      if (getBit() == 0) {
        value = loadBits(len1);
      }
      else {
        value = loadBits(len2);
        table2[table2Pos] = value;
        value = 0;
      }
      table1[table1Pos] = value;
      table1Pos++;
      table2Pos++;
      count--;
    }
  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  int loadValue(int[] table1, int[] table2) {
    int index = 0;
    int value = 0;
    do {
      index = (value << 1) | getBit();
      value = table1[index];
    }
    while (value != 0);
    return table2[index];
  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  int loadCount() {
    return loadValue(countTable1, countTable2);
  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  int loadLength() {
    int length = loadValue(lengthTable1, lengthTable2);
    if (length == 255) {
      int len_hi = (getBit() << 1) | getBit();

      if (inbPos >= inl) {
        return 0;
      }

      int len_lo = ByteConverter.unsign(inb[inbPos]);
      inbPos++;
      length = ((len_hi << 8) | len_lo) + 255;
    }
    return length;
  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  int loadOffset() {
    int offset = loadValue(offsetTable1, offsetTable2);
    if (offset >= 2) {
      int bitcount = offset - 1;
      offset = 1;
      while (bitcount > 0) {
        offset = (offset << 1) | getBit();
        bitcount--;
      }
    }
    return ~offset;
  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  void decompressData() {

    bitsLeft = 0;
    bitBuffer = 0;

    /* initialize bitbuffer */
    getBit();

    do {
      if ((inbPos + 2) > inl)
        break;
      int blocksize = ShortConverter.convertLittle(new byte[] { inb[inbPos], inb[inbPos + 1] });
      inbPos += 2;

      /* load tables */
      loadTable(lengthTable1, lengthTable2);
      loadTable(countTable1, countTable2);
      loadTable(offsetTable1, offsetTable2);

      /* process the block */
      while (blocksize > 0) {
        int copyCount = loadCount();
        while (copyCount-- > 0) {
          if (inbPos >= inl) {
            break;
          }
          if (outbPos >= outl) {
            break;
          }

          outb[outbPos] = inb[inbPos];
          inbPos++;
          outbPos++;
        }
        int length = loadLength();
        if (length > 0) {
          int offset = loadOffset();
          /* don't use memcpy here because source and dest can overlap */
          while (length > 0) {
            if (outbPos >= outl) {
              break;
            }
            outb[outbPos] = outb[outbPos + offset];
            outbPos++;
            length--;
          }
        }
        blocksize--;
      }

    }
    while (getBit() > 0);

  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  boolean isRA(byte[] inBuffer, int inSize) {
    if (inSize > 12 && inBuffer[0] == 82 && inBuffer[1] == 65 && inBuffer[2] == 0 && inBuffer[3] == 2) {
      return true;
    }
    return false;
  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  int rage_xfs_decompress(byte[] inbuffer, int insize, byte[] outbuffer, int outsize) {

    inl = insize;
    outl = outsize;

    inb = inbuffer;

    /* set input to the beginning of the compressed data */
    if (isRA(inbuffer, insize)) {
      inbPos = 12; // magic, outsize, ???
    }
    else {
      inbPos = 0; // raw data
    }

    outb = outbuffer;
    outbPos = 0;

    decompressData();

    /* return the decompressed size */
    return outbPos;
  }

}