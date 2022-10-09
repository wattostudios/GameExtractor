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

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import org.watto.ErrorLogger;
import org.watto.datatype.Resource;
import org.watto.ge.plugin.ExporterPlugin;
import org.watto.io.FileManipulator;

public class Exporter_Encryption_AES extends ExporterPlugin {

  static Exporter_Encryption_AES instance = new Exporter_Encryption_AES();

  static FileManipulator readSource;

  static long readLength = 0;

  Cipher cipher = null;

  byte[] buffer = new byte[0];

  int bufferLength = 0;

  int bufferPos = 0;

  byte[] key = new byte[0];

  /**
  **********************************************************************************************
  Decrypts using the AES algorithm
  **********************************************************************************************
  **/
  public static Exporter_Encryption_AES getInstance() {
    return instance;
  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public Exporter_Encryption_AES() {
    setName("AES Decryption");
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
  public Exporter_Encryption_AES(byte[] key) {
    setName("AES Decryption");
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
          // need to decrypt the next block of data

          bufferLength = 2048;
          if (bufferLength > readLength) {
            bufferLength = (int) readLength;
          }

          //buffer = cipher.doFinal(readSource.readBytes(bufferLength));
          buffer = cipher.update(readSource.readBytes(bufferLength));
          bufferLength = buffer.length;

          bufferPos = 0;
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

      buffer = new byte[0];
      bufferLength = 0;
      bufferPos = 0;

      //MessageDigest sha = MessageDigest.getInstance("SHA-1");
      //byte[] digestedKey = sha.digest(key);
      //digestedKey = Arrays.copyOf(digestedKey, 16);

      //cipher = Cipher.getInstance("AES/ECB/PKCS5PADDING");
      cipher = Cipher.getInstance("AES/ECB/NoPadding");
      SecretKeySpec keySpec = new SecretKeySpec(key, "AES");
      cipher.init(Cipher.DECRYPT_MODE, keySpec);

      readSource = new FileManipulator(source.getSource(), false);
      readSource.seek(source.getOffset());

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
      readSource = fmIn;

      buffer = new byte[0];
      bufferLength = 0;
      bufferPos = 0;

      //MessageDigest sha = MessageDigest.getInstance("SHA-1");
      //byte[] digestedKey = sha.digest(key);
      //digestedKey = Arrays.copyOf(digestedKey, 16);

      //cipher = Cipher.getInstance("AES/ECB/PKCS5PADDING");
      cipher = Cipher.getInstance("AES/ECB/NoPadding");
      SecretKeySpec keySpec = new SecretKeySpec(key, "AES");
      cipher.init(Cipher.DECRYPT_MODE, keySpec);

      readLength = decompLengthIn;
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