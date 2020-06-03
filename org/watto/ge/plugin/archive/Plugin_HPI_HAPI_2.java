
package org.watto.ge.plugin.archive;

import java.io.File;
import org.watto.task.TaskProgressManager;
import org.watto.datatype.Archive;
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
public class Plugin_HPI_HAPI_2 extends ArchivePlugin {

  int realNumFiles = 0;
  int filenameDirOffset = 0;

  /**
  **********************************************************************************************

  **********************************************************************************************
  **/
  public Plugin_HPI_HAPI_2() {

    super("HPI_HAPI_2", "HPI_HAPI_2");

    //         read write replace rename
    setProperties(true, false, false, false);

    setExtensions("hpi");
    setGames("Total Annihilation: Kingdoms");
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
      if (fm.readString(4).equals("HAPI")) {
        rating += 50;
      }

      long arcSize = fm.getLength();

      // 4 - Directory Offset
      if (FieldValidator.checkOffset(fm.readInt(), arcSize)) {
        rating += 5;
      }

      // 4 - Directory Length
      if (FieldValidator.checkLength(fm.readInt(), arcSize)) {
        rating += 5;
      }

      // 4 - Filename Directory Offset
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

  **********************************************************************************************
  **/
  @Override
  @SuppressWarnings("unused")
  public Resource[] read(File path) {
    try {

      addFileTypes();

      // RESETTING THE GLOBAL VARIABLES
      realNumFiles = 0;
      filenameDirOffset = 0;

      FileManipulator fm = new FileManipulator(path, false);

      long arcSize = fm.getLength();

      // 4 - Header (HAPI)
      // 4 - Unknown
      fm.skip(8);

      // 4 - Directory Offset
      long dirOffset = fm.readInt();
      FieldValidator.checkOffset(dirOffset, arcSize);

      // 4 - Directory Length
      int dirLength = fm.readInt();
      FieldValidator.checkLength(dirLength, arcSize);

      // 4 - Filename Directory Offset
      filenameDirOffset = fm.readInt();
      FieldValidator.checkOffset(filenameDirOffset, arcSize);

      // 4 - Unknown (32)
      fm.skip(4);

      // 4 - Archive Length? [+78]
      int archiveLength = fm.readInt() + 78;
      //FieldValidator.checkEquals(archiveLength,arcSize);

      fm.seek(dirOffset);

      // 4 - null
      // 4 - First Directory Offset
      fm.skip(8);

      // 4 - number of directories
      int numDirectories = fm.readInt();
      FieldValidator.checkNumFiles(numDirectories);

      int numFiles = Archive.getMaxFiles(4);

      Resource[] resources = new Resource[numFiles];
      TaskProgressManager.setMaximum(arcSize);

      for (int i = 0; i < numDirectories; i++) {
        // 4 - directory Offset
        long subDirOffset = fm.readInt() + dirOffset;
        FieldValidator.checkOffset(subDirOffset, arcSize);

        // 4 - null
        fm.skip(4);

        int currentPos = (int) fm.getOffset();
        readDirectory(fm, path, resources, subDirOffset);
        fm.seek(currentPos);
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

  public void readDirectory(FileManipulator fm, File path, Resource[] resources, long dirOffset) throws Exception {

    long arcSize = fm.getLength();

    // 4 - directory filename offset
    int dirNameOffset = fm.readInt() + filenameDirOffset;
    FieldValidator.checkOffset(dirNameOffset, arcSize);

    int currentPos = (int) fm.getOffset();
    fm.seek(dirNameOffset);

    // X - Filename (null)
    String dirFilename = fm.readNullString();
    FieldValidator.checkFilename(dirFilename);

    fm.seek(currentPos);

    // 4 - Directory Length
    // 4 - null
    // 4 - Unknown (40)
    fm.skip(12);

    // 4 - numFiles
    int numFiles = fm.readInt();
    FieldValidator.checkNumFiles(numFiles);

    for (int i = 0; i < numFiles; i++) {
      // 4 - Filename Offset
      int filenameOffset = fm.readInt() + filenameDirOffset;
      FieldValidator.checkOffset(filenameOffset, arcSize);

      currentPos = (int) fm.getOffset();
      fm.seek(filenameOffset);
      // X - Filename (null)
      String filename = dirFilename + "\\" + fm.readNullString();
      fm.seek(currentPos);

      // 4 - Data Offset
      long offset = fm.readInt();
      FieldValidator.checkOffset(offset, arcSize);

      // 4 - File Length
      long length = fm.readInt();
      FieldValidator.checkLength(length, arcSize);

      // 4 - Unknown
      // 4 - Unknown
      // 4 - Unknown
      fm.skip(12);

      //path,id,name,offset,length,decompLength,exporter
      resources[realNumFiles] = new Resource(path, filename, offset, length);

      TaskProgressManager.setValue(offset);
      realNumFiles++;
    }

  }

}