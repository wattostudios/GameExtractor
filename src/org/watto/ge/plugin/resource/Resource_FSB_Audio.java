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

package org.watto.ge.plugin.resource;

import java.io.File;
import org.watto.datatype.Resource;
import org.watto.ge.plugin.ExporterPlugin;
import org.watto.io.FilenameSplitter;

public class Resource_FSB_Audio extends Resource {

  /* 8bit integer PCM data. */
  public static int CODEC_PCM8 = 1;

  /* 16bit integer PCM data. */
  public static int CODEC_PCM16 = 2;

  /* 24bit integer PCM data. */
  public static int CODEC_PCM24 = 3;

  /* 32bit integer PCM data. */
  public static int CODEC_PCM32 = 4;

  /* 32bit floating point PCM data. */
  public static int CODEC_PCMFLOAT = 5;

  /* Compressed Nintendo 3DS/Wii DSP data. */
  public static int CODEC_GCADPCM = 6;

  /* Compressed IMA ADPCM data. */
  public static int CODEC_IMAADPCM = 7;

  /* Compressed PlayStation Portable ADPCM data. */
  public static int CODEC_VAG = 8;

  /* Compressed PSVita ADPCM data. */
  public static int CODEC_HEVAG = 9;

  /* Compressed Xbox360 XMA data. */
  public static int CODEC_XMA = 10;

  /* Compressed MPEG layer 2 or 3 data. */
  public static int CODEC_MPEG = 11;

  /* Compressed CELT data. */
  public static int CODEC_CELT = 12;

  /* Compressed PSVita ATRAC9 data. */
  public static int CODEC_AT9 = 13;

  /* Compressed Xbox360 xWMA data. */
  public static int CODEC_XWMA = 14;

  /* Compressed Vorbis data. */
  public static int CODEC_VORBIS = 15;

  /* IT214 data. */
  public static int CODEC_IT214 = 16;

  /* IT215 data. */
  public static int CODEC_IT215 = 17;

  /** The audio codec used by this file **/
  int codec = -1;

  /** The audio frequency (22050, 44000, etc); **/
  int frequency = 0;

  /** Mono or Stereo **/
  short channels = 1;

  /** bitrate **/
  short bits = 16;

  /** Samples Length **/
  int samplesLength = 0;

  /** the CRC used to locate the Setup Header (for OGG) **/
  int setupCRC = 0;

  public int getSetupCRC() {
    return setupCRC;
  }

