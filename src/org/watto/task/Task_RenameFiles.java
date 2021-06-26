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
import org.watto.component.WSFileListPanelHolder;
import org.watto.component.WSPopup;
import org.watto.datatype.Resource;
import org.watto.ge.plugin.RenamerPlugin;

/**
**********************************************************************************************

**********************************************************************************************
**/
public class Task_RenameFiles extends AbstractTask {

  /** The direction to perform in the thread **/
  int direction = 1;

  /** the resources to rename **/
  Resource[] resources;

  RenamerPlugin plugin;

  /** The original contents of the resources that were changed.
      These are clone()s so that they have a separate reference to the original resources **/
  Resource[] originalResources;

  String searchValue = "";
  String replaceValue = "";

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public Task_RenameFiles(Resource[] resources, RenamerPlugin plugin, String searchValue, String replaceValue) {
    this.resources = resources;
    this.plugin = plugin;
    this.searchValue = searchValue;
    this.replaceValue = replaceValue;
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
        WSPopup.showError("RenameFiles_NoFilesSelected", true);
      }
      TaskProgressManager.stopTask();
      return;
    }

    // Progress dialog
    TaskProgressManager.show(1, 0, Language.get("Progress_RenamingFiles"));
    TaskProgressManager.setIndeterminate(true);

    TaskProgressManager.startTask();

    ((WSFileListPanelHolder) ComponentRepository.get("FileListPanelHolder")).stopInlineEditing();

    originalResources = new Resource[resources.length];
    for (int i = 0; i < resources.length; i++) {
      // clone the resource to a separate object
      originalResources[i] = (Resource) resources[i].clone();
      // perform a replace on the resource
      plugin.rename(resources[i], searchValue, replaceValue);
    }

    ((WSFileListPanelHolder) ComponentRepository.get("FileListPanelHolder")).reload();

    TaskProgressManager.stopTask();

    ChangeMonitor.change();

    if (isShowPopups()) {
      WSPopup.showMessage("RenameFiles_FilesRenamed", true);
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

    if (searchValue != null && !searchValue.equals("")) {
      name = name + "_WithSearch";
      return Language.get(name).replace("&number&", "" + resources.length).replace("&search&", "\"" + searchValue + "\"").replace("&value&", "\"" + replaceValue + "\"");
    }
    else {
      name = name + "_NoSearch";
      return Language.get(name).replace("&number&", "" + resources.length).replace("&value&", "\"" + replaceValue + "\"");
    }
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
    TaskProgressManager.show(1, 0, Language.get("Progress_RenamingFiles_Undo"));
    TaskProgressManager.setIndeterminate(true);

    TaskProgressManager.startTask();

    ((WSFileListPanelHolder) ComponentRepository.get("FileListPanelHolder")).stopInlineEditing();

    for (int i = 0; i < resources.length; i++) {
      // copy the details from the clone into the actual resource
      resources[i].copyFrom(originalResources[i]);
    }

    TaskProgressManager.stopTask();

    ((WSFileListPanelHolder) ComponentRepository.get("FileListPanelHolder")).reload();

    if (isShowPopups()) {
      WSPopup.showMessage("RenameFiles_FilesRestored", true);
    }
  }

}
