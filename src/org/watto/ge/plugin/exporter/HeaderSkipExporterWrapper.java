/*
 * Application:  Game Extractor
 * Author:       wattostudios
 * Website:      http://www.watto.org
 * Copyright:    Copyright (c) 2002-2023 wattostudios
 *
 * License Information:
 * This program is free software; you can redistribute it and/or modify it under the terms of the GNU General Public License
 * published by the Free Software Foundation; either version 2 of the License, or (at your option) any later versions. This
 * program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranties
 * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License at http://www.gnu.org for more
 * details. For further information on this application, refer to the authors' website.
 */

package org.watto.ge.plugin.exporter;

import org.watto.datatype.Resource;
import org.watto.ge.plugin.ExporterPlugin;
import org.watto.io.FileManipulator;

public class HeaderSkipExporterWrapper extends ExporterPlugin {

  static HeaderSkipExporterWrapper instance = new HeaderSkipExporterWrapper();

  ExporterPlugin exporter = null;

  int skipLength = 0;

  /**
  **********************************************************************************************
  Reads from any type of Exporter, but the first X bytes are skipped
  **********************************************************************************************
  **/
  public static HeaderSkipExporterWrapper getInstance() {
    return instance;
  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public HeaderSkipExporterWrapper() {
    setName("Exporter with Skipped Header");
  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public HeaderSkipExporterWrapper(ExporterPlugin exporter, int skipLength) {
    this.exporter = exporter;
    this.skipLength = skipLength;
    setName("Exporter with Skipped " + skipLength + " Header");
  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  @Override
  public boolean available() {
    return exporter.available();
  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  @Override
  public void close() {
    exporter.close();
    exporter = null;
  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  @Override
  public void open(Resource source) {
    exporter.open(source);

    for (int p = 0; p < skipLength; p++) {
      read();
    }
  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  @Override
  public void pack(Resource source, FileManipulator destination) {
    exporter.pack(source, destination);
  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  @Override
  public int read() {
    return exporter.read();
  }

}