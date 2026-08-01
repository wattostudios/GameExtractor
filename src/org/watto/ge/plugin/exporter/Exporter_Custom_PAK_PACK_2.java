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

public class Exporter_Custom_PAK_PACK_2 extends ExporterPlugin {

  static Exporter_Custom_PAK_PACK_2 instance = new Exporter_Custom_PAK_PACK_2();

  byte[] decompBuffer = null;

  int decompPos = 0;

  int decompLength = 0;

  /**
  **********************************************************************************************
  Ref: https://gist.github.com/DanielGibson/8bde6241c93e5efe8b75e5e00d0b9858
  **********************************************************************************************
  **/
  public static Exporter_Custom_PAK_PACK_2 getInstance() {
    return instance;
  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public Exporter_Custom_PAK_PACK_2() {
    setName("PAK_PACK_2 Compression");
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
    return "This exporter decompresses PAK_PACK_2 files when exporting\n\n" + super.getDescription();
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
        for (int i = 0; i < decompLength; i++) {
          int control = ByteConverter.unsign(fm.readByte());

          if (control < 64) {
            //x+1 bytes of uncompressed data follow (just read+write them as they are)
            int count = control + 1;
            for (int c = 0; c < count; c++) {
              decompBuffer[decompPos++] = fm.readByte();
            }
          }
          else if (control < 128) {
            // run-length encoded zeros
            //write (x - 62) zero-bytes to output
            int count = control - 62;
            for (int c = 0; c < count; c++) {
              decompBuffer[decompPos++] = 0;
            }
          }
          else if (control < 192) {
            // run-length encoded data
            //read one byte, write it (x-126) times to output
            int count = control - 126;
            int value = fm.readByte();
            for (int c = 0; c < count; c++) {
              decompBuffer[decompPos++] = (byte) value;
            }
          }
          else if (control < 254) {
            // this references previously uncompressed data
            //read one byte to get _offset_
            int offset = ByteConverter.unsign(fm.readByte()) + 2;
            offset = decompPos - offset;

            //read (x-190) bytes from the already uncompressed and written output data, starting at (offset+2) bytes before the current write position
            //(and add them to output, of course)
            int count = control - 190;

            for (int c = 0; c < count; c++) {
              decompBuffer[decompPos++] = decompBuffer[offset++];
            }

          }
          else if (control == 255) {
            //you're done decompressing (used as terminator)
            // but I'd also abort once compressed_length bytes are read, to be sure
            break;
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