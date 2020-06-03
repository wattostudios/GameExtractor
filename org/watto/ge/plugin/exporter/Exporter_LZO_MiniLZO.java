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

import org.jvcompress.lzo.MiniLZO;
import org.jvcompress.util.MInt;
import org.lzo.Constants;
import org.lzo.DataFormatException;
import org.lzo.Util;
import org.watto.ErrorLogger;
import org.watto.datatype.Resource;
import org.watto.ge.plugin.ExporterPlugin;
import org.watto.io.FileManipulator;
import org.watto.io.stream.ManipulatorInputStream;

public class Exporter_LZO_MiniLZO extends ExporterPlugin {

  static Exporter_LZO_MiniLZO instance = new Exporter_LZO_MiniLZO();

  static long readLength = 0;

  static long compLength = 0;

  static int currentByte = 0;

  /**
  **********************************************************************************************
  Reads a single block from a raw LZO data stream
  Based on code from http://www.oberhumer.com/opensource/lzo/download/LZO-v1/
  **********************************************************************************************
  **/
  public static Exporter_LZO_MiniLZO getInstance() {
    return instance;
  }

  boolean forceDecompress = false;

  byte outBuf[] = null;

  int outBufReadPos = 0;

  int outBufSize = 0;

  int blockSize = 0;

  ManipulatorInputStream readStream;

  FileManipulator fm;

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public Exporter_LZO_MiniLZO() {
    setName("LZO Compression");
  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  @Override
  public boolean available() {
    try {

      if (readLength > 0) {
        if (outBufReadPos >= outBufSize) {
          // we've reached the end of the outBuffer - need to read the next block

          int inBufSize = (int) compLength;
          int in = 0;
          //outBufSize = (int) readLength; // TODO CHECK
          outBufSize = (int) readLength + outBufReadPos; // TODO CHECK

          //int out = 0; // TODO CHECK 
          //int out = outBufReadPos; // TODO CHECK start part-way through for multiple blocks

          byte[] inBuf = new byte[(int) compLength];
          Util.xread(readStream, inBuf, in, inBufSize, false);

          if (forceDecompress || inBufSize <= outBufSize) {
            // decompress

            MInt out_len = new MInt();
            int r = MiniLZO.lzo1x_decompress(inBuf, (int) inBufSize, outBuf, out_len);
            // Allow INPUT_NOT_CONSUMED because we're only tracking the decompressed size, not the compressed size
            if ((r != Constants.LZO_E_OK && r != Constants.LZO_E_INPUT_NOT_CONSUMED) || (outBufReadPos + out_len.v) != outBufSize) {
              throw new DataFormatException("compressed data violation");
            }
          }
          else {
            // original (incompressible) block
            outBuf = inBuf;
            outBufSize = inBufSize;
          }

          //outBufReadPos = 0; // TODO CHECK
        }

        currentByte = outBuf[outBufReadPos];
        outBufReadPos++;

        readLength--;

        return true;
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
      fm.close();
      readStream.close();
      readStream = null;
    }
    catch (Throwable t) {
      readStream = null;
    }
  }

  public boolean getForceDecompress() {
    return forceDecompress;
  }

  /**
  **********************************************************************************************
  So we can easily call this from within a Viewer plugin
  **********************************************************************************************
  **/
  public void open(FileManipulator fmIn, int compLengthIn, int decompLengthIn) {
    try {
      fm = fmIn;

      readStream = new ManipulatorInputStream(fm);
      readLength = decompLengthIn;
      compLength = compLengthIn;

      blockSize = decompLengthIn;
      /*// TODO CHECK
      outBuf = new byte[blockSize];
      outBufReadPos = 0;
      outBufSize = 0;
      */

      if (outBuf == null) {
        outBuf = new byte[blockSize];
        outBufReadPos = 0;
        outBufSize = 0;
      }
      else {
        // retain the last block of uncompressed data, in case we need to refer to it in the current block decompression
        byte[] oldBuf = outBuf;
        int oldBufLength = oldBuf.length;

        int newLength = oldBufLength + blockSize;
        int maxLength = blockSize + 40000;
        if (newLength > maxLength) {
          newLength = maxLength;
        }

        int copySize = newLength - blockSize;
        int copyStartPos = oldBufLength - copySize;

        outBuf = new byte[newLength];
        System.arraycopy(oldBuf, copyStartPos, outBuf, 0, copySize);

        outBufReadPos = copySize;
        outBufSize = copySize;
      }

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
      compLength = source.getLength();

      open(fm, (int) compLength, (int) readLength);
    }
    catch (Throwable t) {
    }
  }

  /**
  **********************************************************************************************
  NOT SUPPORTED
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

  public void setForceDecompress(boolean forceDecompress) {
    this.forceDecompress = forceDecompress;
  }

}