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
public class Plugin_VIS_VIS5 extends ArchivePlugin {

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public Plugin_VIS_VIS5() {

    super("VIS_VIS5", "VIS_VIS5");

    //         read write replace rename
    setProperties(true, false, false, false);

    setGames("Born Punk");
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
      if (fm.readString(4).equals("VIS5")) {
        rating += 50;
      }

      // Number Of Files
      if (FieldValidator.checkNumFiles(IntConverter.changeFormat(fm.readInt()))) {
        rating += 5;
      }

      fm.skip(3);

      // 4 - first offset (encrypted)
      if (fm.readInt() == -135119002) {
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

      // 4 - Header (VIS5)
      fm.skip(4);

      // 4 - Number Of Files
      int numFiles = IntConverter.changeFormat(fm.readInt());
      FieldValidator.checkNumFiles(numFiles);

      Resource[] resources = new Resource[numFiles];
      TaskProgressManager.setMaximum(numFiles);

      // DIRECTORY IS XOR WITH REPEATING KEY
      int dirSize = (numFiles * 20) + 6;
      dirSize += calculatePadding(dirSize, 16);
      byte[] directoryBytes = fm.readBytes(dirSize);
      //52,56,53,102,63,242,247,57
      for (int i = 0; i < dirSize - 16; i += 16) {
        directoryBytes[i] ^= 52;
        directoryBytes[i + 1] ^= 56;
        directoryBytes[i + 2] ^= 53;
        directoryBytes[i + 3] ^= 102;
        directoryBytes[i + 4] ^= 49;
        directoryBytes[i + 5] ^= 51;
        directoryBytes[i + 6] ^= 105;
        directoryBytes[i + 7] ^= 50;
        directoryBytes[i + 8] ^= 56;
        directoryBytes[i + 9] ^= 53;
        directoryBytes[i + 10] ^= 70; // ?
        directoryBytes[i + 11] ^= 51;
        directoryBytes[i + 12] ^= 103; // ?
        directoryBytes[i + 13] ^= 242;
        directoryBytes[i + 14] ^= 247;
        directoryBytes[i + 15] ^= 57;
      }

      fm.close();
      fm = new FileManipulator(new ByteBuffer(directoryBytes));

      FileManipulator tempFM = new FileManipulator(new File("C:\\out.tmp"), true);
      tempFM.writeBytes(directoryBytes);
      tempFM.close();

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
        if (fileType == 34034698) {
          fileTypeString = ".webp"; // WebP image with some kind of encryption
        }
        else if (fileType == 34034690) {
          fileTypeString = ".opus"; // OGG Opus Audio
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
