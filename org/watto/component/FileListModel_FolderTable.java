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
import org.watto.datatype.Archive;
import org.watto.datatype.Resource;
import org.watto.ge.helper.FileListSorter;
import org.watto.ge.plugin.ArchivePlugin;

public class FileListModel_FolderTable implements FileListModel, TableModel {

  Resource[] resources;
  WSTableColumn[] columns;
  ArchivePlugin readPlugin;

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public FileListModel_FolderTable() {
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
    return readPlugin.getColumnValue(resources[row], columns[column].getCharCode());
  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  @Override
  public boolean isCellEditable(int row, int column) {
    return columns[column].isEditable();
  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public void reload() {
    //resources = Archive.getResources();
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
  public void setResources(Resource[] resources) {
    this.resources = resources;
  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  @Override
  public void setValueAt(Object value, int row, int column) {
    readPlugin.setColumnValue(resources[row], columns[column].getCharCode(), value);
  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public void sortResources(int column) {
    if (columns[column].isSortable()) {
      resources = FileListSorter.sort(resources, columns[column]);
    }
  }

}