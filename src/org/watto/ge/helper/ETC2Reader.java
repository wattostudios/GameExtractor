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
ETC2 Decoder
**********************************************************************************************
**/
public class ETC2Reader {

  /*
   * Decode texture function (linear). Decode an entire texture into a single
   * image buffer, with pixels stored row-by-row, converting into the given pixel
   * format.

  bool detexDecompressTextureLinear(const detexTexture *texture,
  uint8_t * DETEX_RESTRICT pixel_buffer, uint32_t pixel_format) {
      uint8_t block_buffer[DETEX_MAX_BLOCK_SIZE];
      if (!detexFormatIsCompressed(texture->format)) {
          return detexConvertPixels(texture->data, texture->width * texture->height,
              detexGetPixelFormat(texture->format), pixel_buffer, pixel_format);
      }
      const uint8_t *data = texture->data;
      int pixel_size = detexGetPixelSize(pixel_format);
      bool result = true;
      for (int y = 0; y < texture->height_in_blocks; y++) {
          int nu_rows;
          if (y * 4 + 3 >= texture->height)
              nu_rows = texture->height - y * 4;
          else
              nu_rows = 4;
          for (int x = 0; x < texture->width_in_blocks; x++) {
              bool r = detexDecompressBlock(data, texture->format,
                  DETEX_MODE_MASK_ALL, 0, block_buffer, pixel_format);
              uint32_t block_size = detexGetPixelSize(pixel_format) * 16;
              if (!r) {
                  result = false;
                  memset(block_buffer, 0, block_size);
              }
              uint8_t *pixelp = pixel_buffer +
                  y * 4 * texture->width * pixel_size +
                  + x * 4 * pixel_size;
              int nu_columns;
              if (x * 4 + 3  >= texture->width)
                  nu_columns = texture->width - x * 4;
              else
                  nu_columns = 4;
              for (int row = 0; row < nu_rows; row++)
                  memcpy(pixelp + row * texture->width * pixel_size,
                      block_buffer + row * 4 * pixel_size,
                      nu_columns * pixel_size);
              data += detexGetCompressedBlockSize(texture->format);
          }
      }
      return result;
  }*/

  int[] complement3bitshifted_table = new int[] { 0, 8, 16, 24, -32, -24, -16, -8 };

  int[][] modifier_table = new int[][] {
      { 2, 8, -2, -8 },
      { 5, 17, -5, -17 },
      { 9, 29, -9, -29 },
      { 13, 42, -13, -42 },
      { 18, 60, -18, -60 },
      { 24, 80, -24, -80 },
      { 33, 106, -33, -106 },
      { 47, 183, -47, -183 }
  };

  int[] detex_clamp0to255_table = new int[] {
      0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
      0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
      0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
      0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
      0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
      0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
      0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
      0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
      0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
      0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
      0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
      0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
      0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
      0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
      0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
      0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
      1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16,
      17, 18, 19, 20, 21, 22, 23, 24, 25, 26, 27, 28, 29, 30, 31, 32,
      33, 34, 35, 36, 37, 38, 39, 40, 41, 42, 43, 44, 45, 46, 47, 48,
      49, 50, 51, 52, 53, 54, 55, 56, 57, 58, 59, 60, 61, 62, 63, 64,
      65, 66, 67, 68, 69, 70, 71, 72, 73, 74, 75, 76, 77, 78, 79, 80,
      81, 82, 83, 84, 85, 86, 87, 88, 89, 90, 91, 92, 93, 94, 95, 96,
      97, 98, 99, 100, 101, 102, 103, 104, 105, 106, 107, 108, 109, 110, 111, 112,
      113, 114, 115, 116, 117, 118, 119, 120, 121, 122, 123, 124, 125, 126, 127, 128,
      129, 130, 131, 132, 133, 134, 135, 136, 137, 138, 139, 140, 141, 142, 143, 144,
      145, 146, 147, 148, 149, 150, 151, 152, 153, 154, 155, 156, 157, 158, 159, 160,
      161, 162, 163, 164, 165, 166, 167, 168, 169, 170, 171, 172, 173, 174, 175, 176,
      177, 178, 179, 180, 181, 182, 183, 184, 185, 186, 187, 188, 189, 190, 191, 192,
      193, 194, 195, 196, 197, 198, 199, 200, 201, 202, 203, 204, 205, 206, 207, 208,
      209, 210, 211, 212, 213, 214, 215, 216, 217, 218, 219, 220, 221, 222, 223, 224,
      225, 226, 227, 228, 229, 230, 231, 232, 233, 234, 235, 236, 237, 238, 239, 240,
      241, 242, 243, 244, 245, 246, 247, 248, 249, 250, 251, 252, 253, 254, 255, 255,
      255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255,
      255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255,
      255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255,
      255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255,
      255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255,
      255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255,
      255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255,
      255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255,
      255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255,
      255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255,
      255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255,
      255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255,
      255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255,
      255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255,
      255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255,
      255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255
  };

  int DETEX_MODE_MASK_ETC_INDIVIDUAL = 0x1,
      DETEX_MODE_MASK_ETC_DIFFERENTIAL = 0x2,
      DETEX_MODE_MASK_ETC_T = 0x4,
      DETEX_MODE_MASK_ETC_H = 0x8,
      DETEX_MODE_MASK_ETC_PLANAR = 0x10,
      DETEX_MODE_MASK_ALL_MODES_ETC1 = 0x3,
      DETEX_MODE_MASK_ALL_MODES_ETC2 = 0x1F,
      DETEX_MODE_MASK_ALL_MODES_ETC2_PUNCHTHROUGH = 0X1E,
      DETEX_MODE_MASK_ALL_MODES_BPTC = 0xFF,
      DETEX_MODE_MASK_ALL_MODES_BPTC_FLOAT = 0x3FFF,
      DETEX_MODE_MASK_ALL = 0XFFFFFFFF;

  int[] etc2_distance_table = new int[] { 3, 6, 11, 16, 23, 32, 41, 64 };

