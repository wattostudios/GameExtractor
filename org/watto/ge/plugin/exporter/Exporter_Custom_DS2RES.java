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

public class Exporter_Custom_DS2RES extends ExporterPlugin {

  static Exporter_Custom_DS2RES instance = new Exporter_Custom_DS2RES();

  static FileManipulator fm;

  static InflaterInputStream readSource;

  static long[] readLengths;

  static long[] decompSpacers;

  static long[] readOffsets;

  static int readChunk = 0;

  static byte[] betweenChunkBuffer = new byte[0];

  static int betweenChunkBufferPos = 16;

  /**
  **********************************************************************************************
  NOT USED - REPLACED BY A BlockVariableExportWraper IN PLUGIN DS2REG_DSG2TANK
  **********************************************************************************************
  **/
  public static Exporter_Custom_DS2RES getInstance() {
    return instance;
  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public Exporter_Custom_DS2RES() {
    setName("Dungeon Siege 2 ZLib Compressed Chunks");
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

          fm.seek(decompSpacers[readChunk - 1]);
          betweenChunkBuffer = fm.readBytes(16);
          betweenChunkBufferPos = 0;

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
      decompSpacers = source.getLengths();
      readOffsets = source.getOffsets();

      for (int i = 0; i < decompSpacers.length; i++) {
        decompSpacers[i] += readOffsets[i];
      }

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
      if (betweenChunkBufferPos < 16) {
        int curByte = betweenChunkBuffer[betweenChunkBufferPos];
        betweenChunkBufferPos++;
        return curByte;
      }

      readLengths[readChunk]--;
      return readSource.read();
    }
    catch (Throwable t) {
      return 0;
    }
  }

}