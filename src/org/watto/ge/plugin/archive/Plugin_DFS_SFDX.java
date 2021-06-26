
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
public class Plugin_DFS_SFDX extends ArchivePlugin {

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public Plugin_DFS_SFDX() {

    super("DFS_SFDX", "DFS_SFDX");

    //         read write replace rename
    setProperties(true, false, false, false);

    setGames("Area 51");
    setExtensions("dfs");
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
      if (fm.readString(4).equals("SFDX")) {
        rating += 50;
      }

      // Version
      if (fm.readInt() == 3) {
        rating += 5;
      }

      fm.skip(4);

      // Padding Size
      if (fm.readInt() == 2048) {
        rating += 5;
      }

      fm.skip(4);

      // Number Of Files
      if (FieldValidator.checkNumFiles(fm.readInt())) {
        rating += 5;
      }

      fm.skip(4);

      long arcSize = fm.getLength();

      // Archive Size
      if (FieldValidator.checkLength(fm.readInt(), arcSize)) {
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

      // 4 - Header (SFDX)
      // 4 - Version (3)
      // 4 - Unknown
      // 4 - Padding Size (2048)
      // 4 - Unknown
      fm.skip(20);

      // 4 - Number Of Files
      int numFiles = fm.readInt();
      FieldValidator.checkNumFiles(numFiles);

      // 4 - Unknown (1)
      // 4 - Length Of Filename Directory
      // 4 - Unknown (48)
      // 4 - Archive Header Length (56)
      // 4 - null
      // 4 - Length Of Offset Directory + Archive Header
      // 1 - null
      // 4 - Archive Size?
      // 2 - null
      // 1 - null
      fm.skip(32);

      long arcSize = (int) fm.getLength();

      Resource[] resources = new Resource[numFiles];

      TaskProgressManager.setMaximum(numFiles);

      // skip to the filename directory
      /*
       * fm.skip(numFiles*24);
       * 
       * String[] names = new String[numFiles];
       * 
       * for (int i=0;i<numFiles;i++){ names[i] = fm.readNullString(); }
       */

      // skip back to the directory
      fm.seek(56);

      // Loop through directory
      for (int i = 0; i < numFiles; i++) {
        // 4 - Unknown (Filename Length?)
        // 4 - Unknown (Filename Length?)
        // 4 - null
        // 4 - Unknown (39)
        fm.skip(16);

        // 4 - File Offset (Relative to the end of the archive)
        long offset = fm.readInt();
        FieldValidator.checkOffset(offset, arcSize);

        // 4 - File Size
        long length = fm.readInt();
        FieldValidator.checkLength(length, arcSize);

        //String filename = names[i];
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
