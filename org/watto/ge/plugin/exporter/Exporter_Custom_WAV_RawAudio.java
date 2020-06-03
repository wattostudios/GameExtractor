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
import org.watto.ge.plugin.resource.Resource_WAV_RawAudio;
import org.watto.io.FileManipulator;
import org.watto.io.buffer.ByteBuffer;

public class Exporter_Custom_WAV_RawAudio extends ExporterPlugin {

  static Exporter_Custom_WAV_RawAudio instance = new Exporter_Custom_WAV_RawAudio();

  static FileManipulator readSource;

  static long readLength = 0;

  /**
  **********************************************************************************************
  Adds a WAV audio header to the front of raw audio data
  **********************************************************************************************
  **/
  public static Exporter_Custom_WAV_RawAudio getInstance() {
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
  public Exporter_Custom_WAV_RawAudio() {
    setName("Raw WAV Audio Exporter");
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
    return "This exporter adds the appropriate audio header bytes when extracting raw audio files.\n\n" + super.getDescription();
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

      int frequency = 22050;
      short bitrate = 16;
      short channels = 1;
      int audioLength = (int) readLength;
      signed = true;

      if (source instanceof Resource_WAV_RawAudio) {
        Resource_WAV_RawAudio audioSource = (Resource_WAV_RawAudio) source;
        frequency = audioSource.getFrequency();
        bitrate = audioSource.getBitrate();
        channels = audioSource.getChannels();
        audioLength = audioSource.getAudioLength();
        signed = audioSource.isSigned();
      }

      header = pcmwav_header(frequency, channels, bitrate, audioLength);

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
  public byte[] pcmwav_header(int frequency, short channels, short bits, int dataLength) {
    ByteBuffer byteBuffer = new ByteBuffer(44);
    FileManipulator fm = new FileManipulator(byteBuffer);

    // 4 - Header (RIFF)
    fm.writeString("RIFF");

    // 4 - Length
    int length = 4 + 8 + 16 + 8 + dataLength;
    fm.writeInt(length);

    // 4 - Header 2 (WAVE)
    fm.writeString("WAVE");

    // 4 - Header 3 (fmt )
    fm.writeString("fmt ");

    // 4 - Block Size (16)
    fm.writeInt(16);

    // 2 - Format Tag (0x0001)
    fm.writeShort(0x0001);

    // 2 - Channels
    fm.writeShort(channels);

    // 4 - Samples per Second (Frequency)
    fm.writeInt(frequency);

    // 4 - Average Bytes per Second ()
    fm.writeInt(frequency * (bits / 8 * channels));

    // 2 - Block Alignment (bits/8 * channels)
    fm.writeShort(bits / 8 * channels);

    // 2 - Bits Per Sample (bits)
    fm.writeShort(bits);

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