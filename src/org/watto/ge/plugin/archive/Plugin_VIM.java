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
import org.watto.ge.plugin.ExporterPlugin;
import org.watto.ge.plugin.exporter.Exporter_ZLib;
import org.watto.io.FileManipulator;
import org.watto.io.FilenameSplitter;
import org.watto.task.TaskProgressManager;

/**
**********************************************************************************************

**********************************************************************************************
**/
public class Plugin_VIM extends ArchivePlugin {

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public Plugin_VIM() {

    super("VIM", "VIM");

    //         read write replace rename
    setProperties(true, false, false, false);

    setGames("Broken Sword 2: The Smoking Mirror");
    setExtensions("vim"); // MUST BE LOWER CASE
    setPlatforms("PC");

    // MUST BE LOWER CASE !!!
    //setFileTypes(new FileType("txt", "Text Document", FileType.TYPE_DOCUMENT),
    //             new FileType("bmp", "Bitmap Image", FileType.TYPE_IMAGE)
    //             );

    //setTextPreviewExtensions("colours", "rat", "screen", "styles"); // LOWER CASE

    //setCanScanForFileTypes(true);

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
      else if (FilenameSplitter.getExtension(fm.getFile()).toLowerCase().equals("dat")) {
        // see if there's a corresponding DIC file
        try {
          getDirectoryFile(fm.getFile(), "DIC");
          rating += 25;
        }
        catch (Throwable t) {
          getDirectoryFile(fm.getFile(), "dic");
          rating += 25;
        }
      }

      // 2 - Image Width
      if (FieldValidator.checkWidth(fm.readShort())) {
        rating += 5;
      }

      // 2 - Image Height
      if (FieldValidator.checkHeight(fm.readShort())) {
        rating += 5;
      }

      long arcSize = fm.getLength();

      // 4 - Compressed File Length (NOT including these header fields)
      if (FieldValidator.checkLength(fm.readInt(), arcSize)) {
        rating += 5;
      }

      // X - File Data (ZLib Compression)
      if (fm.readString(1).equals("x")) {
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

      ExporterPlugin exporter = Exporter_ZLib.getInstance();

      // RESETTING GLOBAL VARIABLES

      FileManipulator fm = new FileManipulator(path, false, 12);// small quick reads

      long arcSize = fm.getLength();

      int numFiles = Archive.getMaxFiles();

      Resource[] resources = new Resource[numFiles];
      TaskProgressManager.setMaximum(arcSize);

      // Loop through directory
      int realNumFiles = 0;
      while (fm.getOffset() < arcSize) {
        // 2 - Image Width
        short width = fm.readShort();
        FieldValidator.checkWidth(width);

        // 2 - Image Height
        short height = fm.readShort();
        FieldValidator.checkHeight(height);

        // 4 - Compressed File Length (NOT including these header fields)
        int length = fm.readInt();
        FieldValidator.checkLength(length, arcSize);

        int decompLength = width * height * 4;

        // X - File Data (ZLib Compression)
        long offset = fm.getOffset();
        fm.skip(length);

        // X - null Padding to a multiple of 4? bytes
        fm.skip(calculatePadding(length, 4));

        String filename = Resource.generateFilename(realNumFiles) + ".rgba";

        //path,name,offset,length,decompLength,exporter
        Resource resource = new Resource(path, filename, offset, length, decompLength, exporter);
        resource.addProperty("Width", width);
        resource.addProperty("Height", height);
        resources[realNumFiles] = resource;
        realNumFiles++;

        TaskProgressManager.setValue(offset);
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
