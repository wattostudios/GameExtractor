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
public class Plugin_VOL_VOL_2 extends ArchivePlugin {

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public Plugin_VOL_VOL_2() {

    super("VOL_VOL_2", "VOL_VOL_2");

    //         read write replace rename
    setProperties(true, false, false, false);

    setGames("Starsiege", "Starsiege: Tribes");
    setExtensions("vol"); // MUST BE LOWER CASE
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
      if (fm.readString(4).equals(" VOL")) {
        rating += 50;
      }

      long arcSize = fm.getLength();

      // Directory Offset
      if (FieldValidator.checkOffset(fm.readInt(), arcSize)) {
        rating += 5;
      }

      // Header
      if (fm.readString(4).equals("VBLK")) {
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

      // 4 - Header (" VOL")
      fm.skip(4);

      // 4 - Empty Directory Offset
      int dirOffset = fm.readInt();
      FieldValidator.checkOffset(dirOffset, arcSize);
      fm.seek(dirOffset);

      int filenameDirLength = 0;
      long filenameDirOffset = 0;
      int detailsDirLength = 0;

      while (detailsDirLength == 0 && fm.getOffset() < arcSize) {
        // 4 - Header ("vols")
        fm.skip(4);

        // 4 - Filename Directory Length (not including these 2 fields)
        filenameDirLength = fm.readInt();
        if (filenameDirLength % 2 == 1) {
          filenameDirLength++; // null padding to a multiple of 2 bytes
        }
        FieldValidator.checkLength(filenameDirLength, arcSize);

        filenameDirOffset = fm.getOffset();
        fm.skip(filenameDirLength);

        // 4 - Header (voli)
        fm.skip(4);

        // 4 - Details Directory Length (not including these 2 fields)
        detailsDirLength = fm.readInt();
        FieldValidator.checkLength(detailsDirLength, arcSize);
      }

      if (detailsDirLength <= 0) {
        return null;
      }

      int numFiles = detailsDirLength / 17;
      FieldValidator.checkNumFiles(numFiles);

      Resource[] resources = new Resource[numFiles];
      TaskProgressManager.setMaximum(numFiles);

      // Loop through directory
      long[] nameOffsets = new long[numFiles];
      for (int i = 0; i < numFiles; i++) {

        // 4 - null
        fm.skip(4);

        // 4 - Filename Offset (relative to the start of the Filenames in the FILENAME DIRECTORY)
        long filenameOffset = filenameDirOffset + fm.readInt();
        FieldValidator.checkOffset(filenameOffset, arcSize);
        nameOffsets[i] = filenameOffset;

        // 4 - File Offset
        int offset = fm.readInt() + 8; // skip the 8-byte file header
        FieldValidator.checkOffset(offset, arcSize);

        // 4 - File Length
        int length = fm.readInt() - 8; // skip the 8-byte file header
        FieldValidator.checkLength(length, arcSize);

        // 1 - null
        fm.skip(1);

        String filename = Resource.generateFilename(i);

        //path,name,offset,length,decompLength,exporter
        resources[i] = new Resource(path, filename, offset, length);

        TaskProgressManager.setValue(i);
      }

      // read the filenames
      for (int i = 0; i < numFiles; i++) {
        long nameOffset = nameOffsets[i];
        if (nameOffset != 0) {
          fm.seek(nameOffset);

          // X - Filename
          // 1 - null Filename Terminator
          String filename = fm.readNullString();
          FieldValidator.checkFilename(filename);

          Resource resource = resources[i];

          resource.setName(filename);
          resource.setOriginalName(filename);
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
