
package org.watto.ge.plugin.archive;

import java.io.File;
import org.watto.task.TaskProgressManager;
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
import org.watto.io.converter.IntConverter;

/**
**********************************************************************************************

**********************************************************************************************
**/
public class Plugin_RBD extends ArchivePlugin {

  /**
  **********************************************************************************************

  **********************************************************************************************
  **/
  public Plugin_RBD() {

    super("RBD", "RBD");

    //         read write replace rename
    setProperties(true, false, false, false);

    setGames("Dark Vengence");
    setExtensions("rbd");
    setPlatforms("PC");

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

      fm.skip(16);

      // Header
      if (fm.readString(56).equals("Copyright 1998 Reality Bytes, Inc.  All rights reserved.")) {
        rating += 50;
      }

      // null
      if (fm.readLong() == 0) {
        rating += 5;
      }

      // null
      if (fm.readLong() == 0) {
        rating += 5;
      }

      // null
      if (fm.readLong() == 0) {
        rating += 5;
      }

      // null
      if (fm.readLong() == 0) {
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

      FileManipulator fm = new FileManipulator(path, false);

      // 4 - Unknown (512)
      fm.skip(4);

      // 4 - Number Of Files *BIG*
      int numFiles = IntConverter.changeFormat(fm.readInt());
      FieldValidator.checkNumFiles(numFiles);

      // 4 - Padding (all 255's)
      // 4 - Unknown
      // 56 - Header (Copyright 1998 Reality Bytes, Inc.  All rights reserved.)
      // 952 - null Padding to offset 1024
      fm.seek(1024);

      //long arcSize = fm.getLength();

      Resource[] resources = new Resource[numFiles];
      TaskProgressManager.setMaximum(numFiles);

      int[] numFilesInDir = new int[10];
      String[] dirNames = new String[10];
      int dirLevel = -1;

      int realNumFiles = 0;

      // Loop through directory
      for (int i = 0; i < numFiles; i++) {
        // 4 - Entry Type (0=File, 1=Leaf Directory, 257=Directory with sub-directories in it)
        int entryType = fm.readInt();

        if (entryType == 1 || entryType == 257) {
          // Directory

          if (dirLevel != -1) {
            numFilesInDir[dirLevel]--;
          }

          // 4 - Number Of Files/Subdirectories In Directory *BIG*
          dirLevel++;
          numFilesInDir[dirLevel] = IntConverter.changeFormat(fm.readInt());

          // 4 - Number of the following entries to be read before this directory is finished *BIG*
          // 4 - null
          // 4 - null
          // 4 - null
          // 4 - null
          // 4 - null
          // 1 - Directory Name Length
          fm.skip(25);

          // 63 - Directory Name (null)
          dirNames[dirLevel] = fm.readNullString(63);
        }
        else if (entryType == 0) {
          // File

          // 4 - File Length *BIG*
          long length = IntConverter.changeFormat(fm.readInt());
          //FieldValidator.checkLength(length,arcSize);

          // 4 - File Offset *BIG*
          long offset = IntConverter.changeFormat(fm.readInt());
          //FieldValidator.checkOffset(offset,arcSize);

          // 4 - Unknown
          // 4 - Unknown
          // 4 - Purpose Description (SCPL, ttxt, LMAN, MSWD, 8BIM, etc.)
          // 4 - Type Description (WAVE, TEXT, PNGf, etc.)
          // 4 - Unknown
          // 1 - Filename Length
          fm.skip(21);

          // 63 - Filename (null)
          String filename = fm.readNullString(63);

          // ADD IN THE DIRECTORY DETAILS
          for (int n = 0; n <= dirLevel; n++) {
            filename = dirNames[n] + "/" + filename;
          }

          //path,id,name,offset,length,decompLength,exporter
          resources[realNumFiles] = new Resource(path, filename, offset, length);
          realNumFiles++;

          numFilesInDir[dirLevel]--;

          while (dirLevel >= 0 && numFilesInDir[dirLevel] == 0) {
            dirLevel--;
          }

        }
        else {
          return null;
        }

        TaskProgressManager.setValue(i);
      }

      fm.close();

      resources = resizeResources(resources, realNumFiles);

      return resources;

    }
    catch (Throwable t) {
      logError(t);
      return null;
    }
  }

}
