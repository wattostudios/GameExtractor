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
import org.watto.TemporarySettings;
import org.watto.component.PreviewPanel;
import org.watto.component.PreviewPanel_Image;
import org.watto.datatype.ImageResource;
import org.watto.ge.helper.FieldValidator;
import org.watto.ge.helper.ImageFormatReader;
import org.watto.ge.plugin.ViewerPlugin;
import org.watto.io.FileManipulator;

/**
**********************************************************************************************
PS2 and PSP "TM2" TIM2 Image Viewer.
Based on code from "Rainbow" --> https://github.com/marco-calautti/Rainbow/
**********************************************************************************************
**/
public class Viewer_TM2_TIM2_4 extends ViewerPlugin {

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public Viewer_TM2_TIM2_4() {
    super("TM2_TIM2_4", "Playstation TIM2 Images (version 4) [TM2_TIM2_4]");
    setExtensions("tm2");

    setGames("Rule of Rose",
        "NHL 2K3");
    setPlatforms("PS2");
    setStandardFileFormat(true);
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

      if (FieldValidator.checkExtension(fm, extensions)) {
        rating += 25;
      }

      // 4 - Header
      if (fm.readString(4).equals("TIM2")) {
        rating += 50;
      }

      // 2 - Version (4)
      short version = fm.readShort();
      if (version == 4 || version == 260) { // 260 = 4.1
        rating += 5;
      }

      // 2 - Number of Images
      if (FieldValidator.checkNumFiles(fm.readShort())) {
        rating += 5;
      }

      if (version == 260) {
        rating += 10;
        return rating;
      }

      fm.skip(8);

      // 4 - Image Length [+16]
      if (FieldValidator.checkEquals(fm.readInt() + 16, fm.getLength())) {
        rating += 5;
      }

      fm.skip(16);

      // 2 - Image Width (+1 so it allows zero-sized images)
      if (FieldValidator.checkWidth(fm.readShort() + 1)) {
        rating += 5;
      }

      // 2 - Image Height (+1 so it allows zero-sized images)
      if (FieldValidator.checkHeight(fm.readShort() + 1)) {
        rating += 4; // 4, not 5, so we don't equals 25 if all pass but the Header fails
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

  @SuppressWarnings("unused")
  @Override
  public ImageResource readThumbnail(FileManipulator fm) {
    try {

      long arcSize = fm.getLength();

      // 4 - Header (TIM2)
      // 1 - Version (4)
      fm.skip(5);

      // 1 - Padding
      int paddingFlag = fm.readByte();

      // 2 - Number of Images
      fm.skip(2);

      if (paddingFlag == 1) {
        fm.skip(120);
      }
      else {
        fm.skip(8);
      }

      // 4 - Total Image Length [+16]
      fm.skip(4);

      // 4 - Palette Length
      int paletteLength = fm.readInt();
      FieldValidator.checkLength(paletteLength, arcSize);

      // 4 - Image Data Length
      int imageDataLength = fm.readInt();
      FieldValidator.checkLength(imageDataLength, arcSize);

      // 2 - Header Length [+16]
      short headerLength = (short) (fm.readShort() + 16);
      FieldValidator.checkLength(headerLength, arcSize);

      // 2 - Color Entries
      short colorEntries = fm.readShort();

      // 1 - Image Format (0 = 8bpp paletted?)
      int imageFormat = fm.readByte();

      // 1 - Mipmap Count
      int mipmapCount = fm.readByte();

      // 1 - CLUT Format
      int clutFormat = fm.readByte();

      // 1 - Bits Per Pixel (1=16bbp, 2=24bpp, 3=32bbp, 4=4bbp, 5=8bpp)
      int bitsPerPixel = fm.readByte();
      if (bitsPerPixel == 1) {
        bitsPerPixel = 16;
      }
      else if (bitsPerPixel == 2) {
        bitsPerPixel = 24;
      }
      else if (bitsPerPixel == 3) {
        bitsPerPixel = 32;
      }
      else if (bitsPerPixel == 4) {
        bitsPerPixel = 4;
      }
      else if (bitsPerPixel == 5) {
        bitsPerPixel = 8;
      }

      // 2 - Image Width
      short width = fm.readShort();
      FieldValidator.checkWidth(width);

      // 2 - Image Height
      short height = fm.readShort();
      FieldValidator.checkHeight(height);

      boolean linearPalette = ((clutFormat & 0x80) != 0);
      clutFormat &= 0x7F;
      int colorSize = (bitsPerPixel > 8 ? bitsPerPixel / 8 : (clutFormat & 0x07) + 1);

      long imageDataOffset = headerLength;
      if (paddingFlag == 1) {
        imageDataOffset += 112;
      }

      int[] palette = new int[0];

      if (paletteLength > 0) {
        // ...
        // skip over the image data, so we can read the color palettes

        fm.seek(imageDataOffset + imageDataLength);

        int numberOfPalettes = paletteLength / (colorEntries * colorSize);
        int singlePaletteSize = paletteLength / numberOfPalettes;

        // read only the first color palette

        int numColors = 0;
        if (colorSize == 2) {
          // 16BITLE_ABGR_5551 Format
          numColors = singlePaletteSize / 2;
          palette = ImageFormatReader.readRGBA5551(fm, numColors, 1).getPixels();
        }
        else if (colorSize == 3) {
          // 24BIT_RGB Format
          numColors = singlePaletteSize / 3;
          palette = ImageFormatReader.readRGB(fm, numColors, 1).getPixels();
        }
        else if (colorSize == 4) {
          // 32BIT_RGBA Format
          numColors = singlePaletteSize / 4;
          palette = ImageFormatReader.readRGBA(fm, numColors, 1).getPixels();
        }

        boolean forceStriped = false;
        if (TemporarySettings.has("ForcePaletteStriping")) {
          forceStriped = TemporarySettings.getBoolean("ForcePaletteStriping");
        }

        // Now, if the palette isn't linear, perform the decoding...
        if (forceStriped || (!linearPalette && (bitsPerPixel != 8 && bitsPerPixel != 4))) {

          int currentPaletteLength = palette.length;
          int[] oldPalette = palette;
          palette = new int[currentPaletteLength];

          int parts = currentPaletteLength / 32;
          int stripes = 2;
          int colors = 8;
          int blocks = 2;

          int i = 0;
          for (int part = 0; part < parts; part++) {
            for (int block = 0; block < blocks; block++) {
              for (int stripe = 0; stripe < stripes; stripe++) {

                for (int color = 0; color < colors; color++) {
                  palette[i++] = oldPalette[part * colors * stripes * blocks + block * colors + stripe * stripes * colors + color];
                }
              }
            }
          }
        }

      }

      // ...
      // Now go back to the image data, and read it
      fm.seek(imageDataOffset);

      ImageResource imageResource = null;
      if (bitsPerPixel == 8) {
        // Indexed
        imageResource = ImageFormatReader.read8BitPaletted(fm, width, height, palette);
        imageResource.addProperty("ImageFormat", "8BitPaletted");
        imageResource = ImageFormatReader.doubleAlpha(imageResource);
      }
      else if (bitsPerPixel == 4) {
        // Indexed
        imageResource = ImageFormatReader.read4BitPaletted(fm, width, height, palette);
        imageResource.addProperty("ImageFormat", "4BitPaletted");
        imageResource = ImageFormatReader.doubleAlpha(imageResource);
      }
      else if (colorSize == 2) {
        // 16BITLE_ABGR_5551 Format
        imageResource = ImageFormatReader.readRGBA5551(fm, width, height);
        imageResource.addProperty("ImageFormat", "RGBA5551");
      }
      else if (colorSize == 3) {
        // 24BIT_RGB Format
        imageResource = ImageFormatReader.readRGB(fm, width, height);
        imageResource.addProperty("ImageFormat", "RGB");
      }
      else if (colorSize == 4) {
        // 32BIT_RGBA Format
        imageResource = ImageFormatReader.readRGBA(fm, width, height);
        imageResource.addProperty("ImageFormat", "RGBA");
      }
      else {
        // Unknown format
        ErrorLogger.log("Viewer_TM2_TIM2: Unknown Format");
        fm.close();
        return null;
      }

      if (colorSize == 2 && TemporarySettings.has("SwapRedBlue")) {
        if (TemporarySettings.getBoolean("SwapRedBlue")) {
          imageResource = ImageFormatReader.swapRedAndBlue(imageResource);
        }
      }

      fm.close();

      //ColorConverter.convertToPaletted(resource);

      imageResource.addProperty("MipmapCount", "" + mipmapCount);

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