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

package org.watto.ge.plugin.exporter;

import org.watto.ErrorLogger;
import org.watto.datatype.Resource;
import org.watto.ge.plugin.ExporterPlugin;
import org.watto.io.FileManipulator;
import org.watto.io.converter.ByteConverter;

public class Exporter_TDCB_LZSS extends ExporterPlugin {

  static Exporter_TDCB_LZSS instance = new Exporter_TDCB_LZSS();

  byte[] decompBuffer = null;

  int decompPos = 0;

  int decompLength = 0;

  /**
  **********************************************************************************************
  Ref: https://github.com/LittleBigBug/QuickBMS/blob/5315ffe664b88dc09ae783ad17d9dfd252b1c927/src/libs/tdcb/lzss.c
  **********************************************************************************************
  **/
  public static Exporter_TDCB_LZSS getInstance() {
    return instance;
  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public Exporter_TDCB_LZSS() {
    setName("TDCB LZSS Compression");
  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  @Override
  public boolean available() {
    if (decompPos < decompLength) {
      return true;
    }
    else {
      return false;
    }
  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  @Override
  public void close() {
    decompBuffer = null;
    decompPos = 0;
    decompLength = 0;
  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  @Override
  public String getDescription() {
    return "This exporter decompresses TDCB LZSS files when exporting\n\n" + super.getDescription();
  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  @Override
  public void open(Resource source) {
    try {
      FileManipulator fm = new FileManipulator(source.getSource(), false);
      fm.seek(source.getOffset());

      decompPos = 0;
      decompLength = (int) source.getDecompressedLength();
      decompBuffer = new byte[decompLength];

      int compLength = (int) source.getLength();
      byte[] compBuffer = fm.readBytes(compLength);

      int tmp1 = 12; // INDEX_BIT_COUNT
      int tmp2 = 4; // LENGTH_BIT_COUNT
      int tmp3 = 9; // DUMMY9
      int tmp4 = 0; // END_OF_STREAM
      //get_parameter_numbers(g_comtype_dictionary, &tmp1, &tmp2, &tmp3, &tmp4, NULL);
      tdcb_lzss_init(tmp1, tmp2, tmp3, tmp4);

      lzss_ExpandMemory(compBuffer, compLength, decompBuffer, decompLength);

      decompPos = 0;

      fm.close();
    }
    catch (Throwable t) {
      ErrorLogger.log(t);
    }
  }

  /**
  **********************************************************************************************
  NOT IMPLEMENTED
  **********************************************************************************************
  **/
  @Override
  public void pack(Resource source, FileManipulator destination) {

  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  @Override
  public int read() {
    try {
      byte currentByte = decompBuffer[decompPos];
      decompPos++;
      return currentByte;
    }
    catch (Throwable t) {
      return 0;
    }
  }

  int lzss_INDEX_BIT_COUNT = 12;
  int lzss_LENGTH_BIT_COUNT = 4;
  int lzss_DUMMY9 = 9;
  int lzss_END_OF_STREAM = 0;

  int WINDOW_SIZE() {
    return (1 << lzss_INDEX_BIT_COUNT);
  }

  int RAW_LOOK_AHEAD_SIZE() {
    return (1 << lzss_LENGTH_BIT_COUNT);
  }

  int BREAK_EVEN() {
    return ((1 + lzss_INDEX_BIT_COUNT + lzss_LENGTH_BIT_COUNT) / lzss_DUMMY9);
  }

  int LOOK_AHEAD_SIZE() {
    return (RAW_LOOK_AHEAD_SIZE() + BREAK_EVEN());
  }

  int TREE_ROOT() {
    return WINDOW_SIZE();
  }

  int MOD_WINDOW(int a) {
    return ((a) & (WINDOW_SIZE() - 1));
  }

  void tdcb_lzss_init(int x1, int x2, int x3, int x4) {
    lzss_INDEX_BIT_COUNT = x1;
    lzss_LENGTH_BIT_COUNT = x2;
    lzss_DUMMY9 = x3;
    lzss_END_OF_STREAM = x4;
  }

  int inPos = 0;
  int outPos = 0;

  public int lzss_ExpandMemory(byte[] in, int insz, byte[] out, int outsz) {

    int[] window = new int[WINDOW_SIZE()];

    inPos = 0;
    outPos = 0;

    BitBuffer = 0;
    BitsRemaining = 0;

    int current_position = 1;
    for (;;) {
      if (InputBit(in) != 0) {
        int c = (int) InputBits(in, 8);

        out[outPos] = (byte) c;
        outPos++;

        window[current_position] = ByteConverter.unsign((byte) c);
        current_position = MOD_WINDOW(current_position + 1);
      }
      else {
        int match_position = (int) InputBits(in, lzss_INDEX_BIT_COUNT);
        if (match_position == lzss_END_OF_STREAM) {
          break;
        }
        int match_length = (int) InputBits(in, lzss_LENGTH_BIT_COUNT);
        match_length += BREAK_EVEN();
        for (int i = 0; i <= match_length; i++) {
          int c = window[MOD_WINDOW(match_position + i)];

          out[outPos] = (byte) c;
          outPos++;

          window[current_position] = ByteConverter.unsign((byte) c);
          current_position = MOD_WINDOW(current_position + 1);
        }
      }
    }

    return outPos;
  }

  int BitBuffer = 0;
  int BitsRemaining = 0;

  public int InputBit(byte[] input) {
    int ReturnValue;

    if (BitsRemaining == 0) {
      BitsRemaining = 8;
      BitBuffer = input[inPos];
      inPos++;
    }

    ReturnValue = (int) ((BitBuffer >> 7) & 1);
    BitBuffer <<= 1;
    BitsRemaining--;
    return ReturnValue;
  }

  public int InputBits(byte[] input, int Count) {
    if (BitsRemaining >= Count) {

      int andCount = ((1 << Count) - 1);

      int ReturnValue = (int) ((BitBuffer >> (8 - Count)) & andCount);
      BitBuffer <<= Count;
      BitsRemaining -= Count;
      return ReturnValue;
    }
    else {
      int Remainder = Count - BitsRemaining;

      if (Remainder >= 8) {
        // read blocks of 8
        int ReturnValue = 0;

        for (int i = 0; i < Count; i++) {
          ReturnValue = (ReturnValue << 1) | (InputBit(input) & 1);
        }

        return ReturnValue;
      }
      else {
        // read as a block

        int andCount = ((1 << BitsRemaining) - 1);

        int ReturnValue = (int) (((BitBuffer >> (8 - BitsRemaining)) & andCount) << Remainder);

        BitBuffer = input[inPos];
        inPos++;

        andCount = ((1 << Remainder) - 1);

        ReturnValue |= ((BitBuffer >> (8 - Remainder)) & andCount);
        BitsRemaining = 8 - Remainder;
        BitBuffer <<= Remainder;

        return ReturnValue;
      }
    }
  }

}