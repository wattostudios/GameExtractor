/*
 * Application:  Game Extractor
 * Author:       wattostudios
 * Website:      http://www.watto.org
 * Copyright:    Copyright (c) 2002-2026 wattostudios
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
import org.watto.datatype.Palette;
import org.watto.datatype.Resource;
import org.watto.ge.helper.FieldValidator;
import org.watto.ge.helper.ImageFormatReader;
import org.watto.ge.helper.PaletteManager;
import org.watto.ge.plugin.ArchivePlugin;
import org.watto.io.FileManipulator;
import org.watto.task.TaskProgressManager;

/**
**********************************************************************************************

**********************************************************************************************
**/
public class Plugin_TEX_8 extends ArchivePlugin {

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public Plugin_TEX_8() {

    super("TEX_8", "TEX_8");

    //         read write replace rename
    setProperties(true, false, false, false);

    setGames("Disney's Tarzan");
    setExtensions("tex"); // MUST BE LOWER CASE
    setPlatforms("PC");

    // MUST BE LOWER CASE !!!
    setFileTypes(new FileType("palette", "Color Palette", FileType.TYPE_PALETTE),
        new FileType("image", "Texture Image", FileType.TYPE_IMAGE));

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

      if (fm.readInt() == 5) {
        rating += 5;
      }

      long arcSize = fm.getLength();

      // Archive Size
      if (FieldValidator.checkEquals(fm.readInt(), arcSize)) {
        rating += 5;
      }

      // Number Of Images
      if (FieldValidator.checkNumFiles(fm.readShort())) {
        rating += 5;
      }

      // Number Of Palettes
      if (FieldValidator.checkNumFiles(fm.readShort())) {
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
      PaletteManager.clear();

      FileManipulator fm = new FileManipulator(path, false);

      long arcSize = fm.getLength();

      // 4 - Unknown (5)
      // 4 - Archive Length
      fm.skip(8);

      // 2 - Number of Images
      short numImages = fm.readShort();
      FieldValidator.checkNumFiles(numImages);

      // 2 - Number of Palettes
      short numPalettes = fm.readShort();
      FieldValidator.checkNumFiles(numPalettes);

      // 4 - Image Data Offset [+12]
      int imageDataOffset = fm.readInt() + 12;
      FieldValidator.checkOffset(imageDataOffset, arcSize);

      // 4 - Palette Data Offset [+16]
      int paletteDataOffset = fm.readInt() + 16;
      FieldValidator.checkOffset(paletteDataOffset, arcSize);

      int numFiles = numImages + numPalettes;

      Resource[] resources = new Resource[numFiles];
      TaskProgressManager.setMaximum(numFiles);

      int realNumFiles = 0;

      // Loop through the palettes
      fm.seek(paletteDataOffset);

      int[] paletteBPP = new int[numPalettes];
      for (int i = 0; i < numPalettes; i++) {
        long offset = fm.getOffset();

        // 4 - Number of Colors
        int numColors = fm.readInt();
        FieldValidator.checkRange(numColors, 1, 256);

        // for each color
        //   2 - Color (RGBA5551)
        //int[] palette = ImageFormatReader.readRGBA5551(fm, numColors, 1).getImagePixels();
        int[] palette = ImageFormatReader.swapRedAndBlue(ImageFormatReader.readRGBA5551(fm, numColors, 1)).getImagePixels();
        PaletteManager.addPalette(new Palette(palette), false);

        int length = (numColors * 2) + 4;

        String filename = Resource.generateFilename(i) + ".palette";

        //path,name,offset,length,decompLength,exporter
        resources[realNumFiles] = new Resource(path, filename, offset, length);

        if (numColors <= 16) {
          paletteBPP[i] = 4;
        }
        else {
          paletteBPP[i] = 8;
        }

        TaskProgressManager.setValue(realNumFiles);
        realNumFiles++;
      }

      // Loop through the images
      fm.relativeSeek(imageDataOffset);

      for (int i = 0; i < numImages; i++) {
        long offset = fm.getOffset();
        //System.out.println("Image at " + offset);

        // 2 - Image Width
        short width = fm.readShort();
        FieldValidator.checkWidth(width);

        // 2 - Image Height
        short height = fm.readShort();
        FieldValidator.checkHeight(height);

        // 4 - Unknown
        // 4 - null
        fm.skip(8);

        // 1 - 4-bpp Palette Number
        // 1 - 8-bpp Palette Number
        // 2 - null
        //int paletteNumber = fm.readInt();
        fm.skip(4);

        // X - Image Data
        int length = width * height * 2;
        fm.skip(length);

        length += 16; // for the image header

        String filename = Resource.generateFilename(i) + ".image";

        //path,name,offset,length,decompLength,exporter
        resources[realNumFiles] = new Resource(path, filename, offset, length);

        TaskProgressManager.setValue(realNumFiles);
        realNumFiles++;
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