  int[][] punchthrough_modifier_table = new int[][] {
      { 0, 8, 0, -8 },
      { 0, 17, 0, -17 },
      { 0, 29, 0, -29 },
      { 0, 42, 0, -42 },
      { 0, 60, 0, -60 },
      { 0, 80, 0, -80 },
      { 0, 106, 0, -106 },
      { 0, 183, 0, -183 }
  };

  int[] punchthrough_mask_table = new int[] { 0xFFFFFFFF, 0xFFFFFFFF, 0x00000000, 0xFFFFFFFF };

  /* For compression formats that have opaque and non-opaque modes, */
  /* return false (invalid block) when the compressed block is encoded */
  /* using a non-opaque mode. */
  int DETEX_DECOMPRESS_FLAG_OPAQUE_ONLY = 0x2;

  /* For compression formats that have opaque and non-opaque modes, */
  /* return false (invalid block) when the compressed block is encoded */
  /* using an opaque mode. */
  int DETEX_DECOMPRESS_FLAG_NON_OPAQUE_ONLY = 0x4;

  public int clamp1023_signed(int x) {
    if (x < -1023) {
      return -1023;
    }
    if (x > 1023) {
      return 1023;
    }
    return x;
  }

  public int clamp2047(int x) {
    if (x < 0) {
      return 0;
    }
    if (x > 2047) {
      return 2047;
    }
    return x;
  }

  public int complement3bit(int x) {
    if ((x & 4) == 4) {
      return ((x & 3) - 4);
    }
    return x;
  }

  // This function calculates the 3-bit complement value in the range -4 to 3 of a three bit
  // representation. The result is arithmetically shifted 3 places to the left before returning.
  public int complement3bitshifted(int x) {
    return complement3bitshifted_table[x];
  }

  public int complement3bitshifted_slow(int x) {
    if ((x & 4) == 4) {
      return ((x & 3) - 4) << 3;  // Note: shift is arithmetic.
    }
    return x << 3;
  }

  /* Clamp an integer value in the range -255 to 511 to the the range 0 to 255. */
  public int detexClamp0To255(int x) {
    return detex_clamp0to255_table[x + 255];
  }

  /* Decompress a 64-bit 4x4 pixel texture block compressed using the ETC1 */
  /* format. */
  public boolean detexDecompressBlockETC1(int[] bitstring, int mode_mask, int flags, int[] pixel_buffer) {
    int differential_mode = bitstring[3] & 2;
    //if (differential_mode) {
    if (differential_mode != 0) {
      if ((mode_mask & DETEX_MODE_MASK_ETC_DIFFERENTIAL) == 0) {
        return false;
      }
    }
    else if ((mode_mask & DETEX_MODE_MASK_ETC_INDIVIDUAL) == 0) {
      return false;
    }
    int flipbit = bitstring[3] & 1;
    int[] base_color_subblock1 = new int[3];
    int[] base_color_subblock2 = new int[3];
    //if (differential_mode) {
    if (differential_mode != 0) {
      base_color_subblock1[0] = (bitstring[0] & 0xF8);
      base_color_subblock1[0] |= ((base_color_subblock1[0] & 224) >> 5);
      base_color_subblock1[1] = (bitstring[1] & 0xF8);
      base_color_subblock1[1] |= (base_color_subblock1[1] & 224) >> 5;
      base_color_subblock1[2] = (bitstring[2] & 0xF8);
      base_color_subblock1[2] |= (base_color_subblock1[2] & 224) >> 5;
      base_color_subblock2[0] = (bitstring[0] & 0xF8);            // 5 highest order bits.
      base_color_subblock2[0] += complement3bitshifted(bitstring[0] & 7); // Add difference.
      if ((base_color_subblock2[0] & 0xFF07) == 0xFF07) {
        return false;
      }
      base_color_subblock2[0] |= (base_color_subblock2[0] & 224) >> 5;    // Replicate.
      base_color_subblock2[1] = (bitstring[1] & 0xF8);
      base_color_subblock2[1] += complement3bitshifted(bitstring[1] & 7);
      if ((base_color_subblock2[1] & 0xFF07) == 0xFF07) {
        return false;
      }
      base_color_subblock2[1] |= (base_color_subblock2[1] & 224) >> 5;
      base_color_subblock2[2] = (bitstring[2] & 0xF8);
      base_color_subblock2[2] += complement3bitshifted(bitstring[2] & 7);
      if ((base_color_subblock2[2] & 0xFF07) == 0xFF07) {
        return false;
      }
      base_color_subblock2[2] |= (base_color_subblock2[2] & 224) >> 5;
    }
    else {
      base_color_subblock1[0] = (bitstring[0] & 0xF0);
      base_color_subblock1[0] |= base_color_subblock1[0] >> 4;
      base_color_subblock1[1] = (bitstring[1] & 0xF0);
      base_color_subblock1[1] |= base_color_subblock1[1] >> 4;
      base_color_subblock1[2] = (bitstring[2] & 0xF0);
      base_color_subblock1[2] |= base_color_subblock1[2] >> 4;
      base_color_subblock2[0] = (bitstring[0] & 0x0F);
      base_color_subblock2[0] |= base_color_subblock2[0] << 4;
      base_color_subblock2[1] = (bitstring[1] & 0x0F);
      base_color_subblock2[1] |= base_color_subblock2[1] << 4;
      base_color_subblock2[2] = (bitstring[2] & 0x0F);
      base_color_subblock2[2] |= base_color_subblock2[2] << 4;
    }
    int table_codeword1 = (bitstring[3] & 224) >> 5;
    int table_codeword2 = (bitstring[3] & 28) >> 2;
    int pixel_index_word = (bitstring[4] << 24) | (bitstring[5] << 16) | (bitstring[6] << 8) | bitstring[7];
    if (flipbit == 0) {
      ProcessPixelETC1(0, pixel_index_word, table_codeword1, base_color_subblock1, pixel_buffer);
      ProcessPixelETC1(1, pixel_index_word, table_codeword1, base_color_subblock1, pixel_buffer);
      ProcessPixelETC1(2, pixel_index_word, table_codeword1, base_color_subblock1, pixel_buffer);
      ProcessPixelETC1(3, pixel_index_word, table_codeword1, base_color_subblock1, pixel_buffer);
      ProcessPixelETC1(4, pixel_index_word, table_codeword1, base_color_subblock1, pixel_buffer);
      ProcessPixelETC1(5, pixel_index_word, table_codeword1, base_color_subblock1, pixel_buffer);
      ProcessPixelETC1(6, pixel_index_word, table_codeword1, base_color_subblock1, pixel_buffer);
      ProcessPixelETC1(7, pixel_index_word, table_codeword1, base_color_subblock1, pixel_buffer);
      ProcessPixelETC1(8, pixel_index_word, table_codeword2, base_color_subblock2, pixel_buffer);
      ProcessPixelETC1(9, pixel_index_word, table_codeword2, base_color_subblock2, pixel_buffer);
      ProcessPixelETC1(10, pixel_index_word, table_codeword2, base_color_subblock2, pixel_buffer);
      ProcessPixelETC1(11, pixel_index_word, table_codeword2, base_color_subblock2, pixel_buffer);
      ProcessPixelETC1(12, pixel_index_word, table_codeword2, base_color_subblock2, pixel_buffer);
      ProcessPixelETC1(13, pixel_index_word, table_codeword2, base_color_subblock2, pixel_buffer);
      ProcessPixelETC1(14, pixel_index_word, table_codeword2, base_color_subblock2, pixel_buffer);
      ProcessPixelETC1(15, pixel_index_word, table_codeword2, base_color_subblock2, pixel_buffer);
    }
    else {
      ProcessPixelETC1(0, pixel_index_word, table_codeword1, base_color_subblock1, pixel_buffer);
      ProcessPixelETC1(1, pixel_index_word, table_codeword1, base_color_subblock1, pixel_buffer);
      ProcessPixelETC1(2, pixel_index_word, table_codeword2, base_color_subblock2, pixel_buffer);
      ProcessPixelETC1(3, pixel_index_word, table_codeword2, base_color_subblock2, pixel_buffer);
      ProcessPixelETC1(4, pixel_index_word, table_codeword1, base_color_subblock1, pixel_buffer);
      ProcessPixelETC1(5, pixel_index_word, table_codeword1, base_color_subblock1, pixel_buffer);
      ProcessPixelETC1(6, pixel_index_word, table_codeword2, base_color_subblock2, pixel_buffer);
      ProcessPixelETC1(7, pixel_index_word, table_codeword2, base_color_subblock2, pixel_buffer);
      ProcessPixelETC1(8, pixel_index_word, table_codeword1, base_color_subblock1, pixel_buffer);
      ProcessPixelETC1(9, pixel_index_word, table_codeword1, base_color_subblock1, pixel_buffer);
      ProcessPixelETC1(10, pixel_index_word, table_codeword2, base_color_subblock2, pixel_buffer);
      ProcessPixelETC1(11, pixel_index_word, table_codeword2, base_color_subblock2, pixel_buffer);
      ProcessPixelETC1(12, pixel_index_word, table_codeword1, base_color_subblock1, pixel_buffer);
      ProcessPixelETC1(13, pixel_index_word, table_codeword1, base_color_subblock1, pixel_buffer);
      ProcessPixelETC1(14, pixel_index_word, table_codeword2, base_color_subblock2, pixel_buffer);
      ProcessPixelETC1(15, pixel_index_word, table_codeword2, base_color_subblock2, pixel_buffer);
    }
    return true;
  }

