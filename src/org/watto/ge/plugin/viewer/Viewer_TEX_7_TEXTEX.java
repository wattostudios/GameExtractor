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
import org.watto.ge.plugin.archive.Plugin_TEX_7;
import org.watto.io.FileManipulator;
import org.watto.io.buffer.ByteBuffer;
import org.watto.io.converter.ByteConverter;
import org.watto.io.converter.ShortConverter;

/**
**********************************************************************************************

**********************************************************************************************
**/
public class Viewer_TEX_7_TEXTEX extends ViewerPlugin {

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public Viewer_TEX_7_TEXTEX() {
    super("TEX_7_TEXTEX", "TEX_7_TEXTEX Image");
    setExtensions("tex_tex");

    setGames("Shrek Smash n' Crash Racing");
    setPlatforms("GameCube");
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
      if (plugin instanceof Plugin_TEX_7) {
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

      fm.skip(92);

      // 2 - Image Width
      if (FieldValidator.checkWidth(ShortConverter.changeFormat(fm.readShort()))) {
        rating += 5;
      }

      // 2 - Image Height
      if (FieldValidator.checkHeight(ShortConverter.changeFormat(fm.readShort()))) {
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

      // 4 - Unknown (6) (BIG)
      // 4 - Unknown (4354) (BIG)
      // 4 - null

      // 4 - Unknown (1) (BIG)
      // 4 - Unknown (1) (BIG)
      // 4 - null

      // 32 - Filename (null terminated, filled with nulls)
      // 32 - Junk
      // 2 - null
      fm.skip(90);

      // 1 - Image Format (0=CMPR, 66=4bit Paletted)
      int imageFormat = ByteConverter.unsign(fm.readByte());

      // 1 - Unknown (4)
      fm.skip(1);

      // 2 - Image Width (BIG)
      short width = ShortConverter.changeFormat(fm.readShort());
      FieldValidator.checkWidth(width);

      // 2 - Image Height (BIG)
      short height = ShortConverter.changeFormat(fm.readShort());
      FieldValidator.checkHeight(height);

      // 4 - Unknown
      // 4 - Unknown (0/1) (BIG)
      fm.skip(8);

      int[] palette = null;
      if (imageFormat == 66 || imageFormat == 194) {
        // 4bit paletted (RGB565 Big Endian)

        palette = new int[16];

        for (int i = 0; i < 16; i++) {
          int pixel = ShortConverter.unsign(ShortConverter.changeFormat(fm.readShort()));

          int rPixel = ((pixel >> 11) & 31) * 8;
          int gPixel = ((pixel >> 5) & 63) * 4;
          int bPixel = (pixel & 31) * 8;
          int aPixel = 255;

          // OUTPUT = ARGB
          palette[i] = ((rPixel << 16) | (gPixel << 8) | bPixel | (aPixel << 24));
        }
      }
      else if (imageFormat == 193) {
        // 4bit paletted (BGRA5551 Big Endian)

        palette = new int[16];

        for (int i = 0; i < 16; i++) {
          int pixel = ShortConverter.unsign(ShortConverter.changeFormat(fm.readShort())); // ARRRRRGGGGGBBBBB

          int r = ((pixel >> 10) & 31) * 8;
          int g = ((pixel >> 5) & 31) * 8;
          int b = (pixel & 31) * 8;
          int a = (pixel >> 15) * 255;

          // OUTPUT = ARGB
          palette[i] = ((r << 16) | (g << 8) | b | (a << 24));
        }
      }
      else if (imageFormat == 195 || imageFormat == 67 || imageFormat == 65) {
        // 4bit paletted (N64_RGB5A3 Big Endian)
        palette = ImageFormatReader.readPaletteRGB5A3Wii(fm, 16);
      }
      else if (imageFormat == 162 || imageFormat == 34) {
        // 8bit paletted (RGB565 Big Endian)
        palette = new int[256];

        for (int i = 0; i < 256; i++) {
          int pixel = ShortConverter.unsign(ShortConverter.changeFormat(fm.readShort()));

          int rPixel = ((pixel >> 11) & 31) * 8;
          int gPixel = ((pixel >> 5) & 63) * 4;
          int bPixel = (pixel & 31) * 8;
          int aPixel = 255;

          // OUTPUT = ARGB
          palette[i] = ((rPixel << 16) | (gPixel << 8) | bPixel | (aPixel << 24));
        }
      }
      else if (imageFormat == 35 || imageFormat == 33) {
        // 8bit paletted (N64_RGB5A3 Big Endian)
        palette = ImageFormatReader.readPaletteRGB5A3Wii(fm, 256);
      }

      // 4 - Image Data Length (data only)
      fm.skip(4);

      // X - Pixels
      ImageResource imageResource = null;
      if (imageFormat == 0) {
        // CMPR
        imageResource = ImageFormatReader.readCMPR(fm, width, height);
      }
      else if (imageFormat == 65 || imageFormat == 66 || imageFormat == 67 || imageFormat == 193 || imageFormat == 194 || imageFormat == 195) {
        // BC Swizzle 4-bit
        int dataLength = width * height / 2; // 4-bits can store 2 pixels on each byte
        byte[] rawBytes = fm.readBytes(dataLength);
        byte[] bytes = ImageSwizzler.unswizzleBC4Bit(rawBytes, width, height);

        // in this game, the 4-bit reading needs to occur in the opposite order (BIG Endian), so need to swap the bits in each byte
        int numBytes = bytes.length;
        for (int b = 0; b < numBytes; b++) {
          int currentByte = ByteConverter.unsign(bytes[b]);
          bytes[b] = (byte) ((currentByte & 15) << 4 | ((currentByte >> 4) & 15));
        }

        fm.close();
        fm = new FileManipulator(new ByteBuffer(bytes));

        imageResource = ImageFormatReader.read4BitPaletted(fm, width, height, palette);
      }
      else if (imageFormat == 162 || imageFormat == 35 || imageFormat == 34 || imageFormat == 33) {
        // GameCube Swizzle 8-bit
        int dataLength = width * height;
        byte[] rawBytes = fm.readBytes(dataLength);
        byte[] bytes = ImageSwizzler.unswizzleGameCube8Bit(rawBytes, width, height);

        fm.close();
        fm = new FileManipulator(new ByteBuffer(bytes));

        imageResource = ImageFormatReader.read8BitPaletted(fm, width, height, palette);
      }
      else {
        ErrorLogger.log("[Viewer_TEX_7_TEXTEX] Unknown Image Format: " + imageFormat);
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