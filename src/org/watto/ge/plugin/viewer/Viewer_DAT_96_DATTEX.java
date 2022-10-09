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

import org.watto.component.PreviewPanel;
import org.watto.component.PreviewPanel_Image;
import org.watto.datatype.Archive;
import org.watto.datatype.ImageResource;
import org.watto.ge.helper.FieldValidator;
import org.watto.ge.helper.ImageFormatReader;
import org.watto.ge.helper.ImageManipulator;
import org.watto.ge.plugin.AllFilesPlugin;
import org.watto.ge.plugin.ArchivePlugin;
import org.watto.ge.plugin.ViewerPlugin;
import org.watto.ge.plugin.archive.Plugin_DAT_96;
import org.watto.io.FileManipulator;

/**
**********************************************************************************************

**********************************************************************************************
**/
public class Viewer_DAT_96_DATTEX extends ViewerPlugin {

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public Viewer_DAT_96_DATTEX() {
    super("DAT_96_DATTEX", "ESPN NHL Hockey DAT_TEX Image");
    setExtensions("dat_tex");

    setGames("ESPN NHL Hockey");
    setPlatforms("PS2");
    setStandardFileFormat(false);
  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  @Override
  public boolean canWrite(PreviewPanel panel) {
    if (panel instanceof PreviewPanel_Image) {
      return true;
    }
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
      if (plugin instanceof Plugin_DAT_96) {
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

      int height = 128;
      int width = 128;

      int[] palette = ImageFormatReader.readPaletteRGBA(fm, 256);
      palette = ImageFormatReader.stripePalettePS2(palette);

      // X - Pixels
      ImageResource imageResource = ImageFormatReader.read8BitPaletted(fm, width, height, palette);
      imageResource = ImageFormatReader.doubleAlpha(imageResource);

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
    try {

      if (!(preview instanceof PreviewPanel_Image)) {
        return;
      }

      ImageManipulator im = new ImageManipulator((PreviewPanel_Image) preview);

      int imageWidth = im.getWidth();
      int imageHeight = im.getHeight();

      if (imageWidth == -1 || imageHeight == -1) {
        return;
      }

      // convert to paletted, and then stripe the palette
      im.convertToPaletted();
      im.changeColorCount(256);
      int[] palette = im.getPalette();
      palette = ImageFormatReader.stripePalettePS2(palette);
      im.setPalette(palette);

      // Generate all the mipmaps of the image
      ImageManipulator[] mipmaps = im.generatePalettedMipmaps();
      int mipmapCount = mipmaps.length;

      if (mipmapCount > 5) {
        mipmapCount = 5;
      }

      // work out the file length
      int fileLength = 1024 + (128 * 128) + (64 * 64) + (32 * 32) + (16 * 16) + (8 * 8);

      // X - Palette
      int numColors = palette.length;

      for (int i = 0; i < numColors; i++) {
        // INPUT = ARGB
        int pixel = palette[i];

        // 1 - Red
        int rPixel = (pixel >> 16) & 255;

        // 1 - Green
        int gPixel = (pixel >> 8) & 255;

        // 1 - Blue
        int bPixel = pixel & 255;

        // 1 - Alpha
        int aPixel = (pixel >> 24) & 255;

        // REDUCE THE ALPHA BY 1/2
        if (aPixel == 255) {
          aPixel = 128;
        }
        else {
          aPixel /= 2;
        }

        // OUTPUT = RGBA
        fm.writeByte(rPixel);
        fm.writeByte(gPixel);
        fm.writeByte(bPixel);
        fm.writeByte(aPixel);
      }

      // X - Mipmaps
      for (int i = 0; i < mipmapCount; i++) {
        ImageManipulator mipmap = mipmaps[i];

        int pixelCount = mipmap.getNumPixels();
        int[] pixels = mipmap.getPixels();

        for (int p = 0; p < pixelCount; p++) {
          fm.writeByte(pixels[p]);
        }

      }

      // Write unknown padding at the end
      int paddingCount = 25600 - fileLength;
      for (int p = 0; p < paddingCount; p++) {
        fm.writeByte(0);
      }

      fm.close();

    }
    catch (Throwable t) {
      logError(t);
    }
  }

}