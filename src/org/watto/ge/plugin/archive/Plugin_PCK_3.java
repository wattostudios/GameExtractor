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
public class Plugin_PCK_3 extends ArchivePlugin {

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public Plugin_PCK_3() {

    super("PCK_3", "PCK_3");

    //         read write replace rename
    setProperties(true, false, false, false);

    setGames("Shihiki Shiki");
    setExtensions("pck"); // MUST BE LOWER CASE
    setPlatforms("Android");

    // MUST BE LOWER CASE !!!
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

      // 4 - Version (1)
      if (fm.readInt() == 1) {
        rating += 5;
      }

      // 4 - Number of Files
      if (FieldValidator.checkNumFiles(fm.readInt())) {
        rating += 5;
      }

      long arcSize = fm.getLength();

      // 4 - File Data Offset [+32]
      if (FieldValidator.checkOffset(fm.readInt() + 32, arcSize)) {
        rating += 5;
      }

      // 4 - Details Directory Offset [+32]
      if (FieldValidator.checkOffset(fm.readInt() + 32, arcSize)) {
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

      // 4 - Version (1)
      fm.skip(4);

      // 4 - Number of Files
      int numFiles = fm.readInt();
      FieldValidator.checkNumFiles(numFiles);

      // 4 - File Data Offset [+32]
      fm.skip(4);

      // 4 - Details Directory Offset [+32]
      int dirOffset = fm.readInt() + 32;
      FieldValidator.checkOffset(dirOffset, arcSize);

      // 16 - null
      fm.skip(16);

      int[] nameLengths = new int[numFiles];
      for (int i = 0; i < numFiles; i++) {
        // 4 - Filename Length (in bytes)
        int filenameLength = fm.readInt() / 2;
        FieldValidator.checkFilenameLength(filenameLength);
        nameLengths[i] = filenameLength;
      }

      String[] filenames = new String[numFiles];
      for (int i = 0; i < numFiles; i++) {
        // X - Filename (Unicode)
        filenames[i] = fm.readUnicodeString(nameLengths[i]);
      }

      fm.seek(dirOffset);

      Resource[] resources = new Resource[numFiles];
      TaskProgressManager.setMaximum(numFiles);

      // Loop through directory
      for (int i = 0; i < numFiles; i++) {

        // 8 - File Offset
        long offset = fm.readLong();
        FieldValidator.checkOffset(offset, arcSize);

        // 8 - File Length
        long length = fm.readLong();
        FieldValidator.checkLength(length, arcSize);

        String filename = filenames[i];

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
