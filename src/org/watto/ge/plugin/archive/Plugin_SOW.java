
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
public class Plugin_SOW extends ArchivePlugin {

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public Plugin_SOW() {

    super("SOW", "SOW");

    //         read write replace rename
    setProperties(true, false, false, false);

    setExtensions("sow");
    setGames("Descent 2");
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

      fm.skip(20);

      // null
      if (fm.readLong() == 0) {
        rating += 5;
      }

      // null
      if (fm.readInt() == 0) {
        rating += 5;
      }

      // null
      if (fm.readShort() == 0) {
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

      // 4 - Unknown (header?)
      // 4 - Unknown
      // 2 - Unknown (16)
      // 6 - Unknown
      // 4 - Unknown
      // 14 - null
      // 14 - Archive Filename
      fm.skip(48);

      int numFiles = Archive.getMaxFiles(4);//guess

      long arcSize = fm.getLength();

      Resource[] resources = new Resource[numFiles];
      TaskProgressManager.setMaximum(arcSize);

      int i = 0;
      while (fm.getOffset() < fm.getLength() - 10) {
        //System.out.println(fm.getOffset());
        // 2 - Unknown
        // 4 - Unknown
        // 4 - Unknown
        // 4 - Unknown
        // 4 - Unknown
        // 4 - Unknown
        fm.skip(22);

        // 4 - Compressed File Length
        long length = fm.readInt();
        FieldValidator.checkLength(length, arcSize);

        // 4 - Uncompressed File Length
        // 4 - Unknown
        // 2 - null
        // 4 - Unknown (file ID?)
        fm.skip(14);

        // X - Filename (double-null)
        String filename = fm.readNullString();
        fm.skip(1);
        FieldValidator.checkFilename(filename);

        // X - File Data
        long offset = (int) fm.getOffset();
        fm.skip(length);

        //path,id,name,offset,length,decompLength,exporter
        resources[i] = new Resource(path, filename, offset, length);

        TaskProgressManager.setValue(offset);
        i++;
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