  /* Decompress a 64-bit 4x4 pixel texture block compressed using the ETC2 */
  /* format. */
  public boolean detexDecompressBlockETC2(int[] bitstring, int mode_mask, int flags, int[] pixel_buffer) {
    // Figure out the mode.
    if ((bitstring[3] & 2) == 0) {
      // Individual mode.
      return detexDecompressBlockETC1(bitstring, mode_mask, flags,
          pixel_buffer);
    }
    if ((mode_mask & (~DETEX_MODE_MASK_ETC_INDIVIDUAL)) == 0) {
      return false;
    }
    int R = (bitstring[0] & 0xF8);
    R += complement3bitshifted(bitstring[0] & 7);
    int G = (bitstring[1] & 0xF8);
    G += complement3bitshifted(bitstring[1] & 7);
    int B = (bitstring[2] & 0xF8);
    B += complement3bitshifted(bitstring[2] & 7);
    if ((R & 0xFF07) == 0xFF07) {
      // T mode.
      if ((mode_mask & DETEX_MODE_MASK_ETC_T) == 0) {
        return false;
      }
      ProcessBlockETC2TOrHMode(bitstring, DETEX_MODE_MASK_ETC_T,
          pixel_buffer);
      return true;
    }
    else if ((G & 0xFF07) == 0xFF07) {
      // H mode.
      if ((mode_mask & DETEX_MODE_MASK_ETC_H) == 0) {
        return false;
      }
      ProcessBlockETC2TOrHMode(bitstring, DETEX_MODE_MASK_ETC_H,
          pixel_buffer);
      return true;
    }
    else if ((B & 0xFF07) == 0xFF07) {
      // Planar mode.
      if ((mode_mask & DETEX_MODE_MASK_ETC_PLANAR) == 0) {
        return false;
      }
      ProcessBlockETC2PlanarMode(bitstring, pixel_buffer);
      return true;
    }
    else {
      // Differential mode.
      return detexDecompressBlockETC1(bitstring, mode_mask, flags,
          pixel_buffer);
    }
  }