  public void setSetupCRC(int setupCRC) {
    this.setupCRC = setupCRC;
  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public Resource_FSB_Audio() {
    super();
  }

  public Resource_FSB_Audio(File sourcePath) {
    super(sourcePath);
  }

  public Resource_FSB_Audio(File sourcePath, long offset, long length) {
    super(sourcePath, offset, length);
  }

  public Resource_FSB_Audio(File sourcePath, String name) {
    super(sourcePath, name);
  }

  public Resource_FSB_Audio(File sourcePath, String name, long offset) {
    super(sourcePath, name, offset);
  }

  public Resource_FSB_Audio(File sourcePath, String name, long offset, long length) {
    super(sourcePath, name, offset, length);
  }

  public Resource_FSB_Audio(File sourcePath, String name, long offset, long length, long decompLength) {
    super(sourcePath, name, offset, length, decompLength);
  }

  // ORIGINAL CONSTRUCTORS

  public Resource_FSB_Audio(File sourcePath, String name, long offset, long length, long decompLength, ExporterPlugin exporter) {
    super(sourcePath, name, offset, length, decompLength, exporter);
  }

  public Resource_FSB_Audio(String name, long offset, long length) {
    super(name, offset, length);
  }

  public int getSamplesLength() {
    return samplesLength;
  }

  public void setSamplesLength(int samplesLength) {
    this.samplesLength = samplesLength;
  }

  /**
   **********************************************************************************************
   Adds an appropriate file extension to the filename, based on the codec.
   **********************************************************************************************
   **/
  public void addExtensionForCodec() {
    String proposedExtension = getExtensionForCodec(codec);
    String extension = FilenameSplitter.getExtension(this.name);
    if (!proposedExtension.equalsIgnoreCase("." + extension)) {
      this.name += proposedExtension;
      this.origName = this.name;
    }
  }

  /**
   **********************************************************************************************
   Adds an appropriate file extension to the filename, based on the codec
   **********************************************************************************************
   **/
  public static String getExtensionForCodec(int codec) {
    if (codec == CODEC_VORBIS) {
      return ".ogg";
    }
    else if (codec == CODEC_PCM8) {
      return ".wav";
    }
    else if (codec == CODEC_PCM16) {
      return ".wav";
    }
    else if (codec == CODEC_PCM24) {
      return ".wav";
    }
    else if (codec == CODEC_PCM32) {
      return ".wav";
    }
    else if (codec == CODEC_PCMFLOAT) {
      return ".wav";
    }
    else if (codec == CODEC_GCADPCM) {
      return ".genh";
    }
    else if (codec == CODEC_IMAADPCM) {
      return ".wav";
    }
    else if (codec == CODEC_VAG) {
      return ".ss2";
    }
    else if (codec == CODEC_HEVAG) {
      return ".ss2";
    }
    else if (codec == CODEC_XMA) {
      return ".xma";
    }
    else if (codec == CODEC_MPEG) {
      return ".mp3";
    }
    else if (codec == CODEC_CELT) {
      return ".celt";
    }
    else if (codec == CODEC_AT9) {
      return ".at9";
    }
    else if (codec == CODEC_XWMA) {
      return ".xwma";
    }
    else if (codec == CODEC_IT214) {
      return ".it";
    }
    else if (codec == CODEC_IT215) {
      return ".it";
    }
    else {
      return ".snd";
    }

  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  @Override
  public Object clone() {
    Resource_FSB_Audio newRes = new Resource_FSB_Audio(sourcePath, origName, offset, length, decompLength, exporter);

    // Important - sets the new and orig name!
    newRes.setName(name);

    newRes.setExportedPath(exportedPath);
    newRes.setReplaced(replaced);

    newRes.setCodec(codec);
    newRes.setBits(bits);
    newRes.setFrequency(frequency);
    newRes.setChannels(channels);

    return newRes;
  }

  /**
   **********************************************************************************************
   * Copies all the values from <i>resource</i> into this resource (ie does a replace without
   * affecting pointers)
   **********************************************************************************************
   **/
  @Override
  public void copyFrom(Resource resource) {
    this.decompLength = resource.getDecompressedLength();
    this.exporter = resource.getExporter();
    this.length = resource.getLength();
    this.offset = resource.getOffset();
    this.name = resource.getName();
    this.sourcePath = resource.getSource();

    //this.exportedPath = resource.getExportedPath();
    setExportedPath(resource.getExportedPath());

    this.origName = resource.getOriginalName();
    this.replaced = resource.isReplaced();

    if (resource instanceof Resource_FSB_Audio) {
      Resource_FSB_Audio castResource = (Resource_FSB_Audio) resource;

      this.codec = castResource.getCodec();
      this.channels = castResource.getChannels();
      this.frequency = castResource.getFrequency();
      this.bits = castResource.getBits();

    }
  }

  public short getBits() {
    return bits;
  }

  public short getChannels() {
    return channels;
  }

  public int getCodec() {
    return codec;
  }

  /////
  //
  // METHODS
  //
  /////

  public int getFrequency() {
    return frequency;
  }

  public void setBits(short bits) {
    this.bits = bits;
  }

  public void setChannels(short channels) {
    this.channels = channels;
  }

  public void setCodec(int codec) {
    this.codec = codec;
  }

  public void setFrequency(int frequency) {
    this.frequency = frequency;
  }

}