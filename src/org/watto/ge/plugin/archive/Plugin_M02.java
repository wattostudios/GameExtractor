
package org.watto.ge.plugin.archive;

import java.io.File;
import org.watto.task.TaskProgressManager;
import org.watto.datatype.Archive;
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
import org.watto.io.FilenameSplitter;

/**
**********************************************************************************************

**********************************************************************************************
**/
public class Plugin_M02 extends ArchivePlugin {

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public Plugin_M02() {

    super("M02", "M02");

    //         read write replace rename
    setProperties(true, false, false, false);

    setGames("Hollywood Monsters",
        "Runaway");
    setExtensions("m**", "s**");
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

      String ext = FilenameSplitter.getExtension(fm.getFile());
      int fileNum = Integer.parseInt(ext.substring(1));
      ext = ext.substring(0, 1);

      if ((ext.equals("m") || ext.equals("s") || ext.equals("d") || ext.equals("g")) && (fileNum >= 0 && fileNum <= 99)) {
        rating += 25;
      }

      // null
      if (fm.readInt() == 0) {
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

      int numFiles = Archive.getMaxFiles(4);

      long arcSize = fm.getLength();

      Resource[] resources = new Resource[numFiles];
      TaskProgressManager.setMaximum(0);

      // Loop through directory
      int realNumFiles = 0;
      long lastOffset = -1;
      boolean finished = false;
      while (!finished) {
        // 4 - Data Offset
        long offset = fm.readInt();

        if (offset == arcSize) {
          // reached the end of the directory
          finished = true;
        }
        else if (offset == lastOffset) {
          // ignore duplicate offsets
        }
        else if (offset == 0 && lastOffset != -1) {
          // reached the null padding after the directory
          finished = true;
        }
        else {
          FieldValidator.checkOffset(offset, arcSize);
          lastOffset = offset;

          String filename = Resource.generateFilename(realNumFiles);

          //path,id,name,offset,length,decompLength,exporter
          resources[realNumFiles] = new Resource(path, filename, offset);

          TaskProgressManager.setValue(offset);
          realNumFiles++;
        }
      }

      fm.close();

      resources = resizeResources(resources, realNumFiles);
      calculateFileSizes(resources, arcSize);

      return resources;

    }
    catch (Throwable t) {
      logError(t);
      return null;
    }
  }

}
