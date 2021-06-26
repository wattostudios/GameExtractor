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
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;
import org.watto.ErrorLogger;
import org.watto.datatype.Resource;
import org.watto.ge.plugin.ExporterPlugin;
import org.watto.io.FileManipulator;
import org.watto.io.stream.ManipulatorInputStream;
import org.watto.io.stream.ManipulatorOutputStream;

public class Exporter_GZip extends ExporterPlugin {

  static Exporter_GZip instance = new Exporter_GZip();

  static GZIPInputStream readSource;

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public static Exporter_GZip getInstance() {
    return instance;
  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public Exporter_GZip() {
    setName("GZip Compression");
  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  @Override
  public boolean available() {
    try {
      if (readSource.available() > 0) {
        return true;
      }
      return false;
    }
    catch (Throwable t) {
      t.printStackTrace();
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
  
  **********************************************************************************************
  **/
  @Override
  public void open(Resource source) {
    try {
      FileManipulator fm = new FileManipulator(source.getSource(), false);
      fm.seek(source.getOffset());

      // check this --> NOTE the UNSIGNED input stream! Doesn't work if it is signed.
      ManipulatorInputStream fis = new ManipulatorInputStream(fm);

      readSource = new GZIPInputStream(fis);
      //readSource = new GZIPInputStream(new FileInputStream(source.getSource()));
    }
    catch (Throwable t) {
      t.printStackTrace();
    }
  }

  /**
  **********************************************************************************************
  So we can easily call this from within a Viewer plugin
  **********************************************************************************************
  **/
  public void open(FileManipulator fmIn, int compLengthIn, int decompLengthIn) {
    try {
      // check this --> NOTE the UNSIGNED input stream! Doesn't work if it is signed.
      ManipulatorInputStream fis = new ManipulatorInputStream(fmIn);

      readSource = new GZIPInputStream(fis);
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
    GZIPOutputStream outputStream = null;
    try {
      outputStream = new GZIPOutputStream(new ManipulatorOutputStream(destination));

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
      return readSource.read();
    }
    catch (Throwable t) {
      t.printStackTrace();
      return 0;
    }
  }

}