/*
 * Application:  Game Extractor
 * Author:       wattostudios
 * Website:      http://www.watto.org
 * Copyright:    Copyright (c) 2002-2022 wattostudios
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
import org.watto.io.converter.ShortConverter;
import org.watto.io.converter.StringConverter;
import org.watto.task.TaskProgressManager;

/**
**********************************************************************************************

**********************************************************************************************
**/
public class Plugin_DAT_92 extends ArchivePlugin {

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public Plugin_DAT_92() {

    super("DAT_92", "DAT_92");

    //         read write replace rename
    setProperties(true, false, false, false);

    setExtensions("dat");
    setGames("Prince Of Persia 2: The Shadow and The Flame");
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

      long arcSize = fm.getLength();

      // Directory Offset
      int dirOffset = fm.readInt();
      if (FieldValidator.checkOffset(dirOffset, arcSize)) {
        rating += 5;
      }

      // Directory Length
      int dirLength = ShortConverter.unsign(fm.readShort());
      if (dirLength + dirOffset == arcSize) {
        rating += 11; // slightly higher than DAT_11, as this fails better than DAT_11 does
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

      // 4 - Directory Offset
      int dirOffset = fm.readInt();

      // 2 - Directory Length
      fm.seek(dirOffset);

      // 2 - Number of Types
      int numTypes = fm.readShort();
      FieldValidator.checkNumFiles(numTypes);

      long arcSize = fm.getLength();

      int numFiles = Archive.getMaxFiles();
      int realNumFiles = 0;

      Resource[] resources = new Resource[numFiles];
      TaskProgressManager.setMaximum(numFiles);

      String[] types = new String[numTypes];
      int[] typeOffsets = new int[numTypes];
      for (int i = 0; i < numTypes; i++) {
        // 4 - Type String
        String type = StringConverter.reverse(fm.readNullString(4));

        // 2 - Offset in the Details Directory to the Files for this Type
        int typeOffset = dirOffset + fm.readShort();
        FieldValidator.checkOffset(typeOffset, arcSize);

        types[i] = type;
        typeOffsets[i] = typeOffset;
      }

      for (int i = 0; i < numTypes; i++) {
        fm.relativeSeek(typeOffsets[i]);

        // 2 - Number Of Files for this Type
        short numFilesOfType = fm.readShort();
        FieldValidator.checkNumFiles(numFilesOfType);

        String type = "." + types[i];

        for (int f = 0; f < numFilesOfType; f++) {

          // 2 - File ID
          fm.skip(2);

          // 4 - Data Offset
          long offset = fm.readInt() + 1; // +1 to skip the checksum byte at the start of each file
          FieldValidator.checkOffset(offset, arcSize);

          // 2 - File Length
          long length = fm.readShort();
          FieldValidator.checkLength(length, arcSize);

          // 2 - Unknown (64)
          // 1 - null
          fm.skip(3);

          String filename = Resource.generateFilename(realNumFiles) + type;

          //path,id,name,offset,length,decompLength,exporter
          resources[realNumFiles] = new Resource(path, filename, offset, length);
          realNumFiles++;

          TaskProgressManager.setValue(realNumFiles);
        }
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