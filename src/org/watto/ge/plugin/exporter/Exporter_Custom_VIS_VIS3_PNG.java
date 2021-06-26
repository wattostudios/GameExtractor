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

public class Exporter_Custom_VIS_VIS3_PNG extends ExporterPlugin {

  static Exporter_Custom_VIS_VIS3_PNG instance = new Exporter_Custom_VIS_VIS3_PNG();

  static FileManipulator readSource;
  static long readLength = 0;

  /**
  **********************************************************************************************
  NOT WRITTEN OR IN USE
  **********************************************************************************************
  **/
  public static Exporter_Custom_VIS_VIS3_PNG getInstance() {
    return instance;
  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public Exporter_Custom_VIS_VIS3_PNG() {
    setName("Decrypts PNG images from VIS_VIS3 archives");
  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  @Override
  public boolean available() {
    return readLength > 0;
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
  @SuppressWarnings("unused")
  @Override
  public void open(Resource source) {
    try {
      readLength = source.getLength();

      // try to get the whole file in a single go, if it isn't too large (set to 200KB otherwise)
      int bufferSize = (int) readLength;
      if (bufferSize > 204800) {
        bufferSize = 204800;
      }

      readSource = new FileManipulator(source.getSource(), false, bufferSize);
      readSource.seek(source.getOffset());

      // Grab the header (IHDR) from the image, and force-guess the PNG encryption
      readSource.skip(12);
      byte[] headerBytes = readSource.readBytes(17);
      int checksum = readSource.readInt();

      /*
      // The width and height values of the PNG files stored in the archives of
      // Visionaire engine games have been encrypted. The encryption method is
      // basically the same as the one used to obfuscate the file table structure,
      // but the encryption key varies from file to file based on the file name.
      // If the correct key could not be derived from the file name, using various
      // assumptions (and the fact that each chunk of a PNG file is protected with
      // a CRC) we can brute-force it in reasonable time:
      // - The probability of an image having a dimension larger than 65535 pixels
      //   is low, so we assume that half of the bytes we want to decrypt are zero
      //   anyway. Thus, there are only 4 encrypted bytes left.
      // - The decryption takes place with a string of hex characters, i.e. there
      //   are only 16 possible partial keys per encrypted byte. Together with the
      //   first assumption, we get a key space of 65536 possibilites. Testing each
      //   of these possibilities with a CRC check afterwards takes only fractions
      //   of a second on contemporary machines.
      Result := False;
      
      if Length(Buffer) < SizeOf(TPNG_IHDR) + SizeOf(TargetCRC) then
      begin
      WriteLn('    warning: data too short for a PNG file');
      Exit
      end;
      
      // search the IHDR chunk
      Pattern := 'IHDR';
      Idx := SearchMem(SearchMethod_KnuthMorrisPratt, @Pattern[0], @Buffer[0],
      Length(Pattern), Length(Buffer));
      
      if (Idx < 0) or (Idx + SizeOf(TPNG_IHDR) + SizeOf(TargetCRC) >
      Length(Buffer)) then
      begin
      WriteLn('    warning: no valid IHDR found');
      Exit
      end;
      
      // get the targetted CRC value of the IHDR chunk
      Move(Buffer[Idx + SizeOf(TPNG_IHDR)], TargetCRC, SizeOf(TargetCRC));
      SwitchEndian(TargetCRC, True);
      
      SetLength(TestBuf, SizeOf(TPNG_IHDR));
      Move(Buffer[Idx], TestBuf[0], Length(TestBuf));
      
      // we assume that no image has a dimension larger than 65535
      TestBuf[4] := $00;
      TestBuf[5] := $00;
      TestBuf[8] := $00;
      TestBuf[9] := $00;
      
      // precompute the CRC value for as many fixed bytes as possible
      BaseCRC := $ffffffff;
      CalcCRC32(@TestBuf[0], 6, BaseCRC);
      
      // test all assumed key combinations
      KeyFound := False;
      for CurKey1 := 0 to 255 do
      begin
      // generate hex characters from the first part of the candidate key
      Hex1 := IntToHex(CurKey1, 2);
      
      // convert to lower case if necessary
      if Ord(Hex1[1]) >= $41 then
      Hex1[1] := Chr(Ord(Hex1[1]) + $20);
      if Ord(Hex1[2]) >= $41 then
      Hex1[2] := Chr(Ord(Hex1[2]) + $20);
      
      // try the first part of the decryption
      TestBuf[6] := TestBuf[6] xor Ord(Hex1[1]);
      TestBuf[7] := TestBuf[7] xor Ord(Hex1[2]);
      
      // compute the intermediate CRC
      InCRC := BaseCRC;
      CalcCRC32(@TestBuf[6], 4, InCRC);
      
      // search for second part of the key
      for CurKey2 := 0 to 255 do
      begin
      // generate hex characters from the second part of the candidate key
      Hex2 := IntToHex(CurKey2, 2);
      
      if Ord(Hex2[1]) >= $41 then
        Hex2[1] := Chr(Ord(Hex2[1]) + $20);
      if Ord(Hex2[2]) >= $41 then
        Hex2[2] := Chr(Ord(Hex2[2]) + $20);
      
      // try the second part of the decryption ...
      TestBuf[10] := TestBuf[10] xor Ord(Hex2[1]);
      TestBuf[11] := TestBuf[11] xor Ord(Hex2[2]);
      
      // ... and validate the chunk checksum
      CRCVal := InCRC;
      CalcCRC32(@TestBuf[10], Length(TestBuf) - 10, CRCVal);
      CRCVal := not CRCVal;
      
      // check whether the correct key has been found
      if CRCVal = TargetCRC then
      begin
        KeyFound := True;
        Break
      end;
      
      // get the original data back for the next test
      Move(Buffer[Idx + 10], TestBuf[10], 2)
      end;
      
      if KeyFound then
      Break;
      
      // get the original data back for the next test
      Move(Buffer[Idx + 6], TestBuf[6], 2)
      end;
       */

      // go back to the start of the image
      readSource.seek(source.getOffset());

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

      //destination.forceWrite();

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

      return readSource.readByte();
    }
    catch (Throwable t) {
      return 0;
    }
  }

}