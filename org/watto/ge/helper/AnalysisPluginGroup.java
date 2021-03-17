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

package org.watto.ge.helper;

import java.io.File;
import org.watto.component.WSPlugin;

/**
**********************************************************************************************
A group of files that can be opened by this plugin
**********************************************************************************************
**/
public class AnalysisPluginGroup {

  WSPlugin plugin = null;

  File[] files = null;

  int numFiles = 0;

  public WSPlugin getPlugin() {
    return plugin;
  }

  public void setPlugin(WSPlugin plugin) {
    this.plugin = plugin;
  }

  public File[] getFiles() {
    return files;
  }

  public void setFiles(File[] files) {
    this.files = files;
  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public AnalysisPluginGroup() {
  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public AnalysisPluginGroup(WSPlugin plugin) {
    this.plugin = plugin;
  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public void addFiles(File[] newFiles) {
    if (numFiles == 0) {
      files = newFiles;
      numFiles = files.length;
      return;
    }

    int numNewFiles = newFiles.length;
    int newTotal = numFiles + numNewFiles;

    File[] oldFiles = files;
    files = new File[newTotal];
    System.arraycopy(oldFiles, 0, files, 0, numFiles);
    System.arraycopy(newFiles, 0, files, numFiles, numNewFiles);
    numFiles = newTotal;
  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public void addFile(File newFile) {
    if (numFiles == 0) {
      files = new File[] { newFile };
      numFiles = 1;
      return;
    }

    int newTotal = numFiles + 1;

    File[] oldFiles = files;
    files = new File[newTotal];
    System.arraycopy(oldFiles, 0, files, 0, numFiles);
    files[numFiles] = newFile;
    numFiles = newTotal;
  }

}