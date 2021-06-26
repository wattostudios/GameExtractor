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
import org.watto.task.TaskProgressManager;

/**
**********************************************************************************************

**********************************************************************************************
**/
public class Plugin_DAT_66 extends ArchivePlugin {

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public Plugin_DAT_66() {

    super("DAT_66", "DAT_66");

    //         read write replace rename
    setProperties(true, false, false, false);

    setGames("Rollercoaster Tycoon Classic");
    setExtensions("dat"); // MUST BE LOWER CASE
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

      // Number Of Files
      if (FieldValidator.checkNumFiles(fm.readInt())) {
        rating += 5;
      }

      long arcSize = fm.getLength();

      // Directory Length
      if (FieldValidator.checkLength(fm.readInt(), arcSize)) {
        rating += 5;
      }

      // First File Offset
      if (FieldValidator.checkOffset(fm.readInt(), arcSize)) {
        rating += 5;
      }

      // Image Width
      if (FieldValidator.checkWidth(fm.readShort())) {
        rating += 5;
      }

      // Image Height
      if (FieldValidator.checkHeight(fm.readShort())) {
        rating += 4; // so all of them aren't = 25
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

      // 4 - Number of Files
      int numFiles = fm.readInt();
      FieldValidator.checkNumFiles(numFiles);

      // 4 - File Data Length
      fm.skip(4);

      long relativeOffset = 8 + (numFiles * 16);

      Resource[] resources = new Resource[numFiles];
      TaskProgressManager.setMaximum(numFiles);

      // Loop through directory
      for (int i = 0; i < numFiles; i++) {
        // 4 - File Offset (relative to the start of the File Data)
        long offset = fm.readInt() + relativeOffset;
        FieldValidator.checkOffset(offset, arcSize);

        // 2 - Image Width
        short imageWidth = fm.readShort();
        FieldValidator.checkWidth(imageWidth);

        // 2 - Image Height
        short imageHeight = fm.readShort();
        FieldValidator.checkWidth(imageHeight);

        // 2 - X Offset
        short xOffset = fm.readShort();

        // 2 - Y Offset
        short yOffset = fm.readShort();

        // 2 - Flags (only the lower 4 bits are relevant --> 1=bitmap, 5=compressed, 8=palette)
        int flags = fm.readShort() & 15;

        // 2 - Unknown
        fm.skip(2);

        String filename = Resource.generateFilename(i);
        if (flags == 1) {
          filename += ".bitmap";
        }
        else if (flags == 5) {
          filename += ".compressed_bitmap";
        }
        else if (flags == 8) {
          filename += ".palette";
        }

        //path,name,offset,length,decompLength,exporter
        Resource resource = new Resource(path, filename, offset);
        resource.addProperty("Width", "" + imageWidth);
        resource.addProperty("Height", "" + imageHeight);
        resource.addProperty("XOffset", "" + xOffset);
        resource.addProperty("YOffset", "" + yOffset);
        resources[i] = resource;

        TaskProgressManager.setValue(i);
      }

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