  /* Decompress a 64-bit 4x4 pixel texture block compressed using the */
  /* ETC2_PUNCHTROUGH format. */
  public boolean detexDecompressBlockETC2_PUNCHTHROUGH(int[] bitstring, int mode_mask, int flags, int[] pixel_buffer) {
    int R = (bitstring[0] & 0xF8);
    R += complement3bitshifted(bitstring[0] & 7);
    int G = (bitstring[1] & 0xF8);
    G += complement3bitshifted(bitstring[1] & 7);
    int B = (bitstring[2] & 0xF8);
    B += complement3bitshifted(bitstring[2] & 7);
    int opaque = bitstring[3] & 2;
    //if (opaque && (flags & DETEX_DECOMPRESS_FLAG_NON_OPAQUE_ONLY)) {
    if ((opaque != 0) && ((flags & DETEX_DECOMPRESS_FLAG_NON_OPAQUE_ONLY) == DETEX_DECOMPRESS_FLAG_NON_OPAQUE_ONLY)) {
      return false;
    }
    //if (!opaque && (flags & DETEX_DECOMPRESS_FLAG_OPAQUE_ONLY)) {
    if ((opaque == 0) && ((flags & DETEX_DECOMPRESS_FLAG_OPAQUE_ONLY) == DETEX_DECOMPRESS_FLAG_OPAQUE_ONLY)) {
      return false;
    }
    if ((R & 0xFF07) == 0xFF07) {
      // T mode.
      if ((mode_mask & DETEX_MODE_MASK_ETC_T) == 0) {
        return false;
      }
      if (opaque != 0) {
        ProcessBlockETC2TOrHMode(bitstring, DETEX_MODE_MASK_ETC_T,
            pixel_buffer);
        return true;
      }
      // T mode with punchthrough alpha.
      ProcessBlockETC2PunchthroughTOrHMode(bitstring,
          DETEX_MODE_MASK_ETC_T, pixel_buffer);
      return true;
    }
    else if ((G & 0xFF07) == 0xFF07) {
      // H mode.
      if ((mode_mask & DETEX_MODE_MASK_ETC_H) == 0) {
        return false;
      }
      if (opaque != 0) {
        ProcessBlockETC2TOrHMode(bitstring, DETEX_MODE_MASK_ETC_H,
            pixel_buffer);
        return true;
      }
      // H mode with punchthrough alpha.
      ProcessBlockETC2PunchthroughTOrHMode(bitstring, DETEX_MODE_MASK_ETC_H,
          pixel_buffer);
      return true;
    }
    else if ((B & 0xFF07) == 0xFF07) {
      // Planar mode.
      if ((mode_mask & DETEX_MODE_MASK_ETC_PLANAR) == 0) {
        return false;
      }
      // Opaque always set.
      if ((flags & DETEX_DECOMPRESS_FLAG_NON_OPAQUE_ONLY) == DETEX_DECOMPRESS_FLAG_NON_OPAQUE_ONLY) {
        return false;
      }
      ProcessBlockETC2PlanarMode(bitstring, pixel_buffer);
      return true;
    }
    else {
      // Differential mode.
      if (opaque != 0) {
        return detexDecompressBlockETC1(bitstring, mode_mask, flags,
            pixel_buffer);
      }
      // Differential mode with punchthrough alpha.
      if ((mode_mask & DETEX_MODE_MASK_ETC_DIFFERENTIAL) == 0) {
        return false;
      }
      ProcessBlockETC2PunchthroughDifferentialMode(bitstring, pixel_buffer);
      return true;
    }
  }

  /* Return the internal mode of a ETC1 block. */
  public int detexGetModeETC1(int[] bitstring) {
    // Figure out the mode.
    if ((bitstring[3] & 2) == 0) {
      // Individual mode.
      return 0;
    }
    else {
      return 1;
    }
  }

  /* Return the internal mode of a ETC2 block. */
  public int detexGetModeETC2(int[] bitstring) {
    // Figure out the mode.
    if ((bitstring[3] & 2) == 0) {
      return 0;
    }
    int R = (bitstring[0] & 0xF8);
    R += complement3bitshifted(bitstring[0] & 7);
    int G = (bitstring[1] & 0xF8);
    G += complement3bitshifted(bitstring[1] & 7);
    int B = (bitstring[2] & 0xF8);
    B += complement3bitshifted(bitstring[2] & 7);
    if ((R & 0xFF07) == 0xFF07) {
      return 2;
    }
    else if ((G & 0xFF07) == 0xFF07) {
      return 3;
    }
    else if ((B & 0xFF07) == 0xFF07) {
      return 4;
    }
    else {
      return 1;
    }
  }

  /* Return the internal mode of a ETC2_PUNCHTROUGH block. */
  public int detexGetModeETC2_PUNCHTHROUGH(int[] bitstring) {
    // Figure out the mode.
    //    int opaque = bitstring[3] & 2;
    int R = (bitstring[0] & 0xF8);
    R += complement3bitshifted(bitstring[0] & 7);
    int G = (bitstring[1] & 0xF8);
    G += complement3bitshifted(bitstring[1] & 7);
    int B = (bitstring[2] & 0xF8);
    B += complement3bitshifted(bitstring[2] & 7);
    if ((R & 0xFF07) == 0xFF07) {
      return 2;
    }
    else if ((G & 0xFF07) == 0xFF07) {
      return 3;
    }
    else if ((B & 0xFF07) == 0xFF07) {
      return 4;
    }
    else {
      return 1;
    }
  }

  public int detexPack32RGB8Alpha0xFF(int r, int g, int b) {
    return detexPack32RGBA8(r, g, b, 0xFF);
  }

  public int detexPack32RGBA8(int r, int g, int b, int a) {
    //return r | (g << 8) | (b << 16) | (a << 24);
    return ((a << 24) | (r << 16) | (g << 8) | b);
  }

  public void detexSetModeETC1(int[] bitstring, int mode, int flags, int[] colors) {
    if (mode == 0) {
      bitstring[3] &= ~0x2;
    }
    else {
      bitstring[3] |= 0x2;
    }
  }

  public void detexSetModeETC2(int[] bitstring, int mode, int flags, int[] colors) {
    if (mode == 0) {
      // Set Individual mode.
      bitstring[3] &= ~0x2;
    }
    else {
      // Set Differential, T, H or P mode.
      bitstring[3] |= 0x2;
      SetModeETC2THP(bitstring, mode);
    }
  }

  public void detexSetModeETC2_PUNCHTHROUGH(int[] bitstring, int mode, int flags, int[] colors) {
    if ((flags & DETEX_DECOMPRESS_FLAG_NON_OPAQUE_ONLY) == DETEX_DECOMPRESS_FLAG_NON_OPAQUE_ONLY) {
      bitstring[3] &= ~0x2;
    }
    if ((flags & DETEX_DECOMPRESS_FLAG_OPAQUE_ONLY) == DETEX_DECOMPRESS_FLAG_OPAQUE_ONLY) {
      bitstring[3] |= 0x2;
    }
    SetModeETC2THP(bitstring, flags);
  }

