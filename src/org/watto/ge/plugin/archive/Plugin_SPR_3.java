/*
 * Application:  Game Extractor
 * Author:       wattostudios
 * Website:      http://www.watto.org
 * Copyright:    Copyright (c) 2002-2025 wattostudios
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
public class Plugin_SPR_3 extends ArchivePlugin {

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public Plugin_SPR_3() {

    super("SPR_3", "SPR_3");

    //         read write replace rename
    setProperties(true, false, false, false);

    setGames("Screamer Rally");
    setExtensions("spr"); // MUST BE LOWER CASE
    setPlatforms("PC");

    // MUST BE LOWER CASE !!!
    setFileTypes(new FileType("spr_tex", "Texture Image", FileType.TYPE_IMAGE),
        new FileType("spr_pal", "Color Palette", FileType.TYPE_PALETTE));

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

      // Number Of Files
      if (FieldValidator.checkNumFiles(fm.readInt())) {
        rating += 5;
      }

      // Number Of Files
      if (FieldValidator.checkNumFiles(fm.readInt())) {
        rating += 5;
      }

      long arcSize = fm.getLength();

      // File Data Length
      if (FieldValidator.checkLength(fm.readInt(), arcSize)) {
        rating += 5;
      }

      if (fm.readInt() == 0 && fm.readInt() == 0) {
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

      // 4 - Number of Images
      int numImages = fm.readInt();
      FieldValidator.checkNumFiles(numImages);

      // 4 - Number of Palettes
      int numPalettes = fm.readInt();
      FieldValidator.checkNumFiles(numPalettes);

      // 4 - File Data Length
      //int imagesOffset = (numImages * 24) + 12;
      //int paletteOffset = fm.readInt() + imagesOffset;
      fm.skip(4);

      int paletteOffset = (numImages * 24) + 12;
      FieldValidator.checkOffset(paletteOffset, arcSize);

      int imagesOffset = paletteOffset + (numPalettes * 512);
      FieldValidator.checkOffset(imagesOffset, arcSize);

      int numFiles = numImages + numPalettes;

      Resource[] resources = new Resource[numFiles];
      TaskProgressManager.setMaximum(numFiles);

      // Loop through directory
      for (int i = 0; i < numImages; i++) {

        // 4 - Unknown
        // 4 - Unknown
        fm.skip(8);

        // 4 - Image Width
        int width = fm.readInt();

        // 4 - Image Height
        int height = fm.readInt();

        // 4 - Image Data Offset (relative to the start of the Image Data)
        int offset = fm.readInt() + imagesOffset;

        // 4 - Color Palette ID (0-based index)
        int paletteID = fm.readInt();

        int length = width * height;
        FieldValidator.checkLength(length, arcSize);

        String filename = "Image_" + (i + 1) + ".spr_tex";

        //path,name,offset,length,decompLength,exporter
        Resource resource = new Resource(path, filename, offset, length);
        resource.addProperty("PaletteID", paletteID + numImages); // points to the resource number for the palette
        resource.addProperty("Width", width);
        resource.addProperty("Height", height);
        resources[i] = resource;

        TaskProgressManager.setValue(i);
      }

      // Loop through directory
      int realNumFiles = numImages;
      int offset = paletteOffset;
      int length = 512;
      for (int i = 0; i < numPalettes; i++) {

        String filename = "Palette_" + (i + 1) + ".spr_pal";

        //path,name,offset,length,decompLength,exporter
        resources[realNumFiles] = new Resource(path, filename, offset, length);
        realNumFiles++;
        offset += length;

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
