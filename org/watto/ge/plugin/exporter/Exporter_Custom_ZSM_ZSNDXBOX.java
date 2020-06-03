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
import org.watto.ge.plugin.resource.Resource_FileID;
import org.watto.io.FileManipulator;
import org.watto.io.converter.ByteArrayConverter;

public class Exporter_Custom_ZSM_ZSNDXBOX extends ExporterPlugin {

  static Exporter_Custom_ZSM_ZSNDXBOX instance = new Exporter_Custom_ZSM_ZSNDXBOX();

  static FileManipulator readSource;
  static long readLength = 0;

  /**
  **********************************************************************************************

  **********************************************************************************************
  **/
  public static Exporter_Custom_ZSM_ZSNDXBOX getInstance() {
    return instance;
  }

  int headerPos = 0;

  byte[] header = new byte[0];

  /**
  **********************************************************************************************

  **********************************************************************************************
  **/
  public Exporter_Custom_ZSM_ZSNDXBOX() {
    setName("X-Men Audio Exporter");
  }

  /**
  **********************************************************************************************

  **********************************************************************************************
  **/
  @Override
  public boolean available() {
    return readLength > 0;
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
    return "This exporter extracts the audio from X-Men games when exporting\n\n" + super.getDescription();
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

      readLength = source.getLength() + 44;

      // fill the header data
      header = new byte[] { 82, 73, 70, 70, 0, 0, 0, 0, 87, 65, 86, 69, 102, 109, 116, 32, 16, 0, 0, 0, 1, 0, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1, 0, 8, 0, 100, 97, 116, 97, 0, 0, 0, 0 };
      headerPos = 0;

      long length1 = readLength - 8;
      long length2 = readLength - 44;
      int quality = -1;
      if (source instanceof Resource_FileID) {
        quality = (int) ((Resource_FileID) source).getID();
      }

      byte[] length1bytes = ByteArrayConverter.convertLittle((int) length1);
      byte[] length2bytes = ByteArrayConverter.convertLittle((int) length2);
      byte[] qualitybytes = ByteArrayConverter.convertLittle(quality);

      System.arraycopy(length1bytes, 0, header, 4, 4);
      System.arraycopy(length2bytes, 0, header, 40, 4);
      System.arraycopy(qualitybytes, 0, header, 24, 4);
      System.arraycopy(qualitybytes, 0, header, 28, 4);

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
      //long decompLength = source.getDecompressedLength();

      ExporterPlugin exporter = source.getExporter();
      exporter.open(source);

      while (exporter.available()) {
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

      if (headerPos != 44) {
        headerPos++;
        return header[headerPos - 1];
      }

      return readSource.readByte();
    }
    catch (Throwable t) {
      return 0;
    }
  }

}