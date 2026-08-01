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
import org.watto.io.converter.IntConverter;

public class Exporter_JCALG1 extends ExporterPlugin {

  static Exporter_JCALG1 instance = new Exporter_JCALG1();

  byte[] decompBuffer = null;

  int decompPos = 0;

  int decompLength = 0;

  /**
  **********************************************************************************************
  Ref: https://github.com/jeremycollake/jcalg1/blob/master/CompressedData.cpp
  **********************************************************************************************
  **/
  public static Exporter_JCALG1 getInstance() {
    return instance;
  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public Exporter_JCALG1() {
    setName("JCALG1 Compression");
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
    return "This exporter decompresses JCALG1 files when exporting\n\n" + super.getDescription();
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

      //decompBuffer = new byte[(int) decompLength];

      decompressFile(fm, (int) source.getLength(), decompLength);
      decompPos = 0;

      fm.close();
    }
    catch (Throwable t) {
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

  long BitBuffer = 0; // LONG, so that we can have it as an unsigned integer
  int BitsRemaining = 0;
  int LastIndex = 0;
  int IndexBase = 0;
  int LiteralBits = 0;
  int LiteralOffset = 0;

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public void decompressFile(FileManipulator fm, int compLength, int decompLength) {
    try {

      BitBuffer = 0;
      BitsRemaining = 0;
      LastIndex = 1;
      IndexBase = 8;
      LiteralBits = 0;
      LiteralOffset = 0;

      decompBuffer = new byte[(int) decompLength];

      fm.skip(10); // +10 skips the header of JCALG.

      while (true) {
        if (GetBit(fm) != 0) {
          // Is Literal
          decompBuffer[decompPos++] = (byte) (GetBits(fm, LiteralBits) + LiteralOffset);
        }
        else {
          // Isn't literal
          if (GetBit(fm) != 0) {
            // Normal phrase
            int HighIndex = GetInteger(fm);

            if (HighIndex == 2) // Use the last index
            {
              int PhraseLength = GetInteger(fm);
              TransferMatch(LastIndex, PhraseLength);
            }
            else {
              LastIndex = ((HighIndex - 3) << IndexBase) + GetBits(fm, IndexBase);

              int PhraseLength = GetInteger(fm);

              if (LastIndex >= 0x10000)
                PhraseLength += 3;
              else if (LastIndex >= 0x37FF)
                PhraseLength += 2;
              else if (LastIndex >= 0x27F)
                PhraseLength++;
              else if (LastIndex <= 127)
                PhraseLength += 4;

              TransferMatch(LastIndex, PhraseLength);
            }
          }
          else {
            if (GetBit(fm) != 0) {
              // OneBytePhrase or literal size change
              int OneBytePhraseValue = GetBits(fm, 4) - 1;
              if (OneBytePhraseValue == 0) {
                decompBuffer[decompPos++] = 0;
              }
              else if (OneBytePhraseValue > 0) {
                //*Destination = *(Destination-OneBytePhraseValue);
                //Destination++;
                decompBuffer[decompPos] = decompBuffer[decompPos - OneBytePhraseValue];
                decompPos++;
              }
              else {
                if (GetBit(fm) != 0) {
                  // Next block
                  do {
                    for (int i = 0; i < 256; i++) {
                      decompBuffer[decompPos++] = (byte) GetBits(fm, 8);
                    }
                  }
                  while (GetBit(fm) != 0);
                }
                else {
                  // New literal size
                  LiteralBits = 7 + GetBit(fm);
                  LiteralOffset = 0;
                  if (LiteralBits != 8) {
                    LiteralOffset = GetBits(fm, 8);
                  }
                }
              }
            }
            else {
              // Short match
              int NewIndex = GetBits(fm, 7);
              int MatchLength = 2 + GetBits(fm, 2);

              if (NewIndex == 0) {
                // Extended short
                if (MatchLength == 2)
                  break; // End of decompression

                IndexBase = GetBits(fm, MatchLength + 1);
              }
              else {
                LastIndex = NewIndex;
                TransferMatch(LastIndex, MatchLength);
              }
            }
          }
        }
      }

    }
    catch (Throwable t) {
      ErrorLogger.log(t);
    }
  }

  public int GetBit(FileManipulator fm) {
    int ReturnValue;

    if (BitsRemaining == 0) {
      BitsRemaining = 32;
      BitBuffer = IntConverter.unsign(fm.readInt());
    }

    ReturnValue = (int) ((BitBuffer >> 31) & 1);
    BitBuffer <<= 1;
    BitsRemaining--;
    return ReturnValue;
  }

  public int GetBits(FileManipulator fm, int Count) {
    if (BitsRemaining >= Count) {

      int andCount = ((1 << Count) - 1);

      int ReturnValue = (int) ((BitBuffer >> (32 - Count)) & andCount);
      BitBuffer <<= Count;
      BitsRemaining -= Count;
      return ReturnValue;
    }
    else {
      int Remainder = Count - BitsRemaining;

      int andCount = ((1 << BitsRemaining) - 1);

      int ReturnValue = (int) (((BitBuffer >> (32 - BitsRemaining)) & andCount) << Remainder);
      BitBuffer = IntConverter.unsign(fm.readInt());

      andCount = ((1 << Remainder) - 1);

      ReturnValue |= ((BitBuffer >> (32 - Remainder)) & andCount);
      BitsRemaining = 32 - Remainder;
      BitBuffer <<= Remainder;

      return ReturnValue;
    }
  }

  public int GetInteger(FileManipulator fm) {
    int Value = 1;
    do {
      Value = (Value << 1) + GetBit(fm);
    }
    while (GetBit(fm) != 0);

    return Value;
  }

  public void TransferMatch(int MatchOffset, int MatchLength) {
    //unsigned char* p = Destination;
    //unsigned char* s = p-MatchOffset;
    int p = decompPos;
    int s = p - MatchOffset;

    do {
      decompBuffer[p++] = decompBuffer[s++];
      decompPos++;
    }
    while (--MatchLength > 0);

  }

}