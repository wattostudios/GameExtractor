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
public class Plugin_BF_CRYOBF extends ArchivePlugin {

  int realNumFiles = 0;

  /**
  **********************************************************************************************

  **********************************************************************************************
  **/
  public Plugin_BF_CRYOBF() {

    super("BF_CRYOBF", "BF_CRYOBF");

    //         read write replace rename
    setProperties(true, false, false, false);

    setGames("Return to Mystery Island",
        "Echo: Secret of the Lost Cavern",
        "Versailles 2",
        "Voyage");
    setExtensions("bf");
    setPlatforms("PC");

  }

  /**
  **********************************************************************************************

  **********************************************************************************************
  **/
  public void analyseDirectory(FileManipulator fm, File path, Resource[] resources, String dirName, int numFiles) throws Exception {
    long arcSize = fm.getLength();

    for (int i = 0; i < numFiles; i++) {
      // 4 - Filename Length
      int filenameLength = fm.readInt();

      // X - Filename
      String filename = fm.readString(filenameLength);

      // 4 - Entry Type (1=dir, 2=file)
      int entryType = fm.readInt();

      if (entryType == 1) {
        // Directory

        // 4 - Number Of Files In This Directory
        int numFilesInDir = fm.readInt();
        FieldValidator.checkNumFiles(numFilesInDir);

        analyseDirectory(fm, path, resources, dirName + filename + "\\", numFilesInDir);

      }
      else {
        // 4 - File Length
        long length = fm.readInt();
        FieldValidator.checkLength(length, arcSize);

        // 4 - null
        fm.skip(4);

        // 4 - Offset (relative to the first file offset)
        long offset = fm.readInt() + 32;
        FieldValidator.checkOffset(offset, arcSize);

        //path,id,name,offset,length,decompLength,exporter
        resources[realNumFiles] = new Resource(path, dirName + filename, offset, length);

        realNumFiles++;
        TaskProgressManager.setValue(offset);
      }

    }
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
      if (fm.readString(16).equals("CryoBF - 2.02.0" + (byte) 26)) {
        rating += 50;
      }

      // null
      if (fm.readLong() == 0) {
        rating += 5;
      }

      long arcSize = fm.getLength();

      // Directory Offset
      if (FieldValidator.checkOffset(fm.readInt(), arcSize)) {
        rating += 5;
      }

      // First File Offset (32)
      if (fm.readInt() == 32) {
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

      // RESETTING THE GLOBAL VARIABLES
      realNumFiles = 0;

      FileManipulator fm = new FileManipulator(path, false);

      long arcSize = fm.getLength();

      // 16 - Header (CryoBF - 2.02.0 + (byte)26)
      // 8 - null
      fm.skip(24);

      // 4 - Directory Offset
      long dirOffset = fm.readInt();
      FieldValidator.checkOffset(dirOffset, arcSize);

      // 4 - First File Offset (32)

      fm.seek(dirOffset);

      int numFiles = Archive.getMaxFiles(4);

      Resource[] resources = new Resource[numFiles];
      TaskProgressManager.setMaximum(arcSize);

      // 4 - Number of Directories
      int numDirs = fm.readInt();
      FieldValidator.checkNumFiles(numDirs);

      analyseDirectory(fm, path, resources, "", numDirs);

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
