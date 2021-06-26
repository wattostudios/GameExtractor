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
import org.watto.io.converter.IntConverter;
import org.watto.task.TaskProgressManager;

/**
**********************************************************************************************

**********************************************************************************************
**/
public class Plugin_CPK_CPK extends ArchivePlugin {

  /**
  **********************************************************************************************

  **********************************************************************************************
  **/
  public Plugin_CPK_CPK() {

    super("CPK_CPK", "CPK_CPK");

    //         read write replace rename
    setProperties(true, false, false, false);

    setGames("Viking: Battle for Asgard");
    setExtensions("cpk"); // MUST BE LOWER CASE
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
      if (fm.readString(4).equals("CPK ")) {
        rating += 50;
      }

      fm.skip(12);

      // Header
      if (fm.readString(4).equals("@UTF")) {
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

      // 4 - Header ("CPK ")
      // 4 - Unknown (255)
      // 4 - Unknown
      // 4 - null
      fm.skip(16);

      int numFiles = Archive.getMaxFiles();

      Resource[] resources = new Resource[numFiles];
      TaskProgressManager.setMaximum(arcSize);

      // Loop through directory
      int realNumFiles = 0;
      while (fm.getOffset() < arcSize) {
        // 4 - File Header ("@UTF")
        String header = fm.readString(4);

        if (header.equals("@UTF")) {
          // 4 - File Length (including this field) (BIG ENDIAN)
          int length = IntConverter.changeFormat(fm.readInt()) - 4;
          FieldValidator.checkLength(length, arcSize);

          // X - File Data
          // 0-2047 - null Padding to a multiple of 2048 bytes
          long offset = fm.getOffset();
          fm.skip(length);
          if (realNumFiles == 0) {
            fm.skip(calculatePadding(fm.getOffset(), 2048));
          }
          else {
            // find the next header, up to 512 bytes away
            for (int i = 0; i < 512; i++) {
              if (fm.readByte() == 64) {
                if (fm.readByte() == 85) {
                  if (fm.readByte() == 84) {
                    if (fm.readByte() == 70) {
                      fm.seek(fm.getOffset() - 4);
                      break;
                    }
                  }
                }
              }
            }
          }

          String filename = Resource.generateFilename(realNumFiles);

          //path,name,offset,length,decompLength,exporter
          resources[realNumFiles] = new Resource(path, filename, offset, length);

          TaskProgressManager.setValue(offset);
          realNumFiles++;
        }
        else {
          break;
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
