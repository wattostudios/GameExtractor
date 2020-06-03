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

import java.awt.BorderLayout;
import java.awt.Point;
import java.awt.datatransfer.Transferable;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.io.File;
import java.io.FileFilter;
import javax.swing.DefaultListModel;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.ListModel;
import javax.swing.ListSelectionModel;
import javax.swing.TransferHandler;
import javax.swing.border.EmptyBorder;
import org.watto.Settings;
import org.watto.event.WSClickableInterface;
import org.watto.event.WSKeyableInterface;
import org.watto.event.WSRightClickableInterface;
import org.watto.event.WSTransferableInterface;
import org.watto.event.listener.DirectoryListDirectoryListDirectoryDoubleClickListener;
import org.watto.event.listener.DirectoryListDirectoryListDriveChangeListener;
import org.watto.event.listener.DirectoryListDirectoryListFileDoubleClickListener;
import org.watto.event.listener.DirectoryListDirectoryListFileSingleClickListener;
import org.watto.event.listener.WSTransferableListener;
import org.watto.ge.GameExtractor;
import org.watto.ge.helper.ShellFolderFile;
import org.watto.plaf.DirectoryListDirectoryListCellRenderer;
import org.watto.plaf.DirectoryListDrivesComboBoxCellRenderer;
import org.watto.task.Task;
import org.watto.task.Task_ReadArchive;
import org.watto.task.Task_ReloadDirectoryList;
import org.watto.xml.XMLReader;
import sun.awt.shell.ShellFolder;

