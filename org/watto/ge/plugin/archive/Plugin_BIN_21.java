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
import org.watto.io.converter.ByteConverter;
import org.watto.io.converter.IntConverter;
import org.watto.io.converter.ShortConverter;
import org.watto.task.TaskProgressManager;

/**
**********************************************************************************************

**********************************************************************************************
**/
public class Plugin_BIN_21 extends ArchivePlugin {

  /**
  **********************************************************************************************

  **********************************************************************************************
  **/
  public Plugin_BIN_21() {

    super("BIN_21", "BIN_21");

    //         read write replace rename
    setProperties(true, false, false, false);

    setGames("Chibi-Robo! Plug into Adventure!");
    setExtensions("bin"); // MUST BE LOWER CASE
    setPlatforms("GameCube");

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

      // 4 - Header ((byte)85,170,56,45)
      byte[] headerBytes = fm.readBytes(4);
      if (headerBytes[0] == 85 && ByteConverter.unsign(headerBytes[1]) == 170 && headerBytes[2] == 56 && headerBytes[3] == 45) {
        rating += 50;
      }

      // 4 - Directory Offset (32)
      if (FieldValidator.checkEquals(IntConverter.changeFormat(fm.readInt()), 32)) {
        rating += 5;
      }

      long arcSize = fm.getLength();

      // 4 - Directory Length (total length of both directories, without padding)
      if (FieldValidator.checkLength(IntConverter.changeFormat(fm.readInt()), arcSize)) {
        rating += 5;
      }

      // 4 - File Data Offset
      if (FieldValidator.checkOffset(IntConverter.changeFormat(fm.readInt()), arcSize)) {
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

      // 4 - Header ((byte)85,170,56,45)
      // 4 - Directory Offset (32)
      // 4 - Directory Length (total length of both directories, without padding)
      // 4 - File Data Offset
      // 16 - Padding ((byte)204)

      // The first entry in the Files Directory has an empty name (it's the root folder), and the Index field gives the number of files in the archive.

      // 2 - Entry Type (256=folder, 0=file)
      // 2 - Name Offset
      // 4 - Entry ID
      fm.skip(40);

      // 4 - Index of Last Entry in this Folder
      int numFiles = IntConverter.changeFormat(fm.readInt());
      FieldValidator.checkNumFiles(numFiles);

      fm.seek(32);

      Resource[] resources = new Resource[numFiles];
      TaskProgressManager.setMaximum(numFiles);

      // Loop through directory
      short[] entryTypes = new short[numFiles];
      int[] nameOffsets = new int[numFiles];
      for (int i = 0; i < numFiles; i++) {

        // 2 - Entry Type (256=folder, 0=file)
        short entryType = ShortConverter.changeFormat(fm.readShort());
        entryTypes[i] = entryType;

        // 2 - Name Offset
        int nameOffset = ShortConverter.unsign(ShortConverter.changeFormat(fm.readShort()));
        FieldValidator.checkOffset(nameOffset);
        nameOffsets[i] = nameOffset;

        // 4 - File Offset
        int offset = IntConverter.changeFormat(fm.readInt());
        FieldValidator.checkOffset(offset, arcSize);

        // 4 - File Length
        int length = IntConverter.changeFormat(fm.readInt());
        FieldValidator.checkLength(length, arcSize);

        //path,name,offset,length,decompLength,exporter
        resources[i] = new Resource(path, "", offset, length);

        TaskProgressManager.setValue(i);
      }

      // Loop through the filenames directory
      String[] names = new String[numFiles];
      for (int i = 0; i < numFiles; i++) {

        // X - Filename (null)
        String filename = fm.readNullString();
        //FieldValidator.checkFilename(filename); // to allow the root folder, which has an empty name
        names[i] = filename;
      }

      // Loop through and assign filenames
      String[] dirNames = new String[numFiles];
      int[] dirLastIndexes = new int[numFiles];
      int dirNameDepth = 0;

      int realNumFiles = 0;
      Resource[] realResources = new Resource[numFiles];
      for (int i = 0; i < numFiles; i++) {
        // work out if any of the directories have finished
        while (dirNameDepth > 0) {
          if (i >= dirLastIndexes[dirNameDepth - 1]) {
            // end of this directory - go back a level
            //System.out.println("--going back a level - thisIndexIs:" + i);
            dirNameDepth--;
          }
          else {
            // stop - the current directory is still valid
            break;
          }
        }

        if (entryTypes[i] == 256) {
          // folder

          // add the dirName to the end of the tree
          dirNames[dirNameDepth] = names[i];
          dirLastIndexes[dirNameDepth] = (int) resources[i].getLength();

          //System.out.println("--name:" + dirNames[dirNameDepth] + "\t\tthisIndexIs:" + i + "\t\tlastindex=" + dirLastIndexes[dirNameDepth]);

          dirNameDepth++;
        }
        else {
          // file
          Resource resource = resources[i];

          // Set the name
          String filename = "";
          for (int n = 1; n < dirNameDepth; n++) { // "1" to skip the empty root name 
            filename += dirNames[n] + "\\";
          }
          filename += names[i];
          //System.out.println(filename + "\tthisIndexIs:" + i);

          resource.setName(filename);
          resource.setOriginalName(filename);

          realResources[realNumFiles] = resource;
          realNumFiles++;

          TaskProgressManager.setValue(i);
        }
      }

      resources = resizeResources(realResources, realNumFiles);

      fm.close();

      return resources;

    }
    catch (Throwable t) {
      logError(t);
      return null;
    }
  }

}
