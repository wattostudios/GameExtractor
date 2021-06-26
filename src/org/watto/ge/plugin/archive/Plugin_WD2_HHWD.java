
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
public class Plugin_WD2_HHWD extends ArchivePlugin {

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public Plugin_WD2_HHWD() {

    super("WD2_HHWD", "WD2_HHWD");

    //         read write replace rename
    setProperties(true, false, false, false);

    setGames("American Idol");
    setExtensions("wd2");
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
      if (fm.readString(4).equals("HHWD")) {
        rating += 50;
      }

      fm.skip(12);

      long arcSize = fm.getLength();

      // Header
      if (fm.readString(4).equals("DATA")) {
        rating += 5;
      }

      // Directory Offset
      if (FieldValidator.checkOffset(fm.readInt(), arcSize)) {
        rating += 5;
      }

      // Header
      if (fm.readString(4).equals("INFO")) {
        rating += 5;
      }

      fm.skip(4);

      // Header
      if (fm.readString(4).equals("DIR ")) {
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

      // 4 - Header (HHWD)
      // 4 - Unknown
      // 4 - Unknown
      // 4 - Unknown (3)
      // 4 - Data Header (DATA)
      fm.skip(20);

      // 4 - Files Directory Offset (40)
      long dirOffset = fm.readInt();
      FieldValidator.checkOffset(dirOffset, arcSize);

      // 4 - Information Header (INFO)
      fm.skip(4);

      // 4 - Information Directory Offset
      int infoDirOffset = fm.readInt();
      FieldValidator.checkOffset(infoDirOffset, arcSize);

      // 4 - Directory Header (DIR )
      fm.skip(4);

      // 4 - Folders Directory Offset
      int folderDirOffset = fm.readInt();
      FieldValidator.checkOffset(folderDirOffset, arcSize);

      // 4 - Number Of Files
      int numFiles = fm.readInt();
      FieldValidator.checkNumFiles(numFiles);

      Resource[] resources = new Resource[numFiles];

      TaskProgressManager.setMaximum(numFiles);

      long relOffset = fm.getOffset() + (numFiles * 8);

      // Loop through directory
      for (int i = 0; i < numFiles; i++) {
        // 4 - File Length
        long length = fm.readInt();
        FieldValidator.checkLength(length, arcSize);

        // 4 - File Offset
        long offset = fm.readInt() + relOffset;
        FieldValidator.checkOffset(offset, arcSize);

        //path,id,name,offset,length,decompLength,exporter
        resources[i] = new Resource(path, "", offset, length);

        TaskProgressManager.setValue(i);
      }

      fm.seek(folderDirOffset);
      readFolder(fm, resources, "", folderDirOffset);

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
  public void readFolder(FileManipulator fm, Resource[] resources, String dirName, int folderDirOffset) throws Exception {
    // 4 - Header (DIRA)
    fm.skip(4);

    // 4 - Number Of Files
    int numFiles = fm.readInt();

    // 4 - Number Of Files
    // 4 - null
    fm.skip(8);

    // for each file in this folder
    long firstFilenameOffset = fm.getOffset() + (numFiles * 12);
    for (int i = 0; i < numFiles; i++) {
      // 4 - File/Directory ID (1=Directory,2=File)
      int typeID = fm.readInt();

      // 4 - Sub-directory ID (ie offset to the contents for this sub-directory is 2048*thisValue)
      int id = fm.readInt();

      // 4 - Filename Offset (relative to the first filename)
      long filenameOffset = fm.readInt() + firstFilenameOffset;
      long curPos = fm.getOffset();

      fm.relativeSeek(filenameOffset);

      // X - Filename
      String filename = fm.readNullString();

      fm.relativeSeek(curPos);

      if (typeID == 2) {
        resources[id].setName(filename);
      }
      else if (typeID == 1) {
        // read the sub-directory
        curPos = fm.getOffset();

        long dirOffset = id * 2048 + folderDirOffset;
        FieldValidator.checkOffset(dirOffset, fm.getLength());
        fm.seek(dirOffset);
        readFolder(fm, resources, dirName + filename + "\\", folderDirOffset);

        fm.seek(curPos);
      }

    }

  }

}
