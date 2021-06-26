
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
public class Plugin_SAD_LIONHEAD extends ArchivePlugin {

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public Plugin_SAD_LIONHEAD() {

    super("SAD_LIONHEAD", "Lionhead SAD");

    //         read write replace rename
    setProperties(true, false, false, false);

    setExtensions("sad");
    setGames("Black & White");
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
      if (fm.readString(8).equals("LiOnHeAd")) {
        rating += 50;
      }

      // Description
      if (fm.readString(21).equals("LHFileSegmentBankInfo")) {
        rating += 5;
      }

      fm.skip(11);

      long arcSize = fm.getLength();

      // File Start
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

      // 8 - Header (LiOnHeAd)
      // 21 - Description (LHFileSegmentBankInfo)
      // 11 - null
      fm.seek(40);

      // 4 - filestart
      int fileStart = fm.readInt() + (int) fm.getOffset() + 32;
      FieldValidator.checkOffset(fileStart, arcSize);
      fm.seek(fileStart);

      // 4 - filestart
      fileStart = fm.readInt() + (int) fm.getOffset() + 32;
      FieldValidator.checkOffset(fileStart, arcSize);

      int tailOffset = (int) fm.getOffset();

      fm.seek(fileStart);

      // 4 - offsetVal
      long offsetVal = fm.readInt();

      // 4 - numFiles
      int numFiles = fm.readInt();
      FieldValidator.checkNumFiles(numFiles);

      Resource[] resources = new Resource[numFiles];
      TaskProgressManager.setMaximum(numFiles);

      if (offsetVal > 0) {
        offsetVal = offsetVal / numFiles;
      }

      int currentPos = (int) fm.getOffset();
      for (int i = 0; i < numFiles; i++) {
        fm.seek(currentPos);

        // 2 - Drive Letter (C:)
        fm.skip(2);

        // 254 - Filename
        String filename = fm.readNullString(254);
        FieldValidator.checkFilename(filename);

        // 12 - Unknown
        fm.skip(12);

        // 4 - fileLength
        long length = fm.readInt();
        FieldValidator.checkLength(length, arcSize);

        // 4 - fileJump
        fm.skip(4);

        long offset = tailOffset;
        FieldValidator.checkOffset(offset, arcSize);
        tailOffset += length;

        currentPos += offsetVal;

        //path,id,name,offset,length,decompLength,exporter
        resources[i] = new Resource(path, filename, offset, length);

        TaskProgressManager.setValue(tailOffset);
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