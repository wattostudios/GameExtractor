
package org.watto.ge.plugin.archive;

import java.io.File;
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
import org.watto.task.TaskProgressManager;

/**
**********************************************************************************************

**********************************************************************************************
**/
public class Plugin_PAK_15 extends ArchivePlugin {

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public Plugin_PAK_15() {

    super("PAK_15", "PAK_15");

    //         read write replace rename
    setProperties(true, false, false, false);

    setGames("Star Wars Episode 3");
    setExtensions("pak");
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

      // Number Of Files
      int numFiles = fm.readInt();
      if (FieldValidator.checkNumFiles(numFiles)) {
        rating += 5;
      }

      long arcSize = fm.getLength();

      // End File 1 Offset
      if (FieldValidator.checkOffset(fm.readInt(), arcSize)) {
        rating += 5;
      }

      // End File 2 Offset
      if (FieldValidator.checkOffset(fm.readInt(), arcSize)) {
        rating += 5;
      }

      // End File 1 Length
      if (FieldValidator.checkLength(fm.readInt(), arcSize)) {
        rating += 5;
      }

      // End File 2 Length
      if (FieldValidator.checkLength(fm.readInt(), arcSize)) {
        rating += 4;
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

      long arcSize = fm.getLength();

      // 4 - Number Of Files
      int numFiles = fm.readInt();
      FieldValidator.checkNumFiles(numFiles);

      // 4 - End File 1 Offset
      int endFile1Offset = fm.readInt();
      FieldValidator.checkOffset(endFile1Offset, arcSize);

      // 4 - End File 2 Offset
      int endFile2Offset = fm.readInt();
      FieldValidator.checkOffset(endFile2Offset, arcSize);

      // 4 - End File 1 Length
      int endFile1Length = fm.readInt();
      FieldValidator.checkLength(endFile1Length, arcSize);

      // 4 - End File 2 Length
      int endFile2Length = fm.readInt();
      FieldValidator.checkLength(endFile2Length, arcSize);

      Resource[] resources = new Resource[numFiles + 2];
      TaskProgressManager.setMaximum(numFiles);

      resources[numFiles] = new Resource(path, Resource.generateFilename(numFiles), endFile1Offset, endFile1Length);
      resources[numFiles + 1] = new Resource(path, Resource.generateFilename(numFiles + 1), endFile2Offset, endFile2Length);

      fm.seek(2048);

      // Loop through directory
      for (int i = 0; i < numFiles; i++) {
        // 36 - Filename (null)
        String filename = fm.readNullString(36);
        FieldValidator.checkFilename(filename);

        // 4 - Next File Offset
        fm.skip(4);

        // 4 - File Length (including null padding and this header)
        long length = fm.readInt() - 48;
        FieldValidator.checkLength(length, arcSize);

        // 2 - Unknown (180)
        // 2 - File ID?
        fm.skip(4);

        // X - File Data
        // 0-2047 - null Padding to a multiple of 2048 bytes
        long offset = (int) fm.getOffset();
        fm.skip(length);

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
