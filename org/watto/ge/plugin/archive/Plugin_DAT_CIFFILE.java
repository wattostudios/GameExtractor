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
public class Plugin_DAT_CIFFILE extends ArchivePlugin {

  /**
  **********************************************************************************************

  **********************************************************************************************
  **/
  public Plugin_DAT_CIFFILE() {

    super("DAT_CIFFILE", "DAT_CIFFILE");

    //         read write replace rename
    setProperties(true, false, false, false);

    setGames("Nancy Drew: Alibi In Ashes");
    setExtensions("dat"); // MUST BE LOWER CASE
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

      // 24 - Header ("CIF FILE HerInteractive" + null)
      if (fm.readString(23).equals("CIF FILE HerInteractive") && fm.readByte() == 0) {
        rating += 50;
      }

      // 4 - Version? (3)
      if (fm.readInt() == 3) {
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

      // Skip to the end of the file, to read the Archive Footer
      fm.seek(arcSize - 4);

      // 4 - Details Directory Length
      int dirLength = fm.readInt();
      FieldValidator.checkLength(dirLength, arcSize);

      int numFiles = (dirLength - 4) / 68;
      FieldValidator.checkNumFiles(numFiles);

      long dirOffset = arcSize - dirLength;
      FieldValidator.checkOffset(dirOffset, arcSize);
      fm.seek(dirOffset);

      Resource[] resources = new Resource[numFiles];
      TaskProgressManager.setMaximum(numFiles);

      // Loop through directory
      String[] filenames = new String[numFiles];
      long[] offsets = new long[numFiles];
      for (int i = 0; i < numFiles; i++) {

        // 64 - Filename (filled with nulls)
        String filename = fm.readNullString(64);
        FieldValidator.checkFilename(filename);

        // 4 - File Offset (offset to the "CIF" header for the file)
        int offset = fm.readInt();
        FieldValidator.checkOffset(offset, arcSize);

        filenames[i] = filename;
        offsets[i] = offset;
      }

      // now go through and read the file headers
      for (int i = 0; i < numFiles; i++) {
        long offset = offsets[i];
        fm.seek(offset);

        // 24 - Header ("CIF FILE HerInteractive" + null)
        // 4 - Version? (3)
        fm.skip(28);

        // 4 - File Type? (2=PNG, 3=LUA, 6=XSHEET)
        int fileType = fm.readInt();
        String extension = "." + fileType;
        if (fileType == 2) {
          extension = ".png";
        }
        else if (fileType == 3) {
          extension = ".lua";
        }
        else if (fileType == 6) {
          extension = ".xsheet";
        }
        String filename = filenames[i] + extension;

        // 4 - Unknown
        // 4 - Unknown
        // 4 - Unknown (1/0)
        fm.skip(12);

        // 4 - Data Length
        int length = fm.readInt();
        FieldValidator.checkLength(length, arcSize);

        // X - File Data
        offset = fm.getOffset();

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
