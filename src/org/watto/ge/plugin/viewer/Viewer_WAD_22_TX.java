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
import org.watto.ge.plugin.AllFilesPlugin;
import org.watto.ge.plugin.ArchivePlugin;
import org.watto.ge.plugin.ViewerPlugin;
import org.watto.ge.plugin.archive.Plugin_PATCH;
import org.watto.ge.plugin.archive.Plugin_WAD_22;
import org.watto.io.FileManipulator;

/**
**********************************************************************************************

**********************************************************************************************
**/
public class Viewer_WAD_22_TX extends ViewerPlugin {

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public Viewer_WAD_22_TX() {
    super("WAD_22_TX", "WAD_22_TX Image");
    setExtensions("tx");

    setGames("God of War: Ascension");
    setPlatforms("PS3");
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
      if (plugin instanceof Plugin_WAD_22 || plugin instanceof Plugin_PATCH) {
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

      if ((fm.readNullString(56) + ".TX").equals(fm.getFile().getName())) {
        rating += 25;
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

      fm.skip(92);

      // 4 - Image Height
      int height = fm.readInt();
      FieldValidator.checkHeight(height);

      // 4 - Image Width
      int width = fm.readInt();
      FieldValidator.checkWidth(width);

      fm.skip(64);

      // 4 - Image Format
      String imageFormat = fm.readString(4);

      // 4 - Color Bit Count
      int rgbBitCount = fm.readInt();

      // 4 - Red Bit Mask
      int rBitMask = fm.readInt();

      // 4 - Green Bit Mask
      int gBitMask = fm.readInt();

      // 4 - Blue Bit Mask
      int bBitMask = fm.readInt();

      // 4 - Alpha Bit Mask
      int rgbAlphaBitMask = fm.readInt();

      fm.skip(20);

      // X - Pixels
      ImageResource imageResource = null;
      if (imageFormat.equals("DXT1")) {
        // work out the actual dimensions in this file
        while (width * height / 2 > arcSize) {
          width /= 2;
          height /= 2;
        }
        // read the image
        imageResource = ImageFormatReader.readDXT1(fm, width, height);
      }
      else if (imageFormat.equals("DXT3")) {
        // work out the actual dimensions in this file
        while (width * height > arcSize) {
          width /= 2;
          height /= 2;
        }
        // read the image
        imageResource = ImageFormatReader.readDXT3(fm, width, height);
      }
      else if (imageFormat.equals("DXT5")) {
        // work out the actual dimensions in this file
        while (width * height > arcSize) {
          width /= 2;
          height /= 2;
        }
        // read the image
        imageResource = ImageFormatReader.readDXT5(fm, width, height);
      }
      else if (rgbBitCount == 8) {
        // Indexed pixel data (eg grayscale paletted)

        // work out the actual dimensions in this file
        while (width * height > arcSize) {
          width /= 2;
          height /= 2;
        }
        // read the image
        imageResource = ImageFormatReader.read8BitPaletted(fm, width, height);
      }
      else if (rgbBitCount == 32) {
        // uncompressed RGBA data

        // work out the actual dimensions in this file
        while (width * height * 4 > arcSize) {
          width /= 2;
          height /= 2;
        }
        // read the image
        imageResource = ImageFormatReader.readBGRA(fm, width, height);
      }
      else if (rgbBitCount == 16 && rBitMask == 31744 && gBitMask == 992 && bBitMask == 31 && rgbAlphaBitMask == 32768) {
        // ARGB 1555 format

        // work out the actual dimensions in this file
        while (width * height * 2 > arcSize) {
          width /= 2;
          height /= 2;
        }
        // read the image
        imageResource = ImageFormatReader.readARGB1555(fm, width, height);
      }
      else if (rgbBitCount == 16 && rBitMask != 0 && gBitMask != 0 && bBitMask != 0 && rgbAlphaBitMask != 0) {
        // A4R4G4B4 format

        // work out the actual dimensions in this file
        while (width * height * 2 > arcSize) {
          width /= 2;
          height /= 2;
        }
        // read the image
        imageResource = ImageFormatReader.readARGB4444(fm, width, height);
      }
      else if (rgbBitCount == 16 && rBitMask == 63488 && gBitMask == 2016 && bBitMask == 31 && rgbAlphaBitMask == 0) {
        // RGB565 format

        // work out the actual dimensions in this file
        while (width * height * 2 > arcSize) {
          width /= 2;
          height /= 2;
        }
        // read the image
        imageResource = ImageFormatReader.readRGB565(fm, width, height);
      }
      else if (rgbBitCount == 16 && rBitMask == 255 && rgbAlphaBitMask == 65280) {
        // A8L8 format

        // work out the actual dimensions in this file
        while (width * height * 2 > arcSize) {
          width /= 2;
          height /= 2;
        }
        // read the image
        imageResource = ImageFormatReader.readA8L8(fm, width, height);
      }
      else if (rgbBitCount == 16 && rBitMask == 65535) {
        // A8L8 format

        // work out the actual dimensions in this file
        while (width * height * 2 > arcSize) {
          width /= 2;
          height /= 2;
        }
        // read the image
        imageResource = ImageFormatReader.readA8L8(fm, width, height);
      }
      else {
        ErrorLogger.log("[Viewer_WAD_22_TX] Unknown Image Format: " + imageFormat);
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