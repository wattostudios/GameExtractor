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
import org.watto.ge.helper.FieldValidator;
import org.watto.ge.plugin.ExporterPlugin;
import org.watto.io.FileManipulator;
import org.watto.io.converter.ByteConverter;

public class Exporter_QCMP1 extends ExporterPlugin {

  static Exporter_QCMP1 instance = new Exporter_QCMP1();

  byte[] decompBuffer = null;

  int decompPos = 0;

  int decompLength = 0;

  /**
  **********************************************************************************************
  Ref: QuickBMS
  **********************************************************************************************
  **/
  public static Exporter_QCMP1 getInstance() {
    return instance;
  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public Exporter_QCMP1() {
    setName("QCMP1 Compression");
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
    return "This exporter decompresses QCMP1 files when exporting\n\n" + super.getDescription();
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

      decompressFile(fm, (int) source.getLength());

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

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public void decompressFile(FileManipulator fm, int compLength) {
    try {

      // idstring 1 "PMCQ"
      // get type short 1    # 1
      // get version short 1 # 1
      fm.skip(8);

      // get dataOffset long 1
      int dataOffset = fm.readInt();

      boolean scanning = false;
      if (compLength == 12) { // this happens when scanning for file types
        compLength += dataOffset;
        scanning = true;
      }

      FieldValidator.checkOffset(dataOffset, compLength);

      // get extraSize long 1
      fm.skip(4);

      // get compressedSize longlong 1
      int realCompLength = (int) (fm.readLong() - dataOffset);
      if (!scanning) {
        FieldValidator.checkLength(realCompLength, compLength);
      }
      compLength = realCompLength;

      // get uncompressedSize longlong 1
      int realDecompLength = (int) fm.readLong();
      if (!scanning) {
        FieldValidator.checkLength(realDecompLength, decompLength);
      }
      decompLength = realDecompLength;

      // get uncompressedHash longlong 1
      fm.skip(8);

      // math compressedSize - dataOffset
      // math dataOffset + BCK_OFFSET

      dataOffset -= 40;
      FieldValidator.checkPositive(dataOffset);
      fm.skip(dataOffset);

      byte[] compBytes = fm.readBytes(compLength);
      decompBuffer = new byte[(int) decompLength];

      int i;
      int j;

      //ushort lengths[32] = {0};
      int[] lengths = new int[32];
      //ushort offsets[32] = {0};
      int[] offsets = new int[32];

      int x = 0, y = 0, z = 0;
      for (; y < decompLength;) {
        int op = ByteConverter.unsign(compBytes[x++]);

        if (op < 32) {
          int length = op + 1;
          //memcpy(decompBuffer + y, compBytes + x, length);
          System.arraycopy(compBytes, x, decompBuffer, y, length);
          x += length;
          y += length;
        }
        else {
          int mode = (op >> 5) & 0x07;
          int index = (op >> 0) & 0x1F;

          //ushort length, offset;
          int length;
          int offset;
          if (mode == 1) {
            length = lengths[index];
            offset = offsets[index];
          }
          else {
            //offset = (ushort)(compBytes[x++] | (index << 8));
            offset = (ByteConverter.unsign(compBytes[x++]) | (index << 8));
            //length = (ushort)((mode == 7 ? compBytes[x++] : mode) + 1);
            length = ((mode == 7 ? ByteConverter.unsign(compBytes[x++]) : mode) + 1);

            offsets[z] = offset;
            lengths[z] = length;
            z = (z + 1) % 32;
          }

          for (i = 0, j = y - offset; i < length; i++, j++) {
            decompBuffer[y] = decompBuffer[j];
            y++;
          }
        }
      }
    }
    catch (Throwable t) {
      ErrorLogger.log(t);
    }
  }

}