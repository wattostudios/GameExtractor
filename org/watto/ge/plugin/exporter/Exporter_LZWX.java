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
import org.watto.ge.plugin.archive.datatype.LZWXDictionaryEntry;
import org.watto.io.FileManipulator;
import org.watto.io.converter.ByteConverter;

public class Exporter_LZWX extends ExporterPlugin {

  static Exporter_LZWX instance = new Exporter_LZWX();

  static FileManipulator readSource;

  static byte[] decompBuffer = new byte[0];

  static int decompBufferPos = 0;

  static long decompBufferLength = 0;

  static long readLength = 0;

  /**
  **********************************************************************************************
  Based off QuickBMS unlzwx.c with modifications for repeating bytes
  **********************************************************************************************
  **/
  public static Exporter_LZWX getInstance() {
    return instance;
  }

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
      decompBuffer = null;
      decompBufferPos = 0;
      decompBufferLength = 0;
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

      decompBufferPos = 0;
      decompBufferLength = decompLengthIn;
      decompBuffer = new byte[(int) decompBufferLength];

      byte[] compBuffer = readSource.readBytes(compLengthIn);

      readLength = unlzwx(compBuffer, compLengthIn, decompLengthIn);
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
      readSource = new FileManipulator(source.getSource(), false);
      readSource.seek(source.getOffset());

      open(readSource, (int) source.getLength(), (int) source.getDecompressedLength());
    }
    catch (Throwable t) {
    }
  }

  /**
   **********************************************************************************************
   NOT DONE
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
      int readByte = decompBuffer[decompBufferPos];
      decompBufferPos++;
      readLength--;
      return readByte;
    }
    catch (Throwable t) {
      return 0;
    }
  }

  int UNLZW_BITS = 9;

  int UNLZW_END = 256;

  LZWXDictionaryEntry[] dict = new LZWXDictionaryEntry[0];

  int outlen = 0;                 // current output length

  int outsize = 0;                // output buffer size

  int dictsize = 0;               // offset of the last element in the dictionary

  int dictoff = 0;                // offset of the new entry to add to the dictionary

  int dictalloc = 0;              // used only for dynamic dictionary allocation

  int dictlen = 0;                // offset length (like dict_t.len)

  int bits = 0;                   // init bits (usually max 16)

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  int unlzwx(byte[] in, int insize, int maxsize) {
    int code = 0;                   // current element
    int inlen = 0;                  // current input length
    int cl = 0;
    int dl = 0;
    int totbits = 0;
    int n = 0;                      // bytes written in the output
    int i = 0;

    code = -1;                       // useless
    inlen = 0;                        // current input length
    outlen = 0;                        // current output length
    outsize = maxsize;                  // needed only for the global var
    totbits = 0;

    dict = null;                     // global var initialization
    if (unlzwx_init() < 0) {
      return (0);
    }

    insize -= 2;
    while (inlen < insize) {
      cl = (ByteConverter.unsign(in[inlen + 1]) << 8) | ByteConverter.unsign(in[inlen]);
      dl = ByteConverter.unsign(in[inlen + 2]);
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
        if (unlzwx_init() < 0) {
          break;
        }
        continue;                   // and restart the unpacking
      }

      if (code == dictsize) {          // I think this is used for repeated chars
        if (unlzwx_dictionary_repeating() < 0) {      // fill the dictionary
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

    dict = null;
    return (outlen);                     // return the output length
  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  void unlzwx_cpy(int outpos, byte[] in, int len) {
    for (int i = 0; i < len; i++) {
      decompBuffer[outpos + i] = in[i];
    }
  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  int unlzwx_dictionary_repeating() {     // fill the dictionary
    //int tmp;

    if ((dictlen++) != 0) {
      //if (dictlen != 0) {
      //  dictlen++;

      if ((dictoff + dictlen) > outsize) {
        dictlen = outsize - dictoff;
      }

      /*
      byte[] currentDictData = new byte[dictoff];
      for (int i = 0; i < dictoff; i++) {
        currentDictData[i] = decompBuffer[(int) (decompBufferPos + i)];
      }
      */
      byte[] currentDictData = new byte[dictlen];
      for (int i = 0; i < dictlen; i++) {
        if ((decompBufferPos + dictoff + i) >= outlen) {
          currentDictData[i] = decompBuffer[(int) (decompBufferPos + dictoff + i - 1)];
        }
        else {
          currentDictData[i] = decompBuffer[(int) (decompBufferPos + dictoff + i)];
        }
      }

      //dict[dictsize].setData(currentDictData);
      //dict[dictsize].setLength(dictlen);
      LZWXDictionaryEntry dictEntry = new LZWXDictionaryEntry(currentDictData, dictlen);
      dict[dictsize] = dictEntry;
      dictsize++;
      if (((dictsize >> bits) != 0) && (bits != 12)) {
        bits++;
        /*
                                    // dynamic dictionary
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

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  int unlzwx_dictionary() {
    //int tmp;

    if ((dictlen++) != 0) {
      //if (dictlen != 0) {
      //  dictlen++;

      if ((dictoff + dictlen) > outsize) {
        dictlen = outsize - dictoff;
      }

      /*
      byte[] currentDictData = new byte[dictoff];
      for (int i = 0; i < dictoff; i++) {
        currentDictData[i] = decompBuffer[(int) (decompBufferPos + i)];
      }
      */
      byte[] currentDictData = new byte[dictlen];
      for (int i = 0; i < dictlen; i++) {
        currentDictData[i] = decompBuffer[(int) (decompBufferPos + dictoff + i)];
      }

      //dict[dictsize].setData(currentDictData);
      //dict[dictsize].setLength(dictlen);
      LZWXDictionaryEntry dictEntry = new LZWXDictionaryEntry(currentDictData, dictlen);
      dict[dictsize] = dictEntry;
      dictsize++;
      if (((dictsize >> bits) != 0) && (bits != 12)) {
        bits++;
        /*
                                    // dynamic dictionary
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

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  int unlzwx_expand(int code) {
    if (code >= dictsize) {
      return (0);     // invalid so return 0
    }

    if (code >= UNLZW_END) { // get the data from the dictionary
      if ((outlen + dict[code].getLength()) > outsize) {
        return (-1);
      }
      //System.out.println("[LZWX]: Copying data from dict[" + code + "] to " + (decompBufferPos + outlen));
      //System.out.println("  " + new String(dict[code].getData()));
      unlzwx_cpy(decompBufferPos + outlen, dict[code].getData(), dict[code].getLength());
      return (dict[code].getLength());
    }
    // Get the data from the byte[] array
    if ((outlen + 1) > outsize) {
      return (-1);
    }
    //System.out.println("[LZWX]: Writing data from byte[] array: " + ((char) code));

    decompBuffer[outlen] = (byte) code;
    return (1);
  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  int unlzwx_init() {
    bits = UNLZW_BITS;
    dictsize = UNLZW_END + 2;
    dictoff = 0;
    dictlen = 0;

    dictalloc = 1 * (1 << (UNLZW_BITS + 3));
    dict = new LZWXDictionaryEntry[dictalloc];
    /*
    if(!dict) {                         // allocate memory for a dictionary of UNLZW_BITS bits
        dictalloc = sizeof(dict_t) * (1 << (UNLZW_BITS + 3));
        dict = malloc(dictalloc);       // + 3 is used for avoiding too much realloc() calls
        if(!dict) return(-1);
    }                                   // if dict still exists we use it
    memset((void *)dict, 0, dictalloc); // all lengths set to zero to avoid malicious crashes
    */

    return (0);
  }

}