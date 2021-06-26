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

import org.watto.ErrorLogger;
import org.watto.datatype.Resource;
import org.watto.ge.plugin.ExporterPlugin;
import org.watto.io.FileManipulator;
import org.watto.io.stream.ManipulatorInputStream;
import net.sourceforge.lhadecompressor.LhaDecoderInputStream;
import net.sourceforge.lhadecompressor.LhaEntry;

public class Exporter_LH6 extends ExporterPlugin {

  static Exporter_LH6 instance = new Exporter_LH6();

  static LhaDecoderInputStream readSource;

  static long readLength = 0;

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public static Exporter_LH6 getInstance() {
    return instance;
  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public Exporter_LH6() {
    setName("LHA LH6 Decompression");
  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  @Override
  public boolean available() {
    if (readLength <= 0) {
      return false;
    }
    return true;
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
    return "This exporter decompresses LHA LH6-Compressed files when exporting\n\n" + super.getDescription();
  }

  /**
  **********************************************************************************************
  So we can easily call this from within a Viewer plugin
  **********************************************************************************************
  **/
  public void open(FileManipulator fmIn, int compLengthIn, int decompLengthIn) {
    try {
      readSource = new LhaDecoderInputStream(new ManipulatorInputStream(fmIn), decompLengthIn, LhaEntry.METHOD_SIG_LH6);
      readLength = decompLengthIn;
    }
    catch (Throwable t) {
      ErrorLogger.log(t);
    }
  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  @Override
  public void open(Resource source) {
    try {
      FileManipulator fmIn = new FileManipulator(source.getSource(), false);
      fmIn.seek(source.getOffset());

      int compLengthIn = (int) source.getLength();
      int decompLengthIn = (int) source.getDecompressedLength();

      open(fmIn, compLengthIn, decompLengthIn);

    }
    catch (Throwable t) {
      ErrorLogger.log(t);
    }
  }

  /**
   **********************************************************************************************
   * NOT DONE
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
      readSource.available(); // to ensure it decompresses the next block, if needed.
      int readByte = readSource.read();
      readLength--;
      return readByte;
    }
    catch (Throwable t) {
      return 0;
    }
  }

}