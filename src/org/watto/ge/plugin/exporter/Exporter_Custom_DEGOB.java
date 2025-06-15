/*
 * Application:  Game Extractor
 * Author:       wattostudios
 * Website:      http://www.watto.org
 * Copyright:    Copyright (c) 2002-2025 wattostudios
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
import org.watto.ge.helper.FieldValidator;
import org.watto.ge.plugin.ExporterPlugin;
import org.watto.io.FileManipulator;
import org.watto.io.converter.ByteConverter;

/**
**********************************************************************************************
Ref: https://github.com/scummvm/scummvm-tools/blob/master/engines/gob/degob_script.cpp#L117
Not really sure if this works or not
**********************************************************************************************
**/
public class Exporter_Custom_DEGOB extends ExporterPlugin {

  static Exporter_Custom_DEGOB instance = new Exporter_Custom_DEGOB();

  static FileManipulator readSource;
  static long compLength = 0;
  static long readLength = 0;

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public static Exporter_Custom_DEGOB getInstance() {
    return instance;
  }

  int[] buffer = new int[0];
  int bufferLength = 0;
  int bufferPos = 0;

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public Exporter_Custom_DEGOB() {
    setName("GOB Compression");
  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  @Override
  public boolean available() {
    return bufferPos < bufferLength;
    /*
    if (bufferPos >= bufferLength) {
      // we need to read the next block of data
    
      if (readLength <= 0) {
        return false; // end of all blocks
      }
    
      buffer = unpackBlock();
      bufferPos = 0;
      bufferLength = buffer.length;
    
    }
    
    return readLength > 0 || bufferLength > 0;
    */

  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public int[] unpackBlock() {

    int size = (int) compLength;
    int counter = (int) compLength;

    int off = 0;
    int len = 0;

    int[] tmpBuf = new int[4114];
    for (int i = 0; i < 4078; i++) {
      tmpBuf[i] = 0x20;
    }
    int tmpIndex = 4078;

    int[] dest = new int[(int) readLength];
    int destPos = 0;

    int cmd = 0;
    int readBytes = 0;
    while (readBytes <= size) {
      cmd >>= 1;
      if ((cmd & 0x0100) == 0) {
        cmd = ByteConverter.unsign(readSource.readByte()) | 0xFF00; //cmd = *packedData | 0xFF00;
        readBytes++;
      }
      if ((cmd & 1) != 0) { /* copy */

        //*dest++ = *packedData;
        //tmpBuf[tmpIndex] = *packedData;
        //packedData++;

        int currentByte = ByteConverter.unsign(readSource.readByte());
        dest[destPos] = currentByte;
        destPos++;

        tmpBuf[tmpIndex] = currentByte;
        readBytes++;

        tmpIndex++;
        tmpIndex %= 4096;
        counter--;
        if (counter == 0) {
          break;
        }
      }
      else { /* copy string */

        //off = *packedData++;
        //off |= (*packedData & 0xF0) << 4;
        //len = (*packedData & 0x0F) + 3;
        //packedData++;

        int byte1 = ByteConverter.unsign(readSource.readByte());
        int byte2 = ByteConverter.unsign(readSource.readByte());
        readBytes += 2;

        off = byte1;
        off |= (byte2 & 0xF0) << 4;
        len = (byte2 & 0x0F) + 3;

        for (int i = 0; i < len; i++) {
          //*dest++ = tmpBuf[(off + i) % 4096];
          dest[destPos] = tmpBuf[(off + i) % 4096];
          destPos++;

          counter--;

          if (counter == 0) {
            readLength -= readBytes;
            return dest;
          }
          tmpBuf[tmpIndex] = tmpBuf[(off + i) % 4096];
          tmpIndex++;
          tmpIndex %= 4096;
        }

      }
    }

    readLength -= readBytes;
    return dest;
  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  @Override
  public void close() {
    try {
      readSource.close();
      readSource = null;
    }
    catch (Throwable t) {
      readSource = null;
    }
  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  @Override
  public void open(Resource source) {
    try {
      long fileLength = source.getLength();

      // Reset globals
      bufferPos = 0;
      bufferLength = 0;

      readSource = new FileManipulator(source.getSource(), false);
      readSource.seek(source.getOffset());

      // 4 - Compressed Length
      readLength = readSource.readInt();
      FieldValidator.checkLength(readLength, fileLength);

      compLength = readLength;

      buffer = unpackBlock();
      bufferPos = 0;
      bufferLength = buffer.length;

    }
    catch (Throwable t) {
      readLength = 0;
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
      int readByte = buffer[bufferPos];
      bufferPos++;
      return readByte;
    }
    catch (Throwable t) {
      return 0;
    }
  }

}