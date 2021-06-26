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

import org.watto.Language;
import org.watto.TypecastSingletonManager;
import org.watto.component.ComponentRepository;
import org.watto.component.WSFileListPanelHolder;
import org.watto.component.WSSidePanelHolder;
import org.watto.datatype.Archive;
import org.watto.ge.GameExtractor;

/**
**********************************************************************************************

**********************************************************************************************
**/
public class Task_NewArchive extends AbstractTask {

  /** The direction to perform in the thread **/
  int direction = 1;

  /**
  **********************************************************************************************

  **********************************************************************************************
  **/
  public Task_NewArchive() {
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

    // ask to save the modified archive
    if (GameExtractor.getInstance().promptToSave()) {
      return;
    }
    //ChangeMonitor.change();

    Archive.makeNewArchive();

    WSFileListPanelHolder fileListPanelHolder = ((WSFileListPanelHolder) ComponentRepository.get("FileListPanelHolder"));
    fileListPanelHolder.selectNone();
    fileListPanelHolder.reload();
    ((WSSidePanelHolder) ComponentRepository.get("SidePanelHolder")).reloadPanel();

    // clear out the undo/redo
    TypecastSingletonManager.getTaskManager("TaskManager").clear();

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
