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
public class Plugin_3DN_DESTAN extends ArchivePlugin {

  /**
  **********************************************************************************************

  **********************************************************************************************
  **/
  public Plugin_3DN_DESTAN() {

    super("3DN_DESTAN", "3DN_DESTAN");

    //         read write replace rename
    setProperties(true, false, false, false);

    setGames("Burn");
    setExtensions("3dn"); // MUST BE LOWER CASE
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

      // 52 - Header ("Destan file format, Michal Tatka 2005" + ((bytes)10,13) repeated to fill)
      if (fm.readString(37).equals("Destan file format, Michal Tatka 2005")) {
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

      // 52 - Header ("Destan file format, Michal Tatka 2005" + ((bytes)10,13) repeated to fill)
      // 4 - Unknown (13)
      // 8 - null
      // 4 - Unknown (197)
      fm.skip(68);

      int numFiles = Archive.getMaxFiles();

      Resource[] resources = new Resource[numFiles];
      TaskProgressManager.setMaximum(arcSize);

      // Loop through directory
      int realNumFiles = 0;
      String filename = "";
      while (fm.getOffset() < arcSize) {

        // 4 - Number of Blocks
        int numBlocks = fm.readInt();
        FieldValidator.checkNumFiles(numBlocks);

        for (int i = 0; i < numBlocks; i++) {
          // 4 - Block Name Length (including null)
          int blockNameLength = fm.readInt();
          FieldValidator.checkFilenameLength(blockNameLength);

          // X - Block Name
          // 1 - null Block Name Terminator
          String blockName = fm.readNullString(blockNameLength);
          //FieldValidator.checkFilename(blockName);

          if (blockName.equalsIgnoreCase("file_name")) {
            // 4 - Filename Length
            int filenameLength = fm.readInt();
            FieldValidator.checkFilenameLength(filenameLength);

            if (filenameLength == 1) {
              // empty filename - just the null terminator
              fm.skip(1);
            }
            else {
              // X - Filename
              filename = fm.readNullString(filenameLength);
              FieldValidator.checkFilename(filename);

              // Strip off the directory at the beginning
              int markPos = filename.lastIndexOf("!") + 1; // +1 to skip the slash following it
              if (markPos <= 0) {
                markPos = filename.indexOf("\\");
              }
              if (markPos > 0 && markPos < filename.length()) {
                filename = filename.substring(markPos);
              }
            }
          }
          else if (blockName.equalsIgnoreCase("data")) {
            // 4 - File Length
            int length = fm.readInt();
            FieldValidator.checkLength(length, arcSize);

            // X - File Data
            long offset = fm.getOffset();
            fm.skip(length);

            if (!filename.equals("") && length != 0) {
              //path,name,offset,length,decompLength,exporter
              resources[realNumFiles] = new Resource(path, filename, offset, length);
              realNumFiles++;
              filename = "";
            }

          }
          else {
            // 4 - Block Data Length
            int blockDataLength = fm.readInt();

            if (blockName.equalsIgnoreCase("\n")) {
              continue;
            }

            FieldValidator.checkLength(blockDataLength, arcSize);

            // X - Block Data
            fm.skip(blockDataLength);
          }

          TaskProgressManager.setValue(fm.getOffset());

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
