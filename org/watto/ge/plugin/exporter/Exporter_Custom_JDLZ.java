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

public class Exporter_Custom_JDLZ extends ExporterPlugin {

  static Exporter_Custom_JDLZ instance = new Exporter_Custom_JDLZ();

  static FileManipulator readSource;
  static long readLength = 0;

  /**
  **********************************************************************************************

  **********************************************************************************************
  **/
  public static Exporter_Custom_JDLZ getInstance() {
    return instance;
  }

  byte[] outBuffer = new byte[0];
  int outBufferPos = 0;

  int outBufferLength = 0;

  /**
  **********************************************************************************************

  **********************************************************************************************
  **/
  public Exporter_Custom_JDLZ() {
    setName("EA JDLZ Compression");
  }

  /**
  **********************************************************************************************

  **********************************************************************************************
  **/
  @Override
  public boolean available() {
    if (readLength > 0) {
      if (outBufferPos >= outBufferLength) {
        decompressBlock();
      }
      return true;
    }
    return false;
  }

  /**
  **********************************************************************************************

  **********************************************************************************************
  **/
  @Override
  public void close() {
    try {
      readSource.close();
      readSource = null;
    }
    catch (Throwable t) {
      readSource = null;
    }
  }

  /**
  **********************************************************************************************

  **********************************************************************************************
  **/
  @SuppressWarnings("unused")
  public void decompressBlock() {
    try {

      long offset = readSource.getOffset();

      // 4 - Header (34,17,68,85)
      String compressionType = readSource.readString(4);
      if (compressionType.equals("JDLZ") || compressionType.equals("HUFF")) {
        // a single block, doesn't have the extended header
      }
      else {
        // multiple blocks - has an overarching header

        // 4 - Decompressed Block Length?
        int decompLength = readSource.readInt();

        // 4 - Block Length (including all these header fields)
        int blockLength = readSource.readInt() - 24;

        // 4 - Length of this decompressed file when this and all previous blocks have been decompressed
        int paddingCheck = readSource.readInt();

        // 4 - Length of compressed data for all previous blocks (even includes length of the blocks from previous files)
        // 4 - null
        readSource.skip(8);

        // X - Block Data (JDLZ Compression)
        offset = readSource.getOffset();

        // 4 - Header (JDLZ)
        compressionType = readSource.readString(4);
      }

      // 4 - Unknown (4096)
      readSource.skip(4);

      // 4 - Decompressed Length
      outBufferLength = readSource.readInt();
      outBuffer = new byte[outBufferLength];
      outBufferPos = 0;

      // 4 - Compressed Length
      int compBufferLength = readSource.readInt();

      // X - Compressed Data
      if (compressionType.equals("HUFF")) {
        dehuffman(compBufferLength);
        System.out.println("De-Huff Block Finished: " + readSource.getOffset());
      }
      else {
        readSource.seek(offset);
        byte[] compBuffer = readSource.readBytes(compBufferLength);
        int compBufferPos = 16;

        int flags1 = 1, flags2 = 1;
        int t = 0;
        int length = 0;

        while ((compBufferPos < compBufferLength) && (outBufferPos < outBufferLength)) {
          if (flags1 == 1) {
            flags1 = compBuffer[compBufferPos++] | 0x100;
          }
          if (flags2 == 1) {
            flags2 = compBuffer[compBufferPos++] | 0x100;
          }

          if ((flags1 & 1) == 1) {
            if ((flags2 & 1) == 1) // 3 to 4098(?) iterations, backtracks 1 to 16(?) bytes
            {
              // length max is 4098(?) (0x1002), assuming input[inPos] and input[inPos + 1] are both 0xFF
              length = (compBuffer[compBufferPos + 1] | ((compBuffer[compBufferPos] & 0xF0) << 4)) + 3;
              // t max is 16(?) (0x10), assuming input[inPos] is 0xFF
              t = (compBuffer[compBufferPos] & 0x0F) + 1;
            }
            else // 3(?) to 34(?) iterations, backtracks 17(?) to 2064(?) bytes
            {
              // t max is 2064(?) (0x810), assuming input[inPos] and input[inPos + 1] are both 0xFF
              t = (compBuffer[compBufferPos + 1] | ((compBuffer[compBufferPos] & 0xE0) << 3)) + 17;
              // length max is 34(?) (0x22), assuming input[inPos] is 0xFF
              length = (compBuffer[compBufferPos] & 0x1F) + 3;
            }

            compBufferPos += 2;

            for (int i = 0; i < length; ++i) {
              outBuffer[outBufferPos + i] = outBuffer[outBufferPos + i - t];
            }

            outBufferPos += length;
            flags2 >>= 1;
          }
          else {
            if (outBufferPos < outBufferLength) {
              outBuffer[outBufferPos++] = compBuffer[compBufferPos++];
            }
          }
          flags1 >>= 1;
        }
        System.out.println("Decompress Block Finished: " + readSource.getOffset());
      }

      /*
      // X - null Padding to a multiple of 64 bytes (only if field 4 == 0)
      if (paddingCheck == 0) {
        int paddingSize = 64 - ((length + 24) % 64);
        if (paddingSize != 64) {
          readSource.skip(paddingSize);
        }
      }
      */

    }
    catch (Throwable t) {
      //t.printStackTrace();
    }
  }

