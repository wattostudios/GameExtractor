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

public class Exporter_LZ4X extends ExporterPlugin {

  static Exporter_LZ4X instance = new Exporter_LZ4X();

  static long readLength = 0;

  static int currentByte = 0;

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public static Exporter_LZ4X getInstance() {
    return instance;
  }

  int BLOCK_SIZE = (8 << 20); // 8 MB

  int COMPRESS_BOUND = (16 + BLOCK_SIZE + (BLOCK_SIZE / 255));

  //byte[] buf = new byte[BLOCK_SIZE + COMPRESS_BOUND];
  int[] buf = new int[0];

  int[] readBuffer = new int[0];

  int readBufferPos = 0;

  int readBufferLength = 0;

  FileManipulator fm;

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public Exporter_LZ4X() {
    setName("LZ4X Compression");
  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  @Override
  public boolean available() {
    try {

      if (readLength > 0) {
        if (readBufferPos >= readBufferLength) {
          // finished reading the current buffer, need to read and decompress some more data
          decompress();
        }

        if (readBufferLength == -1) {
          return false; // no more data, or an error
        }

        currentByte = readBuffer[readBufferPos];
        readBufferPos++;

        readLength--;

        if (currentByte >= 0) {
          return true;
        }
      }

      return false;
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
      buf = new int[0];
      readBuffer = new int[0];
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
  void decompress() {

    int[] previousBuffer = readBuffer;
    int previousBufferLength = readBufferLength;

    readBuffer = new int[0];
    readBufferPos = 0;
    readBufferLength = -1;

    // read the bSize
    int bsize = fm.readInt();

    if (bsize < 0 || bsize > COMPRESS_BOUND) {
      ErrorLogger.log("[Exporter_LZ4X]: no more data --> " + bsize);
      return; // no more data
    }

    //myfread(&buf[BLOCK_SIZE], 1, bsize, fin)
    // Read bytes from the file and put them at the end of the block_size
    byte[] bsizeBytes = fm.readBytes(bsize);
    //System.arraycopy(bsizeBytes, 0, buf, BLOCK_SIZE, bsize);
    // so we can make the bytes all unsigned, we write them 1 byte at a time into the buffer
    for (int i = 0; i < bsize; i++) {
      buf[BLOCK_SIZE + i] = ByteConverter.unsign(bsizeBytes[i]);
    }

    int p = 0;
    int bp = 0;

    while (bp < bsize) {
      //int tag = buf[BLOCK_SIZE + (bp++)];
      int readingPos = BLOCK_SIZE + (bp++);
      int bufAtReadingPos = buf[readingPos];
      int tag = bufAtReadingPos;

      if (tag >= 16) {
        int run = tag >> 4;
        if (run == 15) {
          for (;;) {
            int c = buf[BLOCK_SIZE + (bp++)];
            run += c;
            if (c != 255) {
              break;
            }
          }

          for (int i = 0; i < run; i += 16) {
            //copy128(p+i, BLOCK_SIZE+bp+i);
            System.arraycopy(buf, BLOCK_SIZE + bp + i, buf, p + i, 16);
          }
        }
        else {
          //copy128(p, BLOCK_SIZE+bp);
          System.arraycopy(buf, BLOCK_SIZE + bp, buf, p, 16);
        }

        p += run;
        bp += run;

        if (bp >= bsize) {
          break;
        }
      }

      //int s = p - (buf[BLOCK_SIZE + (bp++)]);
      readingPos = BLOCK_SIZE + (bp++);
      bufAtReadingPos = buf[readingPos];
      int s = p - bufAtReadingPos;

      //s -= (buf[BLOCK_SIZE + (bp++)]) << 8;
      readingPos = BLOCK_SIZE + (bp++);
      bufAtReadingPos = buf[readingPos];
      s -= (bufAtReadingPos << 8);

      int len = tag & 15;
      if (len == 15) {
        for (;;) {
          int c = buf[BLOCK_SIZE + (bp++)];
          len += c;
          if (c != 255) {
            break;
          }
        }
      }
      len += 4;

      if ((p - s) >= 16) {
        for (int i = 0; i < len; i += 16) {
          //copy128(p+i, s+i);
          //System.arraycopy(buf, s + i, buf, p + i, 16);
          //// Does the same thing as the line above, but prevents negative array indexes
          for (int j = 0; j < 16; j++) {
            int readPos = s + i + j;
            if (readPos < 0) {
              // need to read from the previously-decoded buffer
              int previousBufReadPos = previousBufferLength + readPos;
              buf[p + i + j] = previousBuffer[previousBufReadPos];
            }
            else {
              buf[p + i + j] = buf[readPos];
            }
          }

        }
        p += len;
      }
      else {
        while (len-- > 0) {
          if (s < 0) {
            // need to read from the previously-decoded buffer
            int previousBufReadPos = previousBufferLength + s;
            buf[p++] = previousBuffer[previousBufReadPos];
            s++;
          }
          else {
            buf[p++] = buf[s++];
          }
        }
      }
    }

    if (bp != bsize) {
      ErrorLogger.log("[Exporter_LZ4X]: bp != bsize --> " + bp + "!=" + bsize);
      return; // error
    }

    //myfwrite(buf, 1, p, fout);
    // put the decompressed bytes into the readBuffer, which is returned from read()
    readBuffer = new int[p];
    System.arraycopy(buf, 0, readBuffer, 0, p);
    readBufferLength = p;

    //System.out.println("[Exporter_LZ4X]: Decompressed bytes: " + p);

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

      buf = new int[BLOCK_SIZE + COMPRESS_BOUND];

      readBufferPos = 0;
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

      // TODO TEMP
      //fm.seek(29700);
      //readLength = 6270;

      buf = new int[BLOCK_SIZE + COMPRESS_BOUND];

      readBufferPos = 0;
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