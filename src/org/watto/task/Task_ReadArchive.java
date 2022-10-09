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

import java.io.File;
import javax.swing.SwingUtilities;
import org.watto.ChangeMonitor;
import org.watto.ErrorLogger;
import org.watto.Language;
import org.watto.Settings;
import org.watto.SingletonManager;
import org.watto.TypecastSingletonManager;
import org.watto.component.ComponentRepository;
import org.watto.component.WSFileListPanelHolder;
import org.watto.component.WSPluginGroup;
import org.watto.component.WSPluginManager;
import org.watto.component.WSPopup;
import org.watto.component.WSSidePanelHolder;
import org.watto.ge.GameExtractor;
import org.watto.ge.plugin.ArchivePlugin;
import org.watto.ge.plugin.PluginFinder;
import org.watto.ge.plugin.RatedPlugin;
import org.watto.ge.script.ScriptManager;
import org.watto.io.FileCopier;
import org.watto.io.FilenameSplitter;

/**
**********************************************************************************************

**********************************************************************************************
**/
public class Task_ReadArchive extends AbstractTask {

  /** The direction to perform in the thread **/
  int direction = 1;

  File path;

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public Task_ReadArchive(File path) {
    this.path = path;
  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  @Override
  public void redo() {
    if (!TaskProgressManager.canDoTask()) {
      return;
    }

    if (path == null || !path.exists()) {
      WSPopup.showError("ReadArchive_FilenameMissing", true);
      TaskProgressManager.stopTask();
      return;
    }

    if (path.isDirectory()) {
      WSPopup.showError("ReadArchive_FileNotAnArchive", true);
      TaskProgressManager.stopTask();
      return;
    }

    // ask to save the modified archive
    if (GameExtractor.getInstance().promptToSave()) {
      return;
    }
    ChangeMonitor.reset();

    // Progress dialog

    TaskProgressManager.show(1, 0, Language.get("Progress_ReadingArchive"));
    TaskProgressManager.startTask();

    RatedPlugin[] plugins;

    //ArchivePlugin selectedPlugin;
    // auto-detect a plugin for this archive
    plugins = PluginFinder.findPlugins(path, ArchivePlugin.class);
    if (plugins == null || plugins.length == 0) {

      // See if it's a BMS script (and add it to GE)
      boolean wasBMS = checkForBMS(path);
      if (wasBMS) {
        return;
      }

      if (Settings.getBoolean("ScanFileIfOpenFailed")) {
        // Run the FormatScanner
        if (GameExtractor.isFullVersion()) {
          scanArchive(path);
          return;
        }
      }

      WSPopup.showError("ReadArchive_NoPluginsFound", true);
      TaskProgressManager.stopTask();
      return;
    }

    java.util.Arrays.sort(plugins);

    //Archive.makeNewArchive();
    //if (ArchiveModificationMonitor.isModified()){
    //  // The user is saving, so do not continue with this method
    //  return;
    //  }

    // try to open the archive using each plugin and openArchive(File,Plugin)
    boolean archiveOpened = false;

    //String oldCurrentArchive = Settings.getString("CurrentArchive");
    Settings.set("CurrentArchive", path.getAbsolutePath());

    for (int i = 0; i < plugins.length; i++) {
      //System.out.println(plugins[i].getRating());

      // true, so it knows it is started within a current task
      Task_ReadArchiveWithPlugin task = new Task_ReadArchiveWithPlugin(path, plugins[i].getPlugin(), true);
      task.redo();
      archiveOpened = task.getResult();

      if (archiveOpened) {
        i = plugins.length;
        TypecastSingletonManager.getRecentFilesManager("RecentFilesManager").addRecentFile(path);
      }
      else {
      }

    }

    //((FileListPanel) ((WSFileListPanelHolder) ComponentRepository.get("FileListPanelHolder")).getCurrentPanel()).reload();
    //((WSFileListPanelHolder) ComponentRepository.get("FileListPanelHolder")).reload();
    //((WSFileListPanelHolder) ComponentRepository.get("FileListPanelHolder")).requestFocus();

    if (!archiveOpened) {
      // See if it's a BMS script (and add it to GE)
      boolean wasBMS = checkForBMS(path);
      if (wasBMS) {
        return;
      }

      if (Settings.getBoolean("ScanFileIfOpenFailed")) {
        // Run the FormatScanner
        if (GameExtractor.isFullVersion()) {
          scanArchive(path);
          return;
        }
      }

      // Also shows this message if the scanner does not exist!
      WSPopup.showError("ReadArchive_NoPluginsFound", true);
      TaskProgressManager.stopTask();
      return;
      //GameExtractor.getInstance().setPluginAllowedEvent();
    }
    else {
      //GameExtractor.getInstance().setPluginAllowedEvent();
    }

    //SidePanel_DirectoryList panel = (SidePanel_DirectoryList)ComponentRepository.get("SidePanel_DirectoryList");
    //panel.onOpenRequest();

    WSSidePanelHolder sidePanelHolder = (WSSidePanelHolder) ComponentRepository.get("SidePanelHolder");
    if (!sidePanelHolder.getCurrentPanelCode().equals("SidePanel_DirectoryList")) {
      sidePanelHolder.reloadPanel();
    }

    TaskProgressManager.stopTask();

    Task_ReloadFileListPanel task = new Task_ReloadFileListPanel((WSFileListPanelHolder) ComponentRepository.get("FileListPanelHolder"));
    task.setDirection(Task.DIRECTION_REDO);
    SwingUtilities.invokeLater(task);

    WSPopup.showMessage("ReadArchive_ArchiveOpened", true);

    if (SingletonManager.has("BulkExport_KeepTempFiles")) {
      // we need to keep the temporary files around, as some of them have been exported already (eg for canScanForFileTypes())
      SingletonManager.remove("BulkExport_KeepTempFiles");
    }
    else {
      GameExtractor.deleteTempFiles(new File("temp"));
    }

  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public void scanArchive(File path) {
    // true, so it knows it is started within a current task
    Task_ScanArchive task = new Task_ScanArchive(path, true);
    task.redo();
    TaskProgressManager.stopTask();
  }

  /**
  **********************************************************************************************
  Looks at the file, and if it's a BMS file, it asks to add it to GE.
  return <b>true</b> if added, <b>false</b> in all other cases
  **********************************************************************************************
  **/
  @SuppressWarnings("unlikely-arg-type")
  public boolean checkForBMS(File path) {

    // If this is a *.bms file, check whether it's a script or not.
    // If it is, ask if the user wants to use it, and if so, copy the file into the scripts directory.
    try {
      if (FilenameSplitter.getExtension(path).equalsIgnoreCase("bms")) {
        int scriptType = ScriptManager.analyseBMS(path);
        if (scriptType != ScriptManager.SCRIPT_UNKNOWN) {
          // a valid script (QuickBMS or MexCom)

          String buttonClicked = WSPopup.showConfirm("AddBMSOnDoubleClick", true);
          if (buttonClicked.equals(WSPopup.BUTTON_OK) || buttonClicked.equals(WSPopup.BUTTON_YES)) {
            // BUTTON_OK is returned if the popup is auto-hidden, BUTTON_YES is returned if the user clicks it.

            // Copy the BMS file to the scripts directory
            File scriptsDirectory = new File(Settings.getString("ScriptsDirectory"));
            if (path.equals(scriptsDirectory) || path.getParent().equals(scriptsDirectory)) {
              // already in the scripts directory
            }
            else {
              // copy the file to the scripts directory
              File outputFile = new File(scriptsDirectory.getAbsolutePath() + File.separatorChar + path.getName());
              if (!outputFile.exists()) {
                FileCopier.copy(path, outputFile);
              }
              path = outputFile; // use the new file from now on
            }

            // Add the script file to the group
            WSPluginGroup group = WSPluginManager.getGroup("Script");
            if (group == null) { // because we haven't loaded any scripts yet
              WSPluginManager.addGroup("Script");
              group = WSPluginManager.getGroup("Script");
              if (group == null) {
                // could not create the group for some reason
                return false;
              }
            }
            String name = FilenameSplitter.getFilename(path);
            ScriptManager.analyseBMS(path, name, group);

            // don't want to scan the file (it's not an archive), we already added it as a script
            TaskProgressManager.stopTask();
            return true;
          }

        }
      }
    }
    catch (Throwable t) {
      ErrorLogger.log(t);
    }
    return false;
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
    if (!TaskProgressManager.canDoTask()) {
      return;
    }
  }

}
