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

package org.watto.datatype;

import java.io.File;
import org.watto.ge.plugin.ExporterPlugin;
import org.watto.ge.plugin.exporter.Exporter_Default;
import org.watto.ge.plugin.exporter.Exporter_SplitChunkDefault;

public class SplitChunkResource extends Resource {

  long[] decompLengths = new long[] { -1 };
  long[] lengths = new long[] { 0 };
  long[] offsets = new long[] { 0 };

  /**
  **********************************************************************************************

  **********************************************************************************************
  **/
  public SplitChunkResource() {
    exporter = Exporter_SplitChunkDefault.getInstance();
  }

  /**
  **********************************************************************************************
  Used when adding an external file to the archive
  **********************************************************************************************
  **/
  public SplitChunkResource(File sourcePath) {
    super(sourcePath);

    this.length = sourcePath.length();

    this.offsets = new long[] { 0 };
    this.lengths = new long[] { length };
    this.decompLengths = new long[] { length };

    exporter = Exporter_SplitChunkDefault.getInstance();
  }

  /**
  **********************************************************************************************
  Used for archives that will set their name later
  **********************************************************************************************
  **/
  public SplitChunkResource(File sourcePath, long offset, long length) {
    super(sourcePath, offset, length);

    this.offsets = new long[] { offset };
    this.lengths = new long[] { length };
    this.decompLengths = new long[] { length };

    exporter = Exporter_SplitChunkDefault.getInstance();
  }

  /**
  **********************************************************************************************
  Used for archives that will set their name later
  **********************************************************************************************
  **/
  public SplitChunkResource(File sourcePath, long[] offsets, long[] lengths) {
    super(sourcePath, offsets[0], lengths[0]);

    this.offsets = offsets;
    this.lengths = lengths;
    this.decompLengths = lengths;

    exporter = Exporter_SplitChunkDefault.getInstance();
  }

  /**
  **********************************************************************************************
  Used when adding an external file to the archive
  **********************************************************************************************
  **/
  public SplitChunkResource(File sourcePath, String name) {
    super(sourcePath);

    this.length = sourcePath.length();

    this.offsets = new long[] { 0 };
    this.lengths = new long[] { length };
    this.decompLengths = new long[] { length };

    exporter = Exporter_SplitChunkDefault.getInstance();
  }

  /**
  **********************************************************************************************

  **********************************************************************************************
  **/
  public SplitChunkResource(File sourcePath, String name, long offset, long length) {
    super(sourcePath, name, offset, length);

    this.offsets = new long[] { offset };
    this.lengths = new long[] { length };
    this.decompLengths = new long[] { length };

    exporter = Exporter_SplitChunkDefault.getInstance();
  }

  /**
  **********************************************************************************************

  **********************************************************************************************
  **/
  public SplitChunkResource(File sourcePath, String name, long offset, long length, long decompLength) {
    super(sourcePath, name, offset, length, decompLength);

    this.offsets = new long[] { offset };
    this.lengths = new long[] { length };
    this.decompLengths = new long[] { decompLength };

    exporter = Exporter_SplitChunkDefault.getInstance();
  }

  /**
  **********************************************************************************************

  **********************************************************************************************
  **/
  public SplitChunkResource(File sourcePath, String name, long offset, long length, long decompLength, ExporterPlugin exporter) {
    super(sourcePath, name, offset, length, decompLength, exporter);

    this.offsets = new long[] { offset };
    this.lengths = new long[] { length };
    this.decompLengths = new long[] { decompLength };

    exporter = Exporter_SplitChunkDefault.getInstance();
  }

  /**
  **********************************************************************************************

  **********************************************************************************************
  **/
  public SplitChunkResource(File sourcePath, String name, long[] offsets, long[] lengths) {
    super(sourcePath, name, offsets[0], lengths[0]);

    this.offsets = offsets;
    this.lengths = lengths;
    this.decompLengths = lengths;

    exporter = Exporter_SplitChunkDefault.getInstance();
  }

  /**
  **********************************************************************************************

  **********************************************************************************************
  **/
  public SplitChunkResource(File sourcePath, String name, long[] offsets, long[] lengths, long[] decompLengths) {
    super(sourcePath, name, offsets[0], lengths[0], decompLengths[0]);

    this.offsets = offsets;
    this.lengths = lengths;
    this.decompLengths = decompLengths;

    exporter = Exporter_SplitChunkDefault.getInstance();
  }

  /**
  **********************************************************************************************

  **********************************************************************************************
  **/
  public SplitChunkResource(File sourcePath, String name, long[] offsets, long[] lengths, long[] decompLengths, ExporterPlugin exporter) {
    super(sourcePath, name, offsets[0], lengths[0], decompLengths[0], exporter);

    this.offsets = offsets;
    this.lengths = lengths;
    this.decompLengths = decompLengths;

    exporter = Exporter_SplitChunkDefault.getInstance();
  }

