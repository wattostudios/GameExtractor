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
import org.watto.ge.plugin.archive.Plugin_BIN_34;
import org.watto.io.FileManipulator;

/**
**********************************************************************************************

**********************************************************************************************
**/
public class Viewer_BIN_34_BINTEX extends ViewerPlugin {

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public Viewer_BIN_34_BINTEX() {
    super("BIN_34_BINTEX", "007 BIN_TEX Image");
    setExtensions("bin_tex");

    setGames("007: Tomorrow Never Dies");
    setPlatforms("PSX");
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
      if (plugin instanceof Plugin_BIN_34) {
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

      if (fm.readInt() == 16) {
        rating += 5;
      }

      fm.skip(12);

      if (FieldValidator.checkNumColors(fm.readShort())) {
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

      // 4 - Unknown (16)
      // 4 - Unknown (9/8)
      // 4 - Unknown
      // 2 - Unknown
      // 2 - Unknown
      fm.skip(16);

      // 2 - Number of Colors (256/16)
      short numColors = fm.readShort();
      FieldValidator.checkNumColors(numColors);

      // 2 - Unknown (1)
      fm.skip(2);

      // for each color
      // 2 - Color (RGBA5551)
      ImageResource paletteImage = ImageFormatReader.readRGBA5551(fm, numColors, 1);
      paletteImage = ImageFormatReader.removeAlphaIfAllInvisible(paletteImage);
      paletteImage = ImageFormatReader.swapRedAndBlue(paletteImage);
      int[] palette = paletteImage.getImagePixels();

      // 4 - Image Data Length (including this field and the remaining fields in this file)
      // 2 - Unknown (640)
      // 2 - Unknown (384)
      fm.skip(8);

      // 2 - Image Width? [*2]
      short width = fm.readShort();
      if (numColors == 256) {
        width *= 2;
      }
      else if (numColors == 16) {
        width *= 4;
      }
      FieldValidator.checkWidth(width);

      // 2 - Image Height
      short height = fm.readShort();
      FieldValidator.checkHeight(height);

      // for each pixel
      // 1 - Palette Index
      ImageResource imageResource = null;
      if (numColors == 256) {
        imageResource = ImageFormatReader.read8BitPaletted(fm, width, height, palette);
      }
      else if (numColors == 16) {
        imageResource = ImageFormatReader.read4BitPaletted(fm, width, height, palette);
      }
      else {
        ErrorLogger.log("[Viewer_BIN_34_BINTEX] Unknown Number of Colors: " + numColors);
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