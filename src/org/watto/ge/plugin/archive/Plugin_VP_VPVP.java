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
public class Plugin_VP_VPVP extends ArchivePlugin {

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public Plugin_VP_VPVP() {

    super("VP_VPVP", "VP_VPVP");

    //         read write replace rename
    setProperties(true, false, false, false);

    setExtensions("vp");
    setGames("Descent Freespace: The Great War",
        "Freespace 2");
    setPlatforms("PC");

    setFileTypes("tbl", "Table Data?",
        "fsm", "Demo File",
        "p3d", "3D Model?",
        "pof", "3D Image?",
        "vf", "Font Image",
        "ani", "Animation");

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
      if (fm.readString(4).equals("VPVP")) {
        rating += 50;
      }

      // Version
      if (fm.readInt() == 2) {
        rating += 5;
      }

      long arcSize = fm.getLength();

      // Directory Offset
      if (FieldValidator.checkOffset(fm.readInt(), arcSize)) {
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
  
  **********************************************************************************************
  **/
  @Override
  public Resource[] read(File path) {
    try {

      addFileTypes();

      FileManipulator fm = new FileManipulator(path, false);

      long arcSize = fm.getLength();

      // 4 - Header (VPVP)
      // 4 - Version (2)
      fm.skip(8);

      // 4 - dirOffset
      long dirOffset = fm.readInt();
      FieldValidator.checkOffset(dirOffset, arcSize);

      // 4 - numFiles
      int numFiles = fm.readInt();
      FieldValidator.checkNumFiles(numFiles);

      Resource[] resources = new Resource[numFiles];
      TaskProgressManager.setMaximum(numFiles);

      fm.seek(dirOffset);
      int realNumFiles = 0;
      String dirName = "";
      for (int i = 0; i < numFiles; i++) {
        // 4 - File Offset
        long offset = fm.readInt();
        FieldValidator.checkOffset(offset, arcSize);

        // 4 - File Length (0=directory, #=file)
        long length = fm.readInt();
        FieldValidator.checkLength(length + 1, arcSize);

        // 32 - Filename (null)
        String filename = fm.readNullString(32);
        FieldValidator.checkFilename(filename);

        // 4 - Type Code? (0=directory)
        fm.skip(4);

        if (length > 0) {
          filename = dirName + filename;

          //path,id,name,offset,length,decompLength,exporter
          resources[realNumFiles] = new Resource(path, filename, offset, length);
          realNumFiles++;
        }
        else {
          // directory
          if (filename.equals("..")) {
            // go back a directory
            int slashPos = dirName.lastIndexOf("/", dirName.length() - 2);
            if (slashPos > 0) {
              dirName = dirName.substring(0, slashPos + 1);
            }
          }
          else {
            dirName += filename + "/";
          }
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