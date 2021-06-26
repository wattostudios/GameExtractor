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
import org.watto.io.converter.ByteArrayConverter;

public class Exporter_Custom_VIS_VIS3_WEBP extends ExporterPlugin {

  static Exporter_Custom_VIS_VIS3_WEBP instance = new Exporter_Custom_VIS_VIS3_WEBP();

  static FileManipulator readSource;

  static long readLength = 0;

  /**
  **********************************************************************************************
  Decrypts the WebP image header used by the VIS_VIS3_# plugins
  **********************************************************************************************
  **/
  public static Exporter_Custom_VIS_VIS3_WEBP getInstance() {
    return instance;
  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public Exporter_Custom_VIS_VIS3_WEBP() {
    setName("Decrypts WebP images from VIS_VIS3 archives");
  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  @Override
  public boolean available() {
    return readLength > 0;
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

  byte[] headerBytes = null;

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  @Override
  public void open(Resource source) {
    try {
      readLength = source.getLength();

      // try to get the whole file in a single go, if it isn't too large (set to 200KB otherwise)
      int bufferSize = (int) readLength;
      if (bufferSize > 204800) {
        bufferSize = 204800;
      }

      readSource = new FileManipulator(source.getSource(), false, bufferSize);
      readSource.seek(source.getOffset());

      // reset the properties that will decrypt the header in read()
      readingHeader = true;
      headerPos = 0;

      // decrypt the header
      headerBytes = readSource.readBytes(34);
      if (headerBytes[30] == 65 && headerBytes[31] == 76 && headerBytes[32] == 80 && headerBytes[33] == 72) {
        // has alpha
        headerBytes[16] = 10;
        headerBytes[17] = 0;
        headerBytes[18] = 0;
        headerBytes[19] = 0;
        headerBytes[20] = 16;
        headerBytes[21] = 0;
        headerBytes[22] = 0;
        headerBytes[23] = 0;
      }
      else {
        // no alpha
        if (headerBytes[15] == 88) {
          // VP8X format
          headerBytes[16] = 10;
          headerBytes[17] = 0;
          headerBytes[18] = 0;
          headerBytes[19] = 0;
          headerBytes[20] = 0;
          headerBytes[21] = 0;
          headerBytes[22] = 0;
          headerBytes[23] = 0;
        }
        else {
          // VP8 format

          // don't know how to fix this yet, because don't know how to decode bytes 20,21,22 appropriately for this image (needs to be somewhat related to the webp block size)
          byte[] readLengthBytes = ByteArrayConverter.convertLittle((int) readLength - 20);
          headerBytes[16] = readLengthBytes[0];
          headerBytes[17] = readLengthBytes[1];
          headerBytes[18] = readLengthBytes[2];
          headerBytes[19] = readLengthBytes[3];
          headerBytes[20] = (byte) 210;
          headerBytes[21] = 75;
          headerBytes[22] = 2;
          headerBytes[23] = (byte) 157;
        }

      }

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

      //destination.forceWrite();

    }
    catch (Throwable t) {
      logError(t);
    }
  }

  boolean readingHeader = true;

  int headerPos = 0;

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  @Override
  public int read() {
    try {

      int currentByte = -1;
      if (readingHeader) {
        currentByte = headerBytes[headerPos];
        headerPos++;
        if (headerPos >= 34) {
          readingHeader = false;
        }
      }
      else {
        currentByte = readSource.readByte();
      }
      readLength--;

      return currentByte;
    }
    catch (Throwable t) {
      return 0;
    }
  }

}