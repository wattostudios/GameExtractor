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
import org.watto.io.buffer.ByteBuffer;
import org.watto.io.converter.IntConverter;
import org.watto.task.TaskProgressManager;

/**
**********************************************************************************************

**********************************************************************************************
**/
public class Plugin_VIS_VIS3_3 extends ArchivePlugin {

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public Plugin_VIS_VIS3_3() {

    super("VIS_VIS3_3", "VIS_VIS3_3");

    //         read write replace rename
    setProperties(true, false, false, false);

    setGames("Deponia: The Complete Journey");
    setExtensions("vis"); // MUST BE LOWER CASE
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
      if (fm.readString(4).equals("VIS3")) {
        rating += 50;
      }

      // Number Of Files
      if (FieldValidator.checkNumFiles(IntConverter.changeFormat(fm.readInt()))) {
        rating += 5;
      }

      fm.skip(3);

      // 4 - first offset (encrypted)
      if (fm.readByte() == 99 && fm.readByte() == 56 && fm.readByte() == 49 && fm.readByte() == 57) {
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

      //ExporterPlugin exporter = Exporter_ZLib.getInstance();

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
      int dirSize = numFiles * 16 + 8; // +8 just so the for loop below doesn't IndexOutOfBounds, but should really just be +6
      byte[] directoryBytes = fm.readBytes(dirSize);
      for (int i = 0; i < dirSize; i += 8) {
        directoryBytes[i] ^= 48;
        directoryBytes[i + 1] ^= 48;
        directoryBytes[i + 2] ^= 54;
        directoryBytes[i + 3] ^= 99;
        directoryBytes[i + 4] ^= 56;
        directoryBytes[i + 5] ^= 49;
        directoryBytes[i + 6] ^= 57;
        directoryBytes[i + 7] ^= 54;
      }

      fm.close();
      fm = new FileManipulator(new ByteBuffer(directoryBytes));

      /*
      String header = fm.readString(3);
      int[] numbers = new int[16];
      for (int i = 0; i < 16; i++) {
        numbers[i] = IntConverter.changeFormat(fm.readInt());
        if (i == 2 || i == 6 || i == 10 || i == 14) {
          numbers[i] -= 1460078289;
        }
      }
      */

      //ExporterPlugin exporter = new Exporter_XOR_RepeatingKey(new int[] { 52, 99, 98, 54, 50, 54, 53, 97 });
      //ExporterPlugin pngExporter = Exporter_Custom_VIS_VIS3_PNG.getInstance();

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
        FieldValidator.checkLength(length, arcSize);

        // 4 - File Decompressed Length
        /*
        int decompLength = IntConverter.changeFormat(fm.readInt());
        decompLength -= 1460078289;
        //FieldValidator.checkLength(decompLength);
         */
        fm.skip(4);

        // 4 - File Type
        int fileType = IntConverter.changeFormat(fm.readInt());

        //System.out.println(offset + "\t" + length);

        String fileTypeString;
        if (fileType == 1459617800) {
          fileTypeString = ".webp"; // WebP image with some kind of encryption
        }
        else if (fileType == 1459617792) {
          fileTypeString = ".opus";
        }
        else {
          fileTypeString = "." + fileType;
        }

        String filename = Resource.generateFilename(i) + fileTypeString;

        //path,name,offset,length,decompLength,exporter
        resources[i] = new Resource(path, filename, offset, length);
        if (fileType == 1381325394) { // PNG
          //Set the brute-force decryption extractor
          //resources[i].setExporter(pngExporter);
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
