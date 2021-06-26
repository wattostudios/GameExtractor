
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
public class Plugin_EPF_EPFS extends ArchivePlugin {

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public Plugin_EPF_EPFS() {

    super("EPF_EPFS", "EPF_EPFS");

    //         read write replace rename
    setProperties(true, false, false, false);

    setExtensions("epf");
    setGames("Overdrive", "Project X", "Tower Assult");
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
      if (fm.readString(4).equals("EPFS")) {
        rating += 50;
      }

      long arcSize = fm.getLength();

      // Directory Offset
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
      long arcSize = fm.getLength();

      // 4 - Header (EPFS)
      fm.skip(4);

      // 4 - dirOffset
      long dirOffset = fm.readInt();
      FieldValidator.checkOffset(dirOffset, arcSize);

      int numFiles = (int) ((arcSize - dirOffset) / 22);//guess

      Resource[] resources = new Resource[numFiles];
      TaskProgressManager.setMaximum(arcSize);

      fm.seek(dirOffset);

      long[] offsets = new long[numFiles];
      for (int i = 0; i < numFiles; i++) {
        // 14 - filename (null) (filled up with non-null junk after the first null)
        String filename = fm.readNullString(14);
        FieldValidator.checkFilename(filename);

        // 4 - Data Offset
        long offset = fm.readInt();
        offsets[i] = offset;
        FieldValidator.checkOffset(offset, arcSize);

        // 4 - Unknown (type? ID?)
        fm.skip(4);

        //path,id,name,offset,length,decompLength,exporter
        resources[i] = new Resource(path, filename, offset);

        TaskProgressManager.setValue(i);
      }

      java.util.Arrays.sort(offsets);

      long[] sizes = new long[numFiles];
      for (int i = 0; i < numFiles - 1; i++) {
        sizes[i] = offsets[i + 1] - offsets[i];
      }
      sizes[numFiles - 1] = dirOffset - offsets[numFiles - 1];

      for (int i = 0; i < numFiles; i++) {
        for (int j = 0; j < numFiles; j++) {
          if (resources[j].getOffset() == offsets[i]) {
            resources[i].setLength(sizes[i]);
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