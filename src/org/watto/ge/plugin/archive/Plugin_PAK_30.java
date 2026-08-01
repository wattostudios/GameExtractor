/*
 * Application:  Game Extractor
 * Author:       wattostudios
 * Website:      http://www.watto.org
 * Copyright:    Copyright (c) 2002-2026 wattostudios
 *
 * License Information:
 * This program is free software; you can redistribute it and/or modify it under the terms of the GNU General Public License
 * published by the Free Software Foundation; either version 2 of the License, or (at your option) any later versions. This
 * program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranties
 * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License at http://www.gnu.org for more
 * details. For further information on this application, refer to the authors' website.
 */
package org.watto.ge.plugin.archive;

import java.io.File;

import org.watto.datatype.Archive;
import org.watto.datatype.Resource;
import org.watto.ge.helper.FieldValidator;
import org.watto.ge.plugin.ArchivePlugin;
import org.watto.ge.plugin.exporter.Exporter_Custom_PAK_30;
import org.watto.io.FileManipulator;
import org.watto.io.buffer.ByteBuffer;
import org.watto.io.buffer.XORBufferWrapper;
import org.watto.io.converter.ByteConverter;
import org.watto.task.TaskProgressManager;

/**
**********************************************************************************************

**********************************************************************************************
**/
public class Plugin_PAK_30 extends ArchivePlugin {

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public Plugin_PAK_30() {

    super("PAK_30", "PAK_30");

    //         read write replace rename
    setProperties(true, false, false, false);

    setExtensions("pak");
    setGames("Rush For The Bomb");
    setPlatforms("PC");

  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  @Override
  public int getMatchRating(FileManipulator fm) {
    try {

      int rating = 0;

      if (FieldValidator.checkExtension(fm, extensions)) {
        rating += 25;
      }

      fm.skip(8);

      // Header (PACK)
      String header = fm.readString(4);
      if (header.equals("PACK") || header.equals("RFTB")) {
        rating += 50;
      }

      long arcSize = fm.getLength();

      // Directory Length
      if (FieldValidator.checkLength(fm.readInt(), arcSize)) {
        rating += 5;
      }

      return rating;

    }
    catch (Throwable t) {
      return 0;
    }
  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  @Override
  public Resource[] read(File path) {
    try {

      addFileTypes();

      FileManipulator fm = new FileManipulator(path, false);

      Exporter_Custom_PAK_30 exporter = Exporter_Custom_PAK_30.getInstance();

      long arcSize = fm.getLength();

      // 12 - Header (Sr + 26 27 13 10 135 10 + PACK)
      fm.skip(12);

      // 4 - Directory Length
      int dirLength = fm.readInt();
      FieldValidator.checkLength(dirLength, arcSize);

      int dataOffset = dirLength + 16;

      // DIRECTORY IS XOR WITH BYTE 66
      byte[] dirBytes = fm.readBytes(dirLength);

      fm.close();
      fm = new FileManipulator(new XORBufferWrapper(new ByteBuffer(dirBytes), 66));

      int numFiles = Archive.getMaxFiles(4);

      Resource[] resources = new Resource[numFiles];
      TaskProgressManager.setMaximum(dirLength);

      int realNumFiles = 0;
      while (fm.getOffset() < dirLength) {
        //System.out.println(fm.getOffset() + 16);

        // 1 - null
        fm.skip(1);

        // 1 - Filename Length (including null)
        int filenameLength = ByteConverter.unsign(fm.readByte()) - 1;
        FieldValidator.checkFilenameLength(filenameLength);

        // X - Filename (unknown encryption)
        // 1 - null Filename Terminator
        String filename = fm.readString(filenameLength);
        if (fm.readByte() == 0) {
          // ok
        }
        else {
          fm.skip(4); // another 3 bytes
        }

        // 4 - Offset
        int offset = fm.readInt() + dataOffset;
        FieldValidator.checkOffset(offset, arcSize);

        // 1 - Encryption Flags?
        fm.skip(1);

        // 4 - Length
        int length = fm.readInt();
        FieldValidator.checkLength(length, arcSize);

        if (length > 0) {
          //path,id,name,offset,length,decompLength,exporter
          resources[realNumFiles] = new Resource(path, filename, offset, length, length, exporter);

          TaskProgressManager.setValue((int) fm.getOffset());
          realNumFiles++;
        }
      }

      resources = resizeResources(resources, realNumFiles);

      fm.close();

      return resources;

    }
    catch (Throwable t) {
      logError(t);
      return null;
    }
  }

}