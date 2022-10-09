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
import org.watto.datatype.FileType;
import org.watto.datatype.Resource;
import org.watto.ge.helper.FieldValidator;
import org.watto.ge.plugin.ArchivePlugin;
import org.watto.io.FileManipulator;
import org.watto.io.converter.IntConverter;
import org.watto.io.converter.ShortConverter;
import org.watto.task.TaskProgressManager;

/**
**********************************************************************************************

**********************************************************************************************
**/
public class Plugin_PTEX_PTEX_2 extends ArchivePlugin {

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public Plugin_PTEX_PTEX_2() {

    super("PTEX_PTEX_2", "PTEX_PTEX_2");

    //         read write replace rename
    setProperties(true, false, false, false);

    setGames("Sonic and SEGA All Stars Racing");
    setExtensions("ptex"); // MUST BE LOWER CASE
    setPlatforms("Wii");

    // MUST BE LOWER CASE !!!
    setFileTypes(new FileType("cmpr", "CMPR Image", FileType.TYPE_IMAGE));

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
      if (fm.readString(4).equals("PTEX")) {
        rating += 50;
      }

      long arcSize = fm.getLength();

      // Directory Offset
      if (FieldValidator.checkOffset(IntConverter.changeFormat(fm.readInt()), arcSize)) {
        rating += 5;
      }

      // Directory Length
      if (FieldValidator.checkLength(IntConverter.changeFormat(fm.readInt()), arcSize)) {
        rating += 5;
      }

      fm.skip(12);

      // Number Of Files
      if (FieldValidator.checkNumFiles(IntConverter.changeFormat(fm.readInt()))) {
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

      int numFiles = Archive.getMaxFiles();

      Resource[] resources = new Resource[numFiles];
      TaskProgressManager.setMaximum(arcSize);

      // Loop through directory
      int realNumFiles = 0;
      while (fm.getOffset() < arcSize) {
        long offset = fm.getOffset();

        // 4 - Header (KFRM, SCNE, ...)
        String header = fm.readString(4);

        // 4 - File Length (including these 2 header fields)
        int length = IntConverter.changeFormat(fm.readInt());
        FieldValidator.checkLength(length, arcSize);

        // X - File Data
        if (header.equals("PTEX")) {
          // find all the images in here

          // 4 - Length of BlocksDirectory + ImagesDirectory + ImageData
          // 4 - Hash?
          // 8 - null
          fm.skip(16);

          // 4 - Number of Images
          int numImages = IntConverter.changeFormat(fm.readInt());
          FieldValidator.checkNumFiles(numImages);

          // 4 - Unknown
          fm.skip(4);

          for (int b = 0; b < numImages; b++) {
            // 4 - Unknown
            // 2 - Unknown
            // 2 - Unknown
            fm.skip(8);

            // 2 - Image Width
            short width = ShortConverter.changeFormat(fm.readShort());

            // 2 - Image Height
            short height = ShortConverter.changeFormat(fm.readShort());

            // 4 - File Offset [+16]
            int imageOffset = (int) (IntConverter.changeFormat(fm.readInt()) + 16 + offset);
            FieldValidator.checkOffset(imageOffset, arcSize);

            // 4 - Unknown
            // 4 - Unknown
            fm.skip(8);

            // 4 - File Length
            int imageLength = IntConverter.changeFormat(fm.readInt());
            FieldValidator.checkLength(imageLength, arcSize);

            // 4 - Unknown
            fm.skip(4);

            String filename = Resource.generateFilename(realNumFiles);
            if (imageLength != 0) {
              filename += ".cmpr";
            }

            //path,name,offset,length,decompLength,exporter
            Resource resource = new Resource(path, filename, imageOffset, imageLength);
            resource.addProperty("Width", width);
            resource.addProperty("Height", height);
            resources[realNumFiles] = resource;
            realNumFiles++;

            TaskProgressManager.setValue(imageOffset);
          }

          fm.seek(offset + length);

        }
        else {
          // return this as the actual file

          fm.skip(length - 8);

          String filename = Resource.generateFilename(realNumFiles) + "." + header;

          //path,name,offset,length,decompLength,exporter
          resources[realNumFiles] = new Resource(path, filename, offset, length);
          realNumFiles++;

          TaskProgressManager.setValue(offset);
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
