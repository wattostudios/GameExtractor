
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
public class Plugin_COD_KAPF extends ArchivePlugin {

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public Plugin_COD_KAPF() {

    super("COD_KAPF", "COD_KAPF");

    //         read write replace rename
    setProperties(true, false, false, false);

    setEnabled(false);

    setGames("Call Of Duty 2: Big Red One");
    setExtensions("cod");
    setPlatforms("XBox");

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
      if (fm.readString(4).equals("KAPF")) {
        rating += 50;
      }

      fm.skip(4);

      // Block size
      if (fm.readInt() == 28) {
        rating += 5;
      }

      // Version
      if (fm.readInt() == 2) {
        rating += 5;
      }

      long arcSize = fm.getLength();

      // Directory offset
      if (FieldValidator.checkOffset(fm.readInt(), arcSize)) {
        rating += 5;
      }

      // filename Directory end offset
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

      FileManipulator fm = new FileManipulator(path, false);

      long arcSize = (int) fm.getLength();

      // 4 - Header (KAPF)
      // 4 - Unknown
      // 4 - Block Size (28)
      // 4 - Unknown (2)
      fm.skip(16);

      // 4 - Directory Offset (328)
      long dirOffset = fm.readInt();
      FieldValidator.checkOffset(dirOffset, arcSize);

      // 4 - Unknown (4672)
      // 4 - Offset To The End Of The Filename Directory
      // 12 - null
      // 2 - Number Of Blocks (8)
      fm.skip(22);

      // 2 - Number Of Files
      short numFilesS = fm.readShort();
      FieldValidator.checkNumFiles(numFilesS);

      int numFiles = numFilesS;

      fm.seek(dirOffset + (numFiles * 20));

      Resource[] resources = new Resource[numFiles];

      TaskProgressManager.setMaximum(numFiles);

      // Loop through directory
      String[] names = new String[numFiles];
      for (int i = 0; i < numFiles; i++) {
        // X - Filename
        // 1 - null Filename Terminator
        String filename = fm.readNullString();
        FieldValidator.checkFilename(filename);

        // 0-3 - null Padding to a multiple of 4 bytes
        int paddingSize = 4 - ((filename.length() + 1) % 4);
        if (paddingSize < 4) {
          fm.skip(paddingSize);
        }

        names[i] = filename;
      }

      fm.seek(dirOffset);

      // Loop through directory
      long offset = 32768;
      for (int i = 0; i < numFiles; i++) {
        // 4 - Unknown
        // 4 - Unknown
        // 4 - Unknown
        // 4 - File Type ID? (1,2,3,4)
        fm.skip(16);

        // 4 - File Length
        long length = fm.readInt();
        FieldValidator.checkLength(length);

        String filename = names[i];

        //path,id,name,offset,length,decompLength,exporter
        resources[i] = new Resource(path, filename, offset, length);

        TaskProgressManager.setValue(i);

        offset += length;
        long paddingSize = 2048 - (length % 2048);
        if (paddingSize < 2048) {
          offset += paddingSize;
        }
      }

      //calculateFileSizes(resources,arcSize);

      fm.close();

      return resources;

    }
    catch (Throwable t) {
      logError(t);
      return null;
    }
  }

}