  public void ProcessBlockETC2PlanarMode(int[] bitstring, int[] pixel_buffer) {
    // Each color O, H and V is in 6-7-6 format.
    int RO = (bitstring[0] & 0x7E) >> 1;
    int GO = ((bitstring[0] & 0x1) << 6) | ((bitstring[1] & 0x7E) >> 1);
    int BO = ((bitstring[1] & 0x1) << 5) | (bitstring[2] & 0x18) | ((bitstring[2] & 0x03) << 1) |
        ((bitstring[3] & 0x80) >> 7);
    int RH = ((bitstring[3] & 0x7C) >> 1) | (bitstring[3] & 0x1);
    int GH = (bitstring[4] & 0xFE) >> 1;
    int BH = ((bitstring[4] & 0x1) << 5) | ((bitstring[5] & 0xF8) >> 3);
    int RV = ((bitstring[5] & 0x7) << 3) | ((bitstring[6] & 0xE0) >> 5);
    int GV = ((bitstring[6] & 0x1F) << 2) | ((bitstring[7] & 0xC0) >> 6);
    int BV = bitstring[7] & 0x3F;
    RO = (RO << 2) | ((RO & 0x30) >> 4);    // Replicate bits.
    GO = (GO << 1) | ((GO & 0x40) >> 6);
    BO = (BO << 2) | ((BO & 0x30) >> 4);
    RH = (RH << 2) | ((RH & 0x30) >> 4);
    GH = (GH << 1) | ((GH & 0x40) >> 6);
    BH = (BH << 2) | ((BH & 0x30) >> 4);
    RV = (RV << 2) | ((RV & 0x30) >> 4);
    GV = (GV << 1) | ((GV & 0x40) >> 6);
    BV = (BV << 2) | ((BV & 0x30) >> 4);
    //uint32_t *buffer = (uint32_t *)pixel_buffer;
    int[] buffer = pixel_buffer;
    for (int y = 0; y < 4; y++) {
      for (int x = 0; x < 4; x++) {
        int r = detexClamp0To255((x * (RH - RO) + y * (RV - RO) + 4 * RO + 2) >> 2);
        int g = detexClamp0To255((x * (GH - GO) + y * (GV - GO) + 4 * GO + 2) >> 2);
        int b = detexClamp0To255((x * (BH - BO) + y * (BV - BO) + 4 * BO + 2) >> 2);
        buffer[y * 4 + x] = detexPack32RGB8Alpha0xFF(r, g, b);
      }
    }
  }

  public void ProcessBlockETC2PunchthroughDifferentialMode(int[] bitstring, int[] pixel_buffer) {
    int flipbit = bitstring[3] & 1;
    int[] base_color_subblock1 = new int[3];
    int[] base_color_subblock2 = new int[3];
    base_color_subblock1[0] = (bitstring[0] & 0xF8);
    base_color_subblock1[0] |= ((base_color_subblock1[0] & 224) >> 5);
    base_color_subblock1[1] = (bitstring[1] & 0xF8);
    base_color_subblock1[1] |= (base_color_subblock1[1] & 224) >> 5;
    base_color_subblock1[2] = (bitstring[2] & 0xF8);
    base_color_subblock1[2] |= (base_color_subblock1[2] & 224) >> 5;
    base_color_subblock2[0] = (bitstring[0] & 0xF8);                // 5 highest order bits.
    base_color_subblock2[0] += complement3bitshifted(bitstring[0] & 7); // Add difference.
    base_color_subblock2[0] |= (base_color_subblock2[0] & 224) >> 5;        // Replicate.
    base_color_subblock2[1] = (bitstring[1] & 0xF8);
    base_color_subblock2[1] += complement3bitshifted(bitstring[1] & 7);
    base_color_subblock2[1] |= (base_color_subblock2[1] & 224) >> 5;
    base_color_subblock2[2] = (bitstring[2] & 0xF8);
    base_color_subblock2[2] += complement3bitshifted(bitstring[2] & 7);
    base_color_subblock2[2] |= (base_color_subblock2[2] & 224) >> 5;
    int table_codeword1 = (bitstring[3] & 224) >> 5;
    int table_codeword2 = (bitstring[3] & 28) >> 2;
    int pixel_index_word = (bitstring[4] << 24) | (bitstring[5] << 16) | (bitstring[6] << 8) | bitstring[7];
    if (flipbit == 0) {
      ProcessPixelETC2Punchthrough(0, pixel_index_word, table_codeword1, base_color_subblock1, pixel_buffer);
      ProcessPixelETC2Punchthrough(1, pixel_index_word, table_codeword1, base_color_subblock1, pixel_buffer);
      ProcessPixelETC2Punchthrough(2, pixel_index_word, table_codeword1, base_color_subblock1, pixel_buffer);
      ProcessPixelETC2Punchthrough(3, pixel_index_word, table_codeword1, base_color_subblock1, pixel_buffer);
      ProcessPixelETC2Punchthrough(4, pixel_index_word, table_codeword1, base_color_subblock1, pixel_buffer);
      ProcessPixelETC2Punchthrough(5, pixel_index_word, table_codeword1, base_color_subblock1, pixel_buffer);
      ProcessPixelETC2Punchthrough(6, pixel_index_word, table_codeword1, base_color_subblock1, pixel_buffer);
      ProcessPixelETC2Punchthrough(7, pixel_index_word, table_codeword1, base_color_subblock1, pixel_buffer);
      ProcessPixelETC2Punchthrough(8, pixel_index_word, table_codeword2, base_color_subblock2, pixel_buffer);
      ProcessPixelETC2Punchthrough(9, pixel_index_word, table_codeword2, base_color_subblock2, pixel_buffer);
      ProcessPixelETC2Punchthrough(10, pixel_index_word, table_codeword2, base_color_subblock2, pixel_buffer);
      ProcessPixelETC2Punchthrough(11, pixel_index_word, table_codeword2, base_color_subblock2, pixel_buffer);
      ProcessPixelETC2Punchthrough(12, pixel_index_word, table_codeword2, base_color_subblock2, pixel_buffer);
      ProcessPixelETC2Punchthrough(13, pixel_index_word, table_codeword2, base_color_subblock2, pixel_buffer);
      ProcessPixelETC2Punchthrough(14, pixel_index_word, table_codeword2, base_color_subblock2, pixel_buffer);
      ProcessPixelETC2Punchthrough(15, pixel_index_word, table_codeword2, base_color_subblock2, pixel_buffer);
    }
    else {
      ProcessPixelETC2Punchthrough(0, pixel_index_word, table_codeword1, base_color_subblock1, pixel_buffer);
      ProcessPixelETC2Punchthrough(1, pixel_index_word, table_codeword1, base_color_subblock1, pixel_buffer);
      ProcessPixelETC2Punchthrough(2, pixel_index_word, table_codeword2, base_color_subblock2, pixel_buffer);
      ProcessPixelETC2Punchthrough(3, pixel_index_word, table_codeword2, base_color_subblock2, pixel_buffer);
      ProcessPixelETC2Punchthrough(4, pixel_index_word, table_codeword1, base_color_subblock1, pixel_buffer);
      ProcessPixelETC2Punchthrough(5, pixel_index_word, table_codeword1, base_color_subblock1, pixel_buffer);
      ProcessPixelETC2Punchthrough(6, pixel_index_word, table_codeword2, base_color_subblock2, pixel_buffer);
      ProcessPixelETC2Punchthrough(7, pixel_index_word, table_codeword2, base_color_subblock2, pixel_buffer);
      ProcessPixelETC2Punchthrough(8, pixel_index_word, table_codeword1, base_color_subblock1, pixel_buffer);
      ProcessPixelETC2Punchthrough(9, pixel_index_word, table_codeword1, base_color_subblock1, pixel_buffer);
      ProcessPixelETC2Punchthrough(10, pixel_index_word, table_codeword2, base_color_subblock2, pixel_buffer);
      ProcessPixelETC2Punchthrough(11, pixel_index_word, table_codeword2, base_color_subblock2, pixel_buffer);
      ProcessPixelETC2Punchthrough(12, pixel_index_word, table_codeword1, base_color_subblock1, pixel_buffer);
      ProcessPixelETC2Punchthrough(13, pixel_index_word, table_codeword1, base_color_subblock1, pixel_buffer);
      ProcessPixelETC2Punchthrough(14, pixel_index_word, table_codeword2, base_color_subblock2, pixel_buffer);
      ProcessPixelETC2Punchthrough(15, pixel_index_word, table_codeword2, base_color_subblock2, pixel_buffer);
    }
  }

