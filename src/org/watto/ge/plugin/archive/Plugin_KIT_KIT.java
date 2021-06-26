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
import org.watto.ge.plugin.ExporterPlugin;
import org.watto.ge.plugin.exporter.Exporter_LZ4X;
import org.watto.io.FileManipulator;
import org.watto.task.TaskProgressManager;

/**
**********************************************************************************************

**********************************************************************************************
**/
public class Plugin_KIT_KIT extends ArchivePlugin {

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public Plugin_KIT_KIT() {

    super("KIT_KIT", "KIT_KIT");

    //         read write replace rename
    setProperties(true, false, false, false);

    setGames("Unravel");
    setExtensions("kit"); // MUST BE LOWER CASE
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
      if (fm.readString(4).equals("kit!")) {
        rating += 50;
      }

      // Version? (1)
      if (fm.readInt() == 1) {
        rating += 5;
      }

      fm.skip(8);

      long arcSize = fm.getLength();

      // Directory Length
      if (FieldValidator.checkLength(fm.readInt(), arcSize)) {
        rating += 5;
      }

      fm.skip(4);

      // Number Of Files
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

      ExporterPlugin exporter = Exporter_LZ4X.getInstance();

      // RESETTING GLOBAL VARIABLES

      FileManipulator fm = new FileManipulator(path, false);

      long arcSize = fm.getLength();

      // 4 - Header ("kit!")
      // 4 - Version? (1)
      // 4 - Unknown (1)
      // 4 - Unknown (1)
      fm.skip(16);

      // 4 - Directory 1 Length
      int dirLength = fm.readInt();
      FieldValidator.checkLength(dirLength, arcSize);

      // 4 - null
      fm.skip(4);

      // 4 - Number of Files
      int numFiles = fm.readInt();
      FieldValidator.checkNumFiles(numFiles);

      // 4 - Unknown (11)
      // 1 - Unknown (47)
      fm.skip(5);

      fm.skip(dirLength);

      Resource[] resources = new Resource[numFiles];
      TaskProgressManager.setMaximum(numFiles);

      // Loop through directory
      short previousKitNumber = -1;
      String baseFilePath = path.getAbsolutePath();
      for (int i = 0; i < numFiles; i++) {
        // 4 - Hash?
        // 4 - Unknown (9)
        fm.skip(8);

        // 2 - Kit File Number (eg 0 = *.kit.0, 1 = *.kit.1, etc)
        short kitNumber = fm.readShort();
        FieldValidator.checkPositive(kitNumber);

        if (kitNumber != previousKitNumber) {
          // Find the kit file - ensure it exists
          path = new File(baseFilePath + "." + kitNumber);
          if (!path.exists()) {
            ErrorLogger.log("[KIT_KIT]: Missing kit file number " + kitNumber);
            return null;
          }

          // get the length of the kit file, for field validation
          arcSize = path.length();

          previousKitNumber = kitNumber; // so we can re-use this for the next file
        }

        // 4 - File Offset (within the Kit File identified in the previous field)
        int offset = fm.readInt();
        FieldValidator.checkOffset(offset, arcSize);

        // 2 - null
        fm.skip(2);

        // 4 - Compressed Length
        int length = fm.readInt();
        FieldValidator.checkLength(length, arcSize);

        // 4 - null
        fm.skip(4);

        // 4 - Decompressed Length?
        int decompLength = fm.readInt();
        FieldValidator.checkLength(decompLength);

        // 4 - null
        // 20 - CRC?
        // 4 - null
        fm.skip(28);

        String filename = Resource.generateFilename(i);

        if (length == decompLength) {
          // No Compression
          //path,name,offset,length,decompLength,exporter
          resources[i] = new Resource(path, filename, offset, length);
          resources[i].forceNotAdded(true);
        }
        else {
          // LZ4X Compression
          //path,name,offset,length,decompLength,exporter
          resources[i] = new Resource(path, filename, offset, length, decompLength, exporter);
          resources[i].forceNotAdded(true);
        }

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
