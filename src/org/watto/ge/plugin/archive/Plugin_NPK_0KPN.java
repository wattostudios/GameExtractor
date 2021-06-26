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
public class Plugin_NPK_0KPN extends ArchivePlugin {

  int realNumFiles = 0;

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public Plugin_NPK_0KPN() {

    super("NPK_0KPN", "NPK_0KPN");

    //         read write replace rename
    setProperties(true, false, false, false);

    setGames("Drakensang",
        "Project Nomad");
    setExtensions("npk"); // MUST BE LOWER CASE
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
      if (fm.readString(4).equals("0KPN")) {
        rating += 50;
      }

      if (fm.readInt() == 4) {
        rating += 5;
      }

      long arcSize = fm.getLength();

      // File Data Offset
      if (FieldValidator.checkOffset(fm.readInt(), arcSize)) {
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
      realNumFiles = 0;

      FileManipulator fm = new FileManipulator(path, false);

      long arcSize = fm.getLength();

      // 4 - Header (0KPN)
      // 4 - Version? (4)
      fm.skip(8);

      // 4 - File Data Offset
      int fileDataOffset = fm.readInt();
      FieldValidator.checkOffset(fileDataOffset, arcSize);

      int numFiles = Archive.getMaxFiles();

      Resource[] resources = new Resource[numFiles];
      TaskProgressManager.setMaximum(numFiles);

      while (fm.getOffset() < fileDataOffset) {
        readDirectory(fm, path, resources, fileDataOffset, arcSize, "");
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

  /**
   **********************************************************************************************
  
   **********************************************************************************************
   **/
  public void readDirectory(FileManipulator fm, File path, Resource[] resources, int fileDataOffset, long arcSize, String dirName) {
    try {

      while (fm.getOffset() < fileDataOffset) { // loop over until a DNED entry drops back a level

        // 4 - Entry Type ("_RID" for directory start, "DNED" for directory end, "ELIF" for file)
        String entryType = fm.readString(4);

        // 4 - Entry Length (not including these 2 fields)
        //int entryLength = fm.readInt();
        //FieldValidator.checkLength(entryLength,arcSize);
        fm.skip(4);

        if (entryType.equals("_RID")) {
          // 2 - Directory Name Length
          short filenameLength = fm.readShort();
          FieldValidator.checkFilenameLength(filenameLength);

          // X - Directory Name
          String folderName = dirName + fm.readString(filenameLength) + "\\";
          //System.out.println("Folder:\t" + folderName);

          readDirectory(fm, path, resources, fileDataOffset, arcSize, folderName);
        }
        else if (entryType.equals("DNED")) {
          // nothing else

          //System.out.println("Folder End");
          return;
        }
        else if (entryType.equals("ELIF")) {
          // 4 - File Offset (relative to the start of the file data) [+8]
          int offset = fm.readInt() + fileDataOffset + 8;
          FieldValidator.checkOffset(offset, arcSize);

          // 4 - File Length
          int length = fm.readInt();
          FieldValidator.checkLength(length, arcSize);

          // 2 - Filename Length
          short filenameLength = fm.readShort();
          FieldValidator.checkFilenameLength(filenameLength);

          // X - Filename
          String filename = fm.readString(filenameLength);
          if (filename.indexOf('.') <= 0) {
            filename += ".unknown";
          }
          filename = dirName + filename;

          //System.out.println("File:\t" + filename);

          //path,name,offset,length,decompLength,exporter
          resources[realNumFiles] = new Resource(path, filename, offset, length);

          TaskProgressManager.setValue(realNumFiles);
          realNumFiles++;
        }
        else {
          ErrorLogger.log("[NPK_0KPN] Unknown entry type: " + entryType);
        }

      }

    }
    catch (Throwable t) {
      logError(t);
    }
  }

}
