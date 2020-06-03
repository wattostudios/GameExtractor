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
import org.watto.ge.plugin.exporter.Exporter_Custom_VPK;

public class Resource_VPK extends Resource {

  long preloadDataOffset = 0;
  int preloadDataLength = 0;
  File preloadDataSource = null;

  /**
  **********************************************************************************************

  **********************************************************************************************
  **/
  public Resource_VPK() {
    super();
    setExporter(Exporter_Custom_VPK.getInstance());
  }

  // ORIGINAL CONSTRUCTORS

  public Resource_VPK(File sourcePath) {
    super(sourcePath);
    setExporter(Exporter_Custom_VPK.getInstance());
  }

  public Resource_VPK(File sourcePath, long offset, long length) {
    super(sourcePath, offset, length);
    setExporter(Exporter_Custom_VPK.getInstance());
  }

  public Resource_VPK(File sourcePath, String name) {
    super(sourcePath, name);
    setExporter(Exporter_Custom_VPK.getInstance());
  }

  public Resource_VPK(File sourcePath, String name, long offset) {
    super(sourcePath, name, offset);
    setExporter(Exporter_Custom_VPK.getInstance());
  }

  public Resource_VPK(File sourcePath, String name, long offset, long length) {
    super(sourcePath, name, offset, length);
    setExporter(Exporter_Custom_VPK.getInstance());
  }

  public Resource_VPK(File sourcePath, String name, long offset, long length, long decompLength) {
    super(sourcePath, name, offset, length, decompLength);
    setExporter(Exporter_Custom_VPK.getInstance());
  }

  public Resource_VPK(File sourcePath, String name, long offset, long length, long decompLength, ExporterPlugin exporter) {
    super(sourcePath, name, offset, length, decompLength, exporter);
    setExporter(Exporter_Custom_VPK.getInstance());
  }

  public Resource_VPK(String name, long offset, long length) {
    super(name, offset, length);
    setExporter(Exporter_Custom_VPK.getInstance());
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
    Resource_VPK newRes = new Resource_VPK(sourcePath, origName, offset, length, decompLength, exporter);

    // Important - sets the new and orig name!
    newRes.setName(name);

    newRes.setExportedPath(exportedPath);
    newRes.setReplaced(replaced);

    newRes.setPreloadData(preloadDataSource, preloadDataOffset, preloadDataLength);

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
    this.exportedPath = resource.getExportedPath();
    this.origName = resource.getOriginalName();
    this.replaced = resource.isReplaced();

    if (resource instanceof Resource_VPK) {
      Resource_VPK castResource = (Resource_VPK) resource;

      this.preloadDataOffset = castResource.getPreloadDataOffset();
      this.preloadDataLength = castResource.getPreloadDataLength();
      this.preloadDataSource = castResource.getPreloadDataSource();

    }
  }

  /**
   **********************************************************************************************
   * Overwritten to get the true length of the file, including the preload length
   **********************************************************************************************
   **/
  @Override
  public long getLength() {
    return super.getLength() + preloadDataLength;
  }

  public long getMainLength() {
    return super.getLength();
  }

  public long getMainOffset() {
    return super.getOffset();
  }

  public int getPreloadDataLength() {
    return preloadDataLength;
  }

  public long getPreloadDataOffset() {
    return preloadDataOffset;
  }

  public File getPreloadDataSource() {
    return preloadDataSource;
  }

  public void setPreloadData(File preloadDataSource, long preloadDataOffset, int preloadDataLength) {
    this.preloadDataSource = preloadDataSource;
    this.preloadDataOffset = preloadDataOffset;
    this.preloadDataLength = preloadDataLength;
  }

  public void setPreloadDataLength(int preloadDataLength) {
    this.preloadDataLength = preloadDataLength;
  }

  public void setPreloadDataOffset(long preloadDataOffset) {
    this.preloadDataOffset = preloadDataOffset;
  }

  public void setPreloadDataSource(File preloadDataSource) {
    this.preloadDataSource = preloadDataSource;
  }

}