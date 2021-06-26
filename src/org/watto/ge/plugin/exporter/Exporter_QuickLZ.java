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
import com.quicklz.QuickLZ;

public class Exporter_QuickLZ extends ExporterPlugin {

  static Exporter_QuickLZ instance = new Exporter_QuickLZ();

  byte[] decompBuffer = null;

  int decompPos = 0;

  int decompLength = 0;

  /**
  **********************************************************************************************
  Ref: https://pastebin.com/rGpBFwAV and QuickBMS
  **********************************************************************************************
  **/
  public static Exporter_QuickLZ getInstance() {
    return instance;
  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public Exporter_QuickLZ() {
    setName("EA LZ77 970 Compression");
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
    return "This exporter decompresses EA LZ77 970 files when exporting\n\n" + super.getDescription();
  }

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

      decompressFile(fm, (int) source.getLength());

      fm.close();
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

      // This doesn't actually compress anything - it just fakes it by creating a header which says that
      // all the remaining data should be read verbatim, and then copies in the full decompressed data.

      // Work out the header bytes required to cover the decompLength
      int firstBlock = 0;

      int decompLength = (int) source.getDecompressedLength();
      if (decompLength >= 15) {
        firstBlock = 15;
      }
      else {
        firstBlock = decompLength;
      }

      decompLength -= 15; // the first block has a max length of 15 bytes.

      int numFullBlocks = 0;
      int remainingLength = 0;

      if (decompLength > 0) {
        numFullBlocks = decompLength / 255;
        remainingLength = decompLength % 255;
      }

      // Now write the header bytes
      destination.writeByte(firstBlock << 4);

      for (int i = 0; i < numFullBlocks; i++) {
        destination.writeByte(255);
      }

      destination.writeByte(remainingLength);

      // now write the file data
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
      byte currentByte = decompBuffer[decompPos];
      decompPos++;
      return currentByte;
    }
    catch (Throwable t) {
      return 0;
    }
  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public void decompressFile(FileManipulator fm, int compLength) {
    try {
      byte[] compBytes = fm.readBytes(compLength);
      decompBuffer = QuickLZ.decompress(compBytes);
    }
    catch (Throwable t) {
      t.printStackTrace();
    }
  }

}