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
import org.watto.io.Hex;
import org.watto.io.converter.ByteArrayConverter;
import org.watto.io.converter.ByteConverter;

public class Exporter_Custom_MHTML_QuotedPrintable extends ExporterPlugin {

  static Exporter_Custom_MHTML_QuotedPrintable instance = new Exporter_Custom_MHTML_QuotedPrintable();

  static FileManipulator readSource;
  static long readLength = 0;

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public static Exporter_Custom_MHTML_QuotedPrintable getInstance() {
    return instance;
  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public Exporter_Custom_MHTML_QuotedPrintable() {
    setName("QuotedPrintable MHTML");
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
  public void open(Resource source) {
    try {
      readSource = new FileManipulator(source.getSource(), false);
      readSource.seek(source.getOffset());
      readLength = source.getLength();
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
    // TEMPORARY - This archive type cannot be saved anyway!
    Exporter_Default.getInstance().pack(source, destination);
  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  @Override
  public int read() {
    try {
      readLength--;
      int value = readSource.readByte();
      if (value == 61) {
        // equals sign

        int controlChar1 = ByteConverter.unsign(readSource.readByte());
        int controlChar2 = ByteConverter.unsign(readSource.readByte());

        readLength -= 2;

        if (controlChar1 == 13 && controlChar2 == 10) {
          // says that the current line data continues on the next line
          return read();
        }
        else {
          try {
            return (ByteArrayConverter.convertLittle(new Hex(new String(new byte[] { (byte) controlChar1, (byte) controlChar2 }))))[0];
          }
          catch (Throwable t) {
            t.printStackTrace();
            return read();
          }
        }

      }
      return value;
    }
    catch (Throwable t) {
      return 0;
    }
  }

}