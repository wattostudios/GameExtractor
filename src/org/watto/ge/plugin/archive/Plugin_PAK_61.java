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
public class Plugin_PAK_61 extends ArchivePlugin {

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public Plugin_PAK_61() {

    super("PAK_61", "PAK_61");

    //         read write replace rename
    setProperties(true, false, false, false);

    setGames("Pathway");
    setExtensions("pak"); // MUST BE LOWER CASE
    setPlatforms("PC");

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

      getDirectoryFile(fm.getFile(), "sdl");
      rating += 25;

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

      long arcSize = (int) path.length();

      File sourcePath = getDirectoryFile(path, "sdl");
      FileManipulator fm = new FileManipulator(sourcePath, false);

      // Line 1 - Number of Files
      // pak files=7788
      int numFiles = 0;
      String line = fm.readLine();
      if (line != null) {
        int equalPos = line.lastIndexOf('=');
        if (equalPos > 0) {
          line = line.substring(equalPos + 1);
        }
        try {
          numFiles = Integer.parseInt(line);
        }
        catch (Throwable t) {
          return null;
        }
      }
      FieldValidator.checkNumFiles(numFiles);

      Resource[] resources = new Resource[numFiles];
      TaskProgressManager.setMaximum(numFiles);

      // Loop through directory
      for (int i = 0; i < numFiles; i++) {
        // Line - File Details
        // file 0x39ff2c68a2fb9e22 block=0 size=14086222 path="base/gfx/anims#depth.dcf"
        // ... where offset = block*4096
        line = fm.readLine();
        if (line == null) {
          continue;
        }

        int blockPos = line.indexOf("block=");
        int sizePos = line.indexOf("size=");
        int pathPos = line.indexOf("path=");

        if (blockPos < 0 || sizePos < 0 || pathPos < 0) {
          continue;
        }

        String blockString = line.substring(blockPos + 6, sizePos - 1);
        String sizeString = line.substring(sizePos + 5, pathPos - 1);
        String pathString = line.substring(pathPos + 6, line.length() - 1); // to cut the " from the front and back of the value

        int offset = 0;
        int length = 0;
        String filename = "";

        try {
          offset = Integer.parseInt(blockString) * 4096;
          length = Integer.parseInt(sizeString);
        }
        catch (Throwable t) {
          continue;
        }
        FieldValidator.checkOffset(offset, arcSize);
        FieldValidator.checkLength(length, arcSize);

        filename = pathString;
        filename = filename.replace('#', '_');

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