  /**
  **********************************************************************************************

  **********************************************************************************************
  **/
  public void dehuffman(int compLength) throws Exception {

    // read mapping codes
    int numCodes = readSource.readByte();
    int[] codes = new int[256];
    for (int i = 0; i < numCodes; i++) {
      codes[i] = readSource.readByte();
    }

    int length = compLength - numCodes;

    // read bits into buffer
    boolean[] buffer = new boolean[600];
    int bufferSize = 0;

    int currentPos = 0;
    while (currentPos < length) {
      // determine if a true is in the buffer
      int stop = 0;
      for (int j = 0; j < bufferSize; j++) {
        if (buffer[j]) {
          stop = j + 1;
          j = bufferSize;
        }
      }

      // read the number found
      if (stop != 0) {
        int number = codes[stop - 1];
        outBuffer[outBufferPos] = (byte) number;
        outBufferPos++;

        for (int j = stop; j < bufferSize; j++) {
          buffer[j - stop] = buffer[j];
        }
        bufferSize -= stop;

      }

      // else read another byte into the buffer
      else {

        boolean[] toAdd = readSource.readBits();
        for (int j = 0; j < 8; j++) {
          buffer[bufferSize] = toAdd[j];
          bufferSize++;
        }

        currentPos++;
      }

    }

    // reset the outBuffer position
    outBufferPos = 0;

    // ignore any left-over data

    System.out.println("DONE DE-HUFFMAN");
  }

  /**
  **********************************************************************************************

  **********************************************************************************************
  **/
  @Override
  public String getDescription() {
    return "This exporter decompresses the EA JDLZ format when exporting\n\n" + super.getDescription();
  }

  /**
  **********************************************************************************************

  **********************************************************************************************
  **/
  @Override
  public void open(Resource source) {
    try {
      readSource = new FileManipulator(source.getSource(), false);
      readSource.seek(source.getOffset());
      readLength = source.getLength();

      decompressBlock();
    }
    catch (Throwable t) {
    }
  }

  /**
  **********************************************************************************************

  **********************************************************************************************
  **/
  @Override
  public void pack(Resource source, FileManipulator destination) {
    // not done
  }

  /**
  **********************************************************************************************

  **********************************************************************************************
  **/
  @Override
  public int read() {
    try {
      readLength--;
      //return readSource.read();
      int value = outBuffer[outBufferPos];
      outBufferPos++;
      return value;
    }
    catch (Throwable t) {
      return 0;
    }
  }

}