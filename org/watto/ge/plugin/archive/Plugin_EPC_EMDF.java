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
public class Plugin_EPC_EMDF extends ArchivePlugin {

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public Plugin_EPC_EMDF() {

    super("EPC_EMDF", "EPC_EMDF");

    //         read write replace rename
    setProperties(true, false, false, false);

    setGames("Doctor Who: Episode 1: City of the Daleks",
        "Doctor Who: Episode 2: Blood of the Cybermen",
        "Doctor Who: Episode 3: Tardis",
        "Doctor Who: Episode 4: Shadows of the Vashta Nerada");
    setExtensions("epc"); // MUST BE LOWER CASE
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

      long arcSize = fm.getLength();

      // 8 - null
      fm.skip(8);

      // 4 - Header (EMDF)
      if (fm.readString(4).equals("EMDF")) {
        rating += 50;
      }

      // 4 - First File Offset
      if (FieldValidator.checkOffset(fm.readInt(), arcSize)) {
        rating += 5;
      }

      // 4 - null
      // 4 - Unknown (336)
      // 24 - null
      // 4 - Unknown
      // 20 - null
      // 4 - Unknown (31)
      // 4 - Unknown (1)
      fm.skip(64);

      // 4 - Number of Files?
      if (FieldValidator.checkNumFiles(fm.readInt())) {
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

      // 8 - null
      // 4 - Header (EMDF)
      // 4 - First File Offset
      // 4 - null
      // 4 - Unknown (336)
      // 24 - null
      // 4 - Unknown
      // 20 - null
      // 4 - Unknown (31)
      // 4 - Unknown (1)
      fm.skip(80);

      // 4 - Number of Files
      int numFiles = fm.readInt();
      FieldValidator.checkNumFiles(numFiles);

      // 4 - Details Directory Offset
      int dirOffset = fm.readInt();
      FieldValidator.checkOffset(dirOffset, arcSize);

      fm.getBuffer().setBufferSize(256);// small, for quick reading through the archive

      // skip past the whole directory, so we can start reading the actual file data
      fm.seek(dirOffset + (numFiles * 12));

      for (int i = 0; i < 100; i++) { // max 100 bytes to check
        if (fm.readByte() == 0 && fm.readByte() == 0 && fm.readByte() == 0 && fm.readByte() == 0) {
          break;
        }
      }
      fm.relativeSeek(fm.getOffset() - 4);

      Resource[] resources = new Resource[numFiles];
      TaskProgressManager.setMaximum(numFiles);

      // Loop through directory
      int realNumFiles = 0;
      while (fm.getOffset() < arcSize) {
        long offset = fm.getOffset();

        // 8 - null
        fm.skip(8);

        // 4 - Header (EMTR)
        String header = fm.readString(4);

        // 4 - File Length (including all these header fields)
        int length = fm.readInt();
        FieldValidator.checkLength(length);

        String filename = Resource.generateFilename(realNumFiles);
        if (header.equals("EMTR")) {
          // 108 - Other Header Data
          fm.skip(108);

          // 4 - File Length (length of the file data only)
          int imageLength = fm.readInt();
          FieldValidator.checkLength(imageLength);

          // 4 - Unknown
          // 4 - Junk Padding
          // 8 - null
          fm.skip(16);

          // X - File Data
          fm.skip(imageLength);

          // X - Filename (including Z:/)
          // 1 - null Filename Terminator
          filename = fm.readNullString();
          FieldValidator.checkFilename(filename);

          if (filename.length() > 3 && filename.charAt(1) == ':') {
            filename = filename.substring(3);
          }

          if (filename.equals("NoName")) {
            filename = Resource.generateFilename(realNumFiles);
          }

        }

        filename += "." + header;

        // go the the next file
        fm.seek(offset + length);

        //path,name,offset,length,decompLength,exporter
        resources[realNumFiles] = new Resource(path, filename, offset, length);

        TaskProgressManager.setValue(offset);
        realNumFiles++;
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
