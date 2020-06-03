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
import java.util.zip.DeflaterOutputStream;
import java.util.zip.InflaterInputStream;
import org.watto.datatype.Resource;
import org.watto.ge.plugin.ExporterPlugin;
import org.watto.io.FileManipulator;
import org.watto.io.stream.ManipulatorInputStream;
import org.watto.io.stream.ManipulatorOutputStream;

public class Exporter_ZLib_CompressedSizeOnly extends ExporterPlugin {

  static Exporter_ZLib_CompressedSizeOnly instance = new Exporter_ZLib_CompressedSizeOnly();

  static InflaterInputStream readSource;
  static long readLength = 0;

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public static Exporter_ZLib_CompressedSizeOnly getInstance() {
    return instance;
  }

  int nextByte = 0;

  /**
   **********************************************************************************************
   * ZLib decompressor where only the compressed size of a file is known
   **********************************************************************************************
   **/
  public Exporter_ZLib_CompressedSizeOnly() {
    setName("ZLib Compression - Compressed Size Only");
  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  @Override
  public boolean available() {
    try {
      nextByte = readSource.read();
      // Checks to see if there is still more data in the stream.
      // If not, the nextByte was the null at the end of the stream
      // so we want to return false, as the null should not be exported.
      if (readSource.available() <= 0) {
        readSource.close();
        return false;
      }
      return true;
    }
    catch (Throwable t) {
      nextByte = 0;
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
  public void open(FileManipulator fmIn, int compLengthIn, int decompLengthIn) {
    try {
      readSource = new InflaterInputStream(new ManipulatorInputStream(fmIn));
      readLength = decompLengthIn;
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
      FileManipulator fm = new FileManipulator(source.getSource(), false);
      fm.seek(source.getOffset());

      readSource = new InflaterInputStream(new ManipulatorInputStream(fm));
      readLength = (int) source.getOffset() + source.getLength();
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
    DeflaterOutputStream outputStream = null;
    try {
      outputStream = new DeflaterOutputStream(new ManipulatorOutputStream(destination));

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
    return nextByte;
  }

}