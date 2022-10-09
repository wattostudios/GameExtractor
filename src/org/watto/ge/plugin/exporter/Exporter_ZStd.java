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
import io.airlift.compress.zstd.ZstdDecompressor;

public class Exporter_ZStd extends ExporterPlugin {

  static Exporter_ZStd instance = new Exporter_ZStd();

  byte[] decompBuffer = null;

  int decompPos = 0;

  int decompLength = 0;

  byte[] compBuffer = null;

  int compPos = 0;

  int compLength = 0;

  /**
  **********************************************************************************************
  ZStd / ZStandard
  Ref: https://github.com/airlift/aircompressor
  **********************************************************************************************
  **/
  public static Exporter_ZStd getInstance() {
    return instance;
  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public Exporter_ZStd() {
    setName("ZStd / ZStandard Compression");
  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  @Override
  public boolean available() {
    if (decompPos < decompLength) {
      return true;
    }
    else {
      return false;
    }
  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  @Override
  public void close() {
    decompBuffer = null;
    decompPos = 0;
    decompLength = 0;
  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  @Override
  public String getDescription() {
    return "This exporter decompresses ZStd / ZStandard files when exporting\n\n" + super.getDescription();
  }

  FileManipulator fm;

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  @Override
  public void open(Resource source) {
    try {
      FileManipulator fm = new FileManipulator(source.getSource(), false);
      fm.seek(source.getOffset());

      decompPos = 0;
      decompLength = (int) source.getDecompressedLength();
      decompBuffer = new byte[(int) decompLength];

      compPos = 0;
      compLength = (int) source.getLength();
      compBuffer = fm.readBytes(compLength);

      ZstdDecompressor decompressor = new ZstdDecompressor();
      decompressor.decompress(compBuffer, 0, compLength, decompBuffer, 0, decompLength);

      compBuffer = null; // discard the comp buffer now that we've finished the decompression

      // return the decompBuffer to the beginning, ready to read from
      decompPos = 0;

      fm.close();
    }
    catch (Throwable t) {
    }
  }

  /**
  **********************************************************************************************
  NOT DONE  
  **********************************************************************************************
  **/
  @Override
  public void pack(Resource source, FileManipulator destination) {
  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  @Override
  public int read() {
    try {
      byte currentByte = decompBuffer[decompPos];
      decompPos++;
      return currentByte;
    }
    catch (Throwable t) {
      return 0;
    }
  }

}