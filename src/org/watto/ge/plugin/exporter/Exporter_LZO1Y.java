/*
 * Application:  Game Extractor
 * Author:       wattostudios
 * Website:      http://www.watto.org
 * Copyright:    Copyright (c) 2002-2024 wattostudios
 *
 * License Information:
 * This program is free software; you can redistribute it and/or modify it under the terms of the GNU General Public License
 * published by the Free Software Foundation; either version 2 of the License, or (at your option) any later versions. This
 * program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranties
 * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License at http://www.gnu.org for more
 * details. For further information on this application, refer to the authors' website.
 */

package org.watto.ge.plugin.exporter;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import org.anarres.lzo.LzoAlgorithm;
import org.anarres.lzo.LzoDecompressor;
import org.anarres.lzo.LzoInputStream;
import org.anarres.lzo.LzoLibrary;
import org.lzo.Util;
import org.watto.ErrorLogger;
import org.watto.datatype.Resource;
import org.watto.ge.plugin.ExporterPlugin;
import org.watto.io.FileManipulator;
import org.watto.io.stream.ManipulatorInputStream;

public class Exporter_LZO1Y extends ExporterPlugin {

  static Exporter_LZO1Y instance = new Exporter_LZO1Y();

  long readLength = 0;

  long compLength = 0;

  int currentByte = 0;

  boolean swapHeaderFields = false;

  public boolean isSwapHeaderFields() {
    return swapHeaderFields;
  }

  public void setSwapHeaderFields(boolean swapHeaderFields) {
    this.swapHeaderFields = swapHeaderFields;
  }

  /**
  **********************************************************************************************
  Reads a LZO1Y data stream with a 8-byte header
  **********************************************************************************************
  **/
  public static Exporter_LZO1Y getInstance() {
    return instance;
  }

  /** True to reset the buffer for each open(), False to retain the buffer to use for the next block **/
  boolean resetBuffer = true;

  public boolean isResetBuffer() {
    return resetBuffer;
  }

  public void setResetBuffer(boolean resetBuffer) {
    this.resetBuffer = resetBuffer;
  }

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
  public Exporter_LZO1Y() {
    setName("LZO1Y Compression");
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
          outBufSize = (int) readLength + outBufReadPos;

          //int paddedSize = (int) compLength + ArchivePlugin.calculatePadding(compLength, 262144)+8;
          //byte[] inBuf = new byte[(int) paddedSize]; // make a larger buffer, but still only fill to the actual size

          byte[] inBuf = new byte[(int) compLength];

          Util.xread(readStream, inBuf, in, inBufSize, false);

          // for this one, it reads the block size from the first 2 INTs, but it reads as BIG ENDIAN instead of LITTLE. We need to swap this.
          if (swapHeaderFields) {
            byte[] swappedBytes = new byte[] { inBuf[7], inBuf[6], inBuf[5], inBuf[4], inBuf[3], inBuf[2], inBuf[1], inBuf[0] };
            System.arraycopy(swappedBytes, 0, inBuf, 0, 8);
          }

          //ManipulatorInputStream inStream = new ManipulatorInputStream(new FileManipulator(new ByteBuffer(inBuf)));
          InputStream inStream = new ByteArrayInputStream(inBuf);
          LzoAlgorithm algorithm = LzoAlgorithm.LZO1Y;
          LzoDecompressor decompressor = LzoLibrary.getInstance().newDecompressor(algorithm, null);
          LzoInputStream stream = new LzoInputStream(inStream, decompressor);
          //while (stream.available() > 0) {
          int outWritePos = 0;
          while (outWritePos < outBufSize) {
            int readByte = stream.read();
            if (readByte < 0) {
              break; // End Of Stream
            }
            outBuf[outWritePos] = (byte) readByte;
            outWritePos++;
          }
          stream.close();

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

  /**
  **********************************************************************************************
  So we can easily call this from within a Viewer plugin
  **********************************************************************************************
  **/
  public void open(FileManipulator fmIn, int compLengthIn, int decompLengthIn) {
    try {
      fm = fmIn;

      // RESET GLOBALS
      if (resetBuffer) {
        outBuf = null;
        outBufReadPos = 0;
        outBufSize = 0;
        blockSize = 0;
      }

      readStream = new ManipulatorInputStream(fm);
      readLength = decompLengthIn;
      compLength = compLengthIn;

      blockSize = decompLengthIn;

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

}