  public void ProcessBlockETC2PunchthroughTOrHMode(int[] bitstring, int mode, int[] pixel_buffer) {
    int base_color1_R, base_color1_G, base_color1_B;
    int base_color2_R, base_color2_G, base_color2_B;
    int[] paint_color_R = new int[4];
    int[] paint_color_G = new int[4];
    int[] paint_color_B = new int[4];
    int distance;
    if (mode == DETEX_MODE_MASK_ETC_T) {
      // T mode.
      base_color1_R = ((bitstring[0] & 0x18) >> 1) | (bitstring[0] & 0x3);
      base_color1_R |= base_color1_R << 4;
      base_color1_G = bitstring[1] & 0xF0;
      base_color1_G |= base_color1_G >> 4;
      base_color1_B = bitstring[1] & 0x0F;
      base_color1_B |= base_color1_B << 4;
      base_color2_R = bitstring[2] & 0xF0;
      base_color2_R |= base_color2_R >> 4;
      base_color2_G = bitstring[2] & 0x0F;
      base_color2_G |= base_color2_G << 4;
      base_color2_B = bitstring[3] & 0xF0;
      base_color2_B |= base_color2_B >> 4;
      // index = (da << 1) | db
      distance = etc2_distance_table[((bitstring[3] & 0x0C) >> 1) | (bitstring[3] & 0x1)];
      paint_color_R[0] = base_color1_R;
      paint_color_G[0] = base_color1_G;
      paint_color_B[0] = base_color1_B;
      paint_color_R[2] = base_color2_R;
      paint_color_G[2] = base_color2_G;
      paint_color_B[2] = base_color2_B;
      paint_color_R[1] = detexClamp0To255(base_color2_R + distance);
      paint_color_G[1] = detexClamp0To255(base_color2_G + distance);
      paint_color_B[1] = detexClamp0To255(base_color2_B + distance);
      paint_color_R[3] = detexClamp0To255(base_color2_R - distance);
      paint_color_G[3] = detexClamp0To255(base_color2_G - distance);
      paint_color_B[3] = detexClamp0To255(base_color2_B - distance);
    }
    else {
      // H mode.
      base_color1_R = (bitstring[0] & 0x78) >> 3;
      base_color1_R |= base_color1_R << 4;
      base_color1_G = ((bitstring[0] & 0x07) << 1) | ((bitstring[1] & 0x10) >> 4);
      base_color1_G |= base_color1_G << 4;
      base_color1_B = (bitstring[1] & 0x08) | ((bitstring[1] & 0x03) << 1) | ((bitstring[2] & 0x80) >> 7);
      base_color1_B |= base_color1_B << 4;
      base_color2_R = (bitstring[2] & 0x78) >> 3;
      base_color2_R |= base_color2_R << 4;
      base_color2_G = ((bitstring[2] & 0x07) << 1) | ((bitstring[3] & 0x80) >> 7);
      base_color2_G |= base_color2_G << 4;
      base_color2_B = (bitstring[3] & 0x78) >> 3;
      base_color2_B |= base_color2_B << 4;
      // da is most significant bit, db is middle bit, least significant bit is
      // (base_color1 value >= base_color2 value).
      int base_color1_value = (base_color1_R << 16) + (base_color1_G << 8) + base_color1_B;
      int base_color2_value = (base_color2_R << 16) + (base_color2_G << 8) + base_color2_B;
      int bit;
      if (base_color1_value >= base_color2_value) {
        bit = 1;
      }
      else {
        bit = 0;
      }
      distance = etc2_distance_table[(bitstring[3] & 0x04) | ((bitstring[3] & 0x01) << 1) | bit];
      paint_color_R[0] = detexClamp0To255(base_color1_R + distance);
      paint_color_G[0] = detexClamp0To255(base_color1_G + distance);
      paint_color_B[0] = detexClamp0To255(base_color1_B + distance);
      paint_color_R[1] = detexClamp0To255(base_color1_R - distance);
      paint_color_G[1] = detexClamp0To255(base_color1_G - distance);
      paint_color_B[1] = detexClamp0To255(base_color1_B - distance);
      paint_color_R[2] = detexClamp0To255(base_color2_R + distance);
      paint_color_G[2] = detexClamp0To255(base_color2_G + distance);
      paint_color_B[2] = detexClamp0To255(base_color2_B + distance);
      paint_color_R[3] = detexClamp0To255(base_color2_R - distance);
      paint_color_G[3] = detexClamp0To255(base_color2_G - distance);
      paint_color_B[3] = detexClamp0To255(base_color2_B - distance);
    }
    int pixel_index_word = (bitstring[4] << 24) | (bitstring[5] << 16) | (bitstring[6] << 8) | bitstring[7];
    //uint32_t *buffer = (uint32_t *)pixel_buffer;
    int[] buffer = pixel_buffer;
    for (int i = 0; i < 16; i++) {
      int pixel_index = ((pixel_index_word & (1 << i)) >> i)          // Least significant bit.
          | ((pixel_index_word & (0x10000 << i)) >> (16 + i - 1));    // Most significant bit.
      int r = paint_color_R[pixel_index];
      int g = paint_color_G[pixel_index];
      int b = paint_color_B[pixel_index];
      int mask = punchthrough_mask_table[pixel_index];
      buffer[(i & 3) * 4 + ((i & 12) >> 2)] = (detexPack32RGB8Alpha0xFF(r, g, b)) & mask;
    }
  }

