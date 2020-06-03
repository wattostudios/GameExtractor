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
public class Plugin_SHD_MRTS extends ArchivePlugin {

  /**
  **********************************************************************************************

  **********************************************************************************************
  **/
  public Plugin_SHD_MRTS() {

    super("SHD_MRTS", "SHD_MRTS");

    //         read write replace rename
    setProperties(true, false, false, false);

    setGames("Harry Potter and the Order of the Phoenix");
    setExtensions("shd", "ffs"); // MUST BE LOWER CASE
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

      // 4 - Header (MRTS)
      if (fm.readString(4).equals("MRTS")) {
        rating += 50;
      }

      long arcSize = fm.getLength();

      // 4 - Archive Length
      if (FieldValidator.checkEquals(fm.readInt(), arcSize)) {
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

      // 4 - Header (MRTS)
      // 4 - Archive Length
      fm.skip(8);

      int numFiles = Archive.getMaxFiles();

      Resource[] resources = new Resource[numFiles];
      TaskProgressManager.setMaximum(arcSize);

      // Loop through directory
      int realNumFiles = 0;
      int[] nameOffsets = new int[numFiles];
      while (fm.getOffset() < arcSize) {

        // 4 - Header
        String header = fm.readString(4);

        if (header.equals("PRGA")) {
          // Group (opening part)

          // 4 - Group Length (including these header fields)
          fm.skip(4);
        }
        else if (header.equals("RDHG")) {
          // Group (header part)

          // 4 - Header Length (20)
          int headerLength = fm.readInt() - 8;
          FieldValidator.checkLength(headerLength, arcSize);

          // 4 - Unknown (16)
          // 4 - Group File Type (Name) Offset
          // 4 - Unknown (16)
          fm.skip(headerLength);
        }
        else if (header.equals("TESA")) {
          // File

          // 4 - File Entry length (40)
          int entryLength = fm.readInt() - 36;
          FieldValidator.checkLength(entryLength, arcSize);

          // 4 - Hash?
          // 4 - Unknown
          // 8 - null
          fm.skip(16);

          // 4 - File Offset
          int offset = fm.readInt();
          FieldValidator.checkOffset(offset, arcSize);

          // 4 - File Length
          int length = fm.readInt();
          FieldValidator.checkLength(length, arcSize);

          // 4 - Filename Offset
          int filenameOffset = fm.readInt();
          FieldValidator.checkOffset(filenameOffset, arcSize);
          nameOffsets[realNumFiles] = filenameOffset;

          // 4 - null
          fm.skip(entryLength);

          String filename = Resource.generateFilename(realNumFiles);

          //path,name,offset,length,decompLength,exporter
          resources[realNumFiles] = new Resource(path, filename, offset, length);
          realNumFiles++;

          TaskProgressManager.setValue(offset);

        }
        else if (header.equals("SRTS")) {
          // Filenames

          // Found the filename table, so just process all the nameOffsets we remembered earlier, then skip this whole block

          // 4 - Length of Filename Directory (including these header fields)
          long endDirOffset = (fm.readInt() - 8) + fm.getOffset();
          FieldValidator.checkOffset(endDirOffset, arcSize);

          for (int f = 0; f < realNumFiles; f++) {
            fm.seek(nameOffsets[f]);

            // X - Filename (including the "c:\")
            // 1 - null Filename Terminator
            String filename = fm.readNullString();
            FieldValidator.checkFilename(filename);

            int dotPos = filename.indexOf(':');
            if (dotPos > 0 && dotPos + 2 < filename.length()) {
              filename = filename.substring(dotPos + 2);
            }

            Resource resource = resources[f];
            resource.setName(filename);
            resource.setOriginalName(filename);
          }

          fm.seek(endDirOffset);
        }
        else {
          // Found the file data, or something else unknown
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
