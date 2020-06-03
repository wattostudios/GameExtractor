
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
public class Plugin_SB0 extends ArchivePlugin {

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public Plugin_SB0() {

    super("SB0", "SB0");

    //         read write replace rename
    setProperties(true, false, false, false);

    setGames("Rainbow Six 3: Raven Shield");
    setExtensions("sb0"); // MUST BE LOWER CASE
    setPlatforms("PC");

    //setFileTypes("","",
    //             "",""
    //             );

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

      // 4 - Number Of Files in Directory 1
      if (FieldValidator.checkNumFiles(fm.readInt() + 1)) {
        rating += 5;
      }

      fm.skip(4);

      // 4 - Number Of Files in Directory 2
      if (FieldValidator.checkNumFiles(fm.readInt() + 1)) {
        rating += 5;
      }

      fm.skip(4);

      // 4 - Number Of Files in Directory 3
      if (FieldValidator.checkNumFiles(fm.readInt() + 1)) {
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

      //ExporterPlugin exporter = Exporter_ZLib.getInstance();

      // RESETTING GLOBAL VARIABLES

      FileManipulator fm = new FileManipulator(path, false);

      long arcSize = fm.getLength();

      // 4 - Unknown (11)
      fm.skip(4);

      // 4 - Number Of Files in Directory 1
      int numFiles1 = fm.readInt();
      FieldValidator.checkNumFiles(numFiles1 + 1);

      // 4 - Unknown (-1)
      fm.skip(4);

      // 4 - Number Of Files in Directory 2
      int numFiles = fm.readInt();
      FieldValidator.checkNumFiles(numFiles);

      // 4 - Unknown (-1)
      fm.skip(4);

      // 4 - Number Of Files in Directory 3
      int numFiles3 = fm.readInt();
      FieldValidator.checkNumFiles(numFiles3 + 1);

      // 4 - Unknown (-1)
      // 4 - Unknown
      fm.skip(8);

      fm.skip(numFiles1 * 92);

      long relOffset = 32 + numFiles1 * 92 + numFiles * 125 + numFiles3 * 100 + 4;

      Resource[] resources = new Resource[numFiles];
      TaskProgressManager.setMaximum(numFiles);

      // Loop through directory
      int realNumFiles = 0;
      for (int i = 0; i < numFiles; i++) {
        // 2 - File ID
        // 2 - Unknown
        fm.skip(4);

        // 4 - Entry Type (1=File, 8=Hash?)
        int entryType = fm.readInt();

        if (entryType == 1) {
          // File
          // 4 - Decompressed File Length
          long length = fm.readInt();
          FieldValidator.checkLength(length, arcSize);

          // 4 - Unknown
          fm.skip(4);

          // 4 - File Offset (relative to the start of the file data)
          long offset = fm.readInt() + relOffset;
          FieldValidator.checkOffset(offset, arcSize);

          // 4 - Unknown
          // 4 - Entry Type (1)
          // 4 - Entry Type (1)
          // 4 - Entry Type (1)
          // 4 - null
          // 4 - Flag (0/1)
          // if (flag == 0){
          //   4 - Unknown Length
          //   4 - Decompressed Length
          //   8 - null
          //   }
          // else if (flag == 1){
          //   8 - null
          //   4 - Unknown Length
          //   4 - Decompressed Length
          //   }
          // 4 - Unknown (64000)
          // 4 - Unknown (32000)
          // 2 - Unknown (16)
          // 2 - Unknown (1)
          // 4 - Unknown (1)
          fm.skip(56);

          // 40 - Filename (null terminated)
          String filename = fm.readNullString(40);
          FieldValidator.checkFilename(filename);

          // 4 - Unknown (1)
          // 4 - Padding (-1)
          fm.skip(8);

          //path,name,offset,length,decompLength,exporter
          resources[realNumFiles] = new Resource(path, filename, offset, length);
          realNumFiles++;

          TaskProgressManager.setValue(i);
        }
        else {
          fm.skip(116);
        }

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
