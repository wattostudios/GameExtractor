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
import org.watto.io.FileManipulator;
import org.watto.io.converter.ByteConverter;

public class Exporter_Custom_FSB5_MP3 extends ExporterPlugin {

  static Exporter_Custom_FSB5_MP3 instance = new Exporter_Custom_FSB5_MP3();

  byte[] buffer = null;

  int bufferLength = 0;

  int bufferPos = 0;

  /**
  **********************************************************************************************
  When exporting Audio from an FSB FMOD Soundbank, this exporter fixes the MP3 headers.
  Based off source code from http://aluigi.altervista.org/papers.htm#fsbext
  **********************************************************************************************
  **/
  public static Exporter_Custom_FSB5_MP3 getInstance() {
    return instance;
  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public Exporter_Custom_FSB5_MP3() {
    setName("FSB FMOD MP3 Audio Exporter");
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
  public String getDescription() {
    return "This exporter extracts MP3 audio from FSB FMOD Soundbank archive\n\n" + super.getDescription();
  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  @Override
  public void open(Resource source) {
    try {

      FileManipulator fm = new FileManipulator(source.getSource(), false);
      fm.seek(source.getOffset());

      int length = (int) source.getLength();

      short channels = 1;
      //boolean multiChannel = false;

      if (source instanceof Resource_FSB_Audio) {
        // generate the appropriate header for the codec
        Resource_FSB_Audio resource = (Resource_FSB_Audio) source;
        channels = resource.getChannels();
        /*
        try {
          multiChannel = (resource.getProperty("MultiChannel") == 1);
        }
        catch (Throwable t) {
        }
        */
      }

      buildMP3Buffer(fm, length, channels);//, multiChannel);

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
  @Override
  public int read() {
    try {
      int currentByte = buffer[bufferPos];
      bufferPos++;
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
  public void buildMP3Buffer(FileManipulator fm, int length, int channels) {//, boolean multiChannel) {

    // set at 0 so that, if something fails, the buffer is effectively empty
    bufferPos = 0;
    bufferLength = 0;

    int maxLength = length;
    buffer = new byte[maxLength];

    int frame_chans = ((channels & 1) == 1) ? channels : (channels / 2);

    int frameNumber = 0;
    while (length > 0) {
      frameNumber++;

      byte[] headerBytes = fm.readBytes(3);
      length -= 3;

      if (length <= 0) {
        break;
      }

      int t = 0;

      while (length > 0) {
        t = mpg_get_frame_size(headerBytes);
        if (t != 0) {
          break; // a valid header length
        }

        byte nextByte = fm.readByte();
        length--;
        if (length <= 0) {
          break;
        }

        headerBytes[0] = headerBytes[1];
        headerBytes[1] = headerBytes[2];
        headerBytes[2] = nextByte;
      }

      if (length <= 0) {
        break;
      }

      if (t > maxLength) {
        ErrorLogger.log("[Exporter_Custom_FSB5_MP3] Invalid mpeg frame size: " + t);
        return;
      }

      t -= 3;
      if ((length - t) < 0) {
        break;
      }

      boolean MP3_CHANS_DOWNMIX = ((channels <= 2) | ((frameNumber % frame_chans) == 0));

      if (MP3_CHANS_DOWNMIX) {
        buffer[bufferPos] = headerBytes[0];
        buffer[bufferPos + 1] = headerBytes[1];
        buffer[bufferPos + 2] = headerBytes[2];
        bufferPos += 3;
      }

      if (t > 0) {
        byte[] readBytes = fm.readBytes(t);
        length -= t;

        if (MP3_CHANS_DOWNMIX) {
          System.arraycopy(readBytes, 0, buffer, bufferPos, t);
          bufferPos += t;
        }
      }

    }

    bufferLength = bufferPos;
    bufferPos = 0;

  }

  /**
  **********************************************************************************************
  from http://www.hydrogenaudio.org/forums/index.php?showtopic=85125
  **********************************************************************************************
  **/
  int mpg_get_frame_size(byte[] hdr) {

    // Bitrates - use [version][layer][bitrate]
    int mpeg_bitrates[][][] = {
        { // Version 2.5
            { 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0 }, // Reserved
            { 0, 8, 16, 24, 32, 40, 48, 56, 64, 80, 96, 112, 128, 144, 160, 0 }, // Layer 3
            { 0, 8, 16, 24, 32, 40, 48, 56, 64, 80, 96, 112, 128, 144, 160, 0 }, // Layer 2
            { 0, 32, 48, 56, 64, 80, 96, 112, 128, 144, 160, 176, 192, 224, 256, 0 }  // Layer 1
        },
        { // Reserved
            { 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0 }, // Invalid
            { 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0 }, // Invalid
            { 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0 }, // Invalid
            { 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0 }  // Invalid
        },
        { // Version 2
            { 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0 }, // Reserved
            { 0, 8, 16, 24, 32, 40, 48, 56, 64, 80, 96, 112, 128, 144, 160, 0 }, // Layer 3
            { 0, 8, 16, 24, 32, 40, 48, 56, 64, 80, 96, 112, 128, 144, 160, 0 }, // Layer 2
            { 0, 32, 48, 56, 64, 80, 96, 112, 128, 144, 160, 176, 192, 224, 256, 0 }  // Layer 1
        },
        { // Version 1
            { 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0 }, // Reserved
            { 0, 32, 40, 48, 56, 64, 80, 96, 112, 128, 160, 192, 224, 256, 320, 0 }, // Layer 3
            { 0, 32, 48, 56, 64, 80, 96, 112, 128, 160, 192, 224, 256, 320, 384, 0 }, // Layer 2
            { 0, 32, 64, 96, 128, 160, 192, 224, 256, 288, 320, 352, 384, 416, 448, 0 }, // Layer 1
        }
    };

    // Sample rates - use [version][srate]
    int mpeg_srates[][] = {
        { 11025, 12000, 8000, 0 }, // MPEG 2.5
        { 0, 0, 0, 0 }, // Reserved
        { 22050, 24000, 16000, 0 }, // MPEG 2
        { 44100, 48000, 32000, 0 }  // MPEG 1
    };

    // Samples per frame - use [version][layer]
    int mpeg_frame_samples[][] = {
        //    Rsvd     3     2     1  < Layer  v Version
        { 0, 576, 1152, 384 }, //       2.5
        { 0, 0, 0, 0 }, //       Reserved
        { 0, 576, 1152, 384 }, //       2
        { 0, 1152, 1152, 384 }  //       1
    };

    // Slot size (MPEG unit of measurement) - use [layer]
    int mpeg_slot_size[] = { 0, 1, 1, 4 }; // Rsvd, 3, 2, 1

    // Quick validity check
    if (((ByteConverter.unsign(hdr[0]) & 0xFF) != 0xFF)
        || ((ByteConverter.unsign(hdr[1]) & 0xE0) != 0xE0)   // 3 sync bits
        || ((ByteConverter.unsign(hdr[1]) & 0x18) == 0x08)   // Version rsvd
        || ((ByteConverter.unsign(hdr[1]) & 0x06) == 0x00)   // Layer rsvd
        || ((ByteConverter.unsign(hdr[2]) & 0xF0) == 0xF0)   // Bitrate rsvd
    )
      return 0;

    // Data to be extracted from the header
    int ver = (hdr[1] & 0x18) >> 3;   // Version index
    int lyr = (hdr[1] & 0x06) >> 1;   // Layer index
    int pad = (hdr[2] & 0x02) >> 1;   // Padding? 0/1
    int brx = (hdr[2] & 0xf0) >> 4;   // Bitrate index
    int srx = (hdr[2] & 0x0c) >> 2;   // SampRate index

    // Lookup real values of these fields
    int bitrate = mpeg_bitrates[ver][lyr][brx] * 1000;
    int samprate = mpeg_srates[ver][srx];
    int samples = mpeg_frame_samples[ver][lyr];
    int slot_size = mpeg_slot_size[lyr];

    // In-between calculations
    float bps = ((float) samples) / 8.0f;
    float fsize = ((bps * (float) bitrate) / (float) samprate);

    if (pad != 0) {
      fsize += slot_size;
    }

    // Frame sizes are truncated integers
    return (int) fsize;
  }

}