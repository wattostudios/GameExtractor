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
import org.watto.ge.helper.FieldValidator;
import org.watto.ge.plugin.ExporterPlugin;
import org.watto.io.FileManipulator;
import org.watto.io.buffer.ByteBuffer;
import org.watto.io.converter.IntConverter;
import org.watto.io.converter.ShortConverter;

/**
 **********************************************************************************************
 Ref: https://sourceforge.net/p/stratlas/wiki/HPI/
 **********************************************************************************************
 **/
public class Exporter_Custom_HPI_HAPI extends ExporterPlugin {

  static Exporter_Custom_HPI_HAPI instance = new Exporter_Custom_HPI_HAPI();

  static FileManipulator readSource;

  static long readLength = 0;

  byte[] decompData = new byte[0];

  int decompPos = 0;

  int decompLength = 0;

  static int key = 0;

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public static Exporter_Custom_HPI_HAPI getInstance() {
    return instance;
  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public Exporter_Custom_HPI_HAPI() {
    setName("Total Annihilation HAPI Encryption + Compression");
  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  @Override
  public boolean available() {
    //if (readLength > 0) {
    if (decompPos < decompLength) {
      return true;
    }
    return false;
  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  @Override
  public void close() {
    try {

      decompData = new byte[0];
      decompPos = 0;
      decompLength = 0;

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
  public String getDescription() {
    return "This exporter decrypts HAPI encrypted files when exporting\n\n" + super.getDescription();
  }

  /**
  **********************************************************************************************
  Opens and decompresses the whole file
  **********************************************************************************************
  **/
  @Override
  public void open(Resource source) {
    try {
      readSource = new FileManipulator(source.getSource(), false);
      readSource.seek(source.getOffset());

      readLength = source.getDecompressedLength();

      decompLength = (int) readLength;
      decompData = new byte[decompLength];
      decompPos = 0;

      // Now lets read the file header
      int numChunks = (int) (readLength / 65536);
      if (readLength % 65536 != 0) {
        numChunks++;
      }

      int[] chunkLengths = new int[numChunks];
      for (int i = 0; i < numChunks; i++) {
        // 4 - Compressed Length (including this header stuff)
        int chunkLength = IntConverter.convertLittle(new byte[] { readByte(), readByte(), readByte(), readByte() });
        FieldValidator.checkLength(chunkLength, readLength);
        chunkLengths[i] = chunkLength;
      }

      // now read and decompress each chunk
      for (int i = 0; i < numChunks; i++) {
        // 4 - Header (SQSH)
        readByte();
        readByte();
        readByte();
        readByte();

        // 1 - Unknown (2)
        readByte();

        // 1 - Compression Type (1=LZ77, 2=ZLib)
        int compType = readByte();

        // 1 - Encryption (1=Encrypted, 0=Not Encrypted)
        int encType = readByte();

        // 4 - Compressed Chunk Length (data only)
        int chunkLength = IntConverter.convertLittle(new byte[] { readByte(), readByte(), readByte(), readByte() });
        FieldValidator.checkLength(chunkLength, readLength);

        // 4 - Decompressed Chunk Length
        int chunkDecompLength = IntConverter.convertLittle(new byte[] { readByte(), readByte(), readByte(), readByte() });
        FieldValidator.checkLength(chunkDecompLength, readLength);

        // 4 - Checksum
        readByte();
        readByte();
        readByte();
        readByte();

        // X - Compressed Chunk Data

        // get the compressed data (including decryption if needed)
        byte[] compData = new byte[chunkLength];
        if (encType == 1) {
          // double-encrypted
          for (int b = 0; b < chunkLength; b++) {
            compData[b] = (byte) ((readByte() - b) ^ b);
          }
        }
        else {
          // single encrypted
          for (int b = 0; b < chunkLength; b++) {
            compData[b] = readByte();
          }
        }

        // decompress it
        if (compType == 1) {
          // LZ77
          decompressChunk(compData);
        }
        else if (compType == 2) {
          // ZLib
          Exporter_ZLib exporter = Exporter_ZLib.getInstance();
          exporter.open(new FileManipulator(new ByteBuffer(compData)), chunkLength, chunkDecompLength);
          for (int b = 0; b < chunkDecompLength; b++) {
            exporter.available();
            decompData[decompPos] = (byte) exporter.read();
            decompPos++;
          }
          exporter.close();
        }

      }

      // now reset the pointer back to the beginning, ready for the reads
      decompPos = 0;

    }
    catch (Throwable t) {
    }
  }

  /**
   **********************************************************************************************
   Ref: https://sourceforge.net/p/stratlas/wiki/HPI/
   **********************************************************************************************
   **/
  public void decompressChunk(byte[] in) {
    int x;
    int outbufptr = 1;
    int mask = 1;
    int tag;
    int inptr = 0;
    int count;
    boolean done = false;
    byte[] window = new byte[4096];
    int inbufptr;

    tag = in[inptr++];

    while (!done) {
      if ((mask & tag) == 0) {
        decompData[decompPos++] = in[inptr]; //out[outptr++] = in[inptr];
        window[outbufptr] = in[inptr];
        outbufptr = (outbufptr + 1) & 0xFFF;
        inptr++;
      }
      else {

        count = ShortConverter.unsign(ShortConverter.convertLittle(new byte[] { in[inptr], in[inptr + 1] }));//count = ((unsigned short ) (in+inptr));
        inptr += 2;
        inbufptr = count >> 4;
        if (inbufptr == 0)
          return;
        else {
          count = (count & 0x0f) + 2;
          if (count >= 0) {
            for (x = 0; x < count; x++) {
              decompData[decompPos++] = window[inbufptr];
              window[outbufptr] = window[inbufptr];
              inbufptr = (inbufptr + 1) & 0xFFF;
              outbufptr = (outbufptr + 1) & 0xFFF;
            }
          }
        }
      }
      mask *= 2;
      if ((mask & 0x0100) == 0x0100) {
        mask = 1;
        tag = in[inptr++];
      }
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
      long decompLength = source.getDecompressedLength();

      ExporterPlugin exporter = source.getExporter();
      exporter.open(source);

      for (int i = 0; i < decompLength; i++) {
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
      int currentByte = decompData[decompPos];
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
  public byte readByte() throws Exception {
    if (key == 0) {
      return readSource.readByte();
    }
    int tKey = (int) (readSource.getOffset() ^ key);
    return (byte) (tKey ^ ~(readSource.readByte()));
  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public void setKey(int key2) {
    key = key2;
  }

}