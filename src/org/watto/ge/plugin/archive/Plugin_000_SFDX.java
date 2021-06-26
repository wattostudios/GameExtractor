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
import org.watto.task.TaskProgressManager;
import org.watto.datatype.Resource;
import org.watto.ge.helper.FieldValidator;
import org.watto.ge.plugin.ArchivePlugin;
import org.watto.io.FileManipulator;

/**
**********************************************************************************************

**********************************************************************************************
**/
public class Plugin_000_SFDX extends ArchivePlugin {

  /**
  **********************************************************************************************

  **********************************************************************************************
  **/
  public Plugin_000_SFDX() {

    super("000_SFDX", "000_SFDX");

    //         read write replace rename
    setProperties(true, false, false, false);

    setGames("The Hobbit");
    setExtensions("000");
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

      getDirectoryFile(fm.getFile(), "dfs");
      rating += 25;

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

      long arcSize = (int) path.length();

      File sourcePath = getDirectoryFile(path, "dfs");

      FileManipulator fm = new FileManipulator(sourcePath, false);

      // 4 - Header (SFDX)
      // 4 - Number Of Folders (1)
      // 4 - Padding Multiple (2048)
      // 4 - Unknown
      fm.skip(16);

      // 4 - Number Of Files
      int numFiles = fm.readInt();
      FieldValidator.checkNumFiles(numFiles);

      // 4 - Folder Name Offset [-1] (relative to the start of the name directory)
      // 4 - Unknown (512)
      // 4 - Unknown (40)
      // 4 - Directory Offset (44)
      fm.skip(16);

      // 4 - Filename Directory Offset
      int filenameDirOffset = fm.readInt();
      FieldValidator.checkOffset(filenameDirOffset);

      // 4 - Unknown
      fm.skip(4);

      Resource[] resources = new Resource[numFiles];

      TaskProgressManager.setMaximum(numFiles);

      // Loop through directory
      int[] name1offsets = new int[numFiles];
      int[] name2offsets = new int[numFiles];
      for (int i = 0; i < numFiles; i++) {
        // 4 - Filename Part 1 Offset (relative to the start of the name directory)
        int name1 = fm.readInt() + filenameDirOffset;
        FieldValidator.checkOffset(name1, arcSize);
        name1offsets[i] = name1;

        // 4 - Filename Part 2 Offset (relative to the start of the name directory)
        int name2 = fm.readInt() + filenameDirOffset;
        FieldValidator.checkOffset(name2, arcSize);
        name2offsets[i] = name2;

        // 4 - null
        // 4 - Unknown (37)
        fm.skip(8);

        // 4 - File Offset
        long offset = fm.readInt();
        FieldValidator.checkOffset(offset, arcSize);

        // 4 - File Length
        long length = fm.readInt();
        FieldValidator.checkLength(length, arcSize);

        //path,id,name,offset,length,decompLength,exporter
        resources[i] = new Resource(path, "", offset, length);

        TaskProgressManager.setValue(i);
      }

      // Filenames
      fm.seek(filenameDirOffset);
      String dirName = fm.readNullString();

      for (int i = 0; i < numFiles; i++) {
        String filename = dirName;

        fm.seek(name1offsets[i]);
        filename += fm.readNullString();

        fm.seek(name2offsets[i]);
        filename += fm.readNullString();

        resources[i].setName(filename);
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
