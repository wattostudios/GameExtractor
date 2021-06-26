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
import org.watto.component.WSPluginException;
import org.watto.datatype.Archive;
import org.watto.datatype.Resource;
import org.watto.ge.helper.FieldValidator;
import org.watto.ge.plugin.ArchivePlugin;
import org.watto.ge.plugin.ExporterPlugin;
import org.watto.ge.plugin.exporter.Exporter_Deflate;
import org.watto.io.FileManipulator;
import org.watto.task.TaskProgressManager;

/**
**********************************************************************************************

**********************************************************************************************
**/
public class Plugin_VFS_SFV extends ArchivePlugin {

  /**
  **********************************************************************************************

  **********************************************************************************************
  **/
  public Plugin_VFS_SFV() {

    super("VFS_SFV", "VFS_SFV");

    //         read write replace rename
    setProperties(true, false, false, false);

    setGames("3SwitcheD");
    setExtensions("vfs"); // MUST BE LOWER CASE
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

      // 1 - Version (1)
      if (fm.readByte() == 1) {
        rating += 5;
      }

      // Header
      if (fm.readString(3).equals("SFV")) {
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

      ExporterPlugin exporter = Exporter_Deflate.getInstance();

      // RESETTING GLOBAL VARIABLES

      FileManipulator fm = new FileManipulator(path, false);

      long arcSize = fm.getLength();

      int numFiles = Archive.getMaxFiles();
      Resource[] resources = new Resource[numFiles];

      TaskProgressManager.setMaximum(arcSize);

      int realNumFiles = 0;
      while (fm.getOffset() < arcSize) {
        int entryType = 0;
        if (realNumFiles == 0) { // first file only
          fm.skip(6);
          entryType = 1311747;
        }
        else {
          // 2 - Header (PK)
          fm.skip(2);

          // 4 - Entry Type (1311747 = File Entry)
          entryType = fm.readInt();
        }
        if (entryType == 1311747) {
          // File Entry

          // 2 - Unknown (2)
          // 2 - Unknown (8)
          // 8 - Checksum?
          fm.skip(12);

          // 4 - Compressed File Size
          int length = fm.readInt();
          FieldValidator.checkLength(length, arcSize);

          // 4 - Decompressed File Size
          int decompLength = fm.readInt();
          FieldValidator.checkLength(decompLength);

          // 4 - Filename Length
          int filenameLength = fm.readInt();
          FieldValidator.checkFilenameLength(filenameLength);

          // X - Filename
          String filename = fm.readString(filenameLength);

          // X - File Data
          long offset = fm.getOffset();
          fm.skip(length);

          //path,name,offset,length,decompLength,exporter
          if (length > 0) {
            if (decompLength == length) {
              // no compression
              resources[realNumFiles] = new Resource(path, filename, offset, length);
            }
            else {
              // compression
              resources[realNumFiles] = new Resource(path, filename, offset, length, decompLength, exporter);
            }
            realNumFiles++;
          }

          TaskProgressManager.setValue(offset);

        }
        else if (entryType == 513) {
          // Directory Entry

          // 2 - Unknown (20)
          // 2 - Unknown (2)
          // 2 - Unknown (8)
          // 8 - Checksum?
          // 4 - Compressed File Size
          // 4 - Decompressed File Size
          fm.skip(22);

          // 4 - Filename Length
          int filenameLength = fm.readInt();
          FieldValidator.checkFilenameLength(filenameLength);

          // 10 - null
          // 4 - File Offset (points to PK for this file in the directory)
          fm.skip(14);

          // X - Filename
          fm.skip(filenameLength);

        }
        else if (entryType == 1541) {
          // EOF Entry

          // 2 - null
          // 8 - Checksum?
          // 4 - Length Of File Data (archive size excluding the directory)
          // 2 - null
          fm.skip(16);
        }
        else {
          // bad header
          String errorMessage = "[VFS_SFV]: Manual read: Unknown entry type " + entryType + " at offset " + (fm.getOffset() - 6);
          if (realNumFiles >= 5) {
            // we found a number of files, so lets just return them, it might be a "prematurely-short" archive.
            ErrorLogger.log(errorMessage);
            break;
          }
          else {
            throw new WSPluginException(errorMessage);
          }
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
