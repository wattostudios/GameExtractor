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

import java.io.IOException;
import org.apache.commons.compress.compressors.lz4.BlockLZ4CompressorInputStream;
import org.apache.commons.compress.compressors.lz4.BlockLZ4CompressorOutputStream;
import org.watto.ErrorLogger;
import org.watto.datatype.Resource;
import org.watto.ge.plugin.ExporterPlugin;
import org.watto.io.FileManipulator;
import org.watto.io.buffer.FileBuffer;
import org.watto.io.buffer.XORRepeatingKeyBufferWrapper;
import org.watto.io.stream.ManipulatorInputStream;
import org.watto.io.stream.ManipulatorOutputStream;

public class Exporter_Custom_RPKG_GKPR_Multi extends ExporterPlugin {

  static Exporter_Custom_RPKG_GKPR_Multi instance = new Exporter_Custom_RPKG_GKPR_Multi();

  static BlockLZ4CompressorInputStream readSource;

  static long readLength = 0;

  static int currentByte = 0;

  /**
  **********************************************************************************************
  Does XOR decryption with a repeating key, then feeds that into an LZ4 decompressor
  **********************************************************************************************
  **/
  public static Exporter_Custom_RPKG_GKPR_Multi getInstance() {
    return instance;
  }

  FileManipulator fm;

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public Exporter_Custom_RPKG_GKPR_Multi() {
    setName("XOR Repeating Key Decryption + LZ4 Compression");
  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  @Override
  public boolean available() {
    try {

      if (readLength > 0) {
        currentByte = readSource.read();
        readLength--;
        if (currentByte >= 0) {
          return true;
        }
      }

      return false;
    }
    catch (Throwable t) {
      ErrorLogger.log(t);
      return false;
      //return true;
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
      readSource.close();
      readSource = null;
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
  public void open(Resource source) {
    try {

      //fm = new FileManipulator(source.getSource(), false);
      //fm.setBuffer(new XORRepeatingKeyBufferWrapper(fm.getBuffer(), new int[] { 220, 69, 166, 156, 211, 114, 76, 171 }));

      fm = new FileManipulator(new XORRepeatingKeyBufferWrapper(new FileBuffer(source.getSource(), false), new int[] { 220, 69, 166, 156, 211, 114, 76, 171 }));

      fm.seek(source.getOffset());

      readSource = new BlockLZ4CompressorInputStream(new ManipulatorInputStream(fm));
      readLength = source.getDecompressedLength();
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
    BlockLZ4CompressorOutputStream outputStream = null;
    try {

      destination.setBuffer(new XORRepeatingKeyBufferWrapper(destination.getBuffer(), new int[] { 220, 69, 166, 156, 211, 114, 76, 171 }));

      outputStream = new BlockLZ4CompressorOutputStream(new ManipulatorOutputStream(destination));

      ExporterPlugin exporter = source.getExporter();
      exporter.open(source);

      while (exporter.available()) {
        outputStream.write(exporter.read());
      }

      exporter.close();

      outputStream.finish();

    }
    catch (Throwable t) {
      logError(t);
      if (outputStream != null) {
        try {
          outputStream.finish();
        }
        catch (IOException e) {
        }
      }
    }
  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  @Override
  public int read() {
    try {
      // NOTE: The actual reading of the byte is done in available()
      return currentByte;
    }
    catch (Throwable t) {
      t.printStackTrace();
      readLength = 0;
      return 0;
    }
  }

}