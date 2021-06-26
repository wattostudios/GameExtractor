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
public class Plugin_BAG_GABA_2 extends ArchivePlugin {

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public Plugin_BAG_GABA_2() {

    super("BAG_GABA_2", "BAG_GABA_2");

    //         read write replace rename
    setProperties(true, false, false, false);

    setExtensions("bag");
    setGames("Dune 3",
        "NOX");
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

      getDirectoryFile(fm.getFile(), "idx");
      rating += 25;

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

      long arcSize = (int) path.length();

      File sourcePath = getDirectoryFile(path, "idx");

      FileManipulator fm = new FileManipulator(sourcePath, false);

      // 4 - Header (GABA)
      // 4 - Version (2)
      fm.skip(8);

      // 4 - Number Of Files
      int numFiles = fm.readInt();
      FieldValidator.checkNumFiles(numFiles);

      Resource[] resources = new Resource[numFiles];
      TaskProgressManager.setMaximum(numFiles);

      for (int i = 0; i < numFiles; i++) {
        // 16 - Filename (null terminated, filled with nulls)
        String filename = fm.readNullString(16);
        System.out.println(filename);

        // 4 - File Offset
        long offset = fm.readInt();
        FieldValidator.checkOffset(offset, arcSize);

        // 4 - File Length
        long length = fm.readInt();
        FieldValidator.checkLength(length, arcSize);

        // 4 - Audio Frequency (22050/44100)
        fm.skip(4);

        // 4 - File Type ID (2,6=*.w, 12,28=*.cmp, 37=*.mp3)
        int type = fm.readInt();

        // 4 - Padding Multiple? (512)
        fm.skip(4);

        if (type == 2 || type == 6) {
          filename += ".w";
        }
        else if (type == 12 || type == 28) {
          filename += ".cmp";
        }
        else if (type == 37) {
          filename += ".mp3";
        }
        else {
          filename += ".raw";
        }

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