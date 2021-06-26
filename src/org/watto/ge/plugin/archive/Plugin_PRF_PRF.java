/*
 * Application:  Game Extractor
 * Author:       wattostudios
 * Website:      http://www.watto.org
 * Copyright:    Copyright (c) 2002-2021 wattostudios
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
import org.watto.datatype.Resource;
import org.watto.ge.helper.FieldValidator;
import org.watto.ge.plugin.ArchivePlugin;
import org.watto.io.FileManipulator;
import org.watto.task.TaskProgressManager;

/**
**********************************************************************************************

**********************************************************************************************
**/
public class Plugin_PRF_PRF extends ArchivePlugin {

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public Plugin_PRF_PRF() {

    super("PRF_PRF", "PRF_PRF");

    //         read write replace rename
    setProperties(true, false, false, false);

    setExtensions("prf");
    setGames("Hoyle Casino 98",
        "Hoyle Casino 99");
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

      // Version
      if (fm.readInt() == 1) {
        rating += 5;
      }

      // Header
      String headerString = fm.readString(3);
      int headerByte = fm.readByte();
      if (headerString.equals("PRF") && headerByte == 0) {
        rating += 50;
      }

      fm.skip(8);

      long arcSize = fm.getLength();

      // Directory Length
      if (FieldValidator.checkLength(fm.readInt(), arcSize)) {
        rating += 5;
      }

      // Directory Offset
      if (FieldValidator.checkOffset(fm.readInt(), arcSize)) {
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
      long arcSize = fm.getLength();

      // 4 - Version (1)
      // 4 - Header (PRF) + null
      // 4 - Unknown (2)
      // 4 - Unknown (64)
      fm.skip(16);

      // 4 - Directory Length
      int dirLength = fm.readInt();
      FieldValidator.checkLength(dirLength, arcSize);

      // 4 - Directory Offset + 24
      long dirOffset = fm.readInt();
      FieldValidator.checkOffset(dirOffset, arcSize);

      int numFiles = 0;
      if (((dirLength - 24) % 16) == 0) {
        numFiles = (dirLength - 24) / 16 - 1;
        FieldValidator.checkNumFiles(numFiles);
        dirOffset += 24;
      }
      else {
        numFiles = (dirLength - 20) / 16 - 1;
        FieldValidator.checkNumFiles(numFiles);
        dirOffset += 20;
      }

      Resource[] resources = new Resource[numFiles];
      TaskProgressManager.setMaximum(numFiles);

      fm.seek(dirOffset);
      for (int i = 0; i < numFiles; i++) {
        // 4 - File Type
        String fileType = fm.readString(4);

        // 4 - Decompressed Length?
        fm.skip(4);
        //int decompLength = fm.readInt();
        //FieldValidator.checkLength(decompLength);

        // 4 - Offset
        long offset = fm.readInt() + 1;
        FieldValidator.checkOffset(offset, arcSize);

        // 4 - File Length
        long length = fm.readInt();
        FieldValidator.checkLength(length, arcSize);

        String filename = Resource.generateFilename(i) + "." + fileType;

        //path,id,name,offset,length,decompLength,exporter
        resources[i] = new Resource(path, filename, offset, length);

        TaskProgressManager.setValue(i);
      }

      fm.close();

      return resources;

    }
    catch (Throwable t) {
      logError(t);
      return null;
    }
  }

}