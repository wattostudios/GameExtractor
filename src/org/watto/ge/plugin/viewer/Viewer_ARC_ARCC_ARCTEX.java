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
import org.watto.ge.helper.PaletteManager;
import org.watto.ge.plugin.AllFilesPlugin;
import org.watto.ge.plugin.ArchivePlugin;
import org.watto.ge.plugin.ViewerPlugin;
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
    super("ARC_ARCC_ARCTEX", "Street Racing Syndicate ARC_TEX Image");
    setExtensions("arc_tex");

    setGames("Street Racing Syndicate");
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
      if (plugin instanceof Plugin_ARC_ARCC) {
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

      // 4 - Image Width
      if (FieldValidator.checkWidth(fm.readInt())) {
        rating += 5;
      }

      // 4 - Image Height
      if (FieldValidator.checkHeight(fm.readInt())) {
        rating += 5;
      }

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

      // 4 - Image Width
      int width = fm.readInt();
      FieldValidator.checkWidth(width);

      // 4 - Image Height
      int height = fm.readInt();
      FieldValidator.checkHeight(height);

      // 4 - Number of Mipmaps?
      // 4 - Hash?
      fm.skip(8);

      // 4 - Image Format (26=ARGB4444, 41=8bitPaletted, "DXT3"=DXT3)
      byte[] imageFormatBytes = fm.readBytes(4);
      int imageFormatInt = IntConverter.convertLittle(imageFormatBytes);
      String imageFormatString = StringConverter.convertLittle(imageFormatBytes);

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
      else if (imageFormatString.equals("DXT3")) {
        imageResource = ImageFormatReader.readDXT3(fm, width, height);
      }
      else if (imageFormatString.equals("DXT1")) {
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