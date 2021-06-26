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

import org.watto.datatype.Resource;
import org.watto.ge.plugin.ViewerPlugin;

/**
**********************************************************************************************
A group of files that can be opened by this plugin
**********************************************************************************************
**/
public class AnalysisViewerGroup {

  ViewerPlugin plugin = null;

  Resource[] resources = null;

  int numFiles = 0;

  public ViewerPlugin getPlugin() {
    return plugin;
  }

  public void setPlugin(ViewerPlugin plugin) {
    this.plugin = plugin;
  }

  public Resource[] getResources() {
    return resources;
  }

  public void setResources(Resource[] resources) {
    this.resources = resources;
  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public AnalysisViewerGroup() {
  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public AnalysisViewerGroup(ViewerPlugin plugin) {
    this.plugin = plugin;
  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public void addResources(Resource[] newFiles) {
    if (numFiles == 0) {
      resources = newFiles;
      numFiles = resources.length;
      return;
    }

    int numNewFiles = newFiles.length;
    int newTotal = numFiles + numNewFiles;

    Resource[] oldFiles = resources;
    resources = new Resource[newTotal];
    System.arraycopy(oldFiles, 0, resources, 0, numFiles);
    System.arraycopy(newFiles, 0, resources, numFiles, numNewFiles);
    numFiles = newTotal;
  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public void addResource(Resource newFile) {
    if (numFiles == 0) {
      resources = new Resource[] { newFile };
      numFiles = 1;
      return;
    }

    int newTotal = numFiles + 1;

    Resource[] oldFiles = resources;
    resources = new Resource[newTotal];
    System.arraycopy(oldFiles, 0, resources, 0, numFiles);
    resources[numFiles] = newFile;
    numFiles = newTotal;
  }

}