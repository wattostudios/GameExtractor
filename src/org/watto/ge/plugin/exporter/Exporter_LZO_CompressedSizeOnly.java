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
import org.watto.ErrorLogger;
import org.watto.datatype.Resource;
import org.watto.ge.plugin.ExporterPlugin;
import org.watto.io.FileManipulator;
import org.watto.io.stream.ManipulatorInputStream;

public class Exporter_LZO_CompressedSizeOnly extends ExporterPlugin {

  static Exporter_LZO_CompressedSizeOnly instance = new Exporter_LZO_CompressedSizeOnly();

  static long readLength = 0;

  static long compLength = 0;

  static int currentByte = 0;

  static Lzo1xDecompressor bc = null;

  static final byte magic[] = { 0x00, (byte) 0xe9, 0x4c, 0x5a, 0x4f, (byte) 0xff, 0x1a };

  /**
  **********************************************************************************************
  Reads a single block from a raw LZO data stream
  Based on code from http://www.oberhumer.com/opensource/lzo/download/LZO-v1/
  **********************************************************************************************
  **/
  public static Exporter_LZO_CompressedSizeOnly getInstance() {
    return instance;
  }

  byte outBuf[] = null;

  int outBufReadPos = 0;

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public Exporter_LZO_CompressedSizeOnly() {
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
  }

  /**
  **********************************************************************************************
  So we can easily call this from within a Viewer plugin
  **********************************************************************************************
  **/
  public void open(FileManipulator fmIn, int compLengthIn, int decompLengthIn) {
    try {

      // We don't actually know the decomp length, so make it up
      int decompSizeMultiple = 5; // guess 5:1 compression for the first try (and make it larger if needed, later in the decomp loop below)

      ManipulatorInputStream readStream = new ManipulatorInputStream(fmIn);

      bc = new Lzo1xDecompressor();

      byte[] inBuf = new byte[compLengthIn];
      Util.xread(readStream, inBuf, 0, compLengthIn, false);

      boolean decompressedOK = false;
      while (!decompressedOK) {
        decompLengthIn = compLengthIn * decompSizeMultiple;
        if (decompSizeMultiple > 20 && decompLengthIn > 1000000) {
          // Still want a limit on files that have a huge decomp multiple, so we don't blow memory out of the water.
          // They could just be incredibly compressed, but don't want to decompress to a massive size.
          decompressedOK = true;
          continue;
        }

        outBuf = new byte[decompLengthIn];
        outBufReadPos = 0;

        Int newSize = new Int(decompLengthIn);
        int r = bc.decompress(inBuf, 0, compLengthIn, outBuf, 0, newSize);
        if (r == Constants.LZO_E_OUTPUT_OVERRUN) {
          // decomp buffer isn't large enough, make it larger and try the decompression again;
          decompSizeMultiple += 5;
          if (decompSizeMultiple > 100) {
            decompressedOK = true; // don't keep trying - if it's better than 100:1 compression, too bad
          }
          continue;
        }

        if ((r != Constants.LZO_E_OK && r != Constants.LZO_E_INPUT_NOT_CONSUMED)) {
          decompressedOK = true;
          throw new DataFormatException("compressed data violation");
        }

        decompressedOK = true;
        readLength = newSize.intValue();
      }

      try {
        fmIn.close();
        readStream.close();
        readStream = null;
      }
      catch (Throwable t) {
        readStream = null;
      }

    }
    catch (Throwable t) {
      ErrorLogger.log(t);
    }
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

      readLength = source.getDecompressedLength();
      compLength = source.getLength();

      open(fm, (int) compLength, (int) readLength);

      source.setDecompressedLength(readLength); // important, as the Thumbnail Viewer (ExporterByteBuffer) relies on an accurate decompLength
    }
    catch (Throwable t) {
      ErrorLogger.log(t);
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