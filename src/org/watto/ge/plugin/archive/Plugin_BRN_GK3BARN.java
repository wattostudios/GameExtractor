
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
public class Plugin_BRN_GK3BARN extends ArchivePlugin {

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public Plugin_BRN_GK3BARN() {

    super("BRN_GK3BARN", "BRN_GK3BARN");

    //         read write replace rename
    setProperties(true, false, false, false);

    setGames("Gabriel Knight 3");
    setExtensions("brn");
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
      if (fm.readString(8).equals("GK3!Barn")) {
        rating += 50;
      }

      // null
      if (fm.readShort() == 0) {
        rating += 5;
      }

      // Version
      if (fm.readInt() == 1) {
        rating += 5;
      }

      fm.skip(54);

      // Copyright
      if (fm.readString(10).equals("Copyright ")) {
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
   * Reads an [archive] File into the Resources
   **********************************************************************************************
   **/
  @Override
  public Resource[] read(File path) {
    try {

      // NOTE - Compressed files MUST know their DECOMPRESSED LENGTH
      //      - Uncompressed files MUST know their LENGTH

      addFileTypes();

      // RESETTING THE GLOBAL VARIABLES

      FileManipulator fm = new FileManipulator(path, false);

      long arcSize = (int) fm.getLength();

      // 8 - Header (GK3!Barn)
      // 2 - null
      // 4 - Version (1)
      // 54 - Unknown
      // 100 - Copyright ("Copyright " + (byte)169 + " 1999 Sierra Studios. All rights reserved." + nulls to fill)
      // 100 - Archive Build Details (null terminated)
      // 4 - Unknown (2)
      // 4 - Data Directory Header (riDD)
      // 2 - Unknown (2)
      // 2 - Unknown (1)
      // 8 - CRC?
      // 4 - Unknown
      // 4 - Unknown
      fm.skip(296);

      // 4 - Directory Offset
      long dirOffset = fm.readInt();
      FieldValidator.checkOffset(dirOffset, arcSize);

      int numFiles = (int) ((dirOffset - 428) / 4);
      FieldValidator.checkNumFiles(numFiles);

      // 4 - Data Header (ataD)
      // 2 - null
      // 2 - Unknown (1)
      // 4 - Unknown
      // 4 - null
      // 4 - Unknown
      fm.skip(20);

      // 4 - End Of Directory Offset [-5]
      int firstFileOffset = fm.readInt() - 5;
      int paddingSize = 4096 - (firstFileOffset % 4096);
      if (paddingSize < 4096) {
        firstFileOffset += paddingSize;
      }

      firstFileOffset -= 4064;

      fm.seek(dirOffset);

      Resource[] resources = new Resource[numFiles];

      TaskProgressManager.setMaximum(numFiles);

      // Loop through directory
      for (int i = 0; i < numFiles; i++) {
        // 4 - File Length
        long length = fm.readInt();
        FieldValidator.checkLength(length, arcSize);

        // 4 - File Offset (relative to the start of the file data)
        long offset = fm.readInt() + firstFileOffset;
        //System.out.println(i + " of " + numFiles + ": " + offset);
        FieldValidator.checkOffset(offset, arcSize);

        // 4 - Hash?
        // 3 - Unknown
        // 1 - Filename Length (not including null)
        fm.skip(8);

        // X - Filename
        // 1 - null Filename Terminator
        String filename = fm.readNullString();
        FieldValidator.checkFilename(filename);

        //path,id,name,offset,length,decompLength,exporter
        resources[i] = new Resource(path, filename, offset, length);

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
