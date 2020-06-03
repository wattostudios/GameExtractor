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
public class Plugin_VPB extends ArchivePlugin {

  /**
  **********************************************************************************************

  **********************************************************************************************
  **/
  public Plugin_VPB() {

    super("VPB", "VPB");

    //         read write replace rename
    setProperties(true, false, false, false);

    setGames("Legends of Atlantis: Exodus");
    setExtensions("vpb"); // MUST BE LOWER CASE
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

      getDirectoryFile(fm.getFile(), "vpi");
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

      File sourcePath = getDirectoryFile(path, "vpi");
      FileManipulator fm = new FileManipulator(sourcePath, false);

      long indexSize = sourcePath.length();

      int numFiles = Archive.getMaxFiles();

      Resource[] resources = new Resource[numFiles];
      TaskProgressManager.setMaximum(arcSize);

      // Loop through directory
      int realNumFiles = 0;
      while (fm.getOffset() < indexSize) {
        // 4 - File Header ("File")
        // 1 - Separator (" ")
        // 1 - Filename Start Indicator (")
        fm.skip(6);

        // X - Filename
        // 1 - Filename End Indicator (")
        String filename = "";
        char letter = (char) fm.readByte();
        while (letter != '"') {
          filename += letter;
          letter = (char) fm.readByte();
        }
        FieldValidator.checkFilename(filename);

        // 1 - Separator (" ")
        fm.skip(1);

        // X - File Offset
        // 1 - Separator (" ")
        String offsetString = "";
        char offsetLetter = (char) fm.readByte();
        while (offsetLetter != ' ') {
          offsetString += offsetLetter;
          offsetLetter = (char) fm.readByte();
        }
        int offset = Integer.parseInt(offsetString);
        FieldValidator.checkOffset(offset, arcSize);

        // X - File Length
        // 2 - End of Entry Indicator ((bytes)13,10)
        String lengthString = "";
        char newline = (char) 13;
        char lengthLetter = (char) fm.readByte();
        while (lengthLetter != newline) {
          lengthString += lengthLetter;
          lengthLetter = (char) fm.readByte();
        }
        fm.skip(1);
        int length = Integer.parseInt(lengthString);
        FieldValidator.checkLength(length, arcSize);

        //path,name,offset,length,decompLength,exporter
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
