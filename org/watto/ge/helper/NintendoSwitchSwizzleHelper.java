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

/**
**********************************************************************************************
Ref: https://github.com/KillzXGaming/Switch-Toolbox/blob/b673b3ee60b05830c0726e9e941e41101ba9d4e9/Switch_Toolbox_Library/Texture%20Decoding/Switch/TegraX1Swizzle.cs
**********************************************************************************************
**/
public class NintendoSwitchSwizzleHelper {

  public static byte[] unswizzle(byte[] bytes, int width, int height) {
    int depth = 1;

    int bpp = 4;

    int tileMode = 0;
    //int size_range = 4;//(int)Math.Max(0, BlockHeightLog2 - blockHeightShift);
    int size_range = GetBlockHeight(height);

    if (size_range == 16) {
      size_range = 4;
    }
    else if (size_range == 8) {
      size_range = 3;
    }
    else if (size_range == 4) {
      size_range = 2; // guess
    }
    else {
      size_range = 1; // guess
    }

    return deswizzle(width, height, depth, bpp, tileMode, size_range, bytes);
  }

  /*---------------------------------------
   * 
   * Code ported from AboodXD's BNTX Extractor https://github.com/aboood40091/BNTX-Extractor/blob/master/swizzle.py
   * 
   *---------------------------------------*/

  public static int GetBlockHeight(int height) {
    int blockHeight = pow2_round_up(height / 8);
    if (blockHeight > 16)
      blockHeight = 16;

    return blockHeight;
  }

  public static int DIV_ROUND_UP(int n, int d) {
    return (n + d - 1) / d;
  }

  public static int round_up(int x, int y) {
    return ((x - 1) | (y - 1)) + 1;
  }

  public static int pow2_round_up(int x) {
    x -= 1;
    x |= x >> 1;
    x |= x >> 2;
    x |= x >> 4;
    x |= x >> 8;
    x |= x >> 16;
    return x + 1;
  }

  private static byte[] _swizzle(int width, int height, int depth, int bpp, int tileMode, int blockHeightLog2, byte[] data, int toSwizzle) {
    try {
      //blockHeightLog2 /= 4;
      //blockHeightLog2 -= 1;
      int block_height = (int) (1 << blockHeightLog2);
      //if (height > 128) {
      //  block_height /= 2;
      //}

      int pitch;
      int surfSize;
      if (tileMode == 1) {
        pitch = width * bpp;

        //if (roundPitch == 1)
        pitch = round_up(pitch, 32);

        surfSize = pitch * height;
      }
      else {
        pitch = round_up(width * bpp, 64);
        surfSize = pitch * round_up(height, block_height * 8);
      }

      if (surfSize > data.length) {
        surfSize = data.length;
      }

      byte[] result = new byte[surfSize];

      for (int y = 0; y < height; y++) {
        for (int x = 0; x < width; x++) {
          int pos;
          int pos_;

          if (tileMode == 1)
            pos = y * pitch + x * bpp;
          else
            pos = getAddrBlockLinear(x, y, width, bpp, 0, block_height);

          pos_ = (y * width + x) * bpp;

          if (pos + bpp <= surfSize) {
            if (toSwizzle == 0)
              System.arraycopy(data, pos, result, pos_, bpp);
            else
              System.arraycopy(data, pos_, result, pos, bpp);
          }
        }
      }
      return result;
    }
    catch (Throwable t) {
      return data;
    }
  }

  public static byte[] deswizzle(int width, int height, int depth, int bpp, int tileMode, int size_range, byte[] data) {
    //return _swizzle(width, height, depth, blkWidth, blkHeight, blkDepth, roundPitch, bpp, tileMode, size_range, data, 0);
    return _swizzle(width, height, depth, bpp, tileMode, size_range, data, 0);
  }

  public static byte[] swizzle(int width, int height, int depth, int bpp, int tileMode, int size_range, byte[] data) {
    //return _swizzle(width, height, depth, blkWidth, blkHeight, blkDepth, roundPitch, bpp, tileMode, size_range, data, 1);
    return _swizzle(width, height, depth, bpp, tileMode, size_range, data, 1);
  }

  static int getAddrBlockLinear(int x, int y, int width, int bytes_per_pixel, int base_address, int block_height) {
    /*
      From Tega X1 TRM 
                       */
    int image_width_in_gobs = DIV_ROUND_UP(width * bytes_per_pixel, 64);

    int GOB_address = (base_address
        + (y / (8 * block_height)) * 512 * block_height * image_width_in_gobs
        + (x * bytes_per_pixel / 64) * 512 * block_height
        + (y % (8 * block_height) / 8) * 512);

    x *= bytes_per_pixel;

    int Address = (GOB_address + ((x % 64) / 32) * 256 + ((y % 8) / 2) * 64
        + ((x % 32) / 16) * 32 + (y % 2) * 16 + (x % 16));
    return Address;
  }
}
