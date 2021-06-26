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
import com.ning.compress.lzf.impl.UnsafeChunkDecoder;

public class Exporter_LZF extends ExporterPlugin {

  static Exporter_LZF instance = new Exporter_LZF();

  static long readLength = 0;

  /** position in decompressed buffer **/
  static int chunkPos = 0;

  /** decompressed buffer length **/
  static int chunkLength = 0;

  /** decompressed buffer **/
  static byte[] chunkData = null;

  static int currentByte = 0;

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public static Exporter_LZF getInstance() {
    return instance;
  }

  FileManipulator fm;

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public Exporter_LZF() {
    setName("LZF Compression");
  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  @Override
  public boolean available() {
    try {
      if (readLength > 0) {
        return (chunkPos < chunkLength);
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

      chunkPos = 0;
      chunkLength = 0;
      chunkData = null;
    }
    catch (Throwable t) {
      chunkPos = 0;
      chunkLength = 0;
      chunkData = null;
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

      chunkPos = 0;
      chunkLength = (int) source.getDecompressedLength();
      chunkData = new byte[chunkLength];

      readLength = chunkLength;

      UnsafeChunkDecoder decoder = new UnsafeChunkDecoder();
      decoder.decodeChunkGE(compData, 0, chunkData, 0, chunkLength);// decomp the full file

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
      if (chunkPos < chunkLength) {
        // should always be true, provided you call available() before read();
        int currentByte = chunkData[chunkPos];
        chunkPos++;
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