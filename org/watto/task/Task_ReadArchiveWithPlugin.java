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
import javax.swing.SwingUtilities;
import org.watto.ChangeMonitor;
import org.watto.ErrorLogger;
import org.watto.Language;
import org.watto.Settings;
import org.watto.TypecastSingletonManager;
import org.watto.component.ComponentRepository;
import org.watto.component.SidePanel_DirectoryList;
import org.watto.component.WSFileListPanelHolder;
import org.watto.component.WSPlugin;
import org.watto.component.WSPopup;
import org.watto.component.WSSidePanelHolder;
import org.watto.datatype.Archive;
import org.watto.datatype.Resource;
import org.watto.ge.GameExtractor;
import org.watto.ge.helper.FileTypeDetector;
import org.watto.ge.plugin.ArchivePlugin;

/**
**********************************************************************************************

**********************************************************************************************
**/
public class Task_ReadArchiveWithPlugin extends AbstractTask {

  /** The direction to perform in the thread **/
  int direction = 1;

  File path;

  WSPlugin plugin;

  boolean result = false;

  // is this called from within an existing thread?
  // if so, don't call TaskManager or set up the WSProgressBar
  boolean withinThread = false;

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public Task_ReadArchiveWithPlugin(File path, WSPlugin plugin) {
    this(path, plugin, false);
  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public Task_ReadArchiveWithPlugin(File path, WSPlugin plugin, boolean withinThread) {
    this.path = path;
    this.plugin = plugin;
    this.withinThread = withinThread;

    result = false;
  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public synchronized boolean getResult() {
    return result;
  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  @Override
  public void redo() {
    try {
      result = false;

      if (path == null || !path.exists()) {
        if (!withinThread) {
          WSPopup.showError("ReadArchive_FilenameMissing", true);
        }
        result = false;
        return;
      }

      if (path.isDirectory()) {
        if (!withinThread) {
          WSPopup.showError("ReadArchive_FileNotAnArchive", true);
        }
        result = false;
        return;
      }

      if (plugin == null || !(plugin instanceof ArchivePlugin)) {
        result = false;
        return;
      }

      try {
        ArchivePlugin arcPlugin = (ArchivePlugin) plugin;

        if (!withinThread) {
          // ask to save the modified archive
          if (GameExtractor.getInstance().promptToSave()) {
            return;
          }
          ChangeMonitor.reset();

          // Progress dialog
          TaskProgressManager.show(1, 0, Language.get("Progress_ReadingArchive"));

          TaskProgressManager.startTask();
        }

        Resource[] resources = (arcPlugin).read(path);

        if (resources != null && resources.length > 0) {
          //if (!ArchiveModificationMonitor.setModified(true)){
          //  return false;
          //  }

          if (arcPlugin.canScanForFileTypes() && Settings.getBoolean("IdentifyUnknownFileTypes")) {
            // Run the file type scanner over all the resources
            FileTypeDetector.determineExtensions(resources, arcPlugin);
          }

          Archive.makeNewArchive();

          Archive.setResources(resources);
          Archive.setReadPlugin(arcPlugin);
          Archive.setBasePath(path);
          Archive.setColumns(arcPlugin.getColumns());

          // now display the files that are in the archive - same as in Task_ReadArchive
          TypecastSingletonManager.getRecentFilesManager("RecentFilesManager").addRecentFile(path);

          //((FileListPanel) ((WSFileListPanelHolder) ComponentRepository.get("FileListPanelHolder")).getCurrentPanel()).reload();
          //((WSFileListPanelHolder) ComponentRepository.get("FileListPanelHolder")).reload();
          //((WSFileListPanelHolder) ComponentRepository.get("FileListPanelHolder")).requestFocus();

          // After everything else has completed, display the table
          //Task_ReloadFileListPanel task = new Task_ReloadFileListPanel(((FileListPanel) ((WSFileListPanelHolder) ComponentRepository.get("FileListPanelHolder")).getCurrentPanel()));

          if (!withinThread) {
            // If this is called directly (eg by choosing a plugin manually), run it here.
            // Otherwise, if it's called as part of the Task_ReadArchive thread, we call it as part of that Task instead.
            Task_ReloadFileListPanel task = new Task_ReloadFileListPanel((WSFileListPanelHolder) ComponentRepository.get("FileListPanelHolder"));
            task.setDirection(Task.DIRECTION_REDO);
            SwingUtilities.invokeLater(task);
          }

          WSSidePanelHolder sidePanelHolder = (WSSidePanelHolder) ComponentRepository.get("SidePanelHolder");
          if (!sidePanelHolder.getCurrentPanelCode().equals("SidePanel_DirectoryList")) {
            sidePanelHolder.reloadPanel();
          }
          else {
            ((SidePanel_DirectoryList) sidePanelHolder.getCurrentPanel()).onOpenRequest();
          }
          result = true;
        }
        else {
          result = false;
        }

        if (!withinThread) {
          TaskProgressManager.stopTask();
        }

      }
      catch (Throwable t) {
        ErrorLogger.log(t);

        if (!withinThread) {
          WSPopup.showError("ReadArchive_ReadWithPluginFailed", true);
        }

        result = false;
        return;
      }

      // check that, after opening, there is at least 1 file in the archive
      if (result == false || Archive.getNumFiles() <= 0) {
        if (!withinThread) {
          WSPopup.showError("ReadArchive_ReadWithPluginFailed", true);
        }
        result = false;
        return;
      }

      if (!withinThread) {
        WSPopup.showMessage("ReadArchive_ArchiveOpened", true);
      }

      // clear out the undo/redo
      TypecastSingletonManager.getTaskManager("TaskManager").clear();

      result = true;

    }
    catch (Throwable t) {
      ErrorLogger.log(t);
      result = false;
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
  }

}
