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

import java.util.zip.InflaterInputStream;
import org.watto.datatype.Resource;
import org.watto.ge.plugin.ExporterPlugin;
import org.watto.io.FileManipulator;
import org.watto.io.stream.ManipulatorInputStream;

public class Exporter_Custom_PKG_3 extends ExporterPlugin {

  static Exporter_Custom_PKG_3 instance = new Exporter_Custom_PKG_3();

  static InflaterInputStream readSource = null;
  static long readLength = 0;

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public static Exporter_Custom_PKG_3 getInstance() {
    return instance;
  }

  FileManipulator fm;
  int blockLength = 0;
  long blockStart = 0;

  int nextByte = 0;

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public Exporter_Custom_PKG_3() {
    setName("Steam PKG Compression");
  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  @Override
  public boolean available() {
    try {
      if (readLength > 0) {// && readSource.available() > 0){
        nextByte = readSource.read();
        if (readSource.available() <= 0) {
          readBlock();
          nextByte = readSource.read();
        }
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
      fm.close();
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
    return "This exporter decompresses Steam PKG files when exporting\n\n" + super.getDescription();
  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  @Override
  public void open(Resource source) {
    try {
      fm = new FileManipulator(source.getSource(), false);
      fm.seek(source.getOffset());

      readLength = source.getDecompressedLength();

      blockLength = 0;
      blockStart = fm.getOffset();
      readBlock();
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
    /*
     * // not done try { DeflaterOutputStream outputStream = new DeflaterOutputStream(new
     * FileManipulatorOutputStream(destination));
     *
     * ExporterPlugin exporter = source.getExporter(); exporter.open(source);
     *
     * while (exporter.available()){ outputStream.write(exporter.read()); }
     *
     * exporter.close();
     *
     * outputStream.finish();
     *
     * } catch (Throwable t){ logError(t); }
     */
  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  @Override
  public int read() {
    try {
      readLength--;
      return nextByte;
    }
    catch (Throwable t) {
      t.printStackTrace();
      readLength = 0;
      return 0;
    }
  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public void readBlock() {
    try {

      // seek to the correct spot
      fm.seek(blockStart + blockLength);

      // 4 - Compressed Length
      blockLength = fm.readInt();

      blockStart = fm.getOffset();

      // X - Compressed File Data (ZLib)
      if (readSource != null) {
        readSource.close();
      }
      readSource = new InflaterInputStream(new ManipulatorInputStream(fm));
    }
    catch (Throwable t) {
      t.printStackTrace();
    }
  }

}