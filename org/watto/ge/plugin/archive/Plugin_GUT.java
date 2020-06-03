
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
public class Plugin_GUT extends ArchivePlugin {

  /**
  **********************************************************************************************

  **********************************************************************************************
  **/
  public Plugin_GUT() {

    super("GUT", "GUT");

    //         read write replace rename
    setProperties(true, false, false, false);

    setExtensions("gut");
    setGames("Shadow Company: Left For Dead");
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
      if (fm.readString(62).equals("**************************************************************")) {
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

      // 326+(filenameLength*2)+(1 if month=10,11,12) - Header (62x"*" + (char)13 + (char)10 + "** Shadow Company: Left For Dead" + (char)13 + (char)10 + "**" + char)13 + (char)10 + "** [" + filename + "] : GUT resource file" + (char)13 + (char)10 + "** Copyright 1998 by Sinister Games Inc." + (char)13 + (char)10 + "**" + (char)13 + (char)10 + "** [gut_tool.exe] Build date: 17:58:56, Aug 24 1999" + (char)13 + (char)10 + "** [" + filename + "] Created: " + time as HH:MM:S + ", " + date as M/DD/YYYY + (char)13 + char(10) + 62x"*" + (char)13 + char(10))
      // 8 - Unknown
      // 4 - Unknown
      // 32 - filename (null)
      fm.seek(392);

      while (fm.readByte() == 0) {
      }

      int currentPos = (int) fm.getOffset() - 1;

      // 4 - Filename Length
      // 4 - File Length
      fm.skip(7); // 7 because of the while loop above

      // 4 - First Data Offset
      int numFiles = (fm.readInt() - currentPos) / 52;
      FieldValidator.checkNumFiles(numFiles);

      fm.seek(currentPos);

      long arcSize = fm.getLength();

      Resource[] resources = new Resource[numFiles];
      TaskProgressManager.setMaximum(numFiles);

      int headerLength = (int) fm.getOffset();
      for (int i = 0; i < numFiles; i++) {
        // 4 - Filename Length
        int filenameLength = fm.readInt();
        if (filenameLength > 1000) {
          resources = resizeResources(resources, i);
          i = numFiles;
        }
        else {
          FieldValidator.checkFilenameLength(filenameLength);

          // 4 - File Length
          long length = fm.readInt();
          FieldValidator.checkLength(length, arcSize);

          // 4 - Data Offset
          long offset = fm.readInt() + headerLength;
          FieldValidator.checkOffset(offset, arcSize);

          // 4 - null
          // 4 - Unknown
          fm.skip(8);

          // X - Encrypted Filename (including null) (XORed with 255 [FF])
          byte[] encName = fm.readBytes(filenameLength - 1);
          fm.skip(1);
          for (int e = 0; e < filenameLength - 1; e++) {
            encName[e] = (byte) (encName[e] ^ 201);
          }
          String filename = new String(encName);

          //path,id,name,offset,length,decompLength,exporter
          resources[i] = new Resource(path, filename, offset, length);

          TaskProgressManager.setValue(i);
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