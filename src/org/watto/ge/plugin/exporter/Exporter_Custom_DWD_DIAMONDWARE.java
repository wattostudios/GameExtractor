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
import org.watto.io.buffer.ByteBuffer;

public class Exporter_Custom_DWD_DIAMONDWARE extends ExporterPlugin {

  static Exporter_Custom_DWD_DIAMONDWARE instance = new Exporter_Custom_DWD_DIAMONDWARE();

  static FileManipulator readSource;

  static long readLength = 0;

  /**
  **********************************************************************************************
  Adds a WAV audio header to the front of raw audio data
  **********************************************************************************************
  **/
  public static Exporter_Custom_DWD_DIAMONDWARE getInstance() {
    return instance;
  }

  int headerPos = 0;

  int headerLength = 0;

  boolean writingHeader = true;

  boolean signed = true;

  byte[] header = new byte[0];

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public Exporter_Custom_DWD_DIAMONDWARE() {
    setName("Diamondware Digitized WAV Audio Exporter");
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
  @Override
  public String getDescription() {
    return "This exporter converts a Diamondware Digitized audio file into a WAV audio file.\n\n" + super.getDescription();
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

      // 00-22   "DiamondWare Digitized\n\0"
      // 23      1A (EOF to abort printing of file)
      // 24      Major version number
      // 25      Minor version number
      // 26-29   Unique sound ID (checksum XOR timestamp)
      // 30      Reserved
      // 31      Compression type (0=none)
      readSource.skip(32);

      // 32-33   Sampling rate (in Hz)
      int frequency = readSource.readShort();

      // 34      Number of channels (1=mono, 2=stereo)
      short channels = readSource.readByte();

      // 35      Number of bits per sample (8, 16)
      short bitrate = readSource.readByte();

      // 36-37   Absolute value of largest sample in file
      // 38-41   length of data section (in bytes)
      // 42-45   Number of samples (16-bit stereo would be 4 bytes/sample)
      readSource.skip(10);

      // 46-49  *Offset of data section from start of file (in bytes)
      int dataOffset = readSource.readInt();

      // 50-53   Reserved for future expansion (markers)
      // ??-??   Future expansion (heed the 2 offsets, above!)
      int skipSize = dataOffset - 50;
      if (skipSize > 0) {
        readSource.skip(skipSize);
      }

      readLength -= dataOffset;
      int audioLength = (int) readLength;

      short codec = 0x0001;
      signed = false;
      byte[] extraData = null;
      int samples = -1;
      short blockAlign = -1;

      header = pcmwav_header(frequency, channels, bitrate, audioLength, codec, samples, blockAlign, extraData);

      headerPos = 0;
      headerLength = header.length;
      readLength += headerLength;

      writingHeader = true; // so when we call read(), it accesses the header bytes

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
  public byte[] pcmwav_header(int frequency, short channels, short bits, int dataLength, short codec, int samples, short blockAlign, byte[] extraData) {
    int headerSize = 44;
    if (extraData != null) {
      headerSize += extraData.length;
    }

    ByteBuffer byteBuffer = new ByteBuffer(headerSize);
    FileManipulator fm = new FileManipulator(byteBuffer);

    // 4 - Header (RIFF)
    fm.writeString("RIFF");

    // 4 - Length
    int length = 4 + 8 + 16 + 8 + dataLength;
    if (extraData != null) {
      length += extraData.length;
    }
    if (samples != -1) {
      length += 12;
    }
    fm.writeInt(length);

    // 4 - Header 2 (WAVE)
    fm.writeString("WAVE");

    // 4 - Header 3 (fmt )
    fm.writeString("fmt ");

    // 4 - Block Size (16)
    int blockSize = 16;
    if (extraData != null) {
      blockSize += extraData.length;
    }
    fm.writeInt(blockSize);

    // 2 - Format Tag (0x0001)
    fm.writeShort(codec);

    // 2 - Channels
    fm.writeShort(channels);

    // 4 - Samples per Second (Frequency)
    fm.writeInt(frequency);

    // 4 - Average Bytes per Second ()
    fm.writeInt(frequency * (bits / 8 * channels));

    // 2 - Block Alignment (bits/8 * channels)
    if (blockAlign != -1) {
      fm.writeShort(blockAlign);
    }
    else {
      fm.writeShort(bits / 8 * channels);
    }

    // 2 - Bits Per Sample (bits)
    fm.writeShort(bits);

    // X - Extra Data
    if (extraData != null) {
      fm.writeBytes(extraData);
    }

    // Samples (optional)

    if (samples != -1) {
      // 4 - Header (fact)
      fm.writeString("fact");

      // 4 - Data Length 
      fm.writeInt(4);

      // 4 - Number of Samples 
      fm.writeInt(samples);
    }

    // Raw Audio Data

    // 4 - Header 4 (data)
    fm.writeString("data");

    // 4 - Data Length (dataLength)
    fm.writeInt(dataLength);

    // return the pointer to the beginning of the buffer, ready for grabbing the buffer contents
    fm.seek(0);
    // close the manipulator
    fm.close();

    return byteBuffer.getBuffer(byteBuffer.getBufferSize());
  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  @Override
  public int read() {
    try {
      readLength--;

      if (writingHeader) {
        if (headerPos != headerLength) {
          headerPos++;
          return header[headerPos - 1];
        }
        writingHeader = false;
      }

      //return readSource.readByte();
      int byteRead = readSource.readByte();
      if (!signed) {
        byteRead = 127 + byteRead;
      }
      return byteRead;
    }
    catch (Throwable t) {
      return 0;
    }
  }

}