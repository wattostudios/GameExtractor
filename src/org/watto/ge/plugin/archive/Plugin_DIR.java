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
import org.watto.component.WSPluginManager;
import org.watto.datatype.Archive;
import org.watto.datatype.Resource;
import org.watto.ge.helper.FieldValidator;
import org.watto.ge.plugin.ArchivePlugin;
import org.watto.ge.plugin.ViewerPlugin;
import org.watto.io.FileManipulator;
import org.watto.io.converter.ByteConverter;
import org.watto.task.TaskProgressManager;

/**
**********************************************************************************************

**********************************************************************************************
**/
public class Plugin_DIR extends ArchivePlugin {

  int i = 0;

  /**
  **********************************************************************************************

  **********************************************************************************************
  **/
  public Plugin_DIR() {

    super("DIR", "DIR");

    //         read write replace rename
    setProperties(true, false, false, false);

    setExtensions("dir");
    setGames("Commandos: Behind Enemy Lines",
        "Commandos: Beyond the Call of Duty");
    setPlatforms("PC");

  }

  /**
  **********************************************************************************************

  **********************************************************************************************
  **/
  public void analyseDirectory(FileManipulator fm, File path, Resource[] resources, String dirName, long offset) throws Exception {
    long arcSize = fm.getLength();

    fm.seek(offset);

    boolean next = true;
    int readLength = 0;
    while (next) {

      // 32 - Filename
      String subfilename = fm.readNullString(32);
      FieldValidator.checkFilename(subfilename);

      // 1 - Entry Type ID (1)
      int entryType = ByteConverter.unsign(fm.readByte());

      // 3 - Padding (all (byte)205)
      fm.skip(3);

      if (entryType == 255) {
        // End Of Directory Marker File
        next = false;

        // 4 - Padding (-1)
        // 4 - Padding (-1)
        fm.skip(8);
      }
      else {

        // 4 - File Length
        int sublength = fm.readInt();
        FieldValidator.checkLength(sublength, arcSize);

        // 4 - Data Offset
        int suboffset = fm.readInt();
        FieldValidator.checkOffset(suboffset, arcSize);

        if (entryType == 1) {
          // Directory
          int currentPos = (int) fm.getOffset();
          analyseDirectory(fm, path, resources, dirName + subfilename + "\\", suboffset);
          fm.seek(currentPos);
        }
        else {
          // File

          //path,id,name,offset,length,decompLength,exporter
          resources[i] = new Resource(path, dirName + subfilename, suboffset, sublength);

          TaskProgressManager.setValue(readLength);
          readLength += sublength;
          i++;
        }

      }

    }

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

      fm.skip(31);

      // 32 filename - check for null
      if (fm.readByte() == 0) {
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
   Provide hints to the previewer so that certain document types are displayed appropriately
   **********************************************************************************************
   **/
  @Override
  public ViewerPlugin previewHint(Resource resource) {
    String extension = resource.getExtension();
    if (extension.equalsIgnoreCase("anm") || extension.equalsIgnoreCase("dat") || extension.equalsIgnoreCase("mac") || extension.equalsIgnoreCase("mis") || extension.equalsIgnoreCase("pol") || extension.equalsIgnoreCase("scr") || extension.equalsIgnoreCase("sec") || extension.equalsIgnoreCase("str") || extension.equalsIgnoreCase("tip") || extension.equalsIgnoreCase("vol") || extension.equalsIgnoreCase("til")) {
      return (ViewerPlugin) WSPluginManager.getPlugin("Viewer", "TXT");
    }
    return null;
  }

  /**
  **********************************************************************************************

  **********************************************************************************************
  **/
  @Override
  public Resource[] read(File path) {
    try {

      addFileTypes();

      // RESETTING THE GLOBAL VARIABLES
      i = 0;

      FileManipulator fm = new FileManipulator(path, false);

      int numFiles = Archive.getMaxFiles(4);

      long arcSize = fm.getLength();

      Resource[] resources = new Resource[numFiles];
      TaskProgressManager.setMaximum(arcSize);

      analyseDirectory(fm, path, resources, "", 0);

      resources = resizeResources(resources, i);

      fm.close();

      return resources;

    }
    catch (Throwable t) {
      logError(t);
      return null;
    }
  }

}