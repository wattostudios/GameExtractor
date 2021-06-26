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
import org.watto.ge.plugin.ExporterPlugin;
import org.watto.ge.plugin.exporter.Exporter_SLLZ;
import org.watto.io.FileManipulator;
import org.watto.io.converter.IntConverter;
import org.watto.task.TaskProgressManager;

/**
**********************************************************************************************

**********************************************************************************************
**/
public class Plugin_PAR_PARC extends ArchivePlugin {

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public Plugin_PAR_PARC() {

    super("PAR_PARC", "PAR_PARC");

    //         read write replace rename
    setProperties(true, false, false, false);

    setGames("Binary Domain");
    setExtensions("par"); // MUST BE LOWER CASE
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
      if (fm.readString(4).equals("PARC")) {
        rating += 50;
      }

      fm.skip(12);

      // Number Of Files
      if (FieldValidator.checkNumFiles(IntConverter.changeFormat(fm.readInt()))) {
        rating += 5;
      }

      long arcSize = fm.getLength();

      // Directory Offset
      if (FieldValidator.checkOffset(IntConverter.changeFormat(fm.readInt()), arcSize)) {
        rating += 5;
      }

      // Number Of Files
      if (FieldValidator.checkNumFiles(IntConverter.changeFormat(fm.readInt()))) {
        rating += 5;
      }

      // Directory Offset
      if (FieldValidator.checkOffset(IntConverter.changeFormat(fm.readInt()), arcSize)) {
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

      ExporterPlugin exporter = Exporter_SLLZ.getInstance();

      // RESETTING GLOBAL VARIABLES

      FileManipulator fm = new FileManipulator(path, false);

      long arcSize = fm.getLength();

      // 4 - Header (PARC)
      // 2 - Unknown (513)
      // 4 - Unknown (2)
      // 2 - Unknown (1)
      // 4 - null
      fm.skip(16);

      // 4 - Number of Folders
      int numDirs = IntConverter.changeFormat(fm.readInt());
      FieldValidator.checkNumFiles(numDirs);

      // 4 - Folder Details Directory Offset
      int dirDirOffset = IntConverter.changeFormat(fm.readInt());
      FieldValidator.checkOffset(dirDirOffset, arcSize);

      // 4 - Number of Files
      int numFiles = IntConverter.changeFormat(fm.readInt());
      FieldValidator.checkNumFiles(numFiles);

      // 4 - Details Directory Offset
      int dirOffset = IntConverter.changeFormat(fm.readInt());
      FieldValidator.checkOffset(dirOffset, arcSize);

      int numEntries = numFiles + numDirs;

      String[] names = new String[numEntries];
      for (int i = 0; i < numEntries; i++) {
        // 64 - Filename (or Folder Name) (null terminated, filled with nulls)
        String filename = fm.readNullString(64);
        FieldValidator.checkFilename(filename);
        names[i] = filename;
      }

      int[] numFilesInDirs = new int[numDirs];
      int[] firstFileInDirs = new int[numDirs];

      for (int i = 0; i < numDirs; i++) {
        // 4 - Unknown
        // 4 - Unknown
        fm.skip(8);

        // 4 - Number of Files in this Folder
        int numFilesInDir = IntConverter.changeFormat(fm.readInt());
        FieldValidator.checkRange(numFilesInDir, 0, numFiles);
        numFilesInDirs[i] = numFilesInDir;

        // 4 - ID Number of First File in this Folder
        int firstFileInDir = IntConverter.changeFormat(fm.readInt());
        FieldValidator.checkRange(firstFileInDir, 0, numFiles);
        firstFileInDirs[i] = firstFileInDir;

        // 4 - Unknown (16)
        // 12 - null
        fm.skip(16);
      }

      Resource[] resources = new Resource[numFiles];
      TaskProgressManager.setMaximum(numFiles);

      // Loop through directory
      int nameID = numDirs;
      for (int i = 0; i < numFiles; i++) {

        // 1 - Compression Flag (0=uncompressed, 1=compressed)
        // 3 - null
        fm.skip(4);

        // 4 - Decompressed File Length
        int decompLength = IntConverter.changeFormat(fm.readInt());
        FieldValidator.checkLength(decompLength);

        // 4 - Compressed File Length
        int length = IntConverter.changeFormat(fm.readInt());
        FieldValidator.checkLength(length, arcSize);

        // 4 - File Offset
        int offset = IntConverter.changeFormat(fm.readInt());
        FieldValidator.checkOffset(offset, arcSize);

        // 4 - Unknown
        // 8 - null
        // 4 - Unknown
        fm.skip(16);

        String filename = names[nameID++];

        //if (compressed == 1) {
        if (length < decompLength) {
          // Skip the compression header
          offset += 16;
          length -= 16;

          //path,name,offset,length,decompLength,exporter
          resources[i] = new Resource(path, filename, offset, length, decompLength, exporter);
        }
        else {
          //path,name,offset,length,decompLength,exporter
          resources[i] = new Resource(path, filename, offset, length, decompLength);
        }

        TaskProgressManager.setValue(i);
      }

      // Go through and set the directory names
      for (int i = 0; i < numDirs; i++) {
        String dirName = names[i];
        int firstFileInDir = firstFileInDirs[i];
        int lastFileInDir = numFilesInDirs[i] + firstFileInDir;

        for (int f = firstFileInDir; f < lastFileInDir; f++) {
          Resource resource = resources[f];
          String name = dirName + resource.getName();
          resource.setName(name);
          resource.setOriginalName(name);
        }
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
