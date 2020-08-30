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

import java.io.IOException;
import java.util.zip.DeflaterOutputStream;
import java.util.zip.InflaterInputStream;
import org.watto.datatype.Resource;
import org.watto.ge.plugin.ExporterPlugin;
import org.watto.io.FileManipulator;
import org.watto.io.stream.ManipulatorInputStream;
import org.watto.io.stream.ManipulatorOutputStream;

public class Exporter_Custom_PAK_20 extends ExporterPlugin {

  static Exporter_Custom_PAK_20 instance = new Exporter_Custom_PAK_20();

  static InflaterInputStream readSource;

  static long readLength = 0;

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public static Exporter_Custom_PAK_20 getInstance() {
    return instance;
  }

  FileManipulator readSourceNormal;

  FileManipulator fm;

  boolean readCompressed = true;

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public Exporter_Custom_PAK_20() {
    setName("The Movies PAK Compression");
  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  @Override
  public boolean available() {
    try {
      if (readCompressed) {
        if (readLength > 0 && readSource.available() > 0) {
          return true;
        }
        return false;
      }
      else {
        return readLength > 0;
      }
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
      if (readCompressed) {
        readSource.close();
        readSource = null;
      }
      else {
        readSourceNormal.close();
        readSourceNormal = null;
      }
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
    return "This exporter decompresses The Movies PAK files when exporting\n\n" + super.getDescription();
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

      boolean shortHeader = false;
      // 4 - Compressed File Length (including file data header fields and null padding)
      if (fm.readString(4).equals("zcmp")) {
        // this has a shortened compression header

        // 4 - Compressed File Length (including file data header fields and null padding) 
        fm.skip(4);

        shortHeader = true;
      }

      // 4 - Decompressed File Length
      int decompLength = fm.readInt();

      // 4 - Compressed File Length
      int length = fm.readInt();

      // 4 - null
      fm.skip(4);

      // X - File Data
      // 0-3 - null Padding to a multiple of 4 bytes
      long offset = fm.getOffset();

      // 4 - Compression 2 Header
      if (!shortHeader) {
        String comp2Head = fm.readString(4);
        if (comp2Head.equals("zcmp")) {
          // 4 - Compressed File Length (including file data header fields and null padding)
          fm.skip(4);

          // 4 - Decompressed File Length
          decompLength = fm.readInt();

          // 4 - Compressed File Length
          length = fm.readInt();

          // 4 - null
          fm.skip(4);

          // X - File Data
          // 0-3 - null Padding to a multiple of 4 bytes
          offset = fm.getOffset();
        }
        else {
          fm.seek(offset);
        }
      }

      if (decompLength != length) {
        //System.out.println("COMPRESSED");
        readSource = new InflaterInputStream(new ManipulatorInputStream(fm));
        readLength = decompLength;
        readCompressed = true;
      }
      else {
        readSourceNormal = fm;
        readLength = length;
        //System.out.println("NORMAL - " + length);
        readCompressed = false;
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
    DeflaterOutputStream outputStream = null;
    try {
      outputStream = new DeflaterOutputStream(new ManipulatorOutputStream(destination));

      ExporterPlugin exporter = source.getExporter();
      exporter.open(source);

      while (exporter.available()) {
        outputStream.write(exporter.read());
      }

      exporter.close();

      outputStream.finish();

    }
    catch (Throwable t) {
      logError(t);
      if (outputStream != null) {
        try {
          outputStream.finish();
        }
        catch (IOException e) {
        }
      }
    }
  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  @Override
  public int read() {
    try {
      if (readCompressed) {
        readLength--;
        return readSource.read();
      }
      else {
        readLength--;
        return readSourceNormal.readByte();
      }
    }
    catch (Throwable t) {
      return 0;
    }
  }

}