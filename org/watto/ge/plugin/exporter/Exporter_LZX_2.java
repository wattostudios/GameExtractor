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

import java.nio.ByteBuffer;
import org.watto.ErrorLogger;
import org.watto.datatype.Resource;
import org.watto.ge.plugin.ExporterPlugin;
import org.watto.io.FileManipulator;
import com.github.antag99.aquarria.xnb.LzxDecoder;

public class Exporter_LZX_2 extends ExporterPlugin {

  static Exporter_LZX_2 instance = new Exporter_LZX_2();

  static long readLength = 0;

  static int currentByte = 0;

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public static Exporter_LZX_2 getInstance() {
    return instance;
  }

  byte[] decompData = null;

  int decompLength = 0;

  int decompPos = 0;

  FileManipulator fm;

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public Exporter_LZX_2() {
    setName("LZX Compression");
  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  @Override
  public boolean available() {
    try {
      if (readLength > 0) {
        return (decompPos < decompLength);
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
      fm.close();

      decompPos = 0;
      decompLength = 0;
      decompData = null;
    }
    catch (Throwable t) {
      decompPos = 0;
      decompLength = 0;
      decompData = null;
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

      // read in all the compressed data
      byte[] compData = fm.readBytes((int) source.getLength());

      decompPos = 0;
      decompLength = (int) source.getDecompressedLength();
      decompData = new byte[decompLength];

      readLength = decompLength;

      LzxDecoder decoder = new LzxDecoder();

      ByteBuffer compBuffer = ByteBuffer.wrap(compData);
      ByteBuffer decompBuffer = ByteBuffer.wrap(decompData);

      // decompress the full file
      decoder.decompress(compBuffer, compData.length, decompBuffer, decompLength);

    }
    catch (Throwable t) {
      ErrorLogger.log(t);
    }
  }

  /**
  **********************************************************************************************
  So we can easily call this from within a Viewer plugin
  **********************************************************************************************
  **/
  public void open(FileManipulator fmIn, int compLengthIn, int decompLengthIn) {
    try {
      fm = fmIn;

      // read in all the compressed data
      byte[] compData = fm.readBytes(compLengthIn);

      decompPos = 0;
      decompLength = decompLengthIn;
      decompData = new byte[decompLength];

      readLength = decompLength;

      LzxDecoder decoder = new LzxDecoder();

      ByteBuffer compBuffer = ByteBuffer.wrap(compData);
      ByteBuffer decompBuffer = ByteBuffer.wrap(decompData);

      // decompress the full file
      decoder.decompress(compBuffer, compData.length, decompBuffer, decompLength);

    }
    catch (Throwable t) {
      ErrorLogger.log(t);
    }
  }

  /**
  **********************************************************************************************
  Unsupported
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
      if (decompPos < decompLength) {
        // should always be true, provided you call available() before read();
        int currentByte = decompData[decompPos];
        decompPos++;
        readLength--;

        return currentByte;
      }
      return 0; // shouldn't get here
    }
    catch (Throwable t) {
      t.printStackTrace();
      readLength = 0;
      return 0;
    }
  }

}