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

import java.util.zip.InflaterInputStream;
import org.watto.ErrorLogger;
import org.watto.datatype.Resource;
import org.watto.ge.plugin.ExporterPlugin;
import org.watto.io.FileManipulator;
import org.watto.io.buffer.ByteBuffer;
import org.watto.io.stream.ManipulatorInputStream;

public class Exporter_Custom_ARCH00_LTAR extends ExporterPlugin {

  static Exporter_Custom_ARCH00_LTAR instance = new Exporter_Custom_ARCH00_LTAR();

  static FileManipulator readSource;
  static long readLength = 0;

  /**
  **********************************************************************************************

  **********************************************************************************************
  **/
  public static Exporter_Custom_ARCH00_LTAR getInstance() {
    return instance;
  }

  int[] buffer = new int[0];
  int bufferLength = 0;
  int bufferPos = 0;

  /**
  **********************************************************************************************

  **********************************************************************************************
  **/
  public Exporter_Custom_ARCH00_LTAR() {
    setName("Chunked ZLib Compression for FEAR games");
  }

  /**
  **********************************************************************************************

  **********************************************************************************************
  **/
  @Override
  public boolean available() {
    if (bufferPos >= bufferLength) {
      // we need to read the next block of data

      if (readLength <= 0) {
        return false; // end of all blocks
      }

      // 4 - Compressed Block Length
      int compLength = readSource.readInt();

      // 4 - Decompressed Block Length
      int decompLength = readSource.readInt();

      if (compLength < 0 || compLength > 100000 || decompLength < 0 || decompLength > 100000) {
        return false; // bad data
      }

      long nextOffset = readSource.getOffset() + compLength;

      int paddingSize = 4 - ((int) nextOffset % 4); // compressed blocks are padded to multiples of 4 bytes
      if (paddingSize != 4) {
        nextOffset += paddingSize;
      }

      // Decompress the block
      buffer = new int[decompLength];
      bufferPos = 0;
      bufferLength = decompLength;

      try {
        byte[] compData = readSource.readBytes(compLength); // so Inflator doesn't overshoot

        InflaterInputStream readStream = new InflaterInputStream(new ManipulatorInputStream(new FileManipulator(new ByteBuffer(compData))));
        for (int i = 0; i < decompLength; i++) { // because Inflator won't fill the buffer if reading into a byte[], so do it byte-by-byte
          buffer[i] = readStream.read();
        }
        readStream.close();

        readSource.seek(nextOffset); // ensure we leave the file pointer at the correct place - the Inflater tends to overshoot
      }
      catch (Throwable t) {
        ErrorLogger.log(t);
      }

      readLength -= (compLength + 8 + paddingSize);
    }

    return readLength > 0 || bufferLength > 0;
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
      readLength = source.getLength();

      // Reset globals
      bufferPos = 0;
      bufferLength = 0;

      readSource = new FileManipulator(source.getSource(), false, 65544); //65536 + 8 
      readSource.seek(source.getOffset());

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
      int readByte = buffer[bufferPos];
      bufferPos++;
      return readByte;
    }
    catch (Throwable t) {
      return 0;
    }
  }

}