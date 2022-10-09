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

import java.security.MessageDigest;
import org.watto.ErrorLogger;
import org.watto.datatype.Resource;
import org.watto.ge.plugin.ExporterPlugin;
import org.watto.io.FileManipulator;
import org.watto.io.converter.ByteConverter;

/**
**********************************************************************************************
Ref: https://github.com/koolkdev/rsdkv5_extract/blob/615219d5d5df9cc1e5fa2b48615359729b586e5b/rsdkv5.py
**********************************************************************************************
**/
public class Exporter_Custom_RSDK_RSDK extends ExporterPlugin {

  static Exporter_Custom_RSDK_RSDK instance = new Exporter_Custom_RSDK_RSDK();

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public static Exporter_Custom_RSDK_RSDK getInstance() {
    return instance;
  }

  int[] key1 = null;

  int[] key2 = null;

  byte[] buffer = null;

  int bufferPos = 0;

  int bufferLength = 0;

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public Exporter_Custom_RSDK_RSDK() {
    setName("RSDK Encrypted File");
  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  @Override
  public boolean available() {
    return bufferPos < bufferLength;
  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  @Override
  public void close() {
    buffer = null;
  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  @Override
  public void open(Resource source) {
    try {
      bufferPos = 0;
      bufferLength = (int) source.getLength();

      FileManipulator fm = new FileManipulator(source.getSource(), false);
      fm.seek(source.getOffset());
      buffer = fm.readBytes(bufferLength);
      fm.close();

      // Calculate the keys
      MessageDigest md5gen = MessageDigest.getInstance("MD5");

      String name = source.getName();
      key1 = endianSwap(md5gen.digest(name.toUpperCase().getBytes("UTF-8")));
      key2 = endianSwap(md5gen.digest(("" + bufferLength).getBytes("UTF-8")));

      // prepare for decryption
      key1_index = 0;
      key2_index = 8;
      swap_nibbles = 0;
      xor_value = (bufferLength >> 2) & 0x7f;
    }
    catch (Throwable t) {
      ErrorLogger.log(t);
    }
  }

  int key1_index = 0;

  int key2_index = 8;

  int swap_nibbles = 0;

  int xor_value = 0;

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  @Override
  public void pack(Resource source, FileManipulator destination) {
    try {

      ExporterPlugin exporter = source.getExporter();
      exporter.open(source);

      // prepare for encryption
      MessageDigest md5gen = MessageDigest.getInstance("MD5");

      String name = source.getName();
      key1 = endianSwap(md5gen.digest(name.toUpperCase().getBytes("UTF-8")));
      key2 = endianSwap(md5gen.digest(("" + bufferLength).getBytes("UTF-8")));

      key1_index = 0;
      key2_index = 8;
      swap_nibbles = 0;
      xor_value = ((int) source.getDecompressedLength() >> 2) & 0x7f;

      while (exporter.available()) {
        destination.writeByte(encryptByte(exporter.read()));
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
      int currentByte = decryptByte(buffer[bufferPos]);
      bufferPos++;
      return currentByte;
    }
    catch (Throwable t) {
      return 0;
    }
  }

  /**
  **********************************************************************************************
  Does endian swapping for every 4 byte block, and unsigns all the values
  **********************************************************************************************
  **/
  public int[] endianSwap(byte[] bytes) {
    int byteLength = bytes.length;

    int[] outBytes = new int[byteLength];
    for (int i = 0; i < byteLength; i += 4) {
      outBytes[i + 3] = ByteConverter.unsign(bytes[i]);
      outBytes[i + 2] = ByteConverter.unsign(bytes[i + 1]);
      outBytes[i + 1] = ByteConverter.unsign(bytes[i + 2]);
      outBytes[i] = ByteConverter.unsign(bytes[i + 3]);
    }

    return outBytes;
  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public int decryptByte(byte currentByte) {
    int c = ByteConverter.unsign(currentByte);

    // do the decryption
    c ^= xor_value ^ key2[key2_index];
    if (swap_nibbles != 0) {
      c = (c >> 4) | ((c & 0xf) << 4);
    }
    c ^= key1[key1_index];

    // Update things for the next call
    key1_index += 1;
    key2_index += 1;
    if (key1_index > 15 && key2_index > 8) {
      xor_value += 2;
      xor_value &= 0x7f;
      if (swap_nibbles != 0) {
        key1_index = xor_value % 7;
        key2_index = (xor_value % 12) + 2;
      }
      else {
        key1_index = (xor_value % 12) + 3;
        key2_index = xor_value % 7;
      }
      swap_nibbles ^= 1;
    }
    else {
      if (key1_index > 15) {
        swap_nibbles ^= 1;
        key1_index = 0;
      }
      if (key2_index > 12) {
        swap_nibbles ^= 1;
        key2_index = 0;
      }
    }

    return c;
  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public byte encryptByte(int c) {

    // do the encryption
    c ^= key1[key1_index];
    if (swap_nibbles != 0) {
      c = (c >> 4) | ((c & 0xf) << 4);
    }
    c ^= xor_value ^ key2[key2_index];

    // Update things for the next call
    key1_index += 1;
    key2_index += 1;
    if (key1_index > 15 && key2_index > 8) {
      xor_value += 2;
      xor_value &= 0x7f;
      if (swap_nibbles != 0) {
        key1_index = xor_value % 7;
        key2_index = (xor_value % 12) + 2;
      }
      else {
        key1_index = (xor_value % 12) + 3;
        key2_index = xor_value % 7;
      }
      swap_nibbles ^= 1;
    }
    else {
      if (key1_index > 15) {
        swap_nibbles ^= 1;
        key1_index = 0;
      }
      if (key2_index > 12) {
        swap_nibbles ^= 1;
        key2_index = 0;
      }
    }

    return (byte) c;
  }

}