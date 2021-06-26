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

package org.watto.ge.plugin.archive;

import java.io.File;
import org.watto.datatype.Archive;
import org.watto.datatype.Resource;
import org.watto.ge.helper.FieldValidator;
import org.watto.ge.plugin.ArchivePlugin;
import org.watto.io.FileManipulator;
import org.watto.task.TaskProgressManager;

/**
**********************************************************************************************

**********************************************************************************************
**/
public class Plugin_CPR_ASCARON extends ArchivePlugin {

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public Plugin_CPR_ASCARON() {

    super("CPR_ASCARON", "CPR_ASCARON");

    //         read write replace rename
    setProperties(true, false, false, false);

    setExtensions("cpr");
    setGames("Port Royale",
        "Patrician 2",
        "Patrician 3",
        "The Great Art Race");
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

      // Header
      if (fm.readString(16).equals("ASCARON_ARCHIVE ")) {
        rating += 50;
      }

      // Version
      if (fm.readString(4).equals("V0.9")) {
        rating += 5;
      }

      // null
      if (fm.readInt() == 0) {
        rating += 5;
      }

      fm.skip(16);

      // Number Of Files
      int numFiles = fm.readInt();
      if (FieldValidator.checkNumFiles(numFiles)) {
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

      // 16 - Header (ASCARON_ARCHIVE )
      // 4 - Version (V0.9)
      // 12 - null
      fm.skip(32);

      int numFiles = Archive.getMaxFiles();
      int realNumFiles = 0;

      Resource[] resources = new Resource[numFiles];
      TaskProgressManager.setMaximum(arcSize);

      // multiple blocks
      while (fm.getOffset() < arcSize) {
        long blockStart = fm.getOffset();

        // 4 - Directory Length (including Padding)
        int blockDirLength = fm.readInt();
        FieldValidator.checkLength(blockDirLength, arcSize);

        // 4 - Directory Length (not including padding)
        fm.skip(4);

        // 4 - Number Of Files
        int numBlockFiles = fm.readInt();
        FieldValidator.checkNumFiles(numBlockFiles);

        // 4 - File Data Length
        int blockDataLength = fm.readInt();
        FieldValidator.checkLength(blockDataLength, arcSize);

        for (int i = 0; i < numBlockFiles; i++) {
          // 4 - File Offset
          int offset = fm.readInt();
          FieldValidator.checkOffset(offset, arcSize);

          // 4 - File Length
          int length = fm.readInt();
          FieldValidator.checkLength(length, arcSize);

          // 4 - Unknown (1)
          fm.skip(4);

          // X - Filename
          // 1 - null Filename Terminator
          String filename = fm.readNullString();
          FieldValidator.checkFilename(filename);

          //path,id,name,offset,length,decompLength,exporter
          resources[realNumFiles] = new Resource(path, filename, offset, length);
          realNumFiles++;

          TaskProgressManager.setValue(offset);
        }

        fm.seek(blockStart + blockDirLength + blockDataLength);
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