/*
 * Application:  Game Extractor
 * Author:       wattostudios
 * Website:      http://www.watto.org
 * Copyright:    Copyright (c) 2002-2021 wattostudios
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
import org.watto.ErrorLogger;
import org.watto.Language;
import org.watto.Settings;
import org.watto.SingletonManager;
import org.watto.component.ComponentRepository;
import org.watto.component.FileListPanel;
import org.watto.component.FileListPanel_TreeTable;
import org.watto.component.PreviewPanel_Text;
import org.watto.component.WSFileListPanelHolder;
import org.watto.component.WSPopup;
import org.watto.datatype.Resource;
import org.watto.io.FileManipulator;

/**
**********************************************************************************************
When editing a text file in Game Extractor, this will export it to the filesystem (if needed)
and then save the changes to that file. Finally, it flags the Archive as being changed.
**********************************************************************************************
**/
public class Task_WriteEditedTextFile extends AbstractTask {

  /** The direction to perform in the thread **/
  int direction = 1;

  Resource resource = null;

  PreviewPanel_Text previewPanel = null;

  // so we can stop "file exported" popup from appearing when doing a preview
  boolean showPopups = true;

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  @Override
  public void setShowPopups(boolean showPopups) {
    this.showPopups = showPopups;
  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public Task_WriteEditedTextFile(PreviewPanel_Text previewPanel) {
    this.previewPanel = previewPanel;
  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public Task_WriteEditedTextFile(PreviewPanel_Text previewPanel, Resource resource) {
    this.resource = resource;
    this.previewPanel = previewPanel;
  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  @Override
  public void redo() {
    if (previewPanel == null) {
      return;
    }

    try {
      if (resource == null) {
        Object resourceObject = SingletonManager.get("CurrentResource");
        if (resourceObject == null) {
          return;
        }
        resource = (Resource) resourceObject;
      }

      if (resource == null) {
        return;
      }

      File exportedPath = resource.getExportedPath();
      if (exportedPath == null || !exportedPath.exists()) {
        // Export it
        File directory = new File(new File(Settings.get("TempDirectory")).getAbsolutePath());

        Task_ExportFiles task = new Task_ExportFiles(directory, resource);
        task.setShowPopups(false);
        task.setShowProgressPopups(false); // this barely appears, and slows down the preview repainting significantly, so don't worry about it.
        task.redo();
      }

      exportedPath = resource.getExportedPath();
      if (exportedPath == null || !exportedPath.exists()) {
        return; // couldn't extract the file for some reason
      }

      // Rename the original extracted file to _GE_ORIGINAL
      File originalPath = new File(exportedPath.getAbsolutePath() + "_ge_original");
      if (originalPath.exists()) {
        // ignore it
      }
      else {
        exportedPath.renameTo(originalPath);
      }

      // Now save the changes to the proper Extracted filename
      if (exportedPath.exists() && exportedPath.isFile()) {
        // try to delete it first
        exportedPath.delete();
      }
      if (exportedPath.exists() && exportedPath.isFile()) {
        return; // Failed to delete for some reason
      }

      FileManipulator fm = new FileManipulator(exportedPath, true);
      fm.writeString(previewPanel.getText());
      fm.close();

      if (!exportedPath.exists()) {
        return; // failed to save for some reason
      }

      boolean reloadRequired = !resource.isReplaced();

      // Set the Resource as being changed
      resource.setReplaced(true);

      // Set the Archive as being changed
      ChangeMonitor.change();

      // the changes have been saved to the temp file
      previewPanel.setTextChanged(false);

      // Update the timestamps, file sizes, etc from the exported file
      resource.updatePropertiesFromExportFile();

      if (reloadRequired) {
        FileListPanel fileListPanel = ((FileListPanel) ((WSFileListPanelHolder) ComponentRepository.get("FileListPanelHolder")).getCurrentPanel());
        if (fileListPanel instanceof FileListPanel_TreeTable) {
          // Only want to reload the table, as that's the only thing that's changed.
          // This stops the tree from being reloaded, removing the filter.
          ((FileListPanel_TreeTable) fileListPanel).reloadTable();
        }
        else {
          fileListPanel.reload();
        }
      }

      if (showPopups) {
        WSPopup.showMessage("PreviewPanel_Text_ChangesSaved", true);
      }

    }
    catch (Throwable t) {
      ErrorLogger.log(t);
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
