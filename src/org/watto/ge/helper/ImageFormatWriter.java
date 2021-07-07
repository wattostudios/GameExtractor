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

package org.watto.ge.helper;

import org.watto.datatype.ImageResource;
import org.watto.io.FileManipulator;

/**
**********************************************************************************************
Writes image data to a FileManipulator using a number of different image formats
**********************************************************************************************
**/
public class ImageFormatWriter {

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public static long computeDXTBitMask(ColorSplit[] colors, int extreme1, int extreme2) {

    ColorSplit color1 = colors[extreme1];
    ColorSplit color2 = colors[extreme2];

    if (color1.equals(color2)) {
      return 0;
    }

    ColorSplit color3 = new ColorSplit();
    color3.setRed((2 * color1.getRed() + color2.getRed() + 1) / 3);
    color3.setGreen((2 * color1.getGreen() + color2.getGreen() + 1) / 3);
    color3.setBlue((2 * color1.getBlue() + color2.getBlue() + 1) / 3);

    ColorSplit color4 = new ColorSplit();
    color4.setRed((color1.getRed() + 2 * color2.getRed() + 1) / 3);
    color4.setGreen((color1.getGreen() + 2 * color2.getGreen() + 1) / 3);
    color4.setBlue((color1.getBlue() + 2 * color2.getBlue() + 1) / 3);

    ColorSplit[] colorPoints = new ColorSplit[] { color1, color2, color3, color4 };

    long bitmask = 0;
    for (int i = 0; i < 16; i++) {
      int closest = 1000;
      int mask = 0;

      ColorSplit color = colors[i];

      for (int j = 0; j < 4; j++) {
        int d = color.getCloseness(colorPoints[j]);
        if (d < closest) {
          closest = d;
          mask = j;
        }
      }
      bitmask |= mask << i * 2;
    }

    return bitmask;
  }

