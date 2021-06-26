
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
public class Plugin_PIG_PPIG extends ArchivePlugin {

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public Plugin_PIG_PPIG() {

    super("PIG_PPIG", "PIG_PPIG");

    //         read write replace rename
    setProperties(true, false, false, false);

    setExtensions("pig");
    setGames("Descent 2");
    setPlatforms("PC");

    setFileTypes("spr", "Object Sprite");

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
      if (fm.readString(4).equals("PPIG")) {
        rating += 50;
      }

      fm.skip(4);

      // Number Of Files
      if (FieldValidator.checkNumFiles(fm.readInt())) {
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

      // 4 - Header

      // 4 - Version

      // 4 - Number Of Files
      fm.skip(8);
      int numFiles = fm.readInt();
      FieldValidator.checkNumFiles(numFiles);

      long arcSize = fm.getLength();

      Resource[] resources = new Resource[numFiles];
      TaskProgressManager.setMaximum(numFiles);

      int currentPos = 12 + (18 * numFiles);
      int readLength = 0;
      for (int i = 0; i < numFiles; i++) {
        // 8 - Filename
        String filename = fm.readNullString(8);
        FieldValidator.checkFilename(filename);

        // 1 Byte - Frame Num
        // 1 Byte - X Length part1
        // 1 Byte - Y Length part1
        // 1 Byte - X/Y Length part2
        // 1 Byte - Compression Type
        // 1 Byte - Average Color
        fm.skip(6);

        // 4 - Data Offset
        long offset = fm.readInt();
        FieldValidator.checkOffset(offset, arcSize);

        long length;
        if (i == numFiles - 1) {
          offset += currentPos;
          length = ((int) path.length()) - offset;
        }
        else {
          int curFilePos = (int) fm.getOffset();
          fm.skip(14);
          length = fm.readInt() - offset;
          fm.seek(curFilePos);
          offset += currentPos;
        }

        //path,id,name,offset,length,decompLength,exporter
        resources[i] = new Resource(path, filename, offset, length);

        TaskProgressManager.setValue(readLength);
        readLength += length;
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