/*
 * Application:  Game Extractor
 * Author:       wattostudios
 * Website:      http://www.watto.org
 * Copyright:    Copyright (c) 2002-2024 wattostudios
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
import org.watto.io.converter.IntConverter;
import org.watto.task.TaskProgressManager;

/**
**********************************************************************************************

**********************************************************************************************
**/
public class Plugin_RCF_ATGCORE_2 extends ArchivePlugin {

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public Plugin_RCF_ATGCORE_2() {

    super("RCF_ATGCORE_2", "RCF_ATGCORE_2");

    //         read write replace rename
    setProperties(true, false, false, false);

    setGames("The Incredible Hulk: Ultimate Destruction");
    setExtensions("rcf");
    setPlatforms("GameCube");

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
      if (fm.readString(23).equals("ATG CORE CEMENT LIBRARY")) {
        rating += 50;
      }

      fm.skip(13);

      // Directory Offset
      if (IntConverter.changeFormat(fm.readInt()) == 60) {
        rating += 5;
      }

      long arcSize = fm.getLength();

      // Directory Length
      if (FieldValidator.checkLength(IntConverter.changeFormat(fm.readInt()), arcSize)) {
        rating += 5;
      }

      fm.skip(12);

      // Number Of Files
      if (FieldValidator.checkNumFiles(IntConverter.changeFormat(fm.readInt()))) {
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
   * Reads an [archive] File into the Resources
   **********************************************************************************************
   **/
  @Override
  public Resource[] read(File path) {
    try {

      // NOTE - Compressed files MUST know their DECOMPRESSED LENGTH
      //      - Uncompressed files MUST know their LENGTH

      addFileTypes();

      // RESETTING THE GLOBAL VARIABLES

      FileManipulator fm = new FileManipulator(path, false);

      long arcSize = (int) fm.getLength();

      // 32 - Header ("ATG CORE CEMENT LIBRARY" + nulls to fill)
      // 4 - Unknown
      fm.skip(36);

      // 4 - Directory Offset (60)
      long dirOffset = IntConverter.changeFormat(fm.readInt());
      FieldValidator.checkOffset(dirOffset, arcSize);

      // 4 - Directory Length
      fm.skip(4);

      // 4 - Offset To Filename Directory
      int filenameDirOffset = IntConverter.changeFormat(fm.readInt());
      FieldValidator.checkOffset(filenameDirOffset, arcSize);

      // 4 - Filename Directory Length
      // 4 - null
      fm.skip(8);

      // 4 - Number Of Files
      int numFiles = IntConverter.changeFormat(fm.readInt());
      FieldValidator.checkNumFiles(numFiles);

      fm.seek(filenameDirOffset + 8);

      // Loop through directory
      String[] names = new String[numFiles];
      for (int i = 0; i < numFiles; i++) {
        // 4 - Unknown
        // 4 - File Data Padding Multiple (2048)
        // 4 - null
        fm.skip(12);

        // 4 - Filename Length (including null terminator)
        int filenameLength = fm.readInt() - 1;
        FieldValidator.checkFilenameLength(filenameLength);

        // X - Filename
        names[i] = fm.readString(filenameLength);

        // 4 nulls
        fm.skip(4);

      }

      Resource[] resources = new Resource[numFiles];

      TaskProgressManager.setMaximum(numFiles);

      fm.seek(dirOffset);

      // Loop through directory
      for (int i = 0; i < numFiles; i++) {
        // 4 - Hash?
        fm.skip(4);

        // 4 - File Offset
        long offset = IntConverter.changeFormat(fm.readInt());
        FieldValidator.checkOffset(offset, arcSize);

        // 4 - File Length
        long length = IntConverter.changeFormat(fm.readInt());
        FieldValidator.checkLength(length, arcSize);

        String filename = names[i];

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
