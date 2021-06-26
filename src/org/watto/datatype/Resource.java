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

import java.awt.Image;
import java.io.File;
import java.io.OutputStream;
import javax.swing.Icon;
import org.watto.ErrorLogger;
import org.watto.ge.plugin.ArchivePlugin;
import org.watto.ge.plugin.ExporterPlugin;
import org.watto.ge.plugin.exporter.Exporter_Default;
import org.watto.ge.plugin.resource.Resource_Property;
import org.watto.io.FileManipulator;
import org.watto.io.FilenameChecker;
import org.watto.io.FilenameSplitter;

public class Resource implements Comparable<Resource> {

  public long getExportedPathTimestamp() {
    return exportedPathTimestamp;
  }

  public void setExportedPathTimestamp(long exportedPathTimestamp) {
    this.exportedPathTimestamp = exportedPathTimestamp;
  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public boolean exportedPathTimestampChanged() {
    if (exportedPath == null || exportedPathTimestamp == -1 || !exportedPath.exists()) {
      return false;
    }
    if (exportedPathTimestamp == exportedPath.lastModified()) {
      return false;
    }
    return true;
  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public static String generateFilename(int fileNum) {
    fileNum++;

    if (fileNum < 10) {
      return "Unnamed File 00000" + fileNum;
    }
    else if (fileNum < 100) {
      return "Unnamed File 0000" + fileNum;
    }
    else if (fileNum < 1000) {
      return "Unnamed File 000" + fileNum;
    }
    else if (fileNum < 10000) {
      return "Unnamed File 00" + fileNum;
    }
    else if (fileNum < 100000) {
      return "Unnamed File 0" + fileNum;
    }
    else {
      return "Unnamed File " + fileNum;
    }

  }

  protected long decompLength = -1;

  protected ExporterPlugin exporter;

  protected long length = 0;

  protected long offset = 0;

  protected String name = "";

  protected File sourcePath;

  // The path after a resource has been exported
  protected File exportedPath = null;

  // The timestamp of the exported file, so that we know when the file has been changed by another program
  protected long exportedPathTimestamp = -1;

  // The original filename of this resource, before any renaming.
  // This is important for ZIP archives (they get entries by name).
  protected String origName = null;

  // only used for the icons - not anywhere else
  protected boolean replaced = false;

  // only used for the icons - not anywhere else. Allows you to force the display to say that this file wasn't added,
  // which is useful for occurrences where the files are split between multiple archive files
  protected boolean forceNotAdded = false;

  /** Used for generating thumbnails of the image file, if applicable **/
  ImageResource imageResource = null;

  /** Any additional properties for the Resource - helpful mostly for writing this resource **/
  Resource_Property[] properties = null;

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public Resource() {
    exporter = Exporter_Default.getInstance();
  }

  /**
   **********************************************************************************************
   * Used when adding an external file to the archive
   **********************************************************************************************
   **/
  public Resource(File sourcePath) {
    this(sourcePath, sourcePath.getName(), 0, sourcePath.length(), sourcePath.length(), Exporter_Default.getInstance());
    this.origName = name;

    //this.exportedPath = sourcePath;
    setExportedPath(sourcePath);
  }

  /**
   **********************************************************************************************
   * Used for archives that will set their name later
   **********************************************************************************************
   **/
  public Resource(File sourcePath, long offset, long length) {
    this(sourcePath, "", offset, length, length, Exporter_Default.getInstance());
    this.origName = null;
  }

  /**
   **********************************************************************************************
   * Used when adding an external file to the archive
   **********************************************************************************************
   **/
  public Resource(File sourcePath, String name) {
    this(sourcePath, name, 0, sourcePath.length(), sourcePath.length(), Exporter_Default.getInstance());
    this.origName = name;

    //this.exportedPath = sourcePath;
    setExportedPath(sourcePath);
  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public Resource(File sourcePath, String name, long offset) {
    this(sourcePath, name, offset, 0, 0, Exporter_Default.getInstance());
  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public Resource(File sourcePath, String name, long offset, long length) {
    this(sourcePath, name, offset, length, length, Exporter_Default.getInstance());
  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public Resource(File sourcePath, String name, long offset, long length, long decompLength) {
    this(sourcePath, name, offset, length, decompLength, Exporter_Default.getInstance());
  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public Resource(File sourcePath, String name, long offset, long length, long decompLength, ExporterPlugin exporter) {
    this.sourcePath = sourcePath;
    this.name = name;
    this.offset = offset;
    this.length = length;
    this.decompLength = decompLength;
    this.exporter = exporter;

    this.origName = name;
  }

  /**
   **********************************************************************************************
   * For the format scanner
   **********************************************************************************************
   **/
  public Resource(String name, long offset, long length) {
    this(null, name, offset, length, length, Exporter_Default.getInstance());
  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public void addProperty(String code, int value) {
    addProperty(code, "" + value);
  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public void addProperty(String code, boolean value) {
    addProperty(code, "" + value);
  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public void addProperty(String code, long value) {
    addProperty(code, "" + value);
  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public void addProperty(String code, short value) {
    addProperty(code, "" + value);
  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public void addProperty(String code, String value) {
    Resource_Property property = new Resource_Property(code, value);

    if (properties == null) {
      // add property to new array
      properties = new Resource_Property[] { property };
      return;
    }

    int numProperties = properties.length;

    // expand array then add property
    Resource_Property[] temp = properties;
    properties = new Resource_Property[numProperties + 1];
    System.arraycopy(temp, 0, properties, 0, numProperties);
    properties[numProperties] = property;
  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  @Override
  public Object clone() {
    Resource newRes = new Resource(sourcePath, origName, offset, length, decompLength, exporter);

    // Important - sets the new and orig name!
    newRes.setName(name);

    newRes.setExportedPath(exportedPath);
    newRes.setReplaced(replaced);

    newRes.setProperties(properties);

    return newRes;
  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  @Override
  public int compareTo(Resource otherResource) {
    return name.compareTo(otherResource.getName());
  }

  /**
   **********************************************************************************************
   * Copies all the values from <i>resource</i> into this resource (ie does a replace without
   * affecting pointers)
   **********************************************************************************************
   **/
  public void copyFrom(Resource resource) {
    this.decompLength = resource.getDecompressedLength();
    this.exporter = resource.getExporter();
    this.length = resource.getLength();
    this.offset = resource.getOffset();
    this.name = resource.getName();
    this.sourcePath = resource.getSource();

    setExportedPath(resource.getExportedPath());
    //this.exportedPath = resource.getExportedPath();

    this.origName = resource.getOriginalName();
    this.replaced = resource.isReplaced();
    this.properties = resource.getProperties();
  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public File extract(File destination) {
    try {
      if (destination.isDirectory()) {
        destination = new File(destination.getAbsolutePath() + File.separator + name);
      }

      destination = FilenameChecker.correctFilename(destination, '_');

      if (destination.exists() && destination.isFile()) {
        // to cater for archives with multiple files of the same name, append a number to the end of the name
        String path = FilenameSplitter.getDirectory(destination) + File.separator + FilenameSplitter.getFilename(destination);
        String extension = "." + FilenameSplitter.getExtension(destination);

        for (int i = 1; i < 1000; i++) {
          File testDestination = new File(path + i + extension);
          if (!testDestination.exists()) {
            destination = testDestination;
            break;
          }
        }
      }

      FileManipulator fm = new FileManipulator(destination, true);
      destination = fm.getFile();
      extract(fm);
      fm.close();

      //exportedPath = destination;
      setExportedPath(destination);

      return destination;
    }
    catch (Throwable t) {
      logError(t);
      return destination;
    }
  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public void extract(FileManipulator fm) {
    exporter.extract(this, fm);
  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public void extract(OutputStream outStream) {
    exporter.extract(this, outStream);
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
  public void forceNotAdded(boolean forceNotAdded) {
    this.forceNotAdded = forceNotAdded;
  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public Icon getAddedIcon() {
    return Archive.getAddedIcon(isAdded());
  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public long getDecompressedLength() {
    return decompLength;
  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public String getDirectory() {
    if (name == null) {
      return "";
    }
    int lastBack = name.lastIndexOf("\\");
    int lastFront = name.lastIndexOf("/");

    if (lastBack == -1 && lastFront == -1) {
      return "";
    }
    else if (lastBack > lastFront) {
      return name.substring(0, lastBack);
    }
    else {
      return name.substring(0, lastFront);
    }

  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public File getExportedPath() {
    return exportedPath;
  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public ExporterPlugin getExporter() {
    return exporter;
  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public String getExtension() {
    if (name == null) {
      return "";
    }
    int lastDot = name.lastIndexOf(".");

    if (lastDot == -1) {
      return "";
    }
    else {
      return name.substring(lastDot + 1);
    }
  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public String getFilename() {
    if (name == null) {
      return "";
    }
    int lastBack = name.lastIndexOf("\\");
    int lastFront = name.lastIndexOf("/");
    int lastDot = name.lastIndexOf(".");

    if (lastBack == -1 && lastFront == -1 && lastDot == -1) {
      return name;
    }
    else if (lastBack > lastFront) {
      if (lastDot < lastBack) {
        return name.substring(lastBack + 1);
      }
      else {
        return name.substring(lastBack + 1, lastDot);
      }
    }
    else {
      if (lastDot < lastFront) {
        return name.substring(lastFront + 1);
      }
      else {
        return name.substring(lastFront + 1, lastDot);
      }
    }
  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public String getFilenameWithExtension() {
    if (name == null) {
      return "";
    }
    int lastBack = name.lastIndexOf("\\");
    int lastFront = name.lastIndexOf("/");

    if (lastBack == -1 && lastFront == -1) {
      return name;
    }
    else if (lastBack > lastFront) {
      return name.substring(lastBack + 1);
    }
    else {
      return name.substring(lastFront + 1);
    }
  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public Icon getIcon() {
    return Archive.getIcon(name);
  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public ImageResource getImageResource() {
    return imageResource;
  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public long getLength() {
    return length;
  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public String getName() {
    return name;
  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public int getNameLength() {
    return name.length();
  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public long getOffset() {
    return offset;
  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public String getOriginalName() {
    return origName;
  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public Resource_Property[] getProperties() {
    return properties;
  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public String getProperty(String code) {
    if (properties == null) {
      return "";
    }

    int numProperties = properties.length;

    for (int i = 0; i < numProperties; i++) {
      if (properties[i].getCode().equals(code)) {
        // found
        return properties[i].getValue();
      }
    }

    return "";
  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public Icon getRenamedIcon() {
    return Archive.getRenamedIcon(isRenamed());
  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public Icon getReplacedIcon() {
    return Archive.getReplacedIcon(isReplaced());
  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public File getSource() {
    return sourcePath;
  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public Image getThumbnail() {
    if (imageResource == null) {
      imageResource = new BlankImageResource(this);
    }
    return imageResource.getThumbnail();
  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public boolean isAdded() {
    if (forceNotAdded) {
      return false;
    }

    if (replaced) {
      return false;
    }

    File path = Archive.getBasePath();
    if (path == null) {
      return false;
    }

    return (!(sourcePath.getAbsolutePath().equals(path.getAbsolutePath())));
  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public boolean isCompressed() {
    return (length != decompLength);
  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public boolean isRenamed() {
    return (!(name.equals(origName)));
  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public boolean isReplaced() {
    /*
     * File path = Archive.getBasePath(); if (path == null){ return false; }
     *
     * return (! (sourcePath.getAbsolutePath().equals(path.getAbsolutePath())));
     */
    return replaced;
  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public void logError(Throwable t) {
    ErrorLogger.log(t);
  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public void rename(String name) {
    this.name = name;
  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public void replace(File file) {
    replace(file, "");
  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public void replace(File file, String directory) {

    //if (!ArchiveModificationMonitor.setModified(true)){
    //  return;
    //  }

    ArchivePlugin readPlugin = Archive.getReadPlugin();
    if (readPlugin.canConvertOnReplace()) {
      file = readPlugin.convertOnReplace(this, file);
    }

    sourcePath = file;
    //name = directory + file.getName();
    offset = 0;
    length = file.length();
    decompLength = length;
    exporter = Exporter_Default.getInstance();

    replaced = true;

    //this.exportedPath = file; // so previews get updated
    setExportedPath(file);

    setImageResource(null); // so thumbnails get updated
  }

  /**
  **********************************************************************************************
  Update the timestamps, file sizes, etc from the exported file
  **********************************************************************************************
  **/
  public void updatePropertiesFromExportFile() {

    offset = 0;
    length = exportedPath.length();
    decompLength = length;
    exporter = Exporter_Default.getInstance();
    replaced = true;

    setExportedPath(exportedPath); // also updates the timestamp

    setImageResource(null); // so thumbnails get updated
  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public void setDecompressedLength(long decompLength) {
    this.decompLength = decompLength;
  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public void setDirectory(String directory) {

    boolean needsSlash = false;
    if (directory.length() >= 1) {
      char dirSlash = directory.charAt(directory.length() - 1);
      if (dirSlash != '\\' && dirSlash != '/') {
        needsSlash = true;
      }
    }

    int lastBack = name.lastIndexOf("\\");
    int lastFront = name.lastIndexOf("/");

    if (lastBack == -1 && lastFront == -1) {
      if (needsSlash) {
        directory += File.separator;
      }
      name = directory + name;
    }
    else if (lastBack > lastFront) {
      if (needsSlash) {
        directory += "\\";
      }
      name = directory + name.substring(lastBack + 1);
    }
    else {
      if (needsSlash) {
        directory += "/";
      }
      name = directory + name.substring(lastFront + 1);
    }

  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public void setExportedPath(File exportedPath) {
    this.exportedPath = exportedPath;
    if (exportedPath != null && exportedPath.exists()) {
      this.exportedPathTimestamp = exportedPath.lastModified();
    }
    else {
      this.exportedPathTimestamp = -1;
    }
  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public void setExporter(ExporterPlugin exporter) {
    this.exporter = exporter;
  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public void setExtension(String extension) {
    if (extension.length() >= 1) {
      char extDot = extension.charAt(0);
      if (extDot != '.') {
        extension = "." + extension;
      }
    }

    int lastDot = name.lastIndexOf(".");

    if (extension.equals("")) {
      if (lastDot == -1) {
        // No change required
      }
      else {
        name = name.substring(0, lastDot);
      }
    }

    else {
      if (lastDot == -1) {
        name += extension;
      }
      else {
        name = name.substring(0, lastDot) + extension;
      }
    }
  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public void setFilename(String filename) {
    int lastBack = name.lastIndexOf("\\");
    int lastFront = name.lastIndexOf("/");
    int lastDot = name.lastIndexOf(".");

    if (lastBack == -1 && lastFront == -1 && lastDot == -1) {
      name = filename;
    }
    else if (lastBack > lastFront) {
      if (lastDot < lastBack) {
        name = name.substring(0, lastBack + 1) + filename;
      }
      else {
        name = name.substring(0, lastBack + 1) + filename + name.substring(lastDot);
      }
    }
    else {
      if (lastDot < lastFront) {
        name = name.substring(0, lastFront + 1) + filename;
      }
      else {
        name = name.substring(0, lastFront + 1) + filename + name.substring(lastDot);
      }
    }
  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public void setImageResource(ImageResource imageResource) {
    this.imageResource = imageResource;
  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public void setLength(long length) {
    this.length = length;
  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public void setName(String name) {
    if (origName == null) {
      //origName = name;
      origName = this.name; // check this!
    }
    this.name = name;
  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public void setOffset(long offset) {
    this.offset = offset;
  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public void setOriginalName(String origName) {
    this.origName = origName;
  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public void setProperties(Resource_Property[] properties) {
    this.properties = properties;
  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public void setProperty(String code, String value) {
    Resource_Property property = new Resource_Property(code, value);

    if (properties == null) {
      // add property to new array
      properties = new Resource_Property[] { property };
      return;
    }

    int numProperties = properties.length;

    for (int i = 0; i < numProperties; i++) {
      if (properties[i].getCode().equals(code)) {
        // found, so replace
        properties[i] = property;
        return;
      }
    }

    // expand array then add property
    Resource_Property[] temp = properties;
    properties = new Resource_Property[numProperties + 1];
    System.arraycopy(temp, 0, properties, 0, numProperties);
    properties[numProperties] = property;
  }

  /**
   **********************************************************************************************
   * Only sets the value of the boolean replaced, which is used for the icons.
   **********************************************************************************************
   **/
  public void setReplaced(boolean replaced) {
    this.replaced = replaced;
  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public void setSource(File sourcePath) {
    this.sourcePath = sourcePath;
  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  @Override
  public String toString() {
    return name;
  }

}