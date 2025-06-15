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

public class Exporter_Custom_JA_ARCHINFO_CFIL extends ExporterPlugin {

  static Exporter_Custom_JA_ARCHINFO_CFIL instance = new Exporter_Custom_JA_ARCHINFO_CFIL();

  static FileManipulator readSource;
  static long readLength = 0;

  /**
  **********************************************************************************************
  NOT WORKING
  **********************************************************************************************
  **/
  public static Exporter_Custom_JA_ARCHINFO_CFIL getInstance() {
    return instance;
  }

  byte[] buffer = new byte[0];
  int bufferPos = 0;

  int bufferLength = 0;

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public Exporter_Custom_JA_ARCHINFO_CFIL() {
    setName("JA CFIL Compression");
  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  @Override
  public boolean available() {
    if (readLength <= 0) {
      return false;
    }

    // decompress the next block of data
    if (bufferPos >= bufferLength) {
      // we've read the whole buffer, need to load the next one

      int blockSize = readSource.readShort();
      if (blockSize > 0) {
        // decompress a block
        decompressBlock(blockSize);
      }
      else {
        // a raw block
        blockSize = 32768 + blockSize - 1;

        buffer = readSource.readBytes(blockSize);
        bufferLength = blockSize;
        bufferPos = 0;
      }
    }

    return true;
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
  public void decompressBlock(int blockSize) {
    try {
      // reset all the buffers before reading
      buffer = new byte[32768];
      bufferLength = 0;
      bufferPos = 0;

      // do the decompression
      int currentByte;
      int tempHolder;
      int temp2Value;
      int tempIt;

      byte[] tempBuffer2 = new byte[255];

      int[] tempBuffer3 = new int[255];
      for (int i = 0; i < 255; i++) {
        tempBuffer3[i] = i;
      }

      byte[] tempBuffer4 = new byte[255];

      int decompBufferPos = 0;

      int counter = 0;

      byte[] buffer1 = readSource.readBytes(blockSize);

      while (counter < (blockSize + 1)) {

        int[] tempBuffer1 = new int[255];
        for (int i = 0; i < 255; i++) {
          tempBuffer1[i] = tempBuffer3[i];
        }

        currentByte = ByteConverter.unsign(buffer1[counter]);
        counter = counter + 1;
        tempHolder = 0;

        while (tempHolder < 256) {
          if (currentByte > 127) {
            tempHolder = currentByte + tempHolder - 127;
          }
          else {
            if (currentByte >= 0) {
              tempIt = currentByte + 1;
              while (tempIt > 0) {
                tempBuffer1[tempHolder] = buffer1[counter];
                counter = counter + 1;
                if (tempHolder != tempBuffer1[tempHolder]) {
                  tempBuffer4[tempHolder] = buffer1[counter];
                  counter++;
                }
                tempHolder++;
                tempIt--;
              }
            }
          }

          if (tempHolder < 256) {
            currentByte = ByteConverter.unsign(buffer1[counter]);
            counter++;
          }
        }

        tempIt = ((buffer1[counter] << 8) | buffer1[counter + 1]);

        counter += 2;
        temp2Value = 0;

        while (true) {
          if (temp2Value != 0) {
            temp2Value--;
            tempHolder = tempBuffer2[temp2Value];
          }
          else {
            tempHolder = tempIt;
            tempIt--;

            if (tempHolder == 0) {
              break; // exit the loop
            }

            tempHolder = buffer1[counter];
            counter++;
          }

          currentByte = tempBuffer1[tempHolder];

          if (tempHolder == (currentByte & 255)) {
            buffer[decompBufferPos] = (byte) tempHolder;
            decompBufferPos++;
          }
          else {
            tempHolder = tempBuffer4[tempHolder];
            tempBuffer2[temp2Value] = (byte) tempHolder;
            tempBuffer2[temp2Value + 1] = (byte) currentByte;
            temp2Value += 2;
          }
        }

      }

    }
    catch (Throwable t) {
      ErrorLogger.log(t);
      readLength = 0; // force-quit the decompression!
    }
  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  @Override
  public void open(Resource source) {
    try {
      readLength = source.getDecompressedLength();

      readSource = new FileManipulator(source.getSource(), false, 32772); // 32772 = 32768 + 4 (maxBlockSize + 4-byte header)
      readSource.seek(source.getOffset());

    }
    catch (Throwable t) {
    }
  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  @Override
  public void pack(Resource source, FileManipulator destination) {
    try {

      ExporterPlugin exporter = source.getExporter();
      exporter.open(source);

      while (exporter.available()) {
        destination.writeByte(exporter.read());
      }

      exporter.close();

      //destination.forceWrite();

    }
    catch (Throwable t) {
      logError(t);
    }
  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  @Override
  public int read() {
    try {

      byte byteToReturn = buffer[bufferPos];
      bufferPos++;

      readLength--;

      return byteToReturn;

    }
    catch (Throwable t) {
      return 0;
    }
  }

}