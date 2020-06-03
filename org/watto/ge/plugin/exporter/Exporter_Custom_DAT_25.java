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

public class Exporter_Custom_DAT_25 extends ExporterPlugin {

  static Exporter_Custom_DAT_25 instance = new Exporter_Custom_DAT_25();

  static FileManipulator readSource;
  static long readLength;

  static long[] offsets;
  static long[] lengths;

  static int partNum = 0;
  static InflaterInputStream partStream;

  /**
  **********************************************************************************************

  **********************************************************************************************
  **/
  public static Exporter_Custom_DAT_25 getInstance() {
    return instance;
  }

  /**
  **********************************************************************************************

  **********************************************************************************************
  **/
  public Exporter_Custom_DAT_25() {
    setName("Juiced Part Joiner");
  }

  /**
  **********************************************************************************************

  **********************************************************************************************
  **/
  @Override
  public boolean available() {
    try {
      return (partNum < offsets.length && partStream.available() > 0);
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
      partStream.close();
      partStream = null;

      offsets = null;
      lengths = null;
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
    return "This exporter converts a file made of Juiced parts into a single file when exporting\n\n" + super.getDescription();
  }

  /**
  **********************************************************************************************

  **********************************************************************************************
  **/
  @Override
  public void open(Resource source) {
    try {

      int fileOffset = (int) source.getOffset();

      readSource = new FileManipulator(source.getSource(), false);
      readSource.seek(source.getOffset());

      readLength = source.getDecompressedLength();

      readSource.seek(fileOffset);

      int firstPartOffset = readSource.readInt();
      int numParts = (firstPartOffset - fileOffset) / 8;

      readSource.seek(fileOffset);

      //Resource clone = (Resource) source.clone();

      offsets = new long[numParts];
      lengths = new long[numParts];
      for (int i = 0; i < numParts; i++) {
        // 4 - File Part Offset
        offsets[i] = readSource.readInt();

        // 4 - File Part Length
        lengths[i] = readSource.readInt();
      }

      readSource.seek(offsets[0]);
      partStream = new InflaterInputStream(new ManipulatorInputStream(readSource));

    }
    catch (Throwable t) {
    }
  }

  /**
   **********************************************************************************************
   * // TEST - NOT DONE!
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
      if (partStream.available() <= 0) {
        partNum++;
        if (partNum < offsets.length) {
          partStream.close();
          readSource.seek(offsets[partNum]);
          partStream = new InflaterInputStream(new ManipulatorInputStream(readSource));
        }
      }

      // TEST THIS LOGIC!
      int byteVal = partStream.read();
      if (partStream.available() <= 0) {
        // don't return this byte - it is the end of compression byte?
        return read();
      }
      return byteVal;
    }
    catch (Throwable t) {
      return 0;
    }
  }

}