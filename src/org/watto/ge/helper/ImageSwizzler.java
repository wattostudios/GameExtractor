/*
 * Application:  Game Extractor
 * Author:       wattostudios
 * Website:      http://www.watto.org
 * Copyright:    Copyright (c) 2002-2025 wattostudios
 *
 * License Information:
 * This program is free software; you can redistribute it and/or modify it under the terms of the GNU General Public License
 * published by the Free Software Foundation; either version 2 of the License, or (at your option) any later versions. This
 * program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranties
 * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License at http://www.gnu.org for more
 * details. For further information on this application, refer to the authors' website.
 */

package org.watto.ge.helper;

/**
**********************************************************************************************
Reads image data from a FileManipulator using a number of different image formats
**********************************************************************************************
**/
public class ImageSwizzler {

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
  Stripes a color palette for the PS2
   **********************************************************************************************
   **/
  public static int[] stripePalettePS2(int[] palette) {
    return unstripePalettePS2(palette); // the function is reversible
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
  Swizzles (Morton Code) an image
  Ref: puyotools --> Libraries/GimSharp/GimTexture/GimDataCodec.cs --> UnSwizzle()
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
  Un-swizzles (Morton Code) an image
  Ref: puyotools --> Libraries/GimSharp/GimTexture/GimDataCodec.cs --> UnSwizzle()
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
  Ref: puyotools --> Libraries/GimSharp/GimTexture/GimDataCodec.cs --> UnSwizzle()
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
  Swizzles an image for the PS2 (8-bit image data)
  @return swizzled data as a byte[]
   **********************************************************************************************
   **/
  public static byte[] swizzlePS2(byte[] input_data, int width, int height) {
    return swizzleHandlerPS2(input_data, width, height, true);
  }

  /**
   **********************************************************************************************
  Un-swizzles an image for the PS2 (8-bit image data)
  @return unswizzled data as a byte[]
   **********************************************************************************************
   **/
  public static byte[] unswizzlePS2(byte[] input_data, int width, int height) {
    return swizzleHandlerPS2(input_data, width, height, false);
  }

  /**
   **********************************************************************************************
  Swizzles/Unswizzles  an image for the PS2 (8-bit image data)
  Ref: https://github.com/bartlomiejduda/ReverseBox/blob/main/reversebox/image/swizzling/swizzle_ps2.py
   **********************************************************************************************
   **/
  private static byte[] swizzleHandlerPS2(byte[] input_data, int width, int height, boolean swizzle) {

    byte[] converted_data = new byte[width * height];

    for (int y = 0; y < height; y++) {
      for (int x = 0; x < width; x++) {
        int block_location = (y & (~0xF)) * width + (x & (~0xF)) * 2;
        int swap_selector = (((y + 2) >> 2) & 0x1) * 4;
        int pos_y = (((y & (~3)) >> 1) + (y & 1)) & 0x7;
        int column_location = pos_y * width * 2 + ((x + swap_selector) & 0x7) * 4;
        int byte_num = ((y >> 1) & 1) + ((x >> 2) & 2);
        int swizzle_id = block_location + column_location + byte_num;

        if (!swizzle) {
          // unswizzle
          converted_data[y * width + x] = input_data[swizzle_id];
        }
        else {
          // swizzle
          converted_data[swizzle_id] = input_data[y * width + x];
        }
      }
    }

    return converted_data;
  }

  /**
   **********************************************************************************************
  Swizzles an image for the PS2 (8-bit image data)
  @return swizzled data as an int[]
   **********************************************************************************************
   **/
  public static int[] swizzlePS2(int[] input_data, int width, int height) {
    return swizzleHandlerPS2(input_data, width, height, true);
  }

  /**
   **********************************************************************************************
  Un-swizzles an image for the PS2 (8-bit image data)
  @return unswizzled data as an int[]
   **********************************************************************************************
   **/
  public static int[] unswizzlePS2(int[] input_data, int width, int height) {
    return swizzleHandlerPS2(input_data, width, height, false);
  }

  /**
   **********************************************************************************************
  Swizzles/Unswizzles  an image for the PS2 (8-bit image data)
  Ref: https://github.com/bartlomiejduda/ReverseBox/blob/main/reversebox/image/swizzling/swizzle_ps2.py
   **********************************************************************************************
   **/
  private static int[] swizzleHandlerPS2(int[] input_data, int width, int height, boolean swizzle) {

    int[] converted_data = new int[width * height];

    for (int y = 0; y < height; y++) {
      for (int x = 0; x < width; x++) {
        int block_location = (y & (~0xF)) * width + (x & (~0xF)) * 2;
        int swap_selector = (((y + 2) >> 2) & 0x1) * 4;
        int pos_y = (((y & (~3)) >> 1) + (y & 1)) & 0x7;
        int column_location = pos_y * width * 2 + ((x + swap_selector) & 0x7) * 4;
        int byte_num = ((y >> 1) & 1) + ((x >> 2) & 2);
        int swizzle_id = block_location + column_location + byte_num;

        if (!swizzle) {
          // unswizzle
          converted_data[y * width + x] = input_data[swizzle_id];
        }
        else {
          // swizzle
          converted_data[swizzle_id] = input_data[y * width + x];
        }
      }
    }

    return converted_data;
  }

  /**
   **********************************************************************************************
  Swizzles an image for the PS2 (4-bit image data), EA format swizzle
  @return 4bit swizzled data
   **********************************************************************************************
   **/
  public static byte[] swizzlePS24Bit(byte[] input_data, int width, int height) {
    return PS2SwizzleHelper._ps2_swizzle4(input_data, width, height);
  }

  /**
   **********************************************************************************************
  Un-swizzles an image for the PS2 (4-bit image data), EA format swizzle
  @return 4bit unswizzled data
   **********************************************************************************************
   **/
  public static byte[] unswizzlePS24Bit(byte[] input_data, int width, int height) {
    return PS2SwizzleHelper._ps2_unswizzle4(input_data, width, height);
  }

  /**
   **********************************************************************************************
  Swizzles an image for the PS2 (4-bit image data), EA format swizzle
  @return 4bit swizzled data
   **********************************************************************************************
   **/
  public static int[] swizzlePS24Bit(int[] input_data, int width, int height) {
    return PS2SwizzleHelper._ps2_swizzle4(input_data, width, height);
  }

  /**
   **********************************************************************************************
  Un-swizzles an image for the PS2 (4-bit image data), EA format swizzle
  @return 4bit unswizzled data
   **********************************************************************************************
   **/
  public static int[] unswizzlePS24Bit(int[] input_data, int width, int height) {
    return PS2SwizzleHelper._ps2_unswizzle4(input_data, width, height);
  }

  /**
   **********************************************************************************************
  Swizzles an image for the PS2 (4-bit image data), using SUBA swizzle
  @return 4bit swizzled data
   **********************************************************************************************
   **/
  public static byte[] swizzlePS24BitSuba(byte[] input_data, int width, int height) {
    return swizzleHandlerPS24BitSuba(input_data, width, height, true);
  }

  /**
   **********************************************************************************************
  Un-swizzles an image for the PS2 (4-bit image data), using SUBA swizzle
  @return 4bit unswizzled data
   **********************************************************************************************
   **/
  public static byte[] unswizzlePS24BitSuba(byte[] input_data, int width, int height) {
    return swizzleHandlerPS24BitSuba(input_data, width, height, false);
  }

  /**
   **********************************************************************************************
  Swizzles/Unswizzles  an image for the PS2 (4-bit image data), using Suba swizzle
  Ref: https://github.com/bartlomiejduda/ReverseBox/blob/main/reversebox/image/swizzling/swizzle_ps2_suba.py#L74
  @return 4bit swizzled/unswizzled data
   **********************************************************************************************
   **/
  private static byte[] swizzleHandlerPS24BitSuba(byte[] input_data, int width, int height, boolean swizzle) {

    byte[] converted_data = new byte[input_data.length];

    for (int y = 0; y < height; y++) {
      for (int x = 0; x < width; x++) {
        int index = y * width + x;
        byte in_byte = input_data[index >> 1];
        int u_pen = (in_byte >> ((index & 1) * 4)) & 0xF;

        int pageX = x & (~0x7F);
        int pageY = y & (~0x7F);

        int pages_horz = (width + 127) / 128;
        int pages_vert = (height + 127) / 128;

        int page_number = (pageY / 128) * pages_horz + (pageX / 128);

        int page32Y = (page_number / pages_vert) * 32;
        int page32X = (page_number % pages_vert) * 64;

        int page_location = page32Y * height * 2 + page32X * 4;

        int locX = x & 0x7F;
        int locY = y & 0x7F;

        int block_location = ((locX & (~0x1F)) >> 1) * height + (locY & (~0xF)) * 2;
        int swap_selector = (((y + 2) >> 2) & 0x1) * 4;
        int posY = (((y & (~3)) >> 1) + (y & 1)) & 0x7;

        int column_location = posY * height * 2 + ((x + swap_selector) & 0x7) * 4;

        int byte_num = (x >> 3) & 3;// # 0,1,2,3
        int bits_set = (y >> 1) & 1;//  # 0, 1

        int index_out = page_location + block_location + column_location + byte_num;

        if (swizzle) {
          int out_byte = converted_data[index_out];
          out_byte = (out_byte & (~(0xF << (bits_set * 4)))) | (u_pen << (bits_set * 4));
          converted_data[index_out] = (byte) out_byte;
        }
        else {
          int in_swiz_byte = input_data[index_out];
          int u_pen_unswiz = (in_swiz_byte >> (bits_set * 4)) & 0xF;
          int out_index = index >> 1;
          int shift = (index & 1) * 4;
          int mask = 0xF << shift;
          converted_data[out_index] = (byte) ((converted_data[out_index] & (~mask)) | (u_pen_unswiz << shift));
        }
      }
    }

    return converted_data;
  }

  /**
   **********************************************************************************************
  Swizzles an image for the PS2 (4-bit image data), using SUBA swizzle
  @return 4bit swizzled data
   **********************************************************************************************
   **/
  public static int[] swizzlePS24BitSuba(int[] input_data, int width, int height) {
    return swizzleHandlerPS24BitSuba(input_data, width, height, true);
  }

  /**
   **********************************************************************************************
  Un-swizzles an image for the PS2 (4-bit image data), using SUBA swizzle
  @return 4bit unswizzled data
   **********************************************************************************************
   **/
  public static int[] unswizzlePS24BitSuba(int[] input_data, int width, int height) {
    return swizzleHandlerPS24BitSuba(input_data, width, height, false);
  }

  /**
   **********************************************************************************************
  Swizzles/Unswizzles  an image for the PS2 (4-bit image data), using Suba swizzle
  Ref: https://github.com/bartlomiejduda/ReverseBox/blob/main/reversebox/image/swizzling/swizzle_ps2_suba.py#L74
  @return 4bit swizzled/unswizzled data
   **********************************************************************************************
   **/
  private static int[] swizzleHandlerPS24BitSuba(int[] input_data, int width, int height, boolean swizzle) {

    int[] converted_data = new int[input_data.length];

    for (int y = 0; y < height; y++) {
      for (int x = 0; x < width; x++) {
        int index = y * width + x;
        int in_byte = input_data[index >> 1];
        int u_pen = (in_byte >> ((index & 1) * 4)) & 0xF;

        int pageX = x & (~0x7F);
        int pageY = y & (~0x7F);

        int pages_horz = (width + 127) / 128;
        int pages_vert = (height + 127) / 128;

        int page_number = (pageY / 128) * pages_horz + (pageX / 128);

        int page32Y = (page_number / pages_vert) * 32;
        int page32X = (page_number % pages_vert) * 64;

        int page_location = page32Y * height * 2 + page32X * 4;

        int locX = x & 0x7F;
        int locY = y & 0x7F;

        int block_location = ((locX & (~0x1F)) >> 1) * height + (locY & (~0xF)) * 2;
        int swap_selector = (((y + 2) >> 2) & 0x1) * 4;
        int posY = (((y & (~3)) >> 1) + (y & 1)) & 0x7;

        int column_location = posY * height * 2 + ((x + swap_selector) & 0x7) * 4;

        int byte_num = (x >> 3) & 3;// # 0,1,2,3
        int bits_set = (y >> 1) & 1;//  # 0, 1

        int index_out = page_location + block_location + column_location + byte_num;

        if (swizzle) {
          int out_byte = converted_data[index_out];
          out_byte = (out_byte & (~(0xF << (bits_set * 4)))) | (u_pen << (bits_set * 4));
          converted_data[index_out] = (byte) out_byte;
        }
        else {
          int in_swiz_byte = input_data[index_out];
          int u_pen_unswiz = (in_swiz_byte >> (bits_set * 4)) & 0xF;
          int out_index = index >> 1;
          int shift = (index & 1) * 4;
          int mask = 0xF << shift;
          converted_data[out_index] = (byte) ((converted_data[out_index] & (~mask)) | (u_pen_unswiz << shift));
        }
      }
    }

    return converted_data;
  }

  /**
   **********************************************************************************************
  Swizzles an image for the PS2 (8-bit image data), using SUBA swizzle
   **********************************************************************************************
   **/
  public static byte[] swizzlePS28BitSuba(byte[] input_data, int width, int height) {
    return swizzleHandlerPS28BitSuba(input_data, width, height, true);
  }

  /**
   **********************************************************************************************
  Un-swizzles an image for the PS2 (8-bit image data), using SUBA swizzle
   **********************************************************************************************
   **/
  public static byte[] unswizzlePS28BitSuba(byte[] input_data, int width, int height) {
    return swizzleHandlerPS28BitSuba(input_data, width, height, false);
  }

  /**
   **********************************************************************************************
  Swizzles/Unswizzles  an image for the PS2 (8-bit image data), using Suba swizzle
  Ref: https://github.com/bartlomiejduda/ReverseBox/blob/main/reversebox/image/swizzling/swizzle_ps2_suba.py
   **********************************************************************************************
   **/
  private static byte[] swizzleHandlerPS28BitSuba(byte[] input_data, int width, int height, boolean swizzle) {

    byte[] converted_data = new byte[input_data.length];

    for (int y = 0; y < height; y++) {
      for (int x = 0; x < width; x++) {

        int block_location = (y & (~0xf)) * width + (x & (~0xf)) * 2;
        int swap_selector = (((y + 2) >> 2) & 0x1) * 4;
        int posY = (((y & (~3)) >> 1) + (y & 1)) & 0x7;
        int column_location = posY * width * 2 + ((x + swap_selector) & 0x7) * 4;

        int byte_num = ((y >> 1) & 1) + ((x >> 2) & 2);//  # 0,1,2,3

        int index = block_location + column_location + byte_num;
        if (swizzle) {
          converted_data[index] = input_data[y * width + x];
        }
        else {
          converted_data[y * width + x] = input_data[index];
        }
      }
    }

    return converted_data;
  }

  /**
   **********************************************************************************************
  Swizzles an image for the PS2 (8-bit image data), using SUBA swizzle
   **********************************************************************************************
   **/
  public static int[] swizzlePS28BitSuba(int[] input_data, int width, int height) {
    return swizzleHandlerPS28BitSuba(input_data, width, height, true);
  }

  /**
   **********************************************************************************************
  Un-swizzles an image for the PS2 (8-bit image data), using SUBA swizzle
   **********************************************************************************************
   **/
  public static int[] unswizzlePS28BitSuba(int[] input_data, int width, int height) {
    return swizzleHandlerPS28BitSuba(input_data, width, height, false);
  }

  /**
   **********************************************************************************************
  Swizzles/Unswizzles  an image for the PS2 (8-bit image data), using Suba swizzle
  Ref: https://github.com/bartlomiejduda/ReverseBox/blob/main/reversebox/image/swizzling/swizzle_ps2_suba.py
   **********************************************************************************************
   **/
  private static int[] swizzleHandlerPS28BitSuba(int[] input_data, int width, int height, boolean swizzle) {

    int[] converted_data = new int[input_data.length];

    for (int y = 0; y < height; y++) {
      for (int x = 0; x < width; x++) {

        int block_location = (y & (~0xf)) * width + (x & (~0xf)) * 2;
        int swap_selector = (((y + 2) >> 2) & 0x1) * 4;
        int posY = (((y & (~3)) >> 1) + (y & 1)) & 0x7;
        int column_location = posY * width * 2 + ((x + swap_selector) & 0x7) * 4;

        int byte_num = ((y >> 1) & 1) + ((x >> 2) & 2);//  # 0,1,2,3

        int index = block_location + column_location + byte_num;
        if (swizzle) {
          converted_data[index] = input_data[y * width + x];
        }
        else {
          converted_data[y * width + x] = input_data[index];
        }
      }
    }

    return converted_data;
  }

  /**
   **********************************************************************************************
  Swizzles an image for the PS2 (16-bit image data), using SUBA swizzle
   **********************************************************************************************
   **/
  public static byte[] swizzlePS216BitSuba(byte[] input_data, int width, int height) {
    return swizzleHandlerPS216BitSuba(input_data, width, height, true);
  }

  /**
   **********************************************************************************************
  Un-swizzles an image for the PS2 (16-bit image data), using SUBA swizzle
   **********************************************************************************************
   **/
  public static byte[] unswizzlePS216BitSuba(byte[] input_data, int width, int height) {
    return swizzleHandlerPS216BitSuba(input_data, width, height, false);
  }

  /**
   **********************************************************************************************
  Swizzles/Unswizzles  an image for the PS2 (16-bit image data), using Suba swizzle
  Ref: https://github.com/bartlomiejduda/ReverseBox/blob/main/reversebox/image/swizzling/swizzle_ps2_suba.py
   **********************************************************************************************
   **/
  private static byte[] swizzleHandlerPS216BitSuba(byte[] input_data, int width, int height, boolean swizzle) {

    byte[] converted_data = new byte[input_data.length];

    for (int y = 0; y < height; y++) {
      for (int x = 0; x < width; x++) {

        int page_x = x & (~0x3f);
        int page_y = y & (~0x3f);

        int pages_horz = (width + 63) / 64;
        int pages_vert = (height + 63) / 64;

        int page_number = (page_y / 64) * pages_horz + (page_x / 64);

        int page32_y = (page_number / pages_vert) * 32;
        int page32_x = (page_number % pages_vert) * 64;

        int page_location = (page32_y * height + page32_x) * 2;

        int loc_x = x & 0x3f;
        int loc_y = y & 0x3f;

        int block_location = (loc_x & (~0xf)) * height + (loc_y & (~0x7)) * 2;
        int column_location = ((y & 0x7) * height + (x & 0x7)) * 2;

        int short_num = (x >> 3) & 1;//  # 0 or 1

        int dest_index = page_location + block_location + column_location + short_num;

        if (swizzle) {
          converted_data[dest_index] = input_data[y * width + x];
        }
        else {
          converted_data[y * width + x] = input_data[dest_index];
        }
      }
    }

    return converted_data;
  }

  /**
   **********************************************************************************************
  Swizzles an image for the PS2 (16-bit image data), using SUBA swizzle
   **********************************************************************************************
   **/
  public static int[] swizzlePS216BitSuba(int[] input_data, int width, int height) {
    return swizzleHandlerPS216BitSuba(input_data, width, height, true);
  }

  /**
   **********************************************************************************************
  Un-swizzles an image for the PS2 (16-bit image data), using SUBA swizzle
   **********************************************************************************************
   **/
  public static int[] unswizzlePS216BitSuba(int[] input_data, int width, int height) {
    return swizzleHandlerPS216BitSuba(input_data, width, height, false);
  }

  /**
   **********************************************************************************************
  Swizzles/Unswizzles  an image for the PS2 (16-bit image data), using Suba swizzle
  Ref: https://github.com/bartlomiejduda/ReverseBox/blob/main/reversebox/image/swizzling/swizzle_ps2_suba.py
   **********************************************************************************************
   **/
  private static int[] swizzleHandlerPS216BitSuba(int[] input_data, int width, int height, boolean swizzle) {

    int[] converted_data = new int[input_data.length];

    for (int y = 0; y < height; y++) {
      for (int x = 0; x < width; x++) {

        int page_x = x & (~0x3f);
        int page_y = y & (~0x3f);

        int pages_horz = (width + 63) / 64;
        int pages_vert = (height + 63) / 64;

        int page_number = (page_y / 64) * pages_horz + (page_x / 64);

        int page32_y = (page_number / pages_vert) * 32;
        int page32_x = (page_number % pages_vert) * 64;

        int page_location = (page32_y * height + page32_x) * 2;

        int loc_x = x & 0x3f;
        int loc_y = y & 0x3f;

        int block_location = (loc_x & (~0xf)) * height + (loc_y & (~0x7)) * 2;
        int column_location = ((y & 0x7) * height + (x & 0x7)) * 2;

        int short_num = (x >> 3) & 1;//  # 0 or 1

        int dest_index = page_location + block_location + column_location + short_num;

        if (swizzle) {
          converted_data[dest_index] = input_data[y * width + x];
        }
        else {
          converted_data[y * width + x] = input_data[dest_index];
        }
      }
    }

    return converted_data;
  }

  /**
   **********************************************************************************************
  Swizzles an image for the PSP (4bpp)
   **********************************************************************************************
   **/
  public static byte[] swizzlePSP4Bit(byte[] input, int width, int height) {
    return swizzleHandlerPSP(input, width, height, 4, true);
  }

  /**
   **********************************************************************************************
  Un-swizzles an image for the PSP (4bpp)
   **********************************************************************************************
   **/
  public static byte[] unswizzlePSP4Bit(byte[] input, int width, int height) {
    return swizzleHandlerPSP(input, width, height, 4, false);
  }

  /**
   **********************************************************************************************
  Swizzles an image for the PSP (8bpp)
   **********************************************************************************************
   **/
  public static byte[] swizzlePSP8Bit(byte[] input, int width, int height) {
    return swizzleHandlerPSP(input, width, height, 8, true);
  }

  /**
   **********************************************************************************************
  Un-swizzles an image for the PSP (8bpp)
   **********************************************************************************************
   **/
  public static byte[] unswizzlePSP8Bit(byte[] input, int width, int height) {
    return swizzleHandlerPSP(input, width, height, 8, false);
  }

  /**
   **********************************************************************************************
  Swizzles an image for the PSP (32bpp)
   **********************************************************************************************
   **/
  public static byte[] swizzlePSP32Bit(byte[] input, int width, int height) {
    return swizzleHandlerPSP(input, width, height, 32, true);
  }

  /**
   **********************************************************************************************
  Un-swizzles an image for the PSP (32bpp)
   **********************************************************************************************
   **/
  public static byte[] unswizzlePSP32Bit(byte[] input, int width, int height) {
    return swizzleHandlerPSP(input, width, height, 32, false);
  }

  /**
   **********************************************************************************************
  Swizzles an image for the PSP (8bpp)
   **********************************************************************************************
   **/
  public static byte[] swizzlePSP(byte[] input, int width, int height) {
    return swizzleHandlerPSP(input, width, height, 8, true);
  }

  /**
   **********************************************************************************************
  Un-swizzles an image for the PSP (8bpp)
   **********************************************************************************************
   **/
  public static byte[] unswizzlePSP(byte[] input, int width, int height) {
    return swizzleHandlerPSP(input, width, height, 8, false);
  }

  /**
   **********************************************************************************************
  Un-swizzles an image for the PSP
  Ref: https://github.com/bartlomiejduda/ReverseBox/blob/main/reversebox/image/swizzling/swizzle_psp.py
     **********************************************************************************************
   **/
  private static byte[] swizzleHandlerPSP(byte[] input, int width, int height, int bpp, boolean swizzle) {
    byte[] output = new byte[input.length];

    int output_offset = 0;
    int stride = width * bpp / 8;
    int row_blocks = stride / 16;

    for (int y = 0; y < height; y++) {
      for (int x = 0; x < stride; x++) {
        int block_x = x / 16;
        int block_y = y / 8;
        int block_index = block_x + (block_y * row_blocks);
        int block_address = block_index * 16 * 8;

        if (!swizzle) {
          // unswizzle
          output[output_offset] = input[block_address + (x - block_x * 16) + ((y - block_y * 8) * 16)];
        }
        else {
          // swizzle
          output[block_address + (x - block_x * 16) + ((y - block_y * 8) * 16)] = input[output_offset];
        }
        output_offset += 1;
      }
    }

    return output;
  }

  /**
   **********************************************************************************************
  Swizzles an image for the PSP
   **********************************************************************************************
   **/
  public static int[] swizzlePSP(int[] input, int width, int height) {
    return swizzleHandlerPSP(input, width, height, true);
  }

  /**
   **********************************************************************************************
  Un-swizzles an image for the PSP
   **********************************************************************************************
   **/
  public static int[] unswizzlePSP(int[] input, int width, int height) {
    return swizzleHandlerPSP(input, width, height, false);
  }

  /**
   **********************************************************************************************
  Un-swizzles an image for the PSP
  Ref: https://github.com/bartlomiejduda/ReverseBox/blob/main/reversebox/image/swizzling/swizzle_psp.py
     **********************************************************************************************
   **/
  private static int[] swizzleHandlerPSP(int[] input, int width, int height, boolean swizzle) {
    int[] output = new int[input.length];

    int output_offset = 0;
    //int stride = get_stride_value(width, bpp);
    int bpp = 8; // each pixel is stored in a single int, which is 8bpp
    int stride = width * bpp / 8;
    int row_blocks = stride / 16;

    for (int y = 0; y < height; y++) {
      for (int x = 0; x < stride; x++) {
        int block_x = x / 16;
        int block_y = y / 8;
        int block_index = block_x + (block_y * row_blocks);
        int block_address = block_index * 16 * 8;

        if (!swizzle) {
          // unswizzle
          output[output_offset] = input[block_address + (x - block_x * 16) + ((y - block_y * 8) * 16)];
        }
        else {
          // swizzle
          output[block_address + (x - block_x * 16) + ((y - block_y * 8) * 16)] = input[output_offset];
        }
        output_offset += 1;
      }
    }

    return output;
  }

  /**
   **********************************************************************************************
  Swizzles an image for the Switch
   **********************************************************************************************
   **/
  public static byte[] swizzleSwitch(byte[] bytes, int width, int height) {
    return NintendoSwitchSwizzleHelper.swizzle(bytes, width, height);
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
  Un-swizzles an image for the Nintendo Switch
  Ref: https://github.com/gdkchan/BnTxx/blob/master/BnTxx/BlockLinearSwizzle.cs
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
  Ref: https://pastebin.com/VDvs7q8Y
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
  Un-swizzles an image for the PS2 (4-bit image data)
  @param return4Bit <i>true</i> to return 4bit pixels, <i>false</i> to return 8bit pixels
   **********************************************************************************************
   **/
  /*public static byte[] unswizzlePS2_4Bit(byte[] buffer, int width, int height, boolean return4Bit) {
  
    int where = 0;
  
    byte[] InterlaceMatrix = {
        0x00, 0x10, 0x02, 0x12,
        0x11, 0x01, 0x13, 0x03,
    };
  
    int[] Matrix = { 0, 1, -1, 0 };
    int[] TileMatrix = { 4, -4 };
  
    byte[] pixels = new byte[width * height];
    byte[] newPixels = new byte[width * height];
  
    int d = 0;
    int s = where;
  
    for (int y = 0; y < height; y++) {
      for (int x = 0; x < (width >> 1); x++) {
        byte p = buffer[s++];
  
        pixels[d++] = (byte) (p & 0xF);
        pixels[d++] = (byte) (p >> 4);
      }
    }
  
    for (int y = 0; y < height; y++) {
      for (int x = 0; x < width; x++) {
        boolean oddRow = ((y & 1) != 0);
  
        int num1 = (byte) ((y / 4) & 1);
        int num2 = (byte) ((x / 4) & 1);
        int num3 = (y % 4);
  
        int num4 = ((x / 4) % 4);
  
        if (oddRow) {
          num4 += 4;
        }
  
        int num5 = ((x * 4) % 16);
        int num6 = ((x / 16) * 32);
  
        int num7 = (oddRow) ? ((y - 1) * width) : (y * width);
  
        int xx = x + num1 * TileMatrix[num2];
        int yy = y + Matrix[num3];
  
        int i = InterlaceMatrix[num4] + num5 + num6 + num7;
        int j = yy * width + xx;
  
        newPixels[j] = pixels[i];
      }
    }
  
    if (return4Bit) {
      byte[] result = new byte[width * height];
  
      s = 0;
      d = 0;
  
      for (int y = 0; y < height; y++) {
        for (int x = 0; x < (width >> 1); x++)
          result[d++] = (byte) ((newPixels[s++] & 0xF) | (newPixels[s++] << 4));
      }
      return result;
    }
    else {
      // return an 8-bit texture
      return newPixels;
    }
  
  }
  */

  /**
   **********************************************************************************************
  Un-swizzles an image for the PS2
    Ref: https://github.com/bartlomiejduda/ReverseBox/blob/main/reversebox/image/swizzling/swizzle_ps2.py
   **********************************************************************************************
   **/
  /*public static byte[] unswizzlePS2(byte[] bytes, int width, int height) {
  
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
  }*/

  /**
   **********************************************************************************************
  Un-swizzles an image for the PS2
  Ref: https://github.com/bartlomiejduda/ReverseBox/blob/main/reversebox/image/swizzling/swizzle_ps2.py
   **********************************************************************************************
   **/
  /*public static int[] unswizzlePS2(int[] bytes, int width, int height) {
  
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
  */

  /**
   **********************************************************************************************
  Swizzles an image for the PS2
   **********************************************************************************************
   **/
  /*public static byte[] swizzlePS2(byte[] bytes, int width, int height) {
  
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
  }*/

  /**
   **********************************************************************************************
  Swizzles an image for the PS2
  SAME AS OTHER METHOD, EXCEPT... the bytes are an int[] instead of a byte[]
   **********************************************************************************************
   **/
  /*public static int[] swizzlePS2(int[] bytes, int width, int height) {
  
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
  }*/

  /**
   **********************************************************************************************
  Swizzles/Unswizzles an image using BC swizzling (4-bit or 8-bit image data)
  Ref: https://github.com/bartlomiejduda/ReverseBox/blob/main/reversebox/image/swizzling/swizzle_bc.py
   **********************************************************************************************
   **/
  private static byte[] swizzleHandlerBC(byte[] input_data, int width, int height, int blockWidth, int blockHeight, int bpp, boolean swizzle) {

    int strip_size = bpp * blockWidth / 8;// strip_size: int = bpp * block_width // 8

    int imageWidth = (width + blockWidth - 1) / blockWidth * blockWidth; // _width, _height = get_storage_wh(image_width, image_height, block_width, block_height)
    int imageHeight = (height + blockHeight - 1) / blockHeight * blockHeight;

    byte[] converted_data = new byte[imageWidth * imageHeight * bpp / 8]; // unswizzled_image_data = bytearray(_width * _height * bpp // 8)

    int ptr = 0;

    try {
      for (int y = 0; y < imageHeight; y += blockHeight) {
        for (int x = 0; x < imageWidth; x += blockWidth) {
          for (int y2 = 0; y2 < blockHeight; y2++) {
            int idx = (((y + y2) * imageWidth) + x) * bpp / 8;

            if (!swizzle) {
              // unswizzle

              // unswizzled_image_data[idx: idx + strip_size] = image_data[ptr: ptr + strip_size];
              for (int s = 0; s < strip_size; s++) {
                converted_data[idx + s] = input_data[ptr + s];
              }

            }
            else {
              // swizzle

              // unswizzled_image_data[idx: idx + strip_size] = image_data[ptr: ptr + strip_size];
              for (int s = 0; s < strip_size; s++) {
                converted_data[ptr + s] = input_data[idx + s];
              }
            }

            ptr += strip_size;
          }
        }
      }
    }
    catch (Throwable t) {
      // premature EOF
    }

    return converted_data;
  }

  /**
   **********************************************************************************************
  Swizzles an image using BC swizzling (8-bit)
   **********************************************************************************************
   **/
  public static byte[] swizzleBC8Bit(byte[] input, int width, int height) {
    return swizzleHandlerBC(input, width, height, 8, 8, 8, true);
  }

  /**
   **********************************************************************************************
  Un-swizzles an image using BC swizzling (8-bit)
   **********************************************************************************************
   **/
  public static byte[] unswizzleBC8Bit(byte[] input, int width, int height) {
    return swizzleHandlerBC(input, width, height, 8, 8, 8, false);
  }

  /**
   **********************************************************************************************
  Swizzles an image using BC swizzling (4-bit)
   **********************************************************************************************
   **/
  public static byte[] swizzleBC4Bit(byte[] input, int width, int height) {
    return swizzleHandlerBC(input, width, height, 8, 8, 4, true);
  }

  /**
   **********************************************************************************************
  Un-swizzles an image using BC swizzling (4-bit)
   **********************************************************************************************
   **/
  public static byte[] unswizzleBC4Bit(byte[] input, int width, int height) {
    return swizzleHandlerBC(input, width, height, 8, 8, 4, false);
  }

  /**
   **********************************************************************************************
  Swizzles/Unswizzles an image using GameCube swizzling (4-bit/8-bit/32-bit image data)
  Ref: https://github.com/bartlomiejduda/ReverseBox/blob/61978ec4c4373010ee3af1b5489c7482b1c0c5f2/reversebox/image/swizzling/swizzle_gamecube.py#L68
   **********************************************************************************************
   **/
  private static byte[] swizzleHandlerGameCube(byte[] input_data, int width, int height, int bpp, boolean swizzle) {

    int destination_index = 0;
    byte[] converted_data = new byte[input_data.length];

    if (!swizzle) {
      // unswizzle
      for (int y = 0; y < height; y++) {
        for (int x = 0; x < width; x++) {
          int index = get_pixel_offset_gamecube(x, y, width, bpp);

          if (bpp == 4) {
            converted_data[destination_index] = input_data[index];
            if ((x & 1) == 1) {
              destination_index += 1;
            }
          }
          else if (bpp == 8) {
            converted_data[destination_index] = input_data[index];
            destination_index++;
          }
          else if (bpp == 32) {
            converted_data[destination_index] = input_data[index];
            converted_data[destination_index + 1] = input_data[index + 1];
            converted_data[destination_index + 2] = input_data[index + 32];
            converted_data[destination_index + 3] = input_data[index + 33];
            destination_index += 4;
          }
          else {
            int bytes_per_pixel = convert_bpp_to_bytes_per_pixel(bpp);

            //converted_data[destination_index: destination_index + bytes_per_pixel] = input_data[index: index + bytes_per_pixel];
            for (int b = 0; b < bytes_per_pixel; b++) {
              converted_data[destination_index + b] = input_data[index + b];
            }

            destination_index += bytes_per_pixel;
          }
        }
      }
    }
    else {
      // swizzle
      for (int y = 0; y < height; y++) {
        for (int x = 0; x < width; x++) {
          int index = get_pixel_offset_gamecube(x, y, width, bpp);

          if (bpp == 4) {
            converted_data[index] = input_data[destination_index];
            if ((x & 1) == 1) {
              destination_index += 1;
            }
          }
          else if (bpp == 8) {
            converted_data[index] = input_data[destination_index];
            destination_index++;
          }
          else if (bpp == 32) {
            converted_data[index] = input_data[destination_index];
            converted_data[index + 1] = input_data[destination_index + 1];
            converted_data[index + 2] = input_data[destination_index + 32];
            converted_data[index + 3] = input_data[destination_index + 33];
            destination_index += 4;
          }
          else {
            int bytes_per_pixel = convert_bpp_to_bytes_per_pixel(bpp);

            //converted_data[destination_index: destination_index + bytes_per_pixel] = input_data[index: index + bytes_per_pixel];
            for (int b = 0; b < bytes_per_pixel; b++) {
              converted_data[index + b] = input_data[destination_index + b];
            }

            destination_index += bytes_per_pixel;
          }
        }
      }
    }

    return converted_data;
  }

  /**
   **********************************************************************************************
  Swizzles an image using GameCube swizzling (8-bit)
   **********************************************************************************************
   **/
  public static byte[] swizzleGameCube8Bit(byte[] input, int width, int height) {
    return swizzleHandlerGameCube(input, width, height, 8, true);
  }

  /**
   **********************************************************************************************
  Un-swizzles an image using GameCube swizzling (8-bit)
   **********************************************************************************************
   **/
  public static byte[] unswizzleGameCube8Bit(byte[] input, int width, int height) {
    return swizzleHandlerGameCube(input, width, height, 8, false);
  }

  /**
   **********************************************************************************************
  Swizzles an image using GameCube swizzling (4-bit)
   **********************************************************************************************
   **/
  public static byte[] swizzleGameCube4Bit(byte[] input, int width, int height) {
    return swizzleHandlerGameCube(input, width, height, 4, true);
  }

  /**
   **********************************************************************************************
  Un-swizzles an image using GameCube swizzling (4-bit)
   **********************************************************************************************
   **/
  public static byte[] unswizzleGameCube4Bit(byte[] input, int width, int height) {
    return swizzleHandlerGameCube(input, width, height, 4, false);
  }

  /**
   **********************************************************************************************
  Swizzles an image using GameCube swizzling (32-bit)
   **********************************************************************************************
   **/
  public static byte[] swizzleGameCube32Bit(byte[] input, int width, int height) {
    return swizzleHandlerGameCube(input, width, height, 32, true);
  }

  /**
   **********************************************************************************************
  Un-swizzles an image using GameCube swizzling (32-bit)
   **********************************************************************************************
   **/
  public static byte[] unswizzleGameCube32Bit(byte[] input, int width, int height) {
    return swizzleHandlerGameCube(input, width, height, 32, false);
  }

  /**
   **********************************************************************************************
  
   **********************************************************************************************
   **/
  private static int get_pixel_offset_gamecube(int x, int y, int width, int bpp) {
    if (bpp == 32) {
      return get_pixel_offset_gamecube_bpp32(x, y, width);
    }
    else if (bpp == 15 || bpp == 16) {
      return get_pixel_offset_gamecube_bpp16(x, y, width);
    }
    else if (bpp == 8) {
      return get_pixel_offset_gamecube_bpp8(x, y, width);
    }
    else if (bpp == 4) {
      return get_pixel_offset_gamecube_bpp4(x, y, width);
    }
    else {
      return -1;
    }
  }

  /**
   **********************************************************************************************
  
   **********************************************************************************************
   **/
  private static int get_pixel_offset_gamecube_bpp32(int x, int y, int width) {
    int number_of_blocks_x = (3 + width) >> 2;
    int x_block = x >> 2;
    int y_block = y >> 2;
    int x_pix = x & 3;
    int y_pix = y & 3;
    int offset = ((y_block * number_of_blocks_x + x_block) << 6) + ((y_pix << 3) + (x_pix << 1));
    return offset;
  }

  /**
   **********************************************************************************************
  
   **********************************************************************************************
   **/
  private static int get_pixel_offset_gamecube_bpp16(int x, int y, int width) {
    int number_of_blocks_x = (3 + width) >> 2;
    int x_block = x >> 2;
    int y_block = y >> 2;
    int x_pix = x & 3;
    int y_pix = y & 3;
    int offset = ((y_block * number_of_blocks_x + x_block) << 5) + ((y_pix << 3) + (x_pix << 1));
    return offset;
  }

  /**
   **********************************************************************************************
  
   **********************************************************************************************
   **/
  private static int get_pixel_offset_gamecube_bpp8(int x, int y, int width) {
    int number_of_blocks_x = (7 + width) >> 3;
    int x_block = x >> 3;
    int y_block = y >> 2;
    int x_pix = x & 7;
    int y_pix = y & 3;
    int offset = ((y_block * number_of_blocks_x + x_block) << 5) + ((y_pix << 3) + x_pix);
    return offset;
  }

  /**
   **********************************************************************************************
  
   **********************************************************************************************
   **/
  private static int get_pixel_offset_gamecube_bpp4(int x, int y, int width) {
    int number_of_blocks_x = (7 + width) >> 3;
    int x_block = x >> 3;
    int y_block = y >> 3;
    int x_pix = x & 7;
    int y_pix = y & 7;
    int offset = ((y_block * number_of_blocks_x + x_block) << 5) + ((y_pix << 2) + (x_pix >> 1));
    return offset;
  }

  /**
   **********************************************************************************************
  
   **********************************************************************************************
   **/
  private static int convert_bpp_to_bytes_per_pixel(int bpp) {
    if (bpp <= 0) {
      return -1;
    }
    else if (bpp <= 8) {
      return 1;
    }
    else if (bpp <= 16) {
      return 2;
    }
    else if (bpp <= 24) {
      return 3;
    }
    else if (bpp <= 32) {
      return 4;
    }
    else {
      return -1;
    }
  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public ImageSwizzler() {
  }

}