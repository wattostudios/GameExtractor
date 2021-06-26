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
import org.watto.ge.helper.FieldValidator;
import org.watto.ge.helper.ImageFormatReader;
import org.watto.ge.plugin.AllFilesPlugin;
import org.watto.ge.plugin.ArchivePlugin;
import org.watto.ge.plugin.ViewerPlugin;
import org.watto.ge.plugin.archive.Plugin_DAT_DAVE;
import org.watto.io.FileManipulator;

/**
**********************************************************************************************

**********************************************************************************************
**/
public class Viewer_DAT_DAVE_TEX extends ViewerPlugin {

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public Viewer_DAT_DAVE_TEX() {
    super("DAT_DAVE_TEX", "DAT_DAVE_TEX Image");
    setExtensions("tex");

    setGames("Red Dead Revolver");
    setPlatforms("XBox");
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
      if (plugin instanceof Plugin_DAT_DAVE) {
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

      // 2 - Image Width
      short width = fm.readShort();
      FieldValidator.checkWidth(width);

      // 2 - Image Height
      short height = fm.readShort();
      FieldValidator.checkHeight(height);

      // 2 - Image Format (22=DXT1, 26=DXT5)
      int imageFormat = fm.readShort();

      // 2 - Unknown (1)
      // 2 - Unknown (1)
      // 4 - null
      fm.skip(8);

      // X - Pixels
      ImageResource imageResource = null;
      if (imageFormat == 8 || imageFormat == 11) { // greyscale, no palette
        imageResource = ImageFormatReader.read8BitPaletted(fm, width, height);
      }
      else if (imageFormat == 1 || imageFormat == 14) {
        int[] palette = ImageFormatReader.readPaletteBGRA(fm, 256);
        imageResource = ImageFormatReader.read8BitPaletted(fm, width, height, palette);
      }
      else if (imageFormat == 16) {
        int[] palette = ImageFormatReader.readPaletteBGRA(fm, 16);
        imageResource = ImageFormatReader.read4BitPaletted(fm, width, height, palette);
      }
      else if (imageFormat == 17) {
        imageResource = ImageFormatReader.readRGB(fm, width, height);
        imageResource = ImageFormatReader.flipVertically(imageResource);
      }
      else if (imageFormat == 22) {
        imageResource = ImageFormatReader.readDXT1(fm, width, height);
        imageResource = ImageFormatReader.flipVertically(imageResource);
      }
      else if (imageFormat == 24) {
        imageResource = ImageFormatReader.readDXT3(fm, width, height);
        imageResource = ImageFormatReader.flipVertically(imageResource);
      }
      else if (imageFormat == 26) {
        imageResource = ImageFormatReader.readDXT5(fm, width, height);
        imageResource = ImageFormatReader.flipVertically(imageResource);
      }
      else {
        ErrorLogger.log("[Viewer_DAT_DAVE_TEX] Unknown Image Format: " + imageFormat);
      }

      fm.close();

      //ColorConverter.convertToPaletted(resource);

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