  /**
  **********************************************************************************************
  Reads a 16-bit floating value (IEEE 754 half-precision s10e5) and converts it to a Java float.
  Code from http://stackoverflow.com/questions/6162651/half-precision-floating-point-in-java/6162687
  **********************************************************************************************
  **/
  protected static float convert16bitToFloat(int hbits) {
    int mant = hbits & 0x03ff;            // 10 bits mantissa
    int exp = hbits & 0x7c00;             // 5 bits exponent
    if (exp == 0x7c00) {
      exp = 0x3fc00;                      // -> NaN/Inf
    }
    else if (exp != 0)                    // normalized value
    {
      exp += 0x1c000;                     // exp - 15 + 127
      if (mant == 0 && exp > 0x1c400) {
        return Float.intBitsToFloat((hbits & 0x8000) << 16
            | exp << 13 | 0x3ff);
      }
    }
    else if (mant != 0)                   // && exp==0 -> subnormal
    {
      exp = 0x1c400;                      // make it normal
      do {
        mant <<= 1;                       // mantissa * 2
        exp -= 0x400;                     // decrease exp by 1
      }
      while ((mant & 0x400) == 0);        // while not normal
      mant &= 0x3ff;                      // discard subnormal bit
    }                                     // else +/-0 -> +/-0
    return Float.intBitsToFloat(          // combine all parts
        (hbits & 0x8000) << 16            // sign  << ( 31 - 15 )
            | (exp | mant) << 13);        // value << ( 23 - 10 )
  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  protected static int convertPixelTo565(ColorSplit color) {
    int r = color.getRed() >> 3;
    int g = color.getGreen() >> 2;
    int b = color.getBlue() >> 3;
    return r << 11 | g << 5 | b;
  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  protected static ColorSplit convertPixelTo565(int pixel) {
    ColorSplit color = new ColorSplit();
    color.setRed((int) (((long) pixel) & 0xf800) >> 11);
    color.setGreen((int) (((long) pixel) & 0x07e0) >> 5);
    color.setBlue((int) (((long) pixel) & 0x001f));
    return color;
  }

  /**
   **********************************************************************************************
  Flips an image that is upside-down
   **********************************************************************************************
   **/
  public static ImageResource flipVertically(ImageResource image) {
    int[] pixels = image.getPixels();
    int numPixels = pixels.length;

    int width = image.getWidth();
    int height = image.getHeight();

    int[] reversedPixels = new int[numPixels];
    int reversePos = 0; // where to write to in the reversedPixels array
    for (int h = height - 1; h >= 0; h--) {
      System.arraycopy(pixels, h * width, reversedPixels, reversePos, width);
      reversePos += width;
    }

    image.setPixels(reversedPixels);
    return image;
  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  protected static int[] getExtremes(ColorSplit[] colors) {
    int farthest = 0;

    int extreme1 = 0;
    int extreme2 = 0;

    for (int i = 0; i < 16 - 1; i++) {
      ColorSplit color = colors[i];

      for (int j = i + 1; j < 16; j++) {
        int d = color.getCloseness(colors[j]);
        if (d > farthest) {
          farthest = d;

          extreme1 = i;
          extreme2 = j;
        }
      }
    }

    return new int[] { extreme1, extreme2 };
  }

  /**
   **********************************************************************************************
   * Writes an ARGB image
   **********************************************************************************************
   **/
  public static void writeARGB(FileManipulator fm, ImageResource imageResource) {
    int[] pixels = imageResource.getImagePixels();

    int numPixels = pixels.length;

    for (int i = 0; i < numPixels; i++) {
      // INPUT = ARGB
      int pixel = pixels[i];

      // 1 - Red
      int rPixel = (pixel >> 16) & 255;

      // 1 - Green
      int gPixel = (pixel >> 8) & 255;

      // 1 - Blue
      int bPixel = pixel & 255;

      // 1 - Alpha
      int aPixel = (pixel >> 24) & 255;

      // OUTPUT = ARGB
      fm.writeByte(aPixel);
      fm.writeByte(rPixel);
      fm.writeByte(gPixel);
      fm.writeByte(bPixel);

    }
  }

  /**
   **********************************************************************************************
   * Writes an ARGB4444 image
   **********************************************************************************************
   **/
  public static void writeARGB4444(FileManipulator fm, ImageResource imageResource) {
    int[] pixels = imageResource.getImagePixels();

    int numPixels = pixels.length;

    for (int i = 0; i < numPixels; i++) {
      // INPUT = ARGB
      int pixel = pixels[i];

      // 4bits - Alpha
      // 4bits - Blue
      // 4bits - Green
      // 4bits - Red
      int a = (((((pixel >> 24) & 255) / 16) << 4) & 240);
      int r = ((((pixel >> 16) & 255) / 16) & 15);
      int byte1 = a | r;

      int g = (((((pixel >> 8) & 255) / 16) << 4) & 240);
      int b = ((((pixel) & 255) / 16) & 15);
      int byte2 = g | b;

      // OUTPUT = ARGB4444
      fm.writeByte(byte1);
      fm.writeByte(byte2);
    }
  }

  /**
   **********************************************************************************************
   * Writes an ARGB1555 image
   **********************************************************************************************
   **/
  public static void writeARGB1555(FileManipulator fm, ImageResource imageResource) {
    int[] pixels = imageResource.getImagePixels();

    int numPixels = pixels.length;

    for (int i = 0; i < numPixels; i++) {
      // INPUT = ARGB
      // OUTPUT = 1555ARGB
      int pixel = pixels[i];

      // 1 - Red
      int rPixel = (((pixel >> 16) & 255) / 8);

      // 1 - Green
      int gPixel = (((pixel >> 8) & 255) / 8);

      // 1 - Blue
      int bPixel = ((pixel & 255) / 8);

      // 1 - Alpha
      int aPixel = (pixel >> 24) & 255;
      if (aPixel != 0) {
        aPixel = 1;
      }

      // 1bits - Alpha
      // 5bits - Red
      // 5bits - Green
      // 5bits - Blue

      int byte1 = (aPixel << 7) | (rPixel << 2) | (gPixel >> 3);
      int byte2 = (gPixel & 8) << 5 | (bPixel);

      fm.writeByte(byte1);
      fm.writeByte(byte2);
    }
  }

  /**
   **********************************************************************************************
   * Writes an BGR image
   **********************************************************************************************
   **/
  public static void writeBGR(FileManipulator fm, ImageResource imageResource) {
    int[] pixels = imageResource.getImagePixels();

    int numPixels = pixels.length;

    for (int i = 0; i < numPixels; i++) {
      // INPUT = ARGB
      int pixel = pixels[i];

      // 1 - Red
      int rPixel = (pixel >> 16) & 255;

      // 1 - Green
      int gPixel = (pixel >> 8) & 255;

      // 1 - Blue
      int bPixel = pixel & 255;

      // OUTPUT = BGR
      fm.writeByte(bPixel);
      fm.writeByte(gPixel);
      fm.writeByte(rPixel);
    }
  }

  /**
   **********************************************************************************************
   * Writes an BGRA image
   **********************************************************************************************
   **/
  public static void writeBGRA(FileManipulator fm, ImageResource imageResource) {
    int[] pixels = imageResource.getImagePixels();

    int numPixels = pixels.length;

    for (int i = 0; i < numPixels; i++) {
      // INPUT = ARGB
      int pixel = pixels[i];

      // 1 - Red
      int rPixel = (pixel >> 16) & 255;

      // 1 - Green
      int gPixel = (pixel >> 8) & 255;

      // 1 - Blue
      int bPixel = pixel & 255;

      // 1 - Alpha
      int aPixel = (pixel >> 24) & 255;

      // OUTPUT = BGRA
      fm.writeByte(bPixel);
      fm.writeByte(gPixel);
      fm.writeByte(rPixel);
      fm.writeByte(aPixel);
    }
  }

  /**
   **********************************************************************************************
   * Writes a DXT1 image
   **********************************************************************************************
   **/
  public static void writeDXT1(FileManipulator fm, ImageResource imageResource) {
    int width = imageResource.getWidth();
    int height = imageResource.getHeight();
    int[] pixels = imageResource.getImagePixels();

    int[] pixelBlock = new int[16];

    int numBlocksWide = width / 4;
    int numBlocksHigh = height / 4;

    for (int i = 0; i < numBlocksHigh; i++) {
      for (int j = 0; j < numBlocksWide; j++) {

        // build the array of data
        int position = (i * 4 * width) + (j * 4);
        System.arraycopy(pixels, position, pixelBlock, 0, 4);
        position += width;
        System.arraycopy(pixels, position, pixelBlock, 4, 4);
        position += width;
        System.arraycopy(pixels, position, pixelBlock, 8, 4);
        position += width;
        System.arraycopy(pixels, position, pixelBlock, 12, 4);

        // split the colors into RGB values
        ColorSplit[] colors = new ColorSplit[16];
        for (int p = 0; p < 16; p++) {
          colors[p] = new ColorSplit(pixelBlock[p]);
        }

        for (int k = 0; k < pixelBlock.length; k++) {
          pixelBlock[k] = convertPixelTo565(colors[k]);
          colors[k] = convertPixelTo565(pixelBlock[k]);
        }

        int[] extremes = getExtremes(colors);

        int extreme1 = extremes[0];
        int extreme2 = extremes[1];

        if (pixelBlock[extreme1] < pixelBlock[extreme2]) {
          int temp = extreme1;
          extreme1 = extreme2;
          extreme2 = temp;
        }

        fm.writeShort((short) pixelBlock[extreme1]);
        fm.writeShort((short) pixelBlock[extreme2]);

        long bitmask = computeDXTBitMask(colors, extreme1, extreme2);
        fm.writeInt((int) bitmask);
      }
    }
  }

  /**
   **********************************************************************************************
   * Writes a DXT3 image
   **********************************************************************************************
   **/
  public static void writeDXT3(FileManipulator fm, ImageResource imageResource) {
    int width = imageResource.getWidth();
    int height = imageResource.getHeight();
    int[] pixels = imageResource.getImagePixels();

    int[] pixelBlock = new int[16];

    int numBlocksWide = width / 4;
    int numBlocksHigh = height / 4;

    for (int i = 0; i < numBlocksHigh; i++) {
      for (int j = 0; j < numBlocksWide; j++) {

        // build the array of data
        int position = (i * 4 * width) + (j * 4);
        System.arraycopy(pixels, position, pixelBlock, 0, 4);
        position += width;
        System.arraycopy(pixels, position, pixelBlock, 4, 4);
        position += width;
        System.arraycopy(pixels, position, pixelBlock, 8, 4);
        position += width;
        System.arraycopy(pixels, position, pixelBlock, 12, 4);

        // split the colors into RGB values
        ColorSplit[] colors = new ColorSplit[16];
        for (int p = 0; p < 16; p++) {
          colors[p] = new ColorSplit(pixelBlock[p]);
        }

        // Store the alpha table
        for (int k = 0; k < 16; k += 2) {
          fm.writeByte((byte) ((pixels[k] >>> 24) | (pixels[k + 1] >>> 28)));
        }

        for (int k = 0; k < pixelBlock.length; k++) {
          pixelBlock[k] = convertPixelTo565(colors[k]);
          colors[k] = convertPixelTo565(pixelBlock[k]);
        }

        int[] extremes = getExtremes(colors);

        int extreme1 = extremes[0];
        int extreme2 = extremes[1];

        if (pixelBlock[extreme1] < pixelBlock[extreme2]) {
          int temp = extreme1;
          extreme1 = extreme2;
          extreme2 = temp;
        }

        fm.writeShort((short) pixelBlock[extreme1]);
        fm.writeShort((short) pixelBlock[extreme2]);

        long bitmask = computeDXTBitMask(colors, extreme1, extreme2);
        fm.writeInt((int) bitmask);
      }
    }
  }

  /**
   **********************************************************************************************
   * Writes a DXT3 image
   **********************************************************************************************
   **/
  public static void writeDXT5(FileManipulator fm, ImageResource imageResource) {
    writeDXT3(fm, imageResource);
  }

  /**
   **********************************************************************************************
   * Writes an GBAR4444 image
   **********************************************************************************************
   **/
  public static void writeGBAR4444(FileManipulator fm, ImageResource imageResource) {
    int[] pixels = imageResource.getImagePixels();

    int numPixels = pixels.length;

    for (int i = 0; i < numPixels; i++) {
      // INPUT = ARGB
      int pixel = pixels[i];

      // 4bits - Alpha
      // 4bits - Blue
      // 4bits - Green
      // 4bits - Red
      int g = (((((pixel >> 8) & 255) / 16) << 4) & 240);
      int b = ((((pixel) & 255) / 16) & 15);
      int byte1 = g | b;

      int a = (((((pixel >> 24) & 255) / 16) << 4) & 240);
      int r = ((((pixel >> 16) & 255) / 16) & 15);
      int byte2 = a | r;

      // OUTPUT = GBAR4444
      fm.writeByte(byte1);
      fm.writeByte(byte2);
    }
  }

  /**
   **********************************************************************************************
   * Writes an RGBA color palette
   **********************************************************************************************
   **/
  public static void writePaletteRGBA(FileManipulator fm, int[] palette) {
    int numColors = palette.length;

    for (int i = 0; i < numColors; i++) {
      // INPUT = ARGB
      int pixel = palette[i];

      // 1 - Red
      int rPixel = (pixel >> 16) & 255;

      // 1 - Green
      int gPixel = (pixel >> 8) & 255;

      // 1 - Blue
      int bPixel = pixel & 255;

      // 1 - Alpha
      int aPixel = (pixel >> 24) & 255;

      // OUTPUT = RGBA
      fm.writeByte(rPixel);
      fm.writeByte(gPixel);
      fm.writeByte(bPixel);
      fm.writeByte(aPixel);
    }
  }

  /**
   **********************************************************************************************
   * Writes an RGB color palette
   **********************************************************************************************
   **/
  public static void writePaletteRGB(FileManipulator fm, int[] palette) {
    int numColors = palette.length;

    for (int i = 0; i < numColors; i++) {
      // INPUT = ARGB
      int pixel = palette[i];

      // 1 - Red
      int rPixel = (pixel >> 16) & 255;

      // 1 - Green
      int gPixel = (pixel >> 8) & 255;

      // 1 - Blue
      int bPixel = pixel & 255;

      // OUTPUT = RGB
      fm.writeByte(rPixel);
      fm.writeByte(gPixel);
      fm.writeByte(bPixel);
    }
  }

  /**
   **********************************************************************************************
   * Writes an BGRA color palette
   **********************************************************************************************
   **/
  public static void writePaletteBGRA(FileManipulator fm, int[] palette) {
    int numColors = palette.length;

    for (int i = 0; i < numColors; i++) {
      // INPUT = ARGB
      int pixel = palette[i];

      // 1 - Red
      int rPixel = (pixel >> 16) & 255;

      // 1 - Green
      int gPixel = (pixel >> 8) & 255;

      // 1 - Blue
      int bPixel = pixel & 255;

      // 1 - Alpha
      int aPixel = (pixel >> 24) & 255;

      // OUTPUT = BGRA
      fm.writeByte(bPixel);
      fm.writeByte(gPixel);
      fm.writeByte(rPixel);
      fm.writeByte(aPixel);
    }
  }

  /**
   **********************************************************************************************
   * Writes an RGB image
   **********************************************************************************************
   **/
  public static void writeRGB(FileManipulator fm, ImageResource imageResource) {
    int[] pixels = imageResource.getImagePixels();

    int numPixels = pixels.length;

    for (int i = 0; i < numPixels; i++) {
      // INPUT = ARGB
      int pixel = pixels[i];

      // 1 - Red
      int rPixel = (pixel >> 16) & 255;

      // 1 - Green
      int gPixel = (pixel >> 8) & 255;

      // 1 - Blue
      int bPixel = pixel & 255;

      // OUTPUT = RGBA
      fm.writeByte(rPixel);
      fm.writeByte(gPixel);
      fm.writeByte(bPixel);
    }
  }

  /**
   **********************************************************************************************
   * Writes an RGB565 image
   **********************************************************************************************
   **/
  public static void writeRGB565(FileManipulator fm, ImageResource imageResource) {
    int[] pixels = imageResource.getImagePixels();

    int numPixels = pixels.length;

    for (int i = 0; i < numPixels; i++) {
      // INPUT = ARGB
      int pixel = pixels[i];

      // 5bits - Blue
      // 6bits - Green
      // 5bits - Red
      int r = (((((pixel >> 16) & 255) / 8) << 3) & 248);
      int g1 = (((((pixel >> 8) & 255) / 4) >> 3) & 7);
      int byte2 = g1 | r;

      int g2 = (((((pixel >> 8) & 255) / 4) << 5) & 224);
      int b = ((((pixel) & 255) / 8) & 31);
      int byte1 = g2 | b;

      // OUTPUT = RGB565
      fm.writeByte(byte1);
      fm.writeByte(byte2);
    }
  }

  /**
   **********************************************************************************************
   * Writes an RGBA image
   **********************************************************************************************
   **/
  public static void writeRGBA(FileManipulator fm, ImageResource imageResource) {
    int[] pixels = imageResource.getImagePixels();

    int numPixels = pixels.length;

    for (int i = 0; i < numPixels; i++) {
      // INPUT = ARGB
      int pixel = pixels[i];

      // 1 - Red
      int rPixel = (pixel >> 16) & 255;

      // 1 - Green
      int gPixel = (pixel >> 8) & 255;

      // 1 - Blue
      int bPixel = pixel & 255;

      // 1 - Alpha
      int aPixel = (pixel >> 24) & 255;

      // OUTPUT = RGBA
      fm.writeByte(rPixel);
      fm.writeByte(gPixel);
      fm.writeByte(bPixel);
      fm.writeByte(aPixel);
    }
  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public ImageFormatWriter() {
  }

}