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
import org.watto.datatype.Resource;
import org.watto.ge.plugin.ExporterPlugin;
import org.watto.io.FileManipulator;
import org.watto.io.stream.ManipulatorOutputStream;
import org.watto.io.stream.ZLibXInputStream;

public class Exporter_ZLibX extends ExporterPlugin {

  static Exporter_ZLibX instance = new Exporter_ZLibX();

  static ZLibXInputStream readSource;
  static long readLength = 0;

  /**
  **********************************************************************************************

  **********************************************************************************************
  **/
  public static Exporter_ZLibX getInstance() {
    return instance;
  }

  FileManipulator fm;

  /**
  **********************************************************************************************

  **********************************************************************************************
  **/
  public Exporter_ZLibX() {
    setName("ZLibX Compression");
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

  **********************************************************************************************
  **/
  @Override
  public void open(Resource source) {
    try {
      fm = new FileManipulator(source.getSource(), false);
      fm.seek(source.getOffset());

      readSource = new ZLibXInputStream(fm, source.getDecompressedLength());
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