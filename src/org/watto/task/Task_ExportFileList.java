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

package org.watto.task;

import java.io.File;
import org.watto.Language;
import org.watto.component.WSPopup;
import org.watto.component.WSTableColumn;
import org.watto.ge.plugin.FileListExporterPlugin;
import org.watto.io.DirectoryBuilder;

/**
**********************************************************************************************

**********************************************************************************************
**/
public class Task_ExportFileList extends AbstractTask {

  /** The direction to perform in the thread **/
  int direction = 1;

  FileListExporterPlugin plugin;

  WSTableColumn[] columns;

  File directory = null;

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public Task_ExportFileList(FileListExporterPlugin plugin, WSTableColumn[] columns, File directory) {
    this.plugin = plugin;
    this.columns = columns;
    this.directory = directory;
  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  @Override
  public void redo() {
    if (!TaskProgressManager.canDoTask()) {
      return;
    }

    // Progress dialog
    TaskProgressManager.show(1, 0, Language.get("Progress_ExportingFiles"));
    TaskProgressManager.setIndeterminate(true);

    TaskProgressManager.startTask();

    // check the directory exists (false because the filename is in the path)
    if (directory == null) {
      // writing to stdout
      plugin.write(columns, directory);
    }
    else {
      DirectoryBuilder.buildDirectory(directory, false);
      plugin.write(columns, directory);
    }

    TaskProgressManager.stopTask();

    if (isShowPopups()) {
      WSPopup.showMessage("ExportFileList_ListExported", true);
    }
  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  @Override
  @SuppressWarnings("rawtypes")
  public String toString() {
    Class cl = getClass();
    String name = cl.getName();
    Package pack = cl.getPackage();

    if (pack != null) {
      name = name.substring(pack.getName().length() + 1);
    }

    return Language.get(name);
  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  @Override
  public void undo() {
    if (!TaskProgressManager.canDoTask()) {
      return;
    }
  }

}
