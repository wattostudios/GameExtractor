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
Exports a file, where the data is XOR with a repeating key
**********************************************************************************************
**/
public class Exporter_ROT_RepeatingKey extends ExporterPlugin {

  static Exporter_ROT_RepeatingKey instance = new Exporter_ROT_RepeatingKey();

  static FileManipulator readSource;

  static long readLength = 0;

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public static Exporter_ROT_RepeatingKey getInstance() {
    return instance;
  }

  int[] rotKey = new int[0];

  int keyLength = 0;

  int currentKeyPos = 0;

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public Exporter_ROT_RepeatingKey() {
    setName("ROT Encrypted File (Repeating Key)");
  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public Exporter_ROT_RepeatingKey(int[] rotKey) {
    super();
    setName("ROT Encrypted File (Repeating Key)");
    this.rotKey = rotKey;
    this.keyLength = rotKey.length;
    this.currentKeyPos = 0;
  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public Exporter_ROT_RepeatingKey(int[] rotKey, int keyPos) {
    super();
    setName("ROT Encrypted File (Repeating Key)");
    this.rotKey = rotKey;
    this.keyLength = rotKey.length;
    this.currentKeyPos = keyPos;
    openAtCurrentKeyPos = true;
  }

  public int getCurrentKeyPos() {
    return currentKeyPos;
  }

  public void setCurrentKeyPos(int currentKeyPos) {
    this.currentKeyPos = currentKeyPos;
  }

  /** false will reset the currentKeyPos in open(), true will retain it **/
  boolean openAtCurrentKeyPos = false;

  public void startAtCurrentKeyPos(boolean openAtCurrentKeyPos) {
    this.openAtCurrentKeyPos = openAtCurrentKeyPos;
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

      if (!openAtCurrentKeyPos) {
        currentKeyPos = 0;
      }

      readLength = source.getLength();

      // try to get the whole file in a single go, if it isn't too large (set to 200KB otherwise)
      int bufferSize = (int) readLength;
      if (bufferSize > 204800) {
        bufferSize = 204800;
      }

      readSource = new FileManipulator(source.getSource(), false, bufferSize);
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

    if (!openAtCurrentKeyPos) {
      currentKeyPos = 0;
    }

    readLength = compLengthIn;

    // try to get the whole file in a single go, if it isn't too large (set to 200KB otherwise)
    int bufferSize = (int) readLength;
    if (bufferSize > 204800) {
      bufferSize = 204800;
    }

    readSource = fmIn;

  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  @Override
  public void pack(Resource source, FileManipulator destination) {
    try {

      ExporterPlugin exporter = source.getExporter();
      exporter.open(source);

      while (exporter.available()) {

        //destination.writeByte(exporter.read() + rotKey[currentKeyPos++]);
        int byteValue = exporter.read() + rotKey[currentKeyPos++];
        if (byteValue < 0) {
          byteValue = 256 + byteValue;
        }
        else if (byteValue >= 256) {
          byteValue -= 256;
        }
        //bytes[i] = (byte) byteValue;
        destination.writeByte(byteValue);

        if (currentKeyPos >= keyLength) {
          currentKeyPos = 0;
        }
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
      //int returnByte = (readSource.readByte() + rotKey[currentKeyPos++]);

      int byteValue = ByteConverter.unsign(readSource.readByte()) + rotKey[currentKeyPos++];
      if (byteValue < 0) {
        byteValue = 256 + byteValue;
      }
      else if (byteValue >= 256) {
        byteValue -= 256;
      }
      //bytes[i] = (byte) byteValue;

      if (currentKeyPos >= keyLength) {
        currentKeyPos = 0;
      }
      return byteValue;
    }
    catch (Throwable t) {
      return 0;
    }
  }

}