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
import org.watto.ge.plugin.AllFilesPlugin;
import org.watto.ge.plugin.ArchivePlugin;
import org.watto.ge.plugin.ViewerPlugin;
import org.watto.ge.plugin.archive.Plugin_TXC;
import org.watto.io.FileManipulator;
import org.watto.io.converter.ShortConverter;

/**
**********************************************************************************************

**********************************************************************************************
**/
public class Viewer_TXC_TXCTEX extends ViewerPlugin {

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public Viewer_TXC_TXCTEX() {
    super("TXC_TXCTEX", "Urban Chaos TXC_TEX Image");
    setExtensions("txc_tex");

    setGames("Urban Chaos");
    setPlatforms("PC");
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
      if (plugin instanceof Plugin_TXC) {
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

      fm.skip(4);

      // 2 - Image Width
      if (FieldValidator.checkWidth(fm.readShort())) {
        rating += 5;
      }

      // 2 - Image Height
      if (FieldValidator.checkHeight(fm.readShort())) {
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

      // 2 - Unknown (-1)
      fm.skip(2);

      // 2 - Has Alpha (1)
      short hasAlpha = fm.readShort();

      // 2 - Image Width
      short width = fm.readShort();
      FieldValidator.checkWidth(width);

      // 2 - Image Height
      short height = fm.readShort();
      FieldValidator.checkHeight(height);

      // 2 - Number of Colors
      int numColors = fm.readShort();
      FieldValidator.checkNumColors(numColors);

      // X - Color Palette
      int[] palette = null;
      if (hasAlpha == 1) {
        palette = ImageFormatReader.readGBAR4444(fm, numColors, 1).getImagePixels();
      }
      else {
        palette = ImageFormatReader.readRGB565(fm, numColors, 1).getImagePixels();
      }

      // Ref: https://github.com/Fire-Head/UCTxcTools/blob/master/tex2tga/main.cpp

      int iTempSize = 1;
      if (numColors > 2) {
        do {
          ++iTempSize;
        }
        while (numColors > 1 << iTempSize);
      }

      // X - Pixels
      int numPixels = width * height;
      int[] pixels = new int[numPixels];
      int pPixels = 0;
      int pPixelsPos = 0;
      int ppPixelsPos = 0;

      int TexPixelsData = ShortConverter.unsign(fm.readShort());
      int NextPixelsData = ShortConverter.unsign(fm.readShort());

      int n = 16;
      int colorIndex;

      // convert color palette indexes to pixel color

      for (int i = numPixels; i > 0; i--) {
        if (n <= iTempSize) {
          int a = iTempSize - n;
          int b = ShortConverter.unsign(((short) TexPixelsData));
          TexPixelsData = NextPixelsData;
          NextPixelsData = ShortConverter.unsign(fm.readShort());

          int c = ShortConverter.unsign(((short) TexPixelsData)) >> (16 - (iTempSize - n));
          n = 16 - (iTempSize - n);
          colorIndex = c | (b >> (16 - iTempSize));
          pPixelsPos = ppPixelsPos;
          TexPixelsData <<= a;
        }
        else {
          colorIndex = ShortConverter.unsign(((short) TexPixelsData)) >> (16 - iTempSize);
          TexPixelsData <<= iTempSize;
          n -= iTempSize;
        }

        if (colorIndex >= numColors) {
          colorIndex = 0;
        }

        pPixels = palette[colorIndex];
        pixels[pPixelsPos] = pPixels;
        pPixelsPos++;
        ppPixelsPos = pPixelsPos;

      }

      ImageResource imageResource = new ImageResource(pixels, width, height);

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