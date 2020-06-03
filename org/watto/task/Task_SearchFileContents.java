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
import java.nio.charset.Charset;
import org.watto.Language;
import org.watto.component.ComponentRepository;
import org.watto.component.FileListPanel;
import org.watto.component.WSFileListPanelHolder;
import org.watto.component.WSPopup;
import org.watto.datatype.Archive;
import org.watto.datatype.Resource;
import org.watto.io.FileManipulator;
import org.watto.io.PatternFinder;

/**
**********************************************************************************************

**********************************************************************************************
**/
public class Task_SearchFileContents extends AbstractTask {

  /** The direction to perform in the thread **/
  int direction = 1;

  String searchVal;
  boolean firstMatchOnly = true;

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public Task_SearchFileContents(String searchVal, boolean firstMatchOnly) {
    this.searchVal = searchVal;
    this.firstMatchOnly = firstMatchOnly;
  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  @SuppressWarnings("unused")
  @Override
  public void redo() {
    if (!TaskProgressManager.canDoTask()) {
      return;
    }

    if (searchVal == null || searchVal.equals("")) {
      WSPopup.showError("Search_NoSearchValue", true);
      return;
    }

    // Progress dialog
    TaskProgressManager.show(1, 0, Language.get("Progress_SearchingFiles"));
    TaskProgressManager.setIndeterminate(true);

    TaskProgressManager.startTask();

    Charset charSet = Charset.forName("UTF-8");
    String searchAscii = new String(searchVal.getBytes(charSet), charSet);
    charSet = Charset.forName("UTF-16LE");
    String searchUnicode = new String(searchVal.getBytes(charSet), charSet);

    // determine the starting position
    int numFiles = Archive.getNumFiles();
    FileListPanel fileList = (FileListPanel) ((WSFileListPanelHolder) ComponentRepository.get("FileListPanelHolder")).getCurrentPanel();

    //ArchivePlugin readPlugin = Archive.getReadPlugin();

    int startPos = fileList.getFirstSelectedRow();
    if (startPos >= numFiles) {
      startPos = 0;
    }
    else {
      startPos++;
    }

    fileList.selectNone();

    // search for the files (StartPos --> end)
    for (int i = startPos; i < numFiles; i++) {
      Resource resource = fileList.getResource(i);
      File path = resource.getExportedPath();
      if (path == null) {
        path = resource.getSource();
      }

      FileManipulator fm = new FileManipulator(path, false);
      fm.seek(resource.getOffset());

      PatternFinder patternFinder = new PatternFinder(fm);
      long found = patternFinder.find(searchAscii);

      if (found > 0) {
        fileList.changeSelection(i);
        if (firstMatchOnly) {
          WSPopup.showMessage("Search_MatchFound", true);
          TaskProgressManager.stopTask();
          return;
        }
      }

    }

    // search for the files (beginning --> StartPos)
    for (int i = 0; i < startPos; i++) {
      Resource resource = fileList.getResource(i);
      File path = resource.getExportedPath();
      if (path == null) {
        path = resource.getSource();
      }

      FileManipulator fm = new FileManipulator(path, false);
      fm.seek(resource.getOffset());

      PatternFinder patternFinder = new PatternFinder(fm);
      long found = patternFinder.find(searchAscii);

      if (found > 0) {
        fileList.changeSelection(i);
        if (firstMatchOnly) {
          WSPopup.showMessage("Search_MatchFound", true);
          TaskProgressManager.stopTask();
          return;
        }
      }

    }

    if (fileList.getNumSelected() <= 0) {
      // Did not find any matching files
      WSPopup.showError("Search_NoMatchFound", true);
    }

    TaskProgressManager.stopTask();

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
