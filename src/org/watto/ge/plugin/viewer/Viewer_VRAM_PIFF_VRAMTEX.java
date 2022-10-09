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
import org.watto.ge.plugin.archive.Plugin_VRAM_PIFF;
import org.watto.io.FileManipulator;

/**
**********************************************************************************************

**********************************************************************************************
**/
public class Viewer_VRAM_PIFF_VRAMTEX extends ViewerPlugin {

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public Viewer_VRAM_PIFF_VRAMTEX() {
    super("VRAM_PIFF_VRAMTEX", "South Park Rally VRAM_TEX Image");
    setExtensions("vram_tex");

    setGames("South Park Rally");
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
      if (plugin instanceof Plugin_VRAM_PIFF) {
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

      // 4 - Palette Offset (64)
      if (fm.readInt() == 64) {
        rating += 5;
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

      fm.skip(12);

      // 4 - Image Data Offset (1088)
      if (fm.readInt() == 1088) {
        rating += 5;
      }

      // 4 - Palette Offset (64)
      if (fm.readInt() == 64) {
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

      long arcSize = fm.getLength();

      // 4 - Palette Offset (64)
      // 4 - Unknown (6)
      fm.skip(8);

      // 2 - Image Width
      short width = fm.readShort();
      FieldValidator.checkWidth(width);

      // 2 - Image Height
      short height = fm.readShort();
      FieldValidator.checkHeight(height);

      // 2 - Unknown
      fm.skip(2);

      // 2 - Image Format (16=RGBA4444, 8=Paletted, 4=4bitPaletted, 24=RGB, 32=RGBA)
      short imageFormat = fm.readShort();

      // 16 - Image Format
      int color1 = fm.readInt();
      int color2 = fm.readInt();
      int color3 = fm.readInt();
      int color4 = fm.readInt();

      // 4 - Image Data Offset (1088)
      int imageDataOffset = fm.readInt();
      FieldValidator.checkOffset(imageDataOffset, arcSize);

      // 4 - Palette Offset (64)
      int paletteOffset = fm.readInt();

      // 24 - null
      ImageResource imageResource = null;

      if (imageFormat == 4) {
        // 4-bit paletted

        FieldValidator.checkOffset(paletteOffset, arcSize);
        fm.seek(paletteOffset);

        // X - Palette
        int[] palette = ImageFormatReader.readPaletteRGBA(fm, 16);

        fm.seek(imageDataOffset); // should already be here, but just in case

        // X - Pixels
        imageResource = ImageFormatReader.read4BitPaletted(fm, width, height, palette);
        imageResource = ImageFormatReader.removeAlpha(imageResource);
      }
      else if (imageFormat == 32) {
        // RGBA

        fm.seek(imageDataOffset);

        imageResource = ImageFormatReader.readRGBA(fm, width, height);
      }
      else if (imageFormat == 16 || (color1 != 0 && color2 != 0 && color3 != 0 && color4 != 0)) {
        fm.seek(imageDataOffset);
        if (color2 == 31744 && color3 == 992 && color4 == 31) {
          // ARGB1555
          imageResource = ImageFormatReader.readARGB1555(fm, width, height);
          imageResource = ImageFormatReader.removeAlpha(imageResource);
        }
        else if (color1 == 61440 && color2 == 3840 && color3 == 240 && color4 == 15) {
          // ARGB4444
          imageResource = ImageFormatReader.readARGB4444(fm, width, height);
        }
        else {
          // RGBA4444
          imageResource = ImageFormatReader.readRGBA4444(fm, width, height);
        }
      }
      else if (color1 == 0 && color2 == 0 && color3 == 0 && color4 == 0) {
        // 8-bit paletted

        FieldValidator.checkOffset(paletteOffset, arcSize);
        fm.seek(paletteOffset);

        // X - Palette
        int[] palette = ImageFormatReader.readPaletteRGBA(fm, 256);

        fm.seek(imageDataOffset); // should already be here, but just in case

        // X - Pixels
        imageResource = ImageFormatReader.read8BitPaletted(fm, width, height, palette);
        imageResource = ImageFormatReader.removeAlpha(imageResource);
      }
      else if (color1 == 0 && color2 != 0 && color3 != 0 && color4 != 0) {
        fm.seek(imageDataOffset);
        if (color2 == 255 && color3 == 65280 && color4 == 16711680) {
          // RGB
          imageResource = ImageFormatReader.readRGB(fm, width, height);
        }
        else {
          // BGR
          imageResource = ImageFormatReader.readBGR(fm, width, height);
        }
      }
      else {
        ErrorLogger.log("[Viewer_VRAM_PIFF_VRAMTEX] Unknown image format: " + color1 + "\t" + color2 + "\t" + color3 + "\t" + color4);
      }

      if (imageResource == null) {
        return null;
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