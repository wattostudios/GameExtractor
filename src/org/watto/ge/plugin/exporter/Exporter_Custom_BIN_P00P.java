/*
 * Application:  Game Extractor
 * Author:       wattostudios
 * Website:      http://www.watto.org
 * Copyright:    Copyright (c) 2002-2024 wattostudios
 *
 * License Information:
 * This program is free software; you can redistribute it and/or modify it under the terms of the GNU General Public License
 * published by the Free Software Foundation; either version 2 of the License, or (at your option) any later versions. This
 * program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranties
 * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License at http://www.gnu.org for more
 * details. For further information on this application, refer to the authors' website.
 */

package org.watto.ge.plugin.exporter;

import java.io.InputStream;
import java.util.zip.Inflater;
import java.util.zip.InflaterInputStream;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import org.tukaani.xz.SingleXZInputStream;
import org.watto.ErrorLogger;
import org.watto.datatype.Resource;
import org.watto.ge.plugin.ExporterPlugin;
import org.watto.io.FileManipulator;
import org.watto.io.buffer.ByteBuffer;
import org.watto.io.converter.ByteArrayConverter;
import org.watto.io.stream.ManipulatorInputStream;

/**
**********************************************************************************************
Ref: https://github.com/vdsk/ravendawn-decrypt/blob/main/FileDecryptor.py
**********************************************************************************************
**/
public class Exporter_Custom_BIN_P00P extends ExporterPlugin {

  static Exporter_Custom_BIN_P00P instance = new Exporter_Custom_BIN_P00P();

