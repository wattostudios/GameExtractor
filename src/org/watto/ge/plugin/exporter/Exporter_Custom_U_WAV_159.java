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

public class Exporter_Custom_U_WAV_159 extends ExporterPlugin {

  static Exporter_Custom_U_WAV_159 instance = new Exporter_Custom_U_WAV_159();

  static FileManipulator readSource;
  static long readLength = 0;

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public static Exporter_Custom_U_WAV_159 getInstance() {
    return instance;
  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public Exporter_Custom_U_WAV_159() {
    setName("Unreal v1.59 WAVE Audio");
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
  public String getDescription() {
    return "This exporter extracts the WAV audio from Unreal Engine 1.59 files when exporting\n\n" + super.getDescription();
  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  @Override
  @SuppressWarnings("unused")
  public void open(Resource source) {
    try {
      readSource = new FileManipulator(source.getSource(), false);
      readSource.seek(source.getOffset());

      // 1-5 - Sound Format Name (index to the name "WAV" in the names table)
      int nameIndex = readIndex(readSource);

      // 1-5 - File Type Index (index to "Sound" in the names table)
      int fileTypeIndex = readIndex(readSource);

      // 1-5 - Unknown (-32)
      int unknownIndex = readIndex(readSource);

      // 1-5 - Extra Field Identifier
      int extraFieldIdentifier = readIndex(readSource);

      if (extraFieldIdentifier != 0) {
        // 4 - Extra Field (150995155)
        readSource.skip(4);
      }
      else {
        // 1 - Unknown (9)
        readSource.skip(1);
      }

      // 4 - Index to the Next Resource
      readSource.skip(4);

      // 1-5 - Sound Length (File Length)
      int length = readIndex(readSource);

      // X - Sound Data (File Data)
      readLength = (int) (source.getLength() - (readSource.getOffset() - source.getOffset()));

    }
    catch (Throwable t) {
    }
  }

  /**
   **********************************************************************************************
   * // TEST - NOT DONE
   **********************************************************************************************
   **/
  @Override
  public void pack(Resource source, FileManipulator destination) {
    try {
      //long decompLength = source.getDecompressedLength();

      ExporterPlugin exporter = source.getExporter();
      exporter.open(source);

      //for (int i=0;i<decompLength;i++){
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
      return readSource.readByte();
    }
    catch (Throwable t) {
      return 0;
    }
  }

  /**
   **********************************************************************************************
   * Reads a single CompactIndex value from the given file
   **********************************************************************************************
   **/
  public int readIndex(FileManipulator fm) throws Exception {

    boolean[] bitData = new boolean[35];
    java.util.Arrays.fill(bitData, false);
    boolean[] byte1 = fm.readBits();

    int bytes = 1;

    boolean negative = false;
    if (byte1[0]) { // positive or negative
      negative = true;
    }

    System.arraycopy(byte1, 2, bitData, 29, 6);
    if (byte1[1]) { // next byte?
      // Read byte 2
      bytes = 2;

      boolean[] byte2 = fm.readBits();
      System.arraycopy(byte2, 1, bitData, 22, 7);
      if (byte2[0]) { // next byte?
        // Read byte 3
        bytes = 3;

        boolean[] byte3 = fm.readBits();
        System.arraycopy(byte3, 1, bitData, 15, 7);
        if (byte3[0]) { // next byte?
          // Read byte 4
          bytes = 4;

          boolean[] byte4 = fm.readBits();
          System.arraycopy(byte4, 1, bitData, 8, 7);
          if (byte4[0]) { // next byte?
            // Read byte 5 (last byte)
            bytes = 5;

            boolean[] byte5 = fm.readBits();
            System.arraycopy(byte5, 0, bitData, 0, 8);

          }

        }

      }

    }

    long number = 0;

    //calculate number
    if (bytes >= 5) {
      if (bitData[7]) {
        number += 134217728;
      }
      ;
      if (bitData[6]) {
        number += 268435456;
      }
      ;
      if (bitData[5]) {
        number += 536870912;
      }
      ;
      if (bitData[4]) {
        number += 1073741824;
      }
      ;
      if (bitData[3]) {
        number += 2147483648L;
      }
      ;
      if (bitData[2]) {
        number += 4294967296L;
      }
      ;
      if (bitData[1]) {
        number += 8589934592L;
      }
      ;
      if (bitData[0]) {
        number += 17179869184L;
      }
      ;
    }
    if (bytes >= 4) {
      if (bitData[14]) {
        number += 1048576;
      }
      ;
      if (bitData[13]) {
        number += 2097152;
      }
      ;
      if (bitData[12]) {
        number += 4194304;
      }
      ;
      if (bitData[11]) {
        number += 8388608;
      }
      ;
      if (bitData[10]) {
        number += 16777216;
      }
      ;
      if (bitData[9]) {
        number += 33554432;
      }
      ;
      if (bitData[8]) {
        number += 67108864;
      }
      ;
    }
    if (bytes >= 3) {
      if (bitData[21]) {
        number += 8192;
      }
      ;
      if (bitData[20]) {
        number += 16384;
      }
      ;
      if (bitData[19]) {
        number += 32768;
      }
      ;
      if (bitData[18]) {
        number += 65536;
      }
      ;
      if (bitData[17]) {
        number += 131072;
      }
      ;
      if (bitData[16]) {
        number += 262144;
      }
      ;
      if (bitData[15]) {
        number += 524288;
      }
      ;
    }
    if (bytes >= 2) {
      if (bitData[28]) {
        number += 64;
      }
      ;
      if (bitData[27]) {
        number += 128;
      }
      ;
      if (bitData[26]) {
        number += 256;
      }
      ;
      if (bitData[25]) {
        number += 512;
      }
      ;
      if (bitData[24]) {
        number += 1024;
      }
      ;
      if (bitData[23]) {
        number += 2048;
      }
      ;
      if (bitData[22]) {
        number += 4096;
      }
      ;
    }
    if (bytes >= 1) {
      if (bitData[34]) {
        number += 1;
      }
      ;
      if (bitData[33]) {
        number += 2;
      }
      ;
      if (bitData[32]) {
        number += 4;
      }
      ;
      if (bitData[31]) {
        number += 8;
      }
      ;
      if (bitData[30]) {
        number += 16;
      }
      ;
      if (bitData[29]) {
        number += 32;
      }
      ;
    }

    if (negative) {
      number = 0 - number;
    }

    return (int) number;

  }

}