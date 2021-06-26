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

package org.watto.event.listener;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import javax.swing.JComboBox;
import org.watto.component.DirectoryList_DirectoryList;
import sun.awt.shell.ShellFolder;

/**
**********************************************************************************************

**********************************************************************************************
**/
public class DirectoryListDirectoryListDriveChangeListener implements ActionListener {

  DirectoryList_DirectoryList list;

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public DirectoryListDirectoryListDriveChangeListener(DirectoryList_DirectoryList list) {
    this.list = list;
  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  @SuppressWarnings("rawtypes")
  @Override
  public void actionPerformed(ActionEvent e) {
    Object source = e.getSource();
    if (source instanceof JComboBox) {
      JComboBox combo = (JComboBox) source;

      File directory = null;

      Object selectedItem = combo.getSelectedItem();
      if (selectedItem instanceof String) {
        directory = new File((String) selectedItem);
      }
      else if (selectedItem instanceof File) {
        directory = (File) selectedItem;
      }
      else {
        return; // break
      }

      list.loadDirectory(directory);

      if (directory instanceof ShellFolder) {
        list.changeDirectory(directory);
      }
      else if (directory.exists() && directory.isDirectory()) {
        list.changeDirectory(directory);

      }

    }
  }

}