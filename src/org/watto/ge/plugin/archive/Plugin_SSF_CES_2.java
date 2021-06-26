
package org.watto.ge.plugin.archive;

import java.io.File;
import org.watto.task.TaskProgressManager;
import org.watto.datatype.Archive;
import org.watto.datatype.Resource;
import org.watto.ge.helper.FieldValidator;
import org.watto.ge.plugin.ArchivePlugin;
////////////////////////////////////////////////////////////////////////////////////////////////
//                                                                                            //
//                                       GAME EXTRACTOR                                       //
//                               Extensible Game Archive Editor                               //
//                                http://www.watto.org/extract                                //
//                                                                                            //
//                           Copyright (C) 2002-2009  WATTO Studios                           //
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
import org.watto.io.FileManipulator;

/**
**********************************************************************************************

**********************************************************************************************
**/
public class Plugin_SSF_CES_2 extends ArchivePlugin {

  int realNumFiles = 0;
  int readLength = 0;

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public Plugin_SSF_CES_2() {

    super("SSF_CES_2", "SSF_CES_2");

    //         read write replace rename
    setProperties(true, false, false, false);

    setExtensions("ssf");
    setGames("Mortal Kombat Deception");
    setPlatforms("PC");

    setFileTypes("1", "Sub-Directory",
        "2", "Sprite?",
        "3", "3D Model?",
        "4", "Texture?",
        "8", "Animation Path?");
    //"5","Unknown",
    //"6","Unknown",
    //"7","Unknown",

  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public void analyseDirectory(FileManipulator fm, File path, Resource[] resources, long offset) throws Exception {
    fm.seek(offset);
    long arcSize = fm.getLength();

    // 4 - Directory Header ( CES)
    // 4 - Unknown (4)
    // 4 - null
    // 4 - Unknown
    fm.skip(16);

    // 4 - Number Of Files
    int numFiles = fm.readInt();
    FieldValidator.checkNumFiles(numFiles + 1);

    long endOfDir = offset + 28 + (numFiles * 16);

    // 4 - Unknown
    // 4 - Directory Length
    fm.skip(8);

    for (int i = 0; i < numFiles; i++) {

      // 4 - File Type ID (1,2,3)
      int fileType = fm.readInt();

      // 4 - Data Offset (relative to the start of the current directory)
      int rawOffset = fm.readInt();
      long fileOffset = offset + rawOffset;
      FieldValidator.checkOffset(fileOffset, arcSize);

      // 4 - File Length
      long length = fm.readInt();
      FieldValidator.checkLength(length, arcSize);

      // 4 - Filename Offset
      int filenameOffsetRelative = fm.readInt();
      long filenameOffset = filenameOffsetRelative + endOfDir;
      if (filenameOffsetRelative == 3473408) {
        filenameOffset = 0;
      }
      else {
        FieldValidator.checkOffset(filenameOffset, arcSize);
      }

      if (fileType == 1) {
        // Directory
        int currentPos = (int) fm.getOffset();
        analyseDirectory(fm, path, resources, rawOffset);
        fm.seek(currentPos);
      }
      else {
        String filename = "";

        if (filenameOffsetRelative <= 0) {
          filename = Resource.generateFilename(realNumFiles) + "." + fileType;
        }
        else {
          int currPos = (int) fm.getOffset();
          fm.seek(filenameOffset);
          filename = fm.readNullString() + "." + fileType;
          fm.seek(currPos);
        }

        //path,id,name,offset,length,decompLength,exporter
        resources[realNumFiles] = new Resource(path, filename, fileOffset, length);

        realNumFiles++;
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

      // Header
      if (fm.readString(4).equals(" CES")) {
        rating += 50;
      }

      fm.skip(12);

      // Number Of Directories
      if (FieldValidator.checkNumFiles(fm.readInt())) {
        rating += 5;
      }

      fm.skip(4);

      long arcSize = fm.getLength();

      // Archive Size
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

      analyseDirectory(fm, path, resources, 0);

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