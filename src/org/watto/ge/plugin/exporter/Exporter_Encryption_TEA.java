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
import org.watto.ge.plugin.ArchivePlugin;
import org.watto.ge.plugin.ExporterPlugin;
import org.watto.io.FileManipulator;
import org.watto.io.converter.ByteArrayConverter;

public class Exporter_Encryption_TEA extends ExporterPlugin {

  static Exporter_Encryption_TEA instance = new Exporter_Encryption_TEA();

  static long readLength = 0;

  byte[] buffer = new byte[0];

  int bufferLength = 0;

  int bufferPos = 0;

  byte[] key = new byte[0];

  /**
  **********************************************************************************************
  Decrypts using the TEA algorithm
  **********************************************************************************************
  **/
  public static Exporter_Encryption_TEA getInstance() {
    return instance;
  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public Exporter_Encryption_TEA() {
    setName("TEA Decryption");
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
  public Exporter_Encryption_TEA(byte[] key) {
    setName("TEA Decryption");
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
      //buffer = fmIn.readBytes(decompLengthIn);
      bufferLength = decompLengthIn;
      bufferPos = 0;

      // init the key

      if (key == null || key.length < 16) {
        return;
      }

      int[] splitKey = new int[4];
      for (int off = 0, i = 0; i < 4; i++) {
        splitKey[i] = ((key[off++] & 0xff)) |
            ((key[off++] & 0xff) << 8) |
            ((key[off++] & 0xff) << 16) |
            ((key[off++] & 0xff) << 24);
      }

      // build the buffer of int[];
      int bufLength = decompLengthIn;
      bufLength += ArchivePlugin.calculatePadding(bufLength, 4); // decrypts in blocks of 4 bytes
      bufLength /= 4;

      bufLength += ArchivePlugin.calculatePadding(bufLength, 2); // decrypts 2 blocks at a time

      int[] buf = new int[bufLength];
      for (int i = 0; i < bufLength; i++) {
        buf[i] = fmIn.readInt();
      }

      // decrypt it
      for (int i = 0; i < bufLength; i += 2) {
        decrypt(buf, i, splitKey);
      }

      // Split the int[] into byte[]
      int bufLengthWithPadding = (bufLength * 4);//decompLengthIn;
      bufLengthWithPadding += ArchivePlugin.calculatePadding(bufLengthWithPadding, 4);

      buffer = new byte[bufLengthWithPadding];

      for (int i = 0, j = 0; i < bufLength; i++, j += 4) {
        byte[] bufBlock = ByteArrayConverter.convertLittle(buf[i]);
        buffer[j] = bufBlock[0];
        buffer[j + 1] = bufBlock[1];
        buffer[j + 2] = bufBlock[2];
        buffer[j + 3] = bufBlock[3];
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

  public static void decrypt(int[] buf, int offset, int[] key) {
    int v0 = buf[offset + 0];
    int v1 = buf[offset + 1];

    int sum = 0xC6EF3720; // 32 rounds * delta
    int delta = 0x9E3779B9;

    for (int i = 0; i < 32; i++) {
      v1 -= ((v0 << 4) + key[2]) ^ (v0 + sum) ^ ((v0 >>> 5) + key[3]);
      v0 -= ((v1 << 4) + key[0]) ^ (v1 + sum) ^ ((v1 >>> 5) + key[1]);
      sum -= delta;
    }

    // return new int[] { v0, v1 };
    buf[offset + 0] = v0;
    buf[offset + 1] = v1;
  }

  /*
  private final static int SUGAR = 0x9E3779B9;
  private final static int UNSUGAR = 0xC6EF3720;
  
  private int[] splitKey = new int[4];
  
  
  public byte[] decrypt(byte[] crypt) {
  
    //assert crypt.length % 4 == 0;
    //assert (crypt.length / 4) % 2 == 1;
  
    int bufferSize = crypt.length;
    // needs to be a multiple of 4
    bufferSize += ArchivePlugin.calculatePadding(bufferSize, 4);
  
    bufferSize /= 4;
  
    int[] buffer = new int[bufferSize];
    pack(crypt, buffer);
    unbrew(buffer);
    return unpack(buffer);
  }
  
  void unbrew(int[] buf) {
    try {
      //assert buf.length % 2 == 1;
  
      int i, v0, v1, sum, n;
      int i = 0;
      while (i < buf.length) {
        n = 32;
        v0 = buf[i];
        v1 = buf[i + 1];
        sum = UNSUGAR;
        while (n-- > 0) {
          v1 -= ((v0 << 4) + splitKey[2] ^ v0) + (sum ^ (v0 >>> 5)) + splitKey[3];
          v0 -= ((v1 << 4) + splitKey[0] ^ v1) + (sum ^ (v1 >>> 5)) + splitKey[1];
          sum -= SUGAR;
        }
        buf[i] = v0;
        buf[i + 1] = v1;
        i += 2;
      }
    }
    catch (Throwable t) {
      ErrorLogger.log(t);
    }
  }
  
  void pack(byte[] src, int[] dest) {
    try {
  
      int i = 0, shift = 24;
      int j = 0;
  
      dest[j] = 0;
      while (i < src.length) {
        dest[j] |= ((src[i] & 0xff) << shift);
        if (shift == 0) {
          shift = 24;
          j++;
          if (j < dest.length)
            dest[j] = 0;
        }
        else {
          shift -= 8;
        }
        i++;
      }
    }
    catch (Throwable t) {
      ErrorLogger.log(t);
    }
  }
  
  byte[] unpack(int[] src) {
    try {
      int destLength = src.length * 4;
      byte[] dest = new byte[destLength];
  
      int i = 0;
      int count = 0;
      for (int j = 0; j < destLength; j++) {
        dest[j] = (byte) ((src[i] >> (24 - (8 * count))) & 0xff);
        count++;
        if (count == 4) {
          count = 0;
          i++;
        }
      }
      return dest;
    }
    catch (Throwable t) {
      ErrorLogger.log(t);
    }
    return null;
  }
  */

}