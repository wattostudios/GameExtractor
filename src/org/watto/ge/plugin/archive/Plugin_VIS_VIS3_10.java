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
import org.watto.datatype.FileType;
import org.watto.datatype.Resource;
import org.watto.ge.helper.FieldValidator;
import org.watto.ge.plugin.ArchivePlugin;
import org.watto.ge.plugin.ExporterPlugin;
import org.watto.ge.plugin.exporter.Exporter_Custom_VIS_VIS3_WEBP;
import org.watto.io.FileManipulator;
import org.watto.io.buffer.ByteBuffer;
import org.watto.io.converter.IntConverter;
import org.watto.task.TaskProgressManager;

/**
**********************************************************************************************

**********************************************************************************************
**/
public class Plugin_VIS_VIS3_10 extends ArchivePlugin {

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public Plugin_VIS_VIS3_10() {

    super("VIS_VIS3_10", "VIS_VIS3_10");

    //         read write replace rename
    setProperties(true, false, false, false);

    setGames("Stasis");
    setExtensions("vis"); // MUST BE LOWER CASE
    setPlatforms("PC");

    // MUST BE LOWER CASE !!!
    setFileTypes(new FileType("opus", "Ogg Opus Audio", FileType.TYPE_AUDIO),
        new FileType("webp", "WebP Image", FileType.TYPE_IMAGE));

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
      if (fm.readString(4).equals("VIS3")) {
        rating += 50;
      }

      // Number Of Files
      if (FieldValidator.checkNumFiles(IntConverter.changeFormat(fm.readInt()))) {
        rating += 5;
      }

      fm.skip(3);

      // 4 - first offset (encrypted)
      if (fm.readByte() == 48 && fm.readByte() == 100 && fm.readByte() == 50 && fm.readByte() == 56) {
        rating += 5;
      }
      else {
        rating = 0;
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

      ExporterPlugin webpExporter = Exporter_Custom_VIS_VIS3_WEBP.getInstance();

      // RESETTING GLOBAL VARIABLES

      FileManipulator fm = new FileManipulator(path, false);

      long arcSize = fm.getLength();

      // 4 - Header (VIS3)
      fm.skip(4);

      // 4 - Number Of Files
      int numFiles = IntConverter.changeFormat(fm.readInt());
      FieldValidator.checkNumFiles(numFiles);

      Resource[] resources = new Resource[numFiles];
      TaskProgressManager.setMaximum(numFiles);

      // DIRECTORY IS XOR WITH REPEATING KEY
      int dirSize = numFiles * 16 + 16; // +16 just so the for loop below doesn't IndexOutOfBounds, but should really just be +6
      byte[] directoryBytes = fm.readBytes(dirSize);
      //97,97,97,48,100,50,56,50,49,100,49,53,99,53,102,49
      for (int i = 0; i < dirSize; i += 16) {
        directoryBytes[i] ^= 97;
        directoryBytes[i + 1] ^= 97;
        directoryBytes[i + 2] ^= 97;
        directoryBytes[i + 3] ^= 48;
        directoryBytes[i + 4] ^= 100;
        directoryBytes[i + 5] ^= 50;
        directoryBytes[i + 6] ^= 56;
        directoryBytes[i + 7] ^= 50;

        directoryBytes[i + 8] ^= 49;
        directoryBytes[i + 9] ^= 100;
        directoryBytes[i + 10] ^= 49;
        directoryBytes[i + 11] ^= 53;
        directoryBytes[i + 12] ^= 99;
        directoryBytes[i + 13] ^= 52;
        directoryBytes[i + 14] ^= 102;
        directoryBytes[i + 15] ^= 49;
      }

      fm.close();
      fm = new FileManipulator(new ByteBuffer(directoryBytes));

      /*
      String header = fm.readString(3);
      int[] numbers = new int[16];
      for (int i = 0; i < 16; i++) {
        numbers[i] = IntConverter.changeFormat(fm.readInt());
        if (i == 2 || i == 6 || i == 10 || i == 14) {
          //numbers[i] -= 1509949440;
        }
      }
      */

      long relativeOffset = 8 + (numFiles * 16) + 6;

      // 3 - Header
      fm.skip(3);

      // Loop through directory
      for (int i = 0; i < numFiles; i++) {

        // 4 - File Offset
        long offset = IntConverter.changeFormat(fm.readInt()) + relativeOffset;
        FieldValidator.checkOffset(offset, arcSize);

        // 4 - File Compressed Length
        int length = IntConverter.changeFormat(fm.readInt());
        //FieldValidator.checkLength(length, arcSize);

        // 4 - File Decompressed Length
        /*
        //int decompLength = IntConverter.changeFormat(fm.readInt());
        //decompLength -= 1509949440;
        byte[] decompBytes = fm.readBytes(4);
        System.out.println(decompBytes[3] + "\t" + decompBytes[2] + "\t" + decompBytes[1] + "\t" + decompBytes[0]);
        byte[] decompBytesReal = new byte[] { decompBytes[3], decompBytes[2], decompBytes[1], 0 };
        int decompLength = IntConverter.convertLittle(decompBytesReal);
        FieldValidator.checkLength(decompLength);
        */
        fm.skip(4);

        // 4 - File Type
        int fileType = IntConverter.changeFormat(fm.readInt());

        String fileTypeString;
        if (fileType == 8) {
          fileTypeString = ".webp"; // WebP image with some kind of encryption
        }
        else if (fileType == 0) {
          fileTypeString = ".ogg"; // OGG Opus Audio
        }
        else {
          fileTypeString = "." + fileType;
        }

        String filename = Resource.generateFilename(i) + fileTypeString;

        //path,name,offset,length,decompLength,exporter
        Resource resource = new Resource(path, filename, offset, length);
        resources[i] = resource;

        if (fileTypeString.equals(".webp")) {
          resource.setExporter(webpExporter);
        }

        TaskProgressManager.setValue(i);

      }

      // for some reason, the vc## files don't have the right lengths in them, so we need to fix it
      calculateFileSizes(resources, arcSize);

      fm.close();

      return resources;

    }
    catch (Throwable t) {
      logError(t);
      return null;
    }
  }

}
