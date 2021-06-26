
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
public class Plugin_IT_IMPM extends ArchivePlugin {

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public Plugin_IT_IMPM() {

    super("IT_IMPM", "IT_IMPM");

    //         read write replace rename
    setProperties(true, false, false, false);

    setEnabled(false);

    setGames("Jazz Jackrabbit 2");
    setExtensions("it");
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
      if (fm.readString(4).equals("IMPM")) {
        rating += 50;
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

      // 4 - Header (IMPM)
      // 98 - Unknown
      // 64 - Unknown (all @ symbols (byte)64)
      fm.skip(166);

      // X - File IDs
      // 1 - 255
      while (fm.readByte() != 255) {
      }

      int currentPos = (int) fm.getOffset();
      // Number Of Files
      int numFiles = (fm.readInt() - currentPos) / 4;
      FieldValidator.checkNumFiles(numFiles);
      fm.seek(currentPos);

      Resource[] resources = new Resource[numFiles];
      TaskProgressManager.setMaximum(numFiles);

      // Files Directory
      int[] offsets = new int[numFiles];
      for (int i = 0; i < numFiles; i++) {

        // 4 - File Offset
        long offset = fm.readInt();
        FieldValidator.checkOffset(offset, arcSize);

      }

      int realNumFiles = 0;
      for (int i = 0; i < numFiles; i++) {
        fm.seek(offsets[i]);

        // 4 - File Type
        if (fm.readString(4).equals("IMPS")) {

          // X - Filename
          String filename = fm.readNullString();

          // 4 - Unknown
          // 26 - File Description
          // 2 - Compressed True/False (1/0)?
          fm.skip(32);

          // 4 - Compressed File Size
          long length = fm.readInt();
          FieldValidator.checkLength(length, arcSize);

          // 8 - null
          // 4 - Decompressed File Size?
          // 8 - null
          fm.skip(20);

          // 4 - File Offset
          long offset = fm.readInt();
          FieldValidator.checkOffset(offset, arcSize);

          // 6 - null

          //path,id,name,offset,length,decompLength,exporter
          resources[realNumFiles] = new Resource(path, filename, offset, length);
          realNumFiles++;

          TaskProgressManager.setValue(i);
        }
      }

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
