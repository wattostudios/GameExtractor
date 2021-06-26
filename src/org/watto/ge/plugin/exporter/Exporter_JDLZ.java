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

public class Exporter_JDLZ extends ExporterPlugin {

  static Exporter_JDLZ instance = new Exporter_JDLZ();

  static FileManipulator readSource;
  static long readLength = 0;

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public static Exporter_JDLZ getInstance() {
    return instance;
  }

  int numRead;
  short nPos;
  short nSize;
  byte nAction1 = 0;
  byte nBitCount1 = 0;
  byte nAction2 = 0;

  byte nBitCount2 = 0;
  int bufferSize = 150000;
  byte[] buffer = new byte[bufferSize];
  int bufferFillPos = 0;

  int readPos = 0;

  // globals for packing
  long codeOffset1 = 16;

  int code = 0;

  int codeCount1 = 0;

  long codeOffset2 = 17;

  int code2 = 0;

  int codeCount2 = 0;

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public Exporter_JDLZ() {
    setName("JDLZ Compression");
  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  @Override
  public boolean available() {
    if (numRead < readLength) {
      return true;
    }

    if (readPos < bufferFillPos) {
      return true;
    }
    else {
      fillBuffer();

      if (numRead < readLength || readPos < bufferFillPos) {
        return true;
      }
      else {
        return false;
      }
    }

  }

  /**
   **********************************************************************************************
   * Calculates the check compression code for part 1
   **********************************************************************************************
   **/
  public void checkCode1(FileManipulator destination) {
    try {
      if (codeCount1 == 8) {
        codeCount1 = 0;
        codeOffset1 = destination.getOffset();
        destination.writeByte((byte) 0);
      }
    }
    catch (Exception e) {
      System.out.println(e);
    }
  }

  /**
   **********************************************************************************************
   * Calculates the check compression code for part 2
   **********************************************************************************************
   **/
  public void checkCode2(FileManipulator destination) {
    try {
      if (codeCount2 == 8) {
        codeCount2 = 0;
        codeOffset2 = destination.getOffset();
        destination.writeByte((byte) 0);
      }
    }
    catch (Exception e) {
      System.out.println(e);
    }
  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public void checkFitsInBuffer(int length) {
    if (bufferFillPos + length >= bufferSize) {
      // data won't fit in the buffer, so move the buffer along
      bufferFillPos -= length;
      readPos -= length;
      System.arraycopy(buffer, length, buffer, 0, bufferFillPos);
    }
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
  public void fillBuffer() {

    if (nBitCount1 == 0) {
      nAction1 = readSource.readByte();
      nBitCount1 = 8;
      numRead++;
    }

    if (nBitCount2 == 0) {
      nAction2 = readSource.readByte();
      nBitCount2 = 8;
      numRead++;
    }

    if ((nAction1 & 1) != 0) {
      int byte1 = readSource.readByte();
      int byte2 = readSource.readByte();
      numRead += 2;

      if ((nAction2 & 1) != 0) {
        nPos = (short) ((byte1 & 0x0F) + 1);
        nSize = (short) (((byte1 & 0xF0) << 4) + byte2 + 3);
      }
      else {
        nPos = (short) (((byte1 & 0xE0) << 3) + byte2 + 17);
        nSize = (short) ((byte1 & 0x1F) + 3);
      }

      checkFitsInBuffer(nSize);

      for (int i = 0; i < nSize; i++) {
        buffer[bufferFillPos] = buffer[bufferFillPos - nPos];
        bufferFillPos++;
      }

      nAction2 >>= 1;
      nBitCount2--;
    }
    else {

      checkFitsInBuffer(1);

      buffer[bufferFillPos] = readSource.readByte();
      bufferFillPos++;

      numRead++;
    }

    nAction1 >>= 1;
    nBitCount1--;
  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  @Override
  public String getDescription() {
    return "JDLZ Compression used by several EA games for audio files.\n\n" + super.getDescription();
  }

  /**
   **********************************************************************************************
   * Calculates the next compression code for part 1
   * @param codeval apply an adjustment to the code, or not?
   **********************************************************************************************
   **/
  public void nextCode1(FileManipulator destination, boolean codeval) {
    try {
      codeCount1++;
      code = code | (codeval ? 0x100 : 0);
      code = (code) >> 1;
      code = (code & 0xFF);

      long currentPos = destination.getOffset();
      destination.seek(codeOffset1);
      destination.writeByte((code) & 0xFF);
      destination.seek(currentPos);
    }
    catch (Exception e) {
      System.out.println(e);
    }
  }

  /**
   **********************************************************************************************
   * Calculates the next compression code for part 2
   * @param codeval apply an adjustment to the code, or not?
   **********************************************************************************************
   **/
  public void nextCode2(FileManipulator destination, boolean codeval) {
    try {
      codeCount2++;
      code2 = code2 | (codeval ? 0x100 : 0);
      code2 = (code2) >> 1;
      code2 = (code2 & 0xFF);

      long currentPos = destination.getOffset();
      destination.seek(codeOffset2);
      destination.writeByte((code2) & 0xFF);
      destination.seek(currentPos);
    }
    catch (Exception e) {
      System.out.println(e);
    }
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

      // 4 - Header (JDLZ)
      // 4 - Version
      // 4 - Compressed Length

      nAction1 = 0;
      nBitCount1 = 0;
      nAction2 = 0;
      nBitCount2 = 0;

      numRead = 16;

      bufferFillPos = 0;
      readPos = 0;

    }
    catch (Throwable t) {
    }
  }

  /**
   **********************************************************************************************
   * NOT TESTED!!!
   **********************************************************************************************
   **/
  @Override
  public void pack(Resource source, FileManipulator destination) {
    try {

      ExporterPlugin exporter = source.getExporter();
      exporter.open(source);

      long relativeOffset = destination.getOffset();

      // 4 - Header (JDLZ)
      destination.writeString("JDLZ");

      // 4 - Version
      destination.writeInt(4098);

      // 4 - Compressed Length
      destination.writeInt((int) source.getDecompressedLength());

      int repeatCount = 0;
      int currentByte = exporter.read();

      while (exporter.available()) {

        int nextByte = exporter.read();

        if ((currentByte == nextByte) && (repeatCount < 4096)) {
          // repeating values found, so continue reading until no longer a repeat.
          repeatCount++;
        }
        else {
          if (repeatCount > 0) {
            if (repeatCount > 2) {
              // finished the repeating block, so write it out.

              int copylen = (repeatCount) - 3;
              int lookback = (1) - 1;
              int emit1 = ((copylen & 0xF00) >> 4) | (lookback & 0xF);
              int emit2 = (copylen & 0xFF);

              nextCode1(destination, true);
              nextCode2(destination, true);

              destination.writeByte(emit1);
              destination.writeByte(emit2);

              checkCode1(destination);
              checkCode2(destination);

              repeatCount = 0;
            }
            else {
              for (int i = 0; i < repeatCount; i++) {
                nextCode1(destination, false);
                destination.writeByte(currentByte);
                checkCode1(destination);
              }
              repeatCount = 0;
            }

          }
          nextCode1(destination, false);
          destination.writeByte(currentByte);
          checkCode1(destination);

          currentByte = nextByte;
        }
      }

      // go back and write the correct decompressed length
      long endOffset = destination.getOffset();
      int decompLength = (int) (endOffset - relativeOffset);
      destination.seek(relativeOffset + 12);
      destination.writeInt(decompLength);

      // return to the end of this file, so the next file will be written at the correct spot
      destination.seek(endOffset);

      exporter.close();

      //destination.forceWrite();

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
      int value = buffer[readPos];
      readPos++;
      return value;
    }
    catch (Throwable t) {
      return 0;
    }
  }

}