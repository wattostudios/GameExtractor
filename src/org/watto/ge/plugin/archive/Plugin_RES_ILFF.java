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

import org.watto.ErrorLogger;
import org.watto.datatype.Archive;
import org.watto.datatype.FileType;
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

    // MUST BE LOWER CASE !!!
    setFileTypes(new FileType("tex", "Texture Image", FileType.TYPE_IMAGE),
        new FileType("spr", "Sprite Image", FileType.TYPE_IMAGE),
        new FileType("mef", "MEF Mesh", FileType.TYPE_MODEL));

    //setTextPreviewExtensions("colours", "rat", "screen", "styles"); // LOWER CASE

    //setCanScanForFileTypes(true);

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

      FileManipulator fm = new FileManipulator(path, false, 64); // small quick reads

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
      String name = null;
      while (fm.getOffset() < arcSize) {
        long startOffset = fm.getOffset();

        // 4 - Header
        String header = fm.readString(4);

        // 4 - Block Length
        int blockLength = fm.readInt();
        FieldValidator.checkLength(blockLength, arcSize);

        // 4 - Unknown (4)
        fm.skip(4);

        // 4 - Offset to the next Block (relative to the start of this entry)
        long nextOffset = fm.readInt();
        if (nextOffset == 0) {
          nextOffset = blockLength + 16;
          nextOffset += calculatePadding(nextOffset, 16);
        }
        nextOffset += startOffset;
        //FieldValidator.checkOffset(nextOffset, arcSize);

        // X - Block Data
        if (header.equals("NAME")) {
          // X - Filename
          // 1 - null Filename Terminator
          name = fm.readNullString(blockLength);
          FieldValidator.checkFilename(name);

          // 0-3 - null Padding to a multiple of 4 bytes
          fm.relativeSeek(nextOffset);
        }
        else if (header.equals("BODY")) {

          if (name == null) {
            name = Resource.generateFilename(realNumFiles);
          }

          // X - File Data
          // 0-3 - null Padding to a multiple of 4 bytes
          long offset = fm.getOffset();
          int length = blockLength;

          String filename = name;
          int dotPos = filename.indexOf(':');
          if (dotPos > 0) {
            filename = filename.substring(dotPos + 1);
          }

          //path,id,name,offset,length,decompLength,exporter
          resources[realNumFiles] = new Resource(path, filename, offset, length);
          realNumFiles++;

          TaskProgressManager.setValue(offset);

          fm.relativeSeek(nextOffset);

          name = null;
        }
        else {
          ErrorLogger.log("[RES_ILFF] Unknown block header: " + header);
          fm.relativeSeek(nextOffset);
        }

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
