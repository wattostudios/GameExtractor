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

/**
**********************************************************************************************
Exports a file, where the data is XOR with a given value
**********************************************************************************************
**/
public class Exporter_XOR extends ExporterPlugin {

  static Exporter_XOR instance = new Exporter_XOR();

  static FileManipulator readSource;
  static long readLength = 0;

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public static Exporter_XOR getInstance() {
    return instance;
  }

  int xorValue = 0;

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public Exporter_XOR() {
    setName("XOR Encrypted File");
  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public Exporter_XOR(int xorValue) {
    super();
    this.xorValue = xorValue;
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
  
  **********************************************************************************************
  **/
  @Override
  public void pack(Resource source, FileManipulator destination) {
    try {

      ExporterPlugin exporter = source.getExporter();
      exporter.open(source);

      while (exporter.available()) {
        destination.writeByte(exporter.read() ^ xorValue);
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
      return (readSource.readByte() ^ xorValue);
    }
    catch (Throwable t) {
      return 0;
    }
  }

}