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

import java.awt.Rectangle;
import java.io.File;
import java.io.FileFilter;
import java.util.Hashtable;
import javax.swing.DefaultListModel;
import org.watto.Language;
import org.watto.component.WSList;
import org.watto.ge.helper.ShellFolderFile;
import org.watto.plaf.LookAndFeelManager;
import sun.awt.shell.ShellFolder;

/**
**********************************************************************************************

**********************************************************************************************
**/
public class Task_ReloadDirectoryList extends AbstractTask {

  /** The direction to perform in the thread **/
  int direction = 1;

  /** the directory to change to **/
  File directory;

  /** the filter for the files **/
  FileFilter filter;

  /** the list to display the files **/
  WSList list;

  boolean rememberSelections = true;

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public Task_ReloadDirectoryList(File directory, FileFilter filter, WSList list, boolean rememberSelections) {
    this.directory = directory;
    this.filter = filter;
    this.list = list;
    this.rememberSelections = rememberSelections;
  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public void bypassListReloadBug() {
    // if we don't set the cell height, a dump occurs in BasicListUI:1345
    int cellHeight = LookAndFeelManager.getPropertyInt("TEXT_HEIGHT") + 8;
    if (cellHeight < 17) { // 17 = 16 (icon height) + 1
      cellHeight = 17;
    }
    list.setFixedCellHeight(cellHeight);
  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  @Override
  public void redo() {
    // Remembering selections takes up more memory, and takes longer to do.
    // So we only want to remember selections if we really have to.
    if (rememberSelections) {
      reloadWithSelections();
    }
    else {
      reloadWithoutSelections();
    }
  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  @SuppressWarnings("unchecked")
  public void reloadWithoutSelections() {
    if (list == null) {
      return;
    }

    File[] files = directory.listFiles(filter);
    if (directory instanceof ShellFolder) {
      ShellFolder shellFolder = (ShellFolder) directory;
      files = shellFolder.listFiles();
    }

    if (files == null) {
      files = new File[0]; // we still need it to load something, it'll just be blank
    }

    DefaultListModel<File> model = new DefaultListModel<File>();

    for (int i = 0; i < files.length; i++) {
      File file = files[i];
      if (file.isDirectory()) {
        if (file instanceof ShellFolder) {
          ShellFolderFile shellFolderFile = new ShellFolderFile(file);
          if (directory instanceof ShellFolderFile) {
            shellFolderFile.setParent((ShellFolderFile) directory);
          }
          model.addElement(shellFolderFile);
        }
        else {
          model.addElement(file);
        }
      }
    }

    for (int i = 0; i < files.length; i++) {
      if (!files[i].isDirectory()) {
        model.addElement(files[i]);
      }
    }

    bypassListReloadBug();
    list.clearSelection();
    list.setModel(model);

    // scroll to the top
    list.scrollRectToVisible(new Rectangle(0, 0, 0, 0));
  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  @SuppressWarnings({ "unchecked", "deprecation" })
  public void reloadWithSelections() {
    if (list == null) {
      return;
    }

    // build a hashtable of the selected items
    Object[] selectedFiles = list.getSelectedValues();
    Hashtable<String, String> selected = new Hashtable<String, String>();
    for (int i = 0; i < selectedFiles.length; i++) {
      selected.put(((File) selectedFiles[i]).getAbsolutePath(), "!");
    }

    File[] files = directory.listFiles(filter);
    if (directory instanceof ShellFolder) {
      ShellFolder shellFolder = (ShellFolder) directory;
      files = shellFolder.listFiles();
    }

    if (files == null) {
      files = new File[0]; // we still need it to load something, it'll just be blank
    }

    DefaultListModel<File> model = new DefaultListModel<File>();

    int numAdded = 0;
    int numSelected = 0;
    int[] selectedIndexes = new int[files.length];

    for (int i = 0; i < files.length; i++) {
      File file = files[i];
      if (file.isDirectory()) {

        if (file instanceof ShellFolder) {
          ShellFolderFile shellFolderFile = new ShellFolderFile(file);
          if (directory instanceof ShellFolderFile) {
            shellFolderFile.setParent((ShellFolderFile) directory);
          }
          model.addElement(shellFolderFile);
        }
        else {
          model.addElement(file);
        }

        if (selected.get(files[i].getAbsolutePath()) != null) {
          selectedIndexes[numSelected] = numAdded;
          numSelected++;
        }
        numAdded++;
      }
    }

    for (int i = 0; i < files.length; i++) {
      if (!files[i].isDirectory()) {
        model.addElement(files[i]);
        if (selected.get(files[i].getAbsolutePath()) != null) {
          selectedIndexes[numSelected] = numAdded;
          numSelected++;
        }
        numAdded++;
      }
    }

    bypassListReloadBug();
    list.clearSelection();
    list.setModel(model);

    if (numSelected > 0) {
      int[] selectedIndices = new int[numSelected];
      System.arraycopy(selectedIndexes, 0, selectedIndices, 0, numSelected);
      list.setSelectedIndices(selectedIndices);
    }

    // scroll to the selection
    //list.ensureIndexIsVisible(list.getSelectedIndex());

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
  }

}
