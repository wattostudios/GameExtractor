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

package org.watto.ge.plugin.archive;

import java.io.File;
import org.watto.Language;
import org.watto.Settings;
import org.watto.datatype.Resource;
import org.watto.ge.helper.FieldValidator;
import org.watto.ge.plugin.ArchivePlugin;
import org.watto.io.FileManipulator;
import org.watto.task.TaskProgressManager;

/**
**********************************************************************************************

**********************************************************************************************
**/
public class Plugin_FPK extends ArchivePlugin {

  int[] numFilesInDir = new int[0];

  String[] dirNames = new String[0];

  int dirNumber = 0;

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public Plugin_FPK() {

    super("FPK", "FPK");

    //         read write replace rename
    setProperties(true, false, true, false);

    setGames("WWE Raw 2");
    setExtensions("fpk");
    setPlatforms("XBox");

  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public void analyseDirectory(FileManipulator fm, File path, Resource[] resources, String dirName) throws Exception {

    //long arcSize = fm.getLength();

    // 20 - Directory Name (null)
    String subDirName = fm.readNullString(20);
    FieldValidator.checkFilename(subDirName);
    dirName += subDirName + "/";

    // 2 - Number Of Sub-Directories in this directory
    int numSubDirs = fm.readShort();

    // 2 - Number Of Files in this directory
    int numFilesInThisDir = fm.readShort();

    // 4 - Unknown (null if no subdirectories)
    // 4 - Unknown (null if no files)
    fm.skip(8);

    // REMEMBER THE NUMFILES AND NAME OF DIRECTORY
    numFilesInDir[dirNumber] = numFilesInThisDir;
    dirNames[dirNumber] = dirName;
    dirNumber++;

    for (int i = 0; i < numSubDirs; i++) {
      analyseDirectory(fm, path, resources, dirName);
    }

  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  @Override
  public int getMatchRating(FileManipulator fm) {
    try {

      int rating = 0;

      if (FieldValidator.checkExtension(fm, extensions)) {
        rating += 25;
      }

      fm.skip(12);

      // Number Of Directories
      if (FieldValidator.checkNumFiles(fm.readInt())) {
        rating += 5;
      }

      // Number Of Files
      if (FieldValidator.checkNumFiles(fm.readInt())) {
        rating += 5;
      }

      long arcSize = fm.getLength();

      // Length of File Data
      if (FieldValidator.checkLength(fm.readInt(), arcSize)) {
        rating += 5;
      }

      return rating;

    }
    catch (Throwable t) {
      return 0;
    }
  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  @Override
  @SuppressWarnings("unused")
  public Resource[] read(File path) {
    try {

      addFileTypes();

      // RESETTING THE GLOBAL VARIABLES
      numFilesInDir = new int[0];
      dirNames = new String[0];
      dirNumber = 0;

      FileManipulator fm = new FileManipulator(path, false);
      dirNumber = 0;

      // 4 - Unknown
      // 4 - Unknown
      // 4 - Unknown
      fm.skip(12);

      // 4 - Number Of Directories
      int numDirs = fm.readInt();
      FieldValidator.checkNumFiles(numDirs);

      // 4 - Number Of Files
      int numFiles = fm.readInt();
      FieldValidator.checkNumFiles(numFiles);

      // 4 - Length Of File Data
      // 4 - Unknown
      // 4 - Unknown
      fm.skip(12);

      long arcSize = fm.getLength();

      Resource[] resources = new Resource[numFiles];
      TaskProgressManager.setMaximum(numFiles);

      numFilesInDir = new int[numDirs];
      dirNames = new String[numDirs];

      // The First Directory

      // 20 - Directory Name (null) (first directory name is "___RootDirectory___")
      fm.skip(20);

      // 2 - Number Of Sub-Directories in this directory
      int numSubDirs = fm.readShort();

      // 2 - Number Of Files in this directory
      int numFilesInThisDir = fm.readShort();

      // 4 - Unknown (null if no subdirectories)
      // 4 - Unknown (null if no files)
      fm.skip(8);

      for (int i = 0; i < numSubDirs; i++) {
        analyseDirectory(fm, path, resources, "");
      }

      dirNumber = 0;

      // find the first directory with files in it
      while (dirNumber < numDirs && numFilesInDir[dirNumber] == 0) {
        dirNumber++;
      }

      // Loop through directory
      int relOffset = 32 + (numDirs * 32) + (numFiles * 28);
      for (int i = 0; i < numFiles; i++) {
        // 20 - Filename (null)
        String filename = fm.readNullString(20);
        FieldValidator.checkFilename(filename);

        // 4 - Offset (relative to the end of the directory)
        long offset = relOffset + fm.readInt();
        FieldValidator.checkOffset(offset, arcSize);

        // 4 - Length
        long length = fm.readInt();
        FieldValidator.checkLength(length, arcSize);

        filename = dirNames[dirNumber] + filename;
        numFilesInDir[dirNumber]--;

        // move to the next directory name when read all the files in this directory
        while (dirNumber < numDirs && numFilesInDir[dirNumber] <= 0) {
          dirNumber++;
        }

        //path,id,name,offset,length,decompLength,exporter
        resources[i] = new Resource(path, filename, offset, length);

        TaskProgressManager.setValue(i);
      }

      fm.close();

      return resources;

    }
    catch (Throwable t) {
      logError(t);
      return null;
    }
  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  @Override
  public void write(Resource[] resources, File path) {
    try {

      int numFiles = resources.length;
      TaskProgressManager.setMaximum(numFiles);

      FileManipulator fm = new FileManipulator(path, true);
      FileManipulator src = new FileManipulator(new File(Settings.getString("CurrentArchive")), false);

      // Calculations
      TaskProgressManager.setMessage(Language.get("Progress_PerformingCalculations"));
      int filesSize = 0;
      for (int i = 0; i < numFiles; i++) {
        filesSize += resources[i].getDecompressedLength();
      }

      // Write Header Data

      // 4 - Unknown
      // 4 - Unknown
      // 4 - Unknown
      fm.writeBytes(src.readBytes(12));

      // 4 - Number Of Directories
      int numDirs = src.readInt();
      fm.writeInt(numDirs);

      // 4 - Number Of Files
      fm.writeBytes(src.readBytes(4));

      // 4 - Length Of File Data
      src.skip(4);
      fm.writeInt(filesSize);

      // 4 - Unknown
      // 4 - Unknown
      fm.writeBytes(src.readBytes(8));

      // for each directory
      // 20 - Directory Name (null) (first directory name is "___RootDirectory___")
      // 2 - Number Of Sub-Directories in this directory
      // 2 - Number Of Files in this directory
      // 4 - Unknown (null if no subdirectories)
      // 4 - Unknown (null if no files)
      fm.writeBytes(src.readBytes(numDirs * 32));

      src.close();

      TaskProgressManager.setMessage(Language.get("Progress_WritingDirectory"));
      long offset = 0;
      for (int i = 0; i < numFiles; i++) {
        Resource fd = resources[i];
        long length = fd.getDecompressedLength();

        //  20 - Filename (null) [ONLY GETS THE FILENAME (and extension), NOT THE DIRECTORY!]
        fm.writeNullString(fd.getFilenameWithExtension(), 20);

        //  4 - Offset (relative to the end of the directory)
        fm.writeInt((int) offset);

        //  4 - Length
        fm.writeInt((int) length);

        offset += length;
      }

      TaskProgressManager.setMessage(Language.get("Progress_WritingFiles"));
      write(resources, fm);

      fm.close();

    }
    catch (Throwable t) {
      logError(t);
    }
  }

}
