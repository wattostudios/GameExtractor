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

import java.awt.event.ActionListener;
import java.awt.event.FocusEvent;
import java.io.File;
import java.util.Arrays;

import javax.swing.ComboBoxModel;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JComponent;

import org.watto.event.WSFocusableInterface;
import org.watto.event.listener.WSFocusableListener;
import org.watto.ge.helper.ShellFolderFile;
import org.watto.plaf.DirectoryListDrivesComboBoxCellRenderer;
import org.watto.plaf.DirectoryListDrivesComboBoxCurrentValueCellRenderer;
import org.watto.plaf.DirectoryListDrivesComboBoxEditor;
import org.watto.xml.XMLNode;

import sun.awt.shell.ShellFolder;

public class DirectoryListDrivesComboBox extends WSComboBox implements WSFocusableInterface {

  /** serialVersionUID */
  private static final long serialVersionUID = 1L;

  static File[] drives = null;

  DirectoryListDrivesComboBoxEditor editor = null;

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  @SuppressWarnings("unchecked")
  public DirectoryListDrivesComboBox() {
    super(new XMLNode(""));
    if (drives == null) {
      loadDrives();
    }

    // Make the combobox editable
    setEditable(true);
    editor = new DirectoryListDrivesComboBoxEditor();
    setEditor(editor);
    //editor.addKeyListener(new WSKeyableListener(this));

    setRenderer(new DirectoryListDrivesComboBoxCellRenderer());
    setCurrentValueRenderer(new DirectoryListDrivesComboBoxCurrentValueCellRenderer());

    addFocusListener(new WSFocusableListener(this));

  }

  /**
  **********************************************************************************************
  Only used when starting GE, for special folders, so we can see if they match with one of these
  **********************************************************************************************
  **/
  public static File[] getDrives() {
    return drives;
  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  @Override
  public void addActionListener(ActionListener listener) {
    super.addActionListener(listener);
    if (editor != null) {
      editor.addActionListener(listener);
    }
  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  @SuppressWarnings({ "unchecked", "rawtypes" })
  public void loadDirectory() {
    setModel(new DefaultComboBoxModel(drives));
  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  @SuppressWarnings({ "unchecked", "rawtypes" })
  public void loadDirectory(File directory) {
    setModel(new DefaultComboBoxModel(drives));
    int insertPos = getItemCount();

    try {
      if (directory instanceof ShellFolder) {
        // it's a directory already
      }
      else if (directory.exists() && directory.isFile()) { // so the combo box only shows the directories, not the files
        directory = directory.getParentFile();
      }
    }
    catch (Throwable t) {
      // ignore it - this is just to catch bad errors form using a BAT to run the program (when running with newer Java versions)
      if (directory.exists() && directory.isFile()) { // so the combo box only shows the directories, not the files
        directory = directory.getParentFile();
      }
    }

    File drive = directory;
    File parent = directory.getParentFile();
    while (parent != null) {
      drive = parent;
      parent = parent.getParentFile();
    }
    for (int i = 0; i < getItemCount(); i++) {
      if (((File) getItemAt(i)).getAbsolutePath().equals(drive.getAbsolutePath())) {
        insertPos = i + 1;
        break;
      }
    }

    int selectPos = insertPos;

    // don't add the item if it is a drive, as the drive letters already exist in the list normally.
    if (!(directory instanceof ShellFolder)) {
      if (directory.getParentFile() != null) {
        insertItemAt(directory, insertPos);
      }
      else {
        selectPos--;
      }

      directory = directory.getParentFile();
      while (directory != null) {
        File parentDirectory = directory.getParentFile();
        if (parentDirectory != null) {
          insertItemAt(directory, insertPos);
          selectPos++;
        }
        directory = parentDirectory;
      }
    }

    try {
      setSelectedIndex(selectPos);
    }
    catch (Throwable t) {

    }
  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  @SuppressWarnings({ "rawtypes", "unchecked" })
  public void loadDrives() {

    drives = new File[100];
    int numDrives = 0;

    // Add roots (drives)
    File[] roots = File.listRoots();
    for (int i = 0; i < roots.length; i++) {
      drives[numDrives] = roots[i];
      numDrives++;
    }

    // Add special folders (Documents, This PC, ...)

    try {
      File[] specialFolders = (File[]) ShellFolder.get("fileChooserShortcutPanelFolders");
      if (specialFolders != null) {
        Arrays.sort(specialFolders);
        for (int i = 0; i < specialFolders.length; i++) {
          drives[numDrives] = new ShellFolderFile(specialFolders[i]);
          numDrives++;
        }
      }
    }
    catch (Throwable t) {
    }

    /*
    // Add roots (Desktop, ...)
    FileSystemView fsv = FileSystemView.getFileSystemView();
    
    roots = fsv.getRoots();
    for (int i=0;i<roots.length;i++){
      drives[numDrives] = roots[i];
      numDrives++;
      }
    */

    File[] temp = new File[numDrives];
    System.arraycopy(drives, 0, temp, 0, numDrives);
    drives = temp;

    setModel(new DefaultComboBoxModel(drives));

  }

  /**
  **********************************************************************************************
  Select all the editor text when it gains focus
  **********************************************************************************************
  **/
  @Override
  public boolean onFocus(JComponent source, FocusEvent event) {
    editor.selectAll();
    return true;
  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  @Override
  public boolean onFocusOut(JComponent source, FocusEvent event) {
    return false;
  }

  /**
  **********************************************************************************************
  Overwritten for performance reasons - makes it much faster!
  Overwritten, so it removeListDataListener() instead of adding it.
  **********************************************************************************************
  **/
  @Override
  @SuppressWarnings({ "rawtypes", "unchecked" })
  public void setModel(ComboBoxModel aModel) {
    ComboBoxModel oldModel = dataModel;
    if (oldModel != null) {
      oldModel.removeListDataListener(this);
    }
    dataModel = aModel;

    dataModel.removeListDataListener(this); // changed

    // set the current selected item.
    selectedItemReminder = dataModel.getSelectedItem();
    firePropertyChange("model", oldModel, dataModel);
  }

  /**
  **********************************************************************************************
  Overwritten for performance reasons - makes it much faster!
  Overwritten, so it does not trigger a selectedItemChanged() when a change occurs. We already
  handle the event with our listener anyway.
  **********************************************************************************************
  **/
  @SuppressWarnings({ "unused", "unchecked" })
  @Override
  public void setSelectedItem(Object anObject) {
    Object oldSelection = selectedItemReminder;
    Object objectToSelect = anObject;
    if (oldSelection == null || !oldSelection.equals(anObject)) {

      if (anObject != null && !isEditable()) {
        // For non editable combo boxes, an invalid selection
        // will be rejected.
        boolean found = false;
        for (int i = 0; i < dataModel.getSize(); i++) {
          Object element = dataModel.getElementAt(i);
          if (anObject.equals(element)) {
            found = true;
            objectToSelect = element;
            break;
          }
        }
        if (!found) {
          return;
        }
      }

      // Must toggle the state of this flag since this method
      // call may result in ListDataEvents being fired.
      boolean selectingItem = true;
      dataModel.setSelectedItem(objectToSelect);
      selectedItemReminder = objectToSelect;
      selectingItem = false;

      if (selectedItemReminder != dataModel.getSelectedItem()) {
        // in case a users implementation of ComboBoxModel
        // doesn't fire a ListDataEvent when the selection
        // changes.
        //selectedItemChanged(); // changed
      }
    }
    fireActionEvent();
  }

}