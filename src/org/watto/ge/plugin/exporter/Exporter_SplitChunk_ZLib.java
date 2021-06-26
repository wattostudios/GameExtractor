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

import java.util.zip.InflaterInputStream;
import org.watto.datatype.Resource;
import org.watto.datatype.SplitChunkResource;
import org.watto.ge.plugin.ExporterPlugin;
import org.watto.io.FileManipulator;
import org.watto.io.stream.ManipulatorInputStream;

public class Exporter_SplitChunk_ZLib extends ExporterPlugin {

  static Exporter_SplitChunk_ZLib instance = new Exporter_SplitChunk_ZLib();

  static FileManipulator fm;
  static InflaterInputStream readSource;

  static long[] readLengths;
  static long[] readOffsets;
  static int readChunk = 0;

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public static Exporter_SplitChunk_ZLib getInstance() {
    return instance;
  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public Exporter_SplitChunk_ZLib() {
    setName("ZLib Compressed Chunks");
  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  @Override
  public boolean available() {
    try {
      if (readLengths[readChunk] <= 0) {
        readChunk++;
        if (readChunk < readOffsets.length) {
          readSource.close();
          fm.seek(readOffsets[readChunk]);
          readSource = new InflaterInputStream(new ManipulatorInputStream(fm));
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

      fm = new FileManipulator(source.getSource(), false);

      readLengths = source.getDecompressedLengths();
      readOffsets = source.getOffsets();

      fm.seek(readOffsets[0]);

      readSource = new InflaterInputStream(new ManipulatorInputStream(fm));

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
      return readSource.read();
    }
    catch (Throwable t) {
      return 0;
    }
  }

}