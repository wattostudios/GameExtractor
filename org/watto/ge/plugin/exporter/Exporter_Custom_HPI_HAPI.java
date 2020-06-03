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

import org.watto.datatype.Resource;
import org.watto.ge.plugin.ExporterPlugin;
import org.watto.io.FileManipulator;

public class Exporter_Custom_HPI_HAPI extends ExporterPlugin {

  static Exporter_Custom_HPI_HAPI instance = new Exporter_Custom_HPI_HAPI();

  static FileManipulator readSource;
  static long readLength = 0;

  static int key = 0;

  /**
  **********************************************************************************************

  **********************************************************************************************
  **/
  public static Exporter_Custom_HPI_HAPI getInstance() {
    return instance;
  }

  /**
  **********************************************************************************************

  **********************************************************************************************
  **/
  public Exporter_Custom_HPI_HAPI() {
    setName("Total Annihilation HAPI Encryption");
  }

  /**
  **********************************************************************************************

  **********************************************************************************************
  **/
  @Override
  public boolean available() {
    if (readLength > 0) {
      return true;
    }
    return false;
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
  public String getDescription() {
    return "This exporter decrypts HAPI encrypted files when exporting\n\n" + super.getDescription();
  }

  /**
  **********************************************************************************************

  **********************************************************************************************
  **/
  @Override
  public void open(Resource source) {
    try {
      readSource = new FileManipulator(source.getSource(), false);
      readSource.seek(source.getOffset());

      readLength = source.getDecompressedLength();
    }
    catch (Throwable t) {
    }
  }

  /**
   **********************************************************************************************
   * // TEST - NOT DONE
   **********************************************************************************************
   **/
  @Override
  public void pack(Resource source, FileManipulator destination) {
    try {
      long decompLength = source.getDecompressedLength();

      ExporterPlugin exporter = source.getExporter();
      exporter.open(source);

      for (int i = 0; i < decompLength; i++) {
        destination.writeByte(exporter.read());
      }

      exporter.close();

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
      readLength--;
      return readByte();
    }
    catch (Throwable t) {
      return 0;
    }
  }

  /**
  **********************************************************************************************

  **********************************************************************************************
  **/
  public byte readByte() throws Exception {
    if (key == 0) {
      return readSource.readByte();
    }
    int tKey = (int) (readSource.getOffset() ^ key);
    return (byte) (tKey ^ ~(readSource.readByte()));
  }

  /**
  **********************************************************************************************

  **********************************************************************************************
  **/
  public void setKey(int key2) {
    key = key2;
  }

}