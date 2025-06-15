/*
 * Application:  Game Extractor
 * Author:       wattostudios
 * Website:      http://www.watto.org
 * Copyright:    Copyright (c) 2002-2025 wattostudios
 *
 * License Information:
 * This program is free software; you can redistribute it and/or modify it under the terms of the GNU General Public License
 * published by the Free Software Foundation; either version 2 of the License, or (at your option) any later versions. This
 * program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranties
 * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License at http://www.gnu.org for more
 * details. For further information on this application, refer to the authors' website.
 */

package org.watto.ge.plugin;

import java.io.File;

import org.watto.datatype.Resource;

public class _Resource_XXX extends Resource {

  int fileID = 0;

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public _Resource_XXX() {
    super();
  }

  // ORIGINAL CONSTRUCTORS

  public _Resource_XXX(File sourcePath) {
    super(sourcePath);
  }

  public _Resource_XXX(File sourcePath, long offset, long length) {
    super(sourcePath, offset, length);
  }

  public _Resource_XXX(File sourcePath, String name) {
    super(sourcePath, name);
  }

  public _Resource_XXX(File sourcePath, String name, long offset) {
    super(sourcePath, name, offset);
  }

  public _Resource_XXX(File sourcePath, String name, long offset, long length) {
    super(sourcePath, name, offset, length);
  }

  public _Resource_XXX(File sourcePath, String name, long offset, long length, long decompLength) {
    super(sourcePath, name, offset, length, decompLength);
  }

  public _Resource_XXX(File sourcePath, String name, long offset, long length, long decompLength, ExporterPlugin exporter) {
    super(sourcePath, name, offset, length, decompLength, exporter);
  }

  // WITH FILE IDs
  public _Resource_XXX(File sourcePath, String name, long offset, long length, long decompLength, ExporterPlugin exporter, int fileID) {
    super(sourcePath, name, offset, length, decompLength, exporter);
    setID(fileID);
  }

  public _Resource_XXX(String name, long offset, long length) {
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
    _Resource_XXX newRes = new _Resource_XXX(sourcePath, origName, offset, length, decompLength, exporter, fileID);

    // Important - sets the new and orig name!
    newRes.setName(name);

    newRes.setExportedPath(exportedPath);
    newRes.setReplaced(replaced);

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

    if (resource instanceof _Resource_XXX) {
      this.fileID = ((_Resource_XXX) resource).getID();
    }
  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public int getID() {
    return fileID;
  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public void setID(int fileID) {
    this.fileID = fileID;
  }

}