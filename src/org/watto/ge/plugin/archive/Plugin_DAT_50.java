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
import org.watto.task.TaskProgressManager;

/**
**********************************************************************************************

**********************************************************************************************
**/
public class Plugin_DAT_50 extends ArchivePlugin {

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public Plugin_DAT_50() {

    super("DAT_50", "DAT_50");

    //         read write replace rename
    setProperties(true, false, false, false);

    setGames("Dance Dance Revolution SuperNOVA",
        "Dance Dance Revolution SuperNOVA 2",
        "Dance Dance Revolution X",
        "Dance Dance Revolution X2");
    setExtensions("dat"); // MUST BE LOWER CASE
    setPlatforms("PC", "PS2");

    setEnabled(false); // DOESN'T WORK - NEED TO ANALYSE FURTHER

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

      // 4 - Unknown
      // 4 - Unknown
      // 4 - Unknown
      fm.skip(12);

      // 4 - Number of Files?
      if (FieldValidator.checkNumFiles(fm.readInt())) {
        rating += 5;
      }

      // 4 - Unknown (8)
      if (fm.readInt() == 8) {
        rating += 5;
      }

      // 4 - Unknown (26)
      if (fm.readInt() == 26) {
        rating += 5;
      }

      // 4 - Unknown (20)
      if (fm.readInt() == 20) {
        rating += 5;
      }

      // 4 - Unknown (28)
      if (fm.readInt() == 28) {
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

      // 4 - Unknown
      // 4 - Unknown
      // 4 - Unknown
      fm.skip(12);

      // 4 - Number of Files?
      int numFiles = fm.readInt();
      FieldValidator.checkNumFiles(numFiles);

      // 4 - Unknown (8)
      // 4 - Unknown (26)
      // 4 - Unknown (20)
      // 4 - Unknown (28)
      // 2016 - null Padding to offset 2048
      fm.seek(2048);

      numFiles = Archive.getMaxFiles();

      Resource[] resources = new Resource[numFiles];
      TaskProgressManager.setMaximum(numFiles);

      // Loop through directory
      for (int i = 0; i < numFiles; i++) {

        // 4 - File Header (TGCD)
        String fileHeader = fm.readString(4);
        System.out.println(fileHeader + "\t\t" + (fm.getOffset() - 4));

        // 4 - Unknown
        fm.skip(4);

        // 4 - File Length (including all these header fields, but not including the padding)
        int length = fm.readInt() - 36;
        FieldValidator.checkLength(length, arcSize);

        // force length for "DTF" files
        boolean padding = true;
        if (fileHeader.substring(1).equals("DTF")) {
          length = 1020;
          padding = false;
        }

        // 4 - Unknown
        // 4 - Unknown
        // 4 - Unknown
        // 4 - Unknown
        // 4 - Unknown
        // 4 - Unknown
        fm.skip(24);

        // X - File Data
        long offset = fm.getOffset();
        fm.skip(length);

        // 0-2047 - null Padding to a multiple of 2048 bytes
        if (padding) {
          fm.skip(calculatePadding(fm.getOffset(), 2048));
        }

        String filename = Resource.generateFilename(i);

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
