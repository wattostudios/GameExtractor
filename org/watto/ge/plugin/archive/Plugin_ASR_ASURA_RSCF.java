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
public class Plugin_ASR_ASURA_RSCF extends ArchivePlugin {

  /**
  **********************************************************************************************

  **********************************************************************************************
  **/
  public Plugin_ASR_ASURA_RSCF() {

    super("ASR_ASURA_RSCF", "ASR_ASURA_RSCF");

    //         read write replace rename
    setProperties(true, false, false, false);

    setGames("Alien vs Predator (2010)",
        "Sniper Elite");
    setExtensions("asr");
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
      if (fm.readString(8).equals("Asura   ")) {
        rating += 50;
      }

      // Header 2
      if (fm.readString(4).equals("RSCF")) {
        rating += 5;
      }

      long arcSize = fm.getLength();

      // First File Length
      if (FieldValidator.checkLength(fm.readInt(), arcSize)) {
        rating += 5;
      }

      // Version
      if (fm.readInt() == 1) {
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

      // 8 - Header 1 (Asura   )
      fm.skip(8);

      int numFiles = Archive.getMaxFiles();

      Resource[] resources = new Resource[numFiles];

      TaskProgressManager.setMaximum(arcSize);

      // Loop through directory
      int realNumFiles = 0;
      while (fm.getOffset() < arcSize) {
        // 4 - File Header
        String fileHeader = fm.readString(4);

        // 4 - Entry Length (including all these fields)
        int entryLength = fm.readInt() - 16;
        if (entryLength == -16) {
          break; // end of archive
        }
        FieldValidator.checkLength(entryLength, arcSize);

        // 4 - Version (1)
        // 4 - null
        fm.skip(8);

        if (fileHeader.equals("RSCF")) {
          // Resource File - a file in the archive

          // 4 - File Type ID?
          // 4 - File ID?
          fm.skip(8);

          // 4 - File Data Length
          long length = fm.readInt();
          FieldValidator.checkLength(length);

          // X - Filename
          // 1 - null Filename Terminator
          String filename = fm.readNullString();
          FieldValidator.checkFilename(filename);

          // 0-3 - null Padding to a multiple of 4 bytes
          int padding = 4 - ((filename.length() + 1) % 4);
          if (padding != 4) {
            fm.skip(padding);
          }

          // X - File Data
          long offset = fm.getOffset();
          fm.skip(length);

          //path,id,name,offset,length,decompLength,exporter
          resources[realNumFiles] = new Resource(path, filename, offset, length);
        }
        else {
          // some other kind of entry

          String filename = Resource.generateFilename(realNumFiles) + "." + fileHeader;

          long offset = fm.getOffset();
          long length = entryLength;
          fm.skip(length);

          //path,id,name,offset,length,decompLength,exporter
          resources[realNumFiles] = new Resource(path, filename, offset, length);
        }

        TaskProgressManager.setValue(fm.getOffset());
        realNumFiles++;
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
