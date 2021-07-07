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
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.io.File;
import org.watto.ErrorLogger;
import org.watto.Language;
import org.watto.Settings;
import org.watto.TypecastSingletonManager;
import org.watto.datatype.Archive;
import org.watto.datatype.Resource;
import org.watto.ge.GameExtractor;
import org.watto.ge.plugin.ArchivePlugin;
import org.watto.task.Task;
import org.watto.task.Task_AddFiles;
import org.watto.task.Task_ReadArchive;
import org.watto.task.Task_ReplaceMatchingFiles;
import org.watto.task.Task_ReplaceSelectedFiles;
import org.watto.xml.XMLReader;

public abstract class FileListPanel extends WSPanelPlugin {// implements WSDropableInterface{

  /** serialVersionUID */
  private static final long serialVersionUID = 1L;

  /** Selections are for rows only **/
  public static int SELECTION_TYPE_ROW_ONLY = 1;

  /** Selections are for rows and columns - ie individual cell selection **/
  public static int SELECTION_TYPE_ROW_COLUMN = 2;

  int selectionType = SELECTION_TYPE_ROW_ONLY;

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public FileListPanel(String name) {
    super();
    setCode(name);
    setLayout(new BorderLayout(2, 2));
    constructInterface();
  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public abstract void addFilesFromDrop();

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public void addFilesFromDrop(File[] dropFiles) {
    Task_AddFiles task = new Task_AddFiles(dropFiles);
    task.setDirection(Task.DIRECTION_REDO);
    new Thread(task).start();
    TypecastSingletonManager.getTaskManager("TaskManager").add(task);
  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public void changeSelection(int row) {
  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public void changeSelection(int row, int column) {
  }

  /**
   **********************************************************************************************
   * @return true if an operation was handled, false if to show the menu
   **********************************************************************************************
   **/
  public boolean checkDragDropOption() {
    String dragOperation = Settings.get("DragDropOption");

    if (dragOperation.equals("DragDrop_Add")) {
      // check that the archive is writable.
      // if not, show the menu instead.
      ArchivePlugin plugin = Archive.getReadPlugin();
      if (plugin != null) {
        if (!plugin.canWrite()) {
          // archive is not writable - replace only
          return false;
        }
      }

      addFilesFromDrop();
    }
    else if (dragOperation.equals("DragDrop_ReplaceCurrent")) {
      replaceCurrentFileFromDrop();
    }
    else if (dragOperation.equals("DragDrop_ReplaceMatching")) {
      replaceMatchingFilesFromDrop();
    }
    else if (dragOperation.equals("DragDrop_ReadArchive")) {
      readArchiveFromDrop();
    }
    else {
      //show menu
      return false;
    }
    return true;
  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public abstract void constructInterface();

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public abstract void dropFiles(File[] files);

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  @Override
  public String getDescription() {

    String description = toString() + "\n\n" + Language.get("Description_FileListPanel");

    if (!isEnabled()) {
      description += "\n\n" + Language.get("Description_PluginDisabled");
    }
    else {
      description += "\n\n" + Language.get("Description_PluginEnabled");
    }

    return description;

  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public WSPopupMenu getDropFilesMenu() {
    return new WSPopupMenu(XMLReader.read("<WSPopupMenu><WSMenuItem code=\"FileListDrop_ReadArchive\" /></WSPopupMenu>"));
  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public abstract int getFirstSelectedRow();

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public abstract int getNumSelected();

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public Resource getResource(int row) {
    return null;
  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public Resource getResource(int row, int column) {
    return null;
  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public WSPopupMenu getRightClickMenu() {
    WSPopupMenu menu = new WSPopupMenu(XMLReader.read("<WSPopupMenu></WSPopupMenu>"));
    menu.add(new WSMenuItem(XMLReader.read("<WSMenuItem code=\"FileList_RightClick_ExtractResources_Selected\" />")));
    menu.add(new WSMenuItem(XMLReader.read("<WSMenuItem code=\"FileList_RightClick_ExtractResources_All\" />")));
    menu.add(new WSPopupMenuSeparator(XMLReader.read("<WSPopupMenuSeparator />")));
    menu.add(new WSMenuItem(XMLReader.read("<WSMenuItem code=\"FileList_RightClick_PreviewResource\" />")));
    menu.add(new WSMenuItem(XMLReader.read("<WSMenuItem code=\"FileList_RightClick_HexEditor\" />")));
    menu.add(new WSMenuItem(XMLReader.read("<WSMenuItem code=\"FileList_RightClick_ImageInvestigator\" />")));
    menu.add(new WSMenuItem(XMLReader.read("<WSMenuItem code=\"FileList_RightClick_MeshInvestigator\" />")));
    if (GameExtractor.isFullVersion()) {
      menu.add(new WSPopupMenuSeparator(XMLReader.read("<WSPopupMenuSeparator />")));
      menu.add(new WSMenuItem(XMLReader.read("<WSMenuItem code=\"FileList_RightClick_RemoveResources\" />")));
      menu.add(new WSMenuItem(XMLReader.read("<WSMenuItem code=\"FileList_RightClick_RenameResources\" />")));
    }
    menu.add(new WSPopupMenuSeparator(XMLReader.read("<WSPopupMenuSeparator />")));
    menu.add(new WSMenu(XMLReader.read("<WSMenu code=\"FileList_RightClick_SelectResources\"><WSMenuItem code=\"FileList_RightClick_SelectResources_All\" /><WSMenuItem code=\"FileList_RightClick_SelectResources_None\" /><WSMenuItem code=\"FileList_RightClick_SelectResources_Inverse\" /></WSMenu>")));
    menu.add(new WSPopupMenuSeparator(XMLReader.read("<WSPopupMenuSeparator />")));
    menu.add(new WSMenu(XMLReader.read("<WSMenu code=\"FileList_RightClick_FileListView\"><WSMenuItem code=\"FileList_RightClick_FileListView_Table\" /><WSMenuItem code=\"FileList_RightClick_FileListView_Tree\" /><WSMenuItem code=\"FileList_RightClick_FileListView_TreeTable\" /><WSMenuItem code=\"FileList_RightClick_FileListView_Thumbnails\" /></WSMenu>")));
    return menu;
  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public abstract Resource[] getSelected();

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public int getSelectionType() {
    return selectionType;
  }

  /**
   **********************************************************************************************
   * Drops the transferable object from the component
   * @param t the transferred data
   * @return true if the event was handled
   **********************************************************************************************
   **/
  @SuppressWarnings({ "rawtypes", "unchecked" })
  public boolean onDrop(Transferable t) {
    try {

      /*
      try {
        new FullVersionVerifier();
      }
      catch (Throwable t3) {
        // don't allow drag-drop for the Basic version
        WSPopup.showErrorInNewThread("FullVersionOnly", true);
        return true;
      }
      */

      ArchivePlugin plugin = Archive.getReadPlugin();
      if (plugin != null) {
        if (!plugin.canReplace() && !plugin.canWrite()) {
          // archive is not modifiable
          WSPopup.showErrorInNewThread("ModifyArchive_NotReplacable", true);
          return true;
        }
      }

      DataFlavor flav = DataFlavor.javaFileListFlavor;
      if (t.isDataFlavorSupported(flav)) {
        // transfer of files from the O/S
        java.util.List data = (java.util.List) t.getTransferData(flav);
        File[] files = (File[]) data.toArray(new File[0]);
        dropFiles(files);
        return true;
      }

      flav = DataFlavor.stringFlavor;
      if (t.isDataFlavorSupported(flav)) {
        // transfer of filenames from the DirList
        String data = (String) t.getTransferData(flav);
        String[] paths = data.split("\n");
        File[] files = new File[paths.length];
        for (int i = 0; i < paths.length; i++) {
          files[i] = new File(paths[i]);
        }
        dropFiles(files);
        return true;
      }

    }
    catch (Throwable t2) {
      ErrorLogger.log(t2);
    }

    /*
     * DataFlavor[] flav = t.getTransferDataFlavors(); for (int i=0;i<flav.length;i++){ try {
     * System.out.println(flav[i]); System.out.println(t.getTransferData(flav[i]));
     * System.out.println("----------"); } catch (Throwable t2){ } }
     */
    return true;
  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public abstract void readArchiveFromDrop();

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public void readArchiveFromDrop(File[] dropFiles) {
    if (dropFiles == null || dropFiles.length <= 0) {
      return;
    }
    Task_ReadArchive task = new Task_ReadArchive(dropFiles[0]);
    task.setDirection(Task.DIRECTION_REDO);
    new Thread(task).start();
  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public abstract void reload();

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public abstract void replaceCurrentFileFromDrop();

  /**
   **********************************************************************************************
   * Replace the current hovered file
   **********************************************************************************************
   **/
  public void replaceCurrentFileFromDrop(Resource resourceToReplace, File newFile) {
    Task_ReplaceSelectedFiles task = new Task_ReplaceSelectedFiles(resourceToReplace, newFile);
    task.setDirection(Task.DIRECTION_REDO);
    new Thread(task).start();
    TypecastSingletonManager.getTaskManager("TaskManager").add(task);
  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public abstract void replaceMatchingFilesFromDrop();

  /**
   **********************************************************************************************
   * Replace matching files
   **********************************************************************************************
   **/
  public void replaceMatchingFilesFromDrop(Resource[] resourcesToReplace, File newFileDirectory) {
    Task_ReplaceMatchingFiles task = new Task_ReplaceMatchingFiles(resourcesToReplace, newFileDirectory);
    task.setDirection(Task.DIRECTION_REDO);
    new Thread(task).start();
    TypecastSingletonManager.getTaskManager("TaskManager").add(task);
  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public abstract void selectAll();

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public abstract void selectInverse();

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public abstract void selectNone();

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public void selectResource(int row) {
  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public void selectResource(int row, int column) {
  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public void setSelectionType(int selectionType) {
    this.selectionType = selectionType;
  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public void stopInlineEditing() {
  }

  /**
   **********************************************************************************************
   * Gets the name of the plugin
   * @return the name
   **********************************************************************************************
   **/
  @Override
  public String toString() {
    String nameCode = "FileListPanel_" + code + "_Name";
    if (Language.has(nameCode)) {
      return Language.get(nameCode);
    }
    return code;
  }

}