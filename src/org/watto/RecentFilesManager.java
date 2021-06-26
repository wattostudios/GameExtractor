////////////////////////////////////////////////////////////////////////////////////////////////
//                                                                                            //
//                                       WATTO STUDIOS                                        //
//                             Java Code, Programs, and Software                              //
//                                    http://www.watto.org                                    //
//                                                                                            //
//                           Copyright (C) 2004-2010  WATTO Studios                           //
//                                                                                            //
// This program is free software; you can redistribute it and/or modify it under the terms of //
// the GNU General Public License published by the Free Software Foundation; either version 2 //
// of the License, or (at your option) any later versions. This program is distributed in the //
// hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranties //
// of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License //
// at http://www.gnu.org for more details. For updates and information about this program, go //
// to the WATTO Studios website at http://www.watto.org or email watto@watto.org . Thanks! :) //
//                                                                                            //
////////////////////////////////////////////////////////////////////////////////////////////////

package org.watto;

import java.io.File;
import org.watto.event.WSEvent;
import org.watto.event.WSEventableInterface;

/***********************************************************************************************
 * Maintains a list of the most recent <code>File</code>s
 ***********************************************************************************************/
public class RecentFilesManager {

  /** The recent files **/
  String[] recentFiles = new String[0];

  /** Monitors that should be triggered when the recent files list is changed **/
  WSEventableInterface[] monitors = new WSEventableInterface[0];

  /***********************************************************************************************
   * Loads the recent files from the <code>Settings</code>
   ***********************************************************************************************/
  public RecentFilesManager() {
    loadRecentFiles();
  }

  /***********************************************************************************************
   * Adds a <code>monitor</code> that listens for <code>WSEvent.RECENT_FILES_CHANGED</code>
   * events.
   * @param monitor the monitor to add
   ***********************************************************************************************/
  public void addMonitor(WSEventableInterface monitor) {
    int numMonitors = monitors.length;
    monitors = resize(monitors, numMonitors + 1);
    monitors[numMonitors] = monitor;
  }

  /***********************************************************************************************
   * Adds a <code>File</code> to the recent files list
   * @param path the file to add
   ***********************************************************************************************/
  public void addRecentFile(File path) {
    try {

      String filePath = path.getAbsolutePath();

      int numRecentFiles = recentFiles.length;
      for (int i = 0; i < numRecentFiles; i++) {
        if (recentFiles[i].equals(filePath)) {
          // This file is already in the recent files list,
          // so just move it forward instead of shuffling the
          // entire array
          System.arraycopy(recentFiles, 0, recentFiles, 1, i);
          recentFiles[0] = filePath;

          updateSettings();
          fireRecentFilesChanged();
          return;
        }
      }

      // this file isn't already in the recent files list,
      // so move all the recent files down 1 and add the
      // new file to the top of the list.
      System.arraycopy(recentFiles, 0, recentFiles, 1, numRecentFiles - 1);
      recentFiles[0] = filePath;

      updateSettings();
      fireRecentFilesChanged();

    }
    catch (Throwable t) {
      ErrorLogger.log(t);
    }
  }

  /***********************************************************************************************
   * Triggers a <code>WSEvent</code>. Alerts all monitors that the event has occurred.
   * @param event the event that was triggered.
   ***********************************************************************************************/
  void fireEvent(WSEvent event) {
    int numMonitors = monitors.length;
    for (int i = 0; i < numMonitors; i++) {
      monitors[i].onEvent(event.getComponent(), event, event.getType());
    }
  }

  /***********************************************************************************************
   * Triggers a <code>WSEvent.RECENT_FILES_CHANGED</code> event
   ***********************************************************************************************/
  public void fireRecentFilesChanged() {
    fireEvent(new WSEvent(this, WSEvent.RECENT_FILES_CHANGED));
  }

  /***********************************************************************************************
   * Loads the recent files list from the <code>Settings</code>
   ***********************************************************************************************/
  public void loadRecentFiles() {
    try {

      int numRecentFiles = Settings.getInt("NumberOfRecentFiles");
      setRecentFileCount(numRecentFiles);

      for (int i = 0; i < numRecentFiles; i++) {
        String recentFile = Settings.getString("RecentFile" + (i + 1)); // +1 because the 1st recent
        // file is #1, not #0
        recentFiles[i] = recentFile;
      }

      fireRecentFilesChanged();

    }
    catch (Throwable t) {
      ErrorLogger.log(t);
    }
  }

  /***********************************************************************************************
   * Removes a <code>WSEventableInterface</code> event monitor
   * @param monitor the monitor to remove
   ***********************************************************************************************/
  public void removeMonitor(WSEventableInterface monitor) {
    int numMonitors = monitors.length;
    for (int i = 0; i < numMonitors; i++) {
      if (monitors[i] == monitor) {
        // found the monitor, so lets remove it
        int numRemaining = numMonitors - i - 1;
        System.arraycopy(monitors, i + 1, monitors, i, numRemaining);
        monitors = resize(monitors, numMonitors - 1);
        return;
      }
    }
  }

  /***********************************************************************************************
   * Resizes the <code>source</code array of type <code>WSEventableInterface</code> to a new size
   * @param source the source <code>WSEventableInterface</code> array
   * @param newSize the new size of the array
   * @return the new array
   ***********************************************************************************************/
  WSEventableInterface[] resize(WSEventableInterface[] source, int newSize) {
    int copySize = source.length;
    if (newSize < copySize) {
      copySize = newSize;
    }

    WSEventableInterface[] target = new WSEventableInterface[newSize];
    System.arraycopy(source, 0, target, 0, copySize);

    return target;
  }

  /***********************************************************************************************
   * Sets the number of <code>File</code>s allowed in the recent file list
   * @param newRecentFileCount the number of files to remember
   ***********************************************************************************************/
  public void setRecentFileCount(int newRecentFileCount) {
    try {

      if (newRecentFileCount < 0) {
        newRecentFileCount = 0;
      }

      int oldRecentFileCount = recentFiles.length;

      if (recentFiles == null || oldRecentFileCount <= 0) {
        recentFiles = new String[newRecentFileCount];
        return;
      }

      if (oldRecentFileCount == newRecentFileCount) {
        return;
      }

      int sizeToCopy = oldRecentFileCount;
      if (newRecentFileCount < oldRecentFileCount) {
        sizeToCopy = newRecentFileCount;
      }

      String[] oldRecentFiles = recentFiles;
      recentFiles = new String[newRecentFileCount];
      System.arraycopy(oldRecentFiles, 0, recentFiles, 0, sizeToCopy);

    }
    catch (Throwable t) {
      ErrorLogger.log(t);
    }
  }

  /***********************************************************************************************
   * Stores all the recent files in <code>Settings</code>
   ***********************************************************************************************/
  public void updateSettings() {
    try {

      int numRecentFiles = recentFiles.length;

      for (int i = 0; i < numRecentFiles; i++) {
        Settings.set("RecentFile" + (i + 1), recentFiles[i]); // +1 because the 1st recent file is
        // #1, not #0
      }

    }
    catch (Throwable t) {
      ErrorLogger.log(t);
    }
  }

}