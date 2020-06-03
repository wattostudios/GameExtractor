
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
public class Plugin_TGW extends ArchivePlugin {

  /**
  **********************************************************************************************

  **********************************************************************************************
  **/
  public Plugin_TGW() {

    super("TGW", "TGW");

    //         read write replace rename
    setProperties(true, false, false, false);

    setExtensions("tgw");
    setGames("Kohan");
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

      fm.skip(60);

      long arcSize = fm.getLength();

      // First File Offset
      if (FieldValidator.checkOffset(fm.readInt(), arcSize)) {
        rating += 5;
      }

      fm.skip(16);

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
  @SuppressWarnings("unused")
  public Resource[] read(File path) {
    try {

      addFileTypes();

      FileManipulator fm = new FileManipulator(path, false);
      long arcSize = fm.getLength();

      // 2 - Unknown (6)
      // 2 - Unknown (1)
      // 4 - Unknown (30000000)
      // 4 - Unknown (1000)
      // 4 - Unknown (20)
      // 4 - Unknown (1000000)
      // 4 - Unknown (2)
      // 4 - Unknown (2)
      // 4 - Unknown
      // 4 - Unknown
      // 4 - Filename Directory Offset
      // 4 - null
      // 4 - null
      // 4 - null
      // 4 - Filename Directory Offset
      // 4 - null
      fm.skip(60);

      // 4 - Filename Directory Offset
      int filenameDirOffset = fm.readInt();
      FieldValidator.checkOffset(filenameDirOffset, arcSize);

      // 4 - Number Of Files
      int numFiles = fm.readInt();
      FieldValidator.checkNumFiles(numFiles);

      // 4 - File Length Directory Offset
      // 4 - Number Of Files
      fm.skip(8);

      // 4 - File Offset Directory Offset
      long offsetDirOffset = fm.readInt();
      FieldValidator.checkOffset(offsetDirOffset, arcSize);

      // 4 - Number Of Files
      // 4 - null
      // 4 - null
      // 4 - null
      fm.skip(16);

      // 4 - First File Data Offset
      long offset = fm.readInt();
      FieldValidator.checkOffset(offset, arcSize);

      // 4 - Unknown
      // 4 - null
      // 4 - null
      // 4 - null
      fm.seek(filenameDirOffset);

      Resource[] resources = new Resource[numFiles];
      TaskProgressManager.setMaximum(numFiles);

      for (int i = 0; i < numFiles; i++) {
        // 80 - Filename (null)
        String filename = fm.readNullString(80);
        FieldValidator.checkFilename(filename);

        // 4 - Unknown
        fm.skip(4);

        // 4 - File Length
        long length = fm.readInt();
        FieldValidator.checkLength(length, arcSize);

        // 4 - Unknown (1)
        fm.skip(4);

        // 4 - File ID (incremental starting from 0)
        int fileID = fm.readInt();

        // 4 - Unknown
        // 4 - Unknown
        fm.skip(8);

        //path,id,name,offset,length,decompLength,exporter
        resources[i] = new Resource(path, filename, offset, length);

        TaskProgressManager.setValue(i);

        offset += length;
      }

      fm.seek(offsetDirOffset);

      for (int i = 0; i < numFiles; i++) {
        // 4 - Offset
        int realOffset = fm.readInt();
        FieldValidator.checkOffset(realOffset, arcSize);

        resources[i].setOffset(realOffset);

        // 4 - Offset
        fm.skip(4);
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