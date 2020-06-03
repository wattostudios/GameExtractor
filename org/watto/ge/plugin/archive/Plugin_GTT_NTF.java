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
import org.watto.datatype.Resource;
import org.watto.ge.helper.FieldValidator;
import org.watto.ge.plugin.ArchivePlugin;
import org.watto.io.FileManipulator;
import org.watto.task.TaskProgressManager;

/**
**********************************************************************************************

**********************************************************************************************
**/
public class Plugin_GTT_NTF extends ArchivePlugin {

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public Plugin_GTT_NTF() {

    super("GTT_NTF", "GTT_NTF");

    //         read write replace rename
    setProperties(true, false, false, false);

    setGames("Knight Online",
        "Legend of Ares");
    setExtensions("gtt", "dxt"); // MUST BE LOWER CASE
    setPlatforms("PC");

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

      // 4 - Filename Length (can be null)
      int filenameLength = fm.readInt();
      if (FieldValidator.checkFilenameLength(filenameLength + 1)) { // +1 to allow empty filenames 
        rating += 5;
      }

      // X - Filename (only if FilenameLength > 0)
      fm.skip(filenameLength);

      // 4 - Header ("NTF" + (byte)3)
      String headerText = fm.readString(3);
      int headerByte = fm.readByte();
      if (headerText.equals("NTF") && headerByte == 3) {
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

        // 4 - Filename Length (can be null)
        int filenameLength = fm.readInt();
        FieldValidator.checkFilenameLength(filenameLength + 1); // +1 to allow empty filenames

        // X - Filename (only if FilenameLength > 0)
        String filename = "";
        if (filenameLength != 0) {
          filename = fm.readString(filenameLength);
        }
        else {
          filename = Resource.generateFilename(realNumFiles);
        }
        filename += ".ftn";

        // remove the D: from the beginning, if it exists.
        if (filename.length() > 3) {
          if (filename.charAt(1) == ':') {
            filename = filename.substring(3);
          }
        }

        long offset = fm.getOffset();

        // 4 - Header ("NTF" + (byte)3)
        fm.skip(4);

        // 4 - Image Width/Height
        int imageWidth = fm.readInt();
        FieldValidator.checkWidth(imageWidth);

        // 4 - Image Width/Height
        int imageHeight = fm.readInt();
        FieldValidator.checkWidth(imageHeight);

        // 4 - Image Format (DXT1)
        // Even though it says DXT1, it's actually DXT3/5 format
        fm.skip(4);

        // 4 - Unknown (1)
        fm.skip(4);

        // Calculate the length of all mipmaps, for all sizes down to 4x4 inclusive
        long length = 0;
        while (imageWidth >= 4 && imageHeight >= 4) {
          int calcImageWidth = imageWidth;
          int calcImageHeight = imageHeight;

          // force it to calculate based on minimum size of 4x4
          if (imageWidth < 4) {
            calcImageWidth = 4;
          }
          if (imageHeight < 4) {
            calcImageHeight = 4;
          }

          length += calcImageWidth * calcImageHeight;
          imageWidth /= 2;
          imageHeight /= 2;
        }

        length -= 8;

        // X - Mipmap Image Data
        fm.skip(length);

        length += 20; // because we're capturing the 5 4-byte header fields

        //path,name,offset,length,decompLength,exporter
        resources[realNumFiles] = new Resource(path, filename, offset, length);
        realNumFiles++;

        TaskProgressManager.setValue(fm.getOffset());
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
