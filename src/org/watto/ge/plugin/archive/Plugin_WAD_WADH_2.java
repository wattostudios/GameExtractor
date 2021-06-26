
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
public class Plugin_WAD_WADH_2 extends ArchivePlugin {

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public Plugin_WAD_WADH_2() {

    super("WAD_WADH_2", "WAD_WADH_2");

    //         read write replace rename
    setProperties(true, false, false, false);

    setGames("Earache Extreme Metal Racing");
    setExtensions("wad");
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
      if (fm.readString(4).equals("WADH")) {
        rating += 50;
      }

      long arcSize = fm.getLength();

      // First File Offset
      if (FieldValidator.checkOffset(fm.readInt(), arcSize)) {
        rating += 5;
      }

      // Number Of Files
      if (FieldValidator.checkNumFiles(fm.readInt())) {
        rating += 5;
      }

      // Filename Directory Length?
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

      // NOTE - Compressed files MUST know their DECOMPRESSED LENGTH
      //      - Uncompressed files MUST know their LENGTH

      addFileTypes();

      // RESETTING THE GLOBAL VARIABLES

      FileManipulator fm = new FileManipulator(path, false);

      long arcSize = (int) fm.getLength();

      // 4 - Header (WADH)
      fm.skip(4);

      // 4 - First File Offset
      int firstFileOffset = fm.readInt();
      FieldValidator.checkOffset(firstFileOffset, arcSize);

      // 4 - Number Of Files
      int numFiles = fm.readInt();
      FieldValidator.checkNumFiles(numFiles);

      // 4 - Filename Directory Length?
      fm.skip(4);

      Resource[] resources = new Resource[numFiles];

      TaskProgressManager.setMaximum(numFiles);

      // Loop through directory
      int realNumFiles = 0;
      int filenameRelOffset = (numFiles * 24) + 16;
      int[] filenameOffsets = new int[numFiles];
      int[] parentIDs = new int[numFiles];
      for (int i = 0; i < numFiles; i++) {
        // 4 - Filename Offset (relative to the start of the filename directory) (-1 for root)
        int filenameOffset = fm.readInt() + filenameRelOffset;
        FieldValidator.checkOffset(filenameOffset, arcSize);
        filenameOffsets[realNumFiles] = filenameOffset;

        // 4 - Hash? (-1 for root)
        fm.skip(4);

        // 4 - File Offset (relative to the start of the file data)
        long offset = fm.readInt();
        if (offset != 0) {
          offset += firstFileOffset;
        }
        FieldValidator.checkOffset(offset, arcSize);

        // 4 - File Length
        long length = fm.readInt();
        FieldValidator.checkLength(length, arcSize);

        // 4 - Compression Flag? (0/1) (some directories also have 1)
        fm.skip(4);

        // 4 - Number Of Files in Directory (-1 if not a directory)
        int numFilesInDir = fm.readInt();

        // 4 - Parent Directory Number (-1 if no parent)
        int parentID = fm.readInt();
        if (parentID != -1) {
          FieldValidator.checkLength(parentID, numFiles); // checks that it is in the numFiles
        }

        parentIDs[i] = parentID;

        if (numFilesInDir == -1) {
          // File

          //path,id,name,offset,length,decompLength,exporter
          resources[realNumFiles] = new Resource(path, "", offset, length);
          realNumFiles++;
        }
        else {
          // Directory - just check that the numFiles is valid
          FieldValidator.checkLength(numFilesInDir, numFiles); // checks that it is in the numFiles
        }

        TaskProgressManager.setValue(i);
      }

      // Loop through Filenames Directory
      for (int i = 0; i < realNumFiles; i++) {
        // X - Filename (null)
        fm.seek(filenameOffsets[i]);
        String filename = fm.readNullString();

        // add the directory name to the start of the filename
        int parentID = parentIDs[i];
        if (parentID != -1) {
          filename = resources[parentID].getName() + "\\" + filename;
        }

        resources[i].setName(filename);
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
