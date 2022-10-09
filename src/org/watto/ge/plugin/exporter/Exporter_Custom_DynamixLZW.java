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

import org.watto.datatype.DynamixLZWTable;
import org.watto.datatype.Resource;
import org.watto.ge.plugin.ExporterPlugin;
import org.watto.io.FileManipulator;
import org.watto.io.converter.ByteConverter;

/**
**********************************************************************************************
Dynamix LZW Decompression
==
==
== UNTESTED / UNVERIFIED ==
==
==
Ref: https://github.com/SkaZzMaSTaH/Dynamix-GDS-Extractor/blob/0837516014d806913c4be0ceb5ffed94b668ab1a/Dynamix%20GDS%20Extractor/Lib/LZW.cs
**********************************************************************************************
**/
public class Exporter_Custom_DynamixLZW extends ExporterPlugin {

  static Exporter_Custom_DynamixLZW instance = new Exporter_Custom_DynamixLZW();

  byte[] buffer = null;

  int bufferLength = 0;

  int bufferPos = 0;

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public static Exporter_Custom_DynamixLZW getInstance() {
    return instance;
  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public Exporter_Custom_DynamixLZW() {
    setName("Dynamix LZW Decompression");
  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  @Override
  public boolean available() {
    return bufferPos < bufferLength;
  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  @Override
  public void close() {
    buffer = null;
  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  @Override
  public void open(Resource source) {
    bufferPos = 0;
    bufferLength = 0;

    try {
      //int compLength = (int) source.getLength();
      int decompLength = (int) source.getDecompressedLength();

      FileManipulator fm = new FileManipulator(source.getSource(), false);
      fm.seek(source.getOffset());

      //byte[] compBuffer = fm.readBytes(compLength);
      buffer = new byte[decompLength];

      Decompress(decompLength, fm);

      fm.close();

      bufferLength = decompLength;

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
    try {

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
      return buffer[bufferPos++];
    }
    catch (Throwable t) {
      bufferPos = bufferLength; // early exit
      return 0;
    }
  }

  DynamixLZWTable[] _codeTable = new DynamixLZWTable[0x4000];

  byte[] _codeCur = new byte[256];

  int _bitsData, _bitsSize; // uint

  int _codeSize, _codeLen, _cacheBits; // uint

  int _tableSize, _tableMax; // uint

  boolean _tableFull;

  int GetCode(int totalBits, FileManipulator input) // uint
  {
    int result, numBits;// uint
    byte[] bitMasks = new byte[] { 0x00, 0x01, 0x03, 0x07, 0x0f, 0x1f, 0x3f, 0x7f, (byte) 0xff };

    numBits = totalBits;
    result = 0;

    while (numBits > 0) {
      int useBits; // uint

      if (input.getRemainingLength() <= 0) {
        return (int) 0xffffffff;
      }

      if (_bitsSize == 0) {
        _bitsSize = 8;
        _bitsData = ByteConverter.unsign(input.readByte());
      }

      useBits = numBits;
      if (useBits > 8) {
        useBits = 8;
      }
      if (useBits > _bitsSize) {
        useBits = _bitsSize;
      }

      result |= (_bitsData & bitMasks[useBits]) << (int) (totalBits - numBits);

      numBits -= useBits;
      _bitsSize -= useBits;
      _bitsData >>= (int) useBits;
    }

    return result;
  }

  public void Decompress(int sz, FileManipulator input) {
    _bitsData = 0;
    _bitsSize = 0;

    Reset();

    int idx; // uint
    idx = 0;
    _cacheBits = 0;

    do {
      int code; // uint

      code = GetCode(_codeSize, input);
      if (code == 0xffffffff) {
        break;
      }

      _cacheBits += _codeSize;
      if (_cacheBits >= _codeSize * 8) {
        _cacheBits -= _codeSize * 8;
      }

      if (code == 0x100) {
        if (_cacheBits > 0) {
          GetCode(_codeSize * 8 - _cacheBits, input);
          Reset();
        }
      }
      else {
        if (code >= _tableSize && !_tableFull) {
          _codeCur[_codeLen++] = _codeCur[0];

          for (int i = 0; i < _codeLen; i++) // uint
          {
            buffer[idx++] = _codeCur[i];
          }
        }
        else {
          for (int i = 0; i < _codeTable[code].len; i++) // uint
          {
            buffer[idx++] = (byte) _codeTable[code].str[i];
          }

          _codeCur[_codeLen++] = (byte) _codeTable[code].str[0];
        }

        if (_codeLen >= 2) {
          if (!_tableFull) {
            int i; // uint

            if (_tableSize == _tableMax && _codeSize == 12) {
              _tableFull = true;
              i = _tableSize;
            }
            else {
              i = _tableSize++;
              _cacheBits = 0;
            }

            if (_tableSize == _tableMax && _codeSize < 12) {
              _codeSize++;
              _tableMax <<= 1;
            }

            for (int j = 0; j < _codeLen; j++) // uint
            {
              _codeTable[i].str[j] = _codeCur[j];
              _codeTable[i].len++;
            }
          }

          for (int i = 0; i < _codeTable[code].len; i++) // uint
          {
            _codeCur[i] = (byte) _codeTable[code].str[i];
          }

          _codeLen = _codeTable[code].len;
        }
      }
    }
    while (idx < sz);
  }

  public void Reset() {
    for (int i = 0; i < _codeTable.length; i++) // uint
    {
      _codeTable[i] = new DynamixLZWTable();
    }

    for (int code = 0; code < 256; code++) // uint
    {
      _codeTable[code].len = 1;
      _codeTable[code].str[0] = (byte) code;
    }

    _tableSize = 0x101;
    _tableMax = 0x200;
    _tableFull = false;

    _codeSize = 9;
    _codeLen = 0;

    _cacheBits = 0;
  }

}