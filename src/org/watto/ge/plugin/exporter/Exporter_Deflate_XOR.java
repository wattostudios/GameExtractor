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
import java.util.zip.Deflater;
import java.util.zip.DeflaterOutputStream;
import java.util.zip.Inflater;
import java.util.zip.InflaterInputStream;
import org.watto.datatype.Resource;
import org.watto.ge.plugin.ExporterPlugin;
import org.watto.io.FileManipulator;
import org.watto.io.buffer.ManipulatorBuffer;
import org.watto.io.buffer.XORBufferWrapper;
import org.watto.io.stream.ManipulatorInputStream;
import org.watto.io.stream.ManipulatorOutputStream;

public class Exporter_Deflate_XOR extends ExporterPlugin {

  static Exporter_Deflate_XOR instance = new Exporter_Deflate_XOR();

  static InflaterInputStream readSource;
  static long readLength = 0;

  /**
  **********************************************************************************************
  Deflate operating on XOR'd data
  **********************************************************************************************
  **/
  public static Exporter_Deflate_XOR getInstance() {
    return instance;
  }

  int xorValue = 0;

  FileManipulator fm;

  /**
  **********************************************************************************************

  **********************************************************************************************
  **/
  public Exporter_Deflate_XOR() {
    setName("ZLib Compression operating on XOR'd data");
  }

  /**
  **********************************************************************************************

  **********************************************************************************************
  **/
  public Exporter_Deflate_XOR(int xorValue) {
    setName("ZLib Compression operating on XOR'd data");
    this.xorValue = xorValue;
  }

  /**
  **********************************************************************************************

  **********************************************************************************************
  **/
  @Override
  public boolean available() {
    try {
      if (readLength > 0 && readSource.available() > 0) {
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
  public void open(FileManipulator fmIn, int compLengthIn, int decompLengthIn) {
    fm = fmIn;

    // Set the XOR
    fm.setBuffer(new XORBufferWrapper(fm.getBuffer(), xorValue));

    readSource = new InflaterInputStream(new ManipulatorInputStream(fm), new Inflater(true));
    readLength = decompLengthIn;
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

      // Set the XOR
      fm.setBuffer(new XORBufferWrapper(fm.getBuffer(), xorValue));

      readSource = new InflaterInputStream(new ManipulatorInputStream(fm), new Inflater(true));
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
    DeflaterOutputStream outputStream = null;
    ManipulatorBuffer existingBuffer = fm.getBuffer();
    try {

      // Set the XOR
      fm.setBuffer(new XORBufferWrapper(existingBuffer, xorValue));

      outputStream = new DeflaterOutputStream(new ManipulatorOutputStream(destination), new Deflater(Deflater.DEFAULT_COMPRESSION, true));

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
      fm.setBuffer(existingBuffer);
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
      readLength--;
      return readSource.read();
    }
    catch (Throwable t) {
      t.printStackTrace();
      readLength = 0;
      return 0;
    }
  }

}