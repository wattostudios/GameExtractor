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

package org.watto.ge.plugin.viewer;

import org.watto.ErrorLogger;
import org.watto.component.PreviewPanel;
import org.watto.component.PreviewPanel_Image;
import org.watto.datatype.Archive;
import org.watto.datatype.ImageResource;
import org.watto.datatype.Palette;
import org.watto.datatype.PalettedImageResource;
import org.watto.ge.helper.FieldValidator;
import org.watto.ge.helper.ImageFormatReader;
import org.watto.ge.helper.ImageSwizzler;
import org.watto.ge.helper.PaletteManager;
import org.watto.ge.plugin.AllFilesPlugin;
import org.watto.ge.plugin.ArchivePlugin;
import org.watto.ge.plugin.ViewerPlugin;
import org.watto.ge.plugin.archive.Plugin_ARC_ARC0;
import org.watto.ge.plugin.archive.Plugin_ARC_ARCC;
import org.watto.io.FileManipulator;
import org.watto.io.converter.ByteConverter;
import org.watto.io.converter.IntConverter;
import org.watto.io.converter.StringConverter;

/**
**********************************************************************************************

**********************************************************************************************
**/
public class Viewer_ARC_ARCC_ARCTEX extends ViewerPlugin {

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public Viewer_ARC_ARCC_ARCTEX() {
    super("ARC_ARCC_ARCTEX", "ARC_ARCC_ARCTEX Image");
    setExtensions("arc_tex");

    setGames("Street Racing Syndicate",
        "Big Mutha Truckers");
    setPlatforms("PC", "XBox");
    setStandardFileFormat(false);
  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  @Override
  public boolean canWrite(PreviewPanel panel) {
    return false;
  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  @Override
  public int getMatchRating(FileManipulator fm) {
    try {

      int rating = 0;

      ArchivePlugin plugin = Archive.getReadPlugin();
      if (plugin instanceof Plugin_ARC_ARCC) {
        rating += 50;
      }
      else if (plugin instanceof Plugin_ARC_ARC0) {
        rating += 25;
      }
      else if (!(plugin instanceof AllFilesPlugin)) {
        return 0;
      }

      if (FieldValidator.checkExtension(fm, extensions)) {
        rating += 25;
      }
      else {
        return 0;
      }

      // 2 - Image Width
      if (FieldValidator.checkWidth(fm.readShort())) {
        rating += 5;
      }
      fm.skip(2);

      // 2 - Image Height
      if (FieldValidator.checkHeight(fm.readShort())) {
        rating += 5;
      }
      fm.skip(2);

      // 4 - Mipmap Count
      if (FieldValidator.checkRange(fm.readInt(), 0, 20)) {
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
  Reads a resource from the FileManipulator, and generates a PreviewPanel for it. The FileManipulator
  is an extracted temp file, not the original archive!
  **********************************************************************************************
  **/
  @Override
  public PreviewPanel read(FileManipulator fm) {
    try {

      ImageResource imageResource = readThumbnail(fm);

      if (imageResource == null) {
        return null;
      }

      PreviewPanel_Image preview = new PreviewPanel_Image(imageResource);

      return preview;

    }
    catch (Throwable t) {
      logError(t);
      return null;
    }
  }

  /**
  **********************************************************************************************
  Reads a resource from the FileManipulator, and generates a Thumbnail for it (generally, only
  an Image ViewerPlugin will do this, but others can do it if they want). The FileManipulator is
  an extracted temp file, not the original archive!
  **********************************************************************************************
  **/

  @Override
  public ImageResource readThumbnail(FileManipulator fm) {
    try {

      int width = 0;
      int height = 0;

      int imageFormatInt = 0;
      String imageFormatString = "";

      try {
        // PC

        // 4 - Image Width
        width = fm.readInt();
        FieldValidator.checkWidth(width);

        // 4 - Image Height
        height = fm.readInt();
        FieldValidator.checkHeight(height);

        // 4 - Number of Mipmaps?
        fm.skip(4);

        // 4 - Hash?
        int hash = fm.readInt();

        // 4 - Image Format (26=ARGB4444, 41=8bitPaletted, "DXT3"=DXT3)
        byte[] imageFormatBytes = fm.readBytes(4);
        imageFormatInt = IntConverter.convertLittle(imageFormatBytes);
        imageFormatString = StringConverter.convertLittle(imageFormatBytes);

        if (imageFormatInt < 0 || imageFormatInt >= 64) {
          imageFormatInt = hash; // Big Mutha Truckers
        }
      }
      catch (Throwable t) {
        fm.relativeSeek(0);

        // XBox

        // 2 - Image Width
        width = fm.readShort();
        FieldValidator.checkWidth(width);

        // 2 - Image Height
        height = fm.readShort();
        FieldValidator.checkHeight(height);

        // 2 - Image Width
        // 2 - Image Height
        fm.skip(4);

        // 4 - Number of Mipmaps?
        fm.skip(4);

        // 4 - Image Format?
        byte[] imageFormatBytes = fm.readBytes(4);
        imageFormatInt = IntConverter.convertLittle(imageFormatBytes);
        imageFormatString = StringConverter.convertLittle(imageFormatBytes);

        // 4 - Number of Pixels
        fm.skip(4);

      }

      // X - Pixels
      ImageResource imageResource = null;

      if (imageFormatInt == 41) {
        // 4 - Number of Palettes
        int numPalettes = fm.readInt();
        FieldValidator.checkRange(numPalettes, 1, 100);

        PaletteManager.clear();

        // X - Color Palettes (256*4)
        int[] firstPalette = null;
        for (int p = 0; p < numPalettes; p++) {
          int[] palette = ImageFormatReader.readPaletteRGBA(fm, 256);
          PaletteManager.addPalette(new Palette(palette));

          if (firstPalette == null) {
            firstPalette = palette;
          }
        }

        // X - Image Data
        //imageResource = ImageFormatReader.read8BitPaletted(fm, width, height, firstPalette);
        int numPixels = width * height;
        int[] pixels = new int[numPixels];
        for (int i = 0; i < numPixels; i++) {
          pixels[i] = ByteConverter.unsign(fm.readByte());
        }

        imageResource = new PalettedImageResource(pixels, width, height, firstPalette);
      }
      else if (imageFormatInt == 26) {
        imageResource = ImageFormatReader.readARGB4444(fm, width, height);
      }
      else if (imageFormatInt == 25) {
        // 8-bit Grayscale + Swizzle
        imageResource = ImageFormatReader.read8BitPaletted(fm, width, height);
        imageResource.setPixels(ImageSwizzler.unswizzle(imageResource.getPixels(), width, height, 1));
      }
      else if (imageFormatInt == 6) {
        // RGBA + Swizzle
        imageResource = ImageFormatReader.readRGBA(fm, width, height);
        imageResource.setPixels(ImageSwizzler.unswizzle(imageResource.getPixels(), width, height, 1));
      }
      else if (imageFormatInt == 2) {
        // RGB555 + Swizzle
        imageResource = ImageFormatReader.readRGB555(fm, width, height);
        imageResource.setPixels(ImageSwizzler.unswizzle(imageResource.getPixels(), width, height, 1));
      }
      else if (imageFormatString.equals("DXT3") || imageFormatInt == 14) {
        imageResource = ImageFormatReader.readDXT3(fm, width, height);
      }
      else if (imageFormatString.equals("DXT1") || imageFormatInt == 12) {
        imageResource = ImageFormatReader.readDXT1(fm, width, height);
      }
      else {
        ErrorLogger.log("[Viewer_ARC_ARCC_ARCTEX] Unknown Image Format: " + imageFormatInt + " - " + imageFormatString);
      }

      fm.close();

      return imageResource;

    }
    catch (Throwable t) {
      logError(t);
      return null;
    }
  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  @Override
  public void write(PreviewPanel preview, FileManipulator fm) {
  }

}