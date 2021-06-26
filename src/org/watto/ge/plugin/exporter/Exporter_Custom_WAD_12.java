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

public class Exporter_Custom_WAD_12 extends ExporterPlugin {

  static Exporter_Custom_WAD_12 instance = new Exporter_Custom_WAD_12();

  static FileManipulator packerSource;
  static long readLength = 0;

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public static Exporter_Custom_WAD_12 getInstance() {
    return instance;
  }

  int byteLength = 1;

  int bufferPos = 0;

  int bufferLength = 0;

  byte[] buffer = new byte[0];

  int minimumDuplicateLength = 2;

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public Exporter_Custom_WAD_12() {
    setName("Croc RLE Decompressor");
  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  @Override
  public boolean available() {
    if (readLength > 0 || bufferPos < bufferLength) {
      return true;
    }
    return false;
  }

  /**
   **********************************************************************************************
   * Closes the global - packing has finished.
   **********************************************************************************************
   **/
  @Override
  public void close() {
    try {
      packerSource.close();
      packerSource = null;
    }
    catch (Throwable t) {
      packerSource = null;
    }
  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public int getByteLength() {
    return byteLength;
  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  @Override
  public String getDescription() {
    return "This exporter decompressed the RLE compression used in the Croc game\n\n" + super.getDescription();
  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  @Override
  public void open(Resource source) {
    try {
      packerSource = new FileManipulator(source.getSource(), false);
      packerSource.seek(source.getOffset());
      readLength = source.getLength();

      bufferPos = 0;
      bufferLength = 0;
      buffer = new byte[0];
    }
    catch (Throwable t) {
    }
  }

  /**
  **********************************************************************************************
  Just raw output, no compression
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

      if (bufferPos < bufferLength) {
        // // still reading from the buffer
        byte currentByte = buffer[bufferPos];
        bufferPos++;
        return currentByte;
      }

      // read the next header and work out what to do

      byte currentByte = packerSource.readByte();
      readLength--;

      if (currentByte < 0) {
        // copy the next X bytes raw
        bufferLength = (0 - currentByte) * byteLength;
        buffer = packerSource.readBytes(bufferLength);
        readLength -= bufferLength;
        bufferPos = 0;
      }
      else {
        // repeating bytes
        bufferLength = (currentByte + minimumDuplicateLength) * byteLength;
        buffer = new byte[bufferLength];

        if (byteLength == 1) {
          // grab the next 1 byte and repeat it X times
          currentByte = packerSource.readByte();
          readLength--;
          for (int i = 0; i < bufferLength; i++) {
            buffer[i] = currentByte;
          }
        }
        else if (byteLength == 2) {
          // grab the next 2 bytes and repeat them X times
          currentByte = packerSource.readByte();
          byte currentByte2 = packerSource.readByte();
          readLength -= 2;
          for (int i = 0; i < bufferLength; i += 2) {
            buffer[i] = currentByte;
            buffer[i + 1] = currentByte2;
          }
        }

        bufferPos = 0;
      }

      if (bufferPos < bufferLength) {
        currentByte = buffer[bufferPos];
        bufferPos++;
        return currentByte;
      }
      else {
        // premature EOF
        return 0;
      }

    }
    catch (Throwable t) {
      return 0;
    }
  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public void setByteLength(int byteLength) {
    this.byteLength = byteLength;

    if (byteLength == 1) {
      minimumDuplicateLength = 3;
    }
    else {
      minimumDuplicateLength = 2;
    }
  }

}