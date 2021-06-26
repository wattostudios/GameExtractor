
package org.watto.ge.plugin.archive;

import java.io.File;
import org.watto.task.TaskProgressManager;
import org.watto.datatype.Archive;
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
public class Plugin_CAM_CYLBPC_2 extends ArchivePlugin {

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public Plugin_CAM_CYLBPC_2() {

    super("CAM_CYLBPC_2", "CAM_CYLBPC_2");

    //         read write replace rename
    setProperties(true, false, false, false);
    setCanImplicitReplace(true);

    setGames("Playboy: The Mansion");
    setExtensions("cam");
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
      if (fm.readString(8).equals("CYLBPC  ")) {
        rating += 50;
      }

      // Version Main
      if (fm.readShort() == 2) {
        rating += 5;
      }

      // Version Sub
      if (fm.readShort() == 1) {
        rating += 5;
      }

      // Number Of File Types
      int numFiles = fm.readInt();
      if (FieldValidator.checkNumFiles(numFiles)) {
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

      // 8 - Header (CYLBPC  )
      // 2 - Version Main (2)
      // 2 - Version Sub (1)
      fm.skip(12);

      // 4 - Number Of File Types
      int numFileTypes = fm.readInt();
      FieldValidator.checkNumFiles(numFileTypes);

      int numFiles = Archive.getMaxFiles(4);

      long arcSize = fm.getLength();

      Resource[] resources = new Resource[numFiles];
      TaskProgressManager.setMaximum(arcSize);

      // Loop through directory
      int realNumFiles = 0;
      for (int i = 0; i < numFileTypes; i++) {
        // 4 - Size of the directory for this file type [+8] (including these 4 fields)
        fm.skip(4);

        // 4 - File Type Description (WAVE)
        String extension = fm.readString(4);

        // 4 - Size of each file entry (28)
        FieldValidator.checkEquals(fm.readInt(), 28);

        // 4 - Number Of Files of this type
        int numFilesOfType = fm.readInt();

        // 4 - null
        fm.skip(4);

        for (int j = 0; j < numFilesOfType; j++) {

          // 4 - Filename? File ID?
          String filename = fm.readString(4) + "." + extension;

          // 4 - File Offset
          long offsetPointerLocation = fm.getOffset();
          long offsetPointerLength = 4;

          long offset = fm.readInt();
          FieldValidator.checkOffset(offset, arcSize);

          // 4 - File Size
          long lengthPointerLocation = fm.getOffset();
          long lengthPointerLength = 4;

          long length = fm.readInt();
          FieldValidator.checkLength(length, arcSize);

          if (extension.equals("PICT")) {
            offset += 4;
            length -= 4;
          }

          // 4 - null
          fm.skip(4);

          //path,id,name,offset,length,decompLength,exporter
          resources[realNumFiles] = new ReplacableResource(path, filename, offset, offsetPointerLocation, offsetPointerLength, length, lengthPointerLocation, lengthPointerLength);

          TaskProgressManager.setValue(offset);
          realNumFiles++;
        }

      }

      fm.close();

      resources = resizeResources(resources, realNumFiles);

      return resources;

    }
    catch (Throwable t) {
      logError(t);
      return null;
    }
  }

}
