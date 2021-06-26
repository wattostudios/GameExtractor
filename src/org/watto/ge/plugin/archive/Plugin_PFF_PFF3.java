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
import org.watto.datatype.Resource;
import org.watto.ge.helper.FieldValidator;
import org.watto.ge.plugin.ArchivePlugin;
import org.watto.ge.plugin.ViewerPlugin;
import org.watto.io.FileManipulator;
import org.watto.task.TaskProgressManager;

/**
**********************************************************************************************

**********************************************************************************************
**/
public class Plugin_PFF_PFF3 extends ArchivePlugin {

  /**
  **********************************************************************************************

  **********************************************************************************************
  **/
  public Plugin_PFF_PFF3() {

    super("PFF_PFF3", "PFF_PFF3");

    //         read write replace rename
    setProperties(true, false, false, false);

    setGames("Armored Fist 3",
        "Delta Force",
        "F-16 Multirole Fighter",
        "F-22 Lightning 3",
        "MiG-29 Fulcrum",
        "Tachyon: The Fringe");
    setExtensions("pff");
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

      // First File Offset
      if (fm.readInt() == 20) {
        rating += 5;
      }

      // Header
      if (fm.readString(4).equals("PFF3")) {
        rating += 50;
      }

      // Number Of Files
      int numFiles = fm.readInt();
      if (FieldValidator.checkNumFiles(numFiles)) {
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
    if (extension.equalsIgnoreCase("scr") || extension.equalsIgnoreCase("ver") || extension.equalsIgnoreCase("mnu") || extension.equalsIgnoreCase("job") || extension.equalsIgnoreCase("hud") || extension.equalsIgnoreCase("box") || extension.equalsIgnoreCase("anm") || extension.equalsIgnoreCase("h") || extension.equalsIgnoreCase("i") || extension.equalsIgnoreCase("trn") || extension.equalsIgnoreCase("ai") || extension.equalsIgnoreCase("bat") || extension.equalsIgnoreCase("lst") || extension.equalsIgnoreCase("snd") || extension.equalsIgnoreCase("key")) {
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

      FileManipulator fm = new FileManipulator(path, false);

      long arcSize = fm.getLength();

      // 4 - First File Offset (20)
      // 4 - Header (PFF3)
      fm.skip(8);

      // 4 - Number Of Files
      int numFiles = fm.readInt();
      FieldValidator.checkNumFiles(numFiles);

      // 4 - Unknown (32)
      fm.skip(4);

      // 4 - Directory Offset
      long dirOffset = fm.readInt();
      FieldValidator.checkOffset(dirOffset, arcSize);

      Resource[] resources = new Resource[numFiles];
      TaskProgressManager.setMaximum(numFiles);

      fm.seek(dirOffset);

      // Loop through directory
      for (int i = 0; i < numFiles; i++) {
        // 4 - End Of Directory Marker (non-null for end of directory, null for a file entry)
        fm.skip(4);

        // 4 - File Offset
        long offset = fm.readInt();
        FieldValidator.checkOffset(offset, arcSize);

        // 4 - File Size
        long length = fm.readInt();
        FieldValidator.checkLength(length, arcSize);

        // 4 - Some kind of file type identifier?
        fm.skip(4);

        // 16 - Filename (null)
        String filename = fm.readNullString(16);

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