  /**
  **********************************************************************************************
  For the format scanner
  **********************************************************************************************
  **/
  public SplitChunkResource(String name, long offset, long length) {
    super(name, offset, length);

    this.offsets = new long[] { offset };
    this.lengths = new long[] { length };
    this.decompLengths = new long[] { length };

    exporter = Exporter_SplitChunkDefault.getInstance();
  }

  /**
  **********************************************************************************************
  For the format scanner
  **********************************************************************************************
  **/
  public SplitChunkResource(String name, long[] offsets, long[] lengths) {
    super(name, offsets[0], lengths[0]);

    this.offsets = offsets;
    this.lengths = lengths;
    this.decompLengths = lengths;

    exporter = Exporter_SplitChunkDefault.getInstance();
  }

  /**
  **********************************************************************************************

  **********************************************************************************************
  **/
  @Override
  public Object clone() {
    SplitChunkResource newRes = new SplitChunkResource(sourcePath, origName, offsets, lengths, decompLengths, exporter);

    // Important - sets the new and orig name!
    newRes.setName(name);

    newRes.setExportedPath(exportedPath);
    newRes.setReplaced(replaced);

    return newRes;
  }

  /**
  **********************************************************************************************
  Copies all the values from <i>resource</i> into this resource (ie does a replace without
  affecting pointers)
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
    this.exportedPath = resource.getExportedPath();
    this.origName = resource.getOriginalName();
    this.replaced = resource.isReplaced();

    if (resource instanceof SplitChunkResource) {
      SplitChunkResource split = (SplitChunkResource) resource;

      this.decompLengths = split.getDecompressedLengths();
      this.lengths = split.getLengths();
      this.offsets = split.getOffsets();
    }
  }

  /**
  **********************************************************************************************

  **********************************************************************************************
  **/
  @Override
  public long getDecompressedLength() {
    /*
    long decompLength = super.getDecompressedLength();
    if (decompLength > 0) {
      return decompLength;
    }
    decompLength = 0;
    */
    long decompLength = 0;
    for (int i = 0; i < decompLengths.length; i++) {
      decompLength += decompLengths[i];
    }
    return decompLength;
  }

  /**
  **********************************************************************************************

  **********************************************************************************************
  **/
  public long[] getDecompressedLengths() {
    return decompLengths;
  }

  /**
  **********************************************************************************************

  **********************************************************************************************
  **/
  @Override
  public long getLength() {
    /*
    long length = super.getLength();
    if (length > 0) {
      return length;
    }
    length = 0;
    */
    long length = 0;
    for (int i = 0; i < lengths.length; i++) {
      length += lengths[i];
    }
    return length;
  }

  /**
  **********************************************************************************************

  **********************************************************************************************
  **/
  public long[] getLengths() {
    return lengths;
  }

  /**
  **********************************************************************************************

  **********************************************************************************************
  **/
  @Override
  public long getOffset() {
    return offsets[0];
  }

  /**
  **********************************************************************************************

  **********************************************************************************************
  **/
  public long[] getOffsets() {
    return offsets;
  }

  /**
  **********************************************************************************************

  **********************************************************************************************
  **/
  @Override
  public boolean isCompressed() {
    return (getLength() != getDecompressedLength());
  }

  /**
  **********************************************************************************************

  **********************************************************************************************
  **/
  @Override
  public void replace(File file, String directory) {
    //if (!ArchiveModificationMonitor.setModified(true)){
    //  return;
    //  }

    sourcePath = file;
    //id = -1;
    //name = directory + file.getName();
    offset = 0;
    offsets = new long[] { offset };

    length = file.length();
    lengths = new long[] { length };

    decompLength = length;
    decompLengths = new long[] { length };

    exporter = Exporter_Default.getInstance();
  }

  /**
  **********************************************************************************************

  **********************************************************************************************
  **/
  @Override
  public void setDecompressedLength(long decompLength) {
    this.decompLength = decompLength;
    this.decompLengths = new long[] { decompLength };
  }

  /**
  **********************************************************************************************

  **********************************************************************************************
  **/
  public void setDecompressedLengths(long[] decompLengths) {
    this.decompLength = decompLengths[0];
    this.decompLengths = decompLengths;
  }

  /**
  **********************************************************************************************

  **********************************************************************************************
  **/
  @Override
  public void setLength(long length) {
    this.length = length;
    this.lengths = new long[] { length };
  }

  /**
  **********************************************************************************************

  **********************************************************************************************
  **/
  public void setLengths(long[] lengths) {
    this.length = lengths[0];
    this.lengths = lengths;
  }

  /**
  **********************************************************************************************

  **********************************************************************************************
  **/
  @Override
  public void setOffset(long offset) {
    this.offset = offset;
    this.offsets = new long[] { offset };
  }

  /**
  **********************************************************************************************

  **********************************************************************************************
  **/
  public void setOffsets(long[] offsets) {
    this.offset = offsets[0];
    this.offsets = offsets;
  }

}