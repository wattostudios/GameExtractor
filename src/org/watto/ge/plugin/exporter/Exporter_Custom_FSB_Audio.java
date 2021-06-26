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

import org.watto.datatype.Archive;
import org.watto.datatype.Resource;
import org.watto.ge.plugin.ExporterPlugin;
import org.watto.ge.plugin.resource.Resource_FSB_Audio;
import org.watto.io.FileManipulator;
import org.watto.io.buffer.ByteBuffer;

public class Exporter_Custom_FSB_Audio extends ExporterPlugin {

  static Exporter_Custom_FSB_Audio instance = new Exporter_Custom_FSB_Audio();

  static FileManipulator readSource;

  static long readLength = 0;

  /**
  **********************************************************************************************
  When exporting Audio from an FSB FMOD Soundbank, this exporter adds the appropriate format header
  for the type of audio contained in the file (eg adds the RIFF header if the codec is a WAV codec).
  Based off source code from http://aluigi.altervista.org/papers.htm#fsbext
  **********************************************************************************************
  **/
  public static Exporter_Custom_FSB_Audio getInstance() {
    return instance;
  }

  int headerPos = 0;

  int headerLength = 0;

  boolean writingHeader = true;

  byte[] header = new byte[0];

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public Exporter_Custom_FSB_Audio() {
    setName("FSB FMOD Audio Exporter");
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
  public byte[] buildGenHHeader(int freq, short chans, int rawlen) {
    int genhsz = 0x80; // in case of future additions to the format

    ByteBuffer byteBuffer = new ByteBuffer(genhsz);
    FileManipulator fm = new FileManipulator(byteBuffer);

    // 4 - Magic Number (0x47454e48)
    fm.writeInt(0x47454e48);

    // 4 - Channel Count
    fm.writeInt(chans);

    // 4 - Interleave
    fm.writeInt(2);

    // 4 - Sample Rate
    fm.writeInt(freq);

    // 4 - Loop Start
    fm.writeInt(0xffffffff);

    // 4 - Loop End
    fm.writeInt(((rawlen * 14) / 8) / chans);

    // 4 - Codec
    fm.writeInt(12);

    // 4 - Start Offset
    fm.writeInt(genhsz + (chans * 32));

    // 4 - Header Size
    fm.writeInt(genhsz + (chans * 32));

    // 4 - coef[0]
    fm.writeInt(genhsz);

    // 4 - coef[1]
    fm.writeInt(genhsz + 32);

    // 4 - DSP Interleave Type
    fm.writeInt(1);

    // 4 - Coef Type
    fm.writeInt(0);

    // 4 - coef_splitted[0]
    fm.writeInt(genhsz);

    // 4 - coef_splitted[1]
    fm.writeInt(genhsz + 32);

    // Fill the header to the right size
    for (int i = (int) fm.getOffset(); i < genhsz; i++) {
      fm.writeByte(0);
    }

    // Add the channel padding info
    for (int i = 0; i < chans; i++) {
      for (int j = 0; j < 16; j++) {
        fm.writeShort(0);
      }

    }

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
  public String getDescription() {
    return "This exporter adds the appropriate audio header bytes when extracting from an FSB FMOD Soundbank archive\n\n" + super.getDescription();
  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public byte[] buildITSHeader(String fname, short chans, short bits, int rawlen) {

    /* note that doesn't seem possible to know if the sample has been encoded with 2.14 or 2.15 */
    int flags = 1 | 8;  // 8 for compression
    if (bits == 16) {
      flags |= 2;
    }
    if (chans == 2) {
      flags |= 4;
    }

    ByteBuffer byteBuffer = new ByteBuffer(80);
    FileManipulator fm = new FileManipulator(byteBuffer);

    // 4 - Header (IMPS)
    fm.writeString("IMPS");

    // 12 - Filename
    fm.writeNullString(fname, 12);

    // 1 - null
    fm.writeByte(0);

    // 1 - gvl
    fm.writeByte(128);

    // 1 - flags
    fm.writeByte(flags);

    // 1 - volume
    fm.writeByte(64);

    // 26 - filename
    fm.writeNullString(fname, 26);

    // 1 - cvt
    fm.writeByte(0xff);

    // 1 - dfp
    fm.writeByte(0x7f);

    // 4 - Length
    fm.writeInt(rawlen);

    // 4 - Loop Beginning
    fm.writeInt(0);

    // 4 - Loop End
    fm.writeInt(0);

    // 4 - C5 Speed
    fm.writeInt(8363);

    // 4 - Sus Loop Beginning
    fm.writeInt(0);

    // 4 - Sus Loop End
    fm.writeInt(0);

    // 4 - Sample Pointer
    fm.writeInt(0);

    // 1 - vis
    fm.writeByte(0);

    // 1 - vid
    fm.writeByte(0);

    // 1 - vir
    fm.writeByte(0);

    // 1 - vit
    fm.writeByte(0);

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
  public void open(Resource source) {
    try {
      readSource = new FileManipulator(source.getSource(), false);
      readSource.seek(source.getOffset());

      readLength = source.getLength();

      short channels = 1;
      int frequency = 0;
      short bits = 16;
      int samplesLength = (int) readLength;

      if (source instanceof Resource_FSB_Audio) {
        // generate the appropriate header for the codec
        Resource_FSB_Audio resource = (Resource_FSB_Audio) source;
        channels = resource.getChannels();
        frequency = resource.getFrequency();
        bits = resource.getBits();
        samplesLength = resource.getSamplesLength();

        int codec = resource.getCodec();

        if (codec == Resource_FSB_Audio.CODEC_GCADPCM) { // untested
          header = buildGenHHeader(frequency, channels, (int) readLength);
        }
        else if (codec == Resource_FSB_Audio.CODEC_IMAADPCM) { // TESTED AND WORKING!
          header = buildXBoxIMAHeader(frequency, channels, (int) readLength, samplesLength);
        }
        else if (codec == Resource_FSB_Audio.CODEC_VAG) { // untested
          header = buildSS2Header(frequency, channels, (int) readLength);
        }
        else if (codec == Resource_FSB_Audio.CODEC_HEVAG) { // untested
          header = buildSS2Header(frequency, channels, (int) readLength);
        }
        else if (codec == Resource_FSB_Audio.CODEC_XMA) { // untested
          header = buildXma2Header(frequency, channels, bits, (int) readLength, Archive.getNumFiles());
        }
        else if (codec == Resource_FSB_Audio.CODEC_MPEG) { // TESTED AND WORKING!
          header = new byte[0]; // mp3 files have no header
        }
        else if (codec == Resource_FSB_Audio.CODEC_CELT) { // untested
          header = new byte[0]; // no header?
        }
        else if (codec == Resource_FSB_Audio.CODEC_AT9) { // untested
          header = new byte[0];// don't know how to write this header
        }
        else if (codec == Resource_FSB_Audio.CODEC_XWMA) { // untested
          header = new byte[0];// don't know how to write this header
        }
        else if (codec == Resource_FSB_Audio.CODEC_VORBIS) { // untested
          // If you get here, you should actually have been using Exporter_Custom_FSB5_OGG instead of this Exporter!
          header = new byte[0];
        }
        else if (codec == Resource_FSB_Audio.CODEC_IT214) { // untested
          header = buildITSHeader(resource.getName(), channels, bits, (int) readLength);
        }
        else if (codec == Resource_FSB_Audio.CODEC_IT215) { // untested
          header = buildITSHeader(resource.getName(), channels, bits, (int) readLength);
        }
        else { // TESTED (PCM16) AND WORKING!
          header = buildPCMWavHeader(frequency, channels, bits, (int) readLength);
        }
      }

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
  public byte[] buildPCMWavHeader(int frequency, short channels, short bits, int dataLength) {
    ByteBuffer byteBuffer = new ByteBuffer(44);
    FileManipulator fm = new FileManipulator(byteBuffer);

    // 4 - Header (RIFF)
    fm.writeString("RIFF");

    // 4 - Length
    int length = 4 + 8 + 16 + 8 + dataLength;
    fm.writeInt(length);

    // 4 - Header 2 (WAVE)
    // 4 - Header 3 (fmt )
    fm.writeString("WAVEfmt ");

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

      return readSource.readByte();
    }
    catch (Throwable t) {
      return 0;
    }
  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public byte[] buildSS2Header(int freq, short chans, int rawlen) {
    ByteBuffer byteBuffer = new ByteBuffer(40);
    FileManipulator fm = new FileManipulator(byteBuffer);

    // 4 - Magic Number (0x53536864)
    fm.writeInt(0x53536864);

    // 4 - Unknown
    fm.writeInt(0x18);

    // 4 - Unknown
    fm.writeInt(0x10);

    // 4 - Frequency
    fm.writeInt(freq);

    // 4 - Channels
    fm.writeInt(chans);

    // 4 - Interleave
    fm.writeInt(rawlen / chans);  // seems to be the correct interleave value

    // 4 - Unknown
    fm.writeInt(0);

    // 4 - Unknown
    fm.writeInt(0xffffffff);

    // 4 - Unknown
    fm.writeInt(0x53536264);

    // 4 - Data Length
    fm.writeInt(rawlen);

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
  public byte[] buildVAGHeader(String fname, int freq, int rawlen) {
    ByteBuffer byteBuffer = new ByteBuffer(16);
    FileManipulator fm = new FileManipulator(byteBuffer);

    // 4 - Header (VAGp)
    fm.writeString("VAGp");

    // 4 - Unknown
    fm.writeInt(32);

    // 4 - Unknown
    fm.writeInt(0);

    // 4 - Data Length
    fm.writeInt(rawlen);

    // 4 - Frequency
    fm.writeInt(freq);

    // 12 - Unknown
    for (int i = 0; i < 12; i++) {
      fm.writeByte(0);
    }

    // 16 - Filename
    fm.writeNullString(fname, 16);

    // 16 - Unknown
    for (int i = 0; i < 16; i++) {
      fm.writeByte(0);
    }

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
  public byte[] buildXBoxIMAHeader(int frequency, short channels, int dataLength, int samplesLength) {
    ByteBuffer byteBuffer = new ByteBuffer(60);
    FileManipulator fm = new FileManipulator(byteBuffer);

    // 4 - Header (RIFF)
    fm.writeString("RIFF");

    // 4 - Length
    int length = 4 + 8 + 16 + 4 + 8 + 12 + dataLength; // Note: larger than normal WAV header
    fm.writeInt(length);

    // 4 - Header 2 (WAVE)
    fm.writeString("WAVE");

    // 4 - Header 3 (fmt )
    fm.writeString("fmt ");

    // 4 - Block Size (20)
    fm.writeInt(20);

    // 2 - Format Tag (0x0011)
    fm.writeShort(0x0011);

    // 2 - Channels
    fm.writeShort(channels);

    // 4 - Samples per Second (Frequency)
    fm.writeInt(frequency);

    // 4 - Average Bytes per Second (689 * (36 * channels) + 4)
    //fm.writeInt(689 * (36 * channels) + 4);
    fm.writeInt(frequency);

    // 2 - Block Alignment (36 * channels)
    fm.writeShort(36 * channels);

    // 2 - Bits Per Sample (bits) (4)
    fm.writeShort(4);

    // 2 - Unknown (2)
    fm.writeShort(2);

    // 2 - Unknown (14733)
    fm.writeShort(14733);

    // 4 - Fact Header (fact)
    fm.writeString("fact");

    // 4 - Fact Length (4)
    fm.writeInt(4);

    // 4 - Fact Data
    fm.writeInt(samplesLength);

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
  public byte[] buildXma2Header(int frequency, short channels, short bits, int dataLength, int sampleCount) {
    if (frequency <= 0) {
      frequency = 44100;
    }
    if (channels <= 0) {
      channels = 1;
    }
    if (bits <= 0) {
      bits = 16;
    }

    ByteBuffer byteBuffer = new ByteBuffer(44);
    FileManipulator fm = new FileManipulator(byteBuffer);

    // 4 - Header (RIFF)
    fm.writeString("RIFF");

    // 4 - Length
    int length = 4 + 8 + 52 + 8 + dataLength;
    fm.writeInt(length);

    // 4 - Header 2 (WAVE)
    fm.writeString("WAVE");

    // 4 - Header 3 (fmt )
    fm.writeString("fmt ");

    // 4 - Block Size (52)
    fm.writeInt(52);

    // 2 - Format Tag (0x0166)
    fm.writeShort(0x0166);

    // 2 - Channels
    fm.writeShort(channels);

    // 4 - Samples per Second (Frequency)
    fm.writeInt(frequency);

    // 4 - Average Bytes per Second (used only by the encoder)
    fm.writeInt(dataLength);

    // 2 - Block Alignment (4)
    fm.writeShort(4);

    // 2 - Bits Per Sample (bits)
    fm.writeShort(bits);

    // 2 - Remaining Block Size (34)
    fm.writeShort(34);

    // 2 - Number of audio streams (1 or 2 channels each)
    fm.writeShort(1);

    // 4 - Spatial positions of the channels in this file
    int mask = 0;
    for (int i = 0; i < channels; i++) {
      mask = 1 << 1;
    }
    fm.writeInt(mask);

    // 4 - Total number of PCM samples the file decodes to
    fm.writeInt(sampleCount);

    // 4 - XMA block size (but the last one may be shorter)
    fm.writeInt(0x10000);

    // 4 - First valid sample in the decoded audio
    fm.writeInt(0);

    // 4 - Length of the valid part of the decoded audio
    fm.writeInt(sampleCount);

    // 4 - Beginning of the loop region in decoded sample terms
    fm.writeInt(0);

    // 4 - Length of the loop region in decoded sample terms
    fm.writeInt(0);

    // 1 - Number of loop repetitions; 255 = infinite
    fm.writeByte(0);

    // 1 - Version of XMA encoder that generated the file
    fm.writeByte(3);

    // 2 - XMA blocks in file (and entries in its seek table)
    fm.writeShort(1);

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

}