public class DirectoryList_DirectoryList extends DirectoryListPanel implements WSClickableInterface,
    WSKeyableInterface,
    WSTransferableInterface,
    WSRightClickableInterface {

  /** serialVersionUID */
  private static final long serialVersionUID = 1L;

  DirectoryListDrivesComboBox drives;

  WSList list;

  WSButton upButton;

  /** for knowing if the directory is just reloaded, or changed to a different one **/
  File currentDirectory = null;

  FileFilter filter = null;

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public DirectoryList_DirectoryList() {
    super("List");
    constructInterface(new File(Settings.get("CurrentDirectory")));
  }

  /**
   **********************************************************************************************
   * Makes a fresh root and calls reloadTree()
   **********************************************************************************************
   **/
  public void changeDirectory(File directory) {
    if (!directory.exists()) {
      return;
    }

    if (directory.isFile()) {
      loadDirectory(directory);
      directory = directory.getParentFile();
    }

    if (directory instanceof ShellFolder) {
      directory = new ShellFolderFile(directory);
    }

    Settings.set("CurrentDirectory", directory.getAbsolutePath());

    drives.loadDirectory(directory);

  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public void changeToParent() {
    File current = null;

    Object selectedItem = drives.getSelectedItem();
    if (selectedItem instanceof File) {
      current = (File) selectedItem;
    }
    else if (selectedItem instanceof String) {
      current = new File((String) selectedItem);
    }
    else {
      return; // break here
    }

    File parent = current.getParentFile();
    if (parent != null) {
      Settings.set("CurrentDirectory", parent.getAbsolutePath());
      changeDirectory(parent);
    }

  }

  /**
   **********************************************************************************************
   * Checks the current directory when it is being repainted incase any files are added/removed
   **********************************************************************************************
   **/
  @SuppressWarnings("rawtypes")
  @Override
  public void checkFilesExist() {
    File currentDirectory = null;

    Object selectedItem = drives.getSelectedItem();
    if (selectedItem instanceof File) {
      currentDirectory = (File) selectedItem;
    }
    else if (selectedItem instanceof String) {
      currentDirectory = new File((String) selectedItem);
    }

    if (currentDirectory == null || !currentDirectory.exists()) {
      // if the dir was removed, change to the program directory
      changeDirectory(new File(new File("").getAbsolutePath()));
      return;
    }

    if (currentDirectory.isFile()) {
      //currentDirectory = currentDirectory.getParentFile();
      changeDirectory(currentDirectory);
      return;
    }

    ListModel listModel = list.getModel();

    // check for same number of files
    try {
      if (currentDirectory.listFiles(filter).length != listModel.getSize()) {
        changeDirectory(currentDirectory);
        return;
      }
    }
    catch (Throwable t) {
      // security exception or something like that, so show the program directory instead 
      changeDirectory(new File(new File("").getAbsolutePath()));
    }

    // check for deleted files (occurs when a file was deleted and replaced by another, so
    // there is still the same number of files in the dir)
    for (int i = 0; i < listModel.getSize(); i++) {
      if (!(((File) listModel.getElementAt(i)).exists())) {
        changeDirectory(currentDirectory);
        return;
      }
    }

  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public void constructInterface() {
    constructInterface(new File(new File("").getAbsolutePath()));
  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  @Override
  @SuppressWarnings({ "unchecked", "static-access" })
  public void constructInterface(File directory) {
    removeAll();

    drives = new DirectoryListDrivesComboBox();
    drives.setBorder(new EmptyBorder(0, 0, 0, 0));
    drives.addActionListener(new DirectoryListDirectoryListDriveChangeListener(this));
    //drives.addMouseListener(new DirectoryListDirectoryListDriveChangeListener(this));

    upButton = new WSButton(XMLReader.read("<WSButton code=\"ParentDirectory\" showText=\"false\" />"));

    list = new WSList(XMLReader.read("<WSList code=\"DirectoryList\" />"));
    //list.setLayoutOrientation(WSList.VERTICAL_WRAP);
    list.setLayoutOrientation(WSList.VERTICAL);
    list.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
    list.addMouseListener(new DirectoryListDirectoryListDirectoryDoubleClickListener(this));
    list.addMouseListener(new DirectoryListDirectoryListFileDoubleClickListener(this));
    list.addMouseListener(new DirectoryListDirectoryListFileSingleClickListener(this));
    list.setCellRenderer(new DirectoryListDirectoryListCellRenderer());

    list.setDragEnabled(true);
    list.addMouseMotionListener(new WSTransferableListener(this));

    JScrollPane listScroll = new JScrollPane(list);
    listScroll.setViewportBorder(new EmptyBorder(0, 0, 0, 0));

    setLayout(new BorderLayout(1, 1));
    add(listScroll, BorderLayout.CENTER);

    JPanel drivePanel = new JPanel(new BorderLayout(1, 1));
    drivePanel.setBorder(new EmptyBorder(0, 0, 0, 0));
    drivePanel.add(drives, BorderLayout.CENTER);
    drivePanel.add(upButton, BorderLayout.EAST);

    add(drivePanel, BorderLayout.NORTH);

    if (directory.exists()) {
      changeDirectory(directory);
    }
    else {
      /*
      // it might be represented by a Special Folder (eg "This PC" or "Documents") - lets check that first
      File[] specialFolders = drives.getDrives();
      int numSpecial = specialFolders.length;
      
      // get the drive name only, from the source file
      String currentPath = directory.getPath();
      if (currentPath != null) {
        int slashPos = currentPath.indexOf(File.separatorChar);
        if (slashPos != -1) {
          currentPath = currentPath.substring(0, slashPos);
        }
      
        for (int i = 0; i < numSpecial; i++) {
          File specialFolder = specialFolders[i];
          if (specialFolder instanceof ShellFolderFile) {
            if (specialFolder.getName().equals(currentPath)) {
              // found it
      
              // Drill through from the special folder into the children until we find the actual folder we want
              String[] parents = null;
              try {
                parents = directory.getPath().split(File.separator);
              }
              catch (Throwable t) {
                parents = directory.getPath().split(File.separator + File.separator);
              }
              int parentCount = parents.length;
      
              for (int p = 1; p < parentCount; p++) { // start at 1, because we've already found 0 as the specialFolder
                String fileToMatch = parents[p];
      
                File[] children = specialFolder.listFiles();
                int numChildren = children.length;
                for (int c = 0; c < numChildren; c++) {
                  File currentChild = children[c];
                  if (currentChild.getName().equals(fileToMatch)) {
                    // found the match, move to the next parent
                    if (currentChild instanceof ShellFolder) {
                      ShellFolderFile currentShellChild = new ShellFolderFile(currentChild);
                      currentShellChild.setParent((ShellFolderFile) specialFolder);
                      currentChild = currentShellChild;
                    }
                    specialFolder = currentChild;
                    break;
                  }
                  else if (currentChild instanceof ShellFolder && ((ShellFolder) currentChild).getDisplayName().equals(fileToMatch)) {
                    // found the match, move to the next parent
                    if (currentChild instanceof ShellFolder) {
                      ShellFolderFile currentShellChild = new ShellFolderFile(currentChild);
                      currentShellChild.setParent((ShellFolderFile) specialFolder);
                      currentChild = currentShellChild;
                    }
                    specialFolder = currentChild;
                    break;
                  }
                }
      
              }
      
              // If we got here, we found all (or most of) the path, so we'll load that
              changeDirectory(specialFolder);
              return;
      
            }
          }
        }
      }
      */
      File specialFolder = ShellFolderFile.getFileForPath(directory);
      if (specialFolder != null) {
        changeDirectory(specialFolder);
        return;
      }

      // nope, maybe the file/folder is missing or something - no biggie, load the GameExtractor folder instead
      changeDirectory(new File(new File("").getAbsolutePath()));
    }

  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  @SuppressWarnings("deprecation")
  @Override
  public File[] getAllSelectedFiles() {
    Object[] files = list.getSelectedValues();
    File[] selections = new File[files.length];

    for (int i = 0; i < files.length; i++) {
      selections[i] = (File) files[i];
    }

    return selections;
  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  @Override
  public File getCurrentDirectory() {
    return (File) drives.getSelectedItem();
  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  @Override
  public DirectoryListPanel getNew() {
    return new DirectoryList_DirectoryList();
  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  @Override
  public File getSelectedFile() {
    Object selectedFile = list.getSelectedValue();
    if (selectedFile == null) {
      String currentDirectory = Settings.get("CurrentDirectory");
      File currentFile = new File(currentDirectory);
      if (currentFile.exists()) {
        return currentFile;
      }
      else {
        File specialFolder = ShellFolderFile.getFileForPath(currentFile);
        if (specialFolder != null) {
          return specialFolder;
        }
      }
      return currentFile;
    }
    else {
      return (File) selectedFile;
    }
  }

  /**
   **********************************************************************************************
   * Makes a fresh root and calls reloadTree()
   **********************************************************************************************
   **/
  public void loadDirectory(File directory) {
    if (currentDirectory != null && currentDirectory.getAbsolutePath().equals(directory.getAbsolutePath())) {
      loadDirectory(directory, true);
    }
    else {
      loadDirectory(directory, false);
    }
  }

  /**
   **********************************************************************************************
   * Makes a fresh root and calls reloadTree()
   **********************************************************************************************
   **/
  public void loadDirectory(File directory, boolean rememberSelections) {
    if (!directory.exists()) {
      return;
    }

    File archiveFile = null;

    if (!directory.isDirectory()) {
      if (directory.isFile()) {
        // Try to load the archive referenced by this file
        archiveFile = directory;

        // Also get the parent directory and load it into the DirectoryList
        directory = directory.getParentFile();

      }
    }

    // if the directory hasn't changed, we want to re-select all the items that are selected,
    // after the refresh has happened
    /*
     * File[] selectedFiles = new File[0]; if (getCurrentDirectory().equals(directory)){
     * selectedFiles = getAllSelectedFiles(); }
     */

    currentDirectory = directory;

    /*
     * if (rememberSelections){ System.out.println("REMEMBER"); } else {
     * System.out.println("FORGET"); } try { throw new Exception(); } catch (Throwable t){
     * t.printStackTrace(); }
     */

    // sets the statics in the renderer so that it paints the list quickly
    new DirectoryListDirectoryListCellRenderer();
    new DirectoryListDrivesComboBoxCellRenderer();

    Task_ReloadDirectoryList reloadTask = new Task_ReloadDirectoryList(directory, filter, list, rememberSelections);
    reloadTask.setDirection(Task.DIRECTION_REDO);
    new Thread(reloadTask).start();
    //task.run();

    /*
     * DefaultListModel model = new DefaultListModel();
     *
     *
     * File[] files = directory.listFiles(filter);
     *
     * if (files == null){ return; }
     *
     * for (int i=0;i<files.length;i++){ if (files[i].isDirectory()){ model.addElement(files[i]);
     * } }
     *
     * for (int i=0;i<files.length;i++){ if (! files[i].isDirectory()){
     * model.addElement(files[i]); } }
     *
     *
     * reloadList(model);
     *
     *
     * setSelectedFiles(selectedFiles);
     */

    // now open the archive, if the user entered a filename of a file
    if (archiveFile != null) {
      Task_ReadArchive readTask = new Task_ReadArchive(archiveFile);
      readTask.setDirection(Task.DIRECTION_REDO);
      new Thread(readTask).start();
    }

  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  @Override
  public boolean onClick(JComponent c, java.awt.event.MouseEvent e) {

    if (c == upButton) {
      changeToParent();
      return true;
    }
    else if (c instanceof WSMenuItem) {
      String code = ((WSMenuItem) c).getCode();

      if (code.equals("DirectoryList_RightClick_Refresh")) {
        changeDirectory(currentDirectory);
      }
      else if (code.equals("DirectoryList_RightClick_ParentDirectory")) {
        changeToParent();
      }
      else if (code.equals("DirectoryList_RightClick_OpenDirectory")) {
        File directory = (File) list.getSelectedValue();
        changeDirectory(directory);
      }
      else {
        File file = (File) list.getSelectedValue();
        SidePanel_DirectoryList dirList = (SidePanel_DirectoryList) ComponentRepository.get("SidePanel_DirectoryList");

        if (code.equals("DirectoryList_RightClick_ReadArchive_Normal")) {
          dirList.changeControls("ReadPanel", false);
          dirList.readArchive(file);
        }
        else if (code.equals("DirectoryList_RightClick_ReadArchive_OpenWith")) {
          dirList.changeControls("ReadPanel", false);
          dirList.readArchive(file);
        }
        else if (code.equals("DirectoryList_RightClick_ReadArchive_Scanner")) {
          dirList.changeControls("ReadPanel", false);
          dirList.scanArchive(file);
        }
        else if (code.equals("DirectoryList_RightClick_ReadArchive_Script")) {
          dirList.changeControls("ScriptPanel", false);
          dirList.readScriptArchive(file);
        }
        else {
          return false;
        }
      }
      return true;
    }

    return false;
  }

  /**
   **********************************************************************************************
   * Creates a TransferHandler for this component, allowing it to be dragged.
   * @param c the component that will be dragged
   * @param e the dragging event
   * @return the TransferHandler for this component
   **********************************************************************************************
   **/
  @Override
  public TransferHandler onDrag(JComponent c, MouseEvent e) {
    return null;
  }

  /**
   **********************************************************************************************
   * Drops the transferable object from the component
   * @param t the transferred data
   * @return true if the event was handled
   **********************************************************************************************
   **/
  @Override
  public boolean onDrop(Transferable t) {
    return true;
  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  @SuppressWarnings("rawtypes")
  @Override
  public boolean onKeyPress(JComponent c, KeyEvent e) {
    if (c == list) {

      // check for tthe BACKSPACE or ENTER keys
      int keyType = e.getKeyCode();
      if (keyType == KeyEvent.VK_BACK_SPACE) {
        // go to the parent directory
        changeToParent();
        return true;
      }
      else if (keyType == KeyEvent.VK_ENTER) {
        // enter a directory, or open the file
        File selectedFile = (File) list.getSelectedValue();
        if (selectedFile == null) {
          return false;
        }
        else if (selectedFile.isDirectory()) {
          //loadDirectory(selectedFile);
          changeDirectory(selectedFile);
        }
        else {
          // trigger a double-click (which is passed to the SidePanel_DirectoryList for processing normally)
          ((WSPanel) getParent()).processEvent(new MouseEvent(list, MouseEvent.MOUSE_CLICKED, 0, 0, 0, 0, 2, false, MouseEvent.BUTTON1));
        }
        return true;
      }

      // move to the next file starting with this letter

      char keyCode = e.getKeyChar();
      if (keyCode == KeyEvent.CHAR_UNDEFINED) {
        return false; // not a letter or number
      }
      keyCode = ("" + keyCode).toLowerCase().charAt(0);
      char keyCodeCaps = ("" + keyCode).toUpperCase().charAt(0);

      ListModel model = list.getModel();

      int numFiles = model.getSize();
      int selectedIndex = list.getSelectedIndex() + 1;

      if (selectedIndex >= numFiles) {
        selectedIndex = 0;
      }

      // search the bottom half of the list
      for (int i = selectedIndex; i < numFiles; i++) {
        String filename = ((File) model.getElementAt(i)).getName();
        char currentChar = filename.charAt(0);
        if (currentChar == keyCode || currentChar == keyCodeCaps) {
          list.setSelectedIndex(i);
          list.ensureIndexIsVisible(i);
          return false; // still want to pass the event through anyway
        }
      }

      if (selectedIndex == 0) {
        // we started searching from the start of the list, so we don't want to re-search
        return false;
      }

      //  search the top half of the list, if not found in the bottom half.
      for (int i = 0; i <= selectedIndex; i++) {
        String filename = ((File) model.getElementAt(i)).getName();
        char currentChar = filename.charAt(0);
        if (currentChar == keyCode || currentChar == keyCodeCaps) {
          list.setSelectedIndex(i);
          list.ensureIndexIsVisible(i);
          return false; // still want to pass the event through anyway
        }
      }

    }

    return false;

  }

  /**
   **********************************************************************************************
   * The event that is triggered from a WSRightClickableListener when a right click occurs
   * @param c the component that triggered the event
   * @param e the event that occurred
   **********************************************************************************************
   **/
  @Override
  public boolean onRightClick(JComponent c, java.awt.event.MouseEvent e) {
    if (c == list) {
      Point p = e.getPoint();

      list.setSelectedIndex(list.locationToIndex(p));
      File selectedFile = (File) list.getSelectedValue();
      if (selectedFile == null) {
        return false;
      }

      WSPopupMenu menu = new WSPopupMenu(XMLReader.read("<WSPopupMenu></WSPopupMenu>"));

      if (selectedFile.isDirectory()) {
        // directory
        menu.add(new WSMenuItem(XMLReader.read("<WSMenuItem code=\"DirectoryList_RightClick_OpenDirectory\" />")));
        menu.add(new WSMenuItem(XMLReader.read("<WSMenuItem code=\"DirectoryList_RightClick_ParentDirectory\" />")));
        menu.add(new WSMenuItem(XMLReader.read("<WSMenuItem code=\"DirectoryList_RightClick_Refresh\" />")));
      }
      else {
        // file
        menu.add(new WSMenuItem(XMLReader.read("<WSMenuItem code=\"DirectoryList_RightClick_ReadArchive_Normal\" />")));
        menu.add(new WSMenuItem(XMLReader.read("<WSMenuItem code=\"DirectoryList_RightClick_ReadArchive_OpenWith\" />")));
        if (GameExtractor.isFullVersion()) {
          menu.add(new WSMenuItem(XMLReader.read("<WSMenuItem code=\"DirectoryList_RightClick_ReadArchive_Scanner\" />")));
        }
        menu.add(new WSMenuItem(XMLReader.read("<WSMenuItem code=\"DirectoryList_RightClick_ReadArchive_Script\" />")));
        menu.add(new WSPopupMenuSeparator(XMLReader.read("<WSPopupMenuSeparator />")));
        menu.add(new WSMenuItem(XMLReader.read("<WSMenuItem code=\"DirectoryList_RightClick_ParentDirectory\" />")));
        menu.add(new WSMenuItem(XMLReader.read("<WSMenuItem code=\"DirectoryList_RightClick_Refresh\" />")));
      }

      menu.show(list, (int) p.getX() - 10, (int) p.getY() - 10);

      return true;
    }

    return false;
  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public void rebuildList(File directory) {
    loadDirectory(directory);
  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  @Override
  public void reload() {
    Object selectedItem = drives.getSelectedItem();
    if (selectedItem instanceof File) {
      rebuildList((File) selectedItem);
    }
    else if (selectedItem instanceof String) {
      rebuildList(new File((String) selectedItem));
    }
  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  @SuppressWarnings({ "unchecked", "rawtypes" })
  public void reloadList(DefaultListModel model) {
    list.setModel(model);
  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  @Override
  public void requestFocus() {
    list.requestFocus();
  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  @Override
  public void scrollToSelected() {
    int height = list.getHeight();
    int cellHeight = list.getFixedCellHeight();
    list.setVisibleRowCount(height / cellHeight);
  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  @Override
  public void setMatchFilter(FileFilter filter) {
    if (filter != this.filter) {
      this.filter = filter;
    }
  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  @Override
  public void setMultipleSelection(boolean multi) {
    if (multi) {
      list.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
    }
    else {
      list.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
    }
  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  @SuppressWarnings("rawtypes")
  public void setSelectedFiles(File[] files) {
    if (files.length <= 0) {
      return;
    }

    File currentSelection = files[0];

    DefaultListModel model = (DefaultListModel) list.getModel();
    int numInModel = model.getSize();

    if (files.length > 1) {
      // select all other files first

      // first, stop the current file from being selected
      int numSelected = files.length - 1;
      files[0] = files[numSelected];

      // now look for all other files
      for (int i = 0; i < numInModel; i++) {
        for (int j = 0; j < numSelected; j++) {
          if (model.getElementAt(i).equals(files[j])) {
            // found a file that needs to be selected
            list.addSelectionInterval(i, i);

            // now reshuffle the files[] array so that it is smaller to read through (quicker)
            numSelected--;
            files[j] = files[numSelected];

            // finish this loop
            j = numSelected;
          }
        }
      }
    }

    // now select the current file
    for (int i = 0; i < numInModel; i++) {
      if (model.getElementAt(i).equals(currentSelection)) {
        // found the currently selected file
        list.addSelectionInterval(i, i);
      }
    }

  }

}