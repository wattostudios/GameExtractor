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
import org.watto.task.TaskProgressManager;
import org.watto.datatype.Archive;
import org.watto.datatype.Resource;
import org.watto.ge.helper.FieldValidator;
import org.watto.ge.plugin.ArchivePlugin;
import org.watto.io.FileManipulator;
import org.watto.io.converter.ByteConverter;

/**
**********************************************************************************************

**********************************************************************************************
**/
public class Plugin_TMF_TMUF extends ArchivePlugin {

  static int[] palette = new int[0];

  /**
   **********************************************************************************************
   * Get the color palette used by all images in this archive
   **********************************************************************************************
   **/
  public static int[] getPalette() {
    return palette;
  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public Plugin_TMF_TMUF() {

    super("TMF_TMUF", "TMF_TMUF");

    //         read write replace rename
    setProperties(true, false, false, false);

    setGames("Test Drive Off-Road");
    setExtensions("tmf");
    setPlatforms("PC");

    setFileTypes("spr", "Sprite Image");

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
      if (fm.readString(4).equals("TMUF")) {
        rating += 50;
      }

      // Palette Header
      if (fm.readString(4).equals("PAL ")) {
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

      // 4 - Header (TMUF)
      // 4 - Palette Header (PAL )
      fm.skip(8);

      // Read the color palette and store it against the plugin (for generating thumbnails and previews)
      int numColors = 256;
      palette = new int[numColors];
      for (int c = 0; c < numColors; c++) {
        // 1 - Red
        // 1 - Green
        // 1 - Blue
        // 1 - Alpha
        int r = ByteConverter.unsign(fm.readByte());
        int g = ByteConverter.unsign(fm.readByte());
        int b = ByteConverter.unsign(fm.readByte());
        int a = 255 - ByteConverter.unsign(fm.readByte());

        palette[c] = ((a << 24) | (r << 16) | (g << 8) | (b));
      }

      int numFiles = Archive.getMaxFiles();

      Resource[] resources = new Resource[numFiles];
      TaskProgressManager.setMaximum(arcSize);

      // Loop through directory
      int realNumFiles = 0;
      for (int i = 0; i < numFiles; i++) {
        long offset = fm.getOffset();
        FieldValidator.checkOffset(offset, arcSize);

        // 4 - Size Header (SIZE)
        fm.skip(4);

        // 2 - Image Width
        int imageWidth = fm.readShort();

        // 2 - Image Height
        int imageHeight = fm.readShort();

        // 4 - Data Header (DATA)
        fm.skip(4);

        // for each pixel
        //   1 - Color Index
        long length = imageWidth * imageHeight;
        fm.skip(length);

        String filename = Resource.generateFilename(i) + ".spr";

        //path,name,offset,length,decompLength,exporter
        resources[realNumFiles] = new Resource(path, filename, offset, length);

        TaskProgressManager.setValue(offset);
        realNumFiles++;

        if (fm.getOffset() >= arcSize) {
          break;
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
