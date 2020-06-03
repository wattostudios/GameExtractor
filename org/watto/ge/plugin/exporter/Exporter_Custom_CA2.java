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

import java.util.zip.DeflaterOutputStream;
import java.util.zip.InflaterInputStream;
import org.watto.datatype.Resource;
import org.watto.ge.plugin.ExporterPlugin;
import org.watto.io.FileManipulator;
import org.watto.io.stream.ManipulatorInputStream;
import org.watto.io.stream.ManipulatorUnclosableOutputStream;

public class Exporter_Custom_CA2 extends ExporterPlugin {

  static Exporter_Custom_CA2 instance = new Exporter_Custom_CA2();

  static InflaterInputStream readSource;
  static long readLength = 0;

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public static Exporter_Custom_CA2 getInstance() {
    return instance;
  }

  FileManipulator fm;

  int nextByte;

  /**
   **********************************************************************************************
   * ZLib decompressor where only the compressed size of a file is known
   **********************************************************************************************
   **/
  public Exporter_Custom_CA2() {
    setName("Forza MotorSport CA2 Compression");
  }

  /**
  **********************************************************************************************

  **********************************************************************************************
  **/
  @Override
  public boolean available() {
    try {
      nextByte = readSource.read();
      if (readSource.available() > 0) {
        return true;
      }
      //System.out.println("unavailable at " + fm.getOffset());

      long offset = fm.getOffset();
      if (offset < fm.getLength()) {
        // still more file
        int paddingSize = (int) (2048 - (offset % 2048));
        if (paddingSize != 2048) {
          fm.skip(paddingSize);
        }

        if (fm.getOffset() < fm.getLength()) {
          // continue;
          //System.out.println("restarting from " + fm.getOffset());
          readSource = new InflaterInputStream(new ManipulatorInputStream(fm));
          return available();
        }
      }

      return false;
    }
    catch (Throwable t) {
      //System.out.println("Trace");
      //t.printStackTrace();
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
      fm = new FileManipulator(source.getSource(), false);
      fm.seek(source.getOffset());

      readSource = new InflaterInputStream(new ManipulatorInputStream(fm));
      readLength = (int) source.getOffset() + source.getLength();
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
      ManipulatorUnclosableOutputStream fmOut = new ManipulatorUnclosableOutputStream(destination);
      DeflaterOutputStream outputStream = new DeflaterOutputStream(fmOut);
      long destinationStart = fmOut.getOffset();

      ExporterPlugin exporter = source.getExporter();
      exporter.open(source);

      int readBlock = 4194304; // 2048*2048
      while (exporter.available()) {
        outputStream.write(exporter.read());
        readBlock--;

        if (readBlock == 0) {
          outputStream.finish();
          outputStream.close();

          long destinationLength = fmOut.getOffset();// - destinationStart;

          //System.out.println("offset: " + fmOut.getOffset());

          // add the padding
          int paddingSize = (int) (2048 - (destinationLength % 2048));
          if (paddingSize != 2048) {
            for (int i = 0; i < paddingSize; i++) {
              fmOut.write(0);
            }
          }

          //System.out.println("next start: " + fmOut.getOffset());

          // start a new deflater
          readBlock = 4194304;
          outputStream = new DeflaterOutputStream(fmOut);
          destinationStart = fmOut.getOffset();
        }
      }

      exporter.close();

      outputStream.finish();
      outputStream.close();

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
      return nextByte;
    }
    catch (Throwable t) {
      return 0;
    }
  }

}