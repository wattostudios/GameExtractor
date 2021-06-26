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
import org.watto.datatype.Resource;
import org.watto.ge.helper.FieldValidator;
import org.watto.ge.plugin.ArchivePlugin;
import org.watto.io.FileManipulator;
import org.watto.task.TaskProgressManager;

/**
**********************************************************************************************

**********************************************************************************************
**/
public class Plugin_CACHE_3 extends ArchivePlugin {

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public Plugin_CACHE_3() {

    super("CACHE_3", "CACHE_3");

    //         read write replace rename
    setProperties(true, false, false, false);

    setGames("The Darkness 2");
    setExtensions("cache"); // MUST BE LOWER CASE
    setPlatforms("PC");

    //setFileTypes(new FileType("txt", "Text Document", FileType.TYPE_DOCUMENT),
    //             new FileType("bmp", "Bitmap Image", FileType.TYPE_IMAGE)
    //             );

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

      getDirectoryFile(fm.getFile(), "toc");
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

      //ExporterPlugin exporter = Exporter_ZLib.getInstance();

      // RESETTING GLOBAL VARIABLES

      long arcSize = path.length();

      File sourcePath = getDirectoryFile(path, "toc");
      FileManipulator fm = new FileManipulator(sourcePath, false);

      // 4 - Header? ((bytes)78,198,103,24)
      // 4 - Version? (16)
      fm.skip(8);

      int numFiles = (int) ((sourcePath.length() - 8) / 96);
      FieldValidator.checkNumFiles(numFiles);

      Resource[] resources = new Resource[numFiles];
      TaskProgressManager.setMaximum(numFiles);

      // Loop through directory
      int realNumFiles = 0;
      String[] dirNames = new String[numFiles];
      int numDirs = 0;
      for (int i = 0; i < numFiles; i++) {

        // 8 - File Offset
        long offset = fm.readLong();
        if (offset == -1) {
          // a Directory

          // 8 - Dummy (-1)
          // 4 - Dummy (-1)
          // 8 - Dummy (-1)
          fm.skip(20);

          // 4 - Parent Directory ID (0 = no parent, 1 = the first directory, 2 = the second directory, ...)
          int parentID = fm.readInt();
          FieldValidator.checkRange(parentID, 0, numFiles);

          // 64 - Directory Name (null terminated, filled with nulls)
          String dirName = fm.readNullString(64);
          FieldValidator.checkFilename(dirName);

          if (parentID != 0) {
            dirName = dirNames[parentID - 1] + "\\" + dirName;
          }

          dirNames[numDirs] = dirName;
          numDirs++;
        }
        else {
          // a File

          FieldValidator.checkOffset(offset, arcSize);

          // 8 - Hash?
          fm.skip(8);

          // 4 - Compressed File Length
          int length = fm.readInt();
          FieldValidator.checkLength(length, arcSize);

          // 8 - Decompressed File Length
          long decompLength = fm.readLong();
          FieldValidator.checkLength(length, arcSize);

          // 4 - Parent Directory ID (0 = no parent, 1 = the first directory, 2 = the second directory, ...)
          int parentID = fm.readInt();
          FieldValidator.checkRange(parentID, 0, numFiles);

          // 64 - Filename (null terminated, filled with nulls)
          String filename = fm.readNullString(64);
          FieldValidator.checkFilename(filename);

          if (parentID != 0) {
            filename = dirNames[parentID - 1] + "\\" + filename;
          }

          //path,name,offset,length,decompLength,exporter
          resources[realNumFiles] = new Resource(path, filename, offset, length, decompLength);
          realNumFiles++;
        }

        TaskProgressManager.setValue(i);
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
