
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
public class Plugin_CRH_MDMD extends ArchivePlugin {

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public Plugin_CRH_MDMD() {

    super("CRH_MDMD", "CRH_MDMD");

    //         read write replace rename
    setProperties(true, false, false, false);
    setCanImplicitReplace(true);

    setExtensions("crh", "mga", "fga", "lz", "wlz");
    setGames("Links 368 Pro");
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
      if (fm.readString(4).equals("MDmd")) {
        rating += 50;
      }

      fm.skip(2);

      // Directory Offset (122)
      if (fm.readShort() == 122) {
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

      // 4 - Header (MDmd)
      // 2 - Unknown (11)
      fm.skip(6);

      // 2 - Dir Offset (122)
      long dirOffset = fm.readShort();
      FieldValidator.checkOffset(dirOffset, arcSize);

      int numFiles = Archive.getMaxFiles(4);//guess

      Resource[] resources = new Resource[numFiles];
      TaskProgressManager.setMaximum(numFiles);

      fm.seek(dirOffset);

      fm.skip(13);
      int firstFileOffset = fm.readInt();
      fm.seek(dirOffset);

      int i = 0;
      boolean hasNext = true;
      while (hasNext) {
        // 13 - Filename (stop at first null - filled with spaces to the null at pos 13 - so max filename length is 11 + 2 nulls)
        String filename = fm.readNullString(13);
        if (filename.equals("        .   ")) {
          hasNext = false;
        }
        else if (fm.getOffset() >= firstFileOffset) {
          hasNext = false;
        }
        else {
          FieldValidator.checkFilename(filename);

          // 4 - Offset
          long offsetPointerLocation = fm.getOffset();
          long offsetPointerLength = 4;

          long offset = fm.readInt();
          FieldValidator.checkOffset(offset, arcSize);

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