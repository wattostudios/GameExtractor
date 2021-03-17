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

import org.watto.datatype.Resource;
import org.watto.ge.plugin.ExporterPlugin;
import org.watto.io.FileManipulator;
import org.watto.io.converter.ByteConverter;

public class Exporter_RNC2 extends ExporterPlugin {

  static Exporter_RNC2 instance = new Exporter_RNC2();

  byte[] decompBuffer = null;

  int decompPos = 0;

  int decompLength = 0;

  int RncDecoder__bitBuffl = 0;

  int RncDecoder__bitCount = 0;

  /**
  **********************************************************************************************
  Ref: https://pastebin.com/rGpBFwAV and QuickBMS
  **********************************************************************************************
  **/
  public static Exporter_RNC2 getInstance() {
    return instance;
  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public Exporter_RNC2() {
    setName("RNC2 Compression");
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

      decompressFile(fm, (int) source.getLength());

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

      decompressFile(fmIn, compLengthIn);
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
  public int decompressFile(FileManipulator fm, int compLength) {
    try {

      //inputptr = (((const uint8 *)input) + RncDecoder_HEADER_LEN);
      //RncDecoder__srcPtr = inputptr;
      //RncDecoder__dstPtr = (uint8 *)output;

      RncDecoder__bitBuffl = 0;
      RncDecoder__bitCount = 0;

      //uint16 ofs, len;
      int ofs;
      int len = 0;

      //byte ofs_hi = 0;
      //byte ofs_lo = 0;
      int ofs_hi = 0;
      int ofs_lo = 0;

      RncDecoder__getbit(fm);
      RncDecoder__getbit(fm);

      int outPos = 0;

      while (outPos < decompLength) {

        boolean loadVal = false;

        while (RncDecoder__getbit(fm) == 0) {
          decompBuffer[outPos++] = fm.readByte();  //*RncDecoder__dstPtr++ = *RncDecoder__srcPtr++;
        }

        len = 2;
        ofs_hi = 0;

        if (RncDecoder__getbit(fm) == 0) {
          len = (len << 1) | RncDecoder__getbit(fm);
          if (RncDecoder__getbit(fm) == 1) {
            len--;
            len = (len << 1) | RncDecoder__getbit(fm);
            if (len == 9) {
              len = 4;
              while (len-- > 0) { // while (len--) {
                ofs_hi = (ofs_hi << 1) | RncDecoder__getbit(fm);
              }
              len = (ofs_hi + 3) * 4;
              while (len-- > 0) { // while (len--) {
                decompBuffer[outPos++] = fm.readByte();  //*RncDecoder__dstPtr++ = *RncDecoder__srcPtr++;
              }
              continue;
            }
          }
          loadVal = true;
        }
        else {
          if (RncDecoder__getbit(fm) == 1) {
            len++;
            if (RncDecoder__getbit(fm) == 1) {
              len = ByteConverter.unsign(fm.readByte()); //len = *RncDecoder__srcPtr++;
              if (len == 0) {
                if (RncDecoder__getbit(fm) == 1) {
                  continue;
                }
                else {
                  break;
                }
              }
              len += 8;
            }
            loadVal = true;
          }
          else {
            loadVal = false;
          }
        }

        if (loadVal) {
          if (RncDecoder__getbit(fm) == 1) {
            ofs_hi = (ofs_hi << 1) | RncDecoder__getbit(fm);
            if (RncDecoder__getbit(fm) == 1) {
              ofs_hi = ((ofs_hi << 1) | RncDecoder__getbit(fm)) | 4;
              if (RncDecoder__getbit(fm) == 0) {
                ofs_hi = (ofs_hi << 1) | RncDecoder__getbit(fm);
              }
            }
            else if (ofs_hi == 0) {
              ofs_hi = 2 | RncDecoder__getbit(fm);
            }
          }
        }

        ofs_lo = ByteConverter.unsign(fm.readByte()); //ofs_lo = *RncDecoder__srcPtr++;
        ofs = (ofs_hi << 8) | ofs_lo;
        while (len-- > 0) { //while (len--) {
          decompBuffer[outPos] = decompBuffer[outPos - ofs - 1];//*RncDecoder__dstPtr = *(byte *)(RncDecoder__dstPtr - ofs - 1); 
          outPos++; //RncDecoder__dstPtr++;

        }

      }

      return outPos;

    }
    catch (Throwable t) {
      t.printStackTrace();
      return -1;
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

}