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
import org.watto.ErrorLogger;
import org.watto.datatype.Resource;
import org.watto.ge.helper.FieldValidator;
import org.watto.ge.plugin.ArchivePlugin;
import org.watto.ge.plugin.ExporterPlugin;
import org.watto.ge.plugin.exporter.Exporter_ZLib;
import org.watto.io.FileManipulator;
import org.watto.io.converter.ByteConverter;
import org.watto.task.TaskProgressManager;

/**
**********************************************************************************************

**********************************************************************************************
**/
public class Plugin_MJZ_MJZ0 extends ArchivePlugin {

  /**
  **********************************************************************************************

  **********************************************************************************************
  **/
  public Plugin_MJZ_MJZ0() {

    super("MJZ_MJZ0", "MJZ_MJZ0");

    //         read write replace rename
    setProperties(true, false, false, false);

    setGames("Luxor 2 HD",
        "Luxor Amun Rising HD",
        "Luxor Evolved",
        "Luxor HD");
    setExtensions("mjz"); // MUST BE LOWER CASE
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
      if (fm.readString(4).equals("MJZ0")) {
        rating += 50;
      }

      // version (2)
      if (fm.readInt() == 2) {
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
   * Reads an [archive] File into the Resources
   **********************************************************************************************
   **/
  @Override
  public Resource[] read(File path) {
    try {

      // NOTE - Compressed files MUST know their DECOMPRESSED LENGTH
      //      - Uncompressed files MUST know their LENGTH

      addFileTypes();

      ExporterPlugin exporter = Exporter_ZLib.getInstance();

      // RESETTING GLOBAL VARIABLES

      FileManipulator fm = new FileManipulator(path, false);

      long arcSize = fm.getLength();

      // 4 - Header (MJZ0)
      // 4 - Version (2)
      fm.skip(8);

      // 4 - Number of Files and Folders?
      int numFiles = fm.readInt();
      FieldValidator.checkNumFiles(numFiles);

      // 4 - Unknown
      fm.skip(4);

      Resource[] resources = new Resource[numFiles];
      TaskProgressManager.setMaximum(numFiles);

      // Loop through directory
      int realNumFiles = 0;
      for (int i = 0; i < numFiles; i++) {
        // 4 - Entry Type (1=Folder, 10=File)
        int entryType = fm.readInt();
        if (entryType == 1) {
          // folder

          // 12 - null
          fm.skip(12);

          // 1 - Filename Length (including null terminator)
          int filenameLength = ByteConverter.unsign(fm.readByte());

          // X - Filename
          // 1 - null Filename Terminator
          fm.skip(filenameLength);
        }
        else if (entryType == 10 || entryType == 0) {
          // file

          // 4 - File Offset
          int offset = fm.readInt();
          FieldValidator.checkOffset(offset, arcSize);

          // 4 - Decompressed File Length
          int decompLength = fm.readInt();
          FieldValidator.checkLength(decompLength);

          // 4 - Compressed File Length
          int length = fm.readInt();
          FieldValidator.checkLength(length, arcSize);

          // 1 - Filename Length (including null terminator)
          int filenameLength = ByteConverter.unsign(fm.readByte());

          // X - Filename
          String filename = fm.readString(filenameLength - 1);
          FieldValidator.checkFilename(filename);

          //System.out.println(fm.getOffset() + "\t" + filename);

          // 1 - null Filename Terminator
          fm.skip(1);

          //path,name,offset,length,decompLength,exporter
          if (decompLength != length) {
            resources[realNumFiles] = new Resource(path, filename, offset, length, decompLength, exporter);
          }
          else {
            resources[realNumFiles] = new Resource(path, filename, offset, length);
          }
          realNumFiles++;
        }
        else {
          ErrorLogger.log("[MJZ_MJZ0] Unknown entry type: " + entryType);
          return null;
        }

        TaskProgressManager.setValue(i);
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
