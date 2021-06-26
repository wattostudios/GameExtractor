
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
public class Plugin_RWS_2 extends ArchivePlugin {

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public Plugin_RWS_2() {

    super("RWS_2", "RWS_2");

    //         read write replace rename
    setProperties(true, false, false, false);

    setGames("Neighbours From Hell");
    setExtensions("rws");
    setPlatforms("XBox");

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

      fm.skip(4);

      long arcSize = fm.getLength();

      // Archive Size
      if (fm.readInt() + 12 == arcSize) {
        rating += 5;
      }

      fm.skip(24);

      // Number Of Files
      if (FieldValidator.checkNumFiles(fm.readInt())) {
        rating += 5;
      }

      fm.skip(20);

      // Number Of Files
      if (FieldValidator.checkNumFiles(fm.readInt())) {
        rating += 5;
      }

      fm.skip(12);

      // First File Offset
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

      // 4 - Unknown
      // 4 - Archive Length [+12]
      // 4 - Unknown
      // 4 - Unknown
      // 4 - Unknown
      // 4 - Unknown
      // 4 - Unknown
      // 4 - Unknown (20)
      fm.skip(32);

      // 4 - Number Of Files
      int numFiles = fm.readInt();
      FieldValidator.checkNumFiles(numFiles);

      // 4 - Unknown (36)
      // 4 - Unknown (5)
      // 4 - Unknown
      // 4 - Unknown
      // 4 - null
      // 4 - Number Of Files
      // 4 - Unknown
      // 4 - Unknown (2)
      // 4 - Unknown
      fm.skip(36);

      // 4 - First File Offset (2048)
      int relOffset = fm.readInt();
      FieldValidator.checkOffset(relOffset, arcSize);

      // 4 - Unknown (4096)
      // 4 - Padding Multiple (2048)
      // 4 - null
      // 16 - CRC?
      // 16 - Archive Name (null, filled with junk) (no extension)
      fm.skip(44 + (numFiles * 56));

      Resource[] resources = new Resource[numFiles];

      TaskProgressManager.setMaximum(numFiles);

      // Loop through directory
      String[] names = new String[numFiles];
      for (int i = 0; i < numFiles; i++) {
        // 32 - Filename (null)
        String name = fm.readNullString(32);
        FieldValidator.checkFilename(name);
        names[i] = name;
      }

      fm.seek(120);

      // Loop through directory
      for (int i = 0; i < numFiles; i++) {
        // 4 - File ID?
        // 4 - File ID?
        // 4 - null
        // 4 - File ID?
        // 8 - null
        fm.skip(24);

        // 4 - File Length
        long length = fm.readInt();
        FieldValidator.checkLength(length, arcSize);

        // 4 - File Offset (relative to the start of the file data)
        long offset = fm.readInt() + relOffset;
        FieldValidator.checkOffset(offset, arcSize);

        String filename = names[i];

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
