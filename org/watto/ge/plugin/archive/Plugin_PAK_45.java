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
import org.watto.io.converter.ByteConverter;
import org.watto.task.TaskProgressManager;

/**
**********************************************************************************************

**********************************************************************************************
**/
public class Plugin_PAK_45 extends ArchivePlugin {

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public Plugin_PAK_45() {

    super("PAK_45", "PAK_45");

    //         read write replace rename
    setProperties(true, false, false, false);

    setGames("Chompy Chomp Chomp");
    setExtensions("pak"); // MUST BE LOWER CASE
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

      // 4 - Number of Groups
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

      // 4 - Number of Groups
      int numGroups = fm.readInt();
      FieldValidator.checkNumFiles(numGroups);

      int numFiles = Archive.getMaxFiles();
      int realNumFiles = 0;

      Resource[] resources = new Resource[numFiles];
      TaskProgressManager.setMaximum(numGroups);

      // Loop through directory
      for (int i = 0; i < numGroups; i++) {

        // 8 - File Offset
        long offset = fm.readLong();
        FieldValidator.checkOffset(offset, arcSize);

        // 4 - Number of Files in this Group
        int numFilesInGroup = fm.readInt();
        FieldValidator.checkNumFiles(numFilesInGroup);

        // Loop through the files in this group
        for (int j = 0; j < numFilesInGroup; j++) {
          // 1 - Filename Length
          int filenameLength = ByteConverter.unsign(fm.readByte());

          // X - Filename ("Song", "Fonts/White", etc)
          String filename = fm.readString(filenameLength) + "_" + realNumFiles;

          // 1 - Type Name Length
          int typeNameLength = ByteConverter.unsign(fm.readByte());

          // X - Type Name
          String typeName = fm.readString(typeNameLength);

          // 4 - File Length
          int length = fm.readInt();
          FieldValidator.checkLength(length, arcSize);

          int dotPos = typeName.lastIndexOf(".");
          if (dotPos < 0) {
            ErrorLogger.log("[PAK_45]: Can't handle type " + typeName);
            return null;
          }
          String shortType = typeName.substring(dotPos);

          filename += shortType;

          //System.out.println(fm.getOffset() + " - " + shortType);

          if (shortType.equals(".SpriteFontData")) {
            // 4 - Number of Blocks
            int numBlocks = fm.readInt();
            FieldValidator.checkNumFiles(numBlocks);

            // 4 - Unknown
            fm.skip(4);

            // for each block
            // 29 - Unknown
            fm.skip(29 * numBlocks);

            // 4 - null
            fm.skip(4);
          }
          else if (shortType.equals(".SoundEffectData")) {
            // nothing
          }
          else if (shortType.equals(".LevelData")) {
            // nothing
          }
          else if (shortType.equals(".TextureData")) {
            // 4 - Image Width?
            // 4 - Image Height?
            fm.skip(8);
          }
          else if (shortType.equals(".LevelDescriptionData")) {
            // 4 - Number of Blocks
            int numBlocks = fm.readInt();
            FieldValidator.checkNumFiles(numBlocks);

            // for each block
            for (int b = 0; b < numBlocks; b++) {
              // 1 - Short Name Length
              int shortNameLength = ByteConverter.unsign(fm.readByte());

              // X - Short Name
              fm.skip(shortNameLength);
              //System.out.println(" --> " + fm.readString(shortNameLength));

              // 1 - Unknown (0/1)
              fm.skip(1);

              // 1 - Long Name Length
              int longNameLength = ByteConverter.unsign(fm.readByte());

              // X - Long Name
              fm.skip(longNameLength);
              //System.out.println("     --> " + fm.readString(longNameLength));

              // 1 - Technical Name Length
              int techNameLength = ByteConverter.unsign(fm.readByte());

              // X - Technical Name
              fm.skip(techNameLength);

              // 8 - null
              // 8 - Hash?
              // 4 - Level ID Number
              fm.skip(20);
            }
          }
          else {
            ErrorLogger.log("[PAK_45]: Unknown data type " + shortType);
            return null;
          }

          //path,name,offset,length,decompLength,exporter
          resources[realNumFiles] = new Resource(path, filename, offset, length);
          realNumFiles++;

          offset += length;

        }

        TaskProgressManager.setValue(i);
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
