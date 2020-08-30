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

public class ReplacableResource extends Resource {

  /**
   * when this file is replaced, these fields remember the original offset and length of the file
   **/
  protected long origLength = -1;

  protected long origOffset = -1;

  protected ReplaceDetails[] replaceDetails = new ReplaceDetails[0];

  // The offset to this file IN THE NEW ARCHIVE.
  protected long implicitReplacedOffset = 0;

  /**
   **********************************************************************************************
   * These resources can be replaced by plugins that allow Implicit File Replacing (ie opening
   * and archive and then rebuilding it automatically with the new files in it)
   **********************************************************************************************
   **/
  public ReplacableResource() {
    super();
  }

  // NEW CONSTRUCTORS

  /**
   **********************************************************************************************
   * Used when adding an external file to the archive
   **********************************************************************************************
   **/
  public ReplacableResource(File sourcePath) {
    super(sourcePath);
  }

  /**
   **********************************************************************************************
   * Used for archives that will set their name later
   **********************************************************************************************
   **/
  public ReplacableResource(File sourcePath, long offset, long length) {
    super(sourcePath, offset, length);
  }

  /**
   **********************************************************************************************
   * Used for archives that will set their name later
   **********************************************************************************************
   **/
  public ReplacableResource(File sourcePath, long offset, long offsetPointerLocation, long offsetPointerLength, long length, long lengthPointerLocation, long lengthPointerLength) {
    super(sourcePath, offset, length);
    addReplaceDetails(new ReplaceDetails("Offset", offsetPointerLocation, offsetPointerLength, offset),
        new ReplaceDetails("Length", lengthPointerLocation, lengthPointerLength, length),
        new ReplaceDetails("Decompressed", 0, 0, length));
  }

