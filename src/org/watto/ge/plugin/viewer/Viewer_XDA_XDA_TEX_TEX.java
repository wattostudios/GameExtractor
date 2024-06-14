/*
 * Application:  Game Extractor
 * Author:       wattostudios
 * Website:      http://www.watto.org
 * Copyright:    Copyright (c) 2002-2024 wattostudios
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
import org.watto.datatype.Palette;
import org.watto.ge.helper.FieldValidator;
import org.watto.ge.helper.ImageFormatReader;
import org.watto.ge.helper.PaletteManager;
import org.watto.ge.plugin.AllFilesPlugin;
import org.watto.ge.plugin.ArchivePlugin;
import org.watto.ge.plugin.ViewerPlugin;
import org.watto.ge.plugin.archive.Plugin_PAC_2;
import org.watto.ge.plugin.archive.Plugin_XDA_XDA;
import org.watto.io.FileManipulator;
import org.watto.io.buffer.ByteBuffer;

/**
**********************************************************************************************

**********************************************************************************************
**/
public class Viewer_XDA_XDA_TEX_TEX extends ViewerPlugin {

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public Viewer_XDA_XDA_TEX_TEX() {
    super("XDA_XDA_TEX_TEX", "XDA TEX Image");
    setExtensions("tex");

    setGames("Summon Night Granthese");
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
      if (plugin instanceof Plugin_XDA_XDA || plugin instanceof Plugin_PAC_2) {
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

      // 4 - Header
      if (fm.readString(4).equals("tex" + (char) 0)) {
        rating += 50;
      }
      else {
        rating = 0;
      }

      if (fm.readInt() == 516) {
        rating += 5;
      }

      fm.skip(16);

      if (fm.readInt() == 64) {
        rating += 5;
      }

      if (FieldValidator.checkOffset(fm.readInt(), fm.getLength())) {
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

      // 4 - Header ("tex" + null)
      // 4 - Unknown (516)
      // 2 - Unknown (2)
      // 2 - Unknown (4880)
      // 8 - null
      // 4 - Unknown (1)
      fm.skip(24);

      // 4 - Block 1 Offset (64)
      int block1Offset = fm.readInt();
      FieldValidator.checkOffset(block1Offset, arcSize);

      // 4 - Block 2 Offset (224)
      int block2Offset = fm.readInt();
      FieldValidator.checkOffset(block2Offset, arcSize);

      // 32 - null
      fm.seek(block1Offset);

      // 4 - Unknown (1)
      // 12 - null
      fm.skip(16);

      // 2 - Image Width
      int width = fm.readShort();
      FieldValidator.checkWidth(width);

      // 2 - Image Height
      int height = fm.readShort();
      FieldValidator.checkHeight(height);

      if (block2Offset == 0) {
        // not paletted - RGBA image instead

        // 2 - Image Width
        // 2 - Image Height
        // 4 - Unknown (288)
        // 4 - Unknown (2048)
        // 4 - Unknown (2048)
        // 12 - null
        // 4 - Image Data Length
        // 4 - Unknown
        // 4 - Unknown (14)
        // 8 - null
        // 4 - Unknown
        // 4 - Unknown (80)
        // 12 - null
        // 4 - Unknown (81)
        // 4 - null
        // 4 - Image Width
        // 4 - Image Height
        // 4 - Unknown (82)
        // 12 - null
        // 4 - Unknown (83)
        // 4 - null
        // 4 - Unknown
        // 4 - Unknown
        // 4 - Unknown (14)
        // 4 - null
        // 4 - Unknown
        // 4 - Unknown (5)
        // 4 - Unknown (6)
        // 4 - null
        fm.skip(140);

        ImageResource imageResource = ImageFormatReader.readRGBA(fm, width, height);
        imageResource = ImageFormatReader.doubleAlpha(imageResource);

        fm.close();

        return imageResource;
      }

      // if we're here, it's paletted
      fm.seek(block2Offset);

      // 4 - Number of Palettes
      int numPalettes = fm.readInt();

      // 12 - null
      fm.skip(12);

      // 2 - Unknown (16=8bit colors, 8=4bit colors)
      int colorType = fm.readShort();

      // 2 - Unknown (16)
      // 4 - Unknown
      // 4 - Unknown
      // 4 - Unknown (64)
      // 4 - Unknown (64)
      // 12 - null
      // 4 - Image Data Length
      // 4 - Unknown
      // 4 - Unknown (14)
      // 8 - null
      // 4 - Unknown
      // 4 - Unknown (80)
      // 12 - null
      // 4 - Unknown (81)
      // 4 - null
      // 4 - Unknown (16)
      // 4 - Unknown (16)
      // 4 - Unknown (82)
      // 12 - null
      // 4 - Unknown (83)
      // 4 - null
      fm.skip(110);

      // X - other palettes
      fm.skip((numPalettes - 1) * 112);

      ImageResource imageResource = null;

      if (colorType == 8) {
        // 16 colors / 4bit colors

        // X - Pixel Data
        int numPixels = width * height;
        byte[] pixelBytes = fm.readBytes(numPixels / 2); // 4bpp

        if (numPalettes == 1) {
          // X - Color Palette
          int[] palette = ImageFormatReader.readPaletteRGBA(fm, 16);

          fm.close();
          fm = new FileManipulator(new ByteBuffer(pixelBytes));

          imageResource = ImageFormatReader.read4BitPaletted(fm, width, height, palette);
          imageResource = ImageFormatReader.doubleAlpha(imageResource);
        }
        else {
          // read each palette
          PaletteManager.clear();

          for (int p = 0; p < numPalettes; p++) {
            int[] palette = ImageFormatReader.readPaletteRGBA(fm, 16);

            // double the alpha
            for (int i = 0; i < 16; i++) {
              int paletteIndex = palette[i];

              int alphaValue = paletteIndex >> 24;
              if (alphaValue == -128) {
                alphaValue = 255;
              }
              else {
                alphaValue *= 2;
              }

              palette[i] = (paletteIndex & 0xFFFFFF) | (alphaValue << 24);
            }

            PaletteManager.addPalette(new Palette(palette));
          }

          // now read the image
          fm.close();
          fm = new FileManipulator(new ByteBuffer(pixelBytes));

          imageResource = ImageFormatReader.read4BitPaletted(fm, width, height, true);
        }
      }
      else {
        // 256 colors / 8bit colors

        // X - Pixel Data
        int numPixels = width * height;
        byte[] pixelBytes = fm.readBytes(numPixels);

        if (numPalettes == 1) {
          // X - Color Palette
          int[] palette = ImageFormatReader.readPaletteRGBA(fm, 256);
          palette = ImageFormatReader.stripePalettePS2(palette);

          fm.close();
          fm = new FileManipulator(new ByteBuffer(pixelBytes));

          imageResource = ImageFormatReader.read8BitPaletted(fm, width, height, palette);
          imageResource = ImageFormatReader.doubleAlpha(imageResource);

        }
        else {
          // read each palette
          PaletteManager.clear();

          for (int p = 0; p < numPalettes; p++) {
            int[] palette = ImageFormatReader.readPaletteRGBA(fm, 256);
            palette = ImageFormatReader.stripePalettePS2(palette);

            // double the alpha
            for (int i = 0; i < 256; i++) {
              int paletteIndex = palette[i];

              int alphaValue = paletteIndex >> 24;
              if (alphaValue == -128) {
                alphaValue = 255;
              }
              else {
                alphaValue *= 2;
              }

              palette[i] = (paletteIndex & 0xFFFFFF) | (alphaValue << 24);
            }

            PaletteManager.addPalette(new Palette(palette));
          }

          // now read the image
          fm.close();
          fm = new FileManipulator(new ByteBuffer(pixelBytes));

          imageResource = ImageFormatReader.read8BitPaletted(fm, width, height, true);
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