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
import java.nio.file.FileAlreadyExistsException;
import org.watto.ChangeMonitor;
import org.watto.ErrorLogger;
import org.watto.Language;
import org.watto.TypecastSingletonManager;
import org.watto.component.ComponentRepository;
import org.watto.component.WSDirectoryListHolder;
import org.watto.component.WSPopup;
import org.watto.datatype.Archive;
import org.watto.ge.plugin.ArchivePlugin;

/**
**********************************************************************************************

**********************************************************************************************
**/
public class Task_WriteArchive extends AbstractTask {

  /** The direction to perform in the thread **/
  int direction = 1;

  File path;
  ArchivePlugin plugin;

  /**
  **********************************************************************************************

  **********************************************************************************************
  **/
  public Task_WriteArchive(File path, ArchivePlugin plugin) {
    this.path = path;
    this.plugin = plugin;
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

    // Progress dialog
    TaskProgressManager.show(2, 0, Language.get("Progress_WritingArchive")); // 2 progress bars
    TaskProgressManager.setIndeterminate(true, 0); // first 1 is indeterminate
    TaskProgressManager.setMaximum(Archive.getNumFiles(), 1); // second one shows how many files are done

    TaskProgressManager.startTask();

    boolean replacingCurrentArchive = false;
    if (Archive.getBasePath().equals(path)) {
      String confirmReplace = WSPopup.showConfirm("WriteArchive_OverwriteCurrentArchiveFile", false);

      if (confirmReplace.equals(WSPopup.BUTTON_YES)) {
        // we're overwriting the current archive. As most of the files will be sourced from the currant archive, we actually want to...
        // 1. Save the archive with a new name
        // 2. Delete the original archive
        // 3. Rename the new archive to the old name
        // 4. Reload the archive, as all the offsets/lengths would potentially have changed
        replacingCurrentArchive = true;
      }
      else {
        // don't do anything - the user needs to enter a new filename
        TaskProgressManager.stopTask();
        return;
      }

    }

    File desiredFile = path;
    String temporaryFilePath = path.getAbsolutePath() + ".temp";
    File temporaryFile = null;

    if (replacingCurrentArchive) {
      // generate a temporary name
      for (int i = 0; i < 10000; i++) {
        if (!new File(temporaryFilePath + i).exists()) {
          // found a suitable temporary filename
          temporaryFilePath += i;
          temporaryFile = new File(temporaryFilePath);
          break;
        }
      }

      if (temporaryFile == null) {
        // couldn't generate a temporary filename - just exit
        TaskProgressManager.stopTask();
        return;
      }

      // Set the path to the temporary filename, so we write into it
      path = temporaryFile;
    }

    if (Archive.getReadPlugin() == null) {
      // write from scratch
      plugin.write(Archive.getResources(), path);
    }
    else {
      // convert from another format (or modifying an existing archive)
      plugin.replace(Archive.getResources(), path);
    }

    if (replacingCurrentArchive) {
      // now that we've written into the temporary file, we need to remove the original file and rename the temporary file to the original filename
      try {
        boolean deleted = desiredFile.delete();
        if (!deleted) {
          throw new FileAlreadyExistsException(desiredFile.getAbsolutePath());
        }
        temporaryFile.renameTo(desiredFile);
      }
      catch (Throwable t) {
        ErrorLogger.log("Had a problem trying to delete the original file and renaming the temporary file to it.");
        ErrorLogger.log(t);
      }
    }

    TypecastSingletonManager.getRecentFilesManager("RecentFilesManager").addRecentFile(path);

    ((WSDirectoryListHolder) ComponentRepository.get("SidePanel_DirectoryList_DirectoryListHolder")).reload();

    TaskProgressManager.stopTask();

    ChangeMonitor.reset();
    WSPopup.showMessage("WriteArchive_ArchiveSaved", true);

    if (replacingCurrentArchive) {
      // now we need to reload the new archive, as the file contents have probably changed offset/length
      Task_ReadArchiveWithPlugin readTask = new Task_ReadArchiveWithPlugin(desiredFile, Archive.getReadPlugin());
      readTask.setDirection(Task.DIRECTION_REDO);
      new Thread(readTask).start();
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
