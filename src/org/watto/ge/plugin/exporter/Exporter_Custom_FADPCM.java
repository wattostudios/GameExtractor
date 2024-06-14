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
import org.watto.ge.plugin.resource.Resource_FSB_Audio;
import org.watto.ge.plugin.resource.Resource_WAV_RawAudio;
import org.watto.io.FileManipulator;
import org.watto.io.buffer.ByteBuffer;
import org.watto.io.converter.ByteArrayConverter;
import org.watto.io.converter.IntConverter;

public class Exporter_Custom_FADPCM extends ExporterPlugin {

  static Exporter_Custom_FADPCM instance = new Exporter_Custom_FADPCM();

  /**
  **********************************************************************************************
  Converts FMOD ADPCM audio into WAV audio
  Ref: https://github.com/vgmstream/vgmstream/blob/master/src/coding/fadpcm_decoder.c
  **********************************************************************************************
  **/
  public static Exporter_Custom_FADPCM getInstance() {
    return instance;
  }

  int headerPos = 0;

  int headerLength = 0;

  boolean writingHeader = true;

  byte[] header = new byte[0];

  byte[] footer = new byte[0];

  int footerPos = 0;

  int footerLength = 0;

  boolean signed = true;

  byte[] buffer = null;

  int bufferPos = 0;

  int bufferLength = 0;

