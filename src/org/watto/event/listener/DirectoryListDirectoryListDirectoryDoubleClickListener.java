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

import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.io.File;
import org.watto.component.DirectoryList_DirectoryList;

/**
**********************************************************************************************

**********************************************************************************************
**/
public class DirectoryListDirectoryListDirectoryDoubleClickListener implements MouseListener {

  DirectoryList_DirectoryList list;

  /**
  **********************************************************************************************

  **********************************************************************************************
  **/
  public DirectoryListDirectoryListDirectoryDoubleClickListener(DirectoryList_DirectoryList list) {
    this.list = list;
  }

  /**
  **********************************************************************************************

  **********************************************************************************************
  **/
  @Override
  public void mouseClicked(MouseEvent e) {

    if (e.getClickCount() == 2) {
      File file = list.getSelectedFile();
      if (file.isDirectory()) {
        list.changeDirectory(file);
      }
    }

  }

  /**
  **********************************************************************************************

  **********************************************************************************************
  **/
  @Override
  public void mouseEntered(MouseEvent e) {
  }

  /**
  **********************************************************************************************

  **********************************************************************************************
  **/
  @Override
  public void mouseExited(MouseEvent e) {
  }

  /**
  **********************************************************************************************

  **********************************************************************************************
  **/
  @Override
  public void mousePressed(MouseEvent e) {
  }

  /**
  **********************************************************************************************

  **********************************************************************************************
  **/
  @Override
  public void mouseReleased(MouseEvent e) {
  }

}