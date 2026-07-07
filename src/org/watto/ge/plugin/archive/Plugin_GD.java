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

import org.watto.datatype.Resource;
import org.watto.ge.helper.FieldValidator;
import org.watto.ge.plugin.ArchivePlugin;
import org.watto.io.FileManipulator;
import org.watto.task.TaskProgressManager;

/**
**********************************************************************************************

**********************************************************************************************
**/
public class Plugin_GD extends ArchivePlugin {

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public Plugin_GD() {

    super("GD", "GD");

    //         read write replace rename
    setProperties(true, false, false, false);

    setGames("Alien Nations: Amazons And Aliens");
    setExtensions("gd");
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

      // Archive Header Length
      if (fm.readInt() == 24) {
        rating += 5;
      }

      long arcSize = (int) fm.getLength();

      // Directory Offset
      if (FieldValidator.checkOffset(fm.readInt(), arcSize)) {
        rating += 5;
      }

      // Directory Offset
      if (FieldValidator.checkOffset(fm.readInt(), arcSize)) {
        rating += 5;
      }

      fm.skip(8);

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

      // 4 - Archive Header Length (24)
      fm.skip(4);

      // 4 - Filename Directory Offset
      int filenameDirOffset = fm.readInt();
      FieldValidator.checkOffset(filenameDirOffset, arcSize);

      // 4 - Filename Offsets Directory Offset
      int filenameOffsetDirOffset = fm.readInt();
      FieldValidator.checkOffset(filenameOffsetDirOffset, arcSize);

      // 4 - Files Directory Offset
      long dirOffset = fm.readInt();
      FieldValidator.checkOffset(dirOffset, arcSize);

      // 4 - File Data Offset
      int fileDataOffset = fm.readInt();
      FieldValidator.checkOffset(fileDataOffset, arcSize);

      // 4 - Number Of Files
      int numFiles = fm.readInt();
      FieldValidator.checkNumFiles(numFiles);

      fm.seek(filenameOffsetDirOffset);

      Resource[] resources = new Resource[numFiles];

      TaskProgressManager.setMaximum(numFiles);

      // Loop through directory
      int[] offsets = new int[numFiles];
      for (int i = 0; i < numFiles; i++) {
        // 4 - Filename Offset (relative to the start of the filename directory)
        int filenameOffset = fm.readInt() + filenameDirOffset;
        FieldValidator.checkOffset(filenameOffset, arcSize);
        offsets[i] = filenameOffset;
      }

      // Loop through directory
      String[] names = new String[numFiles];
      for (int i = 0; i < numFiles; i++) {
        fm.seek(offsets[i]);

        // X - Filename (null)
        String filename = fm.readNullString();
        FieldValidator.checkFilename(filename);
        names[i] = filename;
      }

      fm.seek(dirOffset);

      // Loop through directory
      for (int i = 0; i < numFiles; i++) {
        // 8 - Unknown (12)
        // 4 - File ID (decremental from numFiles-1)
        // 4 - Unknown (52)
        // 4 - Unknown
        // 4 - Unknown
        // 4 - Unknown
        // 4 - Unknown
        // 4 - Unknown
        // 4 - Unknown
        // 4 - Unknown
        // 4 - Unknown
        // 4 - Unknown
        // 2 - Unknown (4)
        // 2 - Unknown (4)
        fm.skip(56);

        // 8 - File Offset
        long offset = fm.readInt() + fileDataOffset;
        FieldValidator.checkOffset(offset, arcSize);

        fm.skip(4);

        String filename = names[i];

        //path,id,name,offset,length,decompLength,exporter
        resources[i] = new Resource(path, filename, offset);

        TaskProgressManager.setValue(i);
      }

      calculateFileSizesReverse(resources, arcSize);

      fm.close();

      return resources;

    }
    catch (Throwable t) {
      logError(t);
      return null;
    }
  }

}
