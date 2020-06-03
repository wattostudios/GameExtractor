
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
public class Plugin_HPF_HMG extends ArchivePlugin {

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public Plugin_HPF_HMG() {

    super("HPF_HMG", "HPF_HMG");

    //         read write replace rename
    setProperties(true, false, false, false);

    setGames("Transworld Snowboarding");
    setExtensions("hpf");
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
      if (fm.readString(15).equals("HMG_PACKED_FILE")) {
        rating += 50;
      }

      fm.skip(273);

      // Number Of Files
      if (FieldValidator.checkNumFiles(fm.readInt())) {
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

      // 32 - Header ("HMG_PACKED_FILE" + null + spaces to fill)
      // 256 - Absolute Archive Path (null terminated, spaces to fill)
      fm.skip(288);

      // 4 - Number Of Files
      int numFiles = fm.readInt();
      FieldValidator.checkNumFiles(numFiles);

      Resource[] resources = new Resource[numFiles];

      TaskProgressManager.setMaximum(numFiles);

      // Loop through directory
      int relOffset = 292 + (numFiles * 80);
      int realNumFiles = 0;
      for (int i = 0; i < numFiles; i++) {
        // 4 - File/Directory Identifier (0/3)
        int fileDirID = fm.readInt();

        if (fileDirID == 0) {
          // File

          // 4 - null
          fm.skip(4);

          // 4 - File Offset (relative to the start of the file data)
          long offset = fm.readInt() + relOffset;
          FieldValidator.checkOffset(offset, arcSize);

          // 4 - File Length
          long length = fm.readInt();
          FieldValidator.checkLength(length, arcSize);

          // 64 - Filename (null terminated, filled with spaces after the null)
          String filename = fm.readNullString(64);
          FieldValidator.checkFilename(filename);

          //path,id,name,offset,length,decompLength,exporter
          resources[realNumFiles] = new Resource(path, filename, offset, length);
          realNumFiles++;

        }
        else {
          // Directory or Unknown

          // 4 - null
          // 4 - File ID (incremental from 48) -OR- Root Directory Identifier (=(byte)1)
          // if (fileID == (byte)1){
          //   4 - File ID (incremental from 48)
          //   }
          // else {
          //   4 - Unknown (1)
          //   }
          // 64 - Directory Name (null terminated, filled with spaces after the null)
          fm.skip(76);
        }

        TaskProgressManager.setValue(i);
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
