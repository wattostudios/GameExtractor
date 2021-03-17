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
import org.watto.component.SidePanel_Preview;
import org.watto.component.WSFileListPanelHolder;
import org.watto.component.WSPopup;
import org.watto.component.WSPreviewPanelHolder;
import org.watto.component.WSSidePanelHolder;
import org.watto.datatype.Archive;
import org.watto.datatype.Resource;
import org.watto.ge.plugin.ArchivePlugin;

/**
**********************************************************************************************

**********************************************************************************************
**/
public class Task_CheckForModifiedExportFiles extends AbstractTask {

  /** The direction to perform in the thread **/
  int direction = 1;

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
  public Task_CheckForModifiedExportFiles() {
  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  @Override
  public void redo() {
    try {
      // first, see if the archive is modifiable or not
      try {
        ArchivePlugin readPlugin = Archive.getReadPlugin();
        if (readPlugin != null) {
          if (readPlugin.canReplace() || readPlugin.canWrite() || readPlugin.canImplicitReplace()) {
            // yep, a modifiable archive
          }
          else {
            // not an editable archive, so quick exit
            return;
          }
        }
      }
      catch (Throwable t) {
        ErrorLogger.log(t);
      }

      // Whether to auto-import files or not...
      boolean autoImportModifiedFiles = Settings.getBoolean("AutoImportModifiedExportFiles");
      boolean askedForImport = false;
      boolean doImport = false;

      if (autoImportModifiedFiles) {
        doImport = true;
        askedForImport = true;
      }

      // Get the currently-previewed file, so we can detect if it's changed and reload the preview accordingly
      File currentPreviewFile = null;
      Resource currentPreviewResource = null;

      Object resourceObject = SingletonManager.get("CurrentResource");
      if (resourceObject != null) {
        currentPreviewResource = (Resource) resourceObject;
        currentPreviewFile = currentPreviewResource.getExportedPath();
      }
      boolean reloadPreview = false;

      boolean filesChanged = false;

      Resource[] resources = Archive.getResources();
      int numFiles = resources.length;

      for (int i = 0; i < numFiles; i++) {
        Resource resource = resources[i];
        if (resource.exportedPathTimestampChanged()) {
          // File has been changed by some other program on the PC, so re-import it

          if (!askedForImport) {
            // We're not auto-importing, so if we find any files to import, ask the user once
            String ok = WSPopup.showConfirm("ImportModifiedExportFiles");
            if (ok.equals(WSPopup.BUTTON_YES)) {
              doImport = true;
            }
            else {
              doImport = false;
            }
            askedForImport = true;
          }

          // Update the timestamps, file sizes, etc from the exported file
          if (doImport) {
            resource.updatePropertiesFromExportFile();
            filesChanged = true;

            if (!reloadPreview && currentPreviewFile != null && resource.getExportedPath().equals(currentPreviewFile)) {
              // want to reload the preview at the end
              reloadPreview = true;
            }

          }
        }
      }

      if (filesChanged) {
        // Set the Archive as being changed
        ChangeMonitor.change();

        // reload the file list with the new details
        FileListPanel fileListPanel = ((FileListPanel) ((WSFileListPanelHolder) ComponentRepository.get("FileListPanelHolder")).getCurrentPanel());
        if (fileListPanel instanceof FileListPanel_TreeTable) {
          // Only want to reload the table, as that's the only thing that's changed.
          // This stops the tree from being reloaded, removing the filter.
          ((FileListPanel_TreeTable) fileListPanel).reloadTable();
        }
        else {
          fileListPanel.reload();
        }

        // If you're showing a preview, and if the previewed file has been changed, we want to reload the preview
        if (reloadPreview && currentPreviewResource != null) {
          try {
            WSSidePanelHolder sidePanelHolder = (WSSidePanelHolder) ComponentRepository.get("SidePanelHolder");
            if (sidePanelHolder.getCurrentPanel() instanceof SidePanel_Preview) {

              // If the preview is a Text one, and it's been modified (but not saved) in the preview window, we need to set it as "not edited"
              // so that it'll reload from the filesystem instead
              WSPreviewPanelHolder previewHolder = (WSPreviewPanelHolder) ComponentRepository.get("SidePanel_Preview_PreviewPanelHolder");
              if (previewHolder != null) {
                if (previewHolder.getCurrentPanel() instanceof PreviewPanel_Text) {
                  PreviewPanel_Text textPreview = (PreviewPanel_Text) previewHolder.getCurrentPanel();
                  if (textPreview.isTextChanged()) {
                    textPreview.setTextChanged(false);
                  }
                }
              }

              // Now reload the preview
              Task_PreviewFile task = new Task_PreviewFile(currentPreviewResource);
              task.setDirection(Task.DIRECTION_REDO);
              task.setShowPopups(false);
              task.run();

            }
          }
          catch (Throwable t) {
            ErrorLogger.log(t);
          }
        }

        WSPopup.showMessage("ModifiedFilesImported", true);
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
