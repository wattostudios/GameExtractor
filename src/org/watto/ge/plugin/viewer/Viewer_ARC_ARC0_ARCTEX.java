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

package org.watto.ge.plugin.viewer;

import org.watto.ErrorLogger;
import org.watto.component.PreviewPanel;
import org.watto.component.PreviewPanel_Image;
import org.watto.datatype.Archive;
import org.watto.datatype.ImageResource;
import org.watto.ge.helper.FieldValidator;
import org.watto.ge.helper.ImageFormatReader;
import org.watto.ge.plugin.AllFilesPlugin;
import org.watto.ge.plugin.ArchivePlugin;
import org.watto.ge.plugin.ViewerPlugin;
import org.watto.ge.plugin.archive.Plugin_ARC_ARC0;
import org.watto.io.FileManipulator;
import org.watto.io.converter.ByteConverter;

/**
**********************************************************************************************

**********************************************************************************************
**/
public class Viewer_ARC_ARC0_ARCTEX extends ViewerPlugin {

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public Viewer_ARC_ARC0_ARCTEX() {
    super("ARC_ARC0_ARCTEX", "ARC_TEX Image");
    setExtensions("arc_tex");

    setGames("Big Mutha Truckers");
    setPlatforms("PS2");
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
      if (plugin instanceof Plugin_ARC_ARC0) {
        rating += 50;
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

      // 4 - Unknown
      // 1 - Unknown (128)
      // 1 - Unknown (0/16)
      // 2 - null
      // 4 - Unknown
      // 4 - Unknown
      // 4 - Unknown

      // 4 - null
      // 4 - Unknown
      // 12 - null

      // 4 - Unknown (4)
      // 4 - Unknown
      // 4 - Unknown (14)
      // 4 - null
      // 4 - Unknown

      // 4 - Unknown
      // 4 - Unknown (80)
      // 8 - null
      // 4 - Unknown

      // 4 - Unknown (81)
      // 4 - null
      // 4 - Unknown (16/8)
      // 4 - Unknown (16/8)
      // 4 - Unknown (82)

      // 12 - null
      // 4 - Unknown (83)
      // 4 - null

      fm.skip(120);

      // 4 - Number of Colors [*4]
      int numColors = fm.readInt() * 4;
      FieldValidator.checkNumColors(numColors);

      if (numColors != 256) {
        return null; // only support 256-color images at the moment, not sure how to support 64-color images
      }

      // 4 - Unknown
      // 8 - null
      fm.skip(12);

      int[] palette = ImageFormatReader.readPaletteRGBA(fm, numColors);
      palette = ImageFormatReader.unstripePalettePS2(palette); // PS2 Striped Palette

      // 4 - Unknown (3)
      // 4 - Unknown
      // 4 - Unknown (14)
      // 12 - null

      // 4 - Unknown (81)
      // 4 - null
      fm.skip(32);

      // 4 - Image Width [*2]
      int width = fm.readInt() * 2;
      FieldValidator.checkWidth(width);

      // 4 - Image Height [*2]
      int height = fm.readInt() * 2;
      FieldValidator.checkHeight(height);

      // 4 - Unknown (82)
      // 12 - null
      // 4 - Unknown (83)

      // 4 - null
      // 4 - Unknown
      // 4 - Unknown
      // 8 - null
      fm.skip(40);

      // X - Pixels
      ImageResource imageResource = null;
      if (numColors == 256) {
        imageResource = ImageFormatReader.read8BitPaletted(fm, width, height, palette);
      }
      else if (numColors == 64) { // not working - not sure what the actual format is here
        int numPixels = width * height;
        int[] pixels = new int[numPixels];

        for (int i = 0; i < numPixels; i += 4) {
          int byte1 = ByteConverter.unsign(fm.readByte());
          int byte2 = ByteConverter.unsign(fm.readByte());
          int byte3 = ByteConverter.unsign(fm.readByte());

          // 11111122 22223333 33444444
          int palette1 = byte1 >> 2;
          int palette2 = (byte1 & 3) << 4 | byte2 >> 4;
          int palette3 = (byte2 & 15) << 2 | byte3 >> 6;
          int palette4 = (byte3 & 63) >> 2;

          System.out.println(palette1);
          System.out.println(palette2);
          System.out.println(palette3);
          System.out.println(palette4);

          pixels[i] = palette[palette1];
          pixels[i + 1] = palette[palette2];
          pixels[i + 2] = palette[palette3];
          pixels[i + 3] = palette[palette4];
        }

        imageResource = new ImageResource(pixels, width, height);

      }
      else {
        ErrorLogger.log("[Viewer_ARC_ARC0_ARCTEX] Unknown Number of Colors: " + numColors);
      }

      if (imageResource != null) {
        imageResource.setPixels(ImageFormatReader.unswizzlePS2(imageResource.getPixels(), width, height)); // PS2 swizzled images
        imageResource = ImageFormatReader.doubleAlpha(imageResource);
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