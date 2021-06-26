
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
import org.watto.io.converter.IntConverter;

/**
**********************************************************************************************

**********************************************************************************************
**/
public class Plugin_RNC_RNCA extends ArchivePlugin {

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public Plugin_RNC_RNCA() {

    super("RNC_RNCA", "RNC_RNCA");

    //         read write replace rename
    setProperties(true, false, false, false);

    setExtensions("rnc");
    setGames("F-14 Fleet Defender");
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
      if (fm.readString(4).equals("RNCA")) {
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

      // 4 - Header (RNCA)
      // 4 - Unknown
      // 3 - Unknown
      fm.skip(11);

      // X - Filename (null)
      String filename1 = fm.readNullString();
      FieldValidator.checkFilename(filename1);

      // 4 - Data Offset
      int firstDataOffset = IntConverter.changeFormat(fm.readInt());
      FieldValidator.checkOffset(firstDataOffset, arcSize);

      fm.seek(11);
      int numFiles = Archive.getMaxFiles(4);

      Resource[] resources = new Resource[numFiles];
      TaskProgressManager.setMaximum(arcSize);

      int currentPos = 11;
      int i = 0;
      while (currentPos < firstDataOffset) {
        // X - Filename (null)
        String filename = fm.readNullString();
        if (filename.length() == 0) {
          fm.skip(2);
          String filenameJunk = fm.readNullString();
          currentPos += 2 + filenameJunk.length() + 1;
        }

        else {
          currentPos += filename.length() + 1 + 4;

          //System.out.println(filename);
          FieldValidator.checkFilename(filename);

          // 4 - Data Offset
          long offset = IntConverter.changeFormat(fm.readInt());
          FieldValidator.checkOffset(offset, arcSize);

          //path,id,name,offset,length,decompLength,exporter
          resources[i] = new Resource(path, filename, offset);

          TaskProgressManager.setValue(currentPos);
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