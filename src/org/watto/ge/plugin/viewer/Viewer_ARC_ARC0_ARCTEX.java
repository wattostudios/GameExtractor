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

package org.watto.ge.plugin.viewer;

import org.watto.ErrorLogger;
import org.watto.component.PreviewPanel;
import org.watto.component.PreviewPanel_Image;
import org.watto.datatype.Archive;
import org.watto.datatype.ImageResource;
import org.watto.ge.helper.FieldValidator;
import org.watto.ge.helper.ImageFormatReader;
import org.watto.ge.helper.ImageSwizzler;
import org.watto.ge.plugin.AllFilesPlugin;
import org.watto.ge.plugin.ArchivePlugin;
import org.watto.ge.plugin.ViewerPlugin;
import org.watto.ge.plugin.archive.Plugin_ARC_ARC0;
import org.watto.ge.plugin.archive.Plugin_ARC_ARCC;
import org.watto.io.FileManipulator;
import org.watto.io.buffer.ByteBuffer;

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

    setGames("Big Mutha Truckers",
        "Street Racing Syndicate");
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
      else if (plugin instanceof Plugin_ARC_ARCC) {
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
      fm.skip(4);

      // 2 - Height
      int height = fm.readShort();

      // 2 - Unknown
      // 2 - Unknown
      // 2 - Unknown
      // 2 - Unknown
      fm.skip(8);

      // 2 - Width [*2]
      int width = fm.readShort() * 2;

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

      fm.skip(104);

      // 4 - Number of Colors
      int numColors = fm.readInt();
      FieldValidator.checkNumColors(numColors);

      // 4 - Unknown
      // 8 - null
      fm.skip(12);

      int[] palette = ImageFormatReader.readPaletteRGBA(fm, numColors);

      if (numColors == 16) {
        // no striping

        // palette is padded to length 256 though (256 bytes, NOT 256 colors)
        fm.skip(192);
      }
      else {
        palette = ImageSwizzler.unstripePalettePS2(palette); // PS2 Striped Palette
      }

      // 4 - Unknown (3)
      // 4 - Unknown
      // 4 - Unknown (14)
      // 12 - null

      // 4 - Unknown (81)
      // 4 - null
      fm.skip(32);

      /*
      // 4 - Image Width [*2]
      int width = fm.readInt() * 2;
      FieldValidator.checkWidth(width);
      
      // 4 - Image Height [*2]
      int height = fm.readInt() * 2;
      FieldValidator.checkHeight(height);
      
      if (numColors == 16) {
        // 4-bit colors
        height *= 2; // another *2 here
      }
      */
      fm.skip(8);

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
      if (numColors == 16) {
        // 4-bit swizzled
        int numBytes = width * height / 2;

        byte[] pixelBytes = fm.readBytes(numBytes);
        pixelBytes = ImageSwizzler.unswizzlePS24BitSuba(pixelBytes, width, height);

        fm.close();
        fm = new FileManipulator(new ByteBuffer(pixelBytes));

        imageResource = ImageFormatReader.read4BitPaletted(fm, width, height, palette);

      }
      else if (numColors == 256) {
        // 8-bit swizzled
        imageResource = ImageFormatReader.read8BitPaletted(fm, width, height, palette);
        imageResource.setPixels(ImageSwizzler.unswizzlePS2(imageResource.getPixels(), width, height)); // PS2 swizzled images
      }
      else if (numColors == 1024) {
        // RGBA
        imageResource = ImageFormatReader.readRGBA(fm, width, height);
      }
      else {
        ErrorLogger.log("[Viewer_ARC_ARC0_ARCTEX] Unknown Number of Colors: " + numColors);
      }

      if (imageResource != null) {
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