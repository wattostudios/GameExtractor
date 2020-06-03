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

public class Exporter_LZWX extends ExporterPlugin {

  static Exporter_LZWX instance = new Exporter_LZWX();

  static FileManipulator readSource;
  static byte[] readBuffer = new byte[0];
  static int readBufferPos = 0;
  static long readLength = 0;
  static int readBufferLevel = 0;

  static int outlen,                 // current output length
      outsize,                // output buffer size
      dictsize,               // offset of the last element in the dictionary
      dictoff,                // offset of the new entry to add to the dictionary
      dictalloc;              // used only for dynamic dictionary allocation

  static int dictlen;                // offset length (like dict_t.len)

  static int bits;                   // init bits (usually max 16)

  /**
  **********************************************************************************************
  DO NOT USE - DOESN'T WORK!!!
  **********************************************************************************************
  **/
  public static Exporter_LZWX getInstance() {
    return instance;
  }

  int UNLZW_BITS = 9;

  int UNLZW_END = 256;

  byte[][] dict_data = new byte[0][0];

  int[] dict_len = new int[0];

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public Exporter_LZWX() {
    setName("LZWX Compression");
  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  @Override
  public boolean available() {
    try {
      if (readLength <= 0) {
        return false;
      }

      if (readBufferPos >= readBufferLevel) {
        unlzwx(readBuffer.length, readSource, (int) readLength);
      }
      return true;

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
      readSource.close();
      readSource = null;
      readBuffer = null;
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
  public String getDescription() {
    return "This exporter decompresses LZWX-compressed files when exporting\n\n" + super.getDescription();
  }

  /**
  **********************************************************************************************
  So we can easily call this from within a Viewer plugin
  **********************************************************************************************
  **/
  public void open(FileManipulator fmIn, int compLengthIn, int decompLengthIn) {
    try {
      readSource = fmIn;

      readBufferPos = 0;
      readBufferLevel = 0;

      readLength = decompLengthIn;

      int readBufferSize = 200000;
      if (readLength < readBufferSize) {
        readBufferSize = (int) readLength;
      }

      readBuffer = new byte[readBufferSize];

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
      readSource = new FileManipulator(source.getSource(), false);
      readSource.seek(source.getOffset());

      readBufferPos = 0;
      readBufferLevel = 0;
      readLength = source.getDecompressedLength();

      int readBufferSize = 200000;
      if (readLength < readBufferSize) {
        readBufferSize = (int) readLength;
      }

      readBuffer = new byte[readBufferSize];

    }
    catch (Throwable t) {
    }
  }

  /**
   **********************************************************************************************
   * // TEST - NOT DONE
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
      int readByte = readBuffer[readBufferPos];
      readBufferPos++;
      readLength--;
      return readByte;
    }
    catch (Throwable t) {
      return 0;
    }
  }

  int unlzwx(int maxsize, FileManipulator fm, int insize) {
    int code,                   // current element
        inlen,                  // current input length
        cl,
        dl,
        totbits;
    int n,                      // bytes written in the output
        i;

    code = -1;                       // useless
    inlen = 0;                        // current input length
    outlen = 0;                        // current output length
    outsize = maxsize;                  // needed only for the global var
    totbits = 0;

    dict_data = new byte[0][0];                     // global var initialization
    dict_len = new int[0];                     // global var initialization
    unlzwx_init();

    insize -= 2;
    while (inlen < insize) {
      int byte1 = fm.readByte();
      int byte2 = fm.readByte();
      int byte3 = fm.readByte();

      cl = (byte2 << 8) | byte1;
      dl = byte3;
      for (i = 0; i < (totbits & 7); i++) {
        cl = (cl >> 1) | ((dl & 1) << 15);
        dl >>= 1;
      }
      code = ((1 << bits) - 1) & cl;

      totbits += bits;
      inlen = totbits >> 3;

      if (code == 257) {
        break;
      }
      if (code == UNLZW_END) {         // means that we need to reset everything
        unlzwx_init();
        continue;                   // and restart the unpacking
      }

      if (code == dictsize) {          // I think this is used for repeated chars
        if (unlzwx_dictionary()      // fill the dictionary
        < 0) {
          break;
        }

        n = unlzwx_expand(code);    // unpack

      }
      else {
        n = unlzwx_expand(code);

        if (unlzwx_dictionary() < 0) {
          break;
        }
      }
      if (n < 0) {
        break;                // break if unlzwx_expand() failed
      }

      dictoff = outlen;               // increment all the remaining values
      dictlen = n;
      outlen += n;
    }

    return (outlen);                     // return the output length
  }

  void unlzwx_cpy(int outpos, byte[] in, int len) {
    for (int i = 0; i < len; i++) {
      readBuffer[outpos + i] = in[i];
    }
  }

  int unlzwx_dictionary() {     // fill the dictionary
    //int tmp;

    if (dictlen++ > 0) {
      if ((dictoff + dictlen) > outsize) {
        dictlen = outsize - dictoff;
      }

      //dict_data[dictsize] = new byte[] { readBuffer[dictoff] };
      //dict_len[dictsize] = dictlen;

      dict_data[dictsize] = new byte[dictlen];
      System.arraycopy(readBuffer, dictoff, dict_data[dictsize], 0, dictlen);
      dict_len[dictsize] = dictlen;

      dictsize++;
      if (((dictsize >> bits) > 0) && (bits != 12)) {
        bits++;
        // dynamic dictionary
        /*
        tmp = sizeof(dict_t) * (1 << bits);
        if(tmp > dictalloc) {
            dict = realloc(dict, tmp);
            if(!dict) return(-1);
            memset((void *)dict + dictalloc, 0, tmp - dictalloc);
            dictalloc = tmp;
        }
        */
      }
    }
    return (0);
  }

  int unlzwx_expand(int code) {
    if (code >= dictsize) {
      return (0);     // invalid so return 0
    }

    if (code >= UNLZW_END) {             // put the data in the dictionary
      if ((outlen + dict_len[code]) > outsize) {
        return (-1);
      }
      unlzwx_cpy(outlen, dict_data[code], dict_len[code]);
      return (dict_len[code]);
    }
    // put the byte
    if ((outlen + 1) > outsize) {
      return (-1);
    }
    readBuffer[outlen] = (byte) code;
    return (1);
  }

  void unlzwx_init() {
    bits = UNLZW_BITS;
    dictsize = UNLZW_END + 2;
    dictoff = 0;
    dictlen = 0;

    int dictinitsize = (1 << (UNLZW_BITS + 3));
    dict_data = new byte[dictinitsize][0];
    dict_len = new int[dictinitsize];
  }

}