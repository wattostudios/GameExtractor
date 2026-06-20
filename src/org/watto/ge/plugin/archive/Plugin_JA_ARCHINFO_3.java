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
public class Plugin_JA_ARCHINFO_3 extends ArchivePlugin {

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public Plugin_JA_ARCHINFO_3() {

    super("JA_ARCHINFO_3", "JA_ARCHINFO_3");

    //         read write replace rename
    setProperties(true, false, false, false);

    setGames("Trainz 1.0");
    setExtensions("ja");
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
      if (fm.readString(8).equals("ARCHINFO")) {
        rating += 50;
      }

      fm.skip(4);

      // Header
      if (fm.readString(4).equals("0CRA")) {
        rating += 5;
      }

      fm.skip(4);

      if (fm.readInt() == 0) {
        rating += 5;
      }
      else {
        rating -= 5;
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

      //ExporterPlugin exporter = Exporter_Custom_JA_ARCHINFO_CFIL.getInstance();

      // RESETTING THE GLOBAL VARIABLES

      FileManipulator fm = new FileManipulator(path, false, 300); // small quick reads

      long arcSize = (int) fm.getLength();

      // 8 - Header (ARCHINFO)
      // 4 - Unknown
      // 4 - Header 2 (0CRA)
      // 4 - Unknown
      // 24 - null
      fm.skip(44);

      int numFiles = Archive.getMaxFiles();
      Resource[] resources = new Resource[numFiles];

      TaskProgressManager.setMaximum(arcSize);

      // Loop through directory
      int realNumFiles = 0;
      while (fm.getOffset() < arcSize) {
        // 4 - File Header (FIL )
        // 4 - Length Of This File Entry (excluding these 2 fields, including all following fields)
        fm.skip(8);

        // 260 - Filename (null)
        String filename = fm.readNullString(260);
        FieldValidator.checkFilename(filename);

        int decompLength = 0;
        long length = 0;

        // 4 - Decompressed File Length
        decompLength = fm.readInt();
        FieldValidator.checkLength(decompLength);

        // 4 - Compressed File Length (starting from, and including, the CFIL Header)
        length = fm.readInt();
        FieldValidator.checkLength(length, arcSize);

        // 4 - Hash?
        // 12 - null
        // 4 - Unknown (128)
        fm.skip(20);

        long offset = fm.getOffset();

        // 4 - Compressed File Header (CFIL)
        if (fm.readString(4).equals("CFIL")) {
          offset = fm.getOffset();
          length -= 4;

          // X - Compressed File Data
          fm.skip(length);

          //path,id,name,offset,length,decompLength,exporter
          //resources[i] = new Resource(path, filename, offset, length, decompLength, exporter);
          resources[realNumFiles] = new Resource(path, filename, offset, length, decompLength);
        }
        else {
          // X - Raw File Data
          fm.skip(length - 4); // -4 because we already read 4 bytes for the check above

          //path,id,name,offset,length,decompLength,exporter
          resources[realNumFiles] = new Resource(path, filename, offset, length, decompLength);
        }
        realNumFiles++;

        TaskProgressManager.setValue(offset);
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
