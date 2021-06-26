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
import org.watto.datatype.ReplacableResource;
import org.watto.datatype.Resource;
import org.watto.ge.helper.FieldValidator;
import org.watto.ge.plugin.ArchivePlugin;
import org.watto.io.FileManipulator;
import org.watto.task.TaskProgressManager;

/**
**********************************************************************************************

**********************************************************************************************
**/
public class Plugin_WAD_DWFB extends ArchivePlugin {

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public Plugin_WAD_DWFB() {

    super("WAD_DWFB", "WAD_DWFB");

    //         read write replace rename
    setProperties(true, false, false, false);
    setCanImplicitReplace(true);

    setExtensions("wad");
    setGames("Theme Park World",
        "Sim Theme Park",
        "Dungeon Keeper 2");
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
      if (fm.readString(4).equals("DWFB")) {
        rating += 50;
      }

      // Version
      if (fm.readInt() == 2) {
        rating += 5;
      }

      fm.skip(64);

      // Number Of Files
      if (FieldValidator.checkNumFiles(fm.readInt())) {
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
      // 4 - Header (DWFB)
      // 4 - version (2)
      // 64 - Filler
      fm.skip(72);

      // 4 - numFiles
      int numFiles = fm.readInt();
      FieldValidator.checkNumFiles(numFiles);

      long arcSize = fm.getLength();

      Resource[] resources = new Resource[numFiles];
      TaskProgressManager.setMaximum(numFiles);

      // 4 - Filename Offset
      // 4 - filename data size
      // 4 - Unknown
      fm.skip(12);

      int[] filenameOffsets = new int[numFiles];
      int[] filenameLengths = new int[numFiles];

      for (int i = 0; i < numFiles; i++) {
        // 4 - Unknown
        fm.skip(4);

        // 4 - Filename Offset
        filenameOffsets[i] = fm.readInt();
        FieldValidator.checkOffset(filenameOffsets[i], arcSize);

        // 4 - Filename Length
        filenameLengths[i] = fm.readInt();
        FieldValidator.checkFilenameLength(filenameLengths[i]);

        // 4 - Data Offset
        long offsetPointerLocation = fm.getOffset();
        long offsetPointerLength = 4;

        long offset = fm.readInt();

        // 4 - File Length
        long lengthPointerLocation = fm.getOffset();
        long lengthPointerLength = 4;

        long length = fm.readInt();

        // 20 - Unknown (filler?)
        fm.skip(20);

        //path,id,name,offset,length,decompLength,exporter
        resources[i] = new ReplacableResource(path, "", offset, offsetPointerLocation, offsetPointerLength, length, lengthPointerLocation, lengthPointerLength);

        TaskProgressManager.setValue(i);
      }

      // Set Filenames
      for (int j = 0; j < numFiles; j++) {
        fm.seek(filenameOffsets[j]);

        // X - Filename
        // 1 - null Filename Terminator
        String filename = fm.readString(filenameLengths[j] - 1);

        Resource resource = resources[j];
        resource.setName(filename);
        resource.setOriginalName(filename);
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