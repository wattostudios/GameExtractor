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
import org.watto.ChangeMonitor;
import org.watto.Language;
import org.watto.component.ComponentRepository;
import org.watto.component.FileListPanel;
import org.watto.component.FileListPanel_TreeTable;
import org.watto.component.WSFileListPanelHolder;
import org.watto.component.WSPopup;
import org.watto.datatype.Resource;

/**
**********************************************************************************************

**********************************************************************************************
**/
public class Task_ReplaceSelectedFiles extends AbstractTask {

  /** The direction to perform in the thread **/
  int direction = 1;

  /** the resources to replace **/
  Resource[] resources;

  /** the file to replace with **/
  File file;

  /** The original contents of the resources that were changed.
      These are clone()s so that they have a separate reference to the original resources **/
  Resource[] originalResources;

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public Task_ReplaceSelectedFiles(Resource resource, File file) {
    this.resources = new Resource[] { resource };
    this.file = file;
  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public Task_ReplaceSelectedFiles(Resource[] resources, File file) {
    this.resources = resources;
    this.file = file;
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

    if (resources == null || resources.length <= 0 || file == null) {
      return;
    }

    if (file.isDirectory()) {
      return;
    }

    // Progress dialog
    TaskProgressManager.show(1, 0, Language.get("Progress_ReplacingFiles"));
    TaskProgressManager.setIndeterminate(true);

    TaskProgressManager.startTask();

    ((WSFileListPanelHolder) ComponentRepository.get("FileListPanelHolder")).stopInlineEditing();

    originalResources = new Resource[resources.length];
    for (int i = 0; i < resources.length; i++) {
      // clone the resource to a separate object
      originalResources[i] = (Resource) resources[i].clone();
      // perform a replace on the resource
      resources[i].replace(file);
    }

    FileListPanel fileListPanel = ((FileListPanel) ((WSFileListPanelHolder) ComponentRepository.get("FileListPanelHolder")).getCurrentPanel());
    if (fileListPanel instanceof FileListPanel_TreeTable) {
      // Only want to reload the table, as that's the only thing that's changed.
      // This stops the tree from being reloaded, removing the filter.
      
      // 3.15 changed so any filter + sort is still retained
      //((FileListPanel_TreeTable) fileListPanel).reloadTable();
      ((FileListPanel_TreeTable) fileListPanel).repaintTable();
    }
    else {
      fileListPanel.reload();
    }

    TaskProgressManager.stopTask();

    ChangeMonitor.change();
    if (isShowPopups()) {
      WSPopup.showMessage("ReplaceFiles_FilesReplaced", true);
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

    return Language.get(name).replace("&number&", "" + resources.length);
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

    if (originalResources == null || originalResources.length <= 0) {
      return;
    }

    // Progress dialog
    TaskProgressManager.show(1, 0, Language.get("Progress_ReplacingFiles_Undo"));
    TaskProgressManager.setIndeterminate(true);

    TaskProgressManager.startTask();

    ((WSFileListPanelHolder) ComponentRepository.get("FileListPanelHolder")).stopInlineEditing();

    for (int i = 0; i < resources.length; i++) {
      // copy the details from the clone into the actual resource
      resources[i].copyFrom(originalResources[i]);
    }

    TaskProgressManager.stopTask();

    ((FileListPanel) ((WSFileListPanelHolder) ComponentRepository.get("FileListPanelHolder")).getCurrentPanel()).reload();

    if (isShowPopups()) {
      WSPopup.showMessage("ReplaceFiles_FilesRestored", true);
    }
  }

}
