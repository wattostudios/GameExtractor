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
import org.watto.ge.plugin.archive.Plugin_BIN_30;
import org.watto.ge.plugin.archive.Plugin_DFS_DFS;
import org.watto.ge.plugin.archive.Plugin_FOG;
import org.watto.ge.plugin.archive.Plugin_HOG;
import org.watto.ge.plugin.archive.Plugin_TIM;
import org.watto.ge.plugin.archive.Plugin_VFS_VFS2;
import org.watto.ge.plugin.archive.Plugin_ZAL;
import org.watto.io.FileManipulator;
import org.watto.io.buffer.ByteBuffer;
import org.watto.io.converter.ByteConverter;

/**
**********************************************************************************************

**********************************************************************************************
**/
public class Viewer_TIM extends ViewerPlugin {

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public Viewer_TIM() {
    super("TIM", "PlayStation TIM Image");
    setExtensions("tim");

    setGames("PlayStation",
        "Bust A Groove",
        "Space Invaders",
        "Syphon Filter");
    setPlatforms("PSX");
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

      ArchivePlugin plugin = Archive.getReadPlugin();
      if (plugin instanceof Plugin_ZAL || plugin instanceof Plugin_BIN_30 || plugin instanceof Plugin_TIM || plugin instanceof Plugin_DFS_DFS || plugin instanceof Plugin_HOG || plugin instanceof Plugin_FOG || plugin instanceof Plugin_VFS_VFS2) {
        rating += 50;
      }
      else if (!(plugin instanceof AllFilesPlugin)) {
        //return 0;
      }

      if (FieldValidator.checkExtension(fm, extensions)) {
        rating += 25;
      }
      else {
        return 0;
      }

      // 4 - Header
      if (fm.readInt() == 16) {
        rating += 20;
      }
      else {
        rating = 0;
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

      ImageResource firstImage = null;
      ImageResource previousImage = null;

      while (fm.getOffset() < arcSize) {
        ImageResource image = readSingleImage(fm, arcSize);

        if (firstImage == null) {
          firstImage = image;
          previousImage = image;
        }
        else {
          previousImage.setNextFrame(image);
          image.setPreviousFrame(previousImage);
          previousImage = image;
        }
      }

      if (firstImage == null) {
        return null;
      }

      if (firstImage != previousImage) {
        // so that the first and last images point to each other
        previousImage.setNextFrame(firstImage);
        firstImage.setPreviousFrame(previousImage);

        // make it multi-frame but not automatic
        firstImage.setManualFrameTransition(true);
      }

      fm.close();

      return firstImage;
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
  public ImageResource readSingleImage(FileManipulator fm, long arcSize) {
    try {
      // 1 - Header (16)
      // 1 - Version (0)
      // 2 - null
      fm.skip(4);

      // 1 - Flags (first 2 bits = color depth --> 0=4bpp, 1=8bpp, 2=16bpp, 3=24bpp)
      int flags = ByteConverter.unsign(fm.readByte());

      // 3 - null
      fm.skip(3);

      int bpp = (flags & 3);
      if (bpp == 3) {
        bpp = 24;
      }
      else if (bpp == 2) {
        bpp = 16;
      }
      else if (bpp == 1) {
        bpp = 8;
      }
      else if (bpp == 0) {
        bpp = 4;
      }

      boolean paletted = (((flags & 8) >> 3) == 1);

      int[] palette = null;
      int[][] palettes = null;
      int numPalettes = 1;

      if (paletted) {
        // 4 - Palette Data Length
        int paletteLength = fm.readInt() - 12;

        // 2 - Palette X
        // 2 - Palette Y
        fm.skip(4);

        // 2 - Number of Colors
        int numColors = fm.readShort();

        // 2 - Number of Palettes
        numPalettes = fm.readShort();

        PaletteManager.clear();

        if (bpp == 4) {
          // use the numColors in the fields above
        }
        else {
          numColors = paletteLength / 2;
          numPalettes = 1;
        }
        FieldValidator.checkNumColors(numColors);

        palettes = new int[numPalettes][0];

        // numColors*16bit - colors (RGB555)

        for (int p = 0; p < numPalettes; p++) {
          palette = new int[numColors];

          for (int i = 0; i < numColors; i++) {
            int byte1 = ByteConverter.unsign(fm.readByte());
            int byte2 = ByteConverter.unsign(fm.readByte());

            // GGGBBBBB ARRRRRGG
            int b = ((byte2 >> 2) & 31) * 8;
            int g = (((byte2 & 3) << 3) | ((byte1 >> 5) & 7)) * 8;
            int r = (byte1 & 31) * 8;

            int a = (byte2 >> 7);
            if (r == 0 && b == 0 && g == 0) {
              a *= 255;
            }
            else {
              if (a == 1) {
                //a = 0;
                a = 255;
              }
              else {
                a = 255;
                //a = 0;
              }
            }

            /*
            // RRRRRGGG GGBBBBBA
            int r = (byte1 >> 3) * 8;
            int g = (((byte1 & 7) << 2) | (byte2 >> 6)) * 8;
            int b = ((byte2 >> 1) & 31) * 8;
            int a = (byte2 & 1) * 255;
            */

            // OUTPUT = ARGB
            int color = ((r << 16) | (g << 8) | b | (a << 24));
            palette[i] = color;
          }

          palettes[p] = palette;

          PaletteManager.addPalette(new Palette(palette));
        }

        palette = palettes[0];

        //palette = ImageFormatReader.readRGBA5551(fm, numColors, 1).getPixels();
      }

      // 4 - Image Data Length
      int imageDataLength = fm.readInt();
      FieldValidator.checkLength(imageDataLength, arcSize);

      // 2 - Image X
      // 2 - Image Y
      fm.skip(4);

      // 2 - Image Width
      short width = fm.readShort();
      FieldValidator.checkWidth(width);

      // 2 - Image Height
      short height = fm.readShort();
      FieldValidator.checkHeight(height);

      // X - Pixels
      ImageResource imageResource = null;
      if (paletted) {

        imageDataLength -= 12;
        /*
        if (numPalettes == 1 && bpp == 4 && (imageDataLength == width * height * 2)) {
          // force it
          bpp = 8;
        }
        */

        if (bpp == 4) {
          width *= 4;

          if (numPalettes == 1) {
            imageResource = ImageFormatReader.read4BitPaletted(fm, width, height, palette);
          }
          else {
            imageResource = ImageFormatReader.read4BitPaletted(fm, width, height, true);
          }
        }
        else if (bpp == 8) {

          width *= 2;

          if (numPalettes == 1) {
            imageResource = ImageFormatReader.read8BitPaletted(fm, width, height, palette);
          }
          else {
            imageResource = ImageFormatReader.read8BitPaletted(fm, width, height, true);
          }
        }
      }
      else {
        if (bpp == 16) {

          /*
          int numPixels = width * height;
          int[] pixels = new int[numPixels];
          
          for (int i = 0; i < numPixels; i++) {
            int byte1 = ByteConverter.unsign(fm.readByte());
            int byte2 = ByteConverter.unsign(fm.readByte());
          
            int r = ((byte2 >> 2) & 31) * 8;
            int g = (((byte2 & 3) << 3) | ((byte1 >> 5) & 7)) * 8;
            int b = (byte1 & 31) * 8;
          
            int a = (byte2 >> 7);
            if (r == 0 && b == 0 && g == 0) {
              a *= 255;
            }
            else {
              if (a == 1) {
                a = 0;
              }
              else {
                a = 255;
              }
            }
          
            // OUTPUT = ARGB
            int color = ((r << 16) | (g << 8) | b | (a << 24));
            pixels[i] = color;
          }
          
          imageResource = new ImageResource(pixels, width, height);
          */
          imageResource = ImageFormatReader.readRGBA5551(fm, width, height);
          imageResource = ImageFormatReader.removeAlpha(imageResource);

        }
        else if (bpp == 24) {
          if (width % 2 == 1) {
            // there is a padding byte at the end of each row, want to skip it
            int numPixels = width * height * 3;
            int writePos = 0;

            byte[] pixelBytes = new byte[numPixels];
            for (int h = 0; h < height; h++) {
              for (int w = 0; w < width; w++) {
                pixelBytes[writePos] = fm.readByte();
                pixelBytes[writePos + 1] = fm.readByte();
                pixelBytes[writePos + 2] = fm.readByte();
                writePos += 3;
              }
              fm.skip(1); // 1-byte padding
            }

            fm.close();
            fm = new FileManipulator(new ByteBuffer(pixelBytes));
          }

          //imageResource = ImageFormatReader.readBGRA(fm, width, height);
          imageResource = ImageFormatReader.readBGR(fm, width, height);
        }
      }

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