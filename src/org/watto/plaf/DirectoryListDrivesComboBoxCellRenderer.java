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
import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.border.EmptyBorder;
import javax.swing.filechooser.FileSystemView;
import javax.swing.plaf.basic.BasicComboBoxRenderer;
import org.watto.Settings;
import org.watto.ge.helper.ShellFolderFile;
import sun.awt.shell.ShellFolder;

public class DirectoryListDrivesComboBoxCellRenderer extends BasicComboBoxRenderer {

  /** serialVersionUID */
  private static final long serialVersionUID = 1L;

  static ImageIcon fileIcon;

  static ImageIcon dirIcon;

  static ImageIcon driveIcon;

  static boolean sysIcons = false;

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public DirectoryListDrivesComboBoxCellRenderer() {
    //fileIcon = new ImageIcon(getClass().getResource("images/WSFileChooser/FileIcon.png"));
    //dirIcon = new ImageIcon(getClass().getResource("images/WSFileChooser/DirectoryIcon.png"));
    //driveIcon = new ImageIcon(getClass().getResource("images/WSFileChooser/HardDriveIcon.png"));

    fileIcon = LookAndFeelManager.getImageIcon("images/WSFileChooser/FileIcon.png");
    dirIcon = LookAndFeelManager.getImageIcon("images/WSFileChooser/DirectoryIcon.png");
    driveIcon = LookAndFeelManager.getImageIcon("images/WSFileChooser/HardDriveIcon.png");

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
    String name = file.getName();

    //System.out.println("ComboCellRender - painting >" + name + "<");

    boolean drive = false;
    if (name.length() <= 0) {
      // this occurs for drive letters (such as A:\ C:\ etc.)
      name = value.toString();
      drive = true;
      //System.out.println("Actual name is >" + name + "<");
    }
    rend.setText(name);

    if (sysIcons) {
      rend.setIcon(FileSystemView.getFileSystemView().getSystemIcon(file));
    }
    else {

      if (drive) {
        rend.setIcon(driveIcon);
      }
      else if (file.isFile()) {
        rend.setIcon(fileIcon);
      }
      else {

        if (file instanceof ShellFolderFile) {
          if (((ShellFolderFile) file).getParentFile() == null) {
            rend.setIcon(driveIcon);
          }
          else {
            rend.setIcon(dirIcon);
          }
        }
        else if (file instanceof ShellFolder) {
          ShellFolder shellFolder = (ShellFolder) file;
          rend.setText(shellFolder.getDisplayName());
          rend.setIcon(driveIcon);
        }
        else {
          rend.setIcon(dirIcon);
        }
      }
    }

    int left = 0;
    if (file instanceof ShellFolder) {
      // no left padding
    }
    else {
      File parent = file.getParentFile();
      while (parent != null) {
        left += 16;
        parent = parent.getParentFile();
      }
    }

    rend.setBorder(new EmptyBorder(0, left, 0, 0));

    return rend;
  }

}