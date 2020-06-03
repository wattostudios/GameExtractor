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
public class Plugin_000_8 extends ArchivePlugin {

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public Plugin_000_8() {

    super("000_8", "000_8");

    //         read write replace rename
    setProperties(true, false, false, false);

    setGames("Batman Vengeance");
    setExtensions("000"); // MUST BE LOWER CASE
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

      getDirectoryFile(fm.getFile(), "fat");
      rating += 25;

      long arcSize = fm.getLength();

      // Decompressed Length
      if (FieldValidator.checkLength(fm.readInt())) {
        rating += 5;
      }

      // Compressed Length
      if (FieldValidator.checkLength(fm.readInt(), arcSize)) {
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

      long arcSize = (int) path.length();

      File sourcePath = getDirectoryFile(path, "fat");
      FileManipulator fm = new FileManipulator(sourcePath, false);

      long fatLength = sourcePath.length();

      // 1 - Unknown (1)
      fm.skip(1);

      // 4 - Number Of Files
      int numFiles = Archive.getMaxFiles();

      Resource[] resources = new Resource[numFiles];
      TaskProgressManager.setMaximum(fatLength);

      // Loop through directory
      int realNumFiles = 0;
      while (fm.getOffset() < fatLength) {

        // 4 - Offset of This Field
        fm.skip(4);

        // 4 - File Offset
        long offset = fm.readInt();
        FieldValidator.checkOffset(offset, arcSize);

        if (offset == 1) {
          // directory entry

          // 4 - null
          // 4 - Directory Header (cali)
          // 8 - Unknown
          fm.skip(16);

          // 4 - Directory Name Length (including null)
          int dirNameLength = fm.readInt();
          FieldValidator.checkFilenameLength(dirNameLength);

          // X - Directory Name
          // 1 - null Directory Name Terminator
          // 4 - null
          fm.skip(dirNameLength + 4);
        }
        else {
          // file entry
          offset += 12; // to skip the 13-byte file header

          // 4 - Decompressed Length
          long decompLength = fm.readInt();
          FieldValidator.checkLength(decompLength);

          // 4 - Compressed Length
          long length = fm.readInt();
          FieldValidator.checkLength(length, arcSize);

          // 8 - Unknown
          fm.skip(8);

          // 4 - Filename Length (including null)
          int filenameLength = fm.readInt();
          FieldValidator.checkFilenameLength(filenameLength);

          // X - Filename
          // 1 - null Filename Terminator
          String filename = fm.readNullString(filenameLength);
          FieldValidator.checkFilename(filename);

          // 2 - Unknown (256)
          // 2 - Unknown (0/47)
          fm.skip(4);

          //path,name,offset,length,decompLength,exporter
          resources[realNumFiles] = new Resource(path, filename, offset, length, decompLength);
          realNumFiles++;
        }

        TaskProgressManager.setValue(fm.getOffset());
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
