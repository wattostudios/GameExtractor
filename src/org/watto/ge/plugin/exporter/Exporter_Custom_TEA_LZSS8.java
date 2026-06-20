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
import org.watto.io.converter.ByteConverter;

public class Exporter_Custom_TEA_LZSS8 extends ExporterPlugin {

  static Exporter_Custom_TEA_LZSS8 instance = new Exporter_Custom_TEA_LZSS8();

  static long readLength = 0;

  /**
  **********************************************************************************************
  Performs TEA decryption, then LZSS8 decompression
  **********************************************************************************************
  **/
  public static Exporter_Custom_TEA_LZSS8 getInstance() {
    return instance;
  }

  byte[] readBuffer = new byte[0];

  int readBufferPos = 0;

  int readBufferLength = 0;

  FileManipulator fm;

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public Exporter_Custom_TEA_LZSS8() {
    setName("TEA Encryption + LZSS8 Compression");
  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public Exporter_Custom_TEA_LZSS8(byte[] key) {
    setName("TEA Encryption + LZSS8 Compression");
    this.key = key;
  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  @Override
  public boolean available() {
    try {
      return (readBufferPos < readBufferLength);
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
      readBuffer = new byte[0];
      readBufferPos = 0;
      readBufferLength = 0;

      fm.close();
    }
    catch (Throwable t) {
    }
  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  byte[] decompress(byte[] i, int usize) {

    int win_size = 0x1000;
    int threshold = 3;
    int maxm = 18;

    byte[] ob = new byte[usize];
    byte[] ring = new byte[win_size];
    int p = 0;
    int rp = win_size - maxm;
    int f = 0;

    int inSize = i.length;
    int outPos = 0;

    while (outPos < usize && p < inSize) {
      if ((f & 0xFF00) == 0) {//if ((f & 0xFF00) != 0xFF00) {
        if (p >= inSize) {
          break;
        }
        f = ByteConverter.unsign(i[p]) | 0x8000;
        p += 1;
      }

      if ((f & 1) == 1) {
        if (p >= inSize) {
          break;
        }
        int c = ByteConverter.unsign(i[p]);
        p += 1;

        ob[outPos] = (byte) c; // ob.append(c);
        outPos++;

        ring[rp % win_size] = (byte) c;
        rp += 1;
      }
      else {
        if (p + 1 >= inSize) {
          break;
        }

        int b1 = ByteConverter.unsign(i[p]);
        p += 1;
        int b2 = ByteConverter.unsign(i[p]);
        p += 1;

        int idx = ((b2 & 0xF0) << 4) | b1;
        int count = ((b2 & 0x0F) + threshold);

        for (int ix = 0; ix < count; ix++) {
          if (outPos >= usize) {
            break;
          }

          //c = ring[(idx + ix) % win_size];
          byte c = ring[(idx + ix) % win_size];

          ob[outPos] = (byte) c; // ob.append(c);
          outPos++;

          ring[rp % win_size] = (byte) c;
          rp += 1;
        }
      }

      f >>= 1;
    }

    return ob;

  }

  byte[] key = new byte[0];

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
  So we can easily call this from within a Viewer plugin
  **********************************************************************************************
  **/
  public void open(FileManipulator fmIn, int compLengthIn, int decompLengthIn) {
    try {
      fm = fmIn;

      readLength = decompLengthIn;

      readBufferLength = (int) readLength;
      readBufferPos = 0;

      // TEA Decryption first
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
      int bufLength = compLengthIn;
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
      int bufLengthWithPadding = (bufLength * 4);
      bufLengthWithPadding += ArchivePlugin.calculatePadding(bufLengthWithPadding, 4);

      readBuffer = new byte[bufLengthWithPadding];

      for (int i = 0, j = 0; i < bufLength; i++, j += 4) {
        byte[] bufBlock = ByteArrayConverter.convertLittle(buf[i]);
        readBuffer[j] = bufBlock[0];
        readBuffer[j + 1] = bufBlock[1];
        readBuffer[j + 2] = bufBlock[2];
        readBuffer[j + 3] = bufBlock[3];
      }

      // LZSS8 Decompression second
      readBuffer = decompress(readBuffer, decompLengthIn);

    }
    catch (Throwable t) {
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

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  @Override
  public void open(Resource source) {
    try {
      fm = new FileManipulator(source.getSource(), false);
      fm.seek(source.getOffset());

      //int decompLength = (int) source.getDecompressedLength();
      int compLength = (int) source.getLength();

      readLength = source.getDecompressedLength();
      readBufferLength = (int) readLength;
      readBufferPos = 0;

      // TEA Decryption first
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
      int bufLength = (int) compLength;
      bufLength += ArchivePlugin.calculatePadding(bufLength, 4); // decrypts in blocks of 4 bytes
      bufLength /= 4;

      bufLength += ArchivePlugin.calculatePadding(bufLength, 2); // decrypts 2 blocks at a time

      int[] buf = new int[bufLength];
      for (int i = 0; i < bufLength; i++) {
        buf[i] = fm.readInt();
      }

      // decrypt it
      for (int i = 0; i < bufLength; i += 2) {
        decrypt(buf, i, splitKey);
      }

      // Split the int[] into byte[]
      int bufLengthWithPadding = (int) (bufLength * 4);
      bufLengthWithPadding += ArchivePlugin.calculatePadding(bufLengthWithPadding, 4);

      readBuffer = new byte[bufLengthWithPadding];

      for (int i = 0, j = 0; i < bufLength; i++, j += 4) {
        byte[] bufBlock = ByteArrayConverter.convertLittle(buf[i]);
        readBuffer[j] = bufBlock[0];
        readBuffer[j + 1] = bufBlock[1];
        readBuffer[j + 2] = bufBlock[2];
        readBuffer[j + 3] = bufBlock[3];
      }

      //readLength = compLength;
      //readBufferLength = compLength;
      // LZSS8 Decompression second
      readBuffer = decompress(readBuffer, (int) readLength);

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
    // NOT IMPLEMENTED
  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  @Override
  public int read() {
    try {
      int currentByte = readBuffer[readBufferPos];
      readBufferPos++;
      return currentByte;
    }
    catch (Throwable t) {
      t.printStackTrace();
      readLength = 0;
      return 0;
    }
  }

}