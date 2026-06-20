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

import org.watto.ErrorLogger;
import org.watto.datatype.Resource;
import org.watto.ge.plugin.ExporterPlugin;
import org.watto.io.FileManipulator;

public class Exporter_LZ11 extends ExporterPlugin {

  static Exporter_LZ11 instance = new Exporter_LZ11();

  static long readLength = 0;

  /**
  **********************************************************************************************
  Ref: https://github.com/SunakazeKun/AlmiaE/blob/master/src/com/aurum/almia/game/Compression.java#L76
  **********************************************************************************************
  **/
  public static Exporter_LZ11 getInstance() {
    return instance;
  }

  byte[] readBuffer = new byte[0];

  int readBufferPos = 0;

  int readBufferLength = 0;

  FileManipulator fm;

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public Exporter_LZ11() {
    setName("LZ11 Compression");
  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  @Override
  public boolean available() {
    try {
      return (readBufferPos < readBufferLength);
    }
    catch (Throwable t) {
      ErrorLogger.log(t);
      return false;
    }
  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  @Override
  public void close() {
    try {
      readBuffer = new byte[0];
      readBufferPos = 0;
      readBufferLength = 0;

      fm.close();
    }
    catch (Throwable t) {
    }
  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  byte[] decompress(byte[] compressed, int decompressedSize) {

    byte[] decompressed = new byte[decompressedSize];
    int curIn = 0;
    int curOut = 0;

    try {
      while (curOut < decompressed.length && curIn < compressed.length) {
        byte flags = compressed[curIn++];

        for (int fi = 0; fi < 8 && curIn < compressed.length; fi++) {
          if ((flags & (0x80 >> fi)) != 0) {
            int token = compressed[curIn++] & 0xFF;
            int token2 = compressed[curIn++] & 0xFF;
            int token3, token4;
            int disp;
            int lenCopy;
            int offCopy;

            switch (token >> 4) {
            case 0:
              token3 = compressed[curIn++] & 0xFF;

              disp = ((token2 & 0xF) << 8) | token3;
              lenCopy = 0x11 + (((token & 0xF) << 4) | (token2 >> 4));
              break;
            case 1:
              token3 = compressed[curIn++] & 0xFF;
              token4 = compressed[curIn++] & 0xFF;

              disp = ((token3 & 0xF) << 8) | token4;
              lenCopy = 0x111 + (((token & 0xF) << 12) | (token2 << 4) | (token3 >> 4));
              break;
            default:
              disp = ((token & 0xF) << 8) | token2;
              lenCopy = 0x1 + (token >> 4);
            }

            offCopy = curOut - disp - 1;

            for (int i = 0; i < lenCopy; i++)
              decompressed[curOut++] = decompressed[offCopy + i];
          }
          else
            decompressed[curOut++] = compressed[curIn++];
        }
      }
    }
    catch (Throwable t) {
      ErrorLogger.log(t);
    }

    return decompressed;

  }

  /**
  **********************************************************************************************
  So we can easily call this from within a Viewer plugin
  **********************************************************************************************
  **/
  public void open(FileManipulator fmIn, int compLengthIn, int decompLengthIn) {
    try {
      fm = fmIn;

      readLength = decompLengthIn;

      readBufferLength = (int) readLength;
      readBufferPos = 0;

      byte[] compBytes = fm.readBytes(compLengthIn);
      readBuffer = decompress(compBytes, decompLengthIn);

    }
    catch (Throwable t) {
      ErrorLogger.log(t);
    }
  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  @Override
  public void open(Resource source) {
    try {
      fm = new FileManipulator(source.getSource(), false);
      fm.seek(source.getOffset());

      readLength = source.getDecompressedLength();

      readBufferLength = (int) readLength;
      readBufferPos = 0;

      byte[] compBytes = fm.readBytes((int) source.getLength());
      readBuffer = decompress(compBytes, (int) readLength);

    }
    catch (Throwable t) {
      ErrorLogger.log(t);
    }
  }

  /**
  **********************************************************************************************
  NOT IMPLEMENTED
  **********************************************************************************************
  **/
  @Override
  public void pack(Resource source, FileManipulator destination) {
    // NOT IMPLEMENTED
  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  @Override
  public int read() {
    try {
      int currentByte = readBuffer[readBufferPos];
      readBufferPos++;
      return currentByte;
    }
    catch (Throwable t) {
      t.printStackTrace();
      readLength = 0;
      return 0;
    }
  }

}