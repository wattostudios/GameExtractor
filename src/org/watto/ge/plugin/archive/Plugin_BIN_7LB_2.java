
package org.watto.ge.plugin.archive;

import java.io.File;
import org.watto.task.TaskProgressManager;
import org.watto.datatype.ReplacableResource;
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
public class Plugin_BIN_7LB_2 extends ArchivePlugin {

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public Plugin_BIN_7LB_2() {

    super("BIN_7LB_2", "7th Level Engine - Format b");

    //         read write replace rename
    setProperties(true, false, false, false);
    setCanImplicitReplace(true);

    setGames("Ace Ventura");
    setExtensions("bin");
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
      if (fm.readString(3).equals("7Lb")) {
        rating += 50;
      }

      fm.skip(1);

      // Version
      if (fm.readInt() == 144945) {
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

      // 4 - Header (7Lb + (byte)1)
      // 4 - Unknown (144945)
      // 78 - Description (null)
      // 4 - Unknown (2)
      // 8 - null
      // 4 - Unknown
      // 92 - null
      // 2 - Unknown (1/2)
      // 4 - Unknown (236)
      // 4 - Unknown
      // 4 - Unknown
      // 2 - Unknown
      fm.skip(242);

      // 4 - Directory Offset
      long dirOffset = fm.readInt();
      FieldValidator.checkOffset(dirOffset, arcSize);

      // 4 - Directory Length
      int numFiles = fm.readInt() / 10;
      FieldValidator.checkNumFiles(numFiles);

      // 16 - null
      // 4 - Counter Directory Offset
      // 4 - Counter Directory Length
      // 4 - Filename Directory Offset
      // 4 - Filename Directory Length
      // 4 - Unknown Directory Offset
      // 4 - Unknown Directory Length
      // 4 - Image Offset
      // 4 - Image Length
      // 24 - null
      // 4 - Functions Directory 1
      // 4 - Functions Directory 1 Length
      // 4 - Functions Directory 2
      // 4 - Functions Directory 2 Length
      // 4 - End Directory Offset
      // 4 - End Directory Length

      fm.seek(dirOffset);

      Resource[] resources = new Resource[numFiles];
      TaskProgressManager.setMaximum(numFiles);

      // Loop through directory
      for (int i = 0; i < numFiles; i++) {
        // 2 - File Type ID? (1-18)
        String ext = "." + fm.readShort();

        // 4 - Offset
        long offsetPointerLocation = fm.getOffset();
        long offsetPointerLength = 4;

        long offset = fm.readInt();
        FieldValidator.checkOffset(offset, arcSize);

        // 4 - Length
        long lengthPointerLocation = fm.getOffset();
        long lengthPointerLength = 4;

        long length = fm.readInt();
        FieldValidator.checkLength(length, arcSize);

        //String filename = names[i] + ext;
        String filename = Resource.generateFilename(i) + ext;

        //path,id,name,offset,length,decompLength,exporter
        resources[i] = new ReplacableResource(path, filename, offset, offsetPointerLocation, offsetPointerLength, length, lengthPointerLocation, lengthPointerLength);

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
