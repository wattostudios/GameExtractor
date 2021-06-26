
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
public class Plugin_CWG_VW extends ArchivePlugin {

  /**
  **********************************************************************************************

  **********************************************************************************************
  **/
  public Plugin_CWG_VW() {

    super("CWG_VW", "CWG_VW");

    //         read write replace rename
    setProperties(true, false, false, false);
    setCanImplicitReplace(true);

    setExtensions("cwg");
    setGames("Castle Of The Winds");
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
      if (fm.readString(2).equals("vw")) {
        rating += 50;
      }

      // Version
      if (fm.readString(2).equals("11")) {
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

      // 2 - Header (vw)
      // 2 - Version ((byte)1 (byte)1) (ie 1.1)
      // 4 - Unknown
      fm.skip(8);

      int numFiles = Archive.getMaxFiles(4);

      long arcSize = fm.getLength();

      Resource[] resources = new Resource[numFiles];
      TaskProgressManager.setMaximum(arcSize);

      //int firstOffset = fm.readInt();
      //FieldValidator.checkOffset(firstOffset,arcSize);
      //fm.seek(8);

      //int readLength = 0;
      int i = 0;
      boolean hasNext = true;
      while (hasNext) {
        // 4 - Data Offset
        long offsetPointerLocation = fm.getOffset();
        long offsetPointerLength = 4;

        long offset = fm.readInt();
        FieldValidator.checkOffset(offset, arcSize);

        if (offset == 0) {
          hasNext = false;
        }
        else {
          String filename = Resource.generateFilename(i);

          //path,id,name,offset,length,decompLength,exporter
          resources[i] = new ReplacableResource(path, filename, offset, offsetPointerLocation, offsetPointerLength);

          TaskProgressManager.setValue(offset);
          i++;
        }
      }

      // Calculate File Sizes
      for (int j = 0; j < i - 1; j++) {
        resources[j].setLength((int) (resources[j + 1].getOffset() - resources[j].getOffset()));
        FieldValidator.checkLength(resources[j].getLength(), arcSize);
      }
      resources[i - 1].setLength((int) (arcSize - resources[i - 1].getOffset()));

      resources = resizeResources(resources, i);

      fm.close();

      return resources;

    }
    catch (Throwable t) {
      logError(t);
      return null;
    }

  }

}