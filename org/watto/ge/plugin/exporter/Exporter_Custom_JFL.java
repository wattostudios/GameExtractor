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
import org.watto.io.converter.BooleanArrayConverter;

public class Exporter_Custom_JFL extends ExporterPlugin {

  static Exporter_Custom_JFL instance = new Exporter_Custom_JFL();

  static FileManipulator readSource;
  static long readLength = 0;
  static byte[] readBuffer;
  static int readBufferPos = 0;

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public static Exporter_Custom_JFL getInstance() {
    return instance;
  }

  /**
   **********************************************************************************************
   * // NOTE - This exporter uses the compressedlength!
   **********************************************************************************************
   **/
  public Exporter_Custom_JFL() {
    setName("Traffic Giant JFL Compression");
  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  @Override
  public boolean available() {
    try {
      if (readLength <= 0) {
        return false;
      }

      if (readBufferPos >= readBuffer.length) {
        fillBuffer();
      }
      return true;

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
      readBuffer = null;
    }
    catch (Throwable t) {
      readSource = null;
    }
  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public void fillBuffer() {
    try {

      byte byt = readSource.readByte();
      boolean[] determinant = BooleanArrayConverter.convertLittle(byt);

      readLength--;

      int bytelength = 0;
      if (determinant[0]) {
        bytelength += 1;
      }
      ;
      if (determinant[1]) {
        bytelength += 2;
      }
      ;
      if (determinant[2]) {
        bytelength += 4;
      }
      ;
      if (determinant[3]) {
        bytelength += 8;
      }
      ;
      if (determinant[4]) {
        bytelength += 16;
      }
      ;
      if (determinant[5]) {
        bytelength += 32;
      }
      ;
      if (determinant[6]) {
        bytelength += 64;
      }
      ;

      if (determinant[7]) {
        //System.out.println("Pos " + readBytes + " of " + length + " - Write a single byte " + (length + 3) + " times");
        readBuffer = new byte[bytelength + 3];

        byte nextbyte = readSource.readByte();

        for (int i = 0; i < bytelength + 3; i++) {
          readBuffer[i] = nextbyte;
        }

        readLength -= bytelength + 3;
      }
      else {
        //System.out.println("Pos " + readBytes + " of " + length + " - Write " + bytelength + " raw bytes");
        readBuffer = readSource.readBytes(bytelength);
        readBufferPos = 0;
        readLength -= bytelength;
      }

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
  public String getDescription() {

    return "This exporter decompresses JFL-compressed files when exporting\n\n" + super.getDescription();

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

      readBuffer = new byte[0];
      readBufferPos = 0;

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
      int byteVal = readBuffer[readBufferPos];
      readBufferPos++;
      return byteVal;
    }
    catch (Throwable t) {
      return 0;
    }
  }

}