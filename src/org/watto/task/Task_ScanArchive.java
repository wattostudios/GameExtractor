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
import org.watto.ChangeMonitor;
import org.watto.ErrorLogger;
import org.watto.Language;
import org.watto.Settings;
import org.watto.TypecastSingletonManager;
import org.watto.component.ComponentRepository;
import org.watto.component.FileListPanel;
import org.watto.component.WSFileListPanelHolder;
import org.watto.component.WSPopup;
import org.watto.datatype.Archive;
import org.watto.datatype.Resource;
import org.watto.ge.GameExtractor;
import org.watto.ge.plugin.PluginListBuilder;
import org.watto.ge.plugin.ScannerPlugin;
import org.watto.io.FileManipulator;

/**
**********************************************************************************************

**********************************************************************************************
**/
public class Task_ScanArchive extends AbstractTask {

  /** The direction to perform in the thread **/
  int direction = 1;

  File path;

  // is this called from within an existing thread?
  // if so, don't call TaskManager or set up the WSProgressBar
  boolean withinThread = false;

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public Task_ScanArchive(File path) {
    this(path, false);
  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public Task_ScanArchive(File path, boolean withinThread) {
    this.path = path;
    this.withinThread = withinThread;
  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  @Override
  public void redo() {
    if (!withinThread && !TaskProgressManager.canDoTask()) {
      return;
    }

    //if (ArchiveModificationMonitor.isModified()){
    //  // The user is saving, so do not continue with this method
    //  return;
    //  }
    //GameExtractor.getInstance().setPluginAllowedEvent();

    // Load the progress dialog
    if (!withinThread) {
      // ask to save the modified archive
      if (GameExtractor.getInstance().promptToSave()) {
        return;
      }
      ChangeMonitor.reset();

      // Progress dialog
      TaskProgressManager.show(1, 0, Language.get("Progress_ScanningArchive"));

      TaskProgressManager.startTask();
    }

    TaskProgressManager.setMessage(Language.get("Progress_ScanningArchive"));

    if (path == null || !path.exists()) {
      if (!withinThread) {
        WSPopup.showError("ReadArchive_FilenameMissing", true);
      }
      return;
    }

    if (path.isDirectory()) {
      if (!withinThread) {
        WSPopup.showError("ReadArchive_FileNotAnArchive", true);
      }
      return;
    }

    ScannerPlugin[] scanners = PluginListBuilder.getEnabledScanners();

    Resource[] resources = new Resource[Settings.getInt("MaxNumberOfFiles4")];
    int numResources = 0;

    FileManipulator fm = new FileManipulator(path, false);
    try {
      long arcSize = fm.getLength();
      TaskProgressManager.setMaximum(arcSize);

      // for each byte of the input file
      while (fm.getOffset() < arcSize) {
        // read the next byte
        int b = fm.readByte();
        // record the offset so we can go back to the offset for each scanner
        long offset = fm.getOffset();
        TaskProgressManager.setValue((int) offset);

        // for each scanner
        for (int s = 0; s < scanners.length; s++) {
          try {
            // scan the current byte, and if successful continue scanning for a file
            // return a non-null resource if a file was found
            Resource resource = scanners[s].scan(b, fm);

            if (resource != null) {
              // set the remaining information for the resource
              resource.setSource(path);
              resource.setName(Resource.generateFilename(numResources) + resource.getName());

              // add the resource into the array
              resources[numResources] = resource;
              numResources++;
              s = scanners.length;
            }
            else {
              // on fail, go back to the offset
              fm.relativeSeek(offset);
            }
          }
          catch (Throwable t) {
            // scanner failed on this byte
            fm.relativeSeek(offset);
          }
        }

      }

    }
    catch (Throwable t) {
      ErrorLogger.log(t);
    }
    fm.close();

    boolean archiveOpened = false;
    if (numResources > 0) {
      Archive.makeNewArchive();
      //if (!ArchiveModificationMonitor.setModified(true)){
      //  return;
      //  }
      Archive.setResources(resources);
      Archive.resizeResources(numResources);
      Archive.setBasePath(path);

      TypecastSingletonManager.getRecentFilesManager("RecentFilesManager").addRecentFile(path);

      archiveOpened = true;
    }

    // Close the progress dialog
    if (!withinThread) {
      TaskProgressManager.stopTask();
    }

    ((FileListPanel) ((WSFileListPanelHolder) ComponentRepository.get("FileListPanelHolder")).getCurrentPanel()).reload();

    if (!archiveOpened) {
      WSPopup.showError("ReadArchive_ReadWithScannerFailed", true);
    }
    else {
      if (!withinThread) {
        WSPopup.showMessage("ReadArchive_ArchiveOpenedWithScanner", true);
      }
      Settings.set("CurrentArchive", path.getAbsolutePath());
    }

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
