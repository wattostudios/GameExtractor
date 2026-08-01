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
import org.watto.io.converter.ByteConverter;
import org.watto.io.converter.ShortConverter;

public class Exporter_Custom_MTF extends ExporterPlugin {

  static Exporter_Custom_MTF instance = new Exporter_Custom_MTF();

  byte[] decompBuffer = null;

  int decompPos = 0;

  int decompLength = 0;

  /**
  **********************************************************************************************
  Ref: https://pastebin.com/DycDYxPe
  **********************************************************************************************
  **/
  public static Exporter_Custom_MTF getInstance() {
    return instance;
  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public Exporter_Custom_MTF() {
    setName("MTF Compression");
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
    return "This exporter decompresses MTF files when exporting\n\n" + super.getDescription();
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
      decompBuffer = new byte[decompLength];

      // do the decompression     
      try {
        while (decompPos < decompLength) {
          int control = ByteConverter.unsign(fm.readByte());

          // "You need to check every bit:"
          for (int j = 0; j < 8; ++j) {
            if ((control & (1 << j)) != 0) {
              // "If bit=1 then just copy 1 byte from compressed buffer to decompressed buffer"
              decompBuffer[decompPos++] = fm.readByte();
            }
            else {
              // "If bit=0 then read 10 bits for X and 6 bits for Y" - "07 0C means X = 3 and Y = 7"
              // This is very confusing. What is really meant is:
              // Read 2 bytes, swap them (reading an Int16 in Little-Endian does the trick)
              int word = ShortConverter.unsign(fm.readShort());

              // For some reason, if and only if word == 0, we end up writing past our output buffer.
              // It appears we can safely ignore all of these.
              if (word == 0) {
                break;
              }

              int X = (word >> 10) & 63; // The first 6 bits are then X                        
              int Y = word & 0x3FF; // And the last 10 bits are Y

              // "copy X+3 bytes from offset Y of decompressed buffer at the end of decompressed buffer"
              for (int k = 0; k < X + 3; ++k) {
                decompBuffer[decompPos] = decompBuffer[decompPos - Y];
                ++decompPos;
              }
            }
          }
        }

      }
      catch (Throwable t) {
        ErrorLogger.log(t);
      }

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

}