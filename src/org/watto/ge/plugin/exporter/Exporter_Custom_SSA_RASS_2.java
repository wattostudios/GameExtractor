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
import org.watto.SingletonManager;
import org.watto.datatype.Resource;
import org.watto.ge.helper.FieldValidator;
import org.watto.ge.plugin.ExporterPlugin;
import org.watto.io.FileManipulator;
import org.watto.io.buffer.ByteBuffer;
import org.watto.io.converter.ByteConverter;

public class Exporter_Custom_SSA_RASS_2 extends ExporterPlugin {

  byte[] buffer = null;

  int bufferPos = 0;

  int bufferLength = 0;

  BlockVariableExporterWrapper blockExporter = null;

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public Exporter_Custom_SSA_RASS_2(BlockVariableExporterWrapper blockExporterIn) {
    setName("SSA PK01 and ZL01 Decompression with 32-byte XOR at the start");
    blockExporter = blockExporterIn;
  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  @Override
  public boolean available() {
    return bufferPos < bufferLength;
  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  @Override
  public void close() {
    //blockExporter = null;
    buffer = null;
    bufferPos = 0;
    bufferLength = 0;
  }

  /**
  **********************************************************************************************
  Just roll the buffer back to the beginning, so that it doesn't trigger a re-decompress of the
  file for each run.
  **********************************************************************************************
  **/
  @Override
  public void closeAndReopen(Resource source) {
    bufferPos = 0;
  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  @Override
  public void open(Resource source) {
    try {

      buffer = null;
      bufferPos = 0;
      bufferLength = 0;

      FileManipulator readSource = new FileManipulator(source.getSource(), false);
      readSource.seek(source.getOffset());

      open(readSource, (int) source.getLength(), (int) source.getDecompressedLength());

    }
    catch (Throwable t) {
    }
  }

  /**
  **********************************************************************************************
  So we can easily call this from within a Viewer plugin
  **********************************************************************************************
  **/
  public void open(FileManipulator fm, int compLengthIn, int decompLengthIn) {

    if (buffer != null && bufferLength > 0) {
      // was already exported and decompressed previously, so don't do it again, just return the existing buffer
      bufferPos = 0;
      return;
    }

    buffer = null;
    bufferPos = 0;
    bufferLength = 0;

    try {

      // Read in all the compressed data

      byte[] compBytes = new byte[compLengthIn];

      File sourceFile = fm.getFile();
      if (sourceFile == null) {
        // reading for a thumbnail, so it's coming from an ExporterBuffer, not from a file. Need to handle differently.
        // We can do this because we actually know which ExporterPlugins we're expecting, which is only a few types.

        ExporterPlugin[] exporters = blockExporter.getBlockExporters();
        int numExporters = exporters.length;
        long[] blockOffsets = blockExporter.getBlockOffsets();
        long[] blockLengths = blockExporter.getBlockLengths();
        long[] blockDecompLengths = blockExporter.getDecompLengths();

        int bufferWritePos = 0;
        for (int i = 0; i < numExporters; i++) {
          ExporterPlugin exporter = exporters[i];

          if (exporter instanceof Exporter_XOR_RepeatingKey) {
            Exporter_XOR_RepeatingKey exporterXOR = (Exporter_XOR_RepeatingKey) exporter;

            fm.relativeSeek(blockOffsets[i]);

            int decompLength = (int) blockDecompLengths[i];
            exporterXOR.open(fm, (int) blockLengths[i], decompLength);

            for (int b = 0; b < decompLength; b++) {
              if (exporterXOR.available()) { // make sure we read the next bit of data, if required
                compBytes[bufferWritePos + b] = (byte) exporterXOR.read();
              }
            }

            bufferWritePos += decompLength;
          }
          else if (exporter instanceof Exporter_Default) {
            Exporter_Default exporterDefault = (Exporter_Default) exporter;

            fm.relativeSeek(blockOffsets[i]);

            int decompLength = (int) blockDecompLengths[i];
            exporterDefault.open(fm, (int) blockLengths[i], decompLength);

            for (int b = 0; b < decompLength; b++) {
              if (exporterDefault.available()) { // make sure we read the next bit of data, if required
                compBytes[bufferWritePos + b] = (byte) exporterDefault.read();
              }
            }

            bufferWritePos += decompLength;

          }
          else {
            return; // some other kind of exporter (which is unexpected, and therefore unsupported)
          }

        }

        // all finished, close everything in the blockExporter
        blockExporter.close();

      }
      else {
        // reading straight from a file - normal read, no issues, nothing fancy

        Resource blockResource = new Resource(sourceFile, "", fm.getOffset(), compLengthIn, decompLengthIn);
        blockExporter.open(blockResource);

        for (int b = 0; b < decompLengthIn; b++) {
          if (blockExporter.available()) { // make sure we read the next bit of data, if required
            compBytes[b] = (byte) blockExporter.read();
          }
        }

        blockExporter.close();
      }

      // open the decompressed data for processing
      fm = new FileManipulator(new ByteBuffer(compBytes));

      // Read the first 4 bytes, determine the compression type

      // 4 - Header
      String type = fm.readString(4);

      // 4 - Decompressed File Length
      int decompLength = fm.readInt();

      // 4 - null
      fm.skip(4);

      // X - Compressed File Data
      if (type.equals("PK01")) {
        // Explode Compression
        FieldValidator.checkLength(decompLength);

        Exporter_Explode exporter = Exporter_Explode.getInstance();
        exporter.open(fm, compLengthIn - 12, decompLength);

        buffer = new byte[decompLength];
        for (int b = 0; b < decompLength; b++) {
          if (exporter.available()) { // make sure we read the next bit of data, if required
            buffer[b] = (byte) exporter.read();
          }
        }

        exporter.close();

        bufferLength = decompLength;
        bufferPos = 0;
      }
      else if (type.equals("ZL01")) {
        // ZLib Deflate Compression
        FieldValidator.checkLength(decompLength);

        Exporter_ZLib exporter = Exporter_ZLib.getInstance();
        exporter.open(fm, compLengthIn - 12, decompLength);

        buffer = new byte[decompLength];
        for (int b = 0; b < decompLength; b++) {
          if (exporter.available()) { // make sure we read the next bit of data, if required
            buffer[b] = (byte) exporter.read();
          }
        }

        exporter.close();

        bufferLength = decompLength;
        bufferPos = 0;
      }
      else {
        // Raw Data
        buffer = compBytes;
        bufferLength = compLengthIn;
        bufferPos = 0;
      }

      // Now that we know the decompressed length, store that back to the Resource.
      // This is important as some viewers (eg DDS) use the Decompressed Length for some error checking.
      Resource resource = (Resource) SingletonManager.get("CurrentResource");
      resource.setDecompressedLength(bufferLength);

      fm.close();

    }
    catch (Throwable t) {
      ErrorLogger.log(t);
    }
  }

  /**
  **********************************************************************************************
  NOT IMPLEMENTED
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
      int currentByte = ByteConverter.unsign(buffer[bufferPos]);
      bufferPos++;
      return currentByte;
    }
    catch (Throwable t) {
      return 0;
    }
  }

}