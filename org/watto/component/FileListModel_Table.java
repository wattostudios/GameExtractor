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

package org.watto.component;

import javax.swing.event.TableModelListener;
import javax.swing.table.TableModel;
import org.watto.Settings;
import org.watto.TypecastSingletonManager;
import org.watto.datatype.Archive;
import org.watto.datatype.Resource;
import org.watto.ge.GameExtractor;
import org.watto.ge.helper.FileListSorter;
import org.watto.ge.plugin.ArchivePlugin;
import org.watto.ge.plugin.RenamerPlugin;
import org.watto.task.Task;
import org.watto.task.Task_RenameFiles;

public class FileListModel_Table implements FileListModel, TableModel {

  Resource[] resources;

  WSTableColumn[] columns;

  ArchivePlugin readPlugin;

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public FileListModel_Table() {
    reload();
  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  @Override
  public void addTableModelListener(TableModelListener tml) {
  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  @Override
  @SuppressWarnings({ "unchecked", "rawtypes" })
  public Class getColumnClass(int column) {
    if (column >= columns.length) {
      return String.class;
    }
    return columns[column].getType();
  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  @Override
  public int getColumnCount() {
    return columns.length;
  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  @Override
  public String getColumnName(int column) {
    return columns[column].getName();
  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public Resource getResource(int row) {
    if (row < resources.length) {
      return resources[row];
    }
    else {
      return null;
    }
  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  @Override
  public int getRowCount() {
    return resources.length;
  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  @Override
  public Object getValueAt(int row, int column) {
    try {
      return readPlugin.getColumnValue(resources[row], columns[column].getCharCode());
    }
    catch (Throwable t) {
      return "";
    }
  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  @Override
  public boolean isCellEditable(int row, int column) {
    // if the column is a customm dcolumn and it is editable, allow it.
    // used codes: a,c,C,d,D,E,F,i,I,N,O,P,r,R,S,z,Z
    if (columns[column].isEditable()) {
      char charCode = columns[column].getCharCode();
      if (charCode != 'a' && charCode != 'c' && charCode != 'C' && charCode != 'd' && charCode != 'D' && charCode != 'E' &&
          charCode != 'F' && charCode != 'i' && charCode != 'I' && charCode != 'N' && charCode != 'O' && charCode != 'P' &&
          charCode != 'r' && charCode != 'R' && charCode != 'S' && charCode != 'z' && charCode != 'Z') {

        return true;
      }
    }

    // else, only allow if editable and the setting is turned on
    if (!Settings.getBoolean("AllowInlineFileListEditing")) {
      return false;
    }
    if (columns[column].isEditable()) {
      if (GameExtractor.isFullVersion()) {
        // only allow inline editing in the Full Version
        return true;
      }
    }
    return false;
  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public void reload() {
    resources = Archive.getResources();
    readPlugin = Archive.getReadPlugin();
    columns = readPlugin.getViewingColumns();
  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public void reload(Resource[] resources) {
    this.resources = resources;
    readPlugin = Archive.getReadPlugin();
    columns = readPlugin.getViewingColumns();
  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  @Override
  public void removeTableModelListener(TableModelListener tml) {
  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  @Override
  public void setValueAt(Object value, int row, int column) {
    char charCode = columns[column].getCharCode();
    if (charCode == 'P' || charCode == 'F' || charCode == 'N' || charCode == 'E') {
      // this is a rename operation
      Resource resource = resources[row];

      // simulate the renaming
      String oldName = resource.getName();
      if (oldName.equals(value)) {
        return; // filename didn't change
      }

      readPlugin.setColumnValue(resource, charCode, value);
      String newName = resource.getName();
      resource.setName(oldName);

      RenamerPlugin plugin = (RenamerPlugin) WSPluginManager.getGroup("Renamer").getPlugin("Renamer_Rename");

      // now do the actual change, via the task, and add it to the UndoManager
      Task_RenameFiles task = new Task_RenameFiles(new Resource[] { resource }, plugin, newName, newName);
      task.setDirection(Task.DIRECTION_REDO);
      new Thread(task).start();
      TypecastSingletonManager.getTaskManager("TaskManager").add(task);

    }
    else {
      // something else - plugin-specific
      readPlugin.setColumnValue(resources[row], charCode, value);
    }
  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public void sortResources(int column) {
    sortResources(column, false);
  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public void sortResources(int column, boolean useCurrentResources) {
    if (columns[column].isSortable()) {
      if (useCurrentResources) {
        resources = FileListSorter.sort(resources, columns[column]);
      }
      else {
        resources = FileListSorter.sort(columns[column]);
      }
    }
  }

}