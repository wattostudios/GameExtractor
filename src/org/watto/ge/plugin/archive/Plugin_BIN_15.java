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
import org.watto.ge.helper.PaletteManager;
import org.watto.ge.plugin.ArchivePlugin;
import org.watto.io.FileManipulator;
import org.watto.io.converter.ByteConverter;
import org.watto.task.TaskProgressManager;

/**
**********************************************************************************************

**********************************************************************************************
**/
public class Plugin_BIN_15 extends ArchivePlugin {

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public Plugin_BIN_15() {

    super("BIN_15", "BIN_15");

    //         read write replace rename
    setProperties(true, false, false, false);

    setGames("Apache Havoc");
    setExtensions("bin");
    setPlatforms("PC");

    setFileTypes(new FileType("bin_pal", "Color Palette", FileType.TYPE_PALETTE),
        new FileType("bin_tex", "Texture Image", FileType.TYPE_IMAGE));

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

      if (fm.getFile().getName().equalsIgnoreCase("Textures.bin")) {
        rating += 25;
      }

      // Number Of Palettes
      if (FieldValidator.checkNumFiles(fm.readInt())) {
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

      // 4 - Number Of Palettes
      int numPalettes = fm.readInt();
      FieldValidator.checkNumFiles(numPalettes);

      // skip over the palettes
      int paletteSize = 256 * 4;
      long dirOffset = numPalettes * paletteSize + 4;
      FieldValidator.checkOffset(dirOffset, arcSize);

      fm.seek(dirOffset);

      // 4 - Number Of Images
      int numImages = fm.readInt();
      FieldValidator.checkNumFiles(numImages);

      int numFiles = numPalettes + numImages;
      FieldValidator.checkNumFiles(numFiles);

      Resource[] resources = new Resource[numFiles];
      TaskProgressManager.setMaximum(numFiles);

      int realNumFiles = 0;

      // Loop through the palettes
      long offset = 4;
      for (int i = 0; i < numPalettes; i++) {
        fm.relativeSeek(offset);

        // X - Palette (256*4) RGBA (although alpha is always 0)
        int[] palette = new int[256];

        for (int p = 0; p < 256; p++) {
          // INPUT = RGBA
          int rPixel = ByteConverter.unsign(fm.readByte());
          int gPixel = ByteConverter.unsign(fm.readByte());
          int bPixel = ByteConverter.unsign(fm.readByte());
          int aPixel = ByteConverter.unsign(fm.readByte());
          aPixel = 255;

          // OUTPUT = ARGB
          palette[p] = ((rPixel << 16) | (gPixel << 8) | bPixel | (aPixel << 24));
        }

        PaletteManager.addPalette(new Palette(palette));

        String filename = "Palette " + (i + 1) + ".bin_pal";

        //path,name,offset,length,decompLength,exporter
        resources[realNumFiles] = new Resource(path, filename, offset, paletteSize);

        TaskProgressManager.setValue(realNumFiles);
        realNumFiles++;

        offset += paletteSize;
      }

      fm.seek(dirOffset + 4);

      // Loop through the images
      for (int i = 0; i < numImages; i++) {
        offset = fm.getOffset();

        // 128 - Filename (null) (no extension)
        String filename = fm.readNullString(128);
        FieldValidator.checkFilename(filename);
        filename += ".bin_tex";

        //System.out.println((fm.getOffset() - 128) + "\t" + filename);

        // 4 - Unknown (1/3)
        int imageType = fm.readInt();

        if (imageType == 4) {
          // empty file

          //path,name,offset,length,decompLength,exporter
          resources[realNumFiles] = new Resource(path, filename, offset, 128 + 4);

          TaskProgressManager.setValue(realNumFiles);
          realNumFiles++;

          continue;
        }

        // 4 - Color Palette Number
        fm.skip(4);

        // 4 - Image Width
        int imageWidth = fm.readInt();
        FieldValidator.checkPositive(imageWidth);

        // 4 - Image Height
        int imageHeight = fm.readInt();
        FieldValidator.checkPositive(imageHeight);

        // 4 - Number of Frames? [+1]
        fm.skip(4);

        // 4 - Number Of Mipmaps
        int numMipmaps = fm.readInt();
        FieldValidator.checkPositive(numMipmaps);

        // for each mipmap
        //   w*h - Pixel Data (Palette Index)

        long length = 0;
        for (int m = 0; m < numMipmaps; m++) {
          length += (imageWidth * imageHeight);

          imageWidth /= 2;
          imageHeight /= 2;
        }

        if (imageType == 3) {
          length *= 2;
        }

        FieldValidator.checkLength(length, arcSize);
        fm.skip(length);

        length += (128 + 24);

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

}
