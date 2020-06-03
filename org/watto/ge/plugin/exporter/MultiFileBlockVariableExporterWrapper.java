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
import org.watto.ge.plugin.resource.Resource_WAV_RawAudio;
import org.watto.io.FileManipulator;

public class MultiFileBlockVariableExporterWrapper extends ExporterPlugin {

  File[] sourceFiles;

  /** the exporters that will do all the actual work **/
  ExporterPlugin[] exporters = null;
  ExporterPlugin currentExporter = null;

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
  public MultiFileBlockVariableExporterWrapper() {
    setName("Wrapper for exporting a file compressed in blocks, where each block can use a different compression algorithm, and each block can be in a different file");
  }

  /**
  **********************************************************************************************

  **********************************************************************************************
  **/
  public MultiFileBlockVariableExporterWrapper(ExporterPlugin[] exporters, File[] sourceFiles, long[] blockOffsets, long[] blockLengths, long[] decompLengths) {

    this.exporters = exporters;
    this.sourceFiles = sourceFiles;
    this.blockOffsets = blockOffsets;
    this.blockLengths = blockLengths;
    this.decompLengths = decompLengths;
  }

  /**
  **********************************************************************************************

  **********************************************************************************************
  **/
  @Override
  public boolean available() {
    if (currentExporter.available()) {
      // still reading the current block
      return true;
    }

    // the current block is finished, move on to the next block
    currentBlock++;
    if (currentBlock < blockOffsets.length) {
      // open the next block...
      currentExporter.close();

      // change to the next exporter
      currentExporter = exporters[currentBlock];
      // open the block
      currentExporter.open(new Resource(sourceFiles[currentBlock], "", blockOffsets[currentBlock], blockLengths[currentBlock], decompLengths[currentBlock]));
      return currentExporter.available();
    }
    else {
      // finished reading the last block
      currentExporter.close();
      return false;
    }

  }

  /**
  **********************************************************************************************

  **********************************************************************************************
  **/
  @Override
  public void close() {
    currentExporter.close();
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

      // open the first block, ready to go

      currentBlock = 0;
      currentExporter = exporters[currentBlock];

      if (source instanceof Resource_WAV_RawAudio) {
        Resource_WAV_RawAudio sourceWAV = (Resource_WAV_RawAudio) source;

        Resource_WAV_RawAudio dummyResource = new Resource_WAV_RawAudio(sourceFiles[currentBlock], "", blockOffsets[currentBlock], blockLengths[currentBlock], decompLengths[currentBlock]);
        dummyResource.setAudioProperties(sourceWAV.getFrequency(), sourceWAV.getBitrate(), sourceWAV.getChannels(), sourceWAV.getAudioLength());
        currentExporter.open(dummyResource);
      }
      else {
        currentExporter.open(new Resource(sourceFiles[currentBlock], "", blockOffsets[currentBlock], blockLengths[currentBlock], decompLengths[currentBlock]));
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
      return currentExporter.read(); // available() already handles the transition between blocks
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