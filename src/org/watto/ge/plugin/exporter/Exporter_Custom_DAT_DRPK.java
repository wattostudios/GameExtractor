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
import org.watto.io.buffer.XORBufferWrapper;
import org.watto.io.converter.ByteConverter;
import org.watto.io.converter.ShortConverter;

public class Exporter_Custom_DAT_DRPK extends ExporterPlugin {

  static Exporter_Custom_DAT_DRPK instance = new Exporter_Custom_DAT_DRPK();

  static FileManipulator readSource;
  static long readLength = 0;

  /**
  **********************************************************************************************
  SFL Block Decompression (iMatix) on top of XOR'd bytes
  **********************************************************************************************
  **/
  public static Exporter_Custom_DAT_DRPK getInstance() {
    return instance;
  }

  byte[] buffer = new byte[0];
  int bufferLength = 0;

  int bufferPos = 0;

  /**
  **********************************************************************************************

  **********************************************************************************************
  **/
  public Exporter_Custom_DAT_DRPK() {
    setName("SFL Block Decompression on XOR'd Bytes");
  }

  /**
  **********************************************************************************************

  **********************************************************************************************
  **/
  @Override
  public boolean available() {

    if (bufferPos >= bufferLength) {
      if (readLength <= 2) {
        return false; // end of file
      }
      // Need to decompress the next block

      // 2 - Compressed Block Length
      int compBlockLength = ShortConverter.unsign(readSource.readShort());
      readLength -= (compBlockLength + 2);

      byte[] sourceBuffer = readSource.readBytes(compBlockLength);
      buffer = new byte[33000]; // max decomp block length is 32750, but add a few as well

      bufferLength = expand_block(sourceBuffer, buffer, compBlockLength, buffer.length);
      if (bufferLength <= 0) {
        return false;
      }
      bufferPos = 0;
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
  Expands a block of data previously compressed using the compress_block() function. The
  compressed block is passed in src, the expanded result in dst. dst must be large enough to
  accomodate the largest possible decompressed block.  Returns the size of the uncompressed data.
  **********************************************************************************************
  **/
  public int expand_block(byte[] src, byte[] dst, int src_size, int max_dstsz) {

    //if (src[0] == 0x80) {
    if (ByteConverter.unsign(src[0]) == 0x80) {
      System.arraycopy(src, 1, dst, 0, src_size - 1);
      return (src_size - 1);
    }

    int SymbolAddress;
    int ChunkSize;
    int Counter;
    int Command = 0;
    int src_index = 1;

    byte Bit = 0;
    int dst_size = 0;

    while (src_index < src_size) {
      if (Bit == 0) {
        //Command = src[src_index++] << 8;
        //Command += src[src_index++];
        Command = ByteConverter.unsign(src[src_index++]) << 8;
        Command += ByteConverter.unsign(src[src_index++]);
        Bit = 16;
      }

      if ((Command & 0x8000) == 0x8000) {
        //SymbolAddress = (short) (src[src_index++] << 4);
        //SymbolAddress += (short) (src[src_index] >> 4);
        SymbolAddress = (short) (ByteConverter.unsign(src[src_index++]) << 4);
        SymbolAddress += (short) (ByteConverter.unsign(src[src_index]) >> 4);

        if (SymbolAddress != 0) {
          //ChunkSize = (short) (src[src_index++] & 0x0f) + 3;
          ChunkSize = (short) (ByteConverter.unsign(src[src_index++]) & 0x0f) + 3;
          SymbolAddress = dst_size - SymbolAddress;

          if (SymbolAddress < 0) {
            return (-1);
          }

          if ((dst_size + ChunkSize) > max_dstsz) {
            return (-1);
          }

          if ((SymbolAddress + ChunkSize) > max_dstsz) {
            return (-1);
          }

          for (Counter = 0; Counter < ChunkSize; Counter++) {
            dst[dst_size++] = dst[SymbolAddress++];
          }
        }
        else {
          //ChunkSize = (short) (src[src_index++] << 8);
          //ChunkSize += (short) (src[src_index++] + 16);
          ChunkSize = (short) (ByteConverter.unsign(src[src_index++]) << 8);
          ChunkSize += (short) (ByteConverter.unsign(src[src_index++]) + 16);

          if ((dst_size + ChunkSize) > max_dstsz) {
            return (-1);
          }

          for (Counter = 0; Counter < ChunkSize; Counter++) {
            dst[dst_size++] = src[src_index];
          }
          src_index++;
        }
      }
      else {
        if (dst_size >= max_dstsz) {
          return (-1);
        }
        dst[dst_size++] = src[src_index++];
      }
      Command <<= 1;
      Bit--;
    }
    return (dst_size);
  }

  /**
  **********************************************************************************************

  **********************************************************************************************
  **/
  @Override
  public String getDescription() {
    return "This exporter decompresses SFL Block-compressed files, where the bytestream is XOR'd with byte 74.\n\n" + super.getDescription();
  }

  /**
  **********************************************************************************************

  **********************************************************************************************
  **/
  @Override
  public void open(Resource source) {
    try {
      readSource = new FileManipulator(source.getSource(), false);
      readSource.seek(source.getOffset());

      // RESET GLOBALS
      bufferLength = 0;
      bufferPos = 0;

      // SET UP THE XOR
      readSource.setBuffer(new XORBufferWrapper(readSource.getBuffer(), 74));

      readLength = source.getLength();

    }
    catch (Throwable t) {
    }
  }

  /**
   **********************************************************************************************
   * NOT DONE
   **********************************************************************************************
   **/
  @Override
  public void pack(Resource source, FileManipulator destination) {
    try {
      //long decompLength = source.getDecompressedLength();

      ExporterPlugin exporter = source.getExporter();
      exporter.open(source);

      while (exporter.available()) {
        destination.writeByte(exporter.read());
      }

      exporter.close();

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
      //readLength--;

      byte returnByte = buffer[bufferPos];
      bufferPos++;

      return returnByte;
    }
    catch (Throwable t) {
      return 0;
    }
  }

}