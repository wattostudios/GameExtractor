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
import org.watto.ErrorLogger;
import org.watto.datatype.Resource;
import org.watto.ge.helper.FieldValidator;
import org.watto.ge.plugin.ArchivePlugin;
import org.watto.io.FileManipulator;
import org.watto.io.converter.ByteConverter;
import org.watto.task.TaskProgressManager;

/**
**********************************************************************************************

**********************************************************************************************
**/
public class Plugin_DAT_61 extends ArchivePlugin {

  /**
  **********************************************************************************************

  **********************************************************************************************
  **/
  public Plugin_DAT_61() {

    super("DAT_61", "DAT_61");

    //         read write replace rename
    setProperties(true, false, false, false);

    setGames("Glowfish",
        "Midnight Mysteries 2: Salem Witch Trials",
        "Midnight Mysteries 3: Devil on the Mississippi",
        "Midnight Mysteries 4: Haunted Houdini",
        "Pickers");
    setExtensions("dat"); // MUST BE LOWER CASE
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

      // 4 - Magic Number? ((bytes)206,222,237,236)
      byte[] headerBytes = fm.readBytes(4);
      if (ByteConverter.unsign(headerBytes[0]) == 206 && ByteConverter.unsign(headerBytes[1]) == 222 && ByteConverter.unsign(headerBytes[2]) == 237 && ByteConverter.unsign(headerBytes[3]) == 236) {
        rating += 50;
      }

      long arcSize = fm.getLength();

      // version? (1)
      if (fm.readInt() == 1) {
        rating += 5;
      }

      // 4 - Details Directory Offset
      if (FieldValidator.checkOffset(fm.readInt(), arcSize)) {
        rating += 5;
      }

      // 8 - null
      if (fm.readLong() == 0) {
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
  @SuppressWarnings("unused")
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

      // 4 - Magic Number? ((bytes)206,222,237,236)
      // 4 - Version? (1)

      fm.skip(8);

      // 4 - Details Directory Offset
      int dirOffset = fm.readInt();
      FieldValidator.checkOffset(dirOffset, arcSize);

      fm.seek(dirOffset);

      // 4 - Number of Entries
      int numFiles = fm.readInt();
      FieldValidator.checkNumFiles(numFiles);

      long relNameOffset = dirOffset + 4 + numFiles * 16 + 4;

      TaskProgressManager.setMaximum(numFiles);

      // Loop through directory
      boolean[] dirEntry = new boolean[numFiles];
      long[] nameOffsets = new long[numFiles];
      int[] offsets = new int[numFiles];
      int[] lengths = new int[numFiles];

      int realNumFiles = 0;
      for (int i = 0; i < numFiles; i++) {
        // 4 - Entry Type (0=File, 1=Folder)
        int entryType = fm.readInt();
        if (entryType == 1) {
          // 8 - null
          fm.skip(8);

          // 4 - Folder Name Offset (relative to the start of the Names directory) [+4]
          long nameOffset = fm.readInt() + relNameOffset;
          FieldValidator.checkOffset(nameOffset, arcSize);
          nameOffsets[i] = nameOffset;

          dirEntry[i] = true;
        }
        else if (entryType == 0) {
          // 4 - File Offset
          int offset = fm.readInt();
          FieldValidator.checkOffset(offset, arcSize);
          offsets[i] = offset;

          // 4 - File Length
          int length = fm.readInt();
          FieldValidator.checkLength(length, arcSize);
          lengths[i] = length;

          // 4 - Filename Offset (relative to the start of the Names directory) [+4]
          long nameOffset = fm.readInt() + relNameOffset;
          FieldValidator.checkOffset(nameOffset, arcSize);
          nameOffsets[i] = nameOffset;

          dirEntry[i] = false;
          realNumFiles++;
        }
        else {
          ErrorLogger.log("[DAT_61] Unknown Entry Type: " + entryType);
          return null;
        }

        TaskProgressManager.setValue(i);
      }

      Resource[] resources = new Resource[realNumFiles];

      realNumFiles = 0;
      String dirName = "";
      for (int i = 0; i < numFiles; i++) {
        if (dirEntry[i]) {
          fm.seek(nameOffsets[i]);

          // X - Filename (null)
          String filename = fm.readNullString();
          FieldValidator.checkFilename(filename);

          dirName = filename + "\\";
        }
        else {

          fm.seek(nameOffsets[i]);

          // X - Filename (null)
          String filename = fm.readNullString();
          FieldValidator.checkFilename(filename);

          //filename = dirName + filename; // not needed, the filename already contains the dirname in it!

          //path,name,offset,length,decompLength,exporter
          resources[realNumFiles] = new Resource(path, filename, offsets[i], lengths[i]);
          realNumFiles++;

          TaskProgressManager.setValue(i);
        }
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
