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

import java.io.InputStream;

import org.watto.ErrorLogger;
import org.watto.datatype.Resource;
import org.watto.ge.plugin.ExporterPlugin;
import org.watto.io.FileManipulator;

import net.lingala.zip4j.ZipFile;
import net.lingala.zip4j.model.FileHeader;

public class Exporter_ZIP_ZipCrypto extends ExporterPlugin {

  static Exporter_ZIP_ZipCrypto instance = new Exporter_ZIP_ZipCrypto();

  static InputStream readSource;
  static long readLength = 0;
  static int currentByte = 0;

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public static Exporter_ZIP_ZipCrypto getInstance() {
    return instance;
  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public Exporter_ZIP_ZipCrypto() {
    setName("ZIP Compression with ZipCrypto Encryption");
  }

  FileHeader fileHeader = null;
  static String password;

  public static void setPassword(String newPassword) {
    password = newPassword;
  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public Exporter_ZIP_ZipCrypto(FileHeader fileHeader) {
    setName("ZIP Compression with ZipCrypto Encryption");
    this.fileHeader = fileHeader;
  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public Exporter_ZIP_ZipCrypto(FileHeader fileHeader, String newPassword) {
    setName("ZIP Compression with ZipCrypto Encryption");
    this.fileHeader = fileHeader;
    password = newPassword;
  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  @Override
  public boolean available() {
    try {

      if (readLength > 0) {
        currentByte = readSource.read();
        readLength--;
        if (currentByte >= 0) {
          return true;
        }
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

      ZipFile zipFile = new ZipFile(source.getSource(), password.toCharArray());
      readSource = zipFile.getInputStream(fileHeader);

      readLength = source.getDecompressedLength();
    }
    catch (Throwable t) {
      ErrorLogger.log(t);
    }
  }

  /**
  **********************************************************************************************
  NOT IMPLEMENTED
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
      // NOTE: The actual reading of the byte is done in available()
      return currentByte;
    }
    catch (Throwable t) {
      t.printStackTrace();
      readLength = 0;
      return 0;
    }
  }

}