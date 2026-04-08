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

import org.watto.datatype.Resource;
import org.watto.ge.plugin.ExporterPlugin;
import org.watto.io.FileManipulator;

/**
**********************************************************************************************
Where each byte is mapped to a different value
**********************************************************************************************
**/
public class Exporter_ByteMapping extends ExporterPlugin {

  static Exporter_ByteMapping instance = new Exporter_ByteMapping();

  static FileManipulator readSource;

  static long readLength = 0;

  int[] byteMap = null;

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public static Exporter_ByteMapping getInstance() {
    return instance;
  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public Exporter_ByteMapping() {
    setName("Byte Mapping");

    // Default bytemap 
    byteMap = new int[256];
    for (int i = 0; i < 256; i++) {
      byteMap[i] = i;
    }
  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public Exporter_ByteMapping(int[] byteMapIn) {
    setName("Byte Mapping");
    this.byteMap = byteMapIn;
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
  So we can easily call this from within a Viewer plugin
  **********************************************************************************************
  **/
  public void open(FileManipulator fmIn, int compLengthIn, int decompLengthIn) {
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

      int currentByte = readSource.readByte();

      if (currentByte < 0) {
        currentByte = (short) (256 + (short) currentByte);
      }

      return byteMap[currentByte];
    }
    catch (Throwable t) {
      return 0;
    }
  }

}