  boolean writeHeaders = true; // will write WAV headers for the extracted file

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public Exporter_Custom_FADPCM() {
    setName("FMOD ADPCM Audio Exporter");
  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public Exporter_Custom_FADPCM(boolean writeHeadersIn) {
    super();
    this.writeHeaders = writeHeadersIn;
  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  @Override
  public boolean available() {
    return (bufferPos < bufferLength || writingHeader);
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
  public String getDescription() {
    return "This exporter converts FMOD ADPCM audio into WAV audio.\n\n" + super.getDescription();
  }

  int loop = 1;

  short channels = 1;

  short bitrate = 16;

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  @Override
  public void open(Resource source) {
    try {
      bufferPos = 0;
      bufferLength = 0;

      int rawLength = (int) source.getLength();

      FileManipulator fm = new FileManipulator(source.getSource(), false);
      fm.seek(source.getOffset());

      byte[] compBytes = fm.readBytes(rawLength);
      fm.close();

      int frequency = 22050;

      short codec = 0x0001;
      signed = true;
      byte[] extraData = null;
      int samples = -1;
      short blockAlign = -1;
      loop = 0;

      if (source instanceof Resource_WAV_RawAudio) {
        Resource_WAV_RawAudio audioSource = (Resource_WAV_RawAudio) source;
        frequency = audioSource.getFrequency();
        bitrate = audioSource.getBitrate();
        channels = audioSource.getChannels();
        //audioLength = audioSource.getAudioLength();
        signed = audioSource.isSigned();
        codec = audioSource.getCodec();
        samples = audioSource.getSamples();
        blockAlign = audioSource.getBlockAlign();

        extraData = audioSource.getExtraData();

        try {
          loop = Integer.parseInt(audioSource.getProperty("Loop"));
        }
        catch (Throwable t) {
        }
      }
      else if (source instanceof Resource_FSB_Audio) {
        Resource_FSB_Audio audioSource = (Resource_FSB_Audio) source;
        frequency = audioSource.getFrequency();
        bitrate = audioSource.getBits();
        channels = audioSource.getChannels();
      }
      else {
        try {
          // see if the frequency edt are stored on the resource as properties
          String frequencyString = source.getProperty("Frequency");
          if (frequencyString != null && frequencyString.length() > 0) {
            frequency = Integer.parseInt(frequencyString);
          }

          String bitrateString = source.getProperty("Bitrate");
          if (bitrateString != null && bitrateString.length() > 0) {
            bitrate = Short.parseShort(bitrateString);
          }

          String channelsString = source.getProperty("Channels");
          if (channelsString != null && channelsString.length() > 0) {
            channels = Short.parseShort(channelsString);
          }
        }
        catch (Throwable t) {
        }
      }

      // now decompress the audio
      buffer = decompressAudio(compBytes);

      bufferPos = 0;
      bufferLength = buffer.length;

      // now build the header
      int audioLength = (int) bufferLength;

      header = pcmwav_header(frequency, channels, bitrate, audioLength, codec, samples, blockAlign, extraData, loop);

      headerPos = 0;
      headerLength = header.length;

      writingHeader = true; // so when we call read(), it accesses the header bytes

      if (!writeHeaders) {
        // we don't actually want to write the headers, just decompress the VAG data
        writingHeader = false;
      }

    }
    catch (Throwable t) {
    }
  }

  boolean enable_looping = false;

  /**
   **********************************************************************************************
   NOT SUPPORTED
   **********************************************************************************************
   **/
  @Override
  public void pack(Resource source, FileManipulator destination) {
    try {

    }
    catch (Throwable t) {
      logError(t);
    }
  }

  int predict_nr = 0;

  int shift_factor = 0;

  int flags = 0;

  double f[][] = new double[][] { { 0.0, 0.0 },
      { -60.0 / 64.0, 0.0 },
      { -115.0 / 64.0, 52.0 / 64.0 },
      { -98.0 / 64.0, 55.0 / 64.0 },
      { -122.0 / 64.0, 60.0 / 64.0 } };

  double _s_1 = 0.0;                            // s[t-1]

  double _s_2 = 0.0;                            // s[t-2]

  double pack_s_1 = 0.0;

  double pack_s_2 = 0.0;

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public byte[] pcmwav_header(int frequency, short channels, short bits, int dataLength, short codec, int samples, short blockAlign, byte[] extraData, int loop) {
    int headerSize = 44;

    ByteBuffer byteBuffer = new ByteBuffer(headerSize);
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
    int blockSize = 16;
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

      if (writingHeader) {
        if (headerPos != headerLength) {
          headerPos++;
          return header[headerPos - 1];
        }
        writingHeader = false;
      }

      //return readSource.readByte();
      int currentByte = buffer[bufferPos];
      if (!signed) {
        currentByte = 127 + currentByte;
      }

      bufferPos++;

      return currentByte;
    }
    catch (Throwable t) {
      return 0;
    }
  }

  double vagPrev1 = 0.0;

  double vagPrev2 = 0.0;

  int fadpcm_coefs[][] = {
      { 0, 0 },
      { 60, 0 },
      { 122, 60 },
      { 115, 52 },
      { 98, 55 },
      { 0, 0 },
      { 0, 0 },
      { 0, 0 }
  };

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public byte[] decompressAudio(byte[] compBytes) {

    FileManipulator fm = new FileManipulator(new ByteBuffer(compBytes));

    int compLength = compBytes.length;

    int channelspacing = channels * (bitrate / 8);//4; 
    int first_sample = 0;//int32_t first_sample

    int frame_offset;//off_t frame_offset;
    int i, j, k, frames_in, sample_count = 0, samples_done = 0;
    int bytes_per_frame, samples_per_frame;//size_t bytes_per_frame, samples_per_frame;
    long coefs, shifts;//uint32_t coefs, shifts;
    int hist1; //int32_t
    int hist2;//int32_t

    /* external interleave (fixed size), mono */
    bytes_per_frame = 0x8c;
    samples_per_frame = (bytes_per_frame - 0xc) * 2;
    frames_in = compLength / bytes_per_frame;
    first_sample = first_sample % samples_per_frame;

    //int samples_to_do = 45824;//int32_t samples_to_do
    int samples_to_do = frames_in * samples_per_frame;

    int decompLength = samples_to_do * channelspacing;
    byte[] decompBytes = new byte[decompLength];

    /* parse 0xc header (header samples are not written to outbuf) */
    frame_offset = 0;//stream->offset + bytes_per_frame * frames_in;
    //read_streamfile(frame, frame_offset, bytes_per_frame, stream->streamfile); /* ignore EOF errors */

    try {

      for (int f = 0; f < frames_in; f++) {
        //System.out.println("Frame " + f + " of " + frames_in);
        coefs = IntConverter.unsign(fm.readInt());//get_u32le(frame + 0x00);
        shifts = IntConverter.unsign(fm.readInt());//get_u32le(frame + 0x04);
        hist1 = fm.readShort();//get_s16le(frame + 0x08);
        hist2 = fm.readShort();//get_s16le(frame + 0x0a);

        /* decode nibbles, grouped in 8 sets of 0x10 * 0x04 * 2 */
        for (i = 0; i < 8; i++) {
          int index, shift, coef1, coef2;

          /* each set has its own coefs/shifts (indexes > 7 are repeat, ex. 0x9 is 0x2) */
          index = (int) (((coefs >> i * 4) & 0x0f) % 0x07);
          shift = (int) ((shifts >> i * 4) & 0x0f);

          coef1 = fadpcm_coefs[index][0];
          coef2 = fadpcm_coefs[index][1];
          shift = 22 - shift; /* pre-adjust for 32b sign extend */

          for (j = 0; j < 4; j++) {
            //uint32_t nibbles = get_u32le(frame + 0x0c + 0x10*i + 0x04*j);
            fm.seek(frame_offset + 0x0c + 0x10 * i + 0x04 * j);
            long nibbles = IntConverter.unsign(fm.readInt());

            for (k = 0; k < 8; k++) {
              //int32_t sample;
              int sample;

              sample = (int) ((nibbles >> k * 4) & 0x0f);
              sample = (sample << 28) >> shift; /* 32b sign extend + scale */
              sample = (sample - hist2 * coef2 + hist1 * coef1) >> 6;
              sample = clamp16(sample);

              if (sample_count >= first_sample && samples_done < samples_to_do) {
                //outbuf[samples_done * channelspacing] = sample;
                int outPos = samples_done * channelspacing;
                byte[] sampleBytes = ByteArrayConverter.convertLittle((short) sample);
                decompBytes[outPos] = sampleBytes[0];
                decompBytes[outPos + 1] = sampleBytes[1];
                samples_done++;
              }
              sample_count++;

              hist2 = hist1;
              hist1 = sample;
            }
          }
        }

        frame_offset = (int) fm.getOffset();
      }

      fm.close();

    }
    catch (Throwable t) {
      ErrorLogger.log(t);
    }

    return decompBytes;
  }

  int clamp16(int val) {
    if (val > 32767)
      return 32767;
    else if (val < -32768)
      return -32768;
    else
      return val;
  }

  //PSX ADPCM coefficients
  double K0[] = new double[] { 0, 0.9375, 1.796875, 1.53125, 1.90625 };

  double K1[] = new double[] { 0, 0, -0.8125, -0.859375, -0.9375 };

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  short vagToPcm(int soundParameter, int soundData) {
    int resultInt = 0;

    double dTmp1 = 0.0;
    double dTmp2 = 0.0;
    double dTmp3 = 0.0;

    if (soundData > 7)
      soundData -= 16;

    dTmp1 = (double) soundData * Math.pow(2, (double) (12 - (soundParameter & 0x0F)));

    dTmp2 = (vagPrev1) * K0[(soundParameter >> 4) & 0x0F];
    dTmp3 = (vagPrev2) * K1[(soundParameter >> 4) & 0x0F];

    (vagPrev2) = (vagPrev1);
    (vagPrev1) = dTmp1 + dTmp2 + dTmp3;

    resultInt = (int) Math.round((vagPrev1));

    if (resultInt > 32767)
      resultInt = 32767;

    if (resultInt < -32768)
      resultInt = -32768;

    return (short) resultInt;
  }

}