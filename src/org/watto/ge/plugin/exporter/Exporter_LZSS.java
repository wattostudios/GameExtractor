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
import org.watto.datatype.Resource;
import org.watto.ge.plugin.ExporterPlugin;
import org.watto.io.FileManipulator;
import org.watto.io.stream.LZSSInputStream;
import org.watto.io.stream.LZSSOutputStream;
import org.watto.io.stream.ManipulatorInputStream;
import org.watto.io.stream.ManipulatorOutputStream;

public class Exporter_LZSS extends ExporterPlugin {

  static Exporter_LZSS instance = new Exporter_LZSS();

  static LZSSInputStream readSource;

  static long readLength = 0;

  static int currentByte = 0;

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public static Exporter_LZSS getInstance() {
    return instance;
  }

  FileManipulator fm;

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public Exporter_LZSS() {
    setName("LZSS Compression");
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
  So we can easily call this from within a Viewer plugin
  **********************************************************************************************
  **/
  public void open(FileManipulator fmIn, int readLengthIn) {
    try {
      fm = fmIn;

      readSource = new LZSSInputStream(new ManipulatorInputStream(fm));

      readLength = readLengthIn;
    }
    catch (Throwable t) {
    }
  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  @Override
  public void open(Resource source) {
    try {
      fm = new FileManipulator(source.getSource(), false);
      fm.seek(source.getOffset());

      readSource = new LZSSInputStream(new ManipulatorInputStream(fm));

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

    ManipulatorOutputStream os = new ManipulatorOutputStream(destination);

    byte[] magic = new byte[] { 0x73, 0x6C, 0x68, 0x21 };
    os.write(magic);

    LZSSOutputStream outputStream = new LZSSOutputStream(os);

    try {
      ExporterPlugin exporter = source.getExporter();
      exporter.open(source);

      while (exporter.available()) {
        outputStream.write(exporter.read());
      }

      exporter.close();

      outputStream.close();

    }
    catch (Throwable t) {
      logError(t);
      if (outputStream != null) {
        try {
          outputStream.close();
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