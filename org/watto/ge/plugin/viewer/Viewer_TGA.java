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

import java.awt.Graphics;
import java.awt.image.BufferedImage;
import java.io.File;
import org.watto.component.PreviewPanel;
import org.watto.component.PreviewPanel_Image;
import org.watto.component.WSPluginException;
import org.watto.datatype.ImageResource;
import org.watto.ge.helper.FieldValidator;
import org.watto.ge.plugin.ViewerPlugin;
import org.watto.io.FileManipulator;
import org.watto.io.converter.ByteConverter;

/**
**********************************************************************************************

**********************************************************************************************
**/
public class Viewer_TGA extends ViewerPlugin {

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public Viewer_TGA() {
    super("TGA", "TGA Image");
    setExtensions("tga");
    setStandardFileFormat(true);
  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  @Override
  public boolean canWrite(PreviewPanel panel) {
    if (panel instanceof PreviewPanel_Image) {
      return true;
    }
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

      // 1 - Header 2 Size
      if (fm.readByte() == 0) {
        rating += 5;
      }

      // 1 - Palette Flag
      fm.skip(1);

      // 1 - Image Type
      int imageType = ByteConverter.unsign(fm.readByte());
      if (imageType >= 0 && imageType <= 11) {
        rating += 5;
      }

      // 2 - First Color Map
      // 2 - Number Of Colors
      // 1 - Number Of Bits per Color
      // 2 - X Position
      // 2 - Y Position
      fm.skip(9);

      // 2 - Image Width
      if (FieldValidator.checkWidth(fm.readShort())) {
        rating += 5;
      }

      // 2 - Image Height
      if (FieldValidator.checkHeight(fm.readShort())) {
        rating += 5;
      }

      // 1 - Color Depth
      int colorDepth = ByteConverter.unsign(fm.readByte());
      if (colorDepth == 8 || colorDepth == 16 || colorDepth == 24 || colorDepth == 32) {
        rating += 4;
      }

      // 1 - Descriptor Flag

      return rating;

    }
    catch (Throwable t) {
      return 0;
    }
  }

