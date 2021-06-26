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
public class Plugin_RES_ILFF extends ArchivePlugin {

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public Plugin_RES_ILFF() {

    super("RES_ILFF", "RES_ILFF");

    //         read write replace rename
    setProperties(true, false, false, false);

    setGames("Project IGI: I'm Going In",
        "Project IGI 2");
    setExtensions("res");
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
      if (fm.readString(4).equals("ILFF")) {
        rating += 50;
      }

      // Archive Size
      if (fm.readInt() == fm.getLength()) {
        rating += 5;
      }

      fm.skip(8);

      // Header 2
      if (fm.readString(4).equals("IRES")) {
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
  
  **********************************************************************************************
  **/
  @Override
  public Resource[] read(File path) {
    try {

      addFileTypes();

      FileManipulator fm = new FileManipulator(path, false);

      // 4 - Header (ILFF)
      // 4 - Archive Size
      // 4 - Unknown (4)
      // 4 - null
      // 4 - Resources Header (IRES)
      fm.skip(20);

      // Some files have a 12-byte null here, others don't
      if (fm.readInt() == 0) {
        // 12 - null
        fm.skip(8);
      }
      else {
        fm.seek(20);
      }

      int numFiles = Archive.getMaxFiles(4);

      long arcSize = fm.getLength();

      Resource[] resources = new Resource[numFiles];
      TaskProgressManager.setMaximum(arcSize);

      // Loop through directory
      int realNumFiles = 0;
      while (fm.getOffset() < arcSize) {

        int startPos = (int) fm.getOffset();

        // 4 - Entry Header (NAME)
        fm.skip(4);
        //System.out.println(fm.readString(4));

        // 4 - Filename Length (including 1 null)
        int filenameLength = fm.readInt() - 1;
        if (filenameLength < 0) {
          filenameLength = 0;
        }
        //FieldValidator.checkFilenameLength(filenameLength);
        FieldValidator.checkRange(filenameLength, 0, 1000);

        // 4 - Unknown (4)
        fm.skip(4);

        // 4 - Offset to BODY header (relative to the start of this file entry)
        int bodyOffset = fm.readInt();

        // X - Filename (including "LOCAL:" or a drive letter)
        String filename = fm.readString(filenameLength);
        int dotPos = filename.indexOf(':') + 1;
        if (dotPos > 0 && dotPos < filename.length()) {
          filename = filename.substring(dotPos);
        }

        // 1 - null Filename Terminator
        // 0-3 - null Padding to a multiple of 4 bytes
        //int padding = 4-((filenameLength+1)%4);
        //fm.skip(padding);
        fm.seek(startPos + bodyOffset);

        // 4 - Body Header (BODY)
        String blockHeader = fm.readString(4);
        if (blockHeader.equals("PATH")) {
          // 4 - File Length (length of the FileData only)
          long length = fm.readInt();
          FieldValidator.checkLength(length, arcSize);

          // 4 - Unknown (4)
          // 4 - null
          fm.skip(8);

          // X - Path Data
          fm.skip(length);
          continue;
        }
        //System.out.println(fm.readString(4));

        // 4 - File Length (length of the FileData only)
        long length = fm.readInt();
        FieldValidator.checkLength(length, arcSize);

        // 4 - Unknown (4)
        fm.skip(4);

        // 4 - File Size (Including the BODY header and all fields after it, and the end-of-file padding)
        long bodySize = fm.readInt();
        FieldValidator.checkLength(bodySize, arcSize);

        // X - File Data
        // 0-3 - null Padding to a multiple of 4 bytes
        long offset = fm.getOffset();
        fm.seek(startPos + bodyOffset + bodySize);

        //path,id,name,offset,length,decompLength,exporter
        resources[realNumFiles] = new Resource(path, filename, offset, length);
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
