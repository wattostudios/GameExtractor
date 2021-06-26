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
import org.watto.Settings;
import org.watto.component.ComponentRepository;
import org.watto.component.FileListPanel;
import org.watto.component.WSFileListPanelHolder;
import org.watto.component.WSPopup;
import org.watto.datatype.Archive;
import org.watto.datatype.Resource;
import org.watto.ge.GameExtractor;
import org.watto.ge.plugin.ArchivePlugin;

/**
**********************************************************************************************

**********************************************************************************************
**/
public class Task_ReadScriptArchive extends AbstractTask {

  /** The direction to perform in the thread **/
  int direction = 1;

  File path;
  ArchivePlugin plugin;

  /**
  **********************************************************************************************

  **********************************************************************************************
  **/
  public Task_ReadScriptArchive(File path, ArchivePlugin plugin) {
    this.path = path;
    this.plugin = plugin;
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

    if (path == null || !path.exists()) {
      WSPopup.showError("ReadArchive_FilenameMissing", true);
      TaskProgressManager.stopTask();
      return;
    }

    if (path.isDirectory()) {
      WSPopup.showError("ReadArchive_FileNotAnArchive", true);
      TaskProgressManager.stopTask();
      return;
    }

    // ask to save the modified archive
    if (GameExtractor.getInstance().promptToSave()) {
      return;
    }
    ChangeMonitor.reset();

    // Progress dialog
    TaskProgressManager.show(1, 0, Language.get("Progress_ReadingArchive"));
    TaskProgressManager.setIndeterminate(true);

    TaskProgressManager.startTask();

    //String oldCurrentArchive = Settings.getString("CurrentArchive");
    Settings.set("CurrentArchive", path.getAbsolutePath());

    Resource[] resources = plugin.read(path);

    if (resources != null && resources.length > 0) {
      Archive.makeNewArchive();

      Archive.setResources(resources);
      Archive.setReadPlugin(plugin);
      Archive.setBasePath(path);
      Archive.setColumns(plugin.getColumns());
    }

    ((FileListPanel) ((WSFileListPanelHolder) ComponentRepository.get("FileListPanelHolder")).getCurrentPanel()).reload();

    TaskProgressManager.stopTask();

    if (resources != null && resources.length > 0) {
      WSPopup.showMessage("ReadArchive_ArchiveOpened", true);
    }
    else {
      WSPopup.showError("ReadArchive_ReadWithScriptFailed", true);
    }

  }

  /**
  **********************************************************************************************

  **********************************************************************************************
  **/
  public void scanArchive(File path) {
    // true, so it knows it is started within a current task
    Task_ScanArchive task = new Task_ScanArchive(path, true);
    task.redo();
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
