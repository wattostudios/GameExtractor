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
import org.watto.ge.plugin.ExporterPlugin;
import org.watto.ge.plugin.exporter.Exporter_ZLib;
import org.watto.io.FileManipulator;
import org.watto.task.TaskProgressManager;

/**
**********************************************************************************************

**********************************************************************************************
**/
public class Plugin_UOP_MYP extends ArchivePlugin {

  /**
  **********************************************************************************************

  **********************************************************************************************
  **/
  public Plugin_UOP_MYP() {

    super("UOP_MYP", "UOP_MYP");

    //         read write replace rename
    setProperties(true, false, false, false);

    setGames("Ultima Online");
    setExtensions("uop"); // MUST BE LOWER CASE
    setPlatforms("PC");

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

      // Header
      if (fm.readString(3).equals("MYP")) {
        rating += 50;
      }
      fm.skip(9);

      if (fm.readInt() == 40) {
        rating += 5;
      }

      fm.skip(4);

      if (fm.readInt() == 100) {
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

      ExporterPlugin exporter = Exporter_ZLib.getInstance();

      // RESETTING GLOBAL VARIABLES

      FileManipulator fm = new FileManipulator(path, false);

      long arcSize = fm.getLength();

      // 4 - Header ("MYP" + null)
      // 4 - Unknown (4)
      // 4 - Unknown
      // 4 - Header Length (40)
      // 4 - null
      // 4 - Block Split Size (100)
      fm.skip(24);

      // 4 - Total Number of Blocks in the Whole Archive
      int numFiles = fm.readInt();
      FieldValidator.checkNumFiles(numFiles);

      // 12 - null
      fm.skip(12);

      Resource[] resources = new Resource[numFiles];
      TaskProgressManager.setMaximum(numFiles);

      // Loop through directory
      int currentPos = 0;
      while (currentPos < numFiles) {

        // 4 - Number of Files in this Block
        int numFilesInBlock = fm.readInt();
        FieldValidator.checkRange(numFilesInBlock, 0, numFiles);

        // 4 - Next Block Offset
        int nextBlockOffset = fm.readInt();
        FieldValidator.checkOffset(nextBlockOffset, arcSize);

        // 4 - null
        fm.skip(4);

        for (int i = 0; i < numFilesInBlock; i++) {

          // 4 - File Data Offset
          int offset = fm.readInt();
          FieldValidator.checkOffset(offset, arcSize);

          // 4 - null
          // 4 - Unknown (12)
          fm.skip(8);

          // 4 - Compressed File Data Length (not including the header)
          int length = fm.readInt();
          FieldValidator.checkLength(length, arcSize);

          // 4 - Decompressed File Data Length
          int decompLength = fm.readInt();
          FieldValidator.checkLength(decompLength);

          // 12 - Hash?
          // 2 - Unknown (1)
          fm.skip(14);

          String filename = Resource.generateFilename(currentPos);

          //path,name,offset,length,decompLength,exporter
          if (length + 12 == decompLength) {
            // no compression
            offset += 12;
            decompLength -= 12;

            resources[currentPos] = new Resource(path, filename, offset, length);
          }
          else {
            // compression
            offset += 12;

            resources[currentPos] = new Resource(path, filename, offset, length, decompLength, exporter);
          }

          currentPos++;
          TaskProgressManager.setValue(currentPos);
        }

        fm.seek(nextBlockOffset);
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