  /**
   **********************************************************************************************
   * Used when adding an external file to the archive
   **********************************************************************************************
   **/
  public ReplacableResource(File sourcePath, String name) {
    super(sourcePath, name);
  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public ReplacableResource(File sourcePath, String name, ExporterPlugin exporter, ReplaceDetails... replaceDetails) {
    super(sourcePath, name, 0, 0, 0, exporter);
    addReplaceDetails(replaceDetails);
  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public ReplacableResource(File sourcePath, String name, long offset) {
    super(sourcePath, name, offset);
  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public ReplacableResource(File sourcePath, String name, long offset, long length) {
    super(sourcePath, name, offset, length);
  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public ReplacableResource(File sourcePath, String name, long offset, long offsetPointerLocation, long offsetPointerLength) {
    super(sourcePath, name, offset);
    addReplaceDetails(new ReplaceDetails("Offset", offsetPointerLocation, offsetPointerLength, offset),
        new ReplaceDetails("Length", 0, 0, 0),
        new ReplaceDetails("Decompressed", 0, 0, 0));
  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public ReplacableResource(File sourcePath, String name, long offset, long length, long decompLength, ExporterPlugin exporter) {
    super(sourcePath, name, offset, length, decompLength, exporter);
  }

  // ORIGINAL CONSTRUCTORS

  /**
   **********************************************************************************************
   * For archives that don't store the offset data
   **********************************************************************************************
   **/
  public ReplacableResource(File sourcePath, String name, long offset, long length, long lengthPointerLocation, long lengthPointerLength) {
    super(sourcePath, name, offset, length);
    addReplaceDetails(new ReplaceDetails("Offset", 0, 0, offset),
        new ReplaceDetails("Length", lengthPointerLocation, lengthPointerLength, length),
        new ReplaceDetails("Decompressed", 0, 0, length));
  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public ReplacableResource(File sourcePath, String name, long offset, long offsetPointerLocation, long offsetPointerLength, long length, long lengthPointerLocation, long lengthPointerLength) {
    super(sourcePath, name, offset, length);
    addReplaceDetails(new ReplaceDetails("Offset", offsetPointerLocation, offsetPointerLength, offset),
        new ReplaceDetails("Length", lengthPointerLocation, lengthPointerLength, length),
        new ReplaceDetails("Decompressed", 0, 0, length));
  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public ReplacableResource(File sourcePath, String name, long offset, long offsetPointerLocation, long offsetPointerLength, long length, long lengthPointerLocation, long lengthPointerLength, long decompLength, long decompPointerLocation, long decompPointerLength) {
    super(sourcePath, name, offset, length, decompLength);
    addReplaceDetails(new ReplaceDetails("Offset", offsetPointerLocation, offsetPointerLength, offset),
        new ReplaceDetails("Length", lengthPointerLocation, lengthPointerLength, length),
        new ReplaceDetails("Decompressed", decompPointerLocation, decompPointerLength, decompLength));
  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public ReplacableResource(File sourcePath, String name, long offset, long offsetPointerLocation, long offsetPointerLength, long length, long lengthPointerLocation, long lengthPointerLength, long decompLength, long decompPointerLocation, long decompPointerLength, ExporterPlugin exporter) {
    super(sourcePath, name, offset, length, decompLength, exporter);
    addReplaceDetails(new ReplaceDetails("Offset", offsetPointerLocation, offsetPointerLength, offset),
        new ReplaceDetails("Length", lengthPointerLocation, lengthPointerLength, length),
        new ReplaceDetails("Decompressed", decompPointerLocation, decompPointerLength, decompLength));
  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public ReplacableResource(File sourcePath, String name, ReplaceDetails... replaceDetails) {
    super(sourcePath, name, 0, 0);
    addReplaceDetails(replaceDetails);
  }

  /**
   **********************************************************************************************
   * For the format scanner
   **********************************************************************************************
   **/
  public ReplacableResource(String name, long offset, long length) {
    super(name, offset, length);
  }

  /**
   **********************************************************************************************
   * For the format scanner
   **********************************************************************************************
   **/
  public ReplacableResource(String name, long offset, long offsetPointerLocation, long offsetPointerLength, long length, long lengthPointerLocation, long lengthPointerLength) {
    super(name, offset, length);
    addReplaceDetails(new ReplaceDetails("Offset", offsetPointerLocation, offsetPointerLength, offset),
        new ReplaceDetails("Length", lengthPointerLocation, lengthPointerLength, length),
        new ReplaceDetails("Decompressed", 0, 0, length));
  }

  ////// OVERWRITTEN IN RESOURCE TO POINT TO THE ReplaceDetails* OBJECTS

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public void addReplaceDetails(ReplaceDetails... details) {
    if (replaceDetails.length == 0) {
      // initialise the array
      replaceDetails = details;
      return;
    }
    else {
      // increase the size of the array, and copy the new details into it
      ReplaceDetails[] temp = replaceDetails;
      replaceDetails = new ReplaceDetails[temp.length + details.length];
      System.arraycopy(temp, 0, replaceDetails, 0, temp.length);
      System.arraycopy(details, 0, replaceDetails, temp.length, details.length);
    }

  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  @Override
  public Object clone() {

    // clone the ReplaceDetails
    int numDetails = replaceDetails.length;
    ReplaceDetails[] clonedDetails = new ReplaceDetails[numDetails];
    for (int i = 0; i < numDetails; i++) {
      clonedDetails[i] = (ReplaceDetails) replaceDetails[i].clone();
    }

    ReplacableResource newRes = new ReplacableResource(sourcePath, origName, exporter, clonedDetails);

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

  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public long getDecompPointerLength() {
    return getReplaceDetailsLength("Decompressed");
  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public long getDecompPointerLocation() {
    return getReplaceDetailsOffset("Decompressed");
  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  @Override
  public long getDecompressedLength() {
    long length = getReplaceDetailsValue("Decompressed");
    if (length == 0) {
      length = getReplaceDetailsValue("Length");
    }
    return length;
  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public ReplaceDetails getImplicitReplaceDecompressed() {
    ReplaceDetails details = getReplaceDetails("Decompressed");
    if (details == null) {
      return new ReplaceDetails("Decompressed", 0, 0, 0);
    }
    return details;
  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public long getImplicitReplacedOffset() {
    if (implicitReplacedOffset == 0) {
      return getOffset();
    }
    else {
      return implicitReplacedOffset;
    }
  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public long getImplicitReplacedOffset_clone() {
    return implicitReplacedOffset;
  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public ReplaceDetails[] getImplicitReplaceFields() {

    // add the file replace field, if it doesn't exist, or overwrite the old one.
    setReplaceDetails("File", getImplicitReplaceFile());

    return replaceDetails;
  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public ReplaceDetails getImplicitReplaceFile() {
    if (origOffset == -1 && origLength == -1) {
      return new ReplaceDetails_File("File", getOffset(), getLength(), this);
    }
    else {
      return new ReplaceDetails_File("File", origOffset, origLength, this);
    }
  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public ReplaceDetails getImplicitReplaceLength() {
    ReplaceDetails details = getReplaceDetails("Length");
    if (details == null) {
      return new ReplaceDetails("Length", 0, 0, 0);
    }
    return details;
  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public ReplaceDetails getImplicitReplaceOffset() {
    ReplaceDetails details = getReplaceDetails("Offset");
    if (details == null) {
      return new ReplaceDetails("Offset", 0, 0, 0);
    }
    return details;
  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  @Override
  public long getLength() {
    return getReplaceDetailsValue("Length");
  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public long getLengthPointerLength() {
    return getReplaceDetailsLength("Length");
  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public long getLengthPointerLocation() {
    return getReplaceDetailsOffset("Length");
  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  @Override
  public long getOffset() {
    return getReplaceDetailsValue("Offset");
  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public long getOffsetPointerLength() {
    return getReplaceDetailsLength("Offset");
  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public long getOffsetPointerLocation() {
    return getReplaceDetailsOffset("Offset");
  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public long getOriginalLength() {
    return origLength;
  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public long getOriginalOffset() {
    return origOffset;
  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public ReplaceDetails getReplaceDecomp() {
    return getReplaceDetails("Decompressed");
  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public ReplaceDetails[] getReplaceDetails() {
    return replaceDetails;
  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public ReplaceDetails getReplaceDetails(String name) {
    for (int i = 0; i < replaceDetails.length; i++) {
      if (replaceDetails[i].getName().equals(name)) {
        return replaceDetails[i];
      }
    }
    return null;
  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public long getReplaceDetailsLength(String name) {
    ReplaceDetails details = getReplaceDetails(name);
    if (details == null) {
      return 0;
    }
    return details.getLength();
  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public long getReplaceDetailsOffset(String name) {
    ReplaceDetails details = getReplaceDetails(name);
    if (details == null) {
      return 0;
    }
    return details.getOffset();
  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public long getReplaceDetailsValue(String name) {
    ReplaceDetails details = getReplaceDetails(name);
    if (details == null) {
      return 0;
    }
    return details.getValue();
  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public ReplaceDetails getReplaceLength() {
    return getReplaceDetails("Length");
  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public ReplaceDetails getReplaceOffset() {
    return getReplaceDetails("Offset");
  }

  /**
   **********************************************************************************************
   * Determines whether the file has been replaced
   **********************************************************************************************
   **/
  @Override
  public boolean isReplaced() {
    return (origLength != -1 && origOffset != -1);
  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  @Override
  public void replace(File file, String directory) {
    if (origOffset == -1) {
      origOffset = offset;
    }

    if (origLength == -1) {
      origLength = length;
    }

    long fileLength = file.length();

    sourcePath = file;
    //name = directory + file.getName();
    setOffset(0);
    //getReplaceDetails("Offset").setValue(0); // IMPORTANT, otherwise implicit replace will have the wrong value here - we want to retain where the real offset is stored in the archive!

    setLength(fileLength);
    setDecompressedLength(fileLength);
    exporter = Exporter_Default.getInstance();

    replaced = true;
  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  @Override
  public void setDecompressedLength(long decompLength) {
    setReplaceDetailsValue("Decompressed", decompLength);
  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public void setDecompressedLength(ReplaceDetails replaceDecomp) {
    setReplaceDetails("Decomprssed", replaceDecomp);
  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public void setImplicitReplacedOffset(long implicitReplacedOffset) {
    this.implicitReplacedOffset = implicitReplacedOffset;
    //setReplaceDetailsValue("Offset", implicitReplacedOffset);
  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  @Override
  public void setLength(long length) {
    setReplaceDetailsValue("Length", length);
  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public void setLength(ReplaceDetails replaceLength) {
    setReplaceDetails("Length", replaceLength);
  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  @Override
  public void setOffset(long offset) {
    setReplaceDetailsValue("Offset", offset);
  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public void setOffset(ReplaceDetails replaceOffset) {
    setReplaceDetails("Offset", replaceOffset);
  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public void setReplaceDetails(ReplaceDetails details) {
    setReplaceDetails(details.getName(), details);
  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public void setReplaceDetails(ReplaceDetails[] details) {
    this.replaceDetails = details;
  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public void setReplaceDetails(String name, ReplaceDetails details) {
    for (int i = 0; i < replaceDetails.length; i++) {
      if (replaceDetails[i].getName().equals(name)) {
        replaceDetails[i] = details;
        return;
      }
    }
    addReplaceDetails(details);
  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public void setReplaceDetailsValue(String name, long value) {
    ReplaceDetails details = getReplaceDetails(name);
    if (details == null) {
      addReplaceDetails(new ReplaceDetails(name, 0, 4, value));
    }
    else {
      details.setValue(value);
    }
  }

}
