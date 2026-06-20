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
import org.watto.ge.plugin.archive.Plugin_FAB_FARC;
import org.watto.ge.plugin.archive.Plugin_FAC_FARC;
import org.watto.io.FileManipulator;
import org.watto.io.buffer.ByteBuffer;

/**
**********************************************************************************************

**********************************************************************************************
**/
public class Viewer_FAB_FARC_GIM extends ViewerPlugin {

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public Viewer_FAB_FARC_GIM() {
    super("FAB_FARC_GIM", "FAB_FARC_GIM Image");
    setExtensions("gim", "gimx");

    setGames("Sunday vs Magazine: Shūketsu! Chōjō Daikessen");
    setPlatforms("PSP");
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
      if (plugin instanceof Plugin_FAB_FARC || plugin instanceof Plugin_FAC_FARC) {
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

      if (fm.readInt() == 1) {
        rating += 5;
      }

      fm.skip(8);

      if (fm.readInt() == 64) {
        rating += 5;
      }

      // 4 - Image Data Length (ImageDataHeader + ImageData + Palette)
      if (FieldValidator.checkLength(fm.readInt(), fm.getLength())) {
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

      // 4 - Unknown (1)
      // 8 - null
      // 4 - Header Length (64)
      // 4 - Image Data Length (ImageDataHeader + ImageData + Palette)
      // 8 - null
      // 2 - Unknown
      // 2 - Unknown
      // 8 - null
      // 4 - Unknown
      // 4 - Unknown
      // 16 - null

      // 2 - Unknown
      // 2 - Unknown
      // 2 - Unknown
      // 2 - Unknown
      fm.skip(72);

      // 2 - Image Width
      short width = fm.readShort();
      FieldValidator.checkWidth(width);

      // 2 - Image Height
      short height = fm.readShort();
      FieldValidator.checkHeight(height);

      // 2 - Image Format? (4=4bit, 8=8-bit)
      short imageFormat = fm.readShort();

      // 2 - Unknown
      // 2 - Unknown
      // 2 - Unknown
      // 4 - null
      // 4 - Unknown (48)
      // 4 - Image Data Header Length? (64)
      fm.skip(18);

      // 4 - Palette Offset [+128]
      int imageDataLength = fm.readInt();
      FieldValidator.checkLength(imageDataLength, arcSize);

      // 4 - null
      // 2 - Unknown
      // 2 - Unknown
      // 2 - Unknown
      // 2 - Unknown
      // 4 - Image Data Offset? (128)
      // 12 - null
      fm.skip(28);

      ImageResource imageResource = null;

      if ((imageFormat == 32 || imageFormat == 16) && (width == 256 || width == 16) && height == 1) {
        // Has the palette first, then another header, then the image data

        // PALETTE
        int numColors = width;
        int[] palette = ImageFormatReader.readPaletteRGBA(fm, numColors);

        // 2 - Unknown
        // 2 - Unknown
        // 2 - Unknown
        // 2 - Unknown
        fm.skip(8);

        // 2 - Image Width
        width = fm.readShort();
        FieldValidator.checkWidth(width);

        // 2 - Image Height
        height = fm.readShort();
        FieldValidator.checkHeight(height);

        // 2 - Image Format? (4=4bit, 8=8-bit)
        imageFormat = fm.readShort();

        // 2 - Unknown
        // 2 - Unknown
        // 2 - Unknown
        // 4 - null
        // 4 - Unknown (48)
        // 4 - Image Data Header Length? (64)
        fm.skip(18);

        // 4 - Palette Offset [+128]
        fm.skip(4);

        // 4 - null
        // 2 - Unknown
        // 2 - Unknown
        // 2 - Unknown
        // 2 - Unknown
        // 4 - Image Data Offset? (128)
        // 12 - null
        fm.skip(28);

        // IMAGE DATA (swizzled by PSP)

        if (imageFormat == 8) {
          byte[] pixelBytes = fm.readBytes(width * height);
          pixelBytes = ImageSwizzler.unswizzlePSP8Bit(pixelBytes, width, height);

          fm.close();
          fm = new FileManipulator(new ByteBuffer(pixelBytes));
        }
        else if (imageFormat == 4) {
          byte[] pixelBytes = fm.readBytes(width * height / 2);
          pixelBytes = ImageSwizzler.unswizzlePSP4Bit(pixelBytes, width, height);

          fm.close();
          fm = new FileManipulator(new ByteBuffer(pixelBytes));
        }

        if (imageFormat == 8) {
          imageResource = ImageFormatReader.read8BitPaletted(fm, width, height, palette);
        }
        else if (imageFormat == 4) {
          imageResource = ImageFormatReader.read4BitPaletted(fm, width, height, palette);
        }
        else {
          ErrorLogger.log("[Viewer_FAB_FARC_GIM] Unknown Image Format: " + imageFormat);
          return null;
        }

      }
      else {
        // image data now, then the palette

        // IMAGE DATA (swizzled by PSP)
        byte[] pixelBytes = fm.readBytes(imageDataLength);
        if (imageFormat == 8) {
          pixelBytes = ImageSwizzler.unswizzlePSP8Bit(pixelBytes, width, height);
        }
        else if (imageFormat == 4) {
          pixelBytes = ImageSwizzler.unswizzlePSP4Bit(pixelBytes, width, height);
        }

        // PALETTE
        int numColors = 256;
        if (imageFormat == 4) {
          numColors = 16;
        }
        int[] palette = ImageFormatReader.readPaletteRGBA(fm, numColors);

        fm.close();
        fm = new FileManipulator(new ByteBuffer(pixelBytes));

        // X - Pixels
        if (imageFormat == 8) {
          imageResource = ImageFormatReader.read8BitPaletted(fm, width, height, palette);
        }
        else if (imageFormat == 4) {
          imageResource = ImageFormatReader.read4BitPaletted(fm, width, height, palette);
        }
        else {
          ErrorLogger.log("[Viewer_FAB_FARC_GIM] Unknown Image Format: " + imageFormat);
          return null;
        }
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