
package org.watto.ge.plugin.archive;

import java.io.File;
import org.watto.component.WSPluginException;
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
public class Plugin_IMG_DAVILEX_2 extends ArchivePlugin {

  int realNumFiles = 0;
  long arcSize = 0;

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public Plugin_IMG_DAVILEX_2() {

    super("IMG_DAVILEX_2", "IMG_DAVILEX_2");

    //         read write replace rename
    setProperties(true, false, false, false);

    setGames("London Racer: Police Madness",
        "Knight Rider 2");
    setExtensions("img");
    setPlatforms("PC", "PS2");

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

      getDirectoryFile(fm.getFile(), "idx");
      rating += 25;

      fm.skip(4);

      // Header
      if (fm.readString(16).equals("Davilex Games BV")) {
        rating += 25;
      }

      // null
      if (fm.readLong() == 0) {
        rating += 5;
      }

      // null
      if (fm.readInt() == 0) {
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

      arcSize = (int) path.length();

      File sourcePath = getDirectoryFile(path, "idx");

      FileManipulator fm = new FileManipulator(sourcePath, false);

      // 4 - Unknown
      // 16 - Header (Davilex Games BV)
      // 12 - null
      fm.skip(32);

      int numFiles = Archive.getMaxFiles();

      Resource[] resources = new Resource[numFiles];

      TaskProgressManager.setMaximum(arcSize);

      while (fm.getOffset() < fm.getLength()) {
        readDirectory(path, fm, resources, "", 1);
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

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public void readDirectory(File path, FileManipulator fm, Resource[] resources, String dirName, int numFiles) throws Exception {

    // Loop through directory
    for (int i = 0; i < numFiles; i++) {
      // 2 - Entry Type
      int entryType = fm.readShort();

      // 2 - Length of Filename (including nulls and padding)
      short filenameLength = (fm.readShort());
      FieldValidator.checkFilenameLength(filenameLength);

      // 8 - null
      fm.skip(8);

      // X - Filename
      // 1 - null Filename Terminator
      // 0-3 - null Padding to a multiple of 4 bytes
      String filename = fm.readNullString(filenameLength);
      //System.out.println(filename);

      if (entryType == 0) {
        // directory

        // 4 - Number of Sub-Directories and Files in this Directory
        int numSubFiles = fm.readInt();
        FieldValidator.checkNumFiles(numSubFiles);

        readDirectory(path, fm, resources, dirName + filename + "\\", numSubFiles);

      }
      else if (entryType == 1) {
        // file

        // 4 - File Length
        long length = fm.readInt();
        FieldValidator.checkLength(length, arcSize);

        // 4 - File Length
        // 4 - Unknown (1)
        fm.skip(8);

        // 4 - File Offset
        long offset = fm.readInt();
        FieldValidator.checkOffset(offset, arcSize);

        //path,id,name,offset,length,decompLength,exporter
        resources[realNumFiles] = new Resource(path, dirName + filename, offset, length);

        TaskProgressManager.setValue(offset);
        realNumFiles++;

      }
      else {
        throw new WSPluginException("Invalid entry type");
      }

    }

  }

}
