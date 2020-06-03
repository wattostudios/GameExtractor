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
import org.watto.io.converter.ByteArrayConverter;

public class Exporter_Custom_CAR extends ExporterPlugin {

  static Exporter_Custom_CAR instance = new Exporter_Custom_CAR();

  static FileManipulator packerSource;
  static long readLength = 0;
  static int headerLength = 0;
  static int[] header = null;

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public static Exporter_Custom_CAR getInstance() {
    return instance;
  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public Exporter_Custom_CAR() {
    setName("Carnivores SND-->WAV Converter");
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
   * Closes the global - packing has finished.
   **********************************************************************************************
   **/
  @Override
  public void close() {
    try {
      packerSource.close();
      packerSource = null;
      header = null;
    }
    catch (Throwable t) {
      packerSource = null;
      header = null;
    }
  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  @Override
  public String getDescription() {
    return "This exporter adds a WAV audio header to the sound files when exporting\n\n" + super.getDescription();
  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  @Override
  public void open(Resource source) {
    try {
      packerSource = new FileManipulator(source.getSource(), false);
      packerSource.seek(source.getOffset());
      readLength = source.getLength();

      if (source.getExtension().equals("wav")) {
        headerLength = 0;
        header = new int[16];

        byte[] riff = "RIFF".getBytes();
        System.arraycopy(riff, 0, header, 0, 4);

        byte[] length = ByteArrayConverter.convertLittle((int) (source.getLength() + 16));
        System.arraycopy(length, 0, header, 4, 4);

        byte[] wavefmt = "WAVEfmt ".getBytes();
        System.arraycopy(wavefmt, 0, header, 8, 8);
      }
      else {
        headerLength = 16;
        header = null;
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
      //long decompLength = source.getDecompressedLength();

      ExporterPlugin exporter = source.getExporter();
      exporter.open(source);

      if (exporter instanceof Exporter_Custom_CAR && source.getExtension().equals("wav")) {
        for (int i = 0; i < 16; i++) {
          // skip the WAVE header
          exporter.read();
        }
      }

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
      if (headerLength == 16) {
        readLength--;
        return packerSource.readByte();
      }
      else {
        int headerByte = header[headerLength];
        headerLength++;
        return headerByte;
      }
    }
    catch (Throwable t) {
      return 0;
    }
  }

}