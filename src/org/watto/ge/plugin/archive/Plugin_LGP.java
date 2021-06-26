
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
public class Plugin_LGP extends ArchivePlugin {

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public Plugin_LGP() {

    super("LGP", "LGP");

    //         read write replace rename
    setProperties(true, false, false, false);

    setExtensions("lgp");
    setGames("Final Fantasy 7");
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

      fm.skip(2);

      // Header
      if (fm.readString(10).equals("SQUARESOFT")) {
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

      FileManipulator fm = new FileManipulator(path, false);

      // 2 - null
      // 10 - header (SQUARESOFT)
      fm.skip(12);

      // 4 - numFiles
      int numFiles = fm.readInt();
      FieldValidator.checkNumFiles(numFiles);

      long arcSize = fm.getLength();

      Resource[] resources = new Resource[numFiles];
      TaskProgressManager.setMaximum(numFiles);

      long[] offsets = new long[numFiles];
      for (int i = 0; i < numFiles; i++) {
        // 20 - Filename
        String filename = fm.readNullString(20);
        FieldValidator.checkFilename(filename);

        // 4 - offset (+24 for file header data)
        long offset = fm.readInt() + 24;
        offsets[i] = offset;
        FieldValidator.checkOffset(offset, arcSize);

        // 2 - File Type ID?
        // 1 - null
        fm.skip(3);

        //path,id,name,offset,length,decompLength,exporter
        resources[i] = new Resource(path, filename, offset);

        TaskProgressManager.setValue(i);
      }

      long[] sizes = new long[numFiles];
      java.util.Arrays.sort(offsets);

      for (int i = 0; i < numFiles - 1; i++) {
        sizes[i] = offsets[i + 1] - offsets[i];
      }
      sizes[numFiles - 1] = arcSize - offsets[numFiles - 1];

      for (int i = 0; i < numFiles; i++) {
        for (int j = 0; j < numFiles; j++) {
          if (resources[j].getOffset() == offsets[i]) {
            resources[j].setLength(sizes[i]);
            j = numFiles;
          }
        }
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