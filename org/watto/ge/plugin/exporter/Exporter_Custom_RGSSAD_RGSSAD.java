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
import org.watto.io.converter.IntConverter;

public class Exporter_Custom_RGSSAD_RGSSAD extends ExporterPlugin {

  static Exporter_Custom_RGSSAD_RGSSAD instance = new Exporter_Custom_RGSSAD_RGSSAD();

  static FileManipulator readSource;

  static long readLength = 0;

  /**
  **********************************************************************************************
  Decrypts RGSSAD_RGSSAD file data
  **********************************************************************************************
  **/
  public static Exporter_Custom_RGSSAD_RGSSAD getInstance() {
    return instance;
  }

  int decryptionKey = 0;

  int originalDecryptionKey = 0;

  byte[] decryptedBuffer = new byte[4];

  int bufferPos = 0;

  int bufferLength = 0;

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public Exporter_Custom_RGSSAD_RGSSAD() {
    setName("Decrypts RGSSAD_RGSSAD file data");
  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public Exporter_Custom_RGSSAD_RGSSAD(int decryptionKey) {
    super();
    this.decryptionKey = decryptionKey;
    this.originalDecryptionKey = decryptionKey;
  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  @Override
  public boolean available() {

    if (bufferPos >= bufferLength) {
      // read the next 4 bytes and decrypt them

      // first, see if we have 4 more bytes to read, or less
      int intValue = 0;
      if (readLength >= 4) {
        intValue = readSource.readInt();
      }
      else {
        byte[] bytes4 = new byte[] { 0, 0, 0, 0 };
        for (int i = 0; i < readLength; i++) {
          bytes4[i] = readSource.readByte();
        }
        intValue = IntConverter.convertLittle(bytes4);
      }
      intValue = decryptBytes(intValue);
      decryptedBuffer = ByteArrayConverter.convertLittle(intValue);
      bufferPos = 0;
      bufferLength = 4; // doesn't matter if readLength is less than this, the available() will cut it short
    }

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
   * WORKING
   **********************************************************************************************
   **/

  public int decryptBytes(int intValue) {
    intValue ^= decryptionKey;
    decryptionKey *= 7;
    decryptionKey += 3;
    return intValue;
  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  @Override
  public String getDescription() {
    return "This exporter decrypts RGSSAD_RGSSAD file data.\n\n" + super.getDescription();
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

      bufferPos = 0;
      bufferLength = 0;

      decryptionKey = originalDecryptionKey; // reset the decryption key back to the beginning

    }
    catch (Throwable t) {
    }
  }

  /**
   **********************************************************************************************
   * NOT DONE
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

      byte returnByte = decryptedBuffer[bufferPos];
      bufferPos++;

      return returnByte;
    }
    catch (Throwable t) {
      return 0;
    }
  }

}