  /**
  **********************************************************************************************

  **********************************************************************************************
  **/
  @Override
  public PreviewPanel read(File path) {
    return read(new FileManipulator(path, false));
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

  **********************************************************************************************
  **/
  public int[] readPaletted(FileManipulator fm, int[] pixels, int width, int height, int numColors, int bitsPerColor) {
    try {

      // First read the color palette
      int[] palette = new int[numColors];

      if (bitsPerColor == 32) {
        for (int i = 0; i < numColors; i++) {
          // 1 - Blue
          // 1 - Green
          // 1 - Red
          // 1 - Alpha
          int b = ByteConverter.unsign(fm.readByte());
          int g = ByteConverter.unsign(fm.readByte());
          int r = ByteConverter.unsign(fm.readByte());
          int a = ByteConverter.unsign(fm.readByte());

          palette[i] = ((a << 24) | (r << 16) | (g << 8) | (b));
        }
      }

      else if (bitsPerColor == 24) {
        for (int i = 0; i < numColors; i++) {
          // 1 - Blue
          // 1 - Green
          // 1 - Red
          int b = ByteConverter.unsign(fm.readByte());
          int g = ByteConverter.unsign(fm.readByte());
          int r = ByteConverter.unsign(fm.readByte());

          palette[i] = ((255 << 24) | (r << 16) | (g << 8) | (b));
        }
      }

      else if (bitsPerColor == 16) {
        for (int i = 0; i < numColors; i++) {
          // 1 bit  - Alpha
          // 5 bits - Blue
          // 5 bits - Green
          // 5 bits - Red
          int byte1 = ByteConverter.unsign(fm.readByte());
          int byte2 = ByteConverter.unsign(fm.readByte());

          int b = (byte1 & 31) * 8;
          int g = (((byte1 & 224) >> 5) | ((byte2 & 3) << 3)) * 8;
          int r = ((byte2 & 124) >> 2) * 8;

          palette[i] = ((255 << 24) | (r << 16) | (g << 8) | (b));
        }
      }

      // Now read the pixels
      for (int h = height - 1; h >= 0; h--) {
        for (int w = 0; w < width; w++) {
          // 1 - Palette Index
          pixels[h * width + w] = palette[ByteConverter.unsign(fm.readByte())];
        }
      }

      return pixels;

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
  public int[] readRGB(FileManipulator fm, int[] pixels, int width, int height, int colorDepth) {
    try {

      if (colorDepth == 32) {
        for (int h = height - 1; h >= 0; h--) {
          for (int w = 0; w < width; w++) {
            // 1 - Blue
            // 1 - Green
            // 1 - Red
            // 1 - Alpha
            int b = ByteConverter.unsign(fm.readByte());
            int g = ByteConverter.unsign(fm.readByte());
            int r = ByteConverter.unsign(fm.readByte());
            int a = ByteConverter.unsign(fm.readByte());

            pixels[h * width + w] = ((a << 24) | (r << 16) | (g << 8) | (b));
          }
        }
      }

      else if (colorDepth == 24) {
        for (int h = height - 1; h >= 0; h--) {
          for (int w = 0; w < width; w++) {
            // 1 - Blue
            // 1 - Green
            // 1 - Red
            int b = ByteConverter.unsign(fm.readByte());
            int g = ByteConverter.unsign(fm.readByte());
            int r = ByteConverter.unsign(fm.readByte());

            pixels[h * width + w] = ((255 << 24) | (r << 16) | (g << 8) | (b));
          }
        }
      }

      else if (colorDepth == 16) {
        for (int h = height - 1; h >= 0; h--) {
          for (int w = 0; w < width; w++) {
            // 1 bit  - Alpha
            // 5 bits - Blue
            // 5 bits - Green
            // 5 bits - Red
            int byte1 = ByteConverter.unsign(fm.readByte());
            int byte2 = ByteConverter.unsign(fm.readByte());

            int b = (byte1 & 31) * 8;
            int g = (((byte1 & 224) >> 5) | ((byte2 & 3) << 3)) * 8;
            int r = ((byte2 & 124) >> 2) * 8;

            pixels[h * width + w] = ((255 << 24) | (r << 16) | (g << 8) | (b));
          }
        }
      }

      return pixels;

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
  public int[] readRLE16(FileManipulator fm, int[] pixels, int numPixels, int width, int height) {
    try {

      int numRead = 0;
      while (numRead < numPixels) {
        // 1 - Header
        int header = ByteConverter.unsign(fm.readByte());

        boolean compressed = ((header & 128) == 128);
        int count = (header & 127);
        if (count < 0) {
          count = 256 + count;
        }

        count++;

        if (count == 0) {
          return null;
        }

        if (compressed) {
          // 1 bit  - Alpha
          // 5 bits - Blue
          // 5 bits - Green
          // 5 bits - Red
          int byte1 = ByteConverter.unsign(fm.readByte());
          int byte2 = ByteConverter.unsign(fm.readByte());

          int b = (byte1 & 31) * 8;
          int g = (((byte1 & 224) >> 5) | ((byte2 & 3) << 3)) * 8;
          int r = ((byte2 & 124) >> 2) * 8;

          for (int i = 0; i < count; i++) {
            pixels[numRead + i] = ((255 << 24) | (r << 16) | (g << 8) | (b));
          }
        }
        else {
          for (int i = 0; i < count; i++) {
            // 1 bit  - Alpha
            // 5 bits - Blue
            // 5 bits - Green
            // 5 bits - Red
            int byte1 = ByteConverter.unsign(fm.readByte());
            int byte2 = ByteConverter.unsign(fm.readByte());

            int b = (byte1 & 31) * 8;
            int g = (((byte1 & 224) >> 5) | ((byte2 & 3) << 3)) * 8;
            int r = ((byte2 & 124) >> 2) * 8;

            pixels[numRead + i] = ((255 << 24) | (r << 16) | (g << 8) | (b));
          }
        }

        numRead += count;
      }

      // flip the image
      int[] tempLine = new int[width];
      for (int h = 0, j = height - 1; h < j; h++, j--) {
        System.arraycopy(pixels, h * width, tempLine, 0, width);
        System.arraycopy(pixels, j * width, pixels, h * width, width);
        System.arraycopy(tempLine, 0, pixels, j * width, width);
      }

      return pixels;

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
  public int[] readRLE24(FileManipulator fm, int[] pixels, int numPixels, int width, int height) {
    try {

      int numRead = 0;
      while (numRead < numPixels) {
        // 1 - Header
        int header = ByteConverter.unsign(fm.readByte());

        boolean compressed = ((header & 128) == 128);
        int count = (header & 127);
        if (count < 0) {
          count = 256 + count;
        }

        count++;

        if (count == 0) {
          return null;
        }

        if (compressed) {
          // 1 - Blue
          // 1 - Green
          // 1 - Red
          int b = ByteConverter.unsign(fm.readByte());
          int g = ByteConverter.unsign(fm.readByte());
          int r = ByteConverter.unsign(fm.readByte());

          for (int i = 0; i < count; i++) {
            pixels[numRead + i] = ((255 << 24) | (r << 16) | (g << 8) | (b));
          }
        }
        else {
          for (int i = 0; i < count; i++) {
            // 1 - Blue
            // 1 - Green
            // 1 - Red
            int b = ByteConverter.unsign(fm.readByte());
            int g = ByteConverter.unsign(fm.readByte());
            int r = ByteConverter.unsign(fm.readByte());

            pixels[numRead + i] = ((255 << 24) | (r << 16) | (g << 8) | (b));
          }
        }

        numRead += count;
      }

      // flip the image
      int[] tempLine = new int[width];
      for (int h = 0, j = height - 1; h < j; h++, j--) {
        System.arraycopy(pixels, h * width, tempLine, 0, width);
        System.arraycopy(pixels, j * width, pixels, h * width, width);
        System.arraycopy(tempLine, 0, pixels, j * width, width);
      }

      return pixels;

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
  public int[] readRLE32(FileManipulator fm, int[] pixels, int numPixels, int width, int height) {
    try {

      int numRead = 0;
      while (numRead < numPixels) {
        // 1 - Header
        int header = ByteConverter.unsign(fm.readByte());

        boolean compressed = ((header & 128) == 128);
        int count = (header & 127);
        if (count < 0) {
          count = 256 + count;
        }

        count++;

        if (count == 0) {
          return null;
        }

        if (compressed) {
          // 1 - Blue
          // 1 - Green
          // 1 - Red
          // 1 - Alpha
          int b = ByteConverter.unsign(fm.readByte());
          int g = ByteConverter.unsign(fm.readByte());
          int r = ByteConverter.unsign(fm.readByte());
          int a = ByteConverter.unsign(fm.readByte());

          for (int i = 0; i < count; i++) {
            pixels[numRead + i] = ((a << 24) | (r << 16) | (g << 8) | (b));
          }
        }
        else {
          for (int i = 0; i < count; i++) {
            // 1 - Blue
            // 1 - Green
            // 1 - Red
            // 1 - Alpha
            int b = ByteConverter.unsign(fm.readByte());
            int g = ByteConverter.unsign(fm.readByte());
            int r = ByteConverter.unsign(fm.readByte());
            int a = ByteConverter.unsign(fm.readByte());

            pixels[numRead + i] = ((a << 24) | (r << 16) | (g << 8) | (b));
          }
        }

        numRead += count;
      }

      // flip the image
      int[] tempLine = new int[width];
      for (int h = 0, j = height - 1; h < j; h++, j--) {
        System.arraycopy(pixels, h * width, tempLine, 0, width);
        System.arraycopy(pixels, j * width, pixels, h * width, width);
        System.arraycopy(tempLine, 0, pixels, j * width, width);
      }

      return pixels;

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

      // 1 - Header 2 Size
      int headerSize = ByteConverter.unsign(fm.readByte());

      // 1 - Palette Flag
      int paletteFlag = ByteConverter.unsign(fm.readByte());
      boolean paletted = (paletteFlag == 1);

      // 1 - Image Type
      int imageType = ByteConverter.unsign(fm.readByte());
      boolean compressed = false;
      if (imageType >= 8) {
        compressed = true;
        imageType -= 8;
      }
      if (paletted) {
        if (imageType == 1 && !compressed) {
          // OK - uncompressed paletted RGB image
        }
        else {
          throw new WSPluginException("Only uncompressed Paletted TGA images are supported");
        }
      }
      else if (imageType != 2) {
        throw new WSPluginException("Only RGB TGA images are supported");
      }

      // 2 - First Color Map
      fm.skip(2);

      // 2 - Number Of Colors
      int numColors = fm.readShort();

      // 1 - Number Of Bits per Color
      int bitsPerColor = fm.readByte();

      // 2 - X Position
      // 2 - Y Position
      fm.skip(4);

      // 2 - Width
      short width = fm.readShort();
      FieldValidator.checkWidth(width);

      // 2 - Height
      short height = fm.readShort();
      FieldValidator.checkHeight(height);

      // 1 - Color Depth
      int colorDepth = ByteConverter.unsign(fm.readByte());

      // 1 - Descriptor Flag
      fm.skip(1);

      fm.skip(headerSize);

      int numPixels = width * height;
      int[] pixels = new int[numPixels];

      if (compressed) {
        if (colorDepth == 32) {
          pixels = readRLE32(fm, pixels, numPixels, width, height);
        }
        else if (colorDepth == 24) {
          pixels = readRLE24(fm, pixels, numPixels, width, height);
        }
        else if (colorDepth == 16) {
          pixels = readRLE16(fm, pixels, numPixels, width, height);
        }
      }
      else {
        if (paletted) {
          pixels = readPaletted(fm, pixels, width, height, numColors, bitsPerColor);
        }
        else {
          pixels = readRGB(fm, pixels, width, height, colorDepth);
        }
      }

      fm.close();

      return new ImageResource(pixels, width, height);

    }
    catch (Throwable t) {
      logError(t);
      return null;
    }
  }

  /**
  **********************************************************************************************
  Writes an [archive] File with the contents of the Resources
  **********************************************************************************************
  **/
  @Override
  public void write(PreviewPanel preview, FileManipulator fm) {
    try {

      if (!(preview instanceof PreviewPanel_Image)) {
        return;
      }

      PreviewPanel_Image ivp = (PreviewPanel_Image) preview;

      int imageWidth = ivp.getImageWidth();
      int imageHeight = ivp.getImageHeight();

      if (imageWidth == -1 || imageHeight == -1) {
        return;
      }

      // 1 - Header 2 Size
      fm.writeByte(0);

      // 1 - Palette Flag
      fm.writeByte(0);

      // 1 - Image Type
      fm.writeByte(2);

      // 2 - First Color Map
      fm.writeShort((short) 0);

      // 2 - Number Of Colors
      fm.writeShort((short) 0);

      // 1 - Number Of Bits per Color
      fm.writeByte(0);

      // 2 - X Position
      fm.writeShort((short) 0);

      // 2 - Y Position
      fm.writeShort((short) 0);

      // 2 - Width
      fm.writeShort((short) imageWidth);

      // 2 - Height
      fm.writeShort((short) imageHeight);

      // 1 - Color Depth
      fm.writeByte(32);

      // 1 - Descriptor Flag
      fm.writeByte(0);

      // X - Pixels
      BufferedImage bufImage = new BufferedImage(imageWidth, imageHeight, BufferedImage.TYPE_INT_RGB);
      Graphics g = bufImage.getGraphics();
      g.drawImage(ivp.getImage(), 0, 0, null);

      int[] pixels = bufImage.getRGB(0, 0, imageWidth, imageHeight, null, 0, imageWidth);

      for (int h = imageHeight - 1; h >= 0; h--) {
        for (int w = 0; w < imageWidth; w++) {
          int pixel = pixels[h * imageWidth + w];

          // 1 - Red
          fm.writeByte(pixel);

          // 1 - Green
          fm.writeByte(pixel >> 8);

          // 1 - Blue
          fm.writeByte(pixel >> 16);

          // 1 - Alpha
          fm.writeByte(pixel >> 24);
        }
      }

      fm.close();

    }
    catch (Throwable t) {
      logError(t);
    }
  }

}