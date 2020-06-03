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

import org.watto.ChangeMonitor;
import org.watto.Language;
import org.watto.component.ComponentRepository;
import org.watto.component.FileListPanel;
import org.watto.component.WSFileListPanelHolder;
import org.watto.component.WSPopup;
import org.watto.datatype.Archive;
import org.watto.datatype.Resource;

/**
**********************************************************************************************

**********************************************************************************************
**/
public class Task_RemoveFiles extends AbstractTask {

  /** The direction to perform in the thread **/
  int direction = 1;

  /** the resources to remove **/
  Resource[] resources;

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public Task_RemoveFiles(Resource[] resources) {
    this.resources = resources;
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

    if (resources == null || resources.length <= 0) {
      if (isShowPopups()) {
        WSPopup.showError("RemoveFiles_NoFilesToRemove", true);
      }
      return;
    }

    // Progress dialog
    TaskProgressManager.show(1, 0, Language.get("Progress_RemovingFiles"));
    TaskProgressManager.setIndeterminate(true);

    TaskProgressManager.startTask();

    ((WSFileListPanelHolder) ComponentRepository.get("FileListPanelHolder")).stopInlineEditing();

    FileListPanel panel = ((FileListPanel) ((WSFileListPanelHolder) ComponentRepository.get("FileListPanelHolder")).getCurrentPanel());
    panel.selectNone();

    Archive.removeResources(resources);

    panel.reload();

    TaskProgressManager.stopTask();

    ChangeMonitor.change();
    if (isShowPopups()) {
      WSPopup.showMessage("RemoveFiles_FilesRemoved", true);
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

    if (resources == null || resources.length <= 0) {
      return;
    }

    // Progress dialog
    TaskProgressManager.show(1, 0, Language.get("Progress_RemovingFiles_Undo"));
    TaskProgressManager.setIndeterminate(true);

    TaskProgressManager.startTask();

    ((WSFileListPanelHolder) ComponentRepository.get("FileListPanelHolder")).stopInlineEditing();
    Archive.addResources(resources);

    TaskProgressManager.stopTask();

    ((FileListPanel) ((WSFileListPanelHolder) ComponentRepository.get("FileListPanelHolder")).getCurrentPanel()).reload();
    if (isShowPopups()) {
      WSPopup.showMessage("RemoveFiles_FilesAdded", true);
    }
  }

}
