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
import org.watto.io.converter.IntConverter;

public class Exporter_Custom_SCUMMVM16 extends ExporterPlugin {

  static Exporter_Custom_SCUMMVM16 instance = new Exporter_Custom_SCUMMVM16();

  byte[] decompBuffer = null;

  int decompPos = 0;

  int decompLength = 0;

  byte[] compBuffer = null;

  int compPos = 0;

  int compLength = 0;

  /**
  **********************************************************************************************
  Ref: QuickBMS
  STACpack/LZS decompressor for SCI32
  **********************************************************************************************
  **/
  public static Exporter_Custom_SCUMMVM16 getInstance() {
    return instance;
  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public Exporter_Custom_SCUMMVM16() {
    setName("Stac LZS (SCUMMVM16) Compression");
  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  @Override
  public boolean available() {
    if (decompPos < decompLength) {
      return true;
    }
    else {
      return false;
    }
  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  @Override
  public void close() {
    decompBuffer = null;
    decompPos = 0;
    decompLength = 0;
  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  @Override
  public String getDescription() {
    return "This exporter decompresses Stac LZS (SCUMMVM16) files when exporting\n\n" + super.getDescription();
  }

  FileManipulator fm;

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  @Override
  public void open(Resource source) {
    try {
      FileManipulator fm = new FileManipulator(source.getSource(), false);
      fm.seek(source.getOffset());

      decompPos = 0;
      decompLength = (int) source.getDecompressedLength();
      decompBuffer = new byte[(int) decompLength];

      compPos = 0;
      compLength = (int) source.getLength() + 8; // slightly larger than the actual comp length
      compBuffer = fm.readBytes(compLength);

      decompressFile();

      compBuffer = null; // discard the comp buffer now that we've finished the decompression

      // return the decompBuffer to the beginning, ready to read from
      decompPos = 0;

      fm.close();
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
      byte currentByte = decompBuffer[decompPos];
      decompPos++;
      return currentByte;
    }
    catch (Throwable t) {
      return 0;
    }
  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  void Decompressor__fetchBitsMSB() {
    while (_nBits <= 24) {
      //_dwBits |= ((uint32)*_src++) << (24 - _nBits);
      _dwBits |= (ByteConverter.unsign(compBuffer[compPos++])) << (24 - _nBits);

      _nBits += 8;
      _dwRead++;
    }
  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  int Decompressor__getBitsMSB(int n) {
    // fetching more data to buffer if needed
    if (_nBits < n)
      Decompressor__fetchBitsMSB();
    int ret = _dwBits >> (32 - n);

    if (ret < 0) {
      ret = (int) (IntConverter.unsign(_dwBits) >> (32 - n));
    }

    _dwBits <<= n;
    _nBits -= n;
    return ret;
  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  int Decompressor__getCompLen() {
    int clen;
    int nibble;
    // The most probable cases are hardcoded
    switch (Decompressor__getBitsMSB(2)) {
      case 0:
        return 2;
      case 1:
        return 3;
      case 2:
        return 4;
      default:
        switch (Decompressor__getBitsMSB(2)) {
          case 0:
            return 5;
          case 1:
            return 6;
          case 2:
            return 7;
          default:
            // Ok, no shortcuts anymore - just get nibbles and add up
            clen = 8;
            do {
              nibble = Decompressor__getBitsMSB(4);
              clen += nibble;
            }
            while (nibble == 0xf);
            return clen;
        }
    }
  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  void Decompressor__copyComp(int offs, int clen) {
    int hpos = _dwWrote - offs;

    while (clen-- > 0) {
      Decompressor__putByte(decompBuffer[hpos++]);
    }
  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  void Decompressor__putByte(byte b) {
    decompBuffer[_dwWrote++] = b;
  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public int decompressFile() {
    try {

      _szPacked = compLength;
      _szUnpacked = decompLength;
      _nBits = 0;
      _dwRead = _dwWrote = 0;
      _dwBits = 0;

      return DecompressorLZS__unpackLZS();

    }
    catch (Throwable t) {
      t.printStackTrace();
      return -1;
    }
  }

  int _dwBits;   ///< bits buffer

  byte _nBits;    ///< number of unread bits in _dwBits

  int _szPacked; ///< size of the compressed data

  int _szUnpacked; ///< size of the decompressed data

  int _dwRead;   ///< number of bytes read from _src

  int _dwWrote;  ///< number of bytes written to _dest

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  boolean Decompressor__isFinished() {
    return (_dwWrote == _szUnpacked) && (_dwRead >= _szPacked);
  }

  int SCI_ERROR_DECOMPRESSION_ERROR = 7;

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  int Decompressor__getByteMSB() {
    return Decompressor__getBitsMSB(8);
  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  int DecompressorLZS__unpackLZS() {

    int offs = 0;
    int clen;

    while (!Decompressor__isFinished()) {
      if (Decompressor__getBitsMSB(1) != 0) { // Compressed bytes follow
        if (Decompressor__getBitsMSB(1) != 0) { // Seven bit offset follows
          offs = Decompressor__getBitsMSB(7);
          if (offs == 0) // This is the end marker - a 7 bit offset of zero
            break;
          if ((clen = Decompressor__getCompLen()) == 0) {
            System.out.println("lzsDecomp: length mismatch");
            return SCI_ERROR_DECOMPRESSION_ERROR;
          }
          Decompressor__copyComp(offs, clen);
        }
        else { // Eleven bit offset follows
          offs = Decompressor__getBitsMSB(11);
          if ((clen = Decompressor__getCompLen()) == 0) {
            System.out.println("lzsDecomp: length mismatch");
            return SCI_ERROR_DECOMPRESSION_ERROR;
          }
          Decompressor__copyComp(offs, clen);
        }
      }
      else // Literal byte follows
        Decompressor__putByte((byte) Decompressor__getByteMSB());
    } // end of while ()
    //return _dwWrote == _szUnpacked ? 0 : SCI_ERROR_DECOMPRESSION_ERROR;
    return _dwWrote;
  }

}