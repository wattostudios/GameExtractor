/*
 * Application:  Game Extractor
 * Author:       wattostudios
 * Website:      http://www.watto.org
 * Copyright:    Copyright (c) 2002-2021 wattostudios
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
import org.watto.datatype.FileType;
import org.watto.datatype.Resource;
import org.watto.ge.helper.FieldValidator;
import org.watto.ge.plugin.ArchivePlugin;
import org.watto.io.FileManipulator;
import org.watto.io.converter.ByteConverter;
import org.watto.task.TaskProgressManager;

/**
**********************************************************************************************

**********************************************************************************************
**/
public class Plugin_PAK_P5CK extends ArchivePlugin {

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public Plugin_PAK_P5CK() {

    super("PAK_P5CK", "PAK_P5CK");

    //         read write replace rename
    setProperties(true, false, false, false);

    setGames("TimeSplitters: Future Perfect");
    setExtensions("pak");
    setPlatforms("PC", "PS2", "GameCube");

    // MUST BE LOWER CASE !!!
    setFileTypes(new FileType("mib", "MIB Audio", FileType.TYPE_AUDIO),
        new FileType("vag", "VAG Audio", FileType.TYPE_AUDIO));

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
      if (fm.readString(4).equals("P5CK")) {
        rating += 50;
      }

      // Directory Offset
      if (FieldValidator.checkOffset(fm.readInt(), (int) fm.getLength())) {
        rating += 5;
      }

      // Directory Length
      if (FieldValidator.checkNumFiles(fm.readInt() / 16)) {
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

      FileManipulator fm = new FileManipulator(path, false);
      long arcSize = fm.getLength();

      // 4 - Header (P5CK)
      fm.skip(4);

      // 4 - Directory Offset?
      long dirOffset = fm.readInt();
      FieldValidator.checkOffset(dirOffset, arcSize);

      // 4 - Directory Length
      int numFiles = (fm.readInt() / 16) - 1;
      FieldValidator.checkNumFiles(numFiles);

      // 2036 - null Padding to offset 2048

      // first, see if this is a GameCube archive, which only has padding of 32 bytes
      fm.relativeSeek(32);
      if (fm.readShort() == 0) {
        // nope, not a GameCube
        fm.seek(2048);
      }
      else {
        // Yep, GameCube, go back to 32 bytes
        fm.relativeSeek(32);
      }

      // 2 - Number Of Files
      fm.skip(2);

      // Loop through directory
      String[] names = new String[numFiles];
      for (int i = 0; i < numFiles; i++) {
        // 1 - Filename Length
        int filenameLength = ByteConverter.unsign(fm.readByte());
        if (filenameLength == 0) {
          return null;
        }
        // X - Filename
        names[i] = fm.readString(filenameLength);
      }

      Resource[] resources = new Resource[numFiles];
      TaskProgressManager.setMaximum(numFiles);

      fm.seek(dirOffset + 16);

      // Loop through directory
      for (int i = 0; i < numFiles; i++) {

        // 4 - Unknown
        fm.skip(4);

        // 4 - Data Offset
        long offset = fm.readInt();
        FieldValidator.checkOffset(offset, arcSize);

        // 4 - File Size
        long length = fm.readInt();
        FieldValidator.checkLength(length, arcSize);

        // 4 - null
        fm.skip(4);

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
