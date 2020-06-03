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
import org.watto.datatype.Resource;
import org.watto.ge.plugin.ExporterPlugin;
import org.watto.io.FileManipulator;

public class BlockExporterWrapper extends ExporterPlugin {

  File sourceFile = null;

  /** the exporter that will do all the actual work **/
  ExporterPlugin exporter = null;

  /** the offset to each compressed block **/
  long[] blockOffsets;

  /** the length of each compressed block **/
  long[] blockLengths;

  /** the decompressed length of each compressed block **/
  long[] decompLengths;

  int currentBlock = -1;

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public BlockExporterWrapper() {
    setName("Wrapper for exporting a file compressed in blocks");
  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public BlockExporterWrapper(ExporterPlugin exporter, long[] blockOffsets, long[] blockLengths, long[] decompLengths) {
    this.exporter = exporter;
    this.blockOffsets = blockOffsets;
    this.blockLengths = blockLengths;
    this.decompLengths = decompLengths;

    if (exporter != null) {
      setName("Block Compression of type " + exporter.getName());
    }
  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  @Override
  public boolean available() {
    if (exporter.available()) {
      // still reading the current block
      return true;
    }

    // the current block is finished, move on to the next block
    currentBlock++;
    if (currentBlock < blockOffsets.length) {
      // open the next block
      exporter.close();
      //System.out.println("Opening block at " + blockOffsets[currentBlock] + " with compressed length " + blockLengths[currentBlock]);
      exporter.open(new Resource(sourceFile, "", blockOffsets[currentBlock], blockLengths[currentBlock], decompLengths[currentBlock]));
      return exporter.available();
    }
    else {
      // finished reading the last block
      exporter.close();
      return false;
    }

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

      // open the first block, ready to go
      currentBlock = 0;
      exporter.open(new Resource(sourceFile, "", blockOffsets[currentBlock], blockLengths[currentBlock], decompLengths[currentBlock]));

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