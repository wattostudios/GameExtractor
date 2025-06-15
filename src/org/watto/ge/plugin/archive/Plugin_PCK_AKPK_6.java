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
public class Plugin_PCK_AKPK_6 extends ArchivePlugin {

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public Plugin_PCK_AKPK_6() {

    super("PCK_AKPK_6", "PCK_AKPK_6");

    //         read write replace rename
    setProperties(true, false, false, false);

    setGames("Dishonored");
    setExtensions("pck"); // MUST BE LOWER CASE
    setPlatforms("PC");

    // MUST BE LOWER CASE !!!
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

      // Header
      if (fm.readString(4).equals("AKPK")) {
        rating += 50;
      }

      long arcSize = fm.getLength();

      // Directory Length
      if (FieldValidator.checkLength(fm.readInt(), arcSize)) {
        rating += 5;
      }

      fm.skip(4);

      if (fm.readInt() == 20) {
        rating += 5;
      }

      fm.skip(56);

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

      //ExporterPlugin exporter = Exporter_ZLib.getInstance();

      // RESETTING GLOBAL VARIABLES

      FileManipulator fm = new FileManipulator(path, false);

      long arcSize = fm.getLength();

      // 4 - Header (AKPK)
      // 4 - Directory Length
      // 4 - Unknown (1)
      // 4 - File Entry Size? (20)
      // 4 - Size of Directory Header? [+??]
      // 4 - Unknown
      // 4 - Unknown (4)
      fm.skip(28);

      // 4 - Number of Folders
      int directoryCount = fm.readInt();
      FieldValidator.checkNumFiles(directoryCount);

      String[] dirNames = new String[directoryCount];
      int[] parentDirs = new int[directoryCount];
      for (int i = 0; i < directoryCount; i++) {
        // 4 - Folder Name Offset (relative to the start of the NumberOfFolders field)
        fm.skip(4);

        // 4 - Parent Folder ID? (0 = no parent folder)
        int parentDir = fm.readInt();
        FieldValidator.checkRange(parentDir, 0, directoryCount);

        parentDirs[i] = parentDir;
      }

      for (int i = 0; i < directoryCount; i++) {
        // X - Folder Name (unicode)
        // 2 - null Unicode Folder Name Terminator
        String directoryName = fm.readNullUnicodeString();
        FieldValidator.checkFilename(directoryName);

        dirNames[i] = directoryName;
      }

      // set the parent directory names
      for (int i = 0; i < directoryCount; i++) {
        int parentDir = parentDirs[i];
        if (parentDir != 0) {
          dirNames[i] = dirNames[parentDir] + "\\" + dirNames[i];
        }
      }

      fm.skip(24);

      // 4 - Number of Files
      int numFiles = fm.readInt();
      FieldValidator.checkNumFiles(numFiles);

      Resource[] resources = new Resource[numFiles];
      TaskProgressManager.setMaximum(numFiles);

      // Loop through directory
      for (int i = 0; i < numFiles; i++) {
        // 4 - Unknown
        // 4 - Unknown (1)
        fm.skip(8);

        // 4 - File Length
        int length = fm.readInt();
        FieldValidator.checkLength(length, arcSize);

        // 4 - File Offset
        int offset = fm.readInt();
        FieldValidator.checkOffset(offset, arcSize);

        // 4 - Folder ID?
        int directoryID = fm.readInt();
        FieldValidator.checkRange(directoryID, 0, directoryCount);

        String dirName = dirNames[directoryID];
        String filename = dirName + "\\" + Resource.generateFilename(i) + ".wav";

        //path,name,offset,length,decompLength,exporter
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
