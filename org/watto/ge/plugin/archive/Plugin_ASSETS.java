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
import org.watto.io.converter.IntConverter;

/**
**********************************************************************************************

**********************************************************************************************
**/
public class Plugin_ASSETS extends ArchivePlugin {

  /**
  **********************************************************************************************

  **********************************************************************************************
  **/
  public Plugin_ASSETS() {

    super("ASSETS", "ASSETS");

    //         read write replace rename
    setProperties(true, false, false, false);

    setGames("StuntMANIA!Jnr");
    setExtensions("assets"); // MUST BE LOWER CASE
    setPlatforms("PC");

    //setFileTypes("","",
    //             "",""
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

      fm.skip(4);

      long arcSize = fm.getLength();

      // Archive Size (BIG ENDIAN)
      if (IntConverter.changeFormat(fm.readInt()) == arcSize) {
        rating += 5;
      }

      // Version (5) (BIG ENDIAN)
      if (IntConverter.changeFormat(fm.readInt()) == 5) {
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

      // 4 - Unknown (BIG ENDIAN)
      // 4 - Archive Length (BIG ENDIAN)
      // 4 - Unknown (5) (BIG ENDIAN)
      // 4 - null

      // seek to the File ID of the last file in the directory
      fm.seek(arcSize - 77);

      // 4 - File ID (incremental from 1)
      int numFiles = fm.readInt();
      FieldValidator.checkNumFiles(numFiles);

      // seek to the directory start
      fm.seek(arcSize - 57 - (numFiles * 20));

      Resource[] resources = new Resource[numFiles];
      TaskProgressManager.setMaximum(numFiles);

      // Loop through directory
      boolean[] filenamesReal = new boolean[numFiles];
      for (int i = 0; i < numFiles; i++) {
        // 4 - File ID (incremental from 1)
        fm.skip(4);

        // 4 - File Offset
        int offset = fm.readInt();
        FieldValidator.checkOffset(offset, arcSize);

        // 4 - File Length
        int length = fm.readInt();
        FieldValidator.checkLength(length, arcSize);

        // 4 - Filename Checker
        int filenameReal = fm.readInt();
        filenamesReal[i] = filenameReal > 4;

        // 4 - Unknown
        fm.skip(4);

        //path,name,offset,length,decompLength,exporter
        resources[i] = new Resource(path, offset, length);

        TaskProgressManager.setValue(i);
      }

      // Get the filenames
      for (int i = 0; i < numFiles; i++) {
        Resource resource = resources[i];

        if (!filenamesReal[i]) {
          resource.setName(Resource.generateFilename(i));
          continue;
        }

        fm.seek(resource.getOffset());

        // 4 - Filename Length
        int filenameLength = fm.readInt();
        if (filenameLength == 0) {
          resource.setName(Resource.generateFilename(i));
          continue;
        }
        FieldValidator.checkFilenameLength(filenameLength);

        // X - Filename
        String filename = fm.readString(filenameLength);
        FieldValidator.checkFilename(filename);

        resource.setName(filename);
        resource.setOffset(fm.getOffset());
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
