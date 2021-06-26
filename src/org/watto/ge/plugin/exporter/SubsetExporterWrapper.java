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
import org.watto.Language;
import org.watto.Settings;
import org.watto.datatype.Resource;
import org.watto.ge.plugin.ExporterPlugin;
import org.watto.io.FileManipulator;
import org.watto.io.FilenameSplitter;
import org.watto.task.TaskProgressManager;

public class SubsetExporterWrapper extends ExporterPlugin {

  File sourceFile = null;

  /** the exporter that will do all the actual work **/
  ExporterPlugin exporter = null;

  /** the offset to the compressed block **/
  long blockOffset;

  /** the length of the compressed block **/
  long blockLength;

  /** the decompressed length of the compressed block **/
  long blockDecompLength;

  /** the offset to the file in the decompressed block **/
  long fileOffset;

  /** the decompressed length of the file in the decompressed block **/
  long fileDecompLength;

  /** after decompressing to a temp file, this will contain the file to read **/
  FileManipulator tempFM = null;

  /** for reading from the decompressed file **/
  long readLength = 0;

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public SubsetExporterWrapper() {
    setName("Wrapper for exporting a single file from a compressed block which contains multiple files compressed together");
  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public SubsetExporterWrapper(ExporterPlugin exporter, long blockOffset, long blockLength, long blockDecompLength, long fileOffset, long fileDecompLength) {
    this.exporter = exporter;

    this.blockOffset = blockOffset;
    this.blockLength = blockLength;
    this.blockDecompLength = blockDecompLength;

    this.fileOffset = fileOffset;
    this.fileDecompLength = fileDecompLength;

    if (exporter != null) {
      setName("Subset Compression of type " + exporter.getName());
    }
  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  @Override
  public boolean available() {
    if (tempFM != null && readLength > 0) {
      // still reading the current block
      return true;
    }

    exporter.close();
    return false;
  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  @Override
  public void close() {
    exporter.close();
    if (tempFM != null) {
      tempFM.close();
      tempFM = null;
    }
  }

  /**
   **********************************************************************************************
   Decompresses the block to a temporary file, then returns it.
   If it's already decompressed, it just returns the already-decompressed file.
   **********************************************************************************************
   **/
  public FileManipulator decompressToTempFile(Resource source) {
    try {

      sourceFile = source.getSource();

      // Build a new "_ge_decompressed" temp file, in the temp directory, with the blockOffset in the filename
      String filenameOnly = FilenameSplitter.getFilename(sourceFile);
      String extensionOnly = FilenameSplitter.getExtension(sourceFile);

      File decompFile = new File(Settings.getString("TempDirectory") + File.separator + filenameOnly + "_ge_decompressed_" + blockOffset + "." + extensionOnly);
      if (decompFile.exists()) {
        // we've already decompressed this file before - open and return it
        return new FileManipulator(decompFile, false);
      }

      FileManipulator decompFM = new FileManipulator(decompFile, true);

      FileManipulator fm = new FileManipulator(sourceFile, false);
      fm.seek(blockOffset); // to fill the buffer from the start of the file, for efficient reading

      // Now decompress the block into the decompressed file
      TaskProgressManager.setMessage(Language.get("Progress_DecompressingArchive")); // progress bar
      TaskProgressManager.setMaximum(blockDecompLength); // progress bar
      TaskProgressManager.setIndeterminate(true);

      //exporter.open(fm, blockLength, blockDecompLength);
      exporter.open(new Resource(sourceFile, "", blockOffset, blockLength, blockDecompLength));

      while (exporter.available()) {
        decompFM.writeByte(exporter.read());
      }

      // Force-write out the decompressed file to write it to disk, then change the buffer to read-only.
      decompFM.close();
      decompFM = new FileManipulator(decompFile, false);

      TaskProgressManager.setMessage(Language.get("Progress_ReadingArchive")); // progress bar
      TaskProgressManager.setIndeterminate(false);

      // Return the file pointer to the beginning, and return the decompressed file
      decompFM.seek(0);
      return decompFM;
    }
    catch (Throwable t) {
      ErrorLogger.log(t);
      return null;
    }
  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  @Override
  public void open(Resource source) {
    try {

      // decompress the whole file to a temporary location (unless it is already decompressed), and open it
      tempFM = decompressToTempFile(source);

      // now seek to the correct place in the decompressed file
      tempFM.relativeSeek(fileOffset);

      readLength = fileDecompLength;

    }
    catch (Throwable t) {
    }
  }

  /**
  **********************************************************************************************
  NOT REALLY SUPPORTED
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
      return tempFM.readByte();
    }
    catch (Throwable t) {
      return 0;
    }
  }

}