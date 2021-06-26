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
public class Plugin_VOXPACK_VOXARCH1 extends ArchivePlugin {

  /**
  **********************************************************************************************

  **********************************************************************************************
  **/
  public Plugin_VOXPACK_VOXARCH1() {

    super("VOXPACK_VOXARCH1", "Sniper Fury VOXPACK Archive");

    //         read write replace rename
    setProperties(true, false, false, false);

    setGames("Sniper Fury");
    setExtensions("voxpack"); // MUST BE LOWER CASE
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

      // 8 - Header (Voxarch1)
      if (fm.readString(8).equals("Voxarch1")) {
        rating += 50;
      }

      // 4 - null
      // 4 - Unknown (2)
      fm.skip(8);

      // 4 - Number of Files
      if (FieldValidator.checkNumFiles(fm.readInt())) {
        rating += 5;
      }

      // 4 - Filename Names Directory Length
      // 4 - Files Directory Offset
      // 4 - Directory 3 Offset
      // 4 - Directory 2 Offset
      fm.skip(16);

      long arcSize = fm.getLength();

      // 4 - Filename Location Directory Offset
      if (FieldValidator.checkOffset(fm.readInt(), arcSize)) {
        rating += 5;
      }

      // 4 - Filename Names Directory Offset
      if (FieldValidator.checkOffset(fm.readInt(), arcSize)) {
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

      // 8 - Header (Voxarch1)
      // 4 - null
      // 4 - Unknown (2)
      fm.skip(16);

      // 4 - Number of Files
      int numFiles = fm.readInt();
      FieldValidator.checkNumFiles(numFiles);

      // 4 - Filename Names Directory Length
      fm.skip(4);

      // 4 - Files Directory Offset
      int filesDirOffset = fm.readInt();
      FieldValidator.checkOffset(filesDirOffset, arcSize);

      // 4 - Directory 3 Offset
      // 4 - Directory 2 Offset
      fm.skip(8);

      // 4 - Filename Location Directory Offset
      int filenameLocationDirOffset = fm.readInt();
      FieldValidator.checkOffset(filenameLocationDirOffset, arcSize);

      // 4 - Filename Names Directory Offset
      int filenamesDirOffset = fm.readInt();
      FieldValidator.checkOffset(filenamesDirOffset, arcSize);

      // 8 - Hash/Checksum/Timestamp?
      // 4 - Hashes Directory 1 Offset
      // 4 - Hashes Directory 2 Offset
      // 4 - Parent Directory Name Offset
      // 36 - Unknown
      // 4 - Number of Parent Directories? (1)
      // 24 - null

      // READ THE FILENAMES
      fm.seek(filenameLocationDirOffset);
      int[] filenameOffsets = new int[numFiles];
      for (int i = 0; i < numFiles; i++) {
        // 4 - Filename Offset (relative to the start of the Filename Names Directory)
        int filenameOffset = filenamesDirOffset + fm.readInt();
        FieldValidator.checkOffset(filenameOffset);

        filenameOffsets[i] = filenameOffset;
      }

      String[] filenames = new String[numFiles];
      for (int i = 0; i < numFiles; i++) {
        // If the filenames are in order, this seek should be instant, as we should already be at the correct spot in the file
        fm.seek(filenameOffsets[i]);

        // X - Filename
        // 1 - null Filename Terminator
        String filename = fm.readNullString();
        FieldValidator.checkFilename(filename);

        filenames[i] = filename;
      }

      // NOW READ THE MAIN DIRECTORY
      fm.seek(filesDirOffset);

      Resource[] resources = new Resource[numFiles];
      TaskProgressManager.setMaximum(numFiles);

      // Loop through directory
      for (int i = 0; i < numFiles; i++) {

        // 8 - Hash/Checksum/Timestamp?
        fm.skip(8);

        // 4 - File Length
        int length = fm.readInt();
        FieldValidator.checkLength(length, arcSize);

        // 4 - File Offset
        int offset = fm.readInt();
        FieldValidator.checkOffset(offset, arcSize);

        String filename = filenames[i];

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
