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
public class Plugin_RPK_RTPK extends ArchivePlugin {

  /**
  **********************************************************************************************

  **********************************************************************************************
  **/
  public Plugin_RPK_RTPK() {

    super("RPK_RTPK", "RPK_RTPK");

    //         read write replace rename
    setProperties(true, false, false, false);

    setGames("Rule of Rose");
    setExtensions("rpk"); // MUST BE LOWER CASE
    setPlatforms("PS2");

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

      // 4 - Header (RTPK)
      if (fm.readString(4).equals("RTPK")) {
        rating += 50;
      }

      long arcSize = fm.getLength();

      // 4 - Archive Length
      if (FieldValidator.checkEquals(fm.readInt(), arcSize)) {
        rating += 5;
      }

      fm.skip(4);

      // 2 - Padding Multiple (2048)
      if (fm.readShort() == 2048) {
        rating += 5;
      }

      // 2 - Number of Files
      if (FieldValidator.checkNumFiles(fm.readShort())) {
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

      // 4 - Header (RTPK)
      // 4 - Archive Length
      // 2 - null
      fm.skip(10);

      // 2 - Flags
      short flags = fm.readShort();

      // 2 - Padding Multiple (2048)
      fm.skip(2);

      // 2 - Number of Files
      short numFiles = fm.readShort();
      FieldValidator.checkNumFiles(numFiles);

      // 4 - Filename Directory Length (can be null if no names are stored)
      int filenameDirLength = fm.readInt();
      FieldValidator.checkLength(filenameDirLength, arcSize);

      // 12 - null
      fm.skip(12);

      Resource[] resources = new Resource[numFiles];
      TaskProgressManager.setMaximum(numFiles);

      boolean lengthsStored = false;
      int[] lengths = new int[numFiles];
      if ((flags & 1) == 1) {
        lengthsStored = true;

        for (int i = 0; i < numFiles; i++) {
          // 4 - File Length
          int length = fm.readInt();
          FieldValidator.checkLength(length, arcSize);
          lengths[i] = length;

          TaskProgressManager.setValue(i);
        }
      }

      int[] offsets = new int[numFiles];
      for (int i = 0; i < numFiles; i++) {
        // 4 - File Offset
        int offset = fm.readInt();
        FieldValidator.checkOffset(offset, arcSize);
        offsets[i] = offset;
        TaskProgressManager.setValue(i);
      }

      if ((flags & 16) == 16) {
        // for each file
        // 2 - Unknown
        fm.skip(numFiles * 2);
      }

      boolean namesStored = false;
      String[] names = new String[numFiles];
      if (filenameDirLength != 0) {
        namesStored = true;
        for (int i = 0; i < numFiles; i++) {
          // X - Filename (null)
          String filename = fm.readNullString();
          FieldValidator.checkFilename(filename);
          names[i] = filename;

          TaskProgressManager.setValue(i);
        }
      }

      // Loop through and create each file
      for (int i = 0; i < numFiles; i++) {
        int offset = offsets[i];

        int length = 0;
        if (lengthsStored) {
          length = lengths[i];
        }

        String filename = names[i];
        if (!namesStored) {
          filename = Resource.generateFilename(i);
        }

        //path,name,offset,length,decompLength,exporter
        resources[i] = new Resource(path, filename, offset, length);

        TaskProgressManager.setValue(i);
      }

      if (!lengthsStored) {
        calculateFileSizes(resources, arcSize);
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
