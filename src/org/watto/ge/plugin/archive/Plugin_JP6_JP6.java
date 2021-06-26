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
public class Plugin_JP6_JP6 extends ArchivePlugin {

  /**
  **********************************************************************************************

  **********************************************************************************************
  **/
  public Plugin_JP6_JP6() {

    super("JP6_JP6", "JP6_JP6");

    //         read write replace rename
    setProperties(true, false, false, false);

    setGames("Atlantis Evolution",
        "Echo: Secret of the Lost Cavern",
        "Return to Mystery Island",
        "The Crystal Key 2",
        "The Egyptian Prophecy");
    setExtensions("jp6");
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

      // Header
      if (fm.readString(4).equals("JPG" + (byte) 0)) {
        rating += 50;
      }

      // Unknown
      if (fm.readShort() == 1) {
        rating += 5;
      }

      // Unknown
      if (fm.readShort() == 2) {
        rating += 5;
      }

      // Unknown
      if (fm.readShort() == 768) {
        rating += 5;
      }

      // Unknown
      if (fm.readShort() == 768) {
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

      // RESETTING THE GLOBAL VARIABLES

      FileManipulator fm = new FileManipulator(path, false);

      long arcSize = (int) fm.getLength();

      // 4 - Header ("JP6" + null)
      // 2 - Unknown (1)
      // 2 - Unknown (2)
      // 2 - Unknown (768)
      // 2 - Unknown (768)
      // 2 - Unknown (1560)

      // 4 - Length Of First File Entry (16)
      fm.skip(18);

      // 4 - First File Offset
      int numFiles = (fm.readInt() - 14) / 16;
      FieldValidator.checkNumFiles(numFiles);

      fm.seek(14);

      Resource[] resources = new Resource[numFiles];

      TaskProgressManager.setMaximum(numFiles);

      // Loop through directory
      long[] offsets = new long[numFiles];
      for (int i = 0; i < numFiles; i++) {
        // 4 - Length Of Entry (16)
        fm.skip(4);

        // 4 - File Offset
        long offset = fm.readInt();
        FieldValidator.checkOffset(offset, arcSize);

        offsets[i] = offset;

        // 4 - File Length
        // 4 - File ID?
        fm.skip(8);
      }

      for (int i = 0; i < numFiles; i++) {
        long offset = offsets[i];
        fm.seek(offset);

        // 4 - File Extension
        String extension = fm.readNullString(4);
        FieldValidator.checkFilename(extension);

        String filename = Resource.generateFilename(i) + "." + extension;

        // 4 - File Length [-4]
        long length = fm.readInt() - 4;
        FieldValidator.checkLength(length);

        offset += 8;

        // X - File Data

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
