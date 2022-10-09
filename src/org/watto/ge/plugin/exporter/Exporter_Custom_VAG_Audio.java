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
import org.watto.ge.plugin.resource.Resource_WAV_RawAudio;
import org.watto.io.FileManipulator;
import org.watto.io.buffer.ByteBuffer;
import org.watto.io.converter.ByteArrayConverter;
import org.watto.io.converter.ByteConverter;
import org.watto.io.converter.ShortConverter;

public class Exporter_Custom_VAG_Audio extends ExporterPlugin {

  static Exporter_Custom_VAG_Audio instance = new Exporter_Custom_VAG_Audio();

  /**
  **********************************************************************************************
  Converts VAG (PSX) audio into WAV audio
  Ref: https://github.com/OpenDriver2/OpenDriver2Tools/blob/master/DriverSoundTool/driver_sound.cpp
  **********************************************************************************************
  **/
  public static Exporter_Custom_VAG_Audio getInstance() {
    return instance;
  }

  int headerPos = 0;

  int headerLength = 0;

  boolean writingHeader = true;

  byte[] header = new byte[0];

  byte[] footer = new byte[0];

  int footerPos = 0;

  int footerLength = 0;

  boolean writingFooter = false;

  boolean signed = true;

  byte[] buffer = null;

  int bufferPos = 0;

  int bufferLength = 0;

  boolean writeHeaders = true; // will write WAV headers for the extracted file

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public Exporter_Custom_VAG_Audio() {
    setName("VAG PSX Audio Exporter");
  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public Exporter_Custom_VAG_Audio(boolean writeHeadersIn) {
    super();
    this.writeHeaders = writeHeadersIn;
  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  @Override
  public boolean available() {
    return (bufferPos < bufferLength || writingHeader || writingFooter);
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
    return "This exporter converts VAG (PSX) audio into WAV audio.\n\n" + super.getDescription();
  }

  int loop = 1;

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
      int decompLength = (int) source.getDecompressedLength();

      if (decompLength == rawLength) {
        decompLength = (rawLength >> 4) * 28 * 2;
      }

      FileManipulator fm = new FileManipulator(source.getSource(), false);
      fm.seek(source.getOffset());

      byte[] compBytes = fm.readBytes(rawLength);
      fm.close();

      buffer = new byte[decompLength];
      decompressAudio(compBytes, buffer);

      bufferPos = 0;
      bufferLength = decompLength;

      int frequency = 22050;
      short bitrate = 16;
      short channels = 1;
      int audioLength = (int) decompLength;
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

      header = pcmwav_header(frequency, channels, bitrate, audioLength, codec, samples, blockAlign, extraData, loop);

      headerPos = 0;
      headerLength = header.length;

      footer = pcmwav_footer(frequency, channels, bitrate, audioLength, codec, samples, blockAlign, extraData, loop);

      footerPos = 0;
      footerLength = footer.length;

      writingHeader = true; // so when we call read(), it accesses the header bytes
      writingFooter = false; // so when we call read(), it accesses the footer bytes

      if (!writeHeaders) {
        // we don't actually want to write the headers, just decompress the VAG data
        writingHeader = false;
        writingFooter = false;
      }

    }
    catch (Throwable t) {
    }
  }

  boolean enable_looping = false;

  /**
   **********************************************************************************************
   Will pack a WAV data into a VAG, as long as the source is 16-bit mono
   **********************************************************************************************
   **/
  @Override
  public void pack(Resource source, FileManipulator destination) {
    try {
      //long decompLength = source.getDecompressedLength();

      ExporterPlugin exporter = source.getExporter();
      exporter.open(source);

      // reset globals
      enable_looping = false;
      flags = 0;

      try {
        enable_looping = (Integer.parseInt(source.getProperty("Loop")) != 0);
      }
      catch (Throwable t) {
      }

      if (enable_looping) {
        flags = 6;
      }

      // pack 28-bytes at a time
      short[] wave = new short[28];
      int wavePos = 0;

      int sourceLength = (int) source.getLength();
      int blockCount = sourceLength / 56; // 56 = 28*2 - we shrink 56 bytes into 28
      if (sourceLength % 56 != 0) {
        blockCount++;
      }

      while (exporter.available()) {
        if (wavePos < 28) {
          // read in a byte
          int byte1 = exporter.read();
          exporter.available(); // because each read() needs to have an available() first
          int byte2 = exporter.read();

          wave[wavePos] = ShortConverter.convertLittle(new byte[] { (byte) byte1, (byte) byte2 });
          wavePos++;
        }

        if (wavePos == 28) {
          blockCount--;
          boolean lastBlock = (blockCount <= 0);

          //System.out.println(blockCount + "\t" + lastBlock + "\t" + enable_looping);

          // now convert it and write it out
          packAndWrite(wave, 28, destination, lastBlock);
          wavePos = 0;
        }
      }

      if (wavePos != 0) {
        // write the remaining bytes for the end of the buffer
        for (int f = wavePos; f < 28; f++) {
          wave[f] = 0; // empty bytes to fill
        }

        blockCount--;
        boolean lastBlock = (blockCount <= 0);

        // now convert it and write it out
        packAndWrite(wave, wavePos, destination, lastBlock);
      }

      if (!enable_looping) {
        // write out the END block
        destination.writeByte(7);
        destination.writeByte(0);

        for (int f = 0; f < 14; f++) {
          destination.writeByte(119);
        }
      }

      exporter.close();

    }
    catch (Throwable t) {
      logError(t);
    }
  }

