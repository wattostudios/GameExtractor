
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
public class Plugin_DAS_DASP extends ArchivePlugin {

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public Plugin_DAS_DASP() {

    super("DAS_DASP", "DAS_DASP");

    //         read write replace rename
    setProperties(true, false, false, false);

    setExtensions("das");
    setGames("Normality");
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
      if (fm.readString(4).equals("DASP")) {
        rating += 50;
      }

      fm.skip(12);

      // Archive Size
      if (fm.readInt() == fm.getLength()) {
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

      // 4 - header (DASP)
      // 2 - numFiles?
      // 4 - Unknown
      // 2 - Unknown
      // 4 - Archive Length
      fm.skip(16);

      int numFiles = Archive.getMaxFiles(4);//guess

      long arcSize = fm.getLength();

      Resource[] resources = new Resource[numFiles];
      TaskProgressManager.setMaximum(arcSize);

      int nullCount = 0;
      int i = 0;
      boolean hasNext = true;
      int readLength = 0;
      while (hasNext) {
        // 4 - Offset (ignore or skip offsets of zero)
        long offset = fm.readInt();
        FieldValidator.checkOffset(offset, arcSize);

        // 2 - Half the Length (ie actual file size = size*2)
        long length = fm.readShort() * 2;
        //FieldValidator.checkLength(length,arcSize);

        // 2 - Unknown
        fm.skip(2);

        String filename = Resource.generateFilename(i);

        if (offset > 0 && length > 0) {
          nullCount = 0;
          //path,id,name,offset,length,decompLength,exporter
          resources[i] = new Resource(path, filename, offset, length);

          TaskProgressManager.setValue(readLength);
          readLength += length;
          i++;
        }
        else {
          nullCount++;
          if (nullCount > 5) {
            hasNext = false;
          }
        }
      }

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