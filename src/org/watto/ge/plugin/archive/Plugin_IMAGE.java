/*
 * Application:  Game Extractor
 * Author:       wattostudios
 * Website:      http://www.watto.org
 * Copyright:    Copyright (c) 2002-2022 wattostudios
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
import org.watto.io.FileManipulator;
import org.watto.task.TaskProgressManager;

/**
**********************************************************************************************

**********************************************************************************************
**/
public class Plugin_IMAGE extends ArchivePlugin {

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public Plugin_IMAGE() {

    super("IMAGE", "IMAGE");

    //         read write replace rename
    setProperties(true, false, false, false);

    setGames("Shantae and the Pirates Curse");
    setExtensions("image"); // MUST BE LOWER CASE
    setPlatforms("PC");

    // MUST BE LOWER CASE !!!
    setFileTypes(new FileType("image_tex", "Texture Image", FileType.TYPE_IMAGE));

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

      // Header
      if (fm.readInt() == 562804706) {
        rating += 50;
      }

      fm.skip(12);

      if (fm.readInt() == 64) {
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

      // 4 - Header (562804706)
      // 4 - null
      // 4 - Unknown
      // 4 - Unknown
      fm.skip(16);

      // 4 - Directory 1 Offset (64)
      int dirOffset = fm.readInt();
      FieldValidator.checkOffset(dirOffset, arcSize);

      // 4 - Unknown
      // 0-63 - null padding to a multiple of 64 bytes
      fm.relativeSeek(dirOffset);

      // 4 - Header (-1369523546)
      // 4 - null
      // 4 - Unknown
      // 4 - Unknown
      fm.skip(16);

      // 4 - Number of Images
      int numFiles = fm.readInt();
      FieldValidator.checkNumFiles(numFiles);

      // 4 - Unknown (1)
      // 4 - Unknown (36)
      fm.skip(8);

      // 4 - Number of Blocks
      int numBlocks = fm.readInt();
      FieldValidator.checkNumFiles(numBlocks);

      // 4 - Block Directory Offset (relative to the start of Directory 1)
      int blockDirOffset = fm.readInt() + dirOffset;
      FieldValidator.checkOffset(blockDirOffset, arcSize);

      // 8 - null

      // for each image
      //   4 - Unknown ID
      //   4 - Unknown ID

      // 0-63 - null padding to a multiple of 64 bytes

      int detailsDirOffset = blockDirOffset + (numBlocks * 32);
      detailsDirOffset += ArchivePlugin.calculatePadding(detailsDirOffset, 64);

      FieldValidator.checkOffset(detailsDirOffset, arcSize);

      fm.relativeSeek(detailsDirOffset);

      // 4 - Header (-1356604614)
      // 4 - Unknown
      // 4 - Header Length (16)
      // 4 - Unknown
      fm.skip(16);

      Resource[] resources = new Resource[numFiles];
      TaskProgressManager.setMaximum(numFiles);

      // Loop through directory
      int realNumFiles = 0;
      for (int i = 0; i < numFiles; i++) {
        // 2 - Image Width
        short width = fm.readShort();
        FieldValidator.checkWidth(width + 1); // +1 to allow empty

        // 2 - Image Height
        short height = fm.readShort();
        FieldValidator.checkHeight(height + 1); // +1 to allow empty

        // 4 - Unknown
        fm.skip(4);

        // 4 - Image Data Offset (relative to the start of the Image Details Directory)
        int offset = fm.readInt() + detailsDirOffset;
        FieldValidator.checkOffset(offset, arcSize);

        // 4 - null
        fm.skip(4);

        String filename = Resource.generateFilename(i) + ".image_tex";

        if (width != 0 && height != 0) {
          //path,name,offset,length,decompLength,exporter
          Resource resource = new Resource(path, filename, offset);
          resource.addProperty("Width", width);
          resource.addProperty("Height", height);
          resources[realNumFiles] = resource;
          realNumFiles++;
        }

        TaskProgressManager.setValue(i);
      }

      resources = resizeResources(resources, realNumFiles);
      calculateFileSizes(resources, arcSize);

      fm.close();

      return resources;

    }
    catch (Throwable t) {
      logError(t);
      return null;
    }
  }

  /**
  **********************************************************************************************
  If an archive doesn't have filenames stored in it, the scanner can come here to try to work out
  what kind of file a Resource is. This method allows the plugin to provide additional plugin-specific
  extensions, which will be tried before any standard extensions.
  @return null if no extension can be determined, or the extension if one can be found
  **********************************************************************************************
  **/
  @Override
  public String guessFileExtension(Resource resource, byte[] headerBytes, int headerInt1, int headerInt2, int headerInt3, short headerShort1, short headerShort2, short headerShort3, short headerShort4, short headerShort5, short headerShort6) {

    /*
    if (headerInt1 == 2037149520) {
      return "js";
    }
    */

    return null;
  }

}
