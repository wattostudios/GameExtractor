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

package org.watto.plaf;

import java.awt.Component;
import java.io.File;
import javax.swing.DefaultListCellRenderer;
import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.filechooser.FileSystemView;
import org.watto.Settings;

public class DirectoryListDirectoryListCellRenderer extends DefaultListCellRenderer {

  /** serialVersionUID */
  private static final long serialVersionUID = 1L;
  static ImageIcon fileIcon;
  static ImageIcon dirIcon;
  static boolean sysIcons = false;

  /**
  **********************************************************************************************

  **********************************************************************************************
  **/
  public DirectoryListDirectoryListCellRenderer() {
    //fileIcon = new ImageIcon(getClass().getResource("images/WSFileChooser/FileIcon.png"));
    //dirIcon = new ImageIcon(getClass().getResource("images/WSFileChooser/DirectoryIcon.png"));

    fileIcon = LookAndFeelManager.getImageIcon("images/WSFileChooser/FileIcon.png");
    dirIcon = LookAndFeelManager.getImageIcon("images/WSFileChooser/DirectoryIcon.png");
    sysIcons = Settings.getBoolean("ShowSystemSpecificIcons");
  }

  /**
  **********************************************************************************************

  **********************************************************************************************
  **/
  @Override
  @SuppressWarnings("rawtypes")
  public Component getListCellRendererComponent(JList list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
    JLabel rend = (JLabel) super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);

    File file = (File) value;
    rend.setText(file.getName());

    //System.out.println("Painting " + file.getName());

    if (sysIcons) {
      rend.setIcon(FileSystemView.getFileSystemView().getSystemIcon(file));
    }
    else {
      if (file.isFile()) {
        rend.setIcon(fileIcon);
      }
      else {
        rend.setIcon(dirIcon);
      }
    }

    return rend;
  }

}