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
public class Plugin_MFS extends ArchivePlugin {

  /**
  **********************************************************************************************

  **********************************************************************************************
  **/
  public Plugin_MFS() {

    super("MFS", "MFS");

    //         read write replace rename
    setProperties(true, false, false, false);

    setGames("Gladiator: Sword Of Vengeance",
        "Made Man");
    setExtensions("mfs");
    setPlatforms("PC", "XBox");

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

      // Number Of Files
      if (FieldValidator.checkNumFiles(fm.readInt())) {
        rating += 5;
      }

      // Version (1)
      if (fm.readInt() == 1) {
        rating += 5;
      }

      long arcSize = fm.getLength();

      // First File Offset
      if (FieldValidator.checkOffset(fm.readInt(), arcSize)) {
        rating += 5;
      }

      // Header
      if (fm.readString(4).equals("MFS4")) {
        rating += 50;
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
    if (extension.equalsIgnoreCase("pea") || extension.equalsIgnoreCase("se") || extension.equalsIgnoreCase("slf")) {
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

      FileManipulator fm = new FileManipulator(path, false);

      long arcSize = fm.getLength();

      // 4 - Number Of Files
      int numFiles = fm.readInt();
      FieldValidator.checkNumFiles(numFiles);

      // 4 - Version (1)
      fm.skip(4);

      // 4 - First File Offset
      int firstFileOffset = fm.readInt();
      FieldValidator.checkOffset(firstFileOffset, arcSize);

      // 4 - Header (MFS4)
      fm.skip(4);

      // 4 - Padding Size (2048)
      int paddingSize = fm.readInt();
      FieldValidator.checkLength(paddingSize, arcSize);

      // 32 - Filename Of Archive, in CAPS (eg CD-2.EN)
      fm.skip(32);

      // 4 - Filename Directory Offset
      long dirOffset = fm.readInt();
      FieldValidator.checkOffset(dirOffset, arcSize);

      // 4 - Filename Directory Length
      // 4 - null

      Resource[] resources = new Resource[numFiles];
      TaskProgressManager.setMaximum(numFiles);

      // Read the Filenames
      fm.seek(dirOffset);

      // 4 - Extensions Header (EXT )
      // 4 - Length Of Data (excluding padding at the end) [+12]
      fm.skip(8);

      // 4 - Offset to the start of filenames
      int filenameDirOffset = fm.readInt();
      FieldValidator.checkOffset(filenameDirOffset, arcSize);

      // Read the offsets directory
      long[] filenameOffsets = new long[numFiles];
      for (int i = 0; i < numFiles; i++) {
        // 4 - Filename Offset
        long filenameOffset = fm.readInt();
        FieldValidator.checkOffset(filenameOffset, arcSize);
        filenameOffsets[i] = filenameOffset;

        // 4 - Unknown
        fm.skip(4);
      }

      fm.seek(filenameDirOffset);

      // Loop through directory
      String[] filenames = new String[numFiles];
      for (int i = 0; i < numFiles; i++) {
        // just to be sure - go to the correct filename offset
        fm.seek(filenameOffsets[i]);

        // X - Filename (null) (including "c:\" etc.)
        String filename = fm.readNullString();
        if (filename.length() > 3 && filename.charAt(1) == ':') {
          filename = filename.substring(3);
        }
        filenames[i] = filename;
      }

      // now loop through the files directory
      fm.seek(64);

      for (int i = 0; i < numFiles; i++) {
        // 4 - Unknown
        fm.skip(4);

        // 4 - File Offset [* PaddingSize]
        long offset = fm.readInt() * paddingSize;
        FieldValidator.checkOffset(offset, arcSize);

        // 4 - File Size
        int length = fm.readInt();
        FieldValidator.checkLength(length, arcSize);

        // 4 - File Size
        int otherLength = fm.readInt();
        FieldValidator.checkLength(otherLength, arcSize);

        // 2 - null
        // 2 - Unknown
        fm.skip(4);

        //path,id,name,offset,length,decompLength,exporter
        resources[i] = new Resource(path, filenames[i], offset, length);

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
