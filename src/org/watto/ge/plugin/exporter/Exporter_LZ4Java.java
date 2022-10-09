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
import net.jpountz.lz4.LZ4Compressor;
import net.jpountz.lz4.LZ4Factory;
import net.jpountz.lz4.LZ4FastDecompressor;

public class Exporter_LZ4Java extends ExporterPlugin {

  static Exporter_LZ4Java instance = new Exporter_LZ4Java();

  byte[] buffer = null;

  int bufferPos = 0;

  int bufferLength = 0;

  /**
  **********************************************************************************************
  LZ4 Decompressor (https://github.com/lz4/lz4-java)
  NOTE: REQUIRES JAVA CLASS FILES PUT DIRECTLY INTO THE BIN DIRECTORY
  (as many of the classes are generated from a Template, not from JAVA files)
  **********************************************************************************************
  **/
  public static Exporter_LZ4Java getInstance() {
    return instance;
  }

  FileManipulator fm;

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public Exporter_LZ4Java() {
    setName("LZ4 Compression");
  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  @Override
  public boolean available() {
    try {

      return (bufferPos < bufferLength);

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
    buffer = null;
  }

  /**
  **********************************************************************************************
  So we can easily call this from within a Viewer plugin
  **********************************************************************************************
  **/
  public void open(FileManipulator fmIn, int compLengthIn, int decompLengthIn) {
    try {
      // reset now, so if there's any errors, it'll return nothing
      bufferPos = 0;
      bufferLength = 0;

      // Read in the compressed bytes
      byte[] compBytes = fm.readBytes(compLengthIn);

      // Pass it into the LZ4 Decompressor
      LZ4Factory factory = LZ4Factory.fastestInstance();
      LZ4FastDecompressor decompressor = factory.fastDecompressor();
      buffer = new byte[decompLengthIn];
      decompressor.decompress(compBytes, 0, buffer, 0, decompLengthIn);

      // ready to read from the buffer
      bufferPos = 0;
      bufferLength = decompLengthIn;

    }
    catch (Throwable t) {
      ErrorLogger.log(t);
    }
  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  @Override
  public void open(Resource source) {
    try {
      // reset now, so if there's any errors, it'll return nothing
      bufferPos = 0;
      bufferLength = 0;

      int compLength = (int) source.getLength();
      int decompLength = (int) source.getDecompressedLength();

      // Read in the compressed bytes
      FileManipulator fm = new FileManipulator(source.getSource(), false);
      fm.seek(source.getOffset());
      byte[] compBytes = fm.readBytes(compLength);
      fm.close();

      // Pass it into the LZ4 Decompressor
      LZ4Factory factory = LZ4Factory.fastestInstance();
      LZ4FastDecompressor decompressor = factory.fastDecompressor();
      buffer = new byte[decompLength];
      decompressor.decompress(compBytes, 0, buffer, 0, decompLength);

      // ready to read from the buffer
      bufferPos = 0;
      bufferLength = decompLength;
    }
    catch (Throwable t) {
      ErrorLogger.log(t);
    }
  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  @Override
  public void pack(Resource source, FileManipulator destination) {
    try {

      // Read in the full source (in uncompressed state)
      int decompLength = (int) source.getDecompressedLength();
      byte[] decompBytes = new byte[decompLength];
      int decompPos = 0;

      ExporterPlugin exporter = source.getExporter();
      exporter.open(source);

      while (exporter.available() && decompPos < decompLength) {
        decompBytes[decompPos] = (byte) exporter.read();
        decompPos++;
      }

      exporter.close();

      // now compress it
      LZ4Factory factory = LZ4Factory.fastestInstance();
      LZ4Compressor compressor = factory.fastCompressor();
      int maxCompressedLength = compressor.maxCompressedLength(decompLength);
      byte[] compressed = new byte[maxCompressedLength];
      int compLength = compressor.compress(decompBytes, 0, decompLength, compressed, 0, maxCompressedLength);

      // now write the compressed bytes to the destination
      byte[] compBytes = new byte[compLength];
      System.arraycopy(compressed, 0, compBytes, 0, compLength);
      destination.writeBytes(compBytes);

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
      int currentByte = buffer[bufferPos];
      bufferPos++;
      return currentByte;
    }
    catch (Throwable t) {
      t.printStackTrace();
      bufferPos = bufferLength;
      return 0;
    }
  }

}