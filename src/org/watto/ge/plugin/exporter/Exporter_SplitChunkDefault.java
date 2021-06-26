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
import org.watto.datatype.SplitChunkResource;
import org.watto.ge.plugin.ExporterPlugin;
import org.watto.io.FileManipulator;

public class Exporter_SplitChunkDefault extends ExporterPlugin {

  static Exporter_SplitChunkDefault instance = new Exporter_SplitChunkDefault();

  static FileManipulator readSource;
  static long[] readLengths;
  static long[] readOffsets;
  static int readChunk = 0;

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public static Exporter_SplitChunkDefault getInstance() {
    return instance;
  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public Exporter_SplitChunkDefault() {
    setName("Uncompressed Chunks");
  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  @Override
  public boolean available() {
    try {
      if (readLengths[readChunk] == 0) {
        readChunk++;
        if (readChunk < readOffsets.length) {
          //System.out.println("Reading " + readLengths[readChunk] + " from offset " + readOffsets[readChunk]);
          readSource.seek(readOffsets[readChunk]);
        }
      }
    }
    catch (Throwable t) {
    }

    return (readChunk < readOffsets.length);
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

      readLengths = null;
      readOffsets = null;
      readChunk = 0;
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
  public void open(Resource src) {
    try {
      SplitChunkResource source = (SplitChunkResource) src;

      readSource = new FileManipulator(source.getSource(), false);

      readLengths = source.getLengths();
      readOffsets = source.getOffsets();

      readSource.seek(readOffsets[0]);

      readChunk = 0;
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
    // This can't be done using this method, so we will run the default pack() method instead
    Exporter_Default.getInstance().pack(source, destination);
  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  @Override
  public int read() {
    try {
      readLengths[readChunk]--;
      return readSource.readByte();
    }
    catch (Throwable t) {
      return 0;
    }
  }

}