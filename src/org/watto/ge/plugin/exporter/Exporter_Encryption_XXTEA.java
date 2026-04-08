/*
 * Application:  Game Extractor
 * Author:       wattostudios
 * Website:      http://www.watto.org
 * Copyright:    Copyright (c) 2002-2025 wattostudios
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
import org.xxtea.XXTEA;

public class Exporter_Encryption_XXTEA extends ExporterPlugin {

  static Exporter_Encryption_XXTEA instance = new Exporter_Encryption_XXTEA();

  static long readLength = 0;

  byte[] buffer = new byte[0];

  int bufferLength = 0;

  int bufferPos = 0;

  byte[] key = new byte[0];

  /**
  **********************************************************************************************
  Decrypts using the XXTEA algorithm
  **********************************************************************************************
  **/
  public static Exporter_Encryption_XXTEA getInstance() {
    return instance;
  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public Exporter_Encryption_XXTEA() {
    setName("XXTEA Decryption");
  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public byte[] getKey() {
    return key;
  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public void setKey(byte[] key) {
    this.key = key;
  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public Exporter_Encryption_XXTEA(byte[] key) {
    setName("XXTEA Decryption");
    this.key = key;
  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  @Override
  public boolean available() {
    try {
      if (readLength > 0) {
        if (bufferPos >= bufferLength) {
          // already read in the full buffer, so if we need more, we've overflowed
          return false;
        }
        return true;
      }
      return false;
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
      buffer = null;
    }
    catch (Throwable t) {
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

      FileManipulator fm = new FileManipulator(source.getSource(), false);
      fm.seek(source.getOffset());

      open(fm, (int) readLength, (int) readLength);

      fm.close();

    }
    catch (Throwable t) {
    }
  }

  /**
  **********************************************************************************************
  So we can easily call this from within a Viewer plugin
  **********************************************************************************************
  **/
  public void open(FileManipulator fmIn, int compLengthIn, int decompLengthIn) {
    try {

      readLength = decompLengthIn;

      // read in the data
      buffer = fmIn.readBytes(decompLengthIn);
      bufferLength = decompLengthIn;
      bufferPos = 0;

      // decrypt it
      buffer = XXTEA.decrypt(buffer, key);

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

  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  @Override
  public int read() {
    try {
      readLength--;

      byte currentByte = buffer[bufferPos];
      bufferPos++;

      return currentByte;
    }
    catch (Throwable t) {
      return 0;
    }
  }

}