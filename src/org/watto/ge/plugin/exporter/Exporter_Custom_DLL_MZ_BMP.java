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

public class Exporter_Custom_DLL_MZ_BMP extends ExporterPlugin {

  static Exporter_Custom_DLL_MZ_BMP instance = new Exporter_Custom_DLL_MZ_BMP();

  static FileManipulator readSource;
  static long readLength = 0;

  /**
  **********************************************************************************************

  **********************************************************************************************
  **/
  public static Exporter_Custom_DLL_MZ_BMP getInstance() {
    return instance;
  }

  byte[] buffer = null;

  int bufferPos = 0;

  /**
  **********************************************************************************************

  **********************************************************************************************
  **/
  public Exporter_Custom_DLL_MZ_BMP() {
    setName("DLL Resource Bitmap Image Exporter");
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

  /**
  **********************************************************************************************

  **********************************************************************************************
  **/
  @Override
  public String getDescription() {
    return "This exporter extracts Bitmap Image resources from Executable programs (DLL, EXE, etc) when exporting.\n\n" + super.getDescription();
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

      // This is a BMP image without the first 14 bytes of the header.
      // This exporter will rebuild the header and then export the image data.
      // ie. rebuilds the complete BMP image

      // we need to determine the number of colors first! (for the pixel offset)
      int pixelOffset = 0;

      // 4 Bytes - Size Of Info Header (40)
      // 4 Bytes - Width
      // 4 Bytes - Height
      // 2 Bytes - Number Of Planes (1)
      readSource.skip(14);

      // 2 Bytes - Bit Count (1/4/8/16/24/32)
      int bitCount = readSource.readShort();
      if (bitCount <= 8) {
        // 4 Bytes - Compression (0/1/2)
        // 4 Bytes - Compressed Image Size (or 0 if uncompressed)
        // 4 Bytes - X Pixels Per Meter
        // 4 Bytes - Y Pixels Per Meter
        readSource.skip(16);

        // 4 Bytes - Actual Number Of Colors Used
        pixelOffset = readSource.readInt() * 4 + 54;
      }
      else {
        pixelOffset = 54;
      }

      // back to the start of the image, ready for export.
      readSource.seek(source.getOffset());

      readLength = source.getLength() + 14;

      buffer = new byte[14];
      bufferPos = 0;

      // 2 Bytes - Header (BM)
      byte[] header = "BM".getBytes();
      buffer[0] = header[0];
      buffer[1] = header[1];

      // 4 Bytes - File Size
      byte[] fileSizeByte = ByteArrayConverter.convertLittle((int) readLength);
      buffer[2] = fileSizeByte[0];
      buffer[3] = fileSizeByte[1];
      buffer[4] = fileSizeByte[2];
      buffer[5] = fileSizeByte[3];

      // 4 Bytes - Blank (0)
      buffer[6] = 0;
      buffer[7] = 0;
      buffer[8] = 0;
      buffer[9] = 0;

      // 4 Bytes - Pixel Data Offset (1054)
      byte[] pixelOffsetByte = ByteArrayConverter.convertLittle(pixelOffset);
      buffer[10] = pixelOffsetByte[0];
      buffer[11] = pixelOffsetByte[1];
      buffer[12] = pixelOffsetByte[2];
      buffer[13] = pixelOffsetByte[3];

    }
    catch (Throwable t) {
    }
  }

  /**
   **********************************************************************************************
   * // TEST - NOT DONE
   **********************************************************************************************
   **/
  @Override
  public void pack(Resource source, FileManipulator destination) {
    try {
      //long decompLength = source.getDecompressedLength();

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
      readLength--;

      // uses all the buffer data, then writes the file as normal when the buffer empties
      if (buffer != null) {
        int buffByte = buffer[bufferPos];

        bufferPos++;
        if (bufferPos >= buffer.length) {
          buffer = null;
        }

        return buffByte;
      }

      return readSource.readByte();
    }
    catch (Throwable t) {
      return 0;
    }
  }

}