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

import org.watto.ErrorLogger;
import org.watto.datatype.Resource;
import org.watto.ge.plugin.ExporterPlugin;
import org.watto.io.FileManipulator;
import org.watto.io.converter.ByteArrayConverter;
import org.watto.io.converter.IntConverter;

/**
**********************************************************************************************

**********************************************************************************************
**/
public class Exporter_Custom_EBO extends ExporterPlugin {

  static Exporter_Custom_EBO instance = new Exporter_Custom_EBO();

  static FileManipulator readSource;

  static long readLength = 0;

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public static Exporter_Custom_EBO getInstance() {
    return instance;
  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public Exporter_Custom_EBO() {
    setName("EBO Encryption");
  }

  byte[] buffer = new byte[0];

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  @Override
  public boolean available() {
    if (readLength > 0) {
      if (bufferPos >= bufferSize) {
        // We have finished reading the buffer, need to decrypt the next block

        int blockSize = PAGESIZE;
        if (readLength < blockSize) {
          // last block - smaller
          blockSize = (int) readLength;
        }

        buffer = readSource.readBytes(blockSize);
        transform(buffer, blockSize);

        bufferPos = 0;
        bufferSize = blockSize;
      }
      return true;
    }
    else {
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

    key = null;
    salt1 = 0;
    salt2 = 0;
    buffer = null;
    bufferPos = 0;
    bufferSize = 0;
    readLength = 0;
  }

  int bufferPos = 0;

  int bufferSize = 0;

  /**
  **********************************************************************************************
  archiveLength is the length of the EBO file
  readLengthIn is the length of the data to decrypt (which is archiveLength - headerLength)
  **********************************************************************************************
  **/
  public void open(FileManipulator fm, long archiveLength, long readLengthIn) {
    try {
      readSource = fm;
      readLength = readLengthIn;

      bufferSize = PAGESIZE;
      bufferPos = bufferSize; // same as bufferSize, so that available() triggers the first decrypt to occur

      initDecrypt((int) archiveLength);

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

      bufferSize = PAGESIZE;
      bufferPos = bufferSize; // same as bufferSize, so that available() triggers the first decrypt to occur

      readSource = new FileManipulator(source.getSource(), false, bufferSize);
      readSource.seek(source.getOffset());

      initDecrypt((int) readLength);

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

      byte currentByte = buffer[bufferPos];
      bufferPos++;

      return currentByte;
    }
    catch (Throwable t) {
      return 0;
    }
  }

  public void initDecrypt(int fileLength) {
    initTransform(A3KeyInt, fileLength);
  }

  public void initEncrypt(int fileLength) {
    initTransform(A3KeyInt, fileLength);
  }

  private static int[] A3KeyInt = new int[] {
      1520625558,
      -1003046484,
      1575274816,
      1086363330
  };

  public void initTransform(int[] keyIn, int inputSize) {
    if (keyIn.length * 4 != KEYLENGTH) {
      ErrorLogger.log("[Exporter_Custom_EBO] Invalid key length: " + (keyIn.length * 4) + " != " + KEYLENGTH);
    }

    salt1 = (inputSize ^ inputSize << 5);
    salt2 = ~salt1;

    key = new int[4];
    System.arraycopy(keyIn, 0, key, 0, 4);

    blockPos = 0;
  }

  public void transform(byte[] buffer, int bufLen) {
    int[] array = new int[4];
    for (int i = 0; i < 4; i++) {
      array[i] = (key[i] ^ salt2 ^ blockPos);
    }
    int num = (blockPos ^ salt1) * 1043968403;
    float num2 = (float) ((double) (12345 - num & Integer.MAX_VALUE) * 4.6566129E-10 * 256.0 - 0.5 + 12582912.0);

    //int num3 = (int) num2; // *(int*)(&num2);
    byte[] floatBytes = ByteArrayConverter.convertLittle(num2);
    int num3 = IntConverter.convertLittle(floatBytes);

    num3 = (num3 & 8388607) - 4194048;
    num3 = Math.min(Math.max(256, num3), 511);

    byte[] arrayAsByte = new byte[KEYLENGTH];
    for (int i = 0, j = 0; i < 4; i++, j += 4) {
      byte[] intAsByte = ByteArrayConverter.convertLittle(array[i]);
      System.arraycopy(intAsByte, 0, arrayAsByte, j, 4);
    }

    ARC4Encode(buffer, bufLen, arrayAsByte, KEYLENGTH, num3);

    blockPos += bufLen;
  }

  public static int PAGESIZE = 4096;

  public static int KEYLENGTH = 16;

  public int[] key;

  public int salt1;

  public int salt2;

  public int blockPos;

  private static void swap(int[] array, int pos1, int pos2) {
    int num = array[pos1];
    array[pos1] = array[pos2];
    array[pos2] = num;
  }

  public static void ARC4Encode(byte[] buffer, int bufLen, byte[] key, int keyLength, int nSwaps) {
    int[] array = new int[256];
    int[] array2 = new int[256];
    int i;
    for (i = 0; i < 256; i++) {
      array2[i] = (int) key[i % keyLength];
      array[i] = i;
    }
    i = 0;
    int num = 0;
    while (i < 256) {
      num = (num + array[i] + array2[i] & 255);
      swap(array, i, num);
      i++;
    }
    int num2 = 0;
    int num3 = 0;
    for (i = 0; i < nSwaps; i++) {
      num2 = (num2 + 1 & 255);
      num3 = (num3 + array[num2] & 255);
      swap(array, num2, num3);
    }
    num2 = num3;
    num3 = nSwaps;
    for (i = 0; i < bufLen; i++) {
      num2 = (num2 + 1 & 255);
      num3 = (num3 + array[num2] & 255);
      swap(array, num2, num3);

      byte ptr = buffer[i];
      ptr ^= (byte) array[array[num2] + array[num3] & 255];
      buffer[i] = ptr;
    }
  }

}