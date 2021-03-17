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
public class Plugin_NFS_NEFS extends ArchivePlugin {

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public Plugin_NFS_NEFS() {

    super("NFS_NEFS", "NFS_NEFS");

    //         read write replace rename
    setProperties(true, false, false, false);

    setGames("F1 2015",
        "F1 2018",
        "GRID Autosport");
    setExtensions("nfs"); // MUST BE LOWER CASE
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
      if (fm.readString(4).equals("NeFS")) {
        rating += 50;
      }

      fm.skip(104);

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

      // 4 - Header (NeFS)
      // 32 - Unknown
      // 64 - Unknown Hex Value
      // 4 - Unknown
      // 4 - Unknown
      fm.skip(108);

      // 4 - Number of Files
      int numFiles = fm.readInt();
      FieldValidator.checkNumFiles(numFiles);

      // 4 - null
      // 12 - Unknown
      // 4 - Unknown (1)
      // 4 - Unknown (4)
      fm.skip(24);

      // 4 - Directory 1 Offset (256)
      int dir1Offset = fm.readInt();
      FieldValidator.checkOffset(dir1Offset, arcSize);

      // 4 - Unknown
      fm.skip(4);

      // 4 - Directory 2 Offset
      int dir2Offset = fm.readInt();
      FieldValidator.checkOffset(dir2Offset, arcSize);

      // 4 - Unknown
      fm.skip(4);

      // 4 - Filename Directory Offset
      int filenameDirOffset = fm.readInt();
      FieldValidator.checkOffset(filenameDirOffset, arcSize);

      // 4 - Directory 3 Offset
      // 4 - Unknown
      // 4 - null
      // 88 - Unknown
      fm.seek(filenameDirOffset);

      Resource[] resources = new Resource[numFiles];
      TaskProgressManager.setMaximum(numFiles);

      //Read the filenames
      String[] names = new String[numFiles];

      //Loop through directory
      for (int i = 0; i < numFiles; i++) {
        // X - Filename (null)
        String filename = fm.readNullString();
        FieldValidator.checkFilename(filename);
        names[i] = filename;

        TaskProgressManager.setValue(i);
      }

      fm.seek(dir1Offset);

      //Read the offsets
      long[] offsets = new long[numFiles];

      //Loop through directory
      for (int i = 0; i < numFiles; i++) {
        //8 - File Offset
        long offset = fm.readLong();
        FieldValidator.checkOffset(offset, arcSize);
        offsets[i] = offset;

        //4 - Unknown ID
        //4 - Unknown ID
        // 4 - File ID (incremental from 0)
        fm.skip(12);

        TaskProgressManager.setValue(i);
      }

      fm.seek(dir2Offset);

      // Read the lengths

      // Loop through directory
      for (int i = 0; i < numFiles; i++) {
        // 4 - Grouping ID
        // 4 - Unknown ID
        // 4 - Unknown ID
        fm.skip(12);

        // 4 - File Length
        int length = fm.readInt();
        FieldValidator.checkLength(length, arcSize);

        // 4 - File ID (incremental from 0)
        fm.skip(4);

        long offset = offsets[i];
        String filename = names[i];

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
