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
import java.util.HashMap;
import org.watto.ChangeMonitor;
import org.watto.Language;
import org.watto.component.ComponentRepository;
import org.watto.component.FileListPanel;
import org.watto.component.WSFileListPanelHolder;
import org.watto.component.WSPopup;
import org.watto.datatype.Archive;
import org.watto.datatype.Resource;
import org.watto.ge.plugin.ArchivePlugin;

/**
**********************************************************************************************

**********************************************************************************************
**/
public class Task_ReplaceMatchingFiles extends AbstractTask {

  /** The direction to perform in the thread **/
  int direction = 1;

  /** the resources to replace **/
  Resource[] resources;

  /** the base path to search for a match **/
  File basePath;

  /** The original contents of the resources that were changed.
      These are clone()s so that they have a separate reference to the original resources **/
  Resource[] originalResources;

  Resource[] replacedResources;

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public Task_ReplaceMatchingFiles(Resource[] resources, File basePath) {
    this.resources = resources;
    this.basePath = basePath;
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

    if (resources == null || resources.length <= 0 || basePath == null) {
      return;
    }

    if (!basePath.isDirectory()) {
      return;
    }

    // Progress dialog
    TaskProgressManager.show(1, 0, Language.get("Progress_ReplacingFiles"));
    TaskProgressManager.setIndeterminate(true);

    TaskProgressManager.startTask();

    ((WSFileListPanelHolder) ComponentRepository.get("FileListPanelHolder")).stopInlineEditing();

    String baseDir = basePath.getAbsolutePath() + File.separator;
    String baseDirParent = basePath.getParentFile().getAbsolutePath() + File.separator;

 // *2 because if the ArchivePlugin allows convertOnReplace, we could end up doing a normal replace followed by a convertReplace for each file
    originalResources = new Resource[resources.length*2]; 
    replacedResources = new Resource[resources.length*2];
    
    int numberReplaced = 0;

    for (int i = 0; i < resources.length; i++) {
      // 1. Check if the file exists
      File newFile = new File(baseDir + resources[i].getName());
      if (newFile.exists()) {
        // clone the resource to a separate object
        originalResources[numberReplaced] = (Resource) resources[i].clone();
        replacedResources[numberReplaced] = resources[i];
        numberReplaced++;
        // perform a replace on the resource
        resources[i].replace(newFile);
      }
      else {
        // 2. If not found, check the parent directory for the file
        newFile = new File(baseDirParent + resources[i].getName());
        if (newFile.exists()) {
          // clone the resource to a separate object
          originalResources[numberReplaced] = (Resource) resources[i].clone();
          replacedResources[numberReplaced] = resources[i];
          numberReplaced++;
          // perform a replace on the resource
          resources[i].replace(newFile);
        }
      }
    }

    // 3. Now that we're here, we've replaced all the matching files.
    // However, if the ArchivePlugin canConvertOnReplace, we also want to see if there's any files that we can
    // convert and then replace into the archive.
    ArchivePlugin readPlugin = Archive.getReadPlugin();
    if (readPlugin.canConvertOnReplace()) {
      // this ArchivePlugin does support convertOnReplace.

      // 3.1 Go through all the Resources, get all the unique directory names. 
      HashMap<String, String> uniqueDirsMap = new HashMap<String, String>();
      int numFiles = resources.length;
      for (int i = 0; i < numFiles; i++) {
        String dirName = resources[i].getDirectory();
        uniqueDirsMap.put(dirName, dirName);
      }
      String[] uniqueDirs = uniqueDirsMap.values().toArray(new String[0]);

      uniqueDirsMap = null; // free memory
      
      // 3.2 For each unique directory, find all the files on the PC in that directory, but *only* files that have 2 "." in the filename.
      // This is because, when we do an extract and convert, it will extract the file as <original_name>.<ext>.png for example, 
      // and what we're looking for (in the matching) is this format so we can strip off the ".png" and match to that shortened filename.
      // Build a map of all the <short> names, which match to the <long> names.
      int numDirs = uniqueDirs.length;
      HashMap<String, File> convertableFilesMap = new HashMap<String, File>();
      for (int d=0;d<numDirs;d++) {
        String directoryToFind = uniqueDirs[d];
        File scanDir = new File(baseDir + directoryToFind);
        if (scanDir.exists()) {
          // found
          directoryToFind = /*baseDir + */directoryToFind + File.separatorChar;
        }
        else {
          // not found, try looking in the parent directory instead
          scanDir = new File(baseDirParent + directoryToFind);
          if (scanDir.exists()) {
            // found
            directoryToFind = /*baseDirParent + */directoryToFind + File.separatorChar;
          }
          else {
            // not found - can't find this directory anywhere - mustn't be trying to replace files in this directory, skip it
            continue;
          }
        }
        if (!scanDir.exists()) {
          // can't find this directory anywhere - mustn't be trying to replace files in this directory, skip it
          continue;
        }
        
        // if we're here, we found a directory to scan, so lets find all the files with 2 "." in the name, and add them to the map
        File[] files = scanDir.listFiles();
        int numScanFiles = files.length;
        char dot = '.';
        for (int s=0;s<numScanFiles;s++) {
          File fileToCheck = files[s];
          if (!fileToCheck.isFile()) {
            // not a file - skip
            continue;
          }
          String filenameToCheck = fileToCheck.getName();
          int firstDotPos = filenameToCheck.indexOf(dot);
          int lastDotPos = filenameToCheck.lastIndexOf(dot);
          if (firstDotPos == -1) {
            // no dot in the filename
            continue;
          }
          if (firstDotPos == lastDotPos) {
            // only 1 dot
            continue;
          }
          // if we're here, there's at least 2 dots, so add the file the map
          String shortFilename = directoryToFind + filenameToCheck.substring(0,lastDotPos);
          convertableFilesMap.put(shortFilename, fileToCheck);
        }
      }

      // 3.3 Go through all the Resources, see if there's a matching name in the map. If so, we have a potential file that could be
      // converted to replace this file. Try to convert and replace the file, if possible.
      for (int i = 0; i < numFiles; i++) {
        Resource resource = resources[i];
        String filename = resource.getName();

        File convertableFile = convertableFilesMap.get(filename);
        if (convertableFile == null) {
          convertableFile = convertableFilesMap.get(File.separatorChar + filename); // 3.15 also need to check for files starting with a single slash
          if (convertableFile == null) {
          // no match for this resource - skip
          continue;
          }
        }
        
        // if we're here, found a file that we'll try to convert to a match
        
        // clone the resource to a separate object
        originalResources[numberReplaced] = (Resource) resources[i].clone();
        replacedResources[numberReplaced] = resources[i];
        numberReplaced++;
        
        // perform a replace on the resource
        resources[i].replace(convertableFile);
      }
      
    }

    if (numberReplaced < resources.length) {
      // resize the clone arrays to take up less memory
      Resource[] tempResources = originalResources;
      originalResources = new Resource[numberReplaced];
      System.arraycopy(tempResources, 0, originalResources, 0, numberReplaced);

      tempResources = replacedResources;
      replacedResources = new Resource[numberReplaced];
      System.arraycopy(tempResources, 0, replacedResources, 0, numberReplaced);
    }

    ((FileListPanel) ((WSFileListPanelHolder) ComponentRepository.get("FileListPanelHolder")).getCurrentPanel()).reload();

    TaskProgressManager.stopTask();

    ChangeMonitor.change();
    if (isShowPopups()) {
      WSPopup.showMessage("ReplaceFiles_FilesReplaced", true);
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

    String numResources = "";
    if (replacedResources != null) {
      numResources = "" + replacedResources.length;
    }

    return Language.get(name).replace("&number&", "" + numResources);
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

    if (originalResources == null || originalResources.length <= 0) {
      return;
    }

    // Progress dialog
    TaskProgressManager.show(1, 0, Language.get("Progress_ReplacingFiles_Undo"));
    TaskProgressManager.setIndeterminate(true);

    TaskProgressManager.startTask();

    ((WSFileListPanelHolder) ComponentRepository.get("FileListPanelHolder")).stopInlineEditing();

    for (int i = 0; i < originalResources.length; i++) {
      // copy the details from the clone into the actual resource
      replacedResources[i].copyFrom(originalResources[i]);
    }

    TaskProgressManager.stopTask();

    ((FileListPanel) ((WSFileListPanelHolder) ComponentRepository.get("FileListPanelHolder")).getCurrentPanel()).reload();

    if (isShowPopups()) {
      WSPopup.showMessage("ReplaceFiles_FilesRestored", true);
    }
  }

}
