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

public class Exporter_Custom_MHTML_Base64 extends ExporterPlugin {

  static Exporter_Custom_MHTML_Base64 instance = new Exporter_Custom_MHTML_Base64();

  static int[] decodeBuffer = new int[3];
  static int decodeBufferPos = 3; // 3 so it fills the buffer on the first read()

  static FileManipulator readSource;
  static long readLength = 0;

  // the conversion table
  static int[] charToValue = null;

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public static Exporter_Custom_MHTML_Base64 getInstance() {
    return instance;
  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public Exporter_Custom_MHTML_Base64() {
    setName("Base64 MHTML");
  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  @Override
  public boolean available() {
    checkDecodeBuffer();
    return readLength > 0;
  }

  /**
   **********************************************************************************************
   * Builds the conversion table
   **********************************************************************************************
   **/
  public void buildConversionTable() {
    try {

      if (charToValue != null) {
        return;
      }

      char[] vc = new char[64];

      for (int i = 0; i <= 25; i++) {
        vc[i] = (char) ('A' + i);
      }

      // 26..51 -> 'a'..'z'
      for (int i = 0; i <= 25; i++) {
        vc[i + 26] = (char) ('a' + i);
      }

      // 52..61 -> '0'..'9'
      for (int i = 0; i <= 9; i++) {
        vc[i + 52] = (char) ('0' + i);
      }

      vc[62] = '+';
      vc[63] = '/';

      int[] cv = new int[256];

      for (int i = 0; i < 256; i++) {
        cv[i] = 0;  // default is to ignore
      }

      cv[10] = 254;
      cv[13] = 254;
      cv[61] = 255;

      for (int i = 0; i < 64; i++) {
        cv[vc[i]] = i;
      }

      charToValue = cv;
    }
    catch (Throwable t) {
      t.printStackTrace();
    }
  }

  /**
   **********************************************************************************************
   * Refills the decode buffer if necessary
   **********************************************************************************************
   **/
  public void checkDecodeBuffer() {
    try {
      if (decodeBufferPos >= 3) {
        // decode the next bytes (read 4 bytes --> convert to 3 bytes)

        int[] encodedBytes = new int[4];

        boolean padding = false;
        for (int i = 0; i < encodedBytes.length; i++) {
          if (padding) {
            encodedBytes[i] = 0;
          }
          else {
            int value = charToValue[readSource.readByte()];
            readLength--;

            // check for new line chars
            //while (value == 13 || value == 10){
            while (value == 254) {
              value = charToValue[readSource.readByte()];
            }

            //if (value == 61){
            if (value == 255) {
              // check for end of file marker (=)
              padding = true;

              encodedBytes[i] = 0;

              // set this so that available() stops at the = sign
              readLength = i;
            }
            else {
              encodedBytes[i] = value;
            }
          }
        }

        //int encodedData = encodedBytes[0] << 18 | encodedBytes[1] << 12 | encodedBytes[2] << 6 | encodedBytes[3];

        int encodedData = encodedBytes[0];

        encodedData <<= 6;
        encodedData |= encodedBytes[1];

        encodedData <<= 6;
        encodedData |= encodedBytes[2];

        encodedData <<= 6;
        encodedData |= encodedBytes[3];

        //decodeBuffer[2] = (byte)encodedData;
        //decodeBuffer[1] = (byte)encodedData >>> 8;
        //decodeBuffer[0] = (byte)encodedData >>> 16;

        decodeBuffer[2] = (byte) encodedData;
        encodedData >>>= 8;

        decodeBuffer[1] = (byte) encodedData;
        encodedData >>>= 8;

        decodeBuffer[0] = (byte) encodedData;

        decodeBufferPos = 0;

      }

    }
    catch (Throwable t) {
      t.printStackTrace();
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
      readSource = new FileManipulator(source.getSource(), false);
      readSource.seek(source.getOffset());
      readLength = source.getLength();

      decodeBufferPos = 3;

      buildConversionTable();
    }
    catch (Throwable t) {
    }
  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  @Override
  public void pack(Resource source, FileManipulator destination) {
    // TEMPORARY - This archive type cannot be saved anyway!
    Exporter_Default.getInstance().pack(source, destination);
  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  @Override
  public int read() {
    try {
      int value = decodeBuffer[decodeBufferPos];
      decodeBufferPos++;
      return value;
    }
    catch (Throwable t) {
      return 0;
    }
  }

}