  /**
   **********************************************************************************************
  Ref: https://github.com/simias/psxsdk/blob/master/tools/wav2vag.c
   **********************************************************************************************
   **/
  public void packAndWrite(short[] wave, int waveLength, FileManipulator destination, boolean lastBlock) {

    if (lastBlock) {
      if (enable_looping) {
        flags = 3;
      }
      else {
        flags = 1;
      }
    }

    double[] d_samples = new double[28];
    short[] four_bit = new short[28];
    predict_nr = 0;
    shift_factor = 0;

    find_predict(wave, d_samples);
    packSamples(d_samples, four_bit);
    byte d = (byte) ((predict_nr << 4) | shift_factor);
    destination.writeByte(d);
    destination.writeByte(flags);

    for (int k = 0; k < 28; k += 2) {
      //d = (byte) (((ShortConverter.unsign(four_bit[k + 1]) >> 8) & 0xf0) | ((ShortConverter.unsign(four_bit[k]) >> 12) & 0xf));
      d = (byte) ((((four_bit[k + 1]) >> 8) & 0xf0) | (((four_bit[k]) >> 12) & 0xf));
      destination.writeByte(d);
    }

    /*
    if ((waveLength < 28 || lastBlock) && !enable_looping) {
      flags = 1;
    }
    */

    if (enable_looping) {
      flags = 2;
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

  /**
  **********************************************************************************************
  Ref: https://github.com/simias/psxsdk/blob/master/tools/wav2vag.c
  **********************************************************************************************
  **/
  void find_predict(short[] samples, double[] d_samples) {
    int i;
    int j;
    double[][] buffer = new double[28][5];
    double min = 1e10;
    double[] max = new double[5];
    double ds;
    int min2;
    int shift_mask;

    double s_0 = 0.0;
    double s_1 = 0.0;
    double s_2 = 0.0;

    for (i = 0; i < 5; i++) {
      max[i] = 0.0;
      s_1 = _s_1;
      s_2 = _s_2;
      for (j = 0; j < 28; j++) {
        //s_0 = (double) ShortConverter.unsign(samples[j]);                      // s[t-0]
        s_0 = (double) samples[j];                      // s[t-0]
        if (s_0 > 30719.0)
          s_0 = 30719.0;
        if (s_0 < -30720.0)
          s_0 = -30720.0;
        ds = s_0 + s_1 * f[i][0] + s_2 * f[i][1];
        buffer[j][i] = ds;
        if (Math.abs(ds) > max[i])
          max[i] = Math.abs(ds);
        //printf( "%+5.2f\n", s2 );
        s_2 = s_1;                                  // new s[t-2]
        s_1 = s_0;                                  // new s[t-1]
      }

      if (max[i] < min) {
        min = max[i];
        predict_nr = i;
      }
      if (min <= 7) {
        predict_nr = 0;
        break;
      }

    }

    //store s[t-2] and s[t-1] in a static variable
    //these than used in the next function call

    _s_1 = s_1;
    _s_2 = s_2;

    for (i = 0; i < 28; i++)
      d_samples[i] = buffer[i][predict_nr];

    //if ( min > 32767.0 )
    //min = 32767.0;

    min2 = (int) min;
    shift_mask = 0x4000;
    shift_factor = 0;

    while (shift_factor < 12) {
      if ((shift_mask & (min2 + (shift_mask >> 3))) != 0) {
        break;
      }
      (shift_factor)++;
      shift_mask = shift_mask >> 1;
    }

  }

  double pack_s_1 = 0.0;

  double pack_s_2 = 0.0;

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  void packSamples(double[] d_samples, short[] four_bit) {
    double ds;
    int di;
    double s_0;
    pack_s_1 = 0.0;
    pack_s_2 = 0.0;
    int i;

    for (i = 0; i < 28; i++) {
      s_0 = d_samples[i] + pack_s_1 * f[predict_nr][0] + pack_s_2 * f[predict_nr][1];
      ds = s_0 * (double) (1 << shift_factor);

      di = ((int) ds + 0x800) & 0xfffff000;

      if (di > 32767)
        di = 32767;
      if (di < -32768)
        di = -32768;

      four_bit[i] = (short) di;

      di = di >> shift_factor;
      pack_s_2 = pack_s_1;
      pack_s_1 = (double) di - s_0;

    }
  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public byte[] pcmwav_header(int frequency, short channels, short bits, int dataLength, short codec, int samples, short blockAlign, byte[] extraData, int loop) {
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
    if (loop != 0) {
      length += 68;
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
  public byte[] pcmwav_footer(int frequency, short channels, short bits, int dataLength, short codec, int samples, short blockAlign, byte[] extraData, int loop) {
    if (loop == 0) {
      return new byte[0];
    }

    int footerSize = 68;

    ByteBuffer byteBuffer = new ByteBuffer(footerSize);
    FileManipulator fm = new FileManipulator(byteBuffer);

    // 4 - Header (smpl)
    fm.writeString("smpl");

    // 4 - Length
    fm.writeInt(60);

    // 4 Manufacturer  0 - 0xFFFFFFFF
    fm.writeInt(0);

    // 4 Product 0 - 0xFFFFFFFF
    fm.writeInt(0);

    // 4 Sample Period 0 - 0xFFFFFFFF
    fm.writeInt((int) (1d / frequency * 1000000000));

    // 4 MIDI Unity Note 0 - 127
    fm.writeInt(60);

    // 4 MIDI Pitch Fraction 0 - 0xFFFFFFFF
    fm.writeInt(0);

    // 4 SMPTE Format  0, 24, 25, 29, 30
    fm.writeInt(0);

    // 4 SMPTE Offset  0 - 0xFFFFFFFF
    fm.writeInt(0);

    // 4 Num Sample Loops  0 - 0xFFFFFFFF
    fm.writeInt(loop);

    // 4 Sampler Data  0 - 0xFFFFFFFF
    fm.writeInt(0);

    // 4 Cue Point ID  0 - 0xFFFFFFFF
    fm.writeInt(0);

    // 4 Type  0 - 0xFFFFFFFF
    fm.writeInt(0);

    // 4 Start 0 - 0xFFFFFFFF
    fm.writeInt(0);

    // 4 End 0 - 0xFFFFFFFF
    fm.writeInt(dataLength);

    // 4 Fraction  0 - 0xFFFFFFFF
    fm.writeInt(0);

    // 4 Play Count  0 - 0xFFFFFFFF
    fm.writeInt(0);

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

      if (writingFooter) {
        if (footerPos != footerLength) {
          footerPos++;

          if (footerPos == footerLength) {
            // this is the last byte of the footer, so finish after returning this byte
            writingFooter = false;
          }

          return footer[footerPos - 1];
        }
        writingFooter = false;
      }

      //return readSource.readByte();
      int currentByte = buffer[bufferPos];
      if (!signed) {
        currentByte = 127 + currentByte;
      }

      bufferPos++;

      if (bufferPos >= bufferLength) {
        // finished writing the data, now write the footer after returning this byte
        writingFooter = true;

        if (footerPos == footerLength) {
          // there isn't actually a footer, so exit now
          writingFooter = false;
        }
        else if (!writeHeaders) {
          // don't want to write the headers (or footers)
          writingFooter = false;
        }
      }

      return currentByte;
    }
    catch (Throwable t) {
      return 0;
    }
  }

  double vagPrev1 = 0.0;

  double vagPrev2 = 0.0;

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public void decompressAudio(byte[] compBytes, byte[] decompBytes) {
    try {
      int soundSize = compBytes.length;

      int soundParam = 0;
      int soundData = 0;
      vagPrev1 = 0.0;
      vagPrev2 = 0.0;
      int k = 0;

      for (int i = 0; i < soundSize; i++) {
        if (i % 16 == 0) {
          soundParam = ByteConverter.unsign(compBytes[i]);
          i += 2;
        }

        soundData = (int) ByteConverter.unsign(compBytes[i]) & 0x0F;

        short decompShort = vagToPcm(soundParam, soundData);
        byte[] shortBytes = ByteArrayConverter.convertLittle(decompShort);
        decompBytes[k++] = shortBytes[0];
        decompBytes[k++] = shortBytes[1];

        soundData = ((int) ByteConverter.unsign(compBytes[i]) >> 4) & 0x0F;

        decompShort = vagToPcm(soundParam, soundData);
        shortBytes = ByteArrayConverter.convertLittle(decompShort);
        decompBytes[k++] = shortBytes[0];
        decompBytes[k++] = shortBytes[1];
      }
    }
    catch (Throwable t) {
      ErrorLogger.log(t);
    }
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