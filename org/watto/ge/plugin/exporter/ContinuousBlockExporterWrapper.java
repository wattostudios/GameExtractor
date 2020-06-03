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

import java.io.File;
import org.watto.ErrorLogger;
import org.watto.datatype.Resource;
import org.watto.ge.plugin.ExporterPlugin;
import org.watto.io.FileManipulator;
import org.watto.io.buffer.ByteBuffer;

public class ContinuousBlockExporterWrapper extends ExporterPlugin {

  File sourceFile = null;

  /** the exporter that will do all the actual work **/
  ExporterPlugin exporter = null;

  /** the offset to each compressed block **/
  long[] blockOffsets;

  /** the length of each compressed block **/
  long[] blockLengths;

  /** the decompressed length of each compressed block **/
  long[] decompLengths;

  /**
  **********************************************************************************************
  As an example, a Zlib compressed file of size 250kb, but the compressed data is stored in
  separate blocks of 50kb. You need to join all the blocks together, then decompress it.
  **********************************************************************************************
  **/
  public ContinuousBlockExporterWrapper() {
    setName("Wrapper for exporting a file compressed in blocks, where each block continues on from the previous, rather than being a separate compressed block");
  }

  /**
  **********************************************************************************************

  **********************************************************************************************
  **/
  public ContinuousBlockExporterWrapper(ExporterPlugin exporter, long[] blockOffsets, long[] blockLengths, long[] decompLengths) {
    this.exporter = exporter;
    this.blockOffsets = blockOffsets;
    this.blockLengths = blockLengths;
    this.decompLengths = decompLengths;

    if (exporter != null) {
      setName("Continuous Block Compression of type " + exporter.getName());
    }
  }

  /**
  **********************************************************************************************

  **********************************************************************************************
  **/
  @Override
  public boolean available() {
    return exporter.available();
  }

  /**
  **********************************************************************************************

  **********************************************************************************************
  **/
  @Override
  public void close() {
    exporter.close();
  }

  public long[] getBlockLengths() {
    return blockLengths;
  }

  public long[] getBlockOffsets() {
    return blockOffsets;
  }

  public long[] getDecompLengths() {
    return decompLengths;
  }

  /**
  **********************************************************************************************

  **********************************************************************************************
  **/
  @Override
  public void open(Resource source) {
    try {

      // get the source file - we already know everything else
      sourceFile = source.getSource();

      // 1. Join all the blocks together into a single stream
      int totalSize = (int) source.getLength();
      byte[] joinedBytes = new byte[totalSize];

      FileManipulator fm = new FileManipulator(sourceFile, false, (int) blockLengths[0]);

      int numBlocks = blockOffsets.length;
      int joinPosition = 0;
      for (int i = 0; i < numBlocks; i++) {
        fm.seek(blockOffsets[i]);
        int numBytes = (int) blockLengths[i];
        byte[] blockBytes = fm.readBytes(numBytes);
        System.arraycopy(blockBytes, 0, joinedBytes, joinPosition, numBytes);
        joinPosition += numBytes;
      }

      fm.close();

      // now open the bytes for decompression
      fm = new FileManipulator(new ByteBuffer(joinedBytes));

      if (exporter instanceof Exporter_ZLib_CompressedSizeOnly) {
        ((Exporter_ZLib_CompressedSizeOnly) exporter).open(fm, totalSize, totalSize);
      }
      else if (exporter instanceof Exporter_ZLib) {
        int totalDecompSize = (int) source.getDecompressedLength();
        ((Exporter_ZLib) exporter).open(fm, totalSize, totalDecompSize);
      }
      else {
        ErrorLogger.log("[ContinuousBlockExporterWrapper]: Unsupported Compression Type");
      }

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
      return exporter.read(); // available() already handles the transition between blocks
    }
    catch (Throwable t) {
      return 0;
    }
  }

  public void setBlockLengths(long[] blockLengths) {
    this.blockLengths = blockLengths;
  }

  public void setBlockOffsets(long[] blockOffsets) {
    this.blockOffsets = blockOffsets;
  }

  public void setDecompLengths(long[] decompLengths) {
    this.decompLengths = decompLengths;
  }

}