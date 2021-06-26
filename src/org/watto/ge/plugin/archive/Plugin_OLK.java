
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
public class Plugin_OLK extends ArchivePlugin {

  int realNumFiles = 0;

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public Plugin_OLK() {

    super("OLK", "OLK");

    //         read write replace rename
    setProperties(true, false, false, false);

    setGames("Soul Calibur 3");
    setExtensions("olk");
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

      // Number Of Files
      if (FieldValidator.checkNumFiles(fm.readInt())) {
        rating += 5;
      }

      // Header
      if (fm.readString(4).equals("olnk")) {
        rating += 50;
      }

      // Padding Size
      if (fm.readInt() == 2048) {
        rating += 5;
      }

      // null
      if (fm.readInt() == 0) {
        rating += 5;
      }

      long arcSize = fm.getLength();

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

      // NOTE - Compressed files MUST know their DECOMPRESSED LENGTH
      //      - Uncompressed files MUST know their LENGTH

      addFileTypes();

      // RESETTING THE GLOBAL VARIABLES
      realNumFiles = 0;

      FileManipulator fm = new FileManipulator(path, false);

      long arcSize = (int) fm.getLength();

      int numFiles = Archive.getMaxFiles();

      Resource[] resources = new Resource[numFiles];

      TaskProgressManager.setMaximum(arcSize);

      readDirectory(path, fm, resources);

      resources = resizeResources(resources, realNumFiles);

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
   * Reads an [archive] File into the Resources
   **********************************************************************************************
   **/
  public boolean readDirectory(File path, FileManipulator fm, Resource[] resources) throws Exception {

    long relOffset = fm.getOffset();
    long arcSize = fm.getLength() + 1;

    // 4 - Number Of Folders and Files?
    int numFiles = fm.readInt();

    // 4 - Header (olnk)
    if (!(fm.readString(4).equals("olnk"))) {
      return false;
    }

    // 4 - Padding Size (2048)
    // 4 - null
    fm.skip(8);

    // 4 - First File Offset (relative to the start of this folder entry)
    long firstFileOffset = fm.readInt() + relOffset;
    FieldValidator.checkOffset(firstFileOffset, arcSize);

    // 4 - Unknown (null if this directory has files in it?)
    // 4 - Hash?
    // 4 - null
    fm.skip(12);

    for (int i = 0; i < numFiles; i++) {
      // 4 - File Offset (relative to the first file offset)
      long offset = fm.readInt() + firstFileOffset;
      FieldValidator.checkOffset(offset, arcSize);

      // 4 - File Length (can be null)
      long length = fm.readInt();
      FieldValidator.checkLength(length, arcSize);

      // 4 - Hash?
      // 4 - null
      fm.skip(8);

      if (length != 0) {

        long currentPos = fm.getOffset();
        fm.seek(offset);

        boolean isDirectory = readDirectory(path, fm, resources);

        fm.seek(currentPos);

        if (!isDirectory) {
          // it is a file
          String filename = Resource.generateFilename(realNumFiles);

          //path,id,name,offset,length,decompLength,exporter
          resources[realNumFiles] = new Resource(path, filename, offset, length);

          TaskProgressManager.setValue(offset);
          realNumFiles++;
        }
      }
    }

    return true;
  }

}
