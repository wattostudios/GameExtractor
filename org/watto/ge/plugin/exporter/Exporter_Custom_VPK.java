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
import org.watto.datatype.Resource;
import org.watto.ge.plugin.ExporterPlugin;
import org.watto.ge.plugin.resource.Resource_VPK;
import org.watto.io.FileManipulator;

public class Exporter_Custom_VPK extends ExporterPlugin {

  static Exporter_Custom_VPK instance = new Exporter_Custom_VPK();

  static FileManipulator readSource = null;

  static long preloadReadLength = 0;
  static long mainReadLength = 0;

  static long preloadReadOffset = 0;
  static long mainReadOffset = 0;

  static File preloadSource = null;
  static File mainSource = null;

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public static Exporter_Custom_VPK getInstance() {
    return instance;
  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public Exporter_Custom_VPK() {
    setName("VPK File Entry with Preload Data");
  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  @Override
  public boolean available() {
    if (preloadReadLength == 0) {
      // trigger the change from preload to main
      if (readSource != null && readSource.isOpen()) {
        readSource.close();
      }

      // try to get the whole file in a single go, if it isn't too large (set to 200KB otherwise)
      int bufferSize = (int) mainReadLength;
      if (bufferSize > 204800) {
        bufferSize = 204800;
      }

      readSource = new FileManipulator(mainSource, false, bufferSize);

      preloadReadLength--; // so we don't trigger the change again
    }

    return mainReadLength > 0; // as we read "preload" before "main", we only need to report on "main" here
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

      if (source instanceof Resource_VPK) {
        Resource_VPK resource = (Resource_VPK) source;
        mainReadLength = resource.getMainLength();
        mainReadOffset = resource.getMainOffset();
        mainSource = resource.getSource();

        preloadReadLength = resource.getPreloadDataLength();
        preloadReadOffset = resource.getPreloadDataOffset();
        preloadSource = resource.getPreloadDataSource();
      }
      else {
        mainReadLength = source.getLength();
        mainReadOffset = source.getOffset();
        mainSource = source.getSource();

        preloadReadLength = 0;
        preloadReadOffset = 0;
        preloadSource = null;
      }

      // try to get the whole file in a single go, if it isn't too large (set to 200KB otherwise)
      int bufferSize = (int) mainReadLength;
      if (bufferSize > 204800) {
        bufferSize = 204800;
      }

      if (preloadSource != null) {
        readSource = new FileManipulator(preloadSource, false, bufferSize);
        readSource.seek(preloadReadOffset);
      }
      else {
        readSource = new FileManipulator(mainSource, false, bufferSize);
        readSource.seek(mainReadOffset);
        preloadReadLength = -1; // so available() doesn't close() and open() the file again
      }

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
      if (preloadReadLength > 0) {
        preloadReadLength--;
      }
      else {
        mainReadLength--;
      }
      return readSource.readByte(); // available() already handles the transition between preload and main
    }
    catch (Throwable t) {
      return 0;
    }
  }

}