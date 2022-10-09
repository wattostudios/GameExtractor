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

package org.watto.ge.plugin.exporter;

import org.watto.ErrorLogger;
import org.watto.datatype.Resource;
import org.watto.ge.plugin.ExporterPlugin;
import org.watto.io.FileManipulator;
import org.watto.io.converter.ByteConverter;
import org.watto.io.converter.ShortConverter;

public class Exporter_RNC1 extends ExporterPlugin {

  static Exporter_RNC1 instance = new Exporter_RNC1();

  byte[] decompBuffer = null;

  int decompPos = 0;

  int decompLength = 0;

  int RncDecoder__bitBuffl = 0;

  int RncDecoder__bitBuffh = 0;

  int RncDecoder__bitCount = 0;

  /**
  **********************************************************************************************
  Ref: QuickBMS
  NOT SURE IF THIS WORKS - THE QUICKBMS ONE DOESN'T OPEN FILES FROM TUROK GAME EITHER
  **********************************************************************************************
  **/
  public static Exporter_RNC1 getInstance() {
    return instance;
  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public Exporter_RNC1() {
    setName("RNC1 Compression");
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

    RncDecoder__bitBuffl = 0;
    RncDecoder__bitCount = 0;
  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  @Override
  public String getDescription() {
    return "This exporter decompresses RNC2 files when exporting\n\n" + super.getDescription();
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

      decompBuffer = new byte[(int) decompLength];

      RncDecoder__unpackM1(fm, (int) source.getLength());

      // reset this back, ready for reading
      decompPos = 0;

      fm.close();
    }
    catch (Throwable t) {
    }
  }

  /**
  **********************************************************************************************
  So we can easily call this from within a Viewer plugin
  **********************************************************************************************
  **/
  public void open(FileManipulator fmIn, int compLengthIn, int decompLengthIn) {
    try {

      decompPos = 0;
      decompLength = decompLengthIn;

      decompBuffer = new byte[(int) decompLength];

      RncDecoder__unpackM1(fmIn, compLengthIn);

      // reset this back, ready for reading
      decompPos = 0;

    }
    catch (Throwable t) {
    }
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

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public int RncDecoder__getbit(FileManipulator fm) {

    if (RncDecoder__bitCount == 0) {
      RncDecoder__bitBuffl = ByteConverter.unsign(fm.readByte()); //*RncDecoder__srcPtr++;
      RncDecoder__bitCount = 8;
    }
    int temp = (RncDecoder__bitBuffl & 0x80) >> 7;//byte temp = (RncDecoder__bitBuffl & 0x80) >> 7;
    RncDecoder__bitBuffl <<= 1;
    RncDecoder__bitCount--;
    return temp;
  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  @Override
  public void pack(Resource source, FileManipulator destination) {
    // NOT IMPLEMENTED

  }

  int[] RncDecoder__rawTable = new int[64];

  int[] RncDecoder__posTable = new int[64];

  int[] RncDecoder__lenTable = new int[64];

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public int RncDecoder__unpackM1(FileManipulator fm, int inputSize) {
    try {
      int RncDecoder_MIN_LENGTH = 2;
      int counts = 0;

      RncDecoder__rawTable = new int[64];
      RncDecoder__posTable = new int[64];
      RncDecoder__lenTable = new int[64];

      RncDecoder__bitBuffl = 0;
      RncDecoder__bitBuffh = 0;
      RncDecoder__bitCount = 0;

      long fmPos = fm.getOffset();
      RncDecoder__bitBuffl = ShortConverter.unsign(fm.readShort()); // just want to read without moving the pointer
      fm.relativeSeek(fmPos);

      RncDecoder__inputBits(fm, 2);

      while (decompPos < decompLength) {
        RncDecoder__makeHufftable(fm, RncDecoder__rawTable);
        RncDecoder__makeHufftable(fm, RncDecoder__posTable);
        RncDecoder__makeHufftable(fm, RncDecoder__lenTable);

        counts = RncDecoder__inputBits(fm, 16);

        do {
          int inputLength = RncDecoder__inputValue(fm, RncDecoder__rawTable);
          int inputOffset;

          if (inputLength > 0) {
            //memcpy(RncDecoder__dstPtr, RncDecoder__srcPtr, inputLength); //memcpy is allowed here
            for (int b = 0; b < inputLength; b++) {
              decompBuffer[decompPos] = fm.readByte();
              decompPos++;
            }

            int a;
            int remaining = (int) fm.getRemainingLength();
            if (remaining <= 0) {
              a = 0;
            }
            else if (remaining == 1) {
              fmPos = fm.getOffset();
              a = ByteConverter.unsign(fm.readByte()); // just want to read without moving the pointer
              fm.relativeSeek(fmPos);
            }
            else {
              fmPos = fm.getOffset();
              a = ShortConverter.unsign(fm.readShort()); // just want to read without moving the pointer
              fm.relativeSeek(fmPos);
            }

            int b;

            if (remaining <= 2) {
              b = 0;
            }
            else if (remaining == 3) {
              fmPos = fm.getOffset();
              fm.skip(2);
              b = ByteConverter.unsign(fm.readByte()); // just want to read without moving the pointer
              fm.relativeSeek(fmPos);
            }
            else {
              fmPos = fm.getOffset();
              fm.skip(2);
              b = ShortConverter.unsign(fm.readShort()); // just want to read without moving the pointer
              fm.relativeSeek(fmPos);
            }

            RncDecoder__bitBuffl &= ((1 << RncDecoder__bitCount) - 1);
            RncDecoder__bitBuffl |= (a << RncDecoder__bitCount);
            RncDecoder__bitBuffh = (a >> (16 - RncDecoder__bitCount)) | (b << RncDecoder__bitCount);
          }

          if (counts > 1) {
            inputOffset = RncDecoder__inputValue(fm, RncDecoder__posTable) + 1;
            inputLength = RncDecoder__inputValue(fm, RncDecoder__lenTable) + RncDecoder_MIN_LENGTH;

            // Don't use memcpy here! because input and output overlap.
            int tmpPtr = decompPos - inputOffset;
            while (inputLength-- > 0) {
              decompBuffer[decompPos] = decompBuffer[tmpPtr];
              decompPos++;
              tmpPtr++;
            }
          }
        }
        while (--counts > 0);
      }

      return decompLength;
    }
    catch (Throwable t) {
      ErrorLogger.log(t);
      return -1;
    }

  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public int RncDecoder__inputBits(FileManipulator fm, int amount) {

    int newBitBuffh = RncDecoder__bitBuffh;
    int newBitBuffl = RncDecoder__bitBuffl;
    int newBitCount = RncDecoder__bitCount;
    int remBits, returnVal;

    returnVal = ((1 << amount) - 1) & newBitBuffl;
    newBitCount -= amount;

    if (newBitCount < 0) {
      newBitCount += amount;
      remBits = (newBitBuffh << (16 - newBitCount));
      newBitBuffh >>= newBitCount;
      newBitBuffl >>= newBitCount;
      newBitBuffl |= remBits;
      //RncDecoder__srcPtr += 2; // done after the if/else

      // added some more check here to prevent reading in the buffer
      // if there are no bytes anymore.
      int remaining = (int) fm.getRemainingLength();
      if (remaining <= 0) {
        newBitBuffh = 0;
      }
      else if (remaining == 1) {
        long fmPos = fm.getOffset();
        newBitBuffh = ByteConverter.unsign(fm.readByte()); // just want to read without moving the pointer
        fm.relativeSeek(fmPos);
      }
      else {
        long fmPos = fm.getOffset();
        newBitBuffh = ShortConverter.unsign(fm.readShort()); // just want to read without moving the pointer
        fm.relativeSeek(fmPos);
      }
      fm.skip(2);

      amount -= newBitCount;
      newBitCount = 16 - amount;
    }
    remBits = (newBitBuffh << (16 - amount));
    RncDecoder__bitBuffh = newBitBuffh >> amount;
    RncDecoder__bitBuffl = (newBitBuffl >> amount) | remBits;
    RncDecoder__bitCount = (int) newBitCount;

    return returnVal;
  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public void RncDecoder__makeHufftable(FileManipulator fm, int[] table) {

    int bitLength, i, j;
    int numCodes = RncDecoder__inputBits(fm, 5);

    //if (!numCodes)
    if (numCodes == 0) {
      return;
    }

    int[] huffLength = new int[16];
    for (i = 0; i < numCodes; i++) {
      huffLength[i] = (int) (RncDecoder__inputBits(fm, 4) & 0x00FF);
    }

    int huffCode = 0;

    int tablePos = 0;
    for (bitLength = 1; bitLength < 17; bitLength++) {
      for (i = 0; i < numCodes; i++) {
        if (huffLength[i] == bitLength) {
          //*table++ = (1 << bitLength) - 1;
          table[tablePos] = (1 << bitLength) - 1;
          tablePos++;

          int b = huffCode >> (16 - bitLength);
          int a = 0;

          for (j = 0; j < bitLength; j++)
            a |= ((b >> j) & 1) << (bitLength - j - 1);
          //*table++ = a
          table[tablePos] = a;
          tablePos++;

          //*(table + 0x1e) = (huffLength[i] << 8) | (i & 0x00FF);
          table[tablePos + 0x1e] = (huffLength[i] << 8) | (i & 0x00FF);

          huffCode += 1 << (16 - bitLength);
        }
      }
    }
  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public int RncDecoder__inputValue(FileManipulator fm, int[] table) {

    int valOne, valTwo, value = RncDecoder__bitBuffl;

    int tablePos = 0;
    do {
      valTwo = table[tablePos] & value;
      tablePos++;
      valOne = table[tablePos];
      tablePos++;
    }
    while (valOne != valTwo);

    value = table[tablePos + 0x1e];
    RncDecoder__inputBits(fm, (int) ((value >> 8) & 0x00FF));
    value &= 0x00FF;

    if (value >= 2) {
      value--;
      valOne = RncDecoder__inputBits(fm, (int) value & 0x00FF);
      valOne |= (1 << value);
      value = valOne;
    }

    return value;
  }

}