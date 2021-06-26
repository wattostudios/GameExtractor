
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

/**
**********************************************************************************************

**********************************************************************************************
**/
public class Plugin_SAM_RIFF extends ArchivePlugin {

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public Plugin_SAM_RIFF() {

    super("SAM_RIFF", "SAM_RIFF");

    //         read write replace rename
    setProperties(true, false, false, false);

    setEnabled(false); // DO NOT ALLOW THIS PLUGIN TO BE USED!

    setExtensions("sam");
    setGames("Le Mans 24 Hours",
        "Test Drive Le Mans");
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
      if (fm.readString(4).equals("RIFF")) {
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

      int numFiles = Archive.getMaxFiles(4);//guess

      long arcSize = fm.getLength();

      Resource[] resources = new Resource[numFiles];
      TaskProgressManager.setMaximum(arcSize);

      int i = 0;
      while (fm.getOffset() < fm.getLength()) {
        long offset = (int) fm.getOffset();
        // 4 - WAVE Header (RIFF)
        fm.skip(4);

        // 4 - File Length [+8]
        long length = fm.readInt();
        FieldValidator.checkLength(length, arcSize);

        // X - File Data
        fm.skip(length);

        length += 8;

        long padding = 16 - (length % 16);
        if (padding < 16) {
          fm.skip(padding);
        }

        int currentPos = (int) fm.getOffset();

        int numToSkip = 0;
        String read = fm.readString(1);
        boolean hasNext = true;
        while (!read.equals("R") && hasNext) {
          if (fm.getOffset() >= fm.getLength()) {
            hasNext = false;
            numToSkip += 50;
          }
          read = fm.readString(1);
          numToSkip++;
        }

        fm.seek(currentPos + numToSkip);

        String filename = Resource.generateFilename(i) + ".wav";

        //path,id,name,offset,length,decompLength,exporter
        resources[i] = new Resource(path, filename, offset, length);

        TaskProgressManager.setValue(offset);

        i++;
      }

      resources = resizeResources(resources, i - 1);

      fm.close();

      return resources;

    }
    catch (Throwable t) {
      logError(t);
      return null;
    }
  }

}