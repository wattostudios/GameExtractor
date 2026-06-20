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
import org.watto.ge.plugin.archive.Plugin_BIN_LF_2;
import org.watto.ge.plugin.archive.Plugin_GMD_GMDF;
import org.watto.io.FileManipulator;
import org.watto.io.buffer.ByteBuffer;
import org.watto.io.converter.ByteConverter;

/**
**********************************************************************************************

**********************************************************************************************
**/
public class Viewer_GMD_GMDF_TEX extends ViewerPlugin {

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public Viewer_GMD_GMDF_TEX() {
    super("GMD_GMDF_TEX", "GMD_GMDF_TEX Image");
    setExtensions("tme");

    setGames("Michigan: Report from Hell");
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
  public boolean canReplace(PreviewPanel panel) {
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
      if (plugin instanceof Plugin_GMD_GMDF || plugin instanceof Plugin_BIN_LF_2) {
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

      fm.skip(64);

      // 4 - Image Width
      if (FieldValidator.checkWidth(fm.readInt())) {
        rating += 5;
      }

      // 4 - Image Height
      if (FieldValidator.checkHeight(fm.readInt())) {
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

      // 2 - Unknown
      int imageFormat = ByteConverter.unsign(fm.readByte());

      // 2 - Unknown
      // 4 - null
      // 4 - Unknown
      // 4 - Unknown
      // 4 - Unknown (4)
      // 4 - Unknown
      // 4 - Unknown
      // 4 - Unknown
      // 4 - Unknown
      // 4 - Unknown
      // 4 - Unknown (80)
      // 4 - null
      // 4 - null
      // 4 - Unknown
      // 4 - Unknown (81)
      // 4 - null
      fm.skip(63);

      // 4 - Image Width
      int width = fm.readInt();
      FieldValidator.checkWidth(width);

      // 4 - Image Height
      int height = fm.readInt();
      FieldValidator.checkHeight(height);

      // 4 - Unknown (82)
      // 12 - null
      // 4 - Unknown (83)
      // 4 - null
      // 4 - Unknown
      // 4 - Unknown
      // 4 - Unknown (-1)
      // 4 - Unknown (-1)
      fm.skip(40);

      if (imageFormat == 16 || width * height > fm.getLength()) {
        imageFormat = 16;
      }

      // X - Pixels
      int numPixels = width * height;
      if (imageFormat == 16) {
        // 4-bit paletted
        numPixels /= 2;
      }
      else { // 76
        // 8-bit paletted
      }
      byte[] pixelBytes = fm.readBytes(numPixels);

      // 4 - Unknown (4)
      // 4 - Unknown
      // 4 - Unknown
      // 4 - Unknown
      // 4 - Unknown
      // 4 - Unknown
      // 4 - Unknown (80)
      // 4 - null
      // 4 - null
      // 4 - Unknown
      // 4 - Unknown (81)
      // 4 - null
      // 4 - Palette Width (16)
      // 4 - Palette Height (16)
      // 4 - Unknown (82)
      // 12 - null
      // 4 - Unknown (83)
      // 4 - null
      // 4 - Unknown
      // 4 - Unknown
      // 4 - Unknown (-1)
      // 4 - Unknown (-1)
      fm.skip(96);

      // X - Palette Data
      int numColors = 256;
      if (imageFormat == 16) {
        // 4-bit paletted
        numColors = 16;
      }
      int[] palette = ImageFormatReader.readPaletteRGBA(fm, numColors);
      palette = ImageFormatReader.doubleAlpha(palette);

      fm.close();
      fm = new FileManipulator(new ByteBuffer(pixelBytes));

      ImageResource imageResource = null;
      if (imageFormat == 16) {
        // 4-bit paletted
        imageResource = ImageFormatReader.read4BitPaletted(fm, width, height, palette);
      }
      else { // 76
        // 8-bit paletted
        palette = ImageSwizzler.stripePalettePS2(palette);

        imageResource = ImageFormatReader.read8BitPaletted(fm, width, height, palette);
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