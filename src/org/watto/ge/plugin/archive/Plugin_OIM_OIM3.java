/*
 * Application:  Game Extractor
 * Author:       wattostudios
 * Website:      http://www.watto.org
 * Copyright:    Copyright (c) 2002-2021 wattostudios
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
import org.watto.io.converter.ByteConverter;
import org.watto.task.TaskProgressManager;

/**
**********************************************************************************************

**********************************************************************************************
**/
public class Plugin_OIM_OIM3 extends ArchivePlugin {

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public Plugin_OIM_OIM3() {

    super("OIM_OIM3", "OIM_OIM3");

    //         read write replace rename
    setProperties(true, false, false, false);

    setGames("Shadow Of Rome");
    setExtensions("oim");
    setPlatforms("PS2");

    // MUST BE LOWER CASE !!!
    setFileTypes(new FileType("oim_img", "Image File", FileType.TYPE_IMAGE),
        new FileType("oim_pal", "Color Palette", FileType.TYPE_PALETTE));

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
      if (fm.readString(4).equals("OIM3")) {
        rating += 50;
      }

      long arcSize = fm.getLength();

      // Archive Size
      if (FieldValidator.checkEquals(fm.readInt(), arcSize)) {
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

      // 4 - Header (OIM3)
      // 4 - Archive Length
      // 4 - Unknown
      fm.skip(12);

      // 4 - Unknown (10)
      int paletteCount = fm.readInt();
      FieldValidator.checkNumFiles(paletteCount);

      // 4 - First Image Data Offset
      int numFiles = (fm.readInt() / 16) - 1;
      FieldValidator.checkNumFiles(numFiles);

      fm.relativeSeek(16);

      Resource[] resources = new Resource[numFiles];
      TaskProgressManager.setMaximum(numFiles);

      // Loop through directory
      for (int i = 0; i < numFiles; i++) {

        // 4 - Image Data Offset
        int offset = fm.readInt();
        FieldValidator.checkOffset(offset, arcSize);

        // 4 - Image Data Length
        int length = fm.readInt();
        FieldValidator.checkLength(length, arcSize);

        // 1 - Image Width [2*]
        int width = (int) Math.pow(2, ByteConverter.unsign(fm.readByte()));
        FieldValidator.checkWidth(width);

        // 1 - Image Height [2*]
        int height = (int) Math.pow(2, ByteConverter.unsign(fm.readByte()));
        FieldValidator.checkHeight(height);

        // 2 - Palette Format (19=8bit palette, 20=4-bit palette)
        short paletteFormat = fm.readShort();

        // 4 - Unknown
        fm.skip(4);

        if (paletteFormat == 0) {
          // Palette

          String filename = Resource.generateFilename(i) + ".oim_pal";

          //path,name,offset,length,decompLength,exporter
          Resource resource = new Resource(path, filename, offset, length);
          resource.addProperty("ColorCount", width);
          resources[i] = resource;
        }
        else {
          // Image

          String filename = Resource.generateFilename(i) + ".oim_img";

          //path,name,offset,length,decompLength,exporter
          Resource resource = new Resource(path, filename, offset, length);
          resource.addProperty("Width", width);
          resource.addProperty("Height", height);
          resource.addProperty("PaletteFormat", paletteFormat);
          resource.addProperty("FileID", paletteCount);
          resources[i] = resource;

          paletteCount++;
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
