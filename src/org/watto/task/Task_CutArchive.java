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
import java.io.FileOutputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import org.watto.ErrorLogger;
import org.watto.Language;
import org.watto.Settings;
import org.watto.component.ComponentRepository;
import org.watto.component.WSDirectoryListHolder;
import org.watto.component.WSPopup;
import org.watto.ge.helper.FilenameMatchFilter;
import org.watto.io.FileManipulator;
import org.watto.io.FilenameChecker;
import org.watto.io.FilenameSplitter;
import org.watto.io.stream.ManipulatorInputStream;

/**
**********************************************************************************************

**********************************************************************************************
**/
public class Task_CutArchive extends AbstractTask {

  /**
  **********************************************************************************************
  Creates an entry in the zip file used for cutting archives. The entry data is obtained from
  an input stream.
  @param filename the filename to store in the zip file
  @param inputStream the data for the file
  @param outputStream the zip file to write to
  @param cutSize the length of the data to copy
  @param seekPos the offset (in the inputStream) to the beginning of the data to copy
  **********************************************************************************************
  **/
  public static void createCutEntry(String filename, ManipulatorInputStream inputStream, ZipOutputStream outputStream, int cutSize, long seekPos) {
    try {

      ZipEntry frontEntry = new ZipEntry(filename);

      if (seekPos < 0) {
        inputStream.seek(0);
      }
      else {
        inputStream.seek(seekPos);
      }

      outputStream.putNextEntry(frontEntry);

      for (int i = 0; i < cutSize; i++) {
        outputStream.write(inputStream.read());
      }

      outputStream.closeEntry();

    }
    catch (Throwable t) {
      ErrorLogger.log(t);
    }
  }

  /**
  **********************************************************************************************
  Creates an entry in the zip file used for cutting archives. The entry data is obtained from
  a byte[] array.
  @param filename the filename to store in the zip file
  @param inputData the data for the file
  @param outputStream the zip file to write to
  **********************************************************************************************
  **/
  public static void createInputEntry(String filename, byte[] inputData, ZipOutputStream outputStream) {
    try {

      ZipEntry frontEntry = new ZipEntry(filename);

      outputStream.putNextEntry(frontEntry);

      outputStream.write(inputData);

      outputStream.closeEntry();

    }
    catch (Throwable t) {
      ErrorLogger.log(t);
    }
  }

  /** The direction to perform in the thread **/
  int direction = 1;
  File input;

  File output;

  int length;

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public Task_CutArchive(File input, File output, int length) {
    this.input = input;
    this.output = output;
    this.length = length;
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
    TaskProgressManager.show(1, 0, Language.get("Progress_CuttingArchive"));
    TaskProgressManager.setIndeterminate(true);

    TaskProgressManager.startTask();

    try {

      output = FilenameChecker.correctFilename(output);

      //ZipFile zip = new ZipFile(outputFile);
      ZipOutputStream outputStream = new ZipOutputStream(new FileOutputStream(output));

      FileManipulator fm = new FileManipulator(input, false);
      ManipulatorInputStream inputStream = new ManipulatorInputStream(fm);

      String extension = FilenameSplitter.getExtension(fm.getFile());

      long endPos = input.length() - length;
      if (input.length() < 5242880) {
        endPos = 0;
        length = (int) input.length();

        // just zip the entire file
        createCutEntry(input.getName(), inputStream, outputStream, length, 0);
      }
      else {
        // Put in the front entry
        createCutEntry(input.getName() + "-Front." + extension, inputStream, outputStream, length, 0);

        // Put in the back entry
        createCutEntry(input.getName() + "-Back." + extension, inputStream, outputStream, length, endPos);
      }

      if (Settings.getBoolean("AlsoCutDirectoryFiles")) {
        searchForDirectoryFiles(input, outputStream);
      }

      // Put in the information entry
      String info = "Filename: " + input.getAbsolutePath() + "\n" +
          "File Size: " + input.length() + "\n" +
          "End Position Offset: " + endPos + "\n" +
          "Cut Length: " + length;

      createInputEntry(input.getName() + "-Info." + extension + ".txt", info.getBytes(), outputStream);

      inputStream.close();
      outputStream.close();

      fm.close();

      ((WSDirectoryListHolder) ComponentRepository.get("SidePanel_DirectoryList_DirectoryListHolder")).reload();

      if (isShowPopups()) {
        WSPopup.showMessage("CutArchive_FilesCut", true);
      }
    }
    catch (Throwable t) {
      ErrorLogger.log(t);
      if (isShowPopups()) {
        WSPopup.showError("CutArchive_CutFailed", true);
      }
    }

    TaskProgressManager.stopTask();

  }

  /**
  **********************************************************************************************
  Searches the same directory as <i>input</i> for any small files with the same name, different
  extension. These files are put in the <i>outputStream</i>, as they might be directory files.
  @param input the input file
  @param outputStream the zip file
  **********************************************************************************************
  **/
  public void searchForDirectoryFiles(File input, ZipOutputStream outputStream) {
    try {

      File directory = new File(input.getParent());
      if (!directory.exists()) {
        return;
      }

      String filename = input.getName();

      int dotPos = filename.lastIndexOf(".");
      if (dotPos > 0) {
        filename = filename.substring(0, dotPos);
      }

      File[] files = directory.listFiles(new FilenameMatchFilter(filename));

      int dirFileMaxLength = Settings.getInt("MaximumDirectoryFileLength");

      for (int i = 0; i < files.length; i++) {
        File file = files[i];

        // only a directory file if...
        // 1. Not a directory
        // 2. Smaller than the MaximumDirectoryFileLength setting (512KBs)
        // 3. Is not the zip archive being created
        if (!file.isDirectory() && file.length() <= dirFileMaxLength && !file.getAbsolutePath().equals(output.getAbsolutePath())) {

          ManipulatorInputStream fileIS = new ManipulatorInputStream(new FileManipulator(file, false));
          createCutEntry(file.getName(), fileIS, outputStream, (int) file.length(), 0);
          fileIS.close();

        }
      }

    }
    catch (Throwable t) {
      ErrorLogger.log(t);
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