  static InputStream readSource;
  static long readLength = 0;
  static int currentByte = 0;

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public static Exporter_Custom_BIN_P00P getInstance() {
    return instance;
  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public Exporter_Custom_BIN_P00P() {
    setName("BIN P00P Compression and Encryption (Deflate + Decryption + XZ-LZMA)");
  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  @Override
  public boolean available() {
    try {

      //if (readLength > 0) {
      currentByte = readSource.read();
      //  readLength--;
      if (currentByte >= 0) {
        return true;
        //  }
      }

      return false;
    }
    catch (Throwable t) {
      ErrorLogger.log(t);

      return false;
    }
  }

  FileManipulator fm;

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  @Override
  public void close() {
    try {
      fm.close();
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
      // try to get the whole file in a single go, if it isn't too large (set to 200KB otherwise)
      readLength = source.getLength();
      int bufferSize = (int) readLength;
      if (bufferSize > 204800) {
        bufferSize = 204800;
      }

      fm = new FileManipulator(source.getSource(), false, bufferSize);
      fm.seek(source.getOffset());

      //
      // First we need to use Deflate to extract the file from the ZIP
      //
      readSource = new InflaterInputStream(new ManipulatorInputStream(fm), new Inflater(true));
      readLength = source.getDecompressedLength();

      byte[] outBytes = new byte[(int) readLength];
      for (int i = 0; i < readLength; i++) {
        if (readSource.available() > 0) {
          outBytes[i] = (byte) readSource.read();
        }
        else {
          break; // error during deflate
        }
      }

      readSource.close();
      fm.close();
      fm = new FileManipulator(new ByteBuffer(outBytes));

      //
      // Now we need to decrypt the file
      //
      //fm = new FileManipulator(source.getSource(), false, bufferSize);
      //fm.seek(source.getOffset());

      byte[] headerBytes = fm.readBytes(16);

      String file_name = source.getFilenameWithExtension();

      int xor_key2 = get_xor_key(headerBytes);
      byte[] key = get_key(get_xor_file(file_name), xor_key2);
      byte[] iv = get_iv(file_name, xor_key2);

      byte[] encryptedBytes = fm.readBytes((int) readLength - 16);

      outBytes = null; // CLEANUP OLD ARRAY THAT'S NO LONGER NEEDED

      byte[] key32 = new byte[32];
      System.arraycopy(key, 0, key32, 0, 32);

      byte[] decryptedBytes = evp_decrypt(key32, encryptedBytes, iv);

      encryptedBytes = null; // CLEANUP OLD ARRAY THAT'S NO LONGER NEEDED

      fm.close();
      fm = new FileManipulator(new ByteBuffer(decryptedBytes));

      //
      // Now we need to decompress (XZ-LZMA) the file
      //
      readSource = new SingleXZInputStream(new ManipulatorInputStream(fm));

    }
    catch (Throwable t) {
    }
  }

  public byte[] evp_decrypt(byte[] key, byte[] ciphertext, byte[] iv) {

    try {
      Cipher cipher = Cipher.getInstance("AES/CBC/NoPadding");
      SecretKeySpec keySpec = new SecretKeySpec(key, "AES");
      cipher.init(Cipher.DECRYPT_MODE, keySpec, new IvParameterSpec(iv));

      byte[] plaintext = cipher.update(ciphertext);
      return plaintext;
    }
    catch (Throwable t) {
      ErrorLogger.log(t);
    }
    return new byte[0];
  }

  public byte get_bytes(int num) {
    byte[] x = ByteArrayConverter.convertBig(num); //x = num.to_bytes(0x10, 'big');
    int length = x.length;
    if (length > 0) {
      return x[length - 1]; // return x[-1];
    }
    return 0;
  }

  public byte[] get_xor_file(String filename) {
    //byte[] a = bytearray(filename, "ascii");
    byte[] a = filename.getBytes();
    int filenameLength = a.length;

    for (int i = 0; i < filenameLength; i++) {//for i in range(0, len(filename)):
      byte v28 = (byte) ((a[i] ^ filenameLength) + 0x69);
      a[i] = v28;
    }
    return a;
  }

  public int get_xor_key(byte[] headerBytes) {
    return get_bytes(headerBytes[5] ^ 0x1337);
  }

  public byte[] get_key(byte[] xored, int xor_key) {

    byte[] key = new byte[64];
    int xoredLength = xored.length;

    for (int i = 0; i < 64; i += 8) {//for i in range(0, 0x40, 8):
      int curr = xored[(i + 105) % xoredLength];
      key[i] = get_bytes(curr);

      curr = (key[i] ^ xor_key) + 105;
      key[i] = get_bytes(curr);

      curr = xored[(i + 106) % xoredLength];
      key[i + 1] = get_bytes(curr);

      curr = (key[i + 1] ^ xor_key) + 105;
      key[i + 1] = get_bytes(curr);

      curr = xored[(i + 107) % xoredLength];
      key[i + 2] = get_bytes(curr);

      curr = (key[i + 2] ^ xor_key) + 105;
      key[i + 2] = get_bytes(curr);

      curr = xored[(i + 108) % xoredLength];
      key[i + 3] = get_bytes(curr);

      curr = (key[i + 3] ^ xor_key) + 105;
      key[i + 3] = get_bytes(curr);

      curr = xored[(i + 109) % xoredLength];
      key[i + 4] = get_bytes(curr);

      curr = (key[i + 4] ^ xor_key) + 105;
      key[i + 4] = get_bytes(curr);

      curr = xored[(i + 110) % xoredLength];
      key[i + 5] = get_bytes(curr);

      curr = (key[i + 5] ^ xor_key) + 105;
      key[i + 5] = get_bytes(curr);

      curr = xored[(i + 111) % xoredLength];
      key[i + 6] = get_bytes(curr);

      curr = (key[i + 6] ^ xor_key) + 105;
      key[i + 6] = get_bytes(curr);

      curr = xored[(i + 112) % xoredLength];
      key[i + 7] = get_bytes(curr);

      curr = (key[i + 7] ^ xor_key) + 105;
      key[i + 7] = get_bytes(curr);
    }

    return key;
  }

  public byte[] get_iv(String filename, int xor_key) {

    int filenameLength = filename.length();

    byte[] iv = new byte[16];
    for (int j = 0; j < 16; j += 8) { //for j in range(0, 0x10, 8):

      int v68 = 0;
      if ((j & 1) == 0) {
        v68 = xor_key;
      }

      int v69 = 0;
      if (j == 3 * (j / 3)) {
        v69 = 105;
      }

      iv[j] = get_bytes(v68 + v69);

      int v71 = filenameLength + (iv[j] ^ xor_key);

      iv[j] = get_bytes(v71);

      int v74 = j - 1;
      int v75 = 0;

      if (((j - 1) & 1) == 0) {
        v75 = xor_key;
      }
      int v76 = 0;

      if (j - 3 * ((v74 + 2) / 3) == -1) {
        v76 = 105;
      }
      iv[j + 1] = get_bytes(v75 + v76);

      int v78 = filenameLength + (iv[j + 1] ^ xor_key);
      iv[j + 1] = get_bytes(v78);

      int v81 = 0;

      if (j - 3 * ((v74 + 3) / 3) == -2) {
        v81 = 105;
      }
      iv[j + 2] = get_bytes(v68 + v81);

      int v83 = filenameLength + (iv[j + 2] ^ xor_key);
      iv[j + 2] = get_bytes(v83);

      int v86 = 0;

      if ((j + 2 * (1 - (v74 + 4) / 3) + 1 - (v74 + 4) / 3) == 0) { //if ( not(j + 2 * (1 - (v74 + 4) / 3) + 1 - (v74 + 4) / 3) ) {
        v86 = 105;
      }
      iv[j + 3] = get_bytes(v75 + v86);

      int v88 = filenameLength + (iv[j + 3] ^ xor_key);
      iv[j + 3] = get_bytes(v88);

      int v91 = 0;
      if (j - 3 * ((v74 + 5) / 3) == -4) {
        v91 = 105;
      }
      iv[j + 4] = get_bytes(v68 + v91);

      int v93 = filenameLength + (iv[j + 4] ^ xor_key);
      iv[j + 4] = get_bytes(v93);

      int v96 = 0;
      if (j - 3 * ((v74 + 6) / 3) == -5) {
        v96 = 105;
      }
      iv[j + 5] = get_bytes(v75 + v96);

      int v98 = filenameLength + (iv[j + 5] ^ xor_key);
      iv[j + 5] = get_bytes(v98);

      int v101 = 0;

      if ((-3 * ((v74 + 7) / 3) + j + 6) == 0) { // if ( not(-3 * ((v74 + 7) / 3) + j + 6) ) {
        v101 = 105;
      }

      iv[j + 6] = get_bytes(v68 + v101);

      int v103 = filenameLength + (iv[j + 6] ^ xor_key);
      iv[j + 6] = get_bytes(v103);

      int v106 = 0;
      if (j - 3 * ((v74 + 8) / 3) == -7) {
        v106 = 105;
      }
      iv[j + 7] = get_bytes(v75 + v106);

      int v108 = filenameLength + (iv[j + 7] ^ xor_key);
      iv[j + 7] = get_bytes(v108);
    }

    return iv;
  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  @Override
  public void pack(Resource source, FileManipulator destination) {
    // not implemented
  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  @Override
  public int read() {
    try {
      // NOTE: The actual reading of the byte is done in available()
      return currentByte;
    }
    catch (Throwable t) {
      t.printStackTrace();
      readLength = 0;
      return 0;
    }
  }

}