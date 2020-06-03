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

import org.lzo.Constants;
import org.lzo.DataFormatException;
import org.lzo.Int;
import org.lzo.Lzo1xDecompressor;
import org.lzo.Util;
import org.watto.datatype.Resource;
import org.watto.ge.plugin.ExporterPlugin;
import org.watto.io.FileManipulator;
import org.watto.io.stream.ManipulatorInputStream;

public class Exporter_LZO extends ExporterPlugin {

  static Exporter_LZO instance = new Exporter_LZO();

  static long readLength = 0;
  static int currentByte = 0;

  static Lzo1xDecompressor bc = null;
  static final byte magic[] = { 0x00, (byte) 0xe9, 0x4c, 0x5a, 0x4f, (byte) 0xff, 0x1a };

  /**
  **********************************************************************************************
  LZO Decompression, including header bytes, etc (UNTESTED).
  Based on code from http://www.oberhumer.com/opensource/lzo/download/LZO-v1/
  **********************************************************************************************
  **/
  public static Exporter_LZO getInstance() {
    return instance;
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
  public Exporter_LZO() {
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

          // read uncompressed size
          outBufSize = Util.xread32(readStream);
          outBuf = new byte[outBufSize];

          // exit if last block (EOF marker)
          if (outBufSize == 0) {
            return false;
          }

          // read compressed size
          int inBufSize = Util.xread32(readStream);

          // sanity check of the size values
          if (inBufSize > blockSize || outBufSize > blockSize ||
              inBufSize <= 0 || outBufSize <= 0 || inBufSize > outBufSize) {
            throw new DataFormatException("block size error - data corrupted");
          }

          // place compressed block at the top of in_buf[]
          int in = blockSize - inBufSize;

          int out = 0;
          byte[] inBuf = new byte[blockSize];
          Util.xread(readStream, inBuf, in, inBufSize, false);

          if (inBufSize < outBufSize) {
            /* decompress */
            Int newSize = new Int(outBufSize);
            int r = bc.decompress(inBuf, in, inBufSize, outBuf, out, newSize);
            if (r != Constants.LZO_E_OK || newSize.intValue() != outBufSize) {
              throw new DataFormatException("compressed data violation");
            }
          }
          else {
            /* write original (incompressible) block */
            outBuf = inBuf;
            outBufSize = inBufSize;
          }

          outBufReadPos = 0;
        }

        currentByte = outBuf[outBufReadPos];
        outBufReadPos++;

        readLength--;

        return true;
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
  @SuppressWarnings("unused")
  public void open(FileManipulator fmIn, int readLengthIn) {
    try {
      fm = fmIn;

      readStream = new ManipulatorInputStream(fm);

      bc = new Lzo1xDecompressor();

      /*
       * Step 1: check magic header, read flags & block size, init checksum
       */
      byte m[] = new byte[magic.length];
      if (Util.xread(readStream, m, 0, m.length, true) != m.length ||
          Util.memcmp(m, magic, m.length) != 0) {
        throw new DataFormatException("header error - this file is not compressed by lpack");
      }
      int flags = Util.xread32(readStream);
      int method = Util.xgetc(readStream);
      int level = Util.xgetc(readStream);
      if (method != 1) {
        throw new DataFormatException("header error - invalid method " + method);
      }
      blockSize = Util.xread32(readStream);
      if (blockSize < 1024 || blockSize > 1024 * 1024) {
        throw new DataFormatException("header error - invalid block size " + blockSize);
      }

      /*
       * Step 2: allocate buffers for decompression
       */
      outBuf = new byte[blockSize];

      outBufReadPos = 0;
      outBufSize = 0;

      readLength = readLengthIn;
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

      open(fm, (int) readLength);
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