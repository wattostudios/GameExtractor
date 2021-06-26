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

public class Resource_Unity3D_TEX extends Resource {

  int imageWidth = 0;

  int imageHeight = 0;

  int formatCode = 0;

  int mipmapCount = 0;

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public Resource_Unity3D_TEX() {
    super();
  }

  // ORIGINAL CONSTRUCTORS

  public Resource_Unity3D_TEX(File sourcePath) {
    super(sourcePath);
  }

  public Resource_Unity3D_TEX(File sourcePath, long offset, long length) {
    super(sourcePath, offset, length);
  }

  public Resource_Unity3D_TEX(File sourcePath, String name) {
    super(sourcePath, name);
  }

  public Resource_Unity3D_TEX(File sourcePath, String name, long offset) {
    super(sourcePath, name, offset);
  }

  public Resource_Unity3D_TEX(File sourcePath, String name, long offset, long length) {
    super(sourcePath, name, offset, length);
  }

  public Resource_Unity3D_TEX(File sourcePath, String name, long offset, long length, long decompLength) {
    super(sourcePath, name, offset, length, decompLength);
  }

  public Resource_Unity3D_TEX(File sourcePath, String name, long offset, long length, long decompLength, ExporterPlugin exporter) {
    super(sourcePath, name, offset, length, decompLength, exporter);
  }

  public Resource_Unity3D_TEX(String name, long offset, long length) {
    super(name, offset, length);
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
    Resource_Unity3D_TEX newRes = new Resource_Unity3D_TEX(sourcePath, origName, offset, length, decompLength, exporter);

    // Important - sets the new and orig name!
    newRes.setName(name);

    newRes.setExportedPath(exportedPath);
    newRes.setReplaced(replaced);

    newRes.setImageWidth(imageWidth);
    newRes.setImageHeight(imageHeight);
    newRes.setFormatCode(formatCode);
    newRes.setMipmapCount(mipmapCount);

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

    if (resource instanceof Resource_Unity3D_TEX) {
      Resource_Unity3D_TEX castResource = (Resource_Unity3D_TEX) resource;

      this.imageWidth = castResource.getImageWidth();
      this.imageHeight = castResource.getImageHeight();
      this.formatCode = castResource.getFormatCode();
      this.mipmapCount = castResource.getMipmapCount();

    }
  }

  public int getFormatCode() {
    return formatCode;
  }

  public int getImageHeight() {
    return imageHeight;
  }

  public int getImageWidth() {
    return imageWidth;
  }

  public int getMipmapCount() {
    return mipmapCount;
  }

  public void setFormatCode(int formatCode) {
    this.formatCode = formatCode;
  }

  public void setImageHeight(int imageHeight) {
    this.imageHeight = imageHeight;
  }

  public void setImageWidth(int imageWidth) {
    this.imageWidth = imageWidth;
  }

  public void setMipmapCount(int mipmapCount) {
    this.mipmapCount = mipmapCount;
  }

}