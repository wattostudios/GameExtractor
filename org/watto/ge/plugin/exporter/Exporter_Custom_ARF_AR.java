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

public class Exporter_Custom_ARF_AR extends ExporterPlugin {

  static Exporter_Custom_ARF_AR instance = new Exporter_Custom_ARF_AR();

  static FileManipulator readSource;
  static long readLength = 0;

  /**
  **********************************************************************************************

  **********************************************************************************************
  **/
  public static Exporter_Custom_ARF_AR getInstance() {
    return instance;
  }

  byte[] xorBytes;
  int xorPos = 0;

  int numRead = 0;

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public Exporter_Custom_ARF_AR() {
    setName("ARF(AR) XOR Encryption");
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
      xorBytes = null;
    }
    catch (Throwable t) {
      readSource = null;
      xorBytes = null;
    }
  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  @Override
  public String getDescription() {
    return "This exporter decrypts the files in ARF(AR) archives when exporting\n\n" + super.getDescription();
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
      readLength = source.getLength();

      if (source instanceof Resource_FileID) {
        xorBytes = ByteArrayConverter.convertLittle((int) ((Resource_FileID) source).getID());
      }
      else {
        xorBytes = ByteArrayConverter.convertLittle(0);
      }
      xorPos = 0;
      numRead = 0;

    }
    catch (Throwable t) {
    }
  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  @Override
  @SuppressWarnings("unused")
  public void pack(Resource source, FileManipulator destination) {
    try {
      //long decompLength = source.getDecompressedLength();

      ExporterPlugin exporter = source.getExporter();
      exporter.open(source);

      int fileID = -1;
      if (source instanceof Resource_FileID) {
        fileID = (int) ((Resource_FileID) source).getID();
      }

      byte[] xorBytesP;
      if (fileID == -1) {
        xorBytesP = new byte[] { 0, 0, 0, 0 };
      }
      else {
        xorBytesP = ByteArrayConverter.convertLittle(fileID);
      }

      int numReadP = 0;

      int xorPosP = 0;
      while (exporter.available()) {

        if (numReadP < 20) {
          destination.writeByte((byte) (exporter.read() ^ xorBytesP[3]));
          numReadP++;
        }
        else {
          destination.writeByte((byte) (exporter.read()));
        }

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
      if (numRead < 20) {
        numRead++;
        return (readSource.readByte() ^ xorBytes[3]);
      }
      else {
        return readSource.readByte();
      }

    }
    catch (Throwable t) {
      return 0;
    }
  }

}