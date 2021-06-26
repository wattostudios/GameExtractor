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

public class Exporter_PRS_8ING extends ExporterPlugin {

  static Exporter_PRS_8ING instance = new Exporter_PRS_8ING();

  static long readLength = 0;

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public static Exporter_PRS_8ING getInstance() {
    return instance;
  }

  byte[] readBuffer = new byte[0];

  int readBufferPos = 0;

  int readBufferLength = 0;

  FileManipulator fm;

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public Exporter_PRS_8ING() {
    setName("PRS_8ING Compression");
  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  @Override
  public boolean available() {
    try {
      return (readBufferPos < readBufferLength);
    }
    catch (Throwable t) {
      ErrorLogger.log(t);
      return false;
    }
  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  @Override
  public void close() {
    try {
      readBuffer = new byte[0];
      readBufferPos = 0;
      readBufferLength = 0;

      fm.close();
    }
    catch (Throwable t) {
    }
  }

  /**
  **********************************************************************************************
  Ref: http://forum.xentax.com/viewtopic.php?p=30387#p30387
  PRS get bit form lsb to msb, FPK get it form msb to lsb  
  **********************************************************************************************
  **/
  int prs_8ing_get_bits(int n, byte[] sbuf) {

    int retv = 0;

    while (n != 0) {
      retv <<= 1;
      if ((blen) == 0) {
        fbuf = sbuf[sptr];
        (sptr)++;
        (blen) = 8;
      }

      if ((fbuf & 0x80) == 0x80) {
        retv |= 1;
      }

      fbuf <<= 1;
      (blen)--;
      n--;
    }

    return retv;
  }

  int sptr = 0;

  int fbuf = 0;

  int blen = 0;

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  void decompress(byte[] sbuf) {

    int slen = sbuf.length;

    int dptr = 0;
    int flag;
    int len;
    int pos;

    blen = 0;
    sptr = 0;
    fbuf = 0;

    while (sptr < slen) {
      flag = prs_8ing_get_bits(1, sbuf);
      if (flag == 1) {
        if (dptr < readBufferLength)
          readBuffer[dptr++] = sbuf[sptr++];
      }
      else {
        flag = prs_8ing_get_bits(1, sbuf);
        if (flag == 0) {
          len = prs_8ing_get_bits(2, sbuf) + 2;
          pos = sbuf[sptr++] | 0xffffff00;
        }
        else {
          pos = (sbuf[sptr++] << 8) | 0xffff0000;
          pos |= sbuf[sptr++] & 0xff;
          len = pos & 0x07;
          pos >>= 3;
          if (len == 0) {
            len = (sbuf[sptr++] & 0xff) + 1;
          }
          else {
            len += 2;
          }
        }
        pos += dptr;
        for (int i = 0; i < len; i++) {
          if (dptr < readBufferLength)
            readBuffer[dptr++] = readBuffer[pos++];
        }
      }
    }

  }

  /**
  **********************************************************************************************
  So we can easily call this from within a Viewer plugin
  **********************************************************************************************
  **/
  public void open(FileManipulator fmIn, int compLengthIn, int decompLengthIn) {
    try {
      fm = fmIn;

      readLength = decompLengthIn;

      readBufferLength = (int) readLength;
      readBuffer = new byte[readBufferLength];
      readBufferPos = 0;

      byte[] compBytes = fm.readBytes(compLengthIn);
      decompress(compBytes);

    }
    catch (Throwable t) {
    }
  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  @Override
  public void open(Resource source) {
    try {
      fm = new FileManipulator(source.getSource(), false);
      fm.seek(source.getOffset());

      readLength = source.getDecompressedLength();

      readBufferLength = (int) readLength;
      readBuffer = new byte[readBufferLength];
      readBufferPos = 0;

      byte[] compBytes = fm.readBytes((int) source.getLength());
      decompress(compBytes);

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
    // NOT IMPLEMENTED
  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  @Override
  public int read() {
    try {
      int currentByte = readBuffer[readBufferPos];
      readBufferPos++;
      return currentByte;
    }
    catch (Throwable t) {
      t.printStackTrace();
      readLength = 0;
      return 0;
    }
  }

}