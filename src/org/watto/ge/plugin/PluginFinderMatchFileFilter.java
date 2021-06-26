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

package org.watto.ge.plugin;

import java.io.File;
import java.io.FileFilter;
import sun.awt.shell.ShellFolder;

/**
**********************************************************************************************

**********************************************************************************************
**/
public class PluginFinderMatchFileFilter implements FileFilter {

  ArchivePlugin plugin;

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public PluginFinderMatchFileFilter(ArchivePlugin plugin) {
    this.plugin = plugin;
  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  @Override
  public boolean accept(File file) {

    if (file.isDirectory() || file instanceof ShellFolder) {
      return true;
    }

    if (plugin instanceof AllFilesPlugin) {
      // quicker when a filter isn't selected,
      // as it doesn't have to open each file in a FileManipulator and check it.
      return true;
    }

    if (plugin.getMatchRating(file) >= 25) {
      return true;
    }
    else {
      return false;
    }

  }

}
