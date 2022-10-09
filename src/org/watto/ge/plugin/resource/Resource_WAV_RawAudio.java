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
import org.watto.ge.plugin.exporter.Exporter_Custom_WAV_RawAudio;

public class Resource_WAV_RawAudio extends Resource {

  int frequency = 22050;

  short bitrate = 16;

  short channels = 1;

  int audioLength = 0;

  short codec = 0x0001;

  byte[] extraData = null;

  int samples = -1;

  short blockAlign = -1;

  public short getBlockAlign() {
    return blockAlign;
  }

  public void setBlockAlign(short blockAlign) {
    this.blockAlign = blockAlign;
  }

  public int getSamples() {
    return samples;
  }

  public void setSamples(int samples) {
    this.samples = samples;
  }

  public byte[] getExtraData() {
    return extraData;
  }

  public void setExtraData(byte[] extraData) {
    this.extraData = extraData;
  }

  public short getCodec() {
    return codec;
  }

  public void setCodec(short codec) {
    this.codec = codec;
  }

  boolean signed = true;

  public boolean isSigned() {
    return signed;
  }

  public void setSigned(boolean signed) {
    this.signed = signed;
  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public Resource_WAV_RawAudio() {
    super();
    setExporter(Exporter_Custom_WAV_RawAudio.getInstance());
  }

  // ORIGINAL CONSTRUCTORS

  public Resource_WAV_RawAudio(File sourcePath) {
    super(sourcePath);
    setExporter(Exporter_Custom_WAV_RawAudio.getInstance());
  }

  public Resource_WAV_RawAudio(File sourcePath, long offset, long length) {
    super(sourcePath, offset, length);
    setExporter(Exporter_Custom_WAV_RawAudio.getInstance());
  }

  public Resource_WAV_RawAudio(File sourcePath, String name) {
    super(sourcePath, name);
    setExporter(Exporter_Custom_WAV_RawAudio.getInstance());
  }

  public Resource_WAV_RawAudio(File sourcePath, String name, long offset) {
    super(sourcePath, name, offset);
    setExporter(Exporter_Custom_WAV_RawAudio.getInstance());
  }

  public Resource_WAV_RawAudio(File sourcePath, String name, long offset, long length) {
    super(sourcePath, name, offset, length);
    setExporter(Exporter_Custom_WAV_RawAudio.getInstance());
  }

  public Resource_WAV_RawAudio(File sourcePath, String name, long offset, long length, long decompLength) {
    super(sourcePath, name, offset, length, decompLength);
    setExporter(Exporter_Custom_WAV_RawAudio.getInstance());
  }

  public Resource_WAV_RawAudio(File sourcePath, String name, long offset, long length, long decompLength, ExporterPlugin exporter) {
    super(sourcePath, name, offset, length, decompLength, exporter);
    setExporter(exporter);
  }

  public Resource_WAV_RawAudio(String name, long offset, long length) {
    super(name, offset, length);
    setExporter(Exporter_Custom_WAV_RawAudio.getInstance());
  }

  /////
  //
  // METHODS
  //
  /////

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  @Override
  public Object clone() {
    Resource_WAV_RawAudio newRes = new Resource_WAV_RawAudio(sourcePath, origName, offset, length, decompLength, exporter);

    // Important - sets the new and orig name!
    newRes.setName(name);

    newRes.setExportedPath(exportedPath);
    newRes.setReplaced(replaced);

    newRes.setAudioProperties(frequency, bitrate, channels, audioLength);

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

    if (resource instanceof Resource_WAV_RawAudio) {
      Resource_WAV_RawAudio castResource = (Resource_WAV_RawAudio) resource;

      this.frequency = castResource.getFrequency();
      this.bitrate = castResource.getBitrate();
      this.channels = castResource.getChannels();
      this.audioLength = castResource.getAudioLength();

    }
  }

  public int getAudioLength() {
    return audioLength;
  }

  public short getBitrate() {
    return bitrate;
  }

  public short getChannels() {
    return channels;
  }

  public int getFrequency() {
    return frequency;
  }

  public void setAudioLength(int audioLength) {
    this.audioLength = audioLength;
  }

  public void setAudioProperties(int frequency, short bitrate, short channels) {
    this.frequency = frequency;
    this.bitrate = bitrate;
    this.channels = channels;
    this.audioLength = (int) getLength();
  }

  public void setAudioProperties(int frequency, int bitrate, int channels) {
    setAudioProperties(frequency, (short) bitrate, (short) channels);
  }

  public void setAudioProperties(int frequency, short bitrate, short channels, boolean signed) {
    this.frequency = frequency;
    this.bitrate = bitrate;
    this.channels = channels;
    this.audioLength = (int) getLength();
    this.signed = signed;
  }

  /** In case we want to force a special audio length in the WAV header only **/
  public void setAudioProperties(int frequency, short bitrate, short channels, int audioLength) {
    this.frequency = frequency;
    this.bitrate = bitrate;
    this.channels = channels;
    this.audioLength = audioLength;
  }

  public void setBitrate(short bitrate) {
    this.bitrate = bitrate;
  }

  public void setChannels(short channels) {
    this.channels = channels;
  }

  public void setFrequency(int frequency) {
    this.frequency = frequency;
  }

}