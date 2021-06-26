
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

/**
**********************************************************************************************

**********************************************************************************************
**/
public class Plugin_XWB_WBND_2 extends ArchivePlugin {

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public Plugin_XWB_WBND_2() {

    super("XWB_WBND_2", "XWB_WBND_2");

    //         read write replace rename
    setProperties(true, false, false, false);

    setGames("Ghost Recon 2");
    setExtensions("xwb");
    setPlatforms("XBox");

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
      if (fm.readString(4).equals("WBND")) {
        rating += 50;
      }

      // Version (2)
      if (fm.readInt() == 2) {
        rating += 5;
      }

      // Size Of Header 1 (40)
      if (fm.readInt() == 40) {
        rating += 5;
      }

      // Size Of Header 2 (36)
      if (fm.readInt() == 36) {
        rating += 5;
      }

      // Directory Offset (76)
      if (fm.readInt() == 76) {
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

      FileManipulator fm = new FileManipulator(path, false);

      long arcSize = fm.getLength();

      // 4 - Header (WBND)
      // 4 - Version (2)
      // 4 - Size Of Header 1 (40)
      // 4 - Size Of Header 2 (36)
      fm.skip(16);

      // 4 - Offset To Details Directory (76)
      long dirOffset = fm.readInt();
      FieldValidator.checkOffset(dirOffset, arcSize);

      // 4 - Length Of Details Directory
      fm.skip(4);

      // 4 - Offset To Filename Directory
      int filenameDirOffset = fm.readInt();
      FieldValidator.checkOffset(filenameDirOffset, arcSize);

      // 4 - Length Of Filename Directory
      fm.skip(4);

      // 4 - First File Offset (512) [+8]
      int firstFileOffset = fm.readInt() + 8;
      FieldValidator.checkOffset(firstFileOffset, arcSize);

      // 4 - Unknown
      // 2 - Unknown (1)
      // 2 - Unknown (1)
      fm.skip(8);

      // 4 - Number Of Files
      int numFiles = fm.readInt();
      FieldValidator.checkNumFiles(numFiles);

      // 16 - Archive Filename (null) (without extension)
      // 4 - Length Of Each Details Entry (24)
      // 4 - Length Of Each Filename Entry (64)
      // 4 - Max padding size between each file (512)

      Resource[] resources = new Resource[numFiles];
      TaskProgressManager.setMaximum(numFiles);

      fm.seek(filenameDirOffset);

      // FILENAMES DIRECTORY
      String[] names = new String[numFiles];
      if (filenameDirOffset > 0) {
        for (int i = 0; i < numFiles; i++) {
          // 64 - Filename (null) (without extension)
          String filename = fm.readNullString(64);
          FieldValidator.checkFilename(filename);
          names[i] = filename;
        }
      }
      else {
        for (int i = 0; i < numFiles; i++) {
          names[i] = Resource.generateFilename(i);
        }
      }

      fm.seek(dirOffset);

      // Loop through directory
      for (int i = 0; i < numFiles; i++) {
        // 4 - null
        // 4 - Unknown
        fm.skip(8);

        // 4 - File Offset (relative to the start of the file data)
        long offset = fm.readInt() + firstFileOffset;
        FieldValidator.checkOffset(offset, arcSize);

        // 4 - File Length
        long length = fm.readInt();
        FieldValidator.checkLength(length, arcSize);

        // 8 - null
        fm.skip(8);

        String filename = names[i];

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

}
