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
import org.watto.io.converter.ByteConverter;

/**
**********************************************************************************************

**********************************************************************************************
**/
public class Exporter_Custom_GLB extends ExporterPlugin {

  static Exporter_Custom_GLB instance = new Exporter_Custom_GLB();

  static FileManipulator readSource;

  static long readLength = 0;

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public static Exporter_Custom_GLB getInstance() {
    return instance;
  }

  byte[] key = new byte[0];

  int keyPos = 0;

  int previousByte = 0;

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public Exporter_Custom_GLB() {
    setName("GLB Encrypted File");
  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public Exporter_Custom_GLB(byte[] key) {
    super();
    setName("GLB Encrypted File");
    this.key = key;
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

      // init the key
      keyPos = 25 % 8;
      previousByte = key[keyPos];

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

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  @Override
  public int read() {
    try {
      readLength--;
      return (decryptByte(readSource.readByte()));
    }
    catch (Throwable t) {
      return 0;
    }
  }

  /**
   **********************************************************************************************
  
   **********************************************************************************************
   **/

  public byte decryptByte(byte currentByte) {
    // 1. Subtract the character value from the current position in the encryption key (i.e. if the current position is 0, subtract 0x33, the character code for "3")
    int decryptedByte = ByteConverter.unsign(currentByte) - key[keyPos];

    // 2. Advance the position in the encryption key by one (i.e. go to the next letter)
    keyPos++;

    // 3. If the end of the encryption key has been reached, go back to the first character
    if (keyPos >= key.length) {
      keyPos = 0;
    }

    // 4. Subtract the value of the previous byte read (note the previous byte *read*, not the decrypted version of that byte)
    decryptedByte -= previousByte;

    // 5. Logical AND with 0xFF to limit the result to 0-255
    decryptedByte &= 255;

    // 6. This byte is now decoded, move on to the next
    previousByte = currentByte;

    return (byte) decryptedByte;
  }

}