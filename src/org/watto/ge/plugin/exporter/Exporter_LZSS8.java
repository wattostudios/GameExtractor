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

public class Exporter_LZSS8 extends ExporterPlugin {

  static Exporter_LZSS8 instance = new Exporter_LZSS8();

  static long readLength = 0;

  /**
  **********************************************************************************************
  Ref: https://github.com/smiRaphi/UniPyX/blob/d7ec8793674fb80cb8d8a9dcef67e95d69fd482c/lib/file.py#L573
  **********************************************************************************************
  **/
  public static Exporter_LZSS8 getInstance() {
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
  public Exporter_LZSS8() {
    setName("LZSS8 Compression");
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
  
  **********************************************************************************************
  **/
  byte[] decompress(byte[] i, int usize) {

    int win_size = 0x1000;
    int threshold = 3;
    int maxm = 18;

    byte[] ob = new byte[usize];
    byte[] ring = new byte[win_size];
    int p = 0;
    int rp = win_size - maxm;
    int f = 0;

    int inSize = i.length;
    int outPos = 0;

    while (outPos < usize && p < inSize) {
      if ((f & 0xFF00) == 0) {//if ((f & 0xFF00) != 0xFF00) {
        if (p >= inSize) {
          break;
        }
        f = ByteConverter.unsign(i[p]) | 0x8000;
        p += 1;
      }

      if ((f & 1) == 1) {
        if (p >= inSize) {
          break;
        }
        int c = ByteConverter.unsign(i[p]);
        p += 1;

        ob[outPos] = (byte) c; // ob.append(c);
        outPos++;

        ring[rp % win_size] = (byte) c;
        rp += 1;
      }
      else {
        if (p + 1 >= inSize) {
          break;
        }

        int b1 = ByteConverter.unsign(i[p]);
        p += 1;
        int b2 = ByteConverter.unsign(i[p]);
        p += 1;

        int idx = ((b2 & 0xF0) << 4) | b1;
        int count = ((b2 & 0x0F) + threshold);

        for (int ix = 0; ix < count; ix++) {
          if (outPos >= usize) {
            break;
          }

          //c = ring[(idx + ix) % win_size];
          byte c = ring[(idx + ix) % win_size];

          ob[outPos] = (byte) c; // ob.append(c);
          outPos++;

          ring[rp % win_size] = (byte) c;
          rp += 1;
        }
      }

      f >>= 1;
    }

    return ob;

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
      readBufferPos = 0;

      byte[] compBytes = fm.readBytes(compLengthIn);
      readBuffer = decompress(compBytes, decompLengthIn);

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
      readBufferPos = 0;

      byte[] compBytes = fm.readBytes((int) source.getLength());
      readBuffer = decompress(compBytes, (int) readLength);

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