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
import org.watto.ErrorLogger;
import org.watto.Language;
import org.watto.Settings;
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
public class Plugin_N extends ArchivePlugin {

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public Plugin_N() {

    super("N", "N");

    //         read write replace rename
    setProperties(true, false, true, false);

    setGames("Ninja Gaiden 3: Razor's Edge",
        "Ninja Gaiden Sigma 2");
    setExtensions(""); // MUST BE LOWER CASE (extension varies on the number of files in the archive)
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

      // Number Of Files
      int numFiles = fm.readInt();
      if (FieldValidator.checkNumFiles(numFiles)) {
        rating += 5;
      }

      if (FieldValidator.checkExtension(fm, "" + (char) numFiles)) {
        rating += 25;
      }

      long arcSize = fm.getLength();

      // Archive Size
      if (FieldValidator.checkEquals(fm.readInt(), arcSize)) {
        rating += 5;
      }

      fm.skip(56);

      // DDS Header
      if (fm.readString(4).equals("DDS ")) {
        rating += 15;
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

      // 4 - Number of Images?
      int numFiles = fm.readInt();
      FieldValidator.checkNumFiles(numFiles);

      // 4 - Archive Length
      // 4 - Hash?
      // 52 - null
      fm.skip(60);

      Resource[] resources = new Resource[numFiles];
      TaskProgressManager.setMaximum(numFiles);

      // Loop through directory
      int paletteNumber = 0;
      for (int i = 0; i < numFiles; i++) {
        long offset = fm.getOffset();
        //System.out.println(offset);

        // 4 - DDS Header ("DDS ")
        fm.skip(4);

        // 4 - Header 1 Length (124)
        int headerLength = fm.readInt();
        FieldValidator.checkLength(headerLength, arcSize);

        // 4 - Flags
        fm.skip(4);

        // 4 - Height
        int height = fm.readInt();
        FieldValidator.checkHeight(height);

        // 4 - Width
        int width = fm.readInt();
        FieldValidator.checkWidth(width);

        // 4 - Linear Size
        // 4 - Depth (0)
        // 4 - Number Of Mipmaps (0)
        // 4 - Alpha Bit Depth (0)
        // 40 - Unknown (0)
        // 4 - Header 2 Length (32)
        // 4 - Flags 2
        // 4 - Format Code (0)
        fm.skip(68);

        // 4 - Color Bit Count (8)
        int bitCount = fm.readInt();
        if (bitCount != 8 && bitCount != 32) {
          ErrorLogger.log("[N] Unknown Bitcount: " + bitCount);
          return null;
        }

        // 4 - Red Bit Mask (255)
        // 4 - Green Bit Mask (0)
        // 4 - Blue Bit Mask (0)
        // 4 - Alpha Bit Mask (0)
        // 16 - DDCAPS2
        // 4 - Texture Stage
        fm.skip(36);

        // X - Image Data
        int length = 4 + headerLength + (width * height * (bitCount / 8));

        String filename = Resource.generateFilename(i) + ".dds";

        //path,name,offset,length,decompLength,exporter
        resources[i] = new Resource(path, filename, offset, length);

        offset += length;
        offset += calculatePadding(offset, 64);

        // for each image with has width=256 and is a 32-bit image, load it as a palette, where each row of the image is a new color palette
        if (bitCount == 32 && width == 256 && height < 20) {
          // palette
          for (int h = 0; h < height; h++) {
            int[] palette = ImageFormatReader.readPaletteBGRA(fm, 256);
            PaletteManager.addPalette(new Palette(palette));
          }
        }
        else if (bitCount == 8) {
          resources[i].addProperty("PaletteID", paletteNumber);
          paletteNumber++;
        }

        fm.seek(offset);

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
   * Writes an [archive] File with the contents of the Resources. The archive is written using
   * data from the initial archive - it isn't written from scratch.
   **********************************************************************************************
   **/
  @Override
  public void replace(Resource[] resources, File path) {
    try {

      FileManipulator fm = new FileManipulator(path, true);
      FileManipulator src = new FileManipulator(new File(Settings.getString("CurrentArchive")), false);

      int numFiles = resources.length;
      TaskProgressManager.setMaximum(numFiles);

      // Calculations
      TaskProgressManager.setMessage(Language.get("Progress_PerformingCalculations"));

      long archiveSize = 64;
      for (int i = 0; i < numFiles; i++) {
        int fileLength = (int) resources[i].getDecompressedLength();
        archiveSize += fileLength + calculatePadding(fileLength, 64);
      }

      // Write Header Data

      // 4 - Number of Images?
      fm.writeInt(numFiles);
      src.skip(4);

      // 4 - Archive Length
      fm.writeInt(archiveSize);
      src.skip(4);

      // 4 - Hash?
      // 52 - null
      fm.writeBytes(src.readBytes(56));

      // Write Files
      TaskProgressManager.setMessage(Language.get("Progress_WritingFiles"));
      for (int i = 0; i < resources.length; i++) {
        Resource resource = resources[i];

        // X - File Data (DDS)
        write(resource, fm);

        // X - Padding
        int fileLength = (int) resource.getDecompressedLength();
        int paddingSize = calculatePadding(fileLength, 64);
        for (int p = 0; p < paddingSize; p++) {
          fm.writeByte(0);
        }

        TaskProgressManager.setValue(i);
      }

      src.close();
      fm.close();

    }
    catch (Throwable t) {
      logError(t);
    }
  }

}
