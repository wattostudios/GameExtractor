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
import org.watto.datatype.Palette;
import org.watto.datatype.Resource;

public class Resource_DAT_34 extends Resource {

  Palette palette = null;
  short width = 0;
  short height = 0;

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public Resource_DAT_34() {
    super();
  }

  public Resource_DAT_34(File sourcePath, String name, long offset, long length, Palette palette, short width, short height) {
    super(sourcePath, name, offset, length);
    this.palette = palette;
    this.width = width;
    this.height = height;
  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  @Override
  public Object clone() {
    Resource_DAT_34 newRes = new Resource_DAT_34(sourcePath, origName, offset, length, palette, width, height);

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

    if (resource instanceof Resource_DAT_34) {
      this.palette = ((Resource_DAT_34) resource).getPalette();
      this.width = ((Resource_DAT_34) resource).getWidth();
      this.height = ((Resource_DAT_34) resource).getHeight();
    }
  }

  public short getHeight() {
    return height;
  }

  // ORIGINAL CONSTRUCTORS

  public Palette getPalette() {
    return palette;
  }

  /////
  //
  // METHODS
  //
  /////

  public short getWidth() {
    return width;
  }

  public void setHeight(short height) {
    this.height = height;
  }

  public void setPalette(Palette palette) {
    this.palette = palette;
  }

  public void setWidth(short width) {
    this.width = width;
  }

}