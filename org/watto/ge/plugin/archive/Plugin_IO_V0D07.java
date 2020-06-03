
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
public class Plugin_IO_V0D07 extends ArchivePlugin {

  int realNumFiles = 0;

  /**
  **********************************************************************************************

  **********************************************************************************************
  **/
  public Plugin_IO_V0D07() {

    super("IO_V0D07", "IO_V0D07");

    //         read write replace rename
    setProperties(true, false, false, false);

    setGames("Braveheart");
    setExtensions("io");
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
      if (fm.readString(6).equals("v0D.07")) {
        rating += 50;
      }

      // Null
      if (fm.readLong() == 0) {
        rating += 5;
      }

      fm.skip(254);

      // Number Of Files
      if (FieldValidator.checkNumFiles(fm.readInt())) {
        rating += 5;
      }

      // Null
      if (fm.readLong() == 0) {
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
      realNumFiles = 0;

      FileManipulator fm = new FileManipulator(path, false);

      //long arcSize = (int) fm.getLength();

      // 268 - Header ("v0D.07" + nulls to fill)
      fm.skip(268);

      // 4 - Number Of Files
      int numFiles = fm.readInt();
      FieldValidator.checkNumFiles(numFiles);

      // 32 - null
      fm.skip(32);

      Resource[] resources = new Resource[numFiles];

      TaskProgressManager.setMaximum(numFiles);

      // 4 - Entry Type (13=File, !13=Directory)
      if (fm.readInt() == 13) {
        return null;
      }

      readDirectory(fm, resources, path, "");

      resources = resizeResources(resources, realNumFiles);

      // change the relative offsets to absolutes, now that we have found the end of the directory
      long relOffset = fm.getOffset();
      for (int i = 0; i < realNumFiles; i++) {
        resources[i].setOffset(resources[i].getOffset() + relOffset);
      }

      fm.close();

      return resources;

    }
    catch (Throwable t) {
      logError(t);
      return null;
    }
  }

  /**
  **********************************************************************************************

  **********************************************************************************************
  **/
  public void readDirectory(FileManipulator fm, Resource[] resources, File path, String dirName) throws Exception {

    long arcSize = (int) fm.getLength();

    // 4 - Number Of Files In This Directory
    int numFilesInDir = fm.readInt();
    FieldValidator.checkNumFiles(numFilesInDir + 1);

    // 4 - Number Of Sub-Directories In This Directory
    int numFoldersInDir = fm.readInt();
    FieldValidator.checkNumFiles(numFoldersInDir + 1);

    // X - Directory Name
    // 1 - null Directory Name Terminator
    String thisDirName = dirName + fm.readNullString() + "\\";
    if (thisDirName.equals("dad:\\\\")) {
      thisDirName = "";
    }

    // Loop through directory
    for (int i = 0; i < numFilesInDir; i++) {
      // 4 - Entry Type (13=File, !13=Directory)
      //int type = fm.readInt();
      fm.skip(4);

      // File

      // 4 - File Offset (relative to the start of the file data)
      long offset = fm.readInt();
      FieldValidator.checkOffset(offset, arcSize);

      // 4 - File Length
      long length = fm.readInt();
      FieldValidator.checkLength(length, arcSize);

      // 8 - null
      fm.skip(8);

      // X - Filename
      // 1 - null Filename Terminator
      String filename = thisDirName + fm.readNullString();
      //System.out.println(filename);

      //path,id,name,offset,length,decompLength,exporter
      resources[realNumFiles] = new Resource(path, filename, offset, length);

      TaskProgressManager.setValue(realNumFiles);
      realNumFiles++;
    }

    for (int i = 0; i < numFoldersInDir; i++) {
      // Directory
      fm.skip(4);
      readDirectory(fm, resources, path, thisDirName);
    }

  }

}
