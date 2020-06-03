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
import org.watto.Settings;
import org.watto.SingletonManager;
import org.watto.component.ComponentRepository;
import org.watto.component.SidePanel_ImageInvestigator;
import org.watto.component.SidePanel_Preview;
import org.watto.datatype.Resource;

/**
**********************************************************************************************

**********************************************************************************************
**/
public class Task_PreviewFile extends AbstractTask {

  /** The direction to perform in the thread **/
  int direction = 1;

  File directory = null;

  Resource resource;

  /**
  **********************************************************************************************
  Extracts a file to Directory, then previews it
  **********************************************************************************************
  **/
  public Task_PreviewFile(File directory, Resource resource) {
    this.directory = directory;
    this.resource = resource;
  }

  /**
  **********************************************************************************************
  Previews a file that has already been extracted
  **********************************************************************************************
  **/
  public Task_PreviewFile(Resource resource) {
    this.resource = resource;
  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  @Override
  public void redo() {
    SingletonManager.set("CurrentResource", resource); // so it can be detected by ViewerPlugins for Thumbnail Generation

    if (directory == null) {
      // already extracted
    }
    else {
      Task_ExportFiles task = new Task_ExportFiles(directory, resource);
      task.setShowPopups(false);
      task.setShowProgressPopups(false); // this barely appears, and slows down the preview repainting significantly, so don't worry about it.
      task.redo();
    }
    File path = resource.getExportedPath();

    // Check if we need to show a modal popup to the user before displaying the preview
    if (SingletonManager.has("ShowMessageBeforeExport")) {
      // show a message to the user. Once the user clicks the OK button, it'll call previewFile(path) as part of that Thread
      Task_Popup_ShowMessageBeforePreview popupTask = new Task_Popup_ShowMessageBeforePreview((String) SingletonManager.get("ShowMessageBeforeExport"), path);
      popupTask.setDirection(Task.DIRECTION_REDO);
      new Thread(popupTask).start();
      SingletonManager.remove("ShowMessageBeforeExport");
    }
    else {
      // preview the file as per normal
      // See if we're trying to open the Preview or ImageInvestigator sidepanels
      String currentSidePanel = Settings.getString("CurrentSidePanel");
      if (currentSidePanel.equals("SidePanel_Preview")) {
        ((SidePanel_Preview) ComponentRepository.get("SidePanel_Preview")).previewFile(path);
      }
      else if (currentSidePanel.equals("SidePanel_ImageInvestigator")) {
        ((SidePanel_ImageInvestigator) ComponentRepository.get("SidePanel_ImageInvestigator")).previewFile(path);
      }
      else {
        ((SidePanel_Preview) ComponentRepository.get("SidePanel_Preview")).previewFile(path);
      }
    }

    //((SidePanel_Preview) ComponentRepository.get("SidePanel_Preview")).previewFile(path);
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
