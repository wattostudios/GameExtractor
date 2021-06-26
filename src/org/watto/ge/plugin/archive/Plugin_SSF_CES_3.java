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
import org.watto.datatype.Archive;
import org.watto.datatype.Resource;
import org.watto.ge.helper.FieldValidator;
import org.watto.ge.plugin.ArchivePlugin;
import org.watto.io.FileManipulator;
import org.watto.task.TaskProgressManager;

/**
**********************************************************************************************

**********************************************************************************************
**/
public class Plugin_SSF_CES_3 extends ArchivePlugin {

  int realNumFiles = 0;
  int readLength = 0;

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public Plugin_SSF_CES_3() {

    super("SSF_CES_3", "SSF_CES_3");

    //         read write replace rename
    setProperties(true, false, false, false);

    setExtensions("ssf");
    setGames("Mortal Kombat Armageddon");
    setPlatforms("XBox");

    /*
    setFileTypes(
        "2", "3D Bone?",
        "3", "3D Model?",
        "5", "Animation?",
        "6", "Directory",
        "7", "Directory Index",
        "8", "Unknown Directory");
    //"4","Unknown",
    //"9","Unknown",
    //"10","Unknown",
    */
  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public void analyseDirectory(FileManipulator fm, File path, Resource[] resources, long offset, String dirName) throws Exception {
    fm.seek(offset);
    long arcSize = fm.getLength();

    // 4 - Header ( CES)
    // 4 - Unknown (4)
    // 4 - Unknown (0/2)
    // 4 - Unknown (first instance of this field is the total number of files in the archive?)
    fm.skip(16);

    // 4 - Number Of Files in this Directory
    int numFiles = fm.readInt();
    FieldValidator.checkNumFiles(numFiles);

    // 4 - Filename Directory Length
    int filenameDirLength = fm.readInt();
    FieldValidator.checkLength(filenameDirLength, arcSize);

    // 4 - Length of all Data in this Directory (including sub-directories) (first instance of the field is the Archive Length)
    fm.skip(4);

    // Read the filenames
    fm.skip(numFiles * 16);
    String[] filenames = new String[numFiles];
    for (int i = 0; i < numFiles; i++) {
      // X - Filename
      // 1 - null Filename Terminator
      filenames[i] = fm.readNullString();
    }

    // Now go back and read the directory
    fm.seek(offset + 28);

    for (int i = 0; i < numFiles; i++) {

      // 4 - File Type ID (1,2,3,4,5,6,7,8,9,10)
      int fileType = fm.readInt();

      // 4 - Offset (relative to the start of the current directory, ie relative to the offset of " CES")
      int rawOffset = fm.readInt();
      long fileOffset = offset + rawOffset;
      FieldValidator.checkOffset(fileOffset, arcSize);

      // 4 - File Length
      int length = fm.readInt();
      FieldValidator.checkLength(length, arcSize);

      // 4 - Filename Offset (relative to the start of the filename directory) (if this number is larger than the filename directory length, then this entry doesn't have a filename)
      int filenameOffset = fm.readInt();

      if (fileType == 6 || (offset == 0 && (fileType == 1 || fileType == 3))) {
        // Directory
        int currentPos = (int) fm.getOffset();

        String filename = filenames[i];
        if (filenameOffset < filenameDirLength) {
          // has a filename
        }
        else {
          // no filename - make one up
          filename = "Directory" + offset + "_" + (i + 1);
        }
        filename = dirName + filename + "\\";

        analyseDirectory(fm, path, resources, fileOffset, filename);
        fm.seek(currentPos);
      }
      else {
        String filename = filenames[i];
        if (filenameOffset < filenameDirLength) {
          // has a filename
        }
        else {
          // no filename - make one up
          filename = Resource.generateFilename(realNumFiles);
        }
        filename = dirName + filename + "." + fileType;

        if (length != 0) { // skip "null" files
          //path,id,name,offset,length,decompLength,exporter
          resources[realNumFiles] = new Resource(path, filename, fileOffset, length);
          realNumFiles++;
        }

        TaskProgressManager.setValue(readLength);
        readLength += length;
      }

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

      // 4 - Header ( CES)
      if (fm.readString(4).equals(" CES")) {
        rating += 50;
      }

      // 4 - Unknown (4)
      // 4 - Unknown (0/2)
      // 4 - Unknown (first instance of this field is the total number of files in the archive?)
      fm.skip(12);

      // 4 - Number Of Files in this Directory
      if (FieldValidator.checkNumFiles(fm.readInt())) {
        rating += 5;
      }

      long arcSize = fm.getLength();

      // 4 - Filename Directory Length
      if (FieldValidator.checkLength(fm.readInt(), arcSize)) {
        rating += 5;
      }

      // 4 - Length of all Data in this Directory (including sub-directories) (first instance of the field is the Archive Length)
      if (FieldValidator.checkLength(fm.readInt() - 1, arcSize)) {
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
  public Resource[] read(File path) {
    try {

      addFileTypes();

      // RESETTING THE GLOBAL VARIABLES
      realNumFiles = 0;
      readLength = 0;

      FileManipulator fm = new FileManipulator(path, false);

      int numFiles = Archive.getMaxFiles(4);// guess

      long arcSize = fm.getLength();

      Resource[] resources = new Resource[numFiles];
      TaskProgressManager.setMaximum(arcSize);

      analyseDirectory(fm, path, resources, 0, "");

      resources = resizeResources(resources, realNumFiles);

      fm.close();

      return resources;

    }
    catch (Throwable t) {
      logError(t);
      return null;
    }
  }

}