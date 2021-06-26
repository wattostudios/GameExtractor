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
import org.watto.io.converter.IntConverter;
import org.watto.io.converter.LongConverter;
import org.watto.task.TaskProgressManager;

/**
**********************************************************************************************

**********************************************************************************************
**/
public class Plugin_SND_SND2 extends ArchivePlugin {

  /**
  **********************************************************************************************

  **********************************************************************************************
  **/
  public Plugin_SND_SND2() {

    super("SND_SND2", "SND_SND2");

    //         read write replace rename
    setProperties(true, false, false, false);

    setGames("Marathon 2",
        "Marathon Infinity",
        "Zero Population Count: No Flesh Shall Be Spared");
    setExtensions("snd", "snda");
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

      // Version (1)
      if (IntConverter.changeFormat(fm.readInt()) == 1) {
        rating += 5;
      }

      // Header
      if (fm.readString(4).equals("snd2")) {
        rating += 50;
      }

      fm.skip(4);

      // null
      if (LongConverter.changeFormat(fm.readLong()) == 0) {
        rating += 5;
      }

      // null
      if (LongConverter.changeFormat(fm.readLong()) == 0) {
        rating += 5;
      }

      // null
      if (LongConverter.changeFormat(fm.readLong()) == 0) {
        rating += 5;
      }

      // null
      if (LongConverter.changeFormat(fm.readLong()) == 0) {
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

      // 4 - Version (1)
      // 4 - Header (snd2)
      // 4 - Unknown
      // 246 - null
      fm.skip(258);

      int numFiles = Archive.getMaxFiles(4);

      long arcSize = fm.getLength();

      Resource[] resources = new Resource[numFiles];
      TaskProgressManager.setMaximum(arcSize);

      // Loop through directory
      boolean keepGoing = true;
      int realNumFiles = 0;
      while (keepGoing) {
        // 4 - File ID (incrementing by +10, starting at 1000)
        int fileID = IntConverter.changeFormat(fm.readInt());

        if (fileID == 0) {
          keepGoing = false;
        }
        else {

          // 2 - Stereo/Mono? (1)
          // 2 - Bitrate? (32)
          // 8 - null
          fm.skip(12);

          // 4 - File Type (0=?, 1=File, 4=?)
          int fileType = IntConverter.changeFormat(fm.readInt());

          // 2 - null
          fm.skip(2);

          // 4 - File Offset
          long offset = IntConverter.changeFormat(fm.readInt());
          FieldValidator.checkOffset(offset, arcSize);

          // 4 - File Size
          long length = IntConverter.changeFormat(fm.readInt());
          FieldValidator.checkLength(length, arcSize);

          // 4 - File Size
          // 30 - null
          fm.skip(34);

          if (fileType == 1) {
            String filename = Resource.generateFilename(realNumFiles);

            //path,id,name,offset,length,decompLength,exporter
            resources[realNumFiles] = new Resource(path, filename, offset, length);

            TaskProgressManager.setValue(offset);
            realNumFiles++;
          }

        }

      }

      fm.close();

      resources = resizeResources(resources, realNumFiles);

      return resources;

    }
    catch (Throwable t) {
      logError(t);
      return null;
    }
  }

}
