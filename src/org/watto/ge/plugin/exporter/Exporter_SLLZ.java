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

public class Exporter_SLLZ extends ExporterPlugin {

  static Exporter_SLLZ instance = new Exporter_SLLZ();

  static long readLength = 0;
  static int currentByte = 0;

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public static Exporter_SLLZ getInstance() {
    return instance;
  }

  int[] previousBuffer = null;
  int previousBufferLength = 0;

  //int[] buffer = new int[4096];
  int[] buffer = new int[8192];
  int bufferLength = 0;

  int bufferPos = 0;

  int a;
  int b;
  boolean firstTime = true;

  FileManipulator fm;

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public Exporter_SLLZ() {
    setName("SLLZ Compression");
  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  @Override
  public boolean available() {
    try {

      if (readLength > 0) {
        if (bufferPos >= bufferLength) {
          decompress(); // decompress the next blocks of data
        }
        if (bufferLength <= 0) {
          return false; // even after doing a re-read, the buffer is still empty, so it's EOF
        }
        currentByte = buffer[bufferPos];
        bufferPos++;

        readLength--;
        if (currentByte >= 0) {
          return true;
        }
      }

      return false;
    }
    catch (Throwable t) {
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
      fm.close();
    }
    catch (Throwable t) {
    }
  }

  /**
  **********************************************************************************************

  **********************************************************************************************
  **/
  public void decompress() {

    bufferLength = 0;
    bufferPos = 0;

    int i;
    int d;

    int op;

    //byte[] p  = in; // unsigned char
    //byte[] o  = out; // unsigned char
    //int il = in.length + insz;
    //int ol = out.length + outsz;

    if (firstTime) {
      b = ByteConverter.unsign(fm.readByte());
      a = 8;
      firstTime = false;
    }

    while (bufferLength < 4096) { // 4096 because the repeat loop can go forward/back at most 4096 bytes

      if ((b & 0x80) == 0x80) {
        op = 1;
      }
      else {
        op = 0;
      }

      b <<= 1;
      a--;
      if (a == 0) {
        b = ByteConverter.unsign(fm.readByte());
        a = 8;
      }

      if (op == 1) {
        int byte1 = ByteConverter.unsign(fm.readByte());
        int byte2 = ByteConverter.unsign(fm.readByte());
        d = ((byte1 >> 4) | (byte2 << 4)) + 1;

        for (i = (byte1 & 15) + 3; i > 0; i--) {
          //if(o >= ol) {
          //  break;
          //}
          int readPos = bufferLength - d;
          if (readPos < 0) {
            // need to read from the previous buffer
            if (previousBuffer == null) {
              return; // asked for the previous buffer, but we don't have one
            }
            readPos = previousBufferLength + readPos;
            buffer[bufferLength] = previousBuffer[readPos]; //*o = *(o - d);  
          }
          else {
            // read from the current buffer
            buffer[bufferLength] = buffer[readPos]; //*o = *(o - d);
          }
          bufferLength++;
        }

      }
      else {
        //if(o >= ol) {
        //  break;
        //}
        buffer[bufferLength] = ByteConverter.unsign(fm.readByte()); //*o++ = *p++;
        bufferLength++;
      }
    }

    // ready for the next iteration
    previousBuffer = buffer;
    previousBufferLength = bufferLength;

  }

  /**
  **********************************************************************************************
  So we can easily call this from within a Viewer plugin
  **********************************************************************************************
  **/
  public void open(FileManipulator fmIn, int readLengthIn) {
    try {
      fm = fmIn;

      readLength = readLengthIn;

      // Reset the globals
      previousBuffer = null;
      previousBufferLength = 0;

      bufferLength = 0;
      bufferPos = 0;

      firstTime = true;

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

      // Reset the globals
      previousBuffer = null;
      previousBufferLength = 0;

      bufferLength = 0;
      bufferPos = 0;

      firstTime = true;

    }
    catch (Throwable t) {
    }
  }

  /**
  **********************************************************************************************
  Not Supported
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
      // NOTE: The actual reading of the byte is done in available()
      return currentByte;
    }
    catch (Throwable t) {
      t.printStackTrace();
      readLength = 0;
      return 0;
    }
  }

}