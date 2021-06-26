
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
public class Plugin_RWS extends ArchivePlugin {

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public Plugin_RWS() {

    super("RWS", "RWS");

    //         read write replace rename
    setProperties(true, false, false, false);

    setGames("Call Of Duty: Finest Hour",
        "Burnout Revenge");
    setExtensions("rws");
    setPlatforms("PS2", "XBox");

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

      fm.skip(52);

      // 4 - null
      if (fm.readInt() == 0) {
        rating += 5;
      }

      // 4 - Number Of Files
      if (FieldValidator.checkNumFiles(fm.readInt())) {
        rating += 5;
      }

      fm.skip(12);

      // 4 - Unknown (2048)
      if (fm.readInt() == 2048) {
        rating += 5;
      }

      fm.skip(4);

      // 4 - End Of Archive Padding Size (8192)
      if (fm.readInt() == 8192) {
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

      // NOTE - Compressed file MUST know their DECOMPRESSED LENGTH
      //      - Uncompressed files MUST know their LENGTH

      addFileTypes();

      // RESETTING THE GLOBAL VARIABLES

      FileManipulator fm = new FileManipulator(path, false);

      long arcSize = (int) fm.getLength();

      // 4 - Unknown
      // 8 - CRC?
      // 4 - Unknown
      // 4 - Unknown
      // 4 - Unknown
      // 4 - Unknown
      // 4 - Unknown (20)
      // 4 - Unknown (16)
      // 4 - Unknown (36)
      // 4 - Archive Name Length?
      // 4 - Unknown
      // 4 - Unknown
      // 4 - null
      fm.skip(56);

      // 4 - Number Of Files
      int numFiles = fm.readInt();
      FieldValidator.checkNumFiles(numFiles);

      // 4 - Unknown
      // 4 - Unknown (1)
      // 4 - Unknown
      // 4 - Unknown (2048)
      // 4 - Unknown
      fm.skip(20);

      // 4 - End Of Archive Padding Size (8192)
      int firstFileOffset = fm.readInt();
      FieldValidator.checkOffset(firstFileOffset, arcSize);

      // 4 - null
      // 16 - Unknown
      fm.skip(20);

      // X - Archive Name
      // 1 - null Archive Name Terminator
      fm.readNullString();

      // 4 - Unknown
      // 4 - Unknown (39)
      fm.skip(8);

      Resource[] resources = new Resource[numFiles];

      TaskProgressManager.setMaximum(numFiles);

      // Loop through directory
      for (int i = 0; i < numFiles; i++) {
        // 4 - Unknown
        // 4 - Unknown
        // 4 - null
        // 4 - Unknown ID (incremental by 4)
        // 8 - null
        fm.skip(24);

        // 4 - File Size
        long length = fm.readInt();
        FieldValidator.checkLength(length, arcSize);

        // 4 - File Offset [+firstFileOffset]
        long offset = fm.readInt() + firstFileOffset;
        FieldValidator.checkOffset(offset, arcSize);

        String filename = Resource.generateFilename(i);

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
