
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
public class Plugin_PCK_0KCP extends ArchivePlugin {

  int realNumFiles = 0;
  int firstFileOffset = 0;

  /**
  **********************************************************************************************

  **********************************************************************************************
  **/
  public Plugin_PCK_0KCP() {

    super("PCK_0KCP", "PCK_0KCP");

    //         read write replace rename
    setProperties(true, false, false, false);

    setGames("WWII Tank Commander");
    setExtensions("pck");
    setPlatforms("PC");

  }

  /**
  **********************************************************************************************

  **********************************************************************************************
  **/
  public void analyseDirectory(FileManipulator fm, File path, Resource[] resources, String dirName) throws Exception {
    long arcSize = fm.getLength();

    // 4 - Type Header (_RID, ELIF)
    String type = fm.readString(4);

    if (type.equals("DNED")) {
      // 4 - null
      fm.skip(4);
    }
    else if (type.equals("_RID")) {
      // Directory

      // 4 - Number Of Files / Subdirectories
      int numFiles = fm.readInt();
      FieldValidator.checkNumFiles(numFiles);

      // 2 - Directory Name Length
      short filenameLength = fm.readShort();
      FieldValidator.checkFilenameLength(filenameLength);

      // X - Directory Name
      String filename = dirName + fm.readNullString(filenameLength) + "/";
      FieldValidator.checkFilename(filename);

      for (int i = 0; i < numFiles; i++) {
        analyseDirectory(fm, path, resources, filename);
      }

    }
    else if (type.equals("ELIF")) {
      // File
      // 4 - Length Of File Entry (not including the 4-byte header or this field)
      fm.skip(4);

      // 4 - File Offset (Relative to the start of the File Data (**))
      long offset = fm.readInt() + firstFileOffset;
      FieldValidator.checkOffset(offset, arcSize);

      // 4 - File Length
      long length = fm.readInt();
      FieldValidator.checkLength(length, arcSize);

      // 2 - Filename Length
      short filenameLength = fm.readShort();
      FieldValidator.checkFilenameLength(filenameLength);

      // X - Filename
      String filename = dirName + fm.readString(filenameLength);
      FieldValidator.checkFilename(filename);

      //path,id,name,offset,length,decompLength,exporter
      resources[realNumFiles] = new Resource(path, filename, offset, length);

      TaskProgressManager.setValue(offset);
      realNumFiles++;
    }
    else {
      //System.out.println((fm.getOffset()-4) + " - " + type);
    }

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
      if (fm.readString(4).equals("0KCP")) {
        rating += 50;
      }

      long arcSize = fm.getLength();

      // Number Of Files
      if (FieldValidator.checkNumFiles(fm.readInt())) {
        rating += 5;
      }

      // First File Offset
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
      realNumFiles = 0;
      firstFileOffset = 0;

      FileManipulator fm = new FileManipulator(path, false);

      // 4 - Header (0KCP)
      fm.skip(4);

      // 4 - Version? Number Of Directories? (4)
      int numDirs = fm.readInt();
      FieldValidator.checkNumFiles(numDirs);

      // 4 - Offset To The "// FILE DATA" section
      firstFileOffset = fm.readInt() + 8;

      int numFiles = Archive.getMaxFiles();

      //long arcSize = (int) fm.getLength();

      Resource[] resources = new Resource[numFiles];

      TaskProgressManager.setMaximum(numFiles);

      // IF THERE IS ONLY 1 DIRECTORY TO START, JUST REMOVE THE FOR LOOP FROM AROUND THIS CODE!
      while (fm.getOffset() < firstFileOffset - 8) {
        analyseDirectory(fm, path, resources, "");
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
