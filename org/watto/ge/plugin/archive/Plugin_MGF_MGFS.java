
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
public class Plugin_MGF_MGFS extends ArchivePlugin {

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public Plugin_MGF_MGFS() {

    super("MGF_MGFS", "MGF_MGFS");

    //         read write replace rename
    setProperties(true, false, false, false);

    setExtensions("mgf");
    setGames("Battleship Surface Thunder");
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

      // Header
      if (fm.readString(4).equals("MGFs")) {
        rating += 50;
      }

      fm.skip(4);

      // Archive Size
      if (fm.readInt() == fm.getLength()) {
        rating += 5;
      }

      // Number Of Files
      int numFiles = fm.readInt();
      if (FieldValidator.checkNumFiles(numFiles)) {
        rating += 5;
      }

      long arcSize = fm.getLength();

      // First Data Offset
      if (FieldValidator.checkOffset(fm.readInt(), arcSize)) {
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

      // 4 - Header (MGFs)
      // 2 - Version? (1)
      // 2 - Version? (1)
      // 4 - Archive Length
      fm.skip(12);

      // 4 - numFiles
      int numFiles = fm.readInt();
      FieldValidator.checkNumFiles(numFiles);

      long arcSize = fm.getLength();

      Resource[] resources = new Resource[numFiles];
      TaskProgressManager.setMaximum(numFiles);

      // 4 - First Data Offset
      // 4 - Unknown
      fm.skip(8);

      long[] sizes = new long[numFiles];
      for (int i = 0; i < numFiles; i++) {
        // 4 - Filename offset (read filename from this position until null)
        int filenameOffset = fm.readInt();
        FieldValidator.checkOffset(filenameOffset, arcSize);

        // 4 - Length?
        fm.skip(4);
        //long length = fm.readInt();

        // 4 - Unknown
        fm.skip(4);

        // 4 - Data Offset
        long offset = fm.readInt();
        FieldValidator.checkOffset(offset, arcSize);
        sizes[i] = offset;

        int currentPos = (int) fm.getOffset();
        fm.seek(filenameOffset);

        // X - Filename (null)
        String filename = fm.readNullString();
        FieldValidator.checkFilename(filename);

        fm.seek(currentPos);

        //path,id,name,offset,length,decompLength,exporter
        resources[i] = new Resource(path, filename, offset);

        TaskProgressManager.setValue(offset);
      }

      // Calculate File Sizes
      java.util.Arrays.sort(sizes);
      for (int i = 0; i < numFiles - 1; i++) {
        resources[i].setLength(sizes[i + 1] - sizes[i]);
        FieldValidator.checkLength(resources[i].getLength(), arcSize);
      }
      resources[numFiles - 1].setLength((int) fm.getLength() - sizes[numFiles - 1]);

      fm.close();

      return resources;

    }
    catch (Throwable t) {
      logError(t);
      return null;
    }
  }

}