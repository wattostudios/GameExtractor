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

import java.io.BufferedInputStream;

import org.watto.datatype.Resource;
import org.watto.ge.plugin.ExporterPlugin;
import org.watto.io.FileManipulator;
import org.watto.io.stream.FakeFileInputStream;
import org.watto.io.stream.LZSSInputStream;
import org.watto.io.stream.LZSSOutputStream;
import org.watto.io.stream.ManipulatorOutputStream;

public class Exporter_LZSS_Old extends ExporterPlugin {

  static Exporter_LZSS_Old instance = new Exporter_LZSS_Old();

  static LZSSInputStream readSource;
  static byte[] singleBuffer = new byte[1];

  static boolean updateDecompLength = false;
  static int bytesRead = 0;
  static Resource readingResource = null;

  public void updateDecompLengthAfterDecompression(boolean update) {
    updateDecompLength = update;
  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public static Exporter_LZSS_Old getInstance() {
    return instance;
  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public Exporter_LZSS_Old() {
    setName("LZSS Compression (Old)");
  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  @Override
  public boolean available() {
    try {
      int numAvailable = readSource.read(singleBuffer);
      if (numAvailable > 0) {
        bytesRead++;
        return true;
      }
      return false;
    }
    catch (Throwable t) {
      return false;
    }
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

      if (updateDecompLength) {
        readingResource.setDecompressedLength(bytesRead);
        bytesRead = 0;
      }

      readingResource = null;
    }
    catch (Throwable t) {
      readSource = null;
      readingResource = null;
    }
  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  @Override
  public void open(Resource source) {
    try {
      FakeFileInputStream is = new FakeFileInputStream(source.getSource());

      is.skip(source.getOffset() + 4);

      is.setFakePointer(0);
      is.setFakeLength(source.getLength() - 4);

      readSource = new LZSSInputStream(new BufferedInputStream(is));

      bytesRead = 0;

      if (updateDecompLength) {
        // remember the Resource, if we're going to update it after decompression
        readingResource = source;

      }
    }
    catch (Throwable t) {
    }
  }

  /**
   **********************************************************************************************
   * Untested
   **********************************************************************************************
   **/
  @Override
  public void pack(Resource source, FileManipulator destination) {
    try {

      //System.out.println(destination.getOffset());

      ManipulatorOutputStream os = new ManipulatorOutputStream(destination);

      byte[] magic = new byte[] { 0x73, 0x6C, 0x68, 0x21 };
      os.write(magic);

      LZSSOutputStream outputStream = new LZSSOutputStream(os);

      ExporterPlugin exporter = source.getExporter();
      exporter.open(source);

      byte[] singleOutBuffer = new byte[1];
      while (exporter.available()) {
        singleOutBuffer[0] = (byte) exporter.read();
        outputStream.write(singleOutBuffer, 0, 1);
      }

      exporter.close();

      //System.out.println(destination.getOffset());

      outputStream.close();
      //destination.forceWrite();

      //System.out.println(destination.getOffset());
      //System.out.println("==========");

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
      return singleBuffer[0];
    }
    catch (Throwable t) {
      return 0;
    }
  }

}