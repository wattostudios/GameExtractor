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
import org.watto.io.converter.ShortConverter;

public class Exporter_SFL_Nulls extends ExporterPlugin {

  static Exporter_SFL_Nulls instance = new Exporter_SFL_Nulls();

  static FileManipulator readSource;
  static long readLength = 0;

  /**
  **********************************************************************************************
  SFL Nulls Decompression (iMatix)
  **********************************************************************************************
  **/
  public static Exporter_SFL_Nulls getInstance() {
    return instance;
  }

  byte[] buffer = new byte[0];
  int bufferLength = 0;

  int bufferPos = 0;

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public Exporter_SFL_Nulls() {
    setName("SFL Nulls Decompression (iMatix)");
  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  @Override
  public boolean available() {

    if (bufferPos >= bufferLength) {
      if (readLength <= 0) {
        return false; // end of file
      }
      // Need to decompress the next block

      // 2 - Compressed Block Length
      int compBlockLength = ShortConverter.unsign(readSource.readShort());
      readLength -= (compBlockLength + 2);

      byte[] sourceBuffer = readSource.readBytes(compBlockLength);
      buffer = new byte[compBlockLength * 10]; // max compression is 10x

      bufferLength = expand_nulls(sourceBuffer, buffer, compBlockLength, bufferLength);
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
  Expands a block of data previously compressed using the compress_bits() function. The
  compressed block is passed in src, the expanded result in dst.  Dst must be large enough to
  accomodate the largest possible decompressed block.  Returns the size of the expanded data.

  !! UNTESTED !!
  **********************************************************************************************
  **/
  public int expand_bits(byte[] src, byte[] dst, short src_size, int max_dstsz) {
    int dst_size = 0; /*  Size of expanded data      */
    int src_scan = 0; /*  Scan through source data     */
    int length; /*  Size of the run or string    */
    int cur_byte; /*  Next byte to process       */

    while (src_scan < src_size) {
      cur_byte = ByteConverter.unsign(src[src_scan++]);

      if (cur_byte < 8) { /*  Single bit in position 0 to 7  */
        if (dst_size >= max_dstsz) {
          return (-1);
        }
        dst[dst_size++] = (byte) (1 << cur_byte);
      }
      else {
        if (cur_byte < 128) { /*  String of 1 to 120 bytes     */
          length = cur_byte - 7;

          if ((dst_size + length) > max_dstsz) {
            return (-1);
          }

          System.arraycopy(src, src_scan, dst, dst_size, length);

          src_scan += length;
          dst_size += length;
        }
        else { /*  Run of 1 or more bytes       */

          switch (cur_byte) {
            case 0x80: /*  381-2^16 binary zeroes       */
              length = ByteConverter.unsign(src[src_scan++]);
              length += ByteConverter.unsign(src[src_scan++]) << 8;
              cur_byte = 0;
              break;
            case 0x81:
              length = ByteConverter.unsign(src[src_scan++]);
              if (length == 0xFE) { /*  4-255 non-zero bytes       */
                length = ByteConverter.unsign(src[src_scan++]);
                cur_byte = ByteConverter.unsign(src[src_scan++]);
              }
              else if (length == 0xFF) { /*  Run of 256-2^15 non-zero bytes   */
                length = ByteConverter.unsign(src[src_scan++]);
                length += ByteConverter.unsign(src[src_scan++]) << 8;
                cur_byte = ByteConverter.unsign(src[src_scan++]);
              }
              else {
                length += 127;
                cur_byte = 0; /*  127 to 380 zeroes        */
              }
              break;
            default: /*  1 to 126 zeroes          */
              length = (cur_byte - 1) & 127;
              cur_byte = 0;
          }

          if ((dst_size + length) > max_dstsz) {
            return (-1);
          }

          for (int i = 0; i < length; i++) {
            dst[dst_size + i] = (byte) cur_byte;
          }

          dst_size += length;
        }
      }
    }
    return (dst_size); /*  Return expanded data size    */
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
  Expands a block of data previously compressed using the compress_nulls() function. The
  compressed block is passed in src; the expanded result in dst.  Dst must be large enough to
  accomodate the largest possible decompressed block.  Returns the size of the expanded data.
  
  !! UNTESTED !!
  **********************************************************************************************
  **/
  public int expand_nulls(byte[] src, byte[] dst, int src_size, int max_dstsz) {
    int dst_size = 0; /*  Size of expanded data      */
    int src_scan = 0; /*  Scan through source data     */
    int length; /*  Size of the run or string    */
    int cur_byte; /*  Next byte to process       */

    while (src_scan < src_size) {
      cur_byte = ByteConverter.unsign(src[src_scan++]);

      /*  1 to 127 is uncompressed string of 1 to 127 bytes        */
      if (cur_byte > 0 && cur_byte < 128) {
        length = cur_byte;

        if ((dst_size + length) > max_dstsz) {
          return (-1);
        }

        System.arraycopy(src, src_scan, dst, dst_size, length);

        src_scan += length;
        dst_size += length;
      }
      else { /*  Run of 2 or more bytes       */
        switch (cur_byte) {
          case 0x00: /*  Run of non-zero bytes      */
            length = ByteConverter.unsign(src[src_scan++]);
            if (length == 0) { /*  Stored as double-byte      */
              length = ByteConverter.unsign(src[src_scan++]);
              length += ByteConverter.unsign(src[src_scan++]) << 8;
            }
            cur_byte = ByteConverter.unsign(src[src_scan++]);
            break;
          case 0x80: /*  256-2^16 zeroes          */
            length = ByteConverter.unsign(src[src_scan++]);
            length += ByteConverter.unsign(src[src_scan++]) << 8;
            cur_byte = 0;
            break;
          case 0x81: /*  128 to 255 zeroes        */
            length = ByteConverter.unsign(src[src_scan++]);
            cur_byte = 0;
            break;
          default: /*  2 to 127 zeroes          */
            length = cur_byte & 127;
            cur_byte = 0;
        }

        if ((dst_size + length) > max_dstsz) {
          return (-1);
        }

        for (int i = 0; i < length; i++) {
          dst[dst_size + i] = (byte) cur_byte;
        }

        dst_size += length;
      }
    }
    return (dst_size); /*  Return expanded data size    */
  }

  /**
  **********************************************************************************************
  Expands a block of data previously compressed using the compress_rle() function. The
  compressed block is passed in src; the expanded result in dst.  Dst must be large enough to
  accomodate the largest possible decompressed block.  Returns the size of the expanded data.
  
  !! UNTESTED !!
  **********************************************************************************************
  **/
  public int expand_rle(byte[] src, byte[] dst, int src_size, int max_dstsz) {
    int dst_size = 0; /*  Size of expanded data      */
    int src_scan = 0; /*  Scan through source data     */
    int length; /*  Size of the run or string    */
    int cur_byte; /*  Next byte to process       */

    while (src_scan < src_size) {
      cur_byte = ByteConverter.unsign(src[src_scan++]);

      /*  1 to 127 is uncompressed string of 1 to 127 bytes        */
      if (cur_byte > 0 && cur_byte < 128) {
        length = cur_byte;

        if ((dst_size + length) > max_dstsz) {
          return (-1);
        }

        System.arraycopy(src, src_scan, dst, dst_size, length);
        src_scan += length;
        dst_size += length;
      }
      else { /*  Run of 3 or more bytes       */
        switch (cur_byte) {
          case 0x00: /*  Run of 3-255 zeroes        */
            length = ByteConverter.unsign(src[src_scan++]);
            cur_byte = 0;
            break;
          case 0x82: /*  Run of 3-255 spaces        */
            length = ByteConverter.unsign(src[src_scan++]);
            cur_byte = ' ';
            break;
          case 0x80: /*  Short run 128-255 bytes      */
            length = ByteConverter.unsign(src[src_scan++]);
            cur_byte = ByteConverter.unsign(src[src_scan++]);
            break;
          case 0x81: /*  Long run 256-2^16 bytes      */
            length = ByteConverter.unsign(src[src_scan++]);
            length += ByteConverter.unsign(src[src_scan++]) << 8;
            cur_byte = ByteConverter.unsign(src[src_scan++]);
            break;
          default: /*  Run of 3 to 127 bytes      */
            length = cur_byte & 127;
            cur_byte = ByteConverter.unsign(src[src_scan++]);
        }
        if ((dst_size + length) > max_dstsz) {
          return (-1);
        }
        for (int i = 0; i < length; i++) {
          dst[dst_size + i] = (byte) cur_byte;
        }
        dst_size += length;
      }
    }
    return (dst_size); /*  Return expanded data size    */
  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  @Override
  public String getDescription() {
    return "This exporter decompresses SFL Nulls-compressed files.\n\n" + super.getDescription();
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
      readLength--;

      byte returnByte = buffer[bufferPos];
      bufferPos++;

      return returnByte;
    }
    catch (Throwable t) {
      return 0;
    }
  }

}