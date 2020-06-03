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
import org.watto.component.ComponentRepository;
import org.watto.component.FileListPanel;
import org.watto.component.WSFileListPanelHolder;
import org.watto.component.WSPopup;
import org.watto.component.WSTableColumn;
import org.watto.datatype.Archive;
import org.watto.datatype.Resource;
import org.watto.ge.plugin.ArchivePlugin;

/**
**********************************************************************************************

**********************************************************************************************
**/
public class Task_SearchFileList extends AbstractTask {

  /** The direction to perform in the thread **/
  int direction = 1;

  WSTableColumn[] columns;
  String searchVal;
  boolean firstMatchOnly = true;

  /**
  **********************************************************************************************

  **********************************************************************************************
  **/
  public Task_SearchFileList(WSTableColumn[] columns, String searchVal, boolean firstMatchOnly) {
    this.columns = columns;
    this.searchVal = searchVal;
    this.firstMatchOnly = firstMatchOnly;
  }

  /**
  **********************************************************************************************

  **********************************************************************************************
  **/
  @Override
  @SuppressWarnings("rawtypes")
  public void redo() {
    if (!TaskProgressManager.canDoTask()) {
      return;
    }

    int numColumns = columns.length;
    if (numColumns <= 0) {
      WSPopup.showError("Search_NoColumnsSelected", true);
      return;
    }

    if (searchVal == null || searchVal.equals("")) {
      WSPopup.showError("Search_NoSearchValue", true);
      return;
    }

    // Progress dialog
    TaskProgressManager.show(1, 0, Language.get("Progress_SearchingFiles"));
    TaskProgressManager.setIndeterminate(true);

    TaskProgressManager.startTask();

    boolean isNumber = false;
    long searchValNumber = -1;
    try {
      searchValNumber = Long.parseLong(searchVal);
      isNumber = true;
    }
    catch (Throwable t) {
    }

    boolean isBoolean = false;
    boolean searchValBoolean = true;
    if (searchVal.equals("true")) {
      isBoolean = true;
      searchValBoolean = true;
    }
    else if (searchVal.equals("false")) {
      isBoolean = true;
      searchValBoolean = false;
    }

    // determine the starting position
    int numFiles = Archive.getNumFiles();
    FileListPanel fileList = (FileListPanel) ((WSFileListPanelHolder) ComponentRepository.get("FileListPanelHolder")).getCurrentPanel();

    ArchivePlugin readPlugin = Archive.getReadPlugin();

    int startPos = fileList.getFirstSelectedRow();
    if (startPos >= numFiles) {
      startPos = 0;
    }
    else {
      startPos++;
    }

    fileList.selectNone();

    // search for the files (StartPos --> end)
    for (int i = startPos; i < numFiles; i++) {
      Resource resource = fileList.getResource(i);
      if (resource != null) {
        for (int c = 0; c < numColumns; c++) {
          WSTableColumn column = columns[c];
          Class type = column.getType();
          char columnChar = column.getCharCode();

          boolean found = false;
          if (type == String.class) {
            found = (((String) readPlugin.getColumnValue(resource, columnChar)).indexOf(searchVal) >= 0);
          }
          else if (isNumber && type == Long.class) {
            found = (((Long) readPlugin.getColumnValue(resource, columnChar)).longValue() == searchValNumber);
          }
          else if (isBoolean && type == Boolean.class) {
            found = (((Boolean) readPlugin.getColumnValue(resource, columnChar)).booleanValue() == searchValBoolean);
          }

          if (found) {
            fileList.changeSelection(i);
            if (firstMatchOnly) {
              WSPopup.showMessage("Search_MatchFound", true);
              TaskProgressManager.stopTask();
              return;
            }
            else {
              break; // stop searching the remaining columns - begin searching for the next file
            }
          }

        }
      }
    }

    // search for the files (beginning --> StartPos)
    for (int i = 0; i < startPos; i++) {
      Resource resource = fileList.getResource(i);
      if (resource != null) {
        for (int c = 0; c < numColumns; c++) {
          WSTableColumn column = columns[c];
          Class type = column.getType();
          char columnChar = column.getCharCode();

          boolean found = false;
          if (type == String.class) {
            found = (((String) readPlugin.getColumnValue(resource, columnChar)).indexOf(searchVal) >= 0);
          }
          else if (isNumber && type == Long.class) {
            found = (((Long) readPlugin.getColumnValue(resource, columnChar)).longValue() == searchValNumber);
          }
          else if (isBoolean && type == Boolean.class) {
            found = (((Boolean) readPlugin.getColumnValue(resource, columnChar)).booleanValue() == searchValBoolean);
          }

          if (found) {
            fileList.changeSelection(i);
            if (firstMatchOnly) {
              WSPopup.showMessage("Search_MatchFound", true);
              TaskProgressManager.stopTask();
              return;
            }
            else {
              break; // stop searching the remaining columns - begin searching for the next file
            }
          }

        }
      }
    }

    if (fileList.getNumSelected() <= 0) {
      // Did not find any matching files
      WSPopup.showError("Search_NoMatchFound", true);
    }

    TaskProgressManager.stopTask();

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
