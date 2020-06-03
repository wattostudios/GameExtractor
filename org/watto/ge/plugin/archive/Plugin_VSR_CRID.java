
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
public class Plugin_VSR_CRID extends ArchivePlugin {

  public int i = 0;
  int readLength = 0;

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public Plugin_VSR_CRID() {

    super("VSR_CRID", "VSR_CRID");
    //         read write replace rename
    setProperties(true, false, false, false);

    setExtensions("vsr");
    setGames("Lemmings Paintball");
    setPlatforms("PC");

  }

  public void analyseDirectory(FileManipulator fm, File path, Resource[] resources, String dirName, long offset) throws Exception {

    long arcSize = fm.getLength();

    fm.seek(offset);

    // 4 - Header (CRID)
    // 4 - Directory Length (same as in the dirEntry above)
    fm.skip(8);

    // 4 - numFiles in directory
    int numFiles = fm.readInt();
    FieldValidator.checkNumFiles(numFiles);

    // 4 - Type? (3)
    // 4 - Data Offset to First File in the directory [+8]
    fm.skip(8);

    String[] names = new String[numFiles];
    for (int j = 0; j < numFiles; j++) {
      // X - Filename (null)
      names[j] = fm.readNullString();
      FieldValidator.checkFilename(names[j]);

      // 0-3 - padding to make filename+null a multiple of 4
      int paddingLength = 4 - ((names[j].length() + 1) % 4);
      if (paddingLength != 4) {
        fm.skip(paddingLength);
      }

    }

    // 4 - Unknown
    // 4 - Unknown
    fm.skip(8);

    for (int j = 0; j < numFiles; j++) {
      // 4 - Type/Extension (reversed)
      fm.skip(4);

      // 4 - Data Offset [+8]
      offset = fm.readInt() + 8;
      FieldValidator.checkOffset(offset, arcSize);

      // 4 - File Length (-8?)
      long length = fm.readInt();
      FieldValidator.checkLength(length, arcSize);

      // 16 - null
      // 4 - Unknown
      // 4 - File ID
      fm.skip(24);

      String filename = dirName + "\\" + names[j];

      //path,id,name,offset,length,decompLength,exporter
      resources[i] = new Resource(path, filename, offset, length);

      TaskProgressManager.setValue(readLength);
      readLength += length;
      i++;

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
      if (fm.readString(4).equals("CRID")) {
        rating += 50;
      }

      // Archive Size
      if (fm.readInt() == fm.getLength() - 8) {
        rating += 5;
      }

      // Number Of Directories
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
  
  **********************************************************************************************
  **/
  @Override
  public Resource[] read(File path) {
    try {

      addFileTypes();

      // RESETTING THE GLOBAL VARIABLES
      i = 0;
      readLength = 0;

      FileManipulator fm = new FileManipulator(path, false);
      int numFiles = Archive.getMaxFiles(4);// guess

      long arcSize = fm.getLength();

      Resource[] resources = new Resource[numFiles];
      TaskProgressManager.setMaximum(arcSize);

      // 4 - Header (CRID)
      // 4 - Archive Length [+8]
      fm.skip(8);

      // 4 - Number Of Directories
      int numDirectories = fm.readInt();
      FieldValidator.checkNumFiles(numDirectories);

      // 4 - Type? (3)
      fm.skip(4);

      // 4 - File Length (not including first 2 fields) (the Length of THIS chunk, not the archive Length!)
      fm.skip(4);

      String[] names = new String[numDirectories];
      for (int j = 0; j < numDirectories; j++) {
        // X - Filename (null)
        names[j] = fm.readNullString();
        FieldValidator.checkFilename(names[j]);

        // 0-3 - padding to make filename+null a multiple of 4
        int paddingLength = 4 - ((names[j].length() + 1) % 4);
        if (paddingLength != 4) {
          fm.skip(paddingLength);
        }
      }

      // 4 - Unknown
      // 4 - Unknown
      fm.skip(8);

      for (int j = 0; j < numDirectories; j++) {
        // 4 - Header (CRID)
        fm.skip(4);

        // 4 - Directory Offset
        long dirOffset = fm.readInt();
        FieldValidator.checkOffset(dirOffset, arcSize);

        // 4 - Directory Length
        // 16 - null
        // 4 - Unknown
        // 4 - File ID
        fm.skip(28);

        int currentPos = (int) fm.getOffset();
        analyseDirectory(fm, path, resources, names[j], dirOffset);
        fm.seek(currentPos);
      }

      resources = resizeResources(resources, i);

      fm.close();

      return resources;

    }
    catch (Throwable t) {
      logError(t);
      return null;

    }
  }

}