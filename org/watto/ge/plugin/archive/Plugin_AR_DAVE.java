
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
public class Plugin_AR_DAVE extends ArchivePlugin {

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public Plugin_AR_DAVE() {

    super("AR_DAVE", "AR_DAVE");

    //         read write replace rename
    setProperties(true, false, false, false);

    setGames("Midtown Madness 2");
    setExtensions("ar");
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
      if (fm.readString(4).equals("DAVE")) {
        rating += 50;
      }

      fm.skip(4);

      long arcSize = fm.getLength();

      // 4 - Filename Offset
      if (FieldValidator.checkOffset(fm.readInt(), arcSize)) {
        rating += 5;
      }

      // 4 - Data Offset
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

      // 4 - Header (DAVE)
      // 4 - Unknown (numFiles?)
      fm.skip(8);

      // 4 - Filename Offset + 2048
      int filenameOffset = fm.readInt() + 2048;
      FieldValidator.checkOffset(filenameOffset, arcSize);

      // 4 - Data Offset + Filename Offset + 2048
      int fileDataOffset = fm.readInt() + 2048 + filenameOffset;
      FieldValidator.checkOffset(fileDataOffset, arcSize);

      String[] names = new String[Archive.getMaxFiles(4)];

      fm.seek(filenameOffset);

      boolean hasMoreFiles = true;
      int numFiles = 0;
      while (hasMoreFiles) {
        // X - Filename (null)
        String filename = fm.readNullString();

        if (filename.length() == 0) {
          hasMoreFiles = false;
        }
        else {
          FieldValidator.checkFilename(filename);
          names[numFiles] = filename;
          numFiles++;
        }
      }

      fm.seek(2048);

      Resource[] resources = new Resource[numFiles];
      TaskProgressManager.setMaximum(numFiles);

      // Loop through directory
      for (int i = 0; i < numFiles; i++) {
        // 4 - File ID
        fm.skip(4);

        // 4 - Data Offset
        long offset = fm.readInt();
        FieldValidator.checkOffset(offset, arcSize);

        // 4 - Raw File Length
        long length = fm.readInt();
        FieldValidator.checkLength(length, arcSize);

        // 4 - Compressed File Length
        int compLength = fm.readInt();
        FieldValidator.checkLength(compLength, arcSize);

        //path,id,name,offset,length,decompLength,exporter
        resources[i] = new Resource(path, names[i], offset, compLength);

        TaskProgressManager.setValue(i);
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
