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
import org.watto.component.WSFileListPanelHolder;
import org.watto.component.WSPopup;
import org.watto.datatype.Resource;

/**
**********************************************************************************************

**********************************************************************************************
**/
public class Task_ReplaceMatchingFiles extends AbstractTask {

  /** The direction to perform in the thread **/
  int direction = 1;

  /** the resources to replace **/
  Resource[] resources;
  /** the base path to search for a match **/
  File basePath;

  /** The original contents of the resources that were changed.
      These are clone()s so that they have a separate reference to the original resources **/
  Resource[] originalResources;
  Resource[] replacedResources;

  /**
  **********************************************************************************************

  **********************************************************************************************
  **/
  public Task_ReplaceMatchingFiles(Resource[] resources, File basePath) {
    this.resources = resources;
    this.basePath = basePath;
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

    if (resources == null || resources.length <= 0 || basePath == null) {
      return;
    }

    if (!basePath.isDirectory()) {
      return;
    }

    // Progress dialog
    TaskProgressManager.show(1, 0, Language.get("Progress_ReplacingFiles"));
    TaskProgressManager.setIndeterminate(true);

    TaskProgressManager.startTask();

    ((WSFileListPanelHolder) ComponentRepository.get("FileListPanelHolder")).stopInlineEditing();

    String baseDir = basePath.getAbsolutePath() + File.separator;
    String baseDirParent = basePath.getParentFile().getAbsolutePath() + File.separator;

    originalResources = new Resource[resources.length];
    replacedResources = new Resource[resources.length];
    int numberReplaced = 0;

    for (int i = 0; i < resources.length; i++) {
      // 1. Check if the file exists
      File newFile = new File(baseDir + resources[i].getName());
      if (newFile.exists()) {
        // clone the resource to a separate object
        originalResources[numberReplaced] = (Resource) resources[i].clone();
        replacedResources[numberReplaced] = resources[i];
        numberReplaced++;
        // perform a replace on the resource
        resources[i].replace(newFile);
      }
      else {
        // 2. If not found, check the parent directory for the file
        newFile = new File(baseDirParent + resources[i].getName());
        if (newFile.exists()) {
          // clone the resource to a separate object
          originalResources[numberReplaced] = (Resource) resources[i].clone();
          replacedResources[numberReplaced] = resources[i];
          numberReplaced++;
          // perform a replace on the resource
          resources[i].replace(newFile);
        }
      }
    }

    if (numberReplaced < resources.length) {
      // resize the clone arrays to take up less memory
      Resource[] tempResources = originalResources;
      originalResources = new Resource[numberReplaced];
      System.arraycopy(tempResources, 0, originalResources, 0, numberReplaced);

      tempResources = replacedResources;
      replacedResources = new Resource[numberReplaced];
      System.arraycopy(tempResources, 0, replacedResources, 0, numberReplaced);
    }

    ((FileListPanel) ((WSFileListPanelHolder) ComponentRepository.get("FileListPanelHolder")).getCurrentPanel()).reload();

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

    String numResources = "";
    if (replacedResources != null) {
      numResources = "" + replacedResources.length;
    }

    return Language.get(name).replace("&number&", "" + numResources);
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

    for (int i = 0; i < originalResources.length; i++) {
      // copy the details from the clone into the actual resource
      replacedResources[i].copyFrom(originalResources[i]);
    }

    TaskProgressManager.stopTask();

    ((FileListPanel) ((WSFileListPanelHolder) ComponentRepository.get("FileListPanelHolder")).getCurrentPanel()).reload();

    if (isShowPopups()) {
      WSPopup.showMessage("ReplaceFiles_FilesRestored", true);
    }
  }

}
