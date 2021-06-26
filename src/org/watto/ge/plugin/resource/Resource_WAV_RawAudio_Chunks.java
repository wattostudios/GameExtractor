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
import org.watto.ge.plugin.exporter.Exporter_Custom_WAV_RawAudio_Chunks;

public class Resource_WAV_RawAudio_Chunks extends Resource {

  int frequency = 22050;

  short bitrate = 16;

  short channels = 1;

  boolean writeHeader = true;

  long[] offsets = new long[0];

  long[] lengths = new long[0];

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public Resource_WAV_RawAudio_Chunks() {
    super();
    setExporter(Exporter_Custom_WAV_RawAudio_Chunks.getInstance());
  }

  public Resource_WAV_RawAudio_Chunks(File sourcePath) {
    super(sourcePath);
    setExporter(Exporter_Custom_WAV_RawAudio_Chunks.getInstance());
  }

  public Resource_WAV_RawAudio_Chunks(File sourcePath, long offset, long length) {
    super(sourcePath, offset, length);
    setExporter(Exporter_Custom_WAV_RawAudio_Chunks.getInstance());
  }

  public Resource_WAV_RawAudio_Chunks(File sourcePath, String name) {
    super(sourcePath, name);
    setExporter(Exporter_Custom_WAV_RawAudio_Chunks.getInstance());
  }

  public Resource_WAV_RawAudio_Chunks(File sourcePath, String name, long offset) {
    super(sourcePath, name, offset);
    setExporter(Exporter_Custom_WAV_RawAudio_Chunks.getInstance());
  }

  public Resource_WAV_RawAudio_Chunks(File sourcePath, String name, long offset, long length) {
    super(sourcePath, name, offset, length);
    setExporter(Exporter_Custom_WAV_RawAudio_Chunks.getInstance());
  }

  public Resource_WAV_RawAudio_Chunks(File sourcePath, String name, long offset, long length, long decompLength) {
    super(sourcePath, name, offset, length, decompLength);
    setExporter(Exporter_Custom_WAV_RawAudio_Chunks.getInstance());
  }

  // ORIGINAL CONSTRUCTORS

  public Resource_WAV_RawAudio_Chunks(File sourcePath, String name, long offset, long length, long decompLength, ExporterPlugin exporter) {
    super(sourcePath, name, offset, length, decompLength, exporter);
    setExporter(Exporter_Custom_WAV_RawAudio_Chunks.getInstance());
  }

  public Resource_WAV_RawAudio_Chunks(String name, long offset, long length) {
    super(name, offset, length);
    setExporter(Exporter_Custom_WAV_RawAudio_Chunks.getInstance());
  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  @Override
  public Object clone() {
    Resource_WAV_RawAudio_Chunks newRes = new Resource_WAV_RawAudio_Chunks(sourcePath, origName, offset, length, decompLength, exporter);

    // Important - sets the new and orig name!
    newRes.setName(name);

    newRes.setExportedPath(exportedPath);
    newRes.setReplaced(replaced);

    newRes.setAudioProperties(frequency, bitrate, channels);
    newRes.setOffsets(offsets);
    newRes.setLengths(lengths);

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
    //this.exporter = resource.getExporter(); // want to leave it forced to the WAV_RawAudio_Chunks exporter
    this.length = resource.getLength();
    this.offset = resource.getOffset();
    this.name = resource.getName();
    this.sourcePath = resource.getSource();

    //this.exportedPath = resource.getExportedPath();
    setExportedPath(resource.getExportedPath());

    this.origName = resource.getOriginalName();
    this.replaced = resource.isReplaced();

    if (resource instanceof Resource_WAV_RawAudio_Chunks) {
      Resource_WAV_RawAudio_Chunks castResource = (Resource_WAV_RawAudio_Chunks) resource;

      this.frequency = castResource.getFrequency();
      this.bitrate = castResource.getBitrate();
      this.channels = castResource.getChannels();
      this.offsets = castResource.getOffsets();
      this.lengths = castResource.getLengths();
    }
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

  public long[] getLengths() {
    return lengths;
  }

  /////
  //
  // METHODS
  //
  /////

  public long[] getOffsets() {
    return offsets;
  }

  public boolean isWriteHeader() {
    return writeHeader;
  }

  public void setAudioProperties(int frequency, short bitrate, short channels) {
    this.frequency = frequency;
    this.bitrate = bitrate;
    this.channels = channels;
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

  public void setLengths(long[] lengths) {
    this.lengths = lengths;
  }

  public void setOffsets(long[] offsets) {
    this.offsets = offsets;
  }

  public void setWriteHeader(boolean writeHeader) {
    this.writeHeader = writeHeader;
  }

}