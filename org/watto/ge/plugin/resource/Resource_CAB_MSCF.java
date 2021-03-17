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

public class Resource_CAB_MSCF extends Resource {

  long blockOffset = 0;

  long blockDiscardBytes = 0;

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public Resource_CAB_MSCF() {
    super();
  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public Resource_CAB_MSCF(File sourcePath, String name, long blockOffset, long blockDiscardBytes, long decompLength, ExporterPlugin exporter) {
    //path,name,offset,length,decompLength,exporter
    super(sourcePath, name, blockOffset, decompLength, decompLength, exporter);

    this.blockOffset = blockOffset;
    this.blockDiscardBytes = blockDiscardBytes;
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
    Resource_CAB_MSCF newRes = new Resource_CAB_MSCF(sourcePath, origName, blockOffset, blockDiscardBytes, decompLength, exporter);

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

    //this.exportedPath = resource.getExportedPath();
    setExportedPath(resource.getExportedPath());

    this.origName = resource.getOriginalName();
    this.replaced = resource.isReplaced();

    if (resource instanceof Resource_CAB_MSCF) {
      this.blockOffset = ((Resource_CAB_MSCF) resource).getBlockOffset();
      this.blockDiscardBytes = ((Resource_CAB_MSCF) resource).getBlockDiscardBytes();
    }
  }

  public long getBlockDiscardBytes() {
    return blockDiscardBytes;
  }

  public long getBlockOffset() {
    return blockOffset;
  }

  public void setBlockDiscardBytes(long blockDiscardBytes) {
    this.blockDiscardBytes = blockDiscardBytes;
  }

  public void setBlockOffset(long blockOffset) {
    this.blockOffset = blockOffset;
  }

}