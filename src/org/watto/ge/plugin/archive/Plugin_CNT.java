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
public class Plugin_CNT extends ArchivePlugin {

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public Plugin_CNT() {

    super("CNT", "CNT");

    //         read write replace rename
    setProperties(true, false, false, false);
    setCanImplicitReplace(true);

    setGames("Rayman 2",
        "Rayman 3");
    setExtensions("cnt");
    setPlatforms("PC");

    setFileTypes("gf", "Graphics File");

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

      // Number Of Folders
      if (FieldValidator.checkNumFiles(fm.readInt())) {
        rating += 5;
      }

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

      long arcSize = fm.getLength();

      // 4 - Number Of Folders
      int numFolders = fm.readInt();
      FieldValidator.checkNumFiles(numFolders);

      // 4 - Number Of Files
      int numFiles = fm.readInt();
      FieldValidator.checkNumFiles(numFiles);

      Resource[] resources = new Resource[numFiles];
      TaskProgressManager.setMaximum(numFiles);

      // 2 - Unknown
      fm.skip(2);

      // 1 - XOR Value
      byte xorVal = fm.readByte();

      String[] dirNames = new String[numFolders];
      for (int i = 0; i < numFolders; i++) {
        // 4 - Folder Name Length
        int dirNameLength = fm.readInt();
        FieldValidator.checkFilenameLength(dirNameLength);

        // X - Encrypted Folder Name
        byte[] dirNameBytes = fm.readBytes(dirNameLength);
        for (int b = 0; b < dirNameLength; b++) {
          dirNameBytes[b] = (byte) (dirNameBytes[b] ^ xorVal);
        }
        String dirName = new String(dirNameBytes) + "\\";

        dirNames[i] = dirName;
      }

      // 1 - End Of Directory Marker (1)
      fm.skip(1);

      // Files Directory
      for (int i = 0; i < numFiles; i++) {

        // 4 - Directory ID
        int directoryID = fm.readInt();
        String dirName = null;
        if (directoryID == -1) {
          dirName = "";
        }
        else {
          FieldValidator.checkRange(directoryID, 0, numFolders);
          dirName = dirNames[directoryID];
        }

        // 4 - Filename Length
        int filenameLength = fm.readInt();
        FieldValidator.checkFilenameLength(filenameLength);

        // X - Filename
        byte[] filenameBytes = fm.readBytes(filenameLength);
        for (int b = 0; b < filenameLength; b++) {
          filenameBytes[b] = (byte) (filenameBytes[b] ^ xorVal);
        }
        String filename = new String(filenameBytes);

        filename = dirName + filename;

        // 4 - Unknown
        // 4 - Unknown
        fm.skip(8);

        // 4 - File Offset
        long offset = fm.readInt();
        FieldValidator.checkOffset(offset, arcSize);

        // 4 - File Length
        long length = fm.readInt();
        FieldValidator.checkLength(length, arcSize);

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
