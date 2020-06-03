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
import org.watto.datatype.ReplacableResource;
import org.watto.datatype.ReplaceDetails;
import org.watto.datatype.Resource;
import org.watto.ge.plugin.ExporterPlugin;

public class ReplacableResource_FileID extends ReplacableResource {

  long fileID = -1;

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public ReplacableResource_FileID() {
    super();
  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public ReplacableResource_FileID(File sourcePath, long fileID, String name, long offset, long offsetPointerLocation, long offsetPointerLength, long length, long lengthPointerLocation, long lengthPointerLength, long decompLength, long decompPointerLocation, long decompPointerLength, ExporterPlugin exporter) {
    super(sourcePath, name, offset, offsetPointerLocation, offsetPointerLength, length, lengthPointerLocation, lengthPointerLength, decompLength, decompPointerLocation, decompPointerLength, exporter);
    setID(fileID);
  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public ReplacableResource_FileID(File sourcePath, long fileID, String name, ReplaceDetails[] replaceDetails, ExporterPlugin exporter) {
    super(sourcePath, name, replaceDetails);
    setExporter(exporter);
    setID(fileID);
  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  //public ReplacableResource_FileID(File sourcePath, long fileID, String name, ReplaceDetails_Offset replaceOffset, ReplaceDetails_Length replaceLength, ReplaceDetails_Decompressed replaceDecomp, ExporterPlugin exporter) {
  //   super(sourcePath, name, replaceOffset, replaceLength, replaceDecomp, exporter);
  //   setID(fileID);
  // }

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
    ReplacableResource_FileID newRes = new ReplacableResource_FileID(sourcePath, fileID, origName, replaceDetails, exporter);

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

    if (resource instanceof ReplacableResource) {
      ReplacableResource replacable = (ReplacableResource) resource;

      this.origLength = replacable.getOriginalLength();
      this.origOffset = replacable.getOriginalOffset();
      this.replaceDetails = replacable.getReplaceDetails();
      this.implicitReplacedOffset = replacable.getImplicitReplacedOffset_clone();
    }

    if (resource instanceof ReplacableResource_FileID) {
      this.fileID = ((ReplacableResource_FileID) resource).getID();
    }
  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public long getID() {
    return fileID;
  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public void setID(long fileID) {
    this.fileID = fileID;
  }

}