  public void ProcessBlockETC2TOrHMode(int[] bitstring, int mode, int[] pixel_buffer) {
    int base_color1_R, base_color1_G, base_color1_B;
    int base_color2_R, base_color2_G, base_color2_B;
    int[] paint_color_R = new int[4];
    int[] paint_color_G = new int[4];
    int[] paint_color_B = new int[4];
    int distance;
    if (mode == DETEX_MODE_MASK_ETC_T) {
      // T mode.
      base_color1_R = ((bitstring[0] & 0x18) >> 1) | (bitstring[0] & 0x3);
      base_color1_R |= base_color1_R << 4;
      base_color1_G = bitstring[1] & 0xF0;
      base_color1_G |= base_color1_G >> 4;
      base_color1_B = bitstring[1] & 0x0F;
      base_color1_B |= base_color1_B << 4;
      base_color2_R = bitstring[2] & 0xF0;
      base_color2_R |= base_color2_R >> 4;
      base_color2_G = bitstring[2] & 0x0F;
      base_color2_G |= base_color2_G << 4;
      base_color2_B = bitstring[3] & 0xF0;
      base_color2_B |= base_color2_B >> 4;
      // index = (da << 1) | db
      distance = etc2_distance_table[((bitstring[3] & 0x0C) >> 1) | (bitstring[3] & 0x1)];
      paint_color_R[0] = base_color1_R;
      paint_color_G[0] = base_color1_G;
      paint_color_B[0] = base_color1_B;
      paint_color_R[2] = base_color2_R;
      paint_color_G[2] = base_color2_G;
      paint_color_B[2] = base_color2_B;
      paint_color_R[1] = detexClamp0To255(base_color2_R + distance);
      paint_color_G[1] = detexClamp0To255(base_color2_G + distance);
      paint_color_B[1] = detexClamp0To255(base_color2_B + distance);
      paint_color_R[3] = detexClamp0To255(base_color2_R - distance);
      paint_color_G[3] = detexClamp0To255(base_color2_G - distance);
      paint_color_B[3] = detexClamp0To255(base_color2_B - distance);
    }
    else {
      // H mode.
      base_color1_R = (bitstring[0] & 0x78) >> 3;
      base_color1_R |= base_color1_R << 4;
      base_color1_G = ((bitstring[0] & 0x07) << 1) | ((bitstring[1] & 0x10) >> 4);
      base_color1_G |= base_color1_G << 4;
      base_color1_B = (bitstring[1] & 0x08) | ((bitstring[1] & 0x03) << 1) | ((bitstring[2] & 0x80) >> 7);
      base_color1_B |= base_color1_B << 4;
      base_color2_R = (bitstring[2] & 0x78) >> 3;
      base_color2_R |= base_color2_R << 4;
      base_color2_G = ((bitstring[2] & 0x07) << 1) | ((bitstring[3] & 0x80) >> 7);
      base_color2_G |= base_color2_G << 4;
      base_color2_B = (bitstring[3] & 0x78) >> 3;
      base_color2_B |= base_color2_B << 4;
      // da is most significant bit, db is middle bit, least significant bit is
      // (base_color1 value >= base_color2 value).
      int base_color1_value = (base_color1_R << 16) + (base_color1_G << 8) + base_color1_B;
      int base_color2_value = (base_color2_R << 16) + (base_color2_G << 8) + base_color2_B;
      int bit;
      if (base_color1_value >= base_color2_value) {
        bit = 1;
      }
      else {
        bit = 0;
      }
      distance = etc2_distance_table[(bitstring[3] & 0x04) | ((bitstring[3] & 0x01) << 1) | bit];
      paint_color_R[0] = detexClamp0To255(base_color1_R + distance);
      paint_color_G[0] = detexClamp0To255(base_color1_G + distance);
      paint_color_B[0] = detexClamp0To255(base_color1_B + distance);
      paint_color_R[1] = detexClamp0To255(base_color1_R - distance);
      paint_color_G[1] = detexClamp0To255(base_color1_G - distance);
      paint_color_B[1] = detexClamp0To255(base_color1_B - distance);
      paint_color_R[2] = detexClamp0To255(base_color2_R + distance);
      paint_color_G[2] = detexClamp0To255(base_color2_G + distance);
      paint_color_B[2] = detexClamp0To255(base_color2_B + distance);
      paint_color_R[3] = detexClamp0To255(base_color2_R - distance);
      paint_color_G[3] = detexClamp0To255(base_color2_G - distance);
      paint_color_B[3] = detexClamp0To255(base_color2_B - distance);
    }
    int pixel_index_word = (bitstring[4] << 24) | (bitstring[5] << 16) | (bitstring[6] << 8) | bitstring[7];
    //uint32_t *buffer = (uint32_t *)pixel_buffer;
    int[] buffer = pixel_buffer;
    for (int i = 0; i < 16; i++) {
      int pixel_index = ((pixel_index_word & (1 << i)) >> i)          // Least significant bit.
          | ((pixel_index_word & (0x10000 << i)) >> (16 + i - 1));    // Most significant bit.
      int r = paint_color_R[pixel_index];
      int g = paint_color_G[pixel_index];
      int b = paint_color_B[pixel_index];
      buffer[(i & 3) * 4 + ((i & 12) >> 2)] = detexPack32RGB8Alpha0xFF(r, g, b);
    }
  }

  // Define inline function to speed up ETC1 decoding.
  // public void ProcessPixelETC1(uint8_t i, uint32_t pixel_index_word, uint32_t table_codeword, int * DETEX_RESTRICT base_color_subblock, uint8_t * DETEX_RESTRICT pixel_buffer) {
  public void ProcessPixelETC1(int i, int pixel_index_word, int table_codeword, int[] base_color_subblock, int[] pixel_buffer) {
    if (pixel_index_word < 0) { // TODO check for the removal of the "sign"
      pixel_index_word = 0 - pixel_index_word;
    }

    int pixel_index = ((pixel_index_word & (1 << i)) >> i) | ((pixel_index_word & (0x10000 << i)) >> (16 + i - 1));

    int r, g, b;
    int modifier = modifier_table[table_codeword][pixel_index];
    r = detexClamp0To255(base_color_subblock[0] + modifier);
    g = detexClamp0To255(base_color_subblock[1] + modifier);
    b = detexClamp0To255(base_color_subblock[2] + modifier);
    //uint32_t *buffer = (uint32_t *)pixel_buffer;
    int[] buffer = pixel_buffer;
    buffer[(i & 3) * 4 + ((i & 12) >> 2)] = detexPack32RGB8Alpha0xFF(r, g, b);
  }

  public void ProcessPixelETC2Punchthrough(int i, int pixel_index_word, int table_codeword, int[] base_color_subblock, int[] pixel_buffer) {
    int pixel_index = ((pixel_index_word & (1 << i)) >> i)
        | ((pixel_index_word & (0x10000 << i)) >> (16 + i - 1));
    int r, g, b;
    int modifier = punchthrough_modifier_table[table_codeword][pixel_index];
    r = detexClamp0To255(base_color_subblock[0] + modifier);
    g = detexClamp0To255(base_color_subblock[1] + modifier);
    b = detexClamp0To255(base_color_subblock[2] + modifier);
    int mask = punchthrough_mask_table[pixel_index];
    //uint32_t *buffer = (uint32_t *)pixel_buffer;
    int[] buffer = pixel_buffer;
    buffer[(i & 3) * 4 + ((i & 12) >> 2)] = detexPack32RGB8Alpha0xFF(r, g, b) & mask;
  }

  public void SetModeETC2THP(int[] bitstring, int mode) {
    if (mode == 2) {
      // bitstring[0] bits 0, 1, 3, 4 are used.
      // Bits 2, 5, 6, 7 can be modified.
      // Modify bits 2, 5, 6, 7 so that R < 0 or R > 31.
      int R_bits_5_to_7_clear = (bitstring[0] & 0x18) >> 3;
      int R_compl_bit_2_clear = complement3bit(bitstring[0] & 0x3);
      if (R_bits_5_to_7_clear + 0x1C + R_compl_bit_2_clear > 31) {
        // Set bits 5, 6, 7 and clear bit 2.
        bitstring[0] &= ~0x04;
        bitstring[0] |= 0xE0;
      }
      else {
        int R_compl_bit_2_set = complement3bit((bitstring[0] & 0x3) | 0x4);
        if (R_bits_5_to_7_clear + R_compl_bit_2_set < 0) {
          // Clear bits 5, 6, 7 and set bit 2.
          bitstring[0] &= ~0xE0;
          bitstring[0] |= 0x04;
        }
        else {
          ; // Shouldn't happen.
        }
      }
    }
    else if (mode == 3) {
      int G_bits_5_to_7_clear = (bitstring[1] & 0x18) >> 3;
      int G_compl_bit_2_clear = complement3bit(bitstring[1] & 0x3);
      if (G_bits_5_to_7_clear + 0x1C + G_compl_bit_2_clear > 31) {
        // Set bits 5, 6, 7 and clear bit 2.
        bitstring[1] &= ~0x04;
        bitstring[1] |= 0xE0;
      }
      else {
        int G_compl_bit_2_set = complement3bit((bitstring[1] & 0x3) | 0x4);
        if (G_bits_5_to_7_clear + G_compl_bit_2_set < 0) {
          // Clear bits 5, 6, 7 and set bit 2.
          bitstring[1] &= ~0xE0;
          bitstring[1] |= 0x04;
        }
        else {
          ; // Shouldn't happen.
        }
      }
    }
    else if (mode == 4) {
      int B_bits_5_to_7_clear = (bitstring[2] & 0x18) >> 3;
      int B_compl_bit_2_clear = complement3bit(bitstring[2] & 0x3);
      if (B_bits_5_to_7_clear + 0x1C + B_compl_bit_2_clear > 31) {
        // Set bits 5, 6, 7 and clear bit 2.
        bitstring[2] &= ~0x04;
        bitstring[2] |= 0xE0;
      }
      else {
        int B_compl_bit_2_set = complement3bit((bitstring[2] & 0x3) | 0x4);
        if (B_bits_5_to_7_clear + B_compl_bit_2_set < 0) {
          // Clear bits 5, 6, 7 and set bit 2.
          bitstring[2] &= ~0xE0;
          bitstring[2] |= 0x04;
        }
        else {
          ; // Shouldn't happen.
        }
      }
    }
  }

}