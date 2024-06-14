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
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import org.watto.datatype.Resource;
import org.watto.ge.plugin.ExporterPlugin;
import org.watto.io.FileManipulator;

public class Exporter_ZIP extends ExporterPlugin {

  static Exporter_ZIP instance = new Exporter_ZIP();

  static BufferedInputStream readSource;
  static long readLength = 0;

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public static Exporter_ZIP getInstance() {
    return instance;
  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public Exporter_ZIP() {
    setName("ZIP Compression");
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
      ZipFile zipArchive = new ZipFile(source.getSource());
      ZipEntry zippedFile = zipArchive.getEntry(source.getOriginalName());

      readSource = new BufferedInputStream(zipArchive.getInputStream(zippedFile));
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
      return 0;
    }
  }

}