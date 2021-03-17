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
import org.watto.component.FileListPanel;
import org.watto.component.WSFileListPanelHolder;

/**
**********************************************************************************************

**********************************************************************************************
**/
public class Task_ReloadFileListPanel extends AbstractTask {

  /** The direction to perform in the thread **/
  int direction = 1;

  FileListPanel panel = null;

  WSFileListPanelHolder panelHolder = null;

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public Task_ReloadFileListPanel(FileListPanel panel) {
    this.panel = panel;
  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public Task_ReloadFileListPanel(WSFileListPanelHolder panelHolder) {
    this.panelHolder = panelHolder;
  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  @Override
  public void redo() {
    //System.out.println("TASK-->RELOAD FILE LIST");
    /*
    System.out.println("Reloading");
    try {
      throw new Exception();
    }
    catch (Throwable t) {
      t.printStackTrace();
    }
    */

    if (panel != null) {
      panel.reload();
    }
    if (panelHolder != null) {
      panelHolder.reload();
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
