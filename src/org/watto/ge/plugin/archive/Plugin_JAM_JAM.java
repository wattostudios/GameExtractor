
package org.watto.ge.plugin.archive;

import java.io.File;
import org.watto.task.TaskProgressManager;
import org.watto.datatype.Archive;
import org.watto.datatype.ReplacableResource;
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
public class Plugin_JAM_JAM extends ArchivePlugin {

  int realNumFiles = 0;
  int readLength = 0;

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public Plugin_JAM_JAM() {

    super("JAM_JAM", "JAM_JAM");

    //         read write replace rename
    setProperties(true, false, false, false);
    setCanImplicitReplace(true);

    setGames("NCAA Final Four");
    setExtensions("jam");
    setPlatforms("PC");

  }

  public void analyseDirectory(FileManipulator fm, File path, Resource[] resources, String dirName, long offset) throws Exception {
    fm.seek(offset);
    long arcSize = fm.getLength();

    // 4 - Number Of Files in this Directory
    int numFiles = fm.readInt();
    FieldValidator.checkNumFiles(numFiles + 1);

    // for each file in this directory
    for (int i = 0; i < numFiles; i++) {

      // 15 - Filename (null)
      String filename = dirName + "\\" + fm.readNullString(15);

      // 4 - Offset
      long offsetPointerLocation = fm.getOffset();
      long offsetPointerLength = 4;

      offset = fm.readInt();
      FieldValidator.checkOffset(offset, arcSize);

      // 4 - Length
      long lengthPointerLocation = fm.getOffset();
      long lengthPointerLength = 4;

      long length = fm.readInt();
      FieldValidator.checkLength(length, arcSize);

      //path,id,name,offset,length,decompLength,exporter
      resources[realNumFiles] = new ReplacableResource(path, filename, offset, offsetPointerLocation, offsetPointerLength, length, lengthPointerLocation, lengthPointerLength);

      realNumFiles++;
      TaskProgressManager.setValue(readLength);
      readLength += length;

    }

    // 4 - Number Of Sub-Directories
    int numDirs = fm.readInt();
    FieldValidator.checkNumFiles(numDirs + 1);

    // for each sub-directory
    for (int i = 0; i < numDirs; i++) {

      // 15 - Directory Name
      String subDirName = fm.readNullString(15);
      FieldValidator.checkFilename(subDirName);

      // 4 - Offset to sub-directory
      long dirOffset = fm.readInt();
      FieldValidator.checkOffset(dirOffset, arcSize);

      int currentPos = (int) fm.getOffset();
      analyseDirectory(fm, path, resources, dirName + "\\" + subDirName, dirOffset);
      fm.seek(currentPos);

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
      if (fm.readString(3).equals("JAM")) {
        rating += 50;
      }

      // Number Of Files
      int numFiles = fm.readInt();
      if (FieldValidator.checkNumFiles(numFiles)) {
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

      // 3 - Header (JAM)

      int numFiles = Archive.getMaxFiles(4);// guess

      long arcSize = fm.getLength();

      Resource[] resources = new Resource[numFiles];
      TaskProgressManager.setMaximum(arcSize);

      analyseDirectory(fm, path, resources, "", 3);

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