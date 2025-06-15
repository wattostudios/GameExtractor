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

import java.io.File;

import org.watto.ErrorLogger;
import org.watto.Settings;
import org.watto.SingletonManager;
import org.watto.datatype.ImageResource;
import org.watto.datatype.PalettedImageResource;
import org.watto.datatype.Resource;
import org.watto.ge.plugin.viewer.Viewer_DDS_DDS;
import org.watto.io.FileManipulator;
import org.watto.io.buffer.ByteBuffer;
import org.watto.io.converter.ByteConverter;
import org.watto.io.converter.ShortConverter;

/**
**********************************************************************************************
Reads image data from a FileManipulator using a number of different image formats
**********************************************************************************************
**/
public class ImageFormatReader {

  /**
  **********************************************************************************************
  Calculates a Morton Index for an (x,y) co-ordinate
  **********************************************************************************************
  **/
  public static long calculateMorton2D(long x, long y) {
    x = (x | (x << 16)) & 0x0000FFFF0000FFFFl;
    x = (x | (x << 8)) & 0x00FF00FF00FF00FFl;
    x = (x | (x << 4)) & 0x0F0F0F0F0F0F0F0Fl;
    x = (x | (x << 2)) & 0x3333333333333333l;
    x = (x | (x << 1)) & 0x5555555555555555l;

    y = (y | (y << 16)) & 0x0000FFFF0000FFFFl;
    y = (y | (y << 8)) & 0x00FF00FF00FF00FFl;
    y = (y | (y << 4)) & 0x0F0F0F0F0F0F0F0Fl;
    y = (y | (y << 2)) & 0x3333333333333333l;
    y = (y | (y << 1)) & 0x5555555555555555l;

    long result = x | (y << 1);
    return result;
  }

  /**
  **********************************************************************************************
  Reads a 16-bit floating value (IEEE 754 half-precision s10e5) and converts it to a Java float.
  Code from http://stackoverflow.com/questions/6162651/half-precision-floating-point-in-java/6162687
  **********************************************************************************************
  **/
  protected static float convert16bitToFloat(int hbits) {
    int mant = hbits & 0x03ff; // 10 bits mantissa
    int exp = hbits & 0x7c00; // 5 bits exponent
    if (exp == 0x7c00) {
      exp = 0x3fc00; // -> NaN/Inf
    }
    else if (exp != 0) // normalized value
    {
      exp += 0x1c000; // exp - 15 + 127
      if (mant == 0 && exp > 0x1c400) {
        return Float.intBitsToFloat((hbits & 0x8000) << 16
            | exp << 13 | 0x3ff);
      }
    }
    else if (mant != 0) // && exp==0 -> subnormal
    {
      exp = 0x1c400; // make it normal
      do {
        mant <<= 1; // mantissa * 2
        exp -= 0x400; // decrease exp by 1
      }
      while ((mant & 0x400) == 0); // while not normal
      mant &= 0x3ff; // discard subnormal bit
    } // else +/-0 -> +/-0
    return Float.intBitsToFloat( // combine all parts
        (hbits & 0x8000) << 16 // sign  << ( 31 - 15 )
            | (exp | mant) << 13); // value << ( 23 - 10 )
  }

  /**
   **********************************************************************************************
  Interlace/deinterlace an image
   **********************************************************************************************
   **/
  public static ImageResource interlaceVertically(ImageResource image, int interlaceSize) {
    int[] pixels = image.getPixels();
    int numPixels = pixels.length;

    int width = image.getWidth();
    int height = image.getHeight();

    // split the image into columns
    int[][] columns = new int[width][0];
    for (int w = 0; w < width; w++) {
      int[] column = new int[height];
      for (int h = 0; h < height; h++) {
        column[h] = pixels[h * width + w];
      }
      columns[w] = column;
    }

    // shuffle the columns based on the interlacing
    int[][] oldColumns = columns;
    columns = new int[width][0];
    int columnPos = 0;

    for (int i = 0; i < interlaceSize; i++) {
      for (int w = i; w < width; w += interlaceSize) {
        columns[columnPos] = oldColumns[w];
        columnPos++;
      }
    }

    // re-build the image
    int[] interlacedPixels = new int[numPixels];
    int pixelPos = 0;
    for (int h = 0; h < height; h++) {
      for (int w = 0; w < width; w++) {
        interlacedPixels[pixelPos] = columns[w][h];
        pixelPos++;
      }
    }

    image.setPixels(interlacedPixels);
    return image;
  }

  /**
   **********************************************************************************************
  Swaps every column pair so that they appear in the opposite order
   **********************************************************************************************
   **/
  public static ImageResource swapColumnPairs(ImageResource image) {
    int[] pixels = image.getPixels();
    int numPixels = pixels.length;

    int width = image.getWidth();
    int height = image.getHeight();

    // split the image into columns
    int[][] columns = new int[width][0];
    for (int w = 0; w < width; w++) {
      int[] column = new int[height];
      for (int h = 0; h < height; h++) {
        column[h] = pixels[h * width + w];
      }
      columns[w] = column;
    }

    // shuffle the columns based on the interlacing
    int[][] oldColumns = columns;
    columns = new int[width][0];

    for (int w = 0; w < width; w += 2) {
      columns[w] = oldColumns[w + 1];
      columns[w + 1] = oldColumns[w];
    }

    // re-build the image
    int[] interlacedPixels = new int[numPixels];
    int pixelPos = 0;
    for (int h = 0; h < height; h++) {
      for (int w = 0; w < width; w++) {
        interlacedPixels[pixelPos] = columns[w][h];
        pixelPos++;
      }
    }

    image.setPixels(interlacedPixels);
    return image;
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
  Re-orders an image which has been stored in blocks of blockWidthxblockHeight
   **********************************************************************************************
   **/
  public static ImageResource reorderPixelBlocks(ImageResource image, int blockWidth, int blockHeight) {
    int[] pixels = image.getPixels();
    int numPixels = pixels.length;

    int width = image.getWidth();
    int height = image.getHeight();

    // First, split up all the existing pixels into blocks
    int numWidthBlocks = width / blockWidth;
    int numHeightBlocks = height / blockHeight;

    int totalBlocks = numHeightBlocks * numWidthBlocks;
    int blockSize = blockWidth * blockHeight;

    int[][] pixelBlocks = new int[totalBlocks][blockSize];
    int readPos = 0;
    for (int b = 0; b < totalBlocks; b++) {
      int[] block = new int[blockSize];
      System.arraycopy(pixels, readPos, block, 0, blockSize);
      readPos += blockSize;
      pixelBlocks[b] = block;
    }

    // Now shuffle the blocks
    /*
    int[][] shuffledBlocks = new int[totalBlocks][blockSize];
    
    int outPos = 0;
    for (int x = 0; x < numWidthBlocks; x++) {
      for (int y = 0; y < numHeightBlocks; y++) {
        int readIndex = (int) calculateMorton2D(x, y);
        System.out.println(x + "," + y + "=" + readIndex);
        if (readIndex >= totalBlocks) {
          continue;
        }
        shuffledBlocks[outPos] = pixelBlocks[readIndex];
        outPos++;
      }
    }
    */

    // Now put the blocks back into the image
    int[] reversedPixels = new int[numPixels];

    int currentHeightBlock = 0;
    int currentWidthBlock = 0;
    for (int b = 0; b < totalBlocks; b++) {
      //int[] block = shuffledBlocks[b];
      int[] block = pixelBlocks[b];

      int blockReadPos = 0;
      int writePos = (currentHeightBlock * blockHeight * width) + (currentWidthBlock * blockWidth);
      for (int h = 0; h < blockHeight; h++) {
        System.arraycopy(block, blockReadPos, reversedPixels, writePos, blockWidth);
        blockReadPos += blockWidth;

        readPos += blockWidth;
        writePos += width;
      }

      currentWidthBlock++;
      if (currentWidthBlock >= numWidthBlocks) {
        currentHeightBlock++;
        currentWidthBlock = 0;
      }
    }

    image.setPixels(reversedPixels);
    return image;
  }

  /**
  **********************************************************************************************
   * Reads R16:G16 pixel data, where each 16 is a float value
   * Doing x255 for each step to convert to a 32-bit image, which is a good approximation. If Java
   * did 64-bit images, would change this to x65535 for the true 64-bit colors
  **********************************************************************************************
  **/
  public static ImageResource read16F16F_RG(FileManipulator fm, int width, int height) {
    int numPixels = width * height;

    // X Bytes - Pixel Data
    int[] pixels = new int[numPixels];

    for (int i = 0; i < numPixels; i++) {
      int rPixel = fm.readShort();
      rPixel = (int) (convert16bitToFloat(rPixel) * 255);

      int gPixel = fm.readShort();
      gPixel = (int) (convert16bitToFloat(gPixel) * 255);

      pixels[i] = ((rPixel << 16) | (0 << 8) | gPixel | (255 << 24));
    }

    return new ImageResource(pixels, width, height);
  }

  /**
  **********************************************************************************************
   * Reads A16:B16:G16:R16 pixel data, where each 16 is a float value
   * Doing x255 for each step to convert to a 32-bit image, which is a good approximation. If Java
   * did 64-bit images, would change this to x65535 for the true 64-bit colors
  **********************************************************************************************
  **/
  public static ImageResource read16F16F16F16F_ABGR(FileManipulator fm, int width, int height) {
    int numPixels = width * height;

    // X Bytes - Pixel Data
    int[] pixels = new int[numPixels];

    for (int i = 0; i < numPixels; i++) {
      int aPixel = ShortConverter.unsign(fm.readShort());
      aPixel = (int) (convert16bitToFloat(aPixel) * 255);
      aPixel = ByteConverter.unsign((byte) aPixel);

      int bPixel = ShortConverter.unsign(fm.readShort());
      bPixel = (int) (convert16bitToFloat(bPixel) * 255);
      bPixel = ByteConverter.unsign((byte) bPixel);

      int rPixel = ShortConverter.unsign(fm.readShort());
      rPixel = (int) (convert16bitToFloat(rPixel) * 255);
      rPixel = ByteConverter.unsign((byte) rPixel);

      int gPixel = ShortConverter.unsign(fm.readShort());
      gPixel = (int) (convert16bitToFloat(gPixel) * 255);
      gPixel = ByteConverter.unsign((byte) gPixel);

      pixels[i] = ((rPixel << 16) | (bPixel << 8) | gPixel | (aPixel << 24));
    }

    return new ImageResource(pixels, width, height);
  }

  /**
  **********************************************************************************************
   * Reads A16:R16:G16:B16 pixel data, where each 16 is a float value
   * Doing x255 for each step to convert to a 32-bit image, which is a good approximation. If Java
   * did 64-bit images, would change this to x65535 for the true 64-bit colors
  **********************************************************************************************
  **/
  public static ImageResource read16F16F16F16F_ARGB(FileManipulator fm, int width, int height) {
    int numPixels = width * height;

    // X Bytes - Pixel Data
    int[] pixels = new int[numPixels];

    for (int i = 0; i < numPixels; i++) {

      int aPixel = ShortConverter.unsign(fm.readShort());
      aPixel = (int) (convert16bitToFloat(aPixel) * 255);
      aPixel = ByteConverter.unsign((byte) aPixel);

      int rPixel = ShortConverter.unsign(fm.readShort());
      rPixel = (int) (convert16bitToFloat(rPixel) * 255);
      rPixel = ByteConverter.unsign((byte) rPixel);

      int gPixel = ShortConverter.unsign(fm.readShort());
      gPixel = (int) (convert16bitToFloat(gPixel) * 255);
      gPixel = ByteConverter.unsign((byte) gPixel);

      int bPixel = ShortConverter.unsign(fm.readShort());
      bPixel = (int) (convert16bitToFloat(bPixel) * 255);
      bPixel = ByteConverter.unsign((byte) bPixel);

      pixels[i] = ((rPixel << 16) | (bPixel << 8) | gPixel | (aPixel << 24));
    }

    return new ImageResource(pixels, width, height);
  }

  /**
  **********************************************************************************************
   * Reads B16:G16:R16:A16 pixel data, where each 16 is a float value
   * Doing x255 for each step to convert to a 32-bit image, which is a good approximation. If Java
   * did 64-bit images, would change this to x65535 for the true 64-bit colors
  **********************************************************************************************
  **/
  public static ImageResource read16F16F16F16F_BGRA(FileManipulator fm, int width, int height) {
    int numPixels = width * height;

    // X Bytes - Pixel Data
    int[] pixels = new int[numPixels];

    for (int i = 0; i < numPixels; i++) {
      int bPixel = ShortConverter.unsign(fm.readShort());
      bPixel = (int) (convert16bitToFloat(bPixel) * 255);
      bPixel = ByteConverter.unsign((byte) bPixel);

      int gPixel = ShortConverter.unsign(fm.readShort());
      gPixel = (int) (convert16bitToFloat(gPixel) * 255);
      gPixel = ByteConverter.unsign((byte) gPixel);

      int rPixel = ShortConverter.unsign(fm.readShort());
      rPixel = (int) (convert16bitToFloat(rPixel) * 255);
      rPixel = ByteConverter.unsign((byte) rPixel);

      int aPixel = ShortConverter.unsign(fm.readShort());
      aPixel = (int) (convert16bitToFloat(aPixel) * 255);
      aPixel = ByteConverter.unsign((byte) aPixel);

      pixels[i] = ((rPixel << 16) | (bPixel << 8) | gPixel | (aPixel << 24));
    }

    return new ImageResource(pixels, width, height);
  }

  /**
  **********************************************************************************************
   * Reads R16:G16:B16:A16 pixel data, where each 16 is a float value
   * Doing x255 for each step to convert to a 32-bit image, which is a good approximation. If Java
   * did 64-bit images, would change this to x65535 for the true 64-bit colors
  **********************************************************************************************
  **/
  public static ImageResource read16F16F16F16F_RGBA(FileManipulator fm, int width, int height) {
    int numPixels = width * height;

    // X Bytes - Pixel Data
    int[] pixels = new int[numPixels];

    for (int i = 0; i < numPixels; i++) {
      int rPixel = ShortConverter.unsign(fm.readShort());
      rPixel = (int) (convert16bitToFloat(rPixel) * 255);
      rPixel = ByteConverter.unsign((byte) rPixel);

      int gPixel = ShortConverter.unsign(fm.readShort());
      gPixel = (int) (convert16bitToFloat(gPixel) * 255);
      gPixel = ByteConverter.unsign((byte) gPixel);

      int bPixel = ShortConverter.unsign(fm.readShort());
      bPixel = (int) (convert16bitToFloat(bPixel) * 255);
      bPixel = ByteConverter.unsign((byte) bPixel);

      int aPixel = ShortConverter.unsign(fm.readShort());
      aPixel = (int) (convert16bitToFloat(aPixel) * 255);
      aPixel = ByteConverter.unsign((byte) aPixel);

      pixels[i] = ((rPixel << 16) | (bPixel << 8) | gPixel | (aPixel << 24));
    }

    return new ImageResource(pixels, width, height);
  }

  /**
   **********************************************************************************************
   * Reads R32 pixel data, where each 32 is a float value
   * Doing x255 for each step to convert to a 32-bit image, which is a good approximation. If Java
   * did 128-bit images, would change this to x(2 pow 32) for the true 128-bit colors
   **********************************************************************************************
   **/
  public static ImageResource read32F_R(FileManipulator fm, int width, int height) {

    int numPixels = width * height;

    // X Bytes - Pixel Data
    int[] pixels = new int[numPixels];

    for (int i = 0; i < numPixels; i++) {

      //byte[] bytes = fm.readBytes(4);
      //bytes[0] |= 1;
      //float r = FloatConverter.convertLittle(bytes);

      float r = (fm.readFloat());

      int rPixel = (int) (r * 255);

      pixels[i] = ((rPixel << 16) | (0 << 8) | 0 | (255 << 24));
    }

    return new ImageResource(pixels, width, height);

  }

  /**
   **********************************************************************************************
   * Reads R32:G32 pixel data, where each 32 is a float value
   * Doing x255 for each step to convert to a 32-bit image, which is a good approximation. If Java
   * did 128-bit images, would change this to x(2 pow 32) for the true 128-bit colors
   **********************************************************************************************
   **/
  public static ImageResource read32F32F_RG(FileManipulator fm, int width, int height) {

    int numPixels = width * height;

    // X Bytes - Pixel Data
    int[] pixels = new int[numPixels];

    for (int i = 0; i < numPixels; i++) {
      float r = fm.readFloat();
      int rPixel = (int) (r * 255);

      float g = fm.readFloat();
      int gPixel = (int) (g * 255);

      pixels[i] = ((rPixel << 16) | (0 << 8) | gPixel | (255 << 24));
    }

    return new ImageResource(pixels, width, height);

  }

  /**
   **********************************************************************************************
   * Reads R32:G32:B32 pixel data, where each 32 is a float value
   * Doing x255 for each step to convert to a 32-bit image, which is a good approximation. If Java
   * did 128-bit images, would change this to x(2 pow 32) for the true 128-bit colors
   **********************************************************************************************
   **/
  public static ImageResource read32F32F32F_RGB(FileManipulator fm, int width, int height) {

    int numPixels = width * height;

    // X Bytes - Pixel Data
    int[] pixels = new int[numPixels];

    for (int i = 0; i < numPixels; i++) {
      float r = fm.readFloat();
      int rPixel = (int) (r * 255);

      float g = fm.readFloat();
      int gPixel = (int) (g * 255);

      float b = fm.readFloat();
      int bPixel = (int) (b * 255);

      pixels[i] = ((rPixel << 16) | (bPixel << 8) | gPixel | (255 << 24));
    }

    return new ImageResource(pixels, width, height);

  }

  /**
   **********************************************************************************************
   * Reads A32:B32:G32:R32 pixel data, where each 32 is a float value
   * Doing x255 for each step to convert to a 32-bit image, which is a good approximation. If Java
   * did 128-bit images, would change this to x(2 pow 32) for the true 128-bit colors
   **********************************************************************************************
   **/
  public static ImageResource read32F32F32F32F_ABGR(FileManipulator fm, int width, int height) {

    int numPixels = width * height;

    // X Bytes - Pixel Data
    int[] pixels = new int[numPixels];

    for (int i = 0; i < numPixels; i++) {
      float a = fm.readFloat();
      int aPixel = (int) (a * 255);

      float b = fm.readFloat();
      int bPixel = (int) (b * 255);

      float g = fm.readFloat();
      int gPixel = (int) (g * 255);

      float r = fm.readFloat();
      int rPixel = (int) (r * 255);

      pixels[i] = ((rPixel << 16) | (bPixel << 8) | gPixel | (aPixel << 24));
    }

    return new ImageResource(pixels, width, height);

  }

  /**
   **********************************************************************************************
   * Reads A32:R32:G32:B32 pixel data, where each 32 is a float value
   * Doing x255 for each step to convert to a 32-bit image, which is a good approximation. If Java
   * did 128-bit images, would change this to x(2 pow 32) for the true 128-bit colors
   **********************************************************************************************
   **/
  public static ImageResource read32F32F32F32F_ARGB(FileManipulator fm, int width, int height) {

    int numPixels = width * height;

    // X Bytes - Pixel Data
    int[] pixels = new int[numPixels];

    for (int i = 0; i < numPixels; i++) {
      float a = fm.readFloat();
      int aPixel = (int) (a * 255);

      float r = fm.readFloat();
      int rPixel = (int) (r * 255);

      float g = fm.readFloat();
      int gPixel = (int) (g * 255);

      float b = fm.readFloat();
      int bPixel = (int) (b * 255);

      pixels[i] = ((rPixel << 16) | (bPixel << 8) | gPixel | (aPixel << 24));
    }

    return new ImageResource(pixels, width, height);

  }

  /**
   **********************************************************************************************
   * Reads B32:G32:R32:A32 pixel data, where each 32 is a float value
   * Doing x255 for each step to convert to a 32-bit image, which is a good approximation. If Java
   * did 128-bit images, would change this to x(2 pow 32) for the true 128-bit colors
   **********************************************************************************************
   **/
  public static ImageResource read32F32F32F32F_BGRA(FileManipulator fm, int width, int height) {

    int numPixels = width * height;

    // X Bytes - Pixel Data
    int[] pixels = new int[numPixels];

    for (int i = 0; i < numPixels; i++) {
      float b = fm.readFloat();
      int bPixel = (int) (b * 255);

      float g = fm.readFloat();
      int gPixel = (int) (g * 255);

      float r = fm.readFloat();
      int rPixel = (int) (r * 255);

      float a = fm.readFloat();
      int aPixel = (int) (a * 255);

      pixels[i] = ((rPixel << 16) | (bPixel << 8) | gPixel | (aPixel << 24));
    }

    return new ImageResource(pixels, width, height);

  }

  /**
   **********************************************************************************************
   * Reads R32:G32:B32:A32 pixel data, where each 32 is a float value
   * Doing x255 for each step to convert to a 32-bit image, which is a good approximation. If Java
   * did 128-bit images, would change this to x(2 pow 32) for the true 128-bit colors
   **********************************************************************************************
   **/
  public static ImageResource read32F32F32F32F_RGBA(FileManipulator fm, int width, int height) {

    int numPixels = width * height;

    // X Bytes - Pixel Data
    int[] pixels = new int[numPixels];

    for (int i = 0; i < numPixels; i++) {
      float r = fm.readFloat();
      int rPixel = (int) (r * 255);

      float g = fm.readFloat();
      int gPixel = (int) (g * 255);

      float b = fm.readFloat();
      int bPixel = (int) (b * 255);

      float a = fm.readFloat();
      int aPixel = (int) (a * 255);

      pixels[i] = ((rPixel << 16) | (bPixel << 8) | gPixel | (aPixel << 24));
    }

    return new ImageResource(pixels, width, height);

  }

  /**
   **********************************************************************************************
   * Reads an uncompressed 4bit image (eg paletted grayscale image with indexed values)
   **********************************************************************************************
   **/
  public static ImageResource read4BitPaletted(FileManipulator fm, int width, int height) {
    int[] palette = PaletteGenerator.getGrayscalePalette().getPalette();
    return read4BitPaletted(fm, width, height, palette);
  }

  /**
   **********************************************************************************************
   * Reads an uncompressed 4bit image (eg paletted grayscale image with indexed values)
   **********************************************************************************************
   **/
  public static ImageResource read4BitPaletted(FileManipulator fm, int width, int height, int[] palette) {
    int numPixels = width * height;
    int[] pixels = new int[numPixels];

    for (int i = 0; i < numPixels; i += 2) {
      int pixel = ByteConverter.unsign(fm.readByte());

      int pixel2 = (pixel >> 4) & 15;
      int pixel1 = (pixel & 15);

      pixels[i] = palette[pixel1];
      pixels[i + 1] = palette[pixel2];
    }

    return new ImageResource(pixels, width, height);
  }

  /**
   **********************************************************************************************
   * Reads an uncompressed 4bit image (eg paletted grayscale image with indexed values)
   **********************************************************************************************
   **/
  public static ImageResource read4BitPaletted(FileManipulator fm, int width, int height, boolean usePaletteManager) {
    if (!usePaletteManager || PaletteManager.getNumPalettes() <= 0) {
      return read4BitPaletted(fm, width, height);
    }

    int numPixels = width * height;
    int[] pixels = new int[numPixels];

    for (int i = 0; i < numPixels; i += 2) {
      int pixel = ByteConverter.unsign(fm.readByte());

      int pixel2 = (pixel >> 4) & 15;
      int pixel1 = (pixel & 15);

      pixels[i] = pixel1;
      pixels[i + 1] = pixel2;
    }

    return new PalettedImageResource(pixels, width, height, PaletteManager.getCurrentPalette().getPalette());
  }

  /**
   **********************************************************************************************
   * Reads an uncompressed 8bit image (eg paletted grayscale image with indexed values)
   **********************************************************************************************
   **/
  public static ImageResource read8BitPaletted(FileManipulator fm, int width, int height) {
    int[] palette = PaletteGenerator.getGrayscalePalette().getPalette();
    return read8BitPaletted(fm, width, height, palette);
  }

  /**
   **********************************************************************************************
   * Reads an 8bit paletted image, using the given palette
   **********************************************************************************************
   **/
  public static ImageResource read8BitPaletted(FileManipulator fm, int width, int height, int[] palette) {
    int numPixels = width * height;
    int[] pixels = new int[numPixels];

    for (int i = 0; i < numPixels; i++) {
      pixels[i] = palette[ByteConverter.unsign(fm.readByte())];
    }

    return new ImageResource(pixels, width, height);
  }

  /**
   **********************************************************************************************
   * Reads an 8bit paletted image, using the given palette
   **********************************************************************************************
   **/
  public static ImageResource read8BitPaletted(FileManipulator fm, int width, int height, boolean usePaletteManager) {
    if (!usePaletteManager || PaletteManager.getNumPalettes() <= 0) {
      return read8BitPaletted(fm, width, height);
    }

    int numPixels = width * height;
    int[] pixels = new int[numPixels];

    for (int i = 0; i < numPixels; i++) {
      pixels[i] = ByteConverter.unsign(fm.readByte());
    }

    return new PalettedImageResource(pixels, width, height, PaletteManager.getCurrentPalette().getPalette());
  }

  /**
   **********************************************************************************************
   * Reads Alpha-8 Luminance-8 pixel data
   **********************************************************************************************
   **/
  public static ImageResource readA8L8(FileManipulator fm, int width, int height) {

    int numPixels = width * height;
    int[] pixels = new int[numPixels];

    for (int i = 0; i < numPixels; i++) {
      int alpha = ByteConverter.unsign(fm.readByte());
      int luminence = ByteConverter.unsign(fm.readByte());

      pixels[i] = ((luminence << 16) | (luminence << 8) | luminence | (alpha << 24));
    }

    return new ImageResource(pixels, width, height);
  }

  /**
   **********************************************************************************************
   * Reads ABGR Pixel Data
   **********************************************************************************************
   **/
  public static ImageResource readABGR(FileManipulator fm, int width, int height) {

    int numPixels = width * height;

    // X Bytes - Pixel Data
    int[] pixels = new int[numPixels];

    for (int i = 0; i < numPixels; i++) {
      // INPUT = ABGR
      int aPixel = ByteConverter.unsign(fm.readByte());
      int bPixel = ByteConverter.unsign(fm.readByte());
      int gPixel = ByteConverter.unsign(fm.readByte());
      int rPixel = ByteConverter.unsign(fm.readByte());

      // OUTPUT = ARGB
      pixels[i] = ((rPixel << 16) | (gPixel << 8) | bPixel | (aPixel << 24));
    }

    return new ImageResource(pixels, width, height);

  }

  /**
   **********************************************************************************************
   * Reads A4R4G4B4 Pixel Data
   **********************************************************************************************
   **/
  public static ImageResource readABGR4444(FileManipulator fm, int width, int height) {

    int numPixels = width * height;

    // X Bytes - Pixel Data
    int[] pixels = new int[numPixels];

    for (int i = 0; i < numPixels; i++) {
      int byte1 = ByteConverter.unsign(fm.readByte());
      int byte2 = ByteConverter.unsign(fm.readByte());

      int aPixel = (byte1 >> 4) * 16;
      int bPixel = (byte1 & 15) * 16;

      int gPixel = (byte2 >> 4) * 16;
      int rPixel = (byte2 & 15) * 16;

      pixels[i] = ((rPixel << 16) | (gPixel << 8) | bPixel | (aPixel << 24));
    }

    return new ImageResource(pixels, width, height);

  }

  /**
   **********************************************************************************************
   * Reads ARGB Pixel Data
   **********************************************************************************************
   **/
  public static ImageResource readARGB(FileManipulator fm, int width, int height) {

    int numPixels = width * height;

    // X Bytes - Pixel Data
    int[] pixels = new int[numPixels];

    for (int i = 0; i < numPixels; i++) {
      // INPUT = ARGB
      int aPixel = ByteConverter.unsign(fm.readByte());
      int rPixel = ByteConverter.unsign(fm.readByte());
      int gPixel = ByteConverter.unsign(fm.readByte());
      int bPixel = ByteConverter.unsign(fm.readByte());

      // OUTPUT = ARGB
      pixels[i] = ((rPixel << 16) | (gPixel << 8) | bPixel | (aPixel << 24));
    }

    return new ImageResource(pixels, width, height);

  }

  /**
   **********************************************************************************************
   * Reads ABGR1555 Pixel Data
   **********************************************************************************************
   **/
  public static ImageResource readABGR1555(FileManipulator fm, int width, int height) {

    int numPixels = width * height;

    // X Bytes - Pixel Data
    int[] pixels = new int[numPixels];

    for (int i = 0; i < numPixels; i++) {
      int byte1 = ByteConverter.unsign(fm.readByte());
      int byte2 = ByteConverter.unsign(fm.readByte());

      int a = (byte2 >> 7) * 255;
      int b = ((byte2 & 124) >> 2) * 8;
      int g = (((byte2 & 3) << 3) | ((byte1 & 224) >> 5)) * 8;
      int r = (byte1 & 31) * 8;

      // GGGRRRRR ABBBBBGG

      // OUTPUT = ARGB
      pixels[i] = ((r << 16) | (g << 8) | b | (a << 24));
    }

    return new ImageResource(pixels, width, height);
  }

  /**
   **********************************************************************************************
   * Reads ARGB1555 Pixel Data
   **********************************************************************************************
   **/
  public static ImageResource readARGB1555(FileManipulator fm, int width, int height) {

    int numPixels = width * height;

    // X Bytes - Pixel Data
    int[] pixels = new int[numPixels];

    for (int i = 0; i < numPixels; i++) {
      int byte1 = ByteConverter.unsign(fm.readByte());
      int byte2 = ByteConverter.unsign(fm.readByte());

      int a = (byte2 >> 7) * 255;
      int r = ((byte2 & 124) >> 2) * 8;
      int g = (((byte2 & 3) << 3) | ((byte1 & 224) >> 5)) * 8;
      int b = (byte1 & 31) * 8;

      // OUTPUT = ARGB
      pixels[i] = ((r << 16) | (g << 8) | b | (a << 24));
    }

    return new ImageResource(pixels, width, height);
  }

  /**
   **********************************************************************************************
   * Reads ARGB1555 Pixel Data
   **********************************************************************************************
   **/
  public static ImageResource readARGB1555BigEndian(FileManipulator fm, int width, int height) {

    int numPixels = width * height;

    // X Bytes - Pixel Data
    int[] pixels = new int[numPixels];

    for (int i = 0; i < numPixels; i++) {
      int byte2 = ByteConverter.unsign(fm.readByte()); // swapped
      int byte1 = ByteConverter.unsign(fm.readByte()); // swapped

      int a = (byte2 >> 7) * 255;
      int r = ((byte2 & 124) >> 2) * 8;
      int g = (((byte2 & 3) << 3) | ((byte1 & 224) >> 5)) * 8;
      int b = (byte1 & 31) * 8;

      // OUTPUT = ARGB
      pixels[i] = ((r << 16) | (g << 8) | b | (a << 24));
    }

    return new ImageResource(pixels, width, height);
  }

  /**
   **********************************************************************************************
   * Reads Nintendo Switch RGB1555 Pixel Data
   **********************************************************************************************
   **/
  /*  public static ImageResource readRGB565Switch(FileManipulator fm, int width, int height) {
  
    int numBytes = width * height * 2;
    byte[] bytes = fm.readBytes(numBytes);
  
    int[] pixels = unswizzleSwitch565(bytes, width, height);
  
    return new ImageResource(pixels, width, height);
  }
  */
  /**
   **********************************************************************************************
   * Reads A4R4G4B4 Pixel Data
   **********************************************************************************************
   **/
  public static ImageResource readARGB4444(FileManipulator fm, int width, int height) {

    int numPixels = width * height;

    // X Bytes - Pixel Data
    int[] pixels = new int[numPixels];

    for (int i = 0; i < numPixels; i++) {
      int byte1 = ByteConverter.unsign(fm.readByte());
      int byte2 = ByteConverter.unsign(fm.readByte());

      int aPixel = (byte2 >> 4) * 16;
      int rPixel = (byte2 & 15) * 16;

      int gPixel = (byte1 >> 4) * 16;
      int bPixel = (byte1 & 15) * 16;

      pixels[i] = ((rPixel << 16) | (gPixel << 8) | bPixel | (aPixel << 24));
    }

    return new ImageResource(pixels, width, height);

  }

  /**
   **********************************************************************************************
   * Reads a BC4 (ATI1) Image
   **********************************************************************************************
   **/
  public static ImageResource readATI1(FileManipulator fm, int width, int height) {
    return readBC4(fm, width, height);
  }

  /**
   **********************************************************************************************
   * Reads a BC5 (ATI2) Image
   **********************************************************************************************
   **/
  public static ImageResource readATI2(FileManipulator fm, int width, int height) {
    return readBC5(fm, width, height);
  }

  /**
   **********************************************************************************************
   * Reads B4A4R4G4 Pixel Data
   **********************************************************************************************
   **/
  public static ImageResource readBARG4444(FileManipulator fm, int width, int height) {

    int numPixels = width * height;

    // X Bytes - Pixel Data
    int[] pixels = new int[numPixels];

    for (int i = 0; i < numPixels; i++) {
      int byte1 = ByteConverter.unsign(fm.readByte());
      int byte2 = ByteConverter.unsign(fm.readByte());

      int bPixel = (byte1 >> 4) * 16;
      int aPixel = (byte1 & 15) * 16;

      int rPixel = (byte2 >> 4) * 16;
      int gPixel = (byte2 & 15) * 16;

      //System.out.println(rPixel + "\t" + gPixel + "\t" + bPixel + "\t" + aPixel);

      pixels[i] = ((rPixel << 16) | (gPixel << 8) | bPixel | (aPixel << 24));
    }

    return new ImageResource(pixels, width, height);

  }

  /**
   **********************************************************************************************
   * Reads B4A4R4G4 Pixel Data (Swizzled)
   **********************************************************************************************
   **/
  public static ImageResource readBARG4444Swizzled(FileManipulator fm, int width, int height) {

    int numPixels = width * height;

    // X Bytes - Pixel Data
    int[] pixels = new int[numPixels];
    pixels = unswizzle(pixels, width, height, 2048);

    for (int i = 0; i < numPixels; i++) {
      int byte1 = ByteConverter.unsign(fm.readByte());
      int byte2 = ByteConverter.unsign(fm.readByte());

      int bPixel = (byte1 >> 4) * 16;
      int aPixel = (byte1 & 15) * 16;

      int rPixel = (byte2 >> 4) * 16;
      int gPixel = (byte2 & 15) * 16;

      //System.out.println(rPixel + "\t" + gPixel + "\t" + bPixel + "\t" + aPixel);

      pixels[i] = ((rPixel << 16) | (gPixel << 8) | bPixel | (aPixel << 24));
    }

    return new ImageResource(pixels, width, height);

  }

  /**
   **********************************************************************************************
   * Reads a BC1 (DXT1) image
   **********************************************************************************************
   **/
  public static ImageResource readBC1(FileManipulator fm, int width, int height) {
    return readDXT1(fm, width, height);
  }

  /**
   **********************************************************************************************
   * Reads a BC2 (DXT3) image
   **********************************************************************************************
   **/
  public static ImageResource readBC2(FileManipulator fm, int width, int height) {
    return readDXT3(fm, width, height);
  }

  /**
   **********************************************************************************************
   * Reads a BC3 (DXT5) image
   **********************************************************************************************
   **/
  public static ImageResource readBC3(FileManipulator fm, int width, int height) {
    return readDXT5(fm, width, height);
  }

  /**
   **********************************************************************************************
   Reads a BC4 (ATI1) Image
  
   Used the following resources for reference...
   java-dds --> DDSLineReader.java --> decodeATI()
   jsquish --> CompressorAlpha.java --> decompressAlphaDxt5()
   https://msdn.microsoft.com/en-us/library/bb694531(v=vs.85).aspx#BC5
   **********************************************************************************************
   **/
  public static ImageResource readBC4(FileManipulator fm, int width, int height) {

    // ensure width and height are multiples of 4...
    int heightMod = height % 4;
    if (heightMod != 0) {
      height += (4 - heightMod);
    }
    int widthMod = width % 4;
    if (widthMod != 0) {
      width += (4 - widthMod);
    }

    // X Bytes - Pixel Data
    int[] data = new int[width * height];

    for (int y = 0; y < height; y += 4) {
      // BC4 encodes 4x4 blocks of pixels
      for (int x = 0; x < width; x += 4) {

        // RED

        // two 8-bit reference colors (min and max)
        int red0 = ByteConverter.unsign(fm.readByte());
        int red1 = ByteConverter.unsign(fm.readByte());

        // red color lookup table
        int[] redLookup = new int[8];
        redLookup[0] = red0;
        redLookup[1] = red1;

        // work out the other red colors
        if (red0 > red1) {
          // 6 interpolated color values
          redLookup[2] = (6 * red0 + 1 * red1) / 7;
          redLookup[3] = (5 * red0 + 2 * red1) / 7;
          redLookup[4] = (4 * red0 + 3 * red1) / 7;
          redLookup[5] = (3 * red0 + 4 * red1) / 7;
          redLookup[6] = (2 * red0 + 5 * red1) / 7;
          redLookup[7] = (1 * red0 + 6 * red1) / 7;
        }
        else {
          // 4 interpolated color values
          redLookup[2] = (4 * red0 + 1 * red1) / 5;
          redLookup[3] = (3 * red0 + 2 * red1) / 5;
          redLookup[4] = (2 * red0 + 3 * red1) / 5;
          redLookup[5] = (1 * red0 + 4 * red1) / 5;
          redLookup[6] = 0;
          redLookup[7] = 255;
        }

        // now read 16 3-bit reds and convert them in to the lookup values
        int red3bytes1 = ByteConverter.unsign(fm.readByte()) | (ByteConverter.unsign(fm.readByte()) << 8) | (ByteConverter.unsign(fm.readByte()) << 16);
        int red3bytes2 = ByteConverter.unsign(fm.readByte()) | (ByteConverter.unsign(fm.readByte()) << 8) | (ByteConverter.unsign(fm.readByte()) << 16);

        int[] redColors = new int[16];
        for (int i = 0, j = 0; i < 8; ++i, j++) {
          int index = (red3bytes1 >> 3 * i) & 0x7;
          redColors[j] = redLookup[index];
        }
        for (int i = 0, j = 8; i < 8; ++i, j++) {
          int index = (red3bytes2 >> 3 * i) & 0x7;
          redColors[j] = redLookup[index];
        }

        // Now we have the Reds - need to use it as Green and Blue, as we write them out to the data array
        for (int by = 0; by < 4; ++by) {
          for (int bx = 0; bx < 4; ++bx) {
            int position = 4 * by + bx;
            int red = redColors[position];
            data[(y + by) * width + x + bx] = ((255 << 24) | (red << 16) | (red << 8) | red);// ARGB
          }
        }
      }
    }

    return new ImageResource(data, width, height);

  }

  /**
   **********************************************************************************************
   Reads a BC5 (ATI2) Image
  
   Used the following resources for reference...
   java-dds --> DDSLineReader.java --> decodeATI()
   jsquish --> CompressorAlpha.java --> decompressAlphaDxt5()
   https://msdn.microsoft.com/en-us/library/bb694531(v=vs.85).aspx#BC5
   **********************************************************************************************
   **/
  public static ImageResource readBC5(FileManipulator fm, int width, int height) {

    // ensure width and height are multiples of 4...
    int heightMod = height % 4;
    if (heightMod != 0) {
      height += (4 - heightMod);
    }
    int widthMod = width % 4;
    if (widthMod != 0) {
      width += (4 - widthMod);
    }

    // X Bytes - Pixel Data
    int[] data = new int[width * height];

    for (int y = 0; y < height; y += 4) {
      // BC5 encodes 4x4 blocks of pixels
      for (int x = 0; x < width; x += 4) {

        // RED

        // two 8-bit reference colors (min and max)
        int red0 = ByteConverter.unsign(fm.readByte());
        int red1 = ByteConverter.unsign(fm.readByte());

        // red color lookup table
        int[] redLookup = new int[8];
        redLookup[0] = red0;
        redLookup[1] = red1;

        // work out the other red colors
        if (red0 > red1) {
          // 6 interpolated color values
          redLookup[2] = (6 * red0 + 1 * red1) / 7;
          redLookup[3] = (5 * red0 + 2 * red1) / 7;
          redLookup[4] = (4 * red0 + 3 * red1) / 7;
          redLookup[5] = (3 * red0 + 4 * red1) / 7;
          redLookup[6] = (2 * red0 + 5 * red1) / 7;
          redLookup[7] = (1 * red0 + 6 * red1) / 7;
        }
        else {
          // 4 interpolated color values
          redLookup[2] = (4 * red0 + 1 * red1) / 5;
          redLookup[3] = (3 * red0 + 2 * red1) / 5;
          redLookup[4] = (2 * red0 + 3 * red1) / 5;
          redLookup[5] = (1 * red0 + 4 * red1) / 5;
          redLookup[6] = 0;
          redLookup[7] = 255;
        }

        // now read 16 3-bit reds and convert them in to the lookup values
        int red3bytes1 = ByteConverter.unsign(fm.readByte()) | (ByteConverter.unsign(fm.readByte()) << 8) | (ByteConverter.unsign(fm.readByte()) << 16);
        int red3bytes2 = ByteConverter.unsign(fm.readByte()) | (ByteConverter.unsign(fm.readByte()) << 8) | (ByteConverter.unsign(fm.readByte()) << 16);

        int[] redColors = new int[16];
        for (int i = 0, j = 0; i < 8; ++i, j++) {
          int index = (red3bytes1 >> 3 * i) & 0x7;
          redColors[j] = redLookup[index];
        }
        for (int i = 0, j = 8; i < 8; ++i, j++) {
          int index = (red3bytes2 >> 3 * i) & 0x7;
          redColors[j] = redLookup[index];
        }

        // GREEN

        // two 8-bit reference colors (min and max)
        int green0 = ByteConverter.unsign(fm.readByte());
        int green1 = ByteConverter.unsign(fm.readByte());

        // green color lookup table
        int[] greenLookup = new int[8];
        greenLookup[0] = green0;
        greenLookup[1] = green1;

        // work out the other green colors
        if (green0 > green1) {
          // 6 interpolated color values
          greenLookup[2] = (6 * green0 + 1 * green1) / 7;
          greenLookup[3] = (5 * green0 + 2 * green1) / 7;
          greenLookup[4] = (4 * green0 + 3 * green1) / 7;
          greenLookup[5] = (3 * green0 + 4 * green1) / 7;
          greenLookup[6] = (2 * green0 + 5 * green1) / 7;
          greenLookup[7] = (1 * green0 + 6 * green1) / 7;
        }
        else {
          // 4 interpolated color values
          greenLookup[2] = (4 * green0 + 1 * green1) / 5;
          greenLookup[3] = (3 * green0 + 2 * green1) / 5;
          greenLookup[4] = (2 * green0 + 3 * green1) / 5;
          greenLookup[5] = (1 * green0 + 4 * green1) / 5;
          greenLookup[6] = 0;
          greenLookup[7] = 255;
        }

        // now read 16 3-bit greens and convert them in to the lookup values
        int green3bytes1 = ByteConverter.unsign(fm.readByte()) | (ByteConverter.unsign(fm.readByte()) << 8) | (ByteConverter.unsign(fm.readByte()) << 16);
        int green3bytes2 = ByteConverter.unsign(fm.readByte()) | (ByteConverter.unsign(fm.readByte()) << 8) | (ByteConverter.unsign(fm.readByte()) << 16);

        int[] greenColors = new int[16];
        for (int i = 0, j = 0; i < 8; ++i, j++) {
          int index = (green3bytes1 >> 3 * i) & 0x7;
          greenColors[j] = greenLookup[index];
        }
        for (int i = 0, j = 8; i < 8; ++i, j++) {
          int index = (green3bytes2 >> 3 * i) & 0x7;
          greenColors[j] = greenLookup[index];
        }

        // Now we have the Reds and Greens - need to combine them to real color values, as we write them out to the data array
        for (int by = 0; by < 4; ++by) {
          for (int bx = 0; bx < 4; ++bx) {
            int position = 4 * by + bx;
            data[(y + by) * width + x + bx] = ((255 << 24) | (redColors[position] << 16) | (greenColors[position] << 8) | 255);// ARGB
          }
        }
      }
    }

    return new ImageResource(data, width, height);

  }

  /**
   **********************************************************************************************
   * Reads a BC7 Image
   **********************************************************************************************
   **/
  public static ImageResource readBC7(FileManipulator fm, int width, int height) {

    // ensure width and height are multiples of 4...
    int heightMod = height % 4;
    if (heightMod != 0) {
      height += (4 - heightMod);
    }
    int widthMod = width % 4;
    if (widthMod != 0) {
      width += (4 - widthMod);
    }

    // X Bytes - Pixel Data
    int numPixels = width * height;

    long remainingLength = fm.getRemainingLength();
    if (remainingLength < (numPixels / 2)) { // quick simple check to kill off some bad reads
      return null;
    }

    int[] data = new int[numPixels];

    NativeBC7Decomp bc7decomp = new NativeBC7Decomp();

    for (int y = 0; y < height; y += 4) {
      // DXT encodes 4x4 blocks of pixels
      for (int x = 0; x < width; x += 4) {

        byte[] blockData = fm.readBytes(16);
        int[] decodedPixels = bc7decomp.unpackBC7Block(blockData);

        int decodedPos = 0;

        for (int by = 0; by < 4; ++by) {
          for (int bx = 0; bx < 4; ++bx) {
            data[(y + by) * width + x + bx] = decodedPixels[decodedPos];
            decodedPos++;
          }
        }
      }
    }

    return new ImageResource(data, width, height);

  }

  /**
   **********************************************************************************************
   * Reads BGR888 Pixel Data
   **********************************************************************************************
   **/
  public static ImageResource readBGR(FileManipulator fm, int width, int height) {

    int numPixels = width * height;

    // X Bytes - Pixel Data
    int[] pixels = new int[numPixels];

    for (int i = 0; i < numPixels; i++) {
      int bPixel = ByteConverter.unsign(fm.readByte());
      int gPixel = ByteConverter.unsign(fm.readByte());
      int rPixel = ByteConverter.unsign(fm.readByte());
      int aPixel = 255;

      //pixels[i] = ((fm.readByte() << 16) | (fm.readByte() << 8) | fm.readByte() | (((byte) 255) << 24));
      pixels[i] = ((rPixel << 16) | (gPixel << 8) | bPixel | (aPixel << 24));
    }

    return new ImageResource(pixels, width, height);
  }

  /**
   **********************************************************************************************
   * Reads RGB565 Pixel Data
   **********************************************************************************************
   **/
  public static ImageResource readBGR565(FileManipulator fm, int width, int height) {

    int numPixels = width * height;

    // X Bytes - Pixel Data
    int[] pixels = new int[numPixels];

    for (int i = 0; i < numPixels; i++) {
      int pixel = ShortConverter.unsign(fm.readShort());

      int bPixel = ((pixel >> 11) & 31) * 8;
      int gPixel = ((pixel >> 5) & 63) * 4;
      int rPixel = (pixel & 31) * 8;
      int aPixel = 255;

      // OUTPUT = ARGB
      pixels[i] = ((rPixel << 16) | (gPixel << 8) | bPixel | (aPixel << 24));
    }

    return new ImageResource(pixels, width, height);
  }

  /**
   **********************************************************************************************
   * Reads RGB565 Pixel Data
   **********************************************************************************************
   **/
  public static ImageResource readBGR565BigEndian(FileManipulator fm, int width, int height) {

    int numPixels = width * height;

    // X Bytes - Pixel Data
    int[] pixels = new int[numPixels];

    for (int i = 0; i < numPixels; i++) {
      int pixel = ShortConverter.unsign(ShortConverter.changeFormat(fm.readShort()));

      int bPixel = ((pixel >> 11) & 31) * 8;
      int gPixel = ((pixel >> 5) & 63) * 4;
      int rPixel = (pixel & 31) * 8;
      int aPixel = 255;

      // OUTPUT = ARGB
      pixels[i] = ((rPixel << 16) | (gPixel << 8) | bPixel | (aPixel << 24));
    }

    return new ImageResource(pixels, width, height);
  }

  /**
   **********************************************************************************************
   * Reads BGRA Pixel Data
   * TODO UNTESTED!!!
   **********************************************************************************************
   **/
  public static ImageResource readBGRA(FileManipulator fm, int width, int height) {

    int numPixels = width * height;

    // X Bytes - Pixel Data
    int[] pixels = new int[numPixels];

    for (int i = 0; i < numPixels; i++) {
      // INPUT = BGRA
      int bPixel = ByteConverter.unsign(fm.readByte());
      int gPixel = ByteConverter.unsign(fm.readByte());
      int rPixel = ByteConverter.unsign(fm.readByte());
      int aPixel = ByteConverter.unsign(fm.readByte());

      // OUTPUT = ARGB
      pixels[i] = ((rPixel << 16) | (gPixel << 8) | bPixel | (aPixel << 24));
    }

    return new ImageResource(pixels, width, height);
  }

  /**
   **********************************************************************************************
   * Reads B4G4R4A4 Pixel Data
   * TODO UNTESTED!!!
   **********************************************************************************************
   **/
  public static ImageResource readBGRA4444(FileManipulator fm, int width, int height) {

    int numPixels = width * height;

    // X Bytes - Pixel Data
    int[] pixels = new int[numPixels];

    for (int i = 0; i < numPixels; i++) {
      int byte1 = ByteConverter.unsign(fm.readByte());
      int byte2 = ByteConverter.unsign(fm.readByte());

      int bPixel = (byte1 >> 4) * 16;
      int gPixel = (byte1 & 15) * 16;

      int rPixel = (byte2 >> 4) * 16;
      int aPixel = (byte2 & 15) * 16;

      //System.out.println(rPixel + "\t" + gPixel + "\t" + bPixel + "\t" + aPixel);

      pixels[i] = ((rPixel << 16) | (gPixel << 8) | bPixel | (aPixel << 24));
    }

    return new ImageResource(pixels, width, height);

  }

  /**
   **********************************************************************************************
   * Reads BGRA5551 Pixel Data
   **********************************************************************************************
   **/
  public static ImageResource readBGRA5551(FileManipulator fm, int width, int height) {

    int numPixels = width * height;

    // X Bytes - Pixel Data
    int[] pixels = new int[numPixels];

    for (int i = 0; i < numPixels; i++) {
      int byte1 = ByteConverter.unsign(fm.readByte());
      int byte2 = ByteConverter.unsign(fm.readByte());

      int b = ((byte2 & 248) >> 3) * 8;
      int g = (((byte2 & 7) << 3) | ((byte1 & 192) >> 6)) * 8;
      int r = (byte1 & 63) * 8;
      int a = (byte2 >> 7) * 255;

      // OUTPUT = ARGB
      pixels[i] = ((r << 16) | (g << 8) | b | (a << 24));
    }

    return new ImageResource(pixels, width, height);
  }

  /**
   **********************************************************************************************
   * Reads BGRA5551 Pixel Data
   **********************************************************************************************
   **/
  public static ImageResource readBGRA5551BigEndian(FileManipulator fm, int width, int height) {

    int numPixels = width * height;

    // X Bytes - Pixel Data
    int[] pixels = new int[numPixels];

    for (int i = 0; i < numPixels; i++) {
      int byte2 = ByteConverter.unsign(fm.readByte()); // swapped
      int byte1 = ByteConverter.unsign(fm.readByte()); // swapped

      int b = ((byte2 & 248) >> 3) * 8;
      int g = (((byte2 & 7) << 3) | ((byte1 & 192) >> 6)) * 8;
      int r = (byte1 & 63) * 8;
      int a = (byte2 >> 7) * 255;

      // OUTPUT = ARGB
      pixels[i] = ((r << 16) | (g << 8) | b | (a << 24));
    }

    return new ImageResource(pixels, width, height);
  }

  /**
   **********************************************************************************************
   * Reads a DXT image
   **********************************************************************************************
   **/
  public static ImageResource readDXT(FileManipulator fm, int width, int height, int format) {

    // ensure width and height are multiples of 4...
    int heightMod = height % 4;
    if (heightMod != 0) {
      height += (4 - heightMod);
    }
    int widthMod = width % 4;
    if (widthMod != 0) {
      width += (4 - widthMod);
    }

    // X Bytes - Pixel Data
    int numPixels = width * height;

    long remainingLength = fm.getRemainingLength();
    if (remainingLength < ((numPixels / 2) - 256)) { // quick simple check to kill off some bad reads (adds 256 bytes as a little tolerance)
      return null;
    }

    int[] data = new int[numPixels];

    for (int y = 0; y < height; y += 4) {
      // DXT encodes 4x4 blocks of pixels
      for (int x = 0; x < width; x += 4) {

        int[] alphaMap = null;

        // skip the alpha data
        if (format == 3) {
          fm.skip(8);
        }
        else if (format == 5) {
          int[] alpha = new int[8];

          // creating alpha table
          alpha[0] = ByteConverter.unsign(fm.readByte());
          alpha[1] = ByteConverter.unsign(fm.readByte());
          if (alpha[0] > alpha[1]) {
            alpha[2] = (6 * alpha[0] + alpha[1]) / 7;
            alpha[3] = (5 * alpha[0] + 2 * alpha[1]) / 7;
            alpha[4] = (4 * alpha[0] + 3 * alpha[1]) / 7;
            alpha[5] = (3 * alpha[0] + 4 * alpha[1]) / 7;
            alpha[6] = (2 * alpha[0] + 5 * alpha[1]) / 7;
            alpha[7] = (alpha[0] + 6 * alpha[1]) / 7;
          }
          else {
            alpha[2] = (4 * alpha[0] + alpha[1]) / 5;
            alpha[3] = (3 * alpha[0] + 2 * alpha[1]) / 5;
            alpha[4] = (2 * alpha[0] + 3 * alpha[1]) / 5;
            alpha[5] = (alpha[0] + 4 * alpha[1]) / 5;
            alpha[6] = 0;
            alpha[7] = 255;
          }

          alphaMap = new int[16];

          // first 3 bytes of alpha
          int bits = ByteConverter.unsign(fm.readByte()) | (ByteConverter.unsign(fm.readByte()) << 8) | (ByteConverter.unsign(fm.readByte()) << 16);
          for (int bi = 0; bi < 8; bi++) {
            alphaMap[bi] = alpha[(bits & 7)];
            bits >>= 3;
          }

          // second 3 bytes of alpha
          bits = ByteConverter.unsign(fm.readByte()) | (ByteConverter.unsign(fm.readByte()) << 8) | (ByteConverter.unsign(fm.readByte()) << 16);
          for (int bi = 8; bi < 16; bi++) {
            alphaMap[bi] = alpha[(bits & 7)];
            bits >>= 3;
          }

        }

        // decode the DXT1/DXT3 RGB data

        // two 16 bit encoded colors (red 5 bits, green 6 bits, blue 5 bits)
        int c1packed16 = ByteConverter.unsign(fm.readByte()) | (ByteConverter.unsign(fm.readByte()) << 8);
        int c2packed16 = ByteConverter.unsign(fm.readByte()) | (ByteConverter.unsign(fm.readByte()) << 8);

        // separate the R,G,B values
        int color1r = (c1packed16 >> 8) & 0xF8;
        int color1g = (c1packed16 >> 3) & 0xFC;
        int color1b = (c1packed16 << 3) & 0xF8;

        int color2r = (c2packed16 >> 8) & 0xF8;
        int color2g = (c2packed16 >> 3) & 0xFC;
        int color2b = (c2packed16 << 3) & 0xF8;

        int colors[] = new int[8]; // color table for all possible codes
        // colors 0 and 1 point to the two 16 bit colors we read in
        colors[0] = (color1r << 16) | (color1g << 8) | color1b | 0xFF000000;
        colors[1] = (color2r << 16) | (color2g << 8) | color2b | 0xFF000000;

        // 2/3 Color1, 1/3 color2
        int colorr = (((color1r << 1) + color2r) / 3);// & 0xFF;
        int colorg = (((color1g << 1) + color2g) / 3);// & 0xFF;
        int colorb = (((color1b << 1) + color2b) / 3);// & 0xFF;
        colors[2] = (colorr << 16) | (colorg << 8) | colorb | 0xFF000000;

        // 2/3 Color2, 1/3 color1
        colorr = (((color2r << 1) + color1r) / 3);// & 0xFF;
        colorg = (((color2g << 1) + color1g) / 3);// & 0xFF;
        colorb = (((color2b << 1) + color1b) / 3);// & 0xFF;
        colors[3] = (colorr << 16) | (colorg << 8) | colorb | 0xFF000000;

        // read in the color code bits, 16 values, each 2 bits long
        // then look up the color in the color table we built
        //int bits = ByteConverter.unsign(fm.readByte()) + (ByteConverter.unsign(fm.readByte()) << 8) + (ByteConverter.unsign(fm.readByte()) << 16) + (ByteConverter.unsign(fm.readByte()) << 24);
        int bits = ByteConverter.unsign(fm.readByte()) | (ByteConverter.unsign(fm.readByte()) << 8) | (ByteConverter.unsign(fm.readByte()) << 16) | (ByteConverter.unsign(fm.readByte()) << 24);

        if (format == 5) {
          int alphaPos = 0;
          for (int by = 0; by < 4; ++by) {
            for (int bx = 0; bx < 4; ++bx) {
              int code = (bits >> (((by << 2) + bx) << 1)) & 0x3;
              data[(y + by) * width + x + bx] = ((colors[code] & 0xFFFFFF) | (alphaMap[alphaPos++] << 24));
            }
          }
        }
        else {
          for (int by = 0; by < 4; ++by) {
            for (int bx = 0; bx < 4; ++bx) {
              int code = (bits >> (((by << 2) + bx) << 1)) & 0x3;
              data[(y + by) * width + x + bx] = colors[code];
            }
          }
        }
      }
    }

    return new ImageResource(data, width, height);

  }

  /**
   **********************************************************************************************
   Reads a Wii CMPR Format (similar to DXT1), but in blocks of 4x4 which are themselves within blocks of 8x8
   // NOT TESTED
   **********************************************************************************************
   **/
  public static ImageResource readCMPR(FileManipulator fm, int width, int height) {

    int originalHeight = height;

    // ensure width and height are multiples of 8...
    int heightMod = height % 8;
    if (heightMod != 0) {
      height += (8 - heightMod);
    }
    int widthMod = width % 8;
    if (widthMod != 0) {
      width += (8 - widthMod);
    }

    // X Bytes - Pixel Data
    int numPixels = width * height;

    long remainingLength = fm.getRemainingLength();
    if (remainingLength < ((numPixels / 2) - 256)) { // quick simple check to kill off some bad reads (adds 256 bytes as a little tolerance)
      return null;
    }

    int[] data = new int[numPixels];

    for (int y1 = 0; y1 < height; y1 += 8) {
      // CMPR encodes 8x8 blocks of pixels, which are 4x 4x4 blocks
      for (int x1 = 0; x1 < width; x1 += 8) {

        for (int y = 0; y < 8; y += 4) {
          // DXT encodes 4x4 blocks of pixels
          for (int x = 0; x < 8; x += 4) {

            // decode the DXT1 RGB data

            // two 16 bit encoded colors (red 5 bits, green 6 bits, blue 5 bits)
            /*
            int c1packed16 = ByteConverter.unsign(fm.readByte()) | (ByteConverter.unsign(fm.readByte()) << 8);
            int c2packed16 = ByteConverter.unsign(fm.readByte()) | (ByteConverter.unsign(fm.readByte()) << 8);
            
            // separate the R,G,B values (RRRRRGGGGGGBBBBB)
            int color1r = (c1packed16 >> 8) & 0xF8;
            int color1g = (c1packed16 >> 3) & 0xFC;
            int color1b = (c1packed16 << 3) & 0xF8;
            
            int color2r = (c2packed16 >> 8) & 0xF8;
            int color2g = (c2packed16 >> 3) & 0xFC;
            int color2b = (c2packed16 << 3) & 0xF8;
            */

            // two 16 bit encoded colors (RRRRRGGGGGGBBBBB)
            int c1packed16 = (ByteConverter.unsign(fm.readByte()) << 8) | ByteConverter.unsign(fm.readByte());
            int c2packed16 = (ByteConverter.unsign(fm.readByte()) << 8) | ByteConverter.unsign(fm.readByte());

            // separate the R,G,B values
            int color1r = ((c1packed16 >> 11) & 31) * 8;
            int color1g = ((c1packed16 >> 5) & 63) * 4;
            int color1b = (c1packed16 & 31) * 8;

            int color2r = ((c2packed16 >> 11) & 31) * 8;
            int color2g = ((c2packed16 >> 5) & 63) * 4;
            int color2b = (c2packed16 & 31) * 8;

            int colors[] = new int[8]; // color table for all possible codes
            // colors 0 and 1 point to the two 16 bit colors we read in
            colors[0] = (color1r << 16) | (color1g << 8) | color1b | 0xFF000000;
            colors[1] = (color2r << 16) | (color2g << 8) | color2b | 0xFF000000;

            // WII difference
            if (colors[0] > colors[1]) {
              // 2x 2/3 colors

              // 2/3 Color1, 1/3 color2
              int colorr = (((color1r << 1) + color2r) / 3);// & 0xFF;
              int colorg = (((color1g << 1) + color2g) / 3);// & 0xFF;
              int colorb = (((color1b << 1) + color2b) / 3);// & 0xFF;
              colors[2] = (colorr << 16) | (colorg << 8) | colorb | 0xFF000000;

              // 2/3 Color2, 1/3 color1
              colorr = (((color2r << 1) + color1r) / 3);// & 0xFF;
              colorg = (((color2g << 1) + color1g) / 3);// & 0xFF;
              colorb = (((color2b << 1) + color1b) / 3);// & 0xFF;
              colors[3] = (colorr << 16) | (colorg << 8) | colorb | 0xFF000000;

            }
            else {
              // 1x 1/2 color, 1x transparent

              int colorr = ((color1r + color2r) >> 1);
              int colorg = ((color1g + color2g) >> 1);
              int colorb = ((color1b + color2b) >> 1);
              colors[2] = (colorr << 16) | (colorg << 8) | colorb | 0xFF000000;

              colors[3] = 0; // transparent
            }

            // read in the color code bits, 16 values, each 2 bits long
            // then look up the color in the color table we built
            //int bits = ByteConverter.unsign(fm.readByte()) + (ByteConverter.unsign(fm.readByte()) << 8) + (ByteConverter.unsign(fm.readByte()) << 16) + (ByteConverter.unsign(fm.readByte()) << 24);
            int bits = ByteConverter.unsign(fm.readByte()) | (ByteConverter.unsign(fm.readByte()) << 8) | (ByteConverter.unsign(fm.readByte()) << 16) | (ByteConverter.unsign(fm.readByte()) << 24);

            for (int by = 0; by < 4; ++by) {
              for (int bx = 0; bx < 4; ++bx) {
                int code = (bits >> (((by << 2) + bx) << 1)) & 0x3;
                //data[(y + by) * width + x + bx] = colors[code];
                //int writePos = ((y1+y+by)*width) + (x1+x+bx);
                int writePos = ((y1 + y + by) * width) + (x1 + x + (3 - bx));
                data[writePos] = colors[code];
              }
            }
          }
        }

      }
    }

    if (originalHeight <= 4) {
      height = 4;
    }

    return new ImageResource(data, width, height);

  }

  /**
   **********************************************************************************************
   * Reads a DXT1 image
   **********************************************************************************************
   **/
  public static ImageResource readDXT1(FileManipulator fm, int width, int height) {
    return readDXT(fm, width, height, 1);
  }

  /**
   **********************************************************************************************
   * Reads a DXT1 with Alpha image
   **********************************************************************************************
   **/
  public static ImageResource readDX1A(FileManipulator fm, int width, int height) {
    // ensure width and height are multiples of 4...
    int heightMod = height % 4;
    if (heightMod != 0) {
      height += (4 - heightMod);
    }
    int widthMod = width % 4;
    if (widthMod != 0) {
      width += (4 - widthMod);
    }

    // X Bytes - Pixel Data
    int[] data = new int[width * height];

    for (int y = 0; y < height; y += 4) {
      // DXT encodes 4x4 blocks of pixels
      for (int x = 0; x < width; x += 4) {

        // decode the DXT1 RGB data

        // two 16 bit encoded colors (red 5 bits, green 6 bits, blue 5 bits)
        int c1packed16 = ByteConverter.unsign(fm.readByte()) | (ByteConverter.unsign(fm.readByte()) << 8);
        int c2packed16 = ByteConverter.unsign(fm.readByte()) | (ByteConverter.unsign(fm.readByte()) << 8);

        // separate the R,G,B values
        int color1r = (c1packed16 >> 8) & 0xF8;
        int color1g = (c1packed16 >> 3) & 0xFC;
        int color1b = (c1packed16 << 3) & 0xF8;

        int color2r = (c2packed16 >> 8) & 0xF8;
        int color2g = (c2packed16 >> 3) & 0xFC;
        int color2b = (c2packed16 << 3) & 0xF8;

        int colors[] = new int[8]; // color table for all possible codes
        // colors 0 and 1 point to the two 16 bit colors we read in
        colors[0] = (color1r << 16) | (color1g << 8) | color1b | 0xFF000000;
        colors[1] = (color2r << 16) | (color2g << 8) | color2b | 0xFF000000;

        // 2/3 Color1, 1/3 color2
        int colorr = (((color1r << 1) + color2r) / 3);// & 0xFF;
        int colorg = (((color1g << 1) + color2g) / 3);// & 0xFF;
        int colorb = (((color1b << 1) + color2b) / 3);// & 0xFF;
        if (colorr == 0 && colorg == 0 && colorb == 0) {
          // alpha black
          colors[2] = 0;
        }
        else {
          // normal color
          colors[2] = (colorr << 16) | (colorg << 8) | colorb | 0xFF000000;
        }

        // 2/3 Color2, 1/3 color1
        colorr = (((color2r << 1) + color1r) / 3);// & 0xFF;
        colorg = (((color2g << 1) + color1g) / 3);// & 0xFF;
        colorb = (((color2b << 1) + color1b) / 3);// & 0xFF;
        if (colorr == 0 && colorg == 0 && colorb == 0) {
          // alpha black
          colors[3] = 0;
        }
        else {
          // normal color
          colors[3] = (colorr << 16) | (colorg << 8) | colorb | 0xFF000000;
        }

        // read in the color code bits, 16 values, each 2 bits long
        // then look up the color in the color table we built
        //int bits = ByteConverter.unsign(fm.readByte()) + (ByteConverter.unsign(fm.readByte()) << 8) + (ByteConverter.unsign(fm.readByte()) << 16) + (ByteConverter.unsign(fm.readByte()) << 24);
        int bits = ByteConverter.unsign(fm.readByte()) | (ByteConverter.unsign(fm.readByte()) << 8) | (ByteConverter.unsign(fm.readByte()) << 16) | (ByteConverter.unsign(fm.readByte()) << 24);

        for (int by = 0; by < 4; ++by) {
          for (int bx = 0; bx < 4; ++bx) {
            int code = (bits >> (((by << 2) + bx) << 1)) & 0x3;
            data[(y + by) * width + x + bx] = colors[code];
          }
        }
      }
    }

    return new ImageResource(data, width, height);
  }

  /**
   **********************************************************************************************
   * Reads DXT1 CRUNCHED file data
   * Calls "Crunch" to convert the compressed data into a DDS image
   **********************************************************************************************
   **/
  public static ImageResource readDXT1Crunched(FileManipulator fm, int width, int height) {
    try {

      //
      // STEP 1
      // Check that Crunch is found
      //

      String crunchPath = Settings.getString("Crunch_Path");

      File crunchFile = new File(crunchPath);

      if (crunchFile.exists() && crunchFile.isDirectory()) {
        // Path is a directory, append the filename to it
        crunchPath = crunchPath + File.separatorChar + "crunch_x64.exe";
        crunchFile = new File(crunchPath);
      }

      if (!crunchFile.exists()) {
        // crunch path is invalid
        ErrorLogger.log("Crunch can't be found at the path " + crunchFile.getAbsolutePath());
        return null;
      }

      crunchPath = crunchFile.getAbsolutePath();

      //
      // STEP 2
      // Dump all the compressed image bytes to a file
      //
      Resource selected = (Resource) SingletonManager.get("CurrentResource");
      if (selected == null) {
        return null;
      }
      /*
      File exportedPath = selected.getExportedPath();
      if (exportedPath.equals(selected.getSource()){
        // the file hasn't been exported - reading from the real archive
      }
      else {
        // the file has been exported already - try to simply rename it to "*.crn" rather than exporting it again
        fm.close();
        boolean fileRenamed = false;
        try {
          File crnFile = new File(exportedPath.getAbsolutePath() + ".crn");
          fileRenamed = exportedPath.renameTo(crnFile);
          if (fileRenamed){
            exportedPath = crnFile;
          }
          else {
            exportedPath = null;
          }
        }
        catch (Throwable t){
          exportedPath = null;
        }
      
        if (exportedPath == null){
          fm = new FileManipulator()
        }
      }
      */

      int numBytes = (int) selected.getDecompressedLength();

      // Create a temporary file for the compressed image bytes
      // NOTE THE FILE EXTENSION *.crn IS VERY IMPORTANT OR CRUNCH WON'T WORK
      String tempFilePath = new File(Settings.get("TempDirectory")).getAbsolutePath();
      tempFilePath += File.separator + "crunch_" + System.currentTimeMillis() + ".ge_temp.crn"; // System.currentTimeMillis() to make it a unique filename
      File tempFile = new File(tempFilePath);
      if (tempFile.exists()) {
        tempFile.delete();
      }
      //tempFile = FilenameChecker.correctFilename(tempFile); // removes funny characters etc.

      // write the compressed image bytes
      FileManipulator tempFM = new FileManipulator(tempFile, true);
      tempFM.writeBytes(fm.readBytes(numBytes));
      tempFM.close();

      // get the absolute path to the temporary file of compressed image bytes
      tempFilePath = tempFile.getAbsolutePath();

      // Create a temporary file for the UNcompressed image bytes

      String tempOutFilePath = tempFilePath + ".dds";
      File tempOutFile = new File(tempOutFilePath);
      if (tempOutFile.exists()) {
        tempOutFile.delete();
      }
      tempOutFilePath = tempOutFile.getAbsolutePath();

      //
      // STEP 3
      // Run Crunch to convert the compressed image data to a DDS file
      //

      ProcessBuilder pb = new ProcessBuilder(crunchPath, "-quiet", "-out", tempOutFilePath, "-file", tempFilePath);
      Process convertProcess = pb.start();
      int returnCode = convertProcess.waitFor(); // wait for Crunch to finish
      if (returnCode == 0) {
        // successful decompression
        //Thread.sleep(1000); // if we try to read the file too quickly, it doesn't exist yet!
        if (!tempOutFile.exists()) {
          ErrorLogger.log("Crunch failed to run the conversion script for the image");
          return null;
        }
      }
      else {
        ErrorLogger.log("Crunch had an error processing the file " + tempFilePath);
        return null;
      }

      //
      // STEP 4
      // Read the DDS data from the output file
      //
      if (!tempOutFile.exists()) {
        ErrorLogger.log("Crunch ran the conversion, but the file is missing: " + tempOutFile);
        return null;
      }

      FileManipulator convertedFM = new FileManipulator(tempOutFile, false);
      ImageResource imageResource = new Viewer_DDS_DDS().readThumbnail(convertedFM);
      convertedFM.close();

      return imageResource;

    }
    catch (Throwable t) {
      ErrorLogger.log(t);
      return null;
    }
  }

  /**
   **********************************************************************************************
   * Reads DXT5 CRUNCHED file data
   * Calls "Crunch" to convert the compressed data into a DDS image
   **********************************************************************************************
   **/
  public static ImageResource readDXT5Crunched(FileManipulator fm, int width, int height, int numCompressedBytes) {
    return readDXT1Crunched(fm, width, height, numCompressedBytes);
  }

  /**
   **********************************************************************************************
   * Reads DXT1 CRUNCHED file data
   * Calls "Crunch" to convert the compressed data into a DDS image
   **********************************************************************************************
   **/
  public static ImageResource readDXT1Crunched(FileManipulator fm, int width, int height, int numCompressedBytes) {
    try {

      //
      // STEP 1
      // Check that Crunch is found
      //

      String crunchPath = Settings.getString("Crunch_Path");

      File crunchFile = new File(crunchPath);

      if (crunchFile.exists() && crunchFile.isDirectory()) {
        // Path is a directory, append the filename to it
        crunchPath = crunchPath + File.separatorChar + "crunch_x64.exe";
        crunchFile = new File(crunchPath);
      }

      if (!crunchFile.exists()) {
        // crunch path is invalid
        ErrorLogger.log("Crunch can't be found at the path " + crunchFile.getAbsolutePath());
        return null;
      }

      crunchPath = crunchFile.getAbsolutePath();

      //
      // STEP 2
      // Dump all the compressed image bytes to a file
      //

      // Create a temporary file for the compressed image bytes
      // NOTE THE FILE EXTENSION *.crn IS VERY IMPORTANT OR CRUNCH WON'T WORK
      String tempFilePath = new File(Settings.get("TempDirectory")).getAbsolutePath();
      tempFilePath += File.separator + "crunch_" + System.currentTimeMillis() + ".ge_temp.crn"; // System.currentTimeMillis() to make it a unique filename
      File tempFile = new File(tempFilePath);
      if (tempFile.exists()) {
        tempFile.delete();
      }
      //tempFile = FilenameChecker.correctFilename(tempFile); // removes funny characters etc.

      // write the compressed image bytes
      FileManipulator tempFM = new FileManipulator(tempFile, true);
      tempFM.writeBytes(fm.readBytes(numCompressedBytes));
      tempFM.close();

      // get the absolute path to the temporary file of compressed image bytes
      tempFilePath = tempFile.getAbsolutePath();

      // Create a temporary file for the UNcompressed image bytes

      String tempOutFilePath = tempFilePath + ".dds";
      File tempOutFile = new File(tempOutFilePath);
      if (tempOutFile.exists()) {
        tempOutFile.delete();
      }
      tempOutFilePath = tempOutFile.getAbsolutePath();

      //
      // STEP 3
      // Run Crunch to convert the compressed image data to a DDS file
      //

      ProcessBuilder pb = new ProcessBuilder(crunchPath, "-quiet", "-out", tempOutFilePath, "-file", tempFilePath);
      Process convertProcess = pb.start();
      int returnCode = convertProcess.waitFor(); // wait for Crunch to finish
      if (returnCode == 0) {
        // successful decompression
        //Thread.sleep(1000); // if we try to read the file too quickly, it doesn't exist yet!
        if (!tempOutFile.exists()) {
          ErrorLogger.log("Crunch failed to run the conversion script for the image");
          return null;
        }
      }
      else {
        ErrorLogger.log("Crunch had an error processing the file " + tempFilePath);
        return null;
      }

      //
      // STEP 4
      // Read the DDS data from the output file
      //
      if (!tempOutFile.exists()) {
        ErrorLogger.log("Crunch ran the conversion, but the file is missing: " + tempOutFile);
        return null;
      }

      FileManipulator convertedFM = new FileManipulator(tempOutFile, false);
      ImageResource imageResource = new Viewer_DDS_DDS().readThumbnail(convertedFM);
      convertedFM.close();

      return imageResource;

    }
    catch (Throwable t) {
      ErrorLogger.log(t);
      return null;
    }
  }

  /**
   **********************************************************************************************
   * Reads a DXT3 image
   **********************************************************************************************
   **/
  public static ImageResource readDXT3(FileManipulator fm, int width, int height) {
    return readDXT(fm, width, height, 3);
  }

  /**
   **********************************************************************************************
   * Reads a DXT5 image
   **********************************************************************************************
   **/
  public static ImageResource readDXT5(FileManipulator fm, int width, int height) {
    return readDXT(fm, width, height, 5);
  }

  /**
   **********************************************************************************************
   * Reads DXT5 CRUNCHED file data
   * Calls "Crunch" to convert the compressed data into a DDS image
   **********************************************************************************************
   **/
  public static ImageResource readDXT5Crunched(FileManipulator fm, int width, int height) {
    return readDXT1Crunched(fm, width, height);
  }

  /**
   **********************************************************************************************
   * Reads a DXT5 image
   **********************************************************************************************
   **/
  public static ImageResource readDXT5Swizzled(FileManipulator fm, int width, int height) {
    return readDXTSwizzled(fm, width, height, 5);
  }

  /**
   **********************************************************************************************
   * Reads a DXT5 image (big endian)
   **********************************************************************************************
   **/
  public static ImageResource readDXT5BigEndian(FileManipulator fm, int width, int height, int blockSize) {
    return readDXTBigEndian(fm, width, height, 5, blockSize);
  }

  /**
   **********************************************************************************************
   * Reads a DXT3 image (big endian)
   **********************************************************************************************
   **/
  public static ImageResource readDXT3BigEndian(FileManipulator fm, int width, int height, int blockSize) {
    return readDXTBigEndian(fm, width, height, 3, blockSize);
  }

  /**
   **********************************************************************************************
   * Reads a DXT1 image (big endian)
   **********************************************************************************************
   **/
  public static ImageResource readDXT1BigEndian(FileManipulator fm, int width, int height, int blockSize) {
    return readDXTBigEndian(fm, width, height, 1, blockSize);
  }

  /**
   **********************************************************************************************
   * Reads a DXT image that has swizzled data bytes
   **********************************************************************************************
   **/
  public static ImageResource readDXTSwizzled(FileManipulator fm, int width, int height, int format) {

    // read the bytes
    int numBytes = width * height;
    byte[] bytes = fm.readBytes(numBytes);

    // deswizzle them
    byte[] outBytes = unswizzle(bytes, width, height, 16);

    /*
    FileManipulator testout = new FileManipulator(new File("c:\\out_java.txt"), true);
    testout.writeBytes(outBytes);
    testout.close();
    */

    fm.close();

    // feed those bytes into a normal DXT reader
    fm = new FileManipulator(new ByteBuffer(outBytes));
    ImageResource imageResource = readDXT(fm, width, height, format);
    fm.close();

    /*
    int[] pixels = imageResource.getPixels();
    int numPixels = pixels.length;
    FileManipulator testout = new FileManipulator(new File("c:\\out_java.txt"), true);
    for (int i = 0; i < numPixels; i++) {
      ColorSplitAlpha split = new ColorSplitAlpha(pixels[i]);
      testout.writeByte(split.getRed());
      testout.writeByte(split.getGreen());
      testout.writeByte(split.getBlue());
      testout.writeByte(split.getAlpha());
    }
    testout.close();
    */

    return imageResource;
  }

  /**
   **********************************************************************************************
   * Reads a DXT image that has big endian blocks of data
   **********************************************************************************************
   **/
  public static ImageResource readDXTBigEndian(FileManipulator fm, int width, int height, int format, int blockSize) {

    // ensure width and height are multiples of 4...
    int heightMod = height % 4;
    if (heightMod != 0) {
      height += (4 - heightMod);
    }
    int widthMod = width % 4;
    if (widthMod != 0) {
      width += (4 - widthMod);
    }

    // read the bytes
    int numBytes = width * height;
    byte[] bytes = fm.readBytes(numBytes);

    // change the ordering (blocks of 4/8/16 bytes)
    byte[] outBytes = new byte[numBytes];
    if (blockSize == 4) {
      for (int i = 0; i < numBytes; i += 4) {
        outBytes[i] = bytes[i + 3];
        outBytes[i + 1] = bytes[i + 2];
        outBytes[i + 2] = bytes[i + 1];
        outBytes[i + 3] = bytes[i];
      }
    }
    else if (blockSize == 8) {
      for (int i = 0; i < numBytes; i += 8) {
        outBytes[i] = bytes[i + 7];
        outBytes[i + 1] = bytes[i + 6];
        outBytes[i + 2] = bytes[i + 5];
        outBytes[i + 3] = bytes[i + 4];
        outBytes[i + 4] = bytes[i + 3];
        outBytes[i + 5] = bytes[i + 2];
        outBytes[i + 6] = bytes[i + 1];
        outBytes[i + 7] = bytes[i];
      }
    }
    else if (blockSize == 16) {
      for (int i = 0; i < numBytes; i += 16) {
        outBytes[i] = bytes[i + 15];
        outBytes[i + 1] = bytes[i + 14];
        outBytes[i + 2] = bytes[i + 13];
        outBytes[i + 3] = bytes[i + 12];
        outBytes[i + 4] = bytes[i + 11];
        outBytes[i + 5] = bytes[i + 10];
        outBytes[i + 6] = bytes[i + 9];
        outBytes[i + 7] = bytes[i + 8];
        outBytes[i + 8] = bytes[i + 7];
        outBytes[i + 9] = bytes[i + 6];
        outBytes[i + 10] = bytes[i + 5];
        outBytes[i + 11] = bytes[i + 4];
        outBytes[i + 12] = bytes[i + 3];
        outBytes[i + 13] = bytes[i + 2];
        outBytes[i + 14] = bytes[i + 1];
        outBytes[i + 15] = bytes[i];
      }
    }

    fm.close();

    // feed those bytes into a normal DXT reader
    fm = new FileManipulator(new ByteBuffer(outBytes));
    ImageResource imageResource = readDXT(fm, width, height, format);
    fm.close();

    return imageResource;
  }

  /**
   **********************************************************************************************
   * Reads an ETC2
   **********************************************************************************************
   **/
  public static ImageResource readETC2_RGBA8(FileManipulator fm, int width, int height) {

    // ensure width and height are multiples of 4...
    int heightMod = height % 4;
    if (heightMod != 0) {
      height += (4 - heightMod);
    }
    int widthMod = width % 4;
    if (widthMod != 0) {
      width += (4 - widthMod);
    }

    ETC2Reader reader = new ETC2Reader();

    // X Bytes - Pixel Data
    int[] data = new int[width * height];

    for (int y = 0; y < height; y += 4) {
      // DXT encodes 4x4 blocks of pixels
      for (int x = 0; x < width; x += 4) {

        int[] blockIn = new int[16];
        int[] blockOut = new int[64];

        for (int i = 0; i < 16; i++) {
          blockIn[i] = ByteConverter.unsign(fm.readByte());
        }

        reader.detexDecompressBlockETC2(blockIn, reader.DETEX_MODE_MASK_ALL, 0, blockOut);

        int readPos = 0;
        for (int by = 0; by < 4; ++by) {
          for (int bx = 0; bx < 4; ++bx) {
            data[(y + by) * width + x + bx] = blockOut[readPos++];
          }
        }
      }
    }

    return new ImageResource(data, width, height);

  }

  /**
   **********************************************************************************************
   * Reads GR Pixel Data, where each color is 16 bits in size
   **********************************************************************************************
   **/
  public static ImageResource readG16R16(FileManipulator fm, int width, int height) {

    int numPixels = width * height;

    // X Bytes - Pixel Data
    int[] pixels = new int[numPixels];

    for (int i = 0; i < numPixels; i++) {
      int gPixel = (ShortConverter.unsign(fm.readShort()) / 2);
      int rPixel = (ShortConverter.unsign(fm.readShort()) / 2);
      int bPixel = 0;
      int aPixel = 255;

      //pixels[i] = ((fm.readByte() << 16) | (fm.readByte() << 8) | fm.readByte() | (((byte) 255) << 24));
      pixels[i] = ((rPixel << 16) | (gPixel << 8) | bPixel | (aPixel << 24));
    }

    return new ImageResource(pixels, width, height);
  }

  /**
   **********************************************************************************************
   * Reads G4B4A4R4 Pixel Data
   * TODO UNTESTED!!!
   **********************************************************************************************
   **/
  public static ImageResource readGBAR4444(FileManipulator fm, int width, int height) {

    int numPixels = width * height;

    // X Bytes - Pixel Data
    int[] pixels = new int[numPixels];

    for (int i = 0; i < numPixels; i++) {
      int byte1 = ByteConverter.unsign(fm.readByte());
      int byte2 = ByteConverter.unsign(fm.readByte());

      int gPixel = (byte1 >> 4) * 16;
      int bPixel = (byte1 & 15) * 16;

      int aPixel = (byte2 >> 4) * 16;
      int rPixel = (byte2 & 15) * 16;

      pixels[i] = ((rPixel << 16) | (gPixel << 8) | bPixel | (aPixel << 24));
    }

    return new ImageResource(pixels, width, height);

  }

  /**
   **********************************************************************************************
   * Reads Luminance-8 Alpha-8 pixel data
   **********************************************************************************************
   **/
  public static ImageResource readL8A8(FileManipulator fm, int width, int height) {

    int numPixels = width * height;
    int[] pixels = new int[numPixels];

    for (int i = 0; i < numPixels; i++) {
      int luminence = ByteConverter.unsign(fm.readByte());
      int alpha = ByteConverter.unsign(fm.readByte());

      pixels[i] = ((luminence << 16) | (luminence << 8) | luminence | (alpha << 24));
    }

    return new ImageResource(pixels, width, height);
  }

  /**
   **********************************************************************************************
   * Reads a color palette in RGBA format
   **********************************************************************************************
   **/
  public static int[] readPaletteBGRA(FileManipulator fm, int colorCount) {

    int[] palette = new int[colorCount];

    for (int i = 0; i < colorCount; i++) {
      // INPUT = BGRA
      // OUTPUT = ARGB
      palette[i] = (ByteConverter.unsign(fm.readByte()) | (ByteConverter.unsign(fm.readByte()) << 8) | (ByteConverter.unsign(fm.readByte()) << 16) | (ByteConverter.unsign(fm.readByte()) << 24));
    }

    return palette;
  }

  /**
   **********************************************************************************************
   * Reads a color palette in RGBA format
   **********************************************************************************************
   **/
  public static int[] readPaletteRGBA(FileManipulator fm, int colorCount) {

    int[] palette = new int[colorCount];

    for (int i = 0; i < colorCount; i++) {
      // INPUT = RGBA
      // OUTPUT = ARGB
      palette[i] = ((ByteConverter.unsign(fm.readByte()) << 16) | (ByteConverter.unsign(fm.readByte()) << 8) | ByteConverter.unsign(fm.readByte()) | (ByteConverter.unsign(fm.readByte()) << 24));
    }

    return palette;
  }

  /**
   **********************************************************************************************
   * Reads a color palette in RGB format
   **********************************************************************************************
   **/
  public static int[] readPaletteRGB(FileManipulator fm, int colorCount) {

    int[] palette = new int[colorCount];

    for (int i = 0; i < colorCount; i++) {
      // INPUT = RGB
      // OUTPUT = ARGB
      palette[i] = ((ByteConverter.unsign(fm.readByte()) << 16) | (ByteConverter.unsign(fm.readByte()) << 8) | ByteConverter.unsign(fm.readByte()) | (255 << 24));
    }

    return palette;
  }

  /**
   **********************************************************************************************
   * Reads a color palette in ARGB format
   **********************************************************************************************
   **/
  public static int[] readPaletteARGB(FileManipulator fm, int colorCount) {

    int[] palette = new int[colorCount];

    for (int i = 0; i < colorCount; i++) {
      // INPUT = ARGB
      // OUTPUT = ARGB
      palette[i] = ((ByteConverter.unsign(fm.readByte()) << 24) | (ByteConverter.unsign(fm.readByte()) << 16) | (ByteConverter.unsign(fm.readByte()) << 8) | (ByteConverter.unsign(fm.readByte())));
    }

    return palette;
  }

  /**
   **********************************************************************************************
   * Reads a color palette in ABGR format
   **********************************************************************************************
   **/
  public static int[] readPaletteABGR(FileManipulator fm, int colorCount) {

    int[] palette = new int[colorCount];

    for (int i = 0; i < colorCount; i++) {
      // INPUT = ABGR
      // OUTPUT = ARGB
      palette[i] = ((ByteConverter.unsign(fm.readByte()) << 24) | (ByteConverter.unsign(fm.readByte())) | (ByteConverter.unsign(fm.readByte()) << 8) | (ByteConverter.unsign(fm.readByte()) << 16));
    }

    return palette;
  }

  /**
   **********************************************************************************************
   * Reads PVRTC 4bpp Pixel Data
   * Calls "Noesis" to convert the compressed data into raw RGBA
   * THIS IS VERY SLOW!!!
   **********************************************************************************************
   **/
  public static ImageResource readPVRTC4bpp(FileManipulator fm, int width, int height) {
    try {
      int numPixels = width * height;
      int numBytes = numPixels / 2;

      //
      // STEP 1
      // Check that Noesis is found, and that we can find the /plugin/python directory
      //

      String noesisPath = Settings.getString("Noesis_Path");

      File noesisFile = new File(noesisPath);

      if (noesisFile.exists() && noesisFile.isDirectory()) {
        // Path is a directory, append the filename to it
        noesisPath = noesisPath + File.separatorChar + "Noesis.exe";
        noesisFile = new File(noesisPath);
      }

      if (!noesisFile.exists()) {
        // noesis path is invalid
        ErrorLogger.log("Noesis can't be found at the path " + noesisFile.getAbsolutePath());
        return null;
      }

      noesisPath = noesisFile.getAbsolutePath();

      // Find the plugins directory
      String pluginsPath = noesisFile.getParent() + File.separatorChar + "plugins" + File.separatorChar + "python";
      File pluginsFile = new File(pluginsPath);

      if (!pluginsFile.exists()) {
        // noesis plugins path is invalid
        ErrorLogger.log("Noesis plugin directory can't be found at the path " + pluginsFile.getAbsolutePath());
        return null;
      }

      //
      // STEP 2
      // Dump all the compressed image bytes to a file
      //

      // Create a temporary file for the compressed image bytes
      String tempFilePath = new File(Settings.get("TempDirectory")).getAbsolutePath();
      tempFilePath += File.separator + "noesis_pvrtc4_compressed_temp_" + System.currentTimeMillis() + ".ge_temp"; // System.currentTimeMillis() to make it a unique filename
      File tempFile = new File(tempFilePath);
      if (tempFile.exists()) {
        tempFile.delete();
      }
      //tempFile = FilenameChecker.correctFilename(tempFile); // removes funny characters etc.

      // write the compressed image bytes
      FileManipulator tempFM = new FileManipulator(tempFile, true);
      tempFM.writeString("WATTO_GE");
      tempFM.writeBytes(fm.readBytes(numBytes)); // 4 bits per pixel
      tempFM.close();

      // get the absolute path to the temporary file of compressed image bytes
      tempFilePath = tempFile.getAbsolutePath();

      // Create a temporary file for the UNcompressed image bytes
      String tempOutFilePath = tempFilePath + ".out";
      File tempOutFile = new File(tempOutFilePath);
      if (tempOutFile.exists()) {
        tempOutFile.delete();
      }
      tempOutFilePath = tempOutFile.getAbsolutePath();

      //
      // STEP 3
      // Write a Noesis plugin for performing the decompression of the image bytes
      //

      // Create a python script to do the decompression
      String scriptFilePath = pluginsPath;
      scriptFilePath += File.separator + "ge3_image_converter.py";
      File scriptFile = new File(scriptFilePath);
      if (scriptFile.exists()) {
        scriptFile.delete();
      }
      //tempFile = FilenameChecker.correctFilename(tempFile); // removes funny characters etc.

      // write the script
      FileManipulator tempScriptFM = new FileManipulator(scriptFile, true);
      tempScriptFM.writeString("from inc_noesis import *\n");
      tempScriptFM.writeString("\n");
      tempScriptFM.writeString("def registerNoesisTypes():\n");
      tempScriptFM.writeString("    handle = noesis.register(\"Game Extractor Image Converter\", \".ge_temp\")\n");
      tempScriptFM.writeString("    noesis.setHandlerTypeCheck(handle, noepyCheckType)\n");
      tempScriptFM.writeString("    noesis.setHandlerLoadRGBA(handle, noepyLoadRGBA)\n");
      tempScriptFM.writeString("    return 1\n");
      tempScriptFM.writeString("\n");
      tempScriptFM.writeString("def noepyCheckType(data):\n");
      tempScriptFM.writeString("    bs = NoeBitStream(data)\n");
      tempScriptFM.writeString("    Magic = noeStrFromBytes(bs.readBytes(8))\n");
      tempScriptFM.writeString("    if Magic != \"WATTO_GE\":\n");
      tempScriptFM.writeString("        return 0\n");
      tempScriptFM.writeString("    return 1\n");
      tempScriptFM.writeString("\n");
      tempScriptFM.writeString("def noepyLoadRGBA(data, texList):\n");
      tempScriptFM.writeString("    bs = NoeBitStream(data)\n");
      tempScriptFM.writeString("    bs.seek(0x8, NOESEEK_ABS)\n");
      tempScriptFM.writeString("    data = bs.readBytes(" + numBytes + ")\n");
      tempScriptFM.writeString("    data = rapi.imageDecodePVRTC(data, " + width + ", " + height + ", 4, noesis.PVRTC_DECODE_PVRTC2)\n");
      tempScriptFM.writeString("    f = open('" + tempOutFilePath.replaceAll("\\\\", "\\\\\\\\") + "', 'wb')\n");
      tempScriptFM.writeString("    f.write(data)\n");
      tempScriptFM.writeString("    f.close()\n");
      //tempScriptFM.writeString("    texFmt = noesis.NOESISTEX_RGBA32\n");
      //tempScriptFM.writeString("    texList.append(NoeTexture(rapi.getInputName(), imgWidth, imgHeight, data, texFmt))\n");
      tempScriptFM.writeString("    return 1\n");
      tempScriptFM.close();

      scriptFilePath = scriptFile.getAbsolutePath();

      //
      // STEP 4
      // Run Noesis to convert the compressed image data to RGBA format
      //

      ProcessBuilder pb = new ProcessBuilder(noesisPath, "?cmode", tempFilePath, "dummyoutput.tmp", "-notex", "-nopause");
      Process convertProcess = pb.start();
      int returnCode = convertProcess.waitFor(); // wait for Noesis to finish
      if (returnCode == 0) {
        // successful decompression
        //Thread.sleep(1000); // if we try to read the file too quickly, it doesn't exist yet!
        if (!tempOutFile.exists()) {
          ErrorLogger.log("Noesis failed to run the conversion script for the image");
          return null;
        }
      }

      //
      // STEP 5
      // Read the RGBA data from the output file
      //
      if (!tempOutFile.exists()) {
        ErrorLogger.log("Noesis ran the conversion, but the file is missing.");
        return null;
      }

      FileManipulator tempConvertedFM = new FileManipulator(tempOutFile, false);

      // X Bytes - Pixel Data
      int[] pixels = new int[numPixels];

      for (int i = 0; i < numPixels; i++) {
        // INPUT = RGBA
        int rPixel = ByteConverter.unsign(tempConvertedFM.readByte());
        int gPixel = ByteConverter.unsign(tempConvertedFM.readByte());
        int bPixel = ByteConverter.unsign(tempConvertedFM.readByte());
        int aPixel = ByteConverter.unsign(tempConvertedFM.readByte());

        // OUTPUT = ARGB
        pixels[i] = ((rPixel << 16) | (gPixel << 8) | bPixel | (aPixel << 24));
      }

      tempConvertedFM.close();

      return new ImageResource(pixels, width, height);
    }
    catch (Throwable t) {
      ErrorLogger.log(t);
      return null;
    }
  }

  /**
   **********************************************************************************************
   * Reads R Pixel Data, where each color is 16 bits in size
   **********************************************************************************************
   **/
  public static ImageResource readR16(FileManipulator fm, int width, int height) {

    int numPixels = width * height;

    // X Bytes - Pixel Data
    int[] pixels = new int[numPixels];

    for (int i = 0; i < numPixels; i++) {
      int rPixel = (ShortConverter.unsign(fm.readShort()) / 2);
      int gPixel = 0;
      int bPixel = 0;
      int aPixel = 255;

      //pixels[i] = ((fm.readByte() << 16) | (fm.readByte() << 8) | fm.readByte() | (((byte) 255) << 24));
      pixels[i] = ((rPixel << 16) | (gPixel << 8) | bPixel | (aPixel << 24));
    }

    return new ImageResource(pixels, width, height);
  }

  /**
   **********************************************************************************************
   * Reads RG Pixel Data, where each color is 16 bits in size
   **********************************************************************************************
   **/
  public static ImageResource readR16G16(FileManipulator fm, int width, int height) {

    int numPixels = width * height;

    // X Bytes - Pixel Data
    int[] pixels = new int[numPixels];

    for (int i = 0; i < numPixels; i++) {
      int rPixel = (ShortConverter.unsign(fm.readShort()) / 2);
      int gPixel = (ShortConverter.unsign(fm.readShort()) / 2);
      int bPixel = 0;
      int aPixel = 255;

      //pixels[i] = ((fm.readByte() << 16) | (fm.readByte() << 8) | fm.readByte() | (((byte) 255) << 24));
      pixels[i] = ((rPixel << 16) | (gPixel << 8) | bPixel | (aPixel << 24));
    }

    return new ImageResource(pixels, width, height);
  }

  /**
   **********************************************************************************************
   * Reads RG Pixel Data
   **********************************************************************************************
   **/
  public static ImageResource readRG(FileManipulator fm, int width, int height) {

    int numPixels = width * height;

    // X Bytes - Pixel Data
    int[] pixels = new int[numPixels];

    for (int i = 0; i < numPixels; i++) {
      int rPixel = ByteConverter.unsign(fm.readByte());
      int gPixel = ByteConverter.unsign(fm.readByte());
      int bPixel = 0;
      int aPixel = 255;

      //pixels[i] = ((fm.readByte() << 16) | (fm.readByte() << 8) | fm.readByte() | (((byte) 255) << 24));
      pixels[i] = ((rPixel << 16) | (gPixel << 8) | bPixel | (aPixel << 24));
    }

    return new ImageResource(pixels, width, height);
  }

  /**
   **********************************************************************************************
   * Reads RGB Pixel Data
   **********************************************************************************************
   **/
  public static ImageResource readRGB(FileManipulator fm, int width, int height) {

    int numPixels = width * height;

    // X Bytes - Pixel Data
    int[] pixels = new int[numPixels];

    for (int i = 0; i < numPixels; i++) {
      int rPixel = ByteConverter.unsign(fm.readByte());
      int gPixel = ByteConverter.unsign(fm.readByte());
      int bPixel = ByteConverter.unsign(fm.readByte());
      int aPixel = 255;

      //pixels[i] = ((fm.readByte() << 16) | (fm.readByte() << 8) | fm.readByte() | (((byte) 255) << 24));
      pixels[i] = ((rPixel << 16) | (gPixel << 8) | bPixel | (aPixel << 24));
    }

    return new ImageResource(pixels, width, height);
  }

  /**
   **********************************************************************************************
   * Reads RGB555 Pixel Data
   **********************************************************************************************
   **/
  public static ImageResource readRGB555(FileManipulator fm, int width, int height) {

    int numPixels = width * height;

    // X Bytes - Pixel Data
    int[] pixels = new int[numPixels];

    for (int i = 0; i < numPixels; i++) {
      int pixel = ShortConverter.unsign(fm.readShort());

      int rPixel = ((pixel >> 10) & 31) * 8;
      int gPixel = ((pixel >> 5) & 31) * 8;
      int bPixel = (pixel & 31) * 8;
      int aPixel = 255;

      // OUTPUT = ARGB
      pixels[i] = ((rPixel << 16) | (gPixel << 8) | bPixel | (aPixel << 24));
    }

    return new ImageResource(pixels, width, height);
  }

  /**
   **********************************************************************************************
   * Reads BGR555 Pixel Data
   **********************************************************************************************
   **/
  public static ImageResource readBGR555(FileManipulator fm, int width, int height) {

    int numPixels = width * height;

    // X Bytes - Pixel Data
    int[] pixels = new int[numPixels];

    for (int i = 0; i < numPixels; i++) {
      int pixel = ShortConverter.unsign(fm.readShort());

      int bPixel = ((pixel >> 10) & 31) * 8;
      int gPixel = ((pixel >> 5) & 31) * 8;
      int rPixel = (pixel & 31) * 8;
      int aPixel = 255;

      // OUTPUT = ARGB
      pixels[i] = ((rPixel << 16) | (gPixel << 8) | bPixel | (aPixel << 24));
    }

    return new ImageResource(pixels, width, height);
  }

  /**
   **********************************************************************************************
   * Reads RGB555 Pixel Data, where the <i>short</i>s are in big endian format
   **********************************************************************************************
   **/
  public static ImageResource readRGB555BigEndian(FileManipulator fm, int width, int height) {

    int numPixels = width * height;

    // X Bytes - Pixel Data
    int[] pixels = new int[numPixels];

    for (int i = 0; i < numPixels; i++) {
      int pixel = ShortConverter.unsign(ShortConverter.changeFormat(fm.readShort()));

      int rPixel = ((pixel >> 10) & 31) * 8;
      int gPixel = ((pixel >> 5) & 31) * 8;
      int bPixel = (pixel & 31) * 8;
      int aPixel = 255;

      // OUTPUT = ARGB
      pixels[i] = ((rPixel << 16) | (gPixel << 8) | bPixel | (aPixel << 24));
    }

    return new ImageResource(pixels, width, height);
  }

  /**
   **********************************************************************************************
   * Reads RGB565 Pixel Data
   **********************************************************************************************
   **/
  public static ImageResource readRGB565(FileManipulator fm, int width, int height) {

    int numPixels = width * height;

    // X Bytes - Pixel Data
    int[] pixels = new int[numPixels];

    for (int i = 0; i < numPixels; i++) {
      int pixel = ShortConverter.unsign(fm.readShort());

      //int rPixel = ((pixel >> 10) & 31) * 8;
      int rPixel = ((pixel >> 11) & 31) * 8; // 3.15 Fixed incorrect pixel shift
      int gPixel = ((pixel >> 5) & 63) * 4;
      int bPixel = (pixel & 31) * 8;
      int aPixel = 255;

      // OUTPUT = ARGB
      pixels[i] = ((rPixel << 16) | (gPixel << 8) | bPixel | (aPixel << 24));

    }

    return new ImageResource(pixels, width, height);
  }

  /**
   **********************************************************************************************
   * Reads RGB565 Pixel Data, where the <i>short</i>s are in big endian format
   **********************************************************************************************
   **/
  public static ImageResource readRGB565BigEndian(FileManipulator fm, int width, int height) {

    int numPixels = width * height;

    // X Bytes - Pixel Data
    int[] pixels = new int[numPixels];

    for (int i = 0; i < numPixels; i++) {
      int pixel = ShortConverter.unsign(ShortConverter.changeFormat(fm.readShort()));

      int rPixel = ((pixel >> 10) & 31) * 8;
      int gPixel = ((pixel >> 5) & 63) * 4;
      int bPixel = (pixel & 31) * 8;
      int aPixel = 255;

      // OUTPUT = ARGB
      pixels[i] = ((rPixel << 16) | (gPixel << 8) | bPixel | (aPixel << 24));
    }

    return new ImageResource(pixels, width, height);
  }

  /**
   **********************************************************************************************
   * Reads RGBA Pixel Data
   **********************************************************************************************
   **/
  public static ImageResource readRGBA(FileManipulator fm, int width, int height) {

    int numPixels = width * height;

    // X Bytes - Pixel Data
    int[] pixels = new int[numPixels];

    for (int i = 0; i < numPixels; i++) {
      // INPUT = RGBA
      int rPixel = ByteConverter.unsign(fm.readByte());
      int gPixel = ByteConverter.unsign(fm.readByte());
      int bPixel = ByteConverter.unsign(fm.readByte());
      int aPixel = ByteConverter.unsign(fm.readByte());

      // OUTPUT = ARGB
      pixels[i] = ((rPixel << 16) | (gPixel << 8) | bPixel | (aPixel << 24));
    }

    return new ImageResource(pixels, width, height);
  }

  /**
   **********************************************************************************************
   * Reads ARGB Pixel Data in a 4x4 block format (from Wii)
   * (ARARARARARARARAR ARARARARARARARAR GBGBGBGBGBGBGBGB GBGBGBGBGBGBGBGB)
   **********************************************************************************************
   **/
  public static ImageResource readRGBA8Wii(FileManipulator fm, int width, int height) {

    // ensure width and height are multiples of 4...
    int heightMod = height % 4;
    if (heightMod != 0) {
      height += (4 - heightMod);
    }
    int widthMod = width % 4;
    if (widthMod != 0) {
      width += (4 - widthMod);
    }

    int numPixels = width * height;

    // X Bytes - Pixel Data
    int[] pixels = new int[numPixels];

    for (int h = 0; h < height; h += 4) {
      for (int w = 0; w < width; w += 4) {

        byte[] arBlock = fm.readBytes(32);
        byte[] gbBlock = fm.readBytes(32);

        int readPos = 0;
        for (int h2 = 0; h2 < 4; h2++) {
          for (int w2 = 0; w2 < 4; w2++) {
            int writePos = ((h + h2) * width) + (w + w2);

            // INPUT = RGBA
            int aPixel = ByteConverter.unsign(arBlock[readPos]);
            int rPixel = ByteConverter.unsign(arBlock[readPos + 1]);
            int gPixel = ByteConverter.unsign(gbBlock[readPos]);
            int bPixel = ByteConverter.unsign(gbBlock[readPos + 1]);

            // OUTPUT = ARGB
            pixels[writePos] = ((rPixel << 16) | (gPixel << 8) | bPixel | (aPixel << 24));

            readPos += 2;
          }
        }

      }
    }

    return new ImageResource(pixels, width, height);
  }

  /**
   **********************************************************************************************
   * Reads R4G4B4A4 Pixel Data
   * TODO UNTESTED!!!
   **********************************************************************************************
   **/
  public static ImageResource readRGBA4444(FileManipulator fm, int width, int height) {

    int numPixels = width * height;

    // X Bytes - Pixel Data
    int[] pixels = new int[numPixels];

    for (int i = 0; i < numPixels; i++) {
      int byte1 = ByteConverter.unsign(fm.readByte());
      int byte2 = ByteConverter.unsign(fm.readByte());

      int rPixel = (byte1 >> 4) * 16;
      int gPixel = (byte1 & 15) * 16;

      int bPixel = (byte2 >> 4) * 16;
      int aPixel = (byte2 & 15) * 16;

      //System.out.println(rPixel + "\t" + gPixel + "\t" + bPixel + "\t" + aPixel);

      pixels[i] = ((rPixel << 16) | (gPixel << 8) | bPixel | (aPixel << 24));
    }

    return new ImageResource(pixels, width, height);

  }

  /**
   **********************************************************************************************
   * Reads RGB5A3 Pixel Data in a 4x4 block format (from Wii)
   * (either 0AAARRRRGGGGBBBB or 1RRRRRGGGGGBBBBB depending on the top bit)
   **********************************************************************************************
   **/
  public static ImageResource readRGB5A3Wii(FileManipulator fm, int width, int height) {

    // ensure width and height are multiples of 4...
    int heightMod = height % 4;
    if (heightMod != 0) {
      height += (4 - heightMod);
    }
    int widthMod = width % 4;
    if (widthMod != 0) {
      width += (4 - widthMod);
    }

    int numPixels = width * height;

    // X Bytes - Pixel Data
    int[] pixels = new int[numPixels];

    for (int h = 0; h < height; h += 4) {
      for (int w = 0; w < width; w += 4) {

        for (int h2 = 0; h2 < 4; h2++) {
          for (int w2 = 0; w2 < 4; w2++) {
            int writePos = ((h + h2) * width) + (w + w2);

            int byte1 = ByteConverter.unsign(fm.readByte());
            int byte2 = ByteConverter.unsign(fm.readByte());

            int topBit = byte1 >> 7;

            if (topBit == 0) {
              // 0AAARRRRGGGGBBBB

              int a = ((byte1 >> 4) & 7) * 32;
              int r = (byte1 & 15) * 16;
              int g = ((byte2 >> 4) & 15) * 16;
              int b = (byte2 & 15) * 16;

              // OUTPUT = ARGB
              pixels[writePos] = ((r << 16) | (g << 8) | b | (a << 24));

            }
            else { // topBit == 1
              // 1RRRRRGG GGGBBBBB

              int a = 255;
              int r = ((byte1 >> 2) & 31) * 8;
              int g = (((byte2 >> 5) & 7) | ((byte1 & 3) << 3)) * 8;
              int b = (byte2 & 31) * 8;

              // OUTPUT = ARGB
              pixels[writePos] = ((r << 16) | (g << 8) | b | (a << 24));

            }
          }

        }
      }

    }

    return new ImageResource(pixels, width, height);
  }

  /**
   **********************************************************************************************
   * Reads GBAR5551 Pixel Data
   **********************************************************************************************
   **/
  public static ImageResource readGBAR5551(FileManipulator fm, int width, int height) {

    int numPixels = width * height;

    // X Bytes - Pixel Data (gggbbbbb arrrrrgg)
    int[] pixels = new int[numPixels];

    for (int i = 0; i < numPixels; i++) {
      int byte1 = ByteConverter.unsign(fm.readByte());
      int byte2 = ByteConverter.unsign(fm.readByte());

      int r = ((byte2 >> 2) & 31) * 8;
      int g = (((byte2 & 3) << 3) | ((byte1 >> 5) & 7)) * 8;
      int b = (byte1 & 31) * 8;
      int a = (byte2 >> 7) * 255;

      // OUTPUT = ARGB
      pixels[i] = ((r << 16) | (g << 8) | b | (a << 24));
    }

    return new ImageResource(pixels, width, height);
  }

  /**
   **********************************************************************************************
   * Reads GRAB5551 Pixel Data
   **********************************************************************************************
   **/
  public static ImageResource readGRAB5551(FileManipulator fm, int width, int height) {

    int numPixels = width * height;

    // X Bytes - Pixel Data
    int[] pixels = new int[numPixels];

    for (int i = 0; i < numPixels; i++) {
      int byte1 = ByteConverter.unsign(fm.readByte());
      int byte2 = ByteConverter.unsign(fm.readByte());

      int b = ((byte2 >> 2) & 31) * 8;
      int g = (((byte2 & 3) << 3) | ((byte1 >> 5) & 7)) * 8;
      int r = (byte1 & 31) * 8;
      int a = (byte2 >> 7) * 255;

      // GGGRRRRR ABBBBBGG

      // OUTPUT = ARGB
      pixels[i] = ((r << 16) | (g << 8) | b | (a << 24));
    }

    return new ImageResource(pixels, width, height);
  }

  /**
   **********************************************************************************************
   * Reads RGBA5551 Pixel Data
   **********************************************************************************************
   **/
  public static ImageResource readRGBA5551(FileManipulator fm, int width, int height) {

    int numPixels = width * height;

    // X Bytes - Pixel Data
    int[] pixels = new int[numPixels];

    // gggbbbbb arrrrrgg
    for (int i = 0; i < numPixels; i++) {
      int byte1 = ByteConverter.unsign(fm.readByte());
      int byte2 = ByteConverter.unsign(fm.readByte());

      int r = ((byte2 >> 2) & 31) * 8;
      int g = (((byte2 & 3) << 3) | ((byte1 >> 5) & 7)) * 8;
      int b = (byte1 & 31) * 8;
      int a = (byte2 >> 7) * 255;

      // OUTPUT = ARGB
      pixels[i] = ((r << 16) | (g << 8) | b | (a << 24));
    }

    return new ImageResource(pixels, width, height);
  }

  /**
   **********************************************************************************************
   * Reads RGBA5551 Pixel Data
   **********************************************************************************************
   **/
  public static ImageResource readRGBA5551BigEndian(FileManipulator fm, int width, int height) {

    int numPixels = width * height;

    // X Bytes - Pixel Data
    int[] pixels = new int[numPixels];

    for (int i = 0; i < numPixels; i++) {
      int byte2 = ByteConverter.unsign(fm.readByte()); // byte 2 (to swap the endian)
      int byte1 = ByteConverter.unsign(fm.readByte()); // byte 1 (to swap the endian)

      int r = ((byte2 >> 2) & 31) * 8;
      int g = (((byte2 & 3) << 3) | ((byte1 >> 5) & 7)) * 8;
      int b = (byte1 & 31) * 8;
      int a = (byte2 >> 7) * 255;

      // OUTPUT = ARGB
      pixels[i] = ((r << 16) | (g << 8) | b | (a << 24));
    }

    return new ImageResource(pixels, width, height);
  }

  /**
   **********************************************************************************************
   * Reads DuDv Map pixel data - U 8bits V 8bits
   **********************************************************************************************
   **/
  public static ImageResource readU8V8(FileManipulator fm, int width, int height) {

    int numPixels = width * height;
    int[] pixels = new int[numPixels];

    for (int i = 0; i < numPixels; i++) {
      int uPixel = ByteConverter.unsign(fm.readByte());
      int vPixel = ByteConverter.unsign(fm.readByte());

      pixels[i] = ((uPixel << 16) | (vPixel << 8) | 255 | (255 << 24));
    }

    return new ImageResource(pixels, width, height);
  }

  /**
   **********************************************************************************************
  Removes all the Alpha values (sets them to 255 - full alpha)
   **********************************************************************************************
   **/
  public static ImageResource removeAlpha(ImageResource image) {
    int[] pixels = image.getPixels();
    int numPixels = pixels.length;

    int[] reversedPixels = new int[numPixels];
    for (int i = 0; i < numPixels; i++) {
      int pixel = pixels[i];

      reversedPixels[i] = ((pixel << 8) >> 8) | (255 << 24); // <<8 >>8 removes the alpha from the top of the pixel
    }

    image.setPixels(reversedPixels);
    return image;
  }

  /**
   **********************************************************************************************
  Removes all the Alpha values, but only if they're all set to invisible
   **********************************************************************************************
   **/
  public static ImageResource removeAlphaIfAllInvisible(ImageResource image) {
    int[] pixels = image.getPixels();
    int numPixels = pixels.length;

    int[] reversedPixels = new int[numPixels];
    for (int i = 0; i < numPixels; i++) {
      int pixel = pixels[i];

      int alpha = pixel >> 24;
      if (alpha != 0) {
        return image; // at least 1 non-alpha found, so just return the image as it currently is.
      }

      reversedPixels[i] = ((pixel << 8) >> 8) | (255 << 24); // <<8 >>8 removes the alpha from the top of the pixel
    }

    image.setPixels(reversedPixels);
    return image;
  }

  /**
   **********************************************************************************************
  Changes all the color values so that 0=255, 1=254, etc. ie for when 0 actually means full color
   **********************************************************************************************
   **/
  public static ImageResource reverseAllColors(ImageResource image) {
    int[] pixels = image.getPixels();
    int numPixels = pixels.length;

    int[] reversedPixels = new int[numPixels];
    for (int i = 0; i < numPixels; i++) {
      int pixel = pixels[i];

      int aPixel = pixel >> 24;
      aPixel = 255 - aPixel;

      int rPixel = (pixel >> 16) & 255;
      rPixel = 255 - rPixel;

      int gPixel = (pixel >> 8) & 255;
      gPixel = 255 - gPixel;

      int bPixel = pixel & 255;
      bPixel = 255 - bPixel;

      reversedPixels[i] = (aPixel << 24) | (rPixel << 16) | (gPixel << 8) | (bPixel);
    }

    image.setPixels(reversedPixels);
    return image;
  }

  /**
   **********************************************************************************************
  Converts alpha values 0-127 to 0-255
   **********************************************************************************************
   **/
  public static ImageResource doubleAlpha(ImageResource image) {
    int[] pixels = image.getPixels();
    int numPixels = pixels.length;

    int[] reversedPixels = new int[numPixels];
    for (int i = 0; i < numPixels; i++) {
      int pixel = pixels[i];

      int alphaValue = pixel >> 24;
      if (alphaValue == -128) {
        alphaValue = 255;
      }
      else {
        alphaValue *= 2;
      }

      reversedPixels[i] = (pixel & 0xFFFFFF) | (alphaValue << 24);
    }

    image.setPixels(reversedPixels);
    return image;
  }

  /**
   **********************************************************************************************
  Converts alpha values 0-127 to 0-255
   **********************************************************************************************
   **/
  public static int[] doubleAlpha(int[] pixels) {
    int numPixels = pixels.length;

    int[] reversedPixels = new int[numPixels];
    for (int i = 0; i < numPixels; i++) {
      int pixel = pixels[i];

      int alphaValue = pixel >> 24;
      if (alphaValue == -128) {
        alphaValue = 255;
      }
      else {
        alphaValue *= 2;
      }

      reversedPixels[i] = (pixel & 0xFFFFFF) | (alphaValue << 24);
    }

    return reversedPixels;
  }

  /**
   **********************************************************************************************
  Changes all the Alpha values so that 0=255, 1=254, etc. Ie for when 0 actually means full alpha
   **********************************************************************************************
   **/
  public static ImageResource reverseAlpha(ImageResource image) {
    if (image == null) {
      return null;
    }

    int[] pixels = image.getPixels();
    int numPixels = pixels.length;

    int[] reversedPixels = new int[numPixels];
    for (int i = 0; i < numPixels; i++) {
      int pixel = pixels[i];

      int aPixel = pixel >> 24;
      aPixel = 255 - aPixel;

      reversedPixels[i] = (pixel & 0xFFFFFF) | (aPixel << 24);
    }

    image.setPixels(reversedPixels);
    return image;
  }

  /**
   **********************************************************************************************
  Stripes a color palette for the PS2
   **********************************************************************************************
   **/
  public static int[] stripePalettePS2(int[] palette) {
    return unstripePalettePS2(palette); // the function is reversible
  }

  /**
   **********************************************************************************************
  Swizzles an image for the PS2
   **********************************************************************************************
   **/
  public static byte[] swizzlePS2(byte[] bytes, int width, int height) {

    // Make a copy of the swizzled input
    int dataLength = bytes.length;
    byte[] swizzled = new byte[dataLength];
    System.arraycopy(bytes, 0, swizzled, 0, dataLength);

    for (int y = 0; y < height; y++) {
      for (int x = 0; x < width; x++) {
        int block_location = (y & (~0xf)) * width + (x & (~0xf)) * 2;
        int swap_selector = (((y + 2) >> 2) & 0x1) * 4;
        int posY = (((y & (~3)) >> 1) + (y & 1)) & 0x7;
        int column_location = posY * width * 2 + ((x + swap_selector) & 0x7) * 4;

        int byte_num = ((y >> 1) & 1) + ((x >> 2) & 2); // 0,1,2,3

        //bytes[(y * width) + x] = swizzled[block_location + column_location + byte_num];
        bytes[block_location + column_location + byte_num] = swizzled[(y * width) + x];
      }
    }

    return bytes;
  }

  /**
   **********************************************************************************************
  Swizzles an image for the PS2
  SAME AS OTHER METHOD, EXCEPT... the bytes are an int[] instead of a byte[]
   **********************************************************************************************
   **/
  public static int[] swizzlePS2(int[] bytes, int width, int height) {

    // Make a copy of the swizzled input
    int dataLength = bytes.length;
    int[] swizzled = new int[dataLength];
    System.arraycopy(bytes, 0, swizzled, 0, dataLength);

    for (int y = 0; y < height; y++) {
      for (int x = 0; x < width; x++) {
        int block_location = (y & (~0xf)) * width + (x & (~0xf)) * 2;
        int swap_selector = (((y + 2) >> 2) & 0x1) * 4;
        int posY = (((y & (~3)) >> 1) + (y & 1)) & 0x7;
        int column_location = posY * width * 2 + ((x + swap_selector) & 0x7) * 4;

        int byte_num = ((y >> 1) & 1) + ((x >> 2) & 2); // 0,1,2,3

        //bytes[(y * width) + x] = swizzled[block_location + column_location + byte_num];
        bytes[block_location + column_location + byte_num] = swizzled[(y * width) + x];
      }
    }

    return bytes;
  }

  /**
   **********************************************************************************************
  Unstripes a color palette for the PS2
   **********************************************************************************************
   **/
  public static int[] unstripePalettePS2(int[] palette) {

    int numColors = palette.length;

    int parts = numColors / 32;
    int stripes = 2;
    int colors = 8;
    int blocks = 2;

    int i = 0;
    int[] newPalette = new int[numColors];
    for (int part = 0; part < parts; part++) {
      for (int block = 0; block < blocks; block++) {
        for (int stripe = 0; stripe < stripes; stripe++) {
          for (int color = 0; color < colors; color++) {
            newPalette[i++] = palette[part * colors * stripes * blocks + block * colors + stripe * stripes * colors + color];
          }
        }
      }
    }

    return newPalette;
  }

  /**
   **********************************************************************************************
  Un-swizzles (Morton Code) an image
  Based on puyotools --> Libraries/GimSharp/GimTexture/GimDataCodec.cs --> UnSwizzle()
   **********************************************************************************************
   **/
  public static byte[] unswizzle(byte[] bytes, int width, int height, int blockSize) {

    int numBytes = bytes.length;
    byte[] outBytes = new byte[numBytes];

    int maxPos = numBytes / blockSize;

    int outPos = 0;

    for (int x = 0; x < width; x++) {
      for (int y = 0; y < height; y++) {
        int index = (int) calculateMorton2D(x, y);
        //System.out.println("Index: " + index);
        if (index >= maxPos) {
          continue;
        }

        //outBytes[outPos] = bytes[index*4:index*4+4];
        System.arraycopy(bytes, index * blockSize, outBytes, outPos, blockSize);
        outPos += blockSize;
      }
    }

    return outBytes;
  }

  /**
   **********************************************************************************************
  Un-swizzles (Morton Code) an image
  Based on puyotools --> Libraries/GimSharp/GimTexture/GimDataCodec.cs --> UnSwizzle()
   **********************************************************************************************
   **/
  public static int[] unswizzle(int[] bytes, int width, int height, int blockSize) {

    int numBytes = bytes.length;
    int[] outBytes = new int[numBytes];

    int maxPos = numBytes / blockSize;

    int outPos = 0;

    for (int x = 0; x < width; x++) {
      for (int y = 0; y < height; y++) {
        int index = (int) calculateMorton2D(x, y);
        //System.out.println("Index: " + index);
        if (index >= maxPos) {
          continue;
        }

        //outBytes[outPos] = bytes[index*4:index*4+4];
        System.arraycopy(bytes, index * blockSize, outBytes, outPos, blockSize);
        outPos += blockSize;
      }
    }

    return outBytes;
  }

  /**
   **********************************************************************************************
  Swizzles (Morton Code) an image
  Based on puyotools --> Libraries/GimSharp/GimTexture/GimDataCodec.cs --> UnSwizzle()
   **********************************************************************************************
   **/
  public static int[] swizzle(int[] bytes, int width, int height, int blockSize) {

    int numBytes = bytes.length;
    int[] outBytes = new int[numBytes];

    int maxPos = numBytes / blockSize;

    int outPos = 0;

    for (int x = 0; x < width; x++) {
      for (int y = 0; y < height; y++) {
        int index = (int) calculateMorton2D(x, y);
        if (index >= maxPos) {
          continue;
        }

        //System.arraycopy(bytes, index * blockSize, outBytes, outPos, blockSize);
        System.arraycopy(bytes, outPos, outBytes, index * blockSize, blockSize);
        outPos += blockSize;
      }
    }

    return outBytes;
  }

  /**
   **********************************************************************************************
  Un-swizzles an image for the PS2
   **********************************************************************************************
   **/
  public static byte[] unswizzlePS2(byte[] bytes, int width, int height) {

    // Make a copy of the swizzled input
    int dataLength = bytes.length;
    byte[] swizzled = new byte[dataLength];
    System.arraycopy(bytes, 0, swizzled, 0, dataLength);

    for (int y = 0; y < height; y++) {
      for (int x = 0; x < width; x++) {
        int block_location = (y & (~0xf)) * width + (x & (~0xf)) * 2;
        int swap_selector = (((y + 2) >> 2) & 0x1) * 4;
        int posY = (((y & (~3)) >> 1) + (y & 1)) & 0x7;
        int column_location = posY * width * 2 + ((x + swap_selector) & 0x7) * 4;

        int byte_num = ((y >> 1) & 1) + ((x >> 2) & 2); // 0,1,2,3

        bytes[(y * width) + x] = swizzled[block_location + column_location + byte_num];
      }
    }

    return bytes;
  }

  /**
   **********************************************************************************************
  Un-swizzles an image for the Switch
   **********************************************************************************************
   **/
  public static byte[] unswizzleSwitch(byte[] bytes, int width, int height) {
    return NintendoSwitchSwizzleHelper.unswizzle(bytes, width, height);
  }

  /**
   **********************************************************************************************
  Un-swizzles an image for the PS2
   **********************************************************************************************
   **/
  public static int[] unswizzlePS2(int[] bytes, int width, int height) {

    // Make a copy of the swizzled input
    int dataLength = bytes.length;
    int[] swizzled = new int[dataLength];
    System.arraycopy(bytes, 0, swizzled, 0, dataLength);

    for (int y = 0; y < height; y++) {
      for (int x = 0; x < width; x++) {
        int block_location = (y & (~0xf)) * width + (x & (~0xf)) * 2;
        int swap_selector = (((y + 2) >> 2) & 0x1) * 4;
        int posY = (((y & (~3)) >> 1) + (y & 1)) & 0x7;
        int column_location = posY * width * 2 + ((x + swap_selector) & 0x7) * 4;

        int byte_num = ((y >> 1) & 1) + ((x >> 2) & 2); // 0,1,2,3

        bytes[(y * width) + x] = swizzled[block_location + column_location + byte_num];
      }
    }

    return bytes;
  }

  /**
   **********************************************************************************************
  Un-swizzles an image for the Nintendo Switch
  https://github.com/gdkchan/BnTxx/blob/master/BnTxx/BlockLinearSwizzle.cs
   **********************************************************************************************
   **/
  /*  public static int[] unswizzleSwitch565(byte[] swizzled, int width, int height) {
  
  
    int[] output = new int[width * height];
  
    int OOffset = 0;
  
    int Bpp = 2;
    int BlockHeight = 1; // 16
    int BhMask = (BlockHeight * 8) - 1;
  
    int BhShift = CountLsbZeros(BlockHeight * 8);
    int BppShift = CountLsbZeros(Bpp);
  
    int WidthInGobs = (int) (width * Bpp / 64f);
  
    int GobStride = 512 * BlockHeight * WidthInGobs;
  
    int XShift = CountLsbZeros(512 * BlockHeight);
  
    for (int Y = 0; Y < height; Y++) {
      for (int X = 0; X < width; X++) {
        int IOffs = GetSwitchSwizzleOffset(X, Y, BppShift, BhShift, GobStride, XShift, BhMask);
  
        int Value = swizzled[IOffs + 0] << 0 |
            swizzled[IOffs + 1] << 8;
  
        int R = ((Value >> 0) & 0x1f) << 3;
        int G = ((Value >> 5) & 0x3f) << 2;
        int B = ((Value >> 11) & 0x1f) << 3;
  
        B = (B | (B >> 5));
        G = (G | (G >> 6));
        R = (R | (R >> 5));
        int A = 255;
  
        // OUTPUT = ARGB
        output[OOffset] = ((R << 16) | (G << 8) | B | (A << 24));
        OOffset++;
      }
    }
  
    return output;
  }
  */
  /**
   * For Switch Swizzle
   */
  /*private static int CountLsbZeros(int Value) {
    int Count = 0;
  
    while (((Value >> Count) & 1) == 0) {
      Count++;
    }
  
    return Count;
  }
  */
  /**
   * For Switch Swizzle
   */
  /*public static int GetSwitchSwizzleOffset(int X, int Y, int BppShift, int BhShift, int GobStride, int XShift, int BhMask) {
    X <<= BppShift;
  
    int Position = (Y >> BhShift) * GobStride;
  
    Position += (X >> 6) << XShift;
  
    Position += ((Y & BhMask) >> 3) << 9;
  
    Position += ((X & 0x3f) >> 5) << 8;
    Position += ((Y & 0x07) >> 1) << 6;
    Position += ((X & 0x1f) >> 4) << 5;
    Position += ((Y & 0x01) >> 0) << 4;
    Position += ((X & 0x0f) >> 0) << 0;
  
    return Position;
  }
  */

  /**
   **********************************************************************************************
  Un-swizzles an image for the GameCube
  https://pastebin.com/VDvs7q8Y
   **********************************************************************************************
   **/
  /* public static int[] unswizzleGameCube(int[] bytes, int width, int height, int pitch) {
  
    // Make a copy of the swizzled input
    int dataLength = bytes.length;
    int[] swizzled = new int[dataLength];
    System.arraycopy(bytes, 0, swizzled, 0, dataLength);
  
    for (int y = 0; y < height; y++) {
      for (int x = 0; x < width; x++) {
  
        int bpp = 16;
  
        int rowOffset = y * pitch;
        int pixOffset = x;
  
        int pos = (rowOffset + pixOffset) * bpp;
        pos /= 8;
  
        bpp /= 8;
  
        int pos2 = (y * width + x) * bpp;
        if ((pos2 < dataLength) && (pos < dataLength)) {
          //swizzled[pos2:pos2 + bpp] = bytes[pos:pos + bpp];
          System.arraycopy(swizzled, pos, bytes, pos2, bpp);
        }
      }
    }
  
    return bytes;
  }
  */

  /**
   **********************************************************************************************
   * Rotates the image left by 90 degrees
   **********************************************************************************************
   **/
  public static ImageResource rotateLeft(ImageResource imageResource) {

    // X Bytes - Pixel Data
    int[] pixels = imageResource.getPixels();
    int numPixels = pixels.length;

    // after the rotate, the width and height are swapped, so read them differently here
    int width = imageResource.getHeight();
    int height = imageResource.getWidth();

    int[] outPixels = new int[numPixels];

    int inPos = 0;
    for (int w = 0; w < width; w++) {
      for (int h = height - 1; h >= 0; h--) {
        outPixels[(h * width) + w] = pixels[inPos];
        inPos++;
      }
    }

    // we already swapped them at the top, so just set them properly here.
    imageResource.setWidth(width);
    imageResource.setHeight(height);

    imageResource.setPixels(outPixels);

    return imageResource;
  }

  /**
   **********************************************************************************************
   * Swaps the Red and Blue values in an image. Input should be in (effectively) ABGR format.
   **********************************************************************************************
   **/
  public static ImageResource swapRedAndBlue(ImageResource imageResource) {

    // X Bytes - Pixel Data
    int[] pixels = imageResource.getPixels();
    int numPixels = pixels.length;

    for (int i = 0; i < numPixels; i++) {
      int pixel = pixels[i];

      // INPUT = ABGR
      int rPixel = (pixel & 255);
      int gPixel = ((pixel >> 8) & 255);
      int bPixel = ((pixel >> 16) & 255);
      int aPixel = (pixel >> 24);

      // OUTPUT = ARGB
      pixels[i] = ((rPixel << 16) | (gPixel << 8) | bPixel | (aPixel << 24));
    }

    imageResource.setPixels(pixels);

    return imageResource;
  }

  /**
   **********************************************************************************************
   * Swaps the Alpha and Green values in an image. Input should be in (effectively) GBAR format.
   **********************************************************************************************
   **/
  public static ImageResource swapGBARtoARGB(ImageResource imageResource) {

    // X Bytes - Pixel Data
    int[] pixels = imageResource.getPixels();
    int numPixels = pixels.length;

    for (int i = 0; i < numPixels; i++) {
      int pixel = pixels[i];

      // INPUT = GBAR
      int gPixel = ((pixel >> 24) & 255); // g
      if (gPixel < 0) {
        gPixel = ((128 | (gPixel & 127)) & 255);
        //gPixel = ByteConverter.unsign((byte) gPixel);
      }
      int bPixel = ((pixel >> 16) & 255); // b
      int aPixel = ((pixel >> 8) & 255); // a
      int rPixel = (pixel & 255); // r

      // OUTPUT = ARGB
      pixels[i] = ((rPixel << 16) | (gPixel << 8) | bPixel | (aPixel << 24));

    }

    imageResource.setPixels(pixels);

    return imageResource;
  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public ImageFormatReader() {
  }

}