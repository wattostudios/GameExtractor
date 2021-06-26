
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
public class Plugin_RWD_TGCK extends ArchivePlugin {

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public Plugin_RWD_TGCK() {

    super("RWD_TGCK", "RWD_TGCK");

    //         read write replace rename
    setProperties(true, false, false, false);

    setGames("Kohan 2: Kings Of War",
        "Axis And Allies");
    setExtensions("rwd");
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
      if (fm.readString(4).equals("TGCK")) {
        rating += 50;
      }

      // Unknown (2)
      if (fm.readInt() == 2) {
        rating += 5;
      }

      // Unknown (3)
      if (fm.readInt() == 3) {
        rating += 5;
      }

      // Unknown (2)
      if (fm.readInt() == 2) {
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

      // 4 - Header (TGCK)
      // 4 - Unknown (2)
      // 4 - Unknown (3)
      // 4 - Unknown (2)
      // 2 - Description Length [*2, as it is unicode text]
      // X - Description (Unicode text)
      // 4 - null
      // 4 - Unknown

      fm.seek(arcSize - 288);

      // HEADER DETAILS
      // 64 - Header Name (Header) (Unicode Text)
      // 8 - Header Offset (12)
      // 8 - Header Length
      // 4 - Unknown
      // 4 - Unknown
      // 8 - Header Length

      // FILE DATA DETAILS
      // 64 - File Data Name (Files) (Unicode Text)
      fm.skip(160);

      // 8 - File Data Offset
      int relOffset = (int) fm.readLong();
      FieldValidator.checkOffset(relOffset, arcSize);

      // 8 - File Data Length
      // 4 - Unknown
      // 4 - Unknown
      // 8 - File Data Length

      // FOOTER DETAILS
      // 64 - Directory Name (Footer) (Unicode Text)
      fm.skip(88);

      // 8 - Directory Offset
      long dirOffset = (int) fm.readLong();
      FieldValidator.checkOffset(dirOffset, arcSize);

      // 8 - Directory Length
      int dirLength = (int) fm.readLong();
      FieldValidator.checkLength(dirLength, arcSize);

      // 4 - Unknown
      // 4 - Unknown
      // 8 - Directory Length

      // Approximation only
      int numFiles = dirLength / 26;
      FieldValidator.checkNumFiles(numFiles);

      Resource[] resources = new Resource[numFiles];
      TaskProgressManager.setMaximum(arcSize);

      fm.seek(dirOffset);

      // Loop through directory
      int realNumFiles = 0;
      long dirEnd = dirOffset + dirLength - 4;
      int readPos = 0;
      while (fm.getOffset() < dirEnd) {
        // 4 - File Type ID?
        fm.skip(4);

        // 2 - Filename Length [*2, as it is unicode text]
        int filenameLength = fm.readShort();
        FieldValidator.checkFilenameLength(filenameLength);

        // X - Filename (Unicode Text)
        String filename = fm.readUnicodeString(filenameLength);

        // 8 - Offset [+X] (relative to the start of the file data)
        long offset = (int) fm.readLong() + relOffset;
        FieldValidator.checkOffset(offset, arcSize);

        // 8 - Length
        long length = (int) fm.readLong();
        FieldValidator.checkLength(length, arcSize);

        // 4 - null
        fm.skip(4);

        //path,id,name,offset,length,decompLength,exporter
        resources[realNumFiles] = new Resource(path, filename, offset, length);

        TaskProgressManager.setValue(readPos);
        realNumFiles++;
        readPos += length;
      }

      resources = resizeResources(resources, realNumFiles);

      fm.close();

      return resources;

    }
    catch (Throwable t) {
      logError(t);
      return null;
    }
  }

}
