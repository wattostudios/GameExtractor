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

import org.openteufel.file.mpq.explode.Exploder;
import org.watto.datatype.Resource;
import org.watto.ge.plugin.ExporterPlugin;
import org.watto.io.FileManipulator;

public class Exporter_Explode extends ExporterPlugin {

  static Exporter_Explode instance = new Exporter_Explode();

  static int decompLength = 0;

  static int currentByte = 0;

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public static Exporter_Explode getInstance() {
    return instance;
  }

  byte[] buffer = new byte[0];

  int bufferPos = 0;

  FileManipulator fm;

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public Exporter_Explode() {
    setName("ZIP Explode Compression");
  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  @Override
  public boolean available() {
    try {

      if (bufferPos < decompLength) {
        currentByte = buffer[bufferPos];
        bufferPos++;
        return true;
      }

      return false;
    }
    catch (Throwable t) {
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
      fm.close();
      buffer = new byte[0];
    }
    catch (Throwable t) {
      buffer = new byte[0];
    }
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

      decompLength = (int) source.getDecompressedLength();
      int compLength = (int) source.getLength();

      byte[] compData = fm.readBytes(compLength);
      buffer = new byte[decompLength];
      bufferPos = 0;

      Exploder.pkexplode(compData, buffer);

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

      byte[] compData = fmIn.readBytes(compLengthIn);
      buffer = new byte[decompLengthIn];
      bufferPos = 0;

      Exploder.pkexplode(compData, buffer);

    }
    catch (Throwable t) {
    }
  }

  /**
  **********************************************************************************************
  UNSUPPORTED
  **********************************************************************************************
  **/
  @Override
  public void pack(Resource source, FileManipulator destination) {
    // UNSUPPORTED
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
      decompLength = 0;
      return 0;
    }
  }

}