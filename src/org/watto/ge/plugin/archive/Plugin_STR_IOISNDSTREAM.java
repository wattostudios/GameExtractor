
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
public class Plugin_STR_IOISNDSTREAM extends ArchivePlugin {

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public Plugin_STR_IOISNDSTREAM() {

    super("STR_IOISNDSTREAM", "STR_IOISNDSTREAM");

    //         read write replace rename
    setProperties(true, false, false, false);

    setGames("Hitman Blood Money");
    setExtensions("str");
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

      // Header
      if (fm.readString(12).equals("IOISNDSTREAM")) {
        rating += 50;
      }

      fm.skip(4);

      long arcSize = fm.getLength();

      // Directory Offset
      if (FieldValidator.checkOffset(fm.readInt(), arcSize)) {
        rating += 5;
      }

      // Number Of Files
      if (FieldValidator.checkNumFiles(fm.readInt())) {
        rating += 5;
      }

      // Unknown (256)
      if (fm.readInt() == 256) {
        rating += 5;
      }

      // null
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

      //ExporterPlugin exporter = Exporter_ZLib.getInstance();

      // RESETTING GLOBAL VARIABLES

      FileManipulator fm = new FileManipulator(path, false);

      long arcSize = fm.getLength();

      // 12 - Header (IOISNDSTREAM)
      // 4 - Unknown (9)
      fm.skip(16);

      // 4 - Directory Offset
      long dirOffset = fm.readInt();
      FieldValidator.checkOffset(dirOffset, arcSize);

      // 4 - Number Of Files?
      int numFiles = fm.readInt();
      FieldValidator.checkNumFiles(numFiles);

      // 4 - Unknown (256)
      // 228 - null
      fm.seek(dirOffset);

      Resource[] resources = new Resource[numFiles];
      TaskProgressManager.setMaximum(numFiles);

      // Loop through directory
      long[] nameOffsets = new long[numFiles];
      int[] nameLengths = new int[numFiles];
      for (int i = 0; i < numFiles; i++) {
        // 8 - File ID?
        fm.skip(8);

        // 8 - File Offset
        long offset = fm.readLong();
        FieldValidator.checkOffset(offset, arcSize);

        // 8 - File Length (including Padding)
        long length = fm.readLong();
        FieldValidator.checkLength(length, arcSize);

        // 8 - Sub-Directory Offset
        // 4 - Sub-Directory Entry Size (28)
        // 4 - Unknown
        fm.skip(16);

        // 8 - Filename Length (not including null)
        int filenameLength = (int) fm.readLong();
        FieldValidator.checkFilenameLength(filenameLength);
        nameLengths[i] = filenameLength;

        // 8 - Filename Offset
        long filennameOffset = fm.readInt();
        FieldValidator.checkOffset(filennameOffset, arcSize);
        nameOffsets[i] = filennameOffset;

        // 4 - Unknown (0/4)
        // 8 - Unknown (0/16/32/48/64)
        // 4 - null
        fm.skip(16);

        //path,name,offset,length,decompLength,exporter
        resources[i] = new Resource(path, "", offset, length);

        TaskProgressManager.setValue(i);
      }

      // now go and grab the filenames
      for (int i = 0; i < numFiles; i++) {
        fm.seek(nameOffsets[i]);
        // X - Filename
        // 1 - null Filename Terminator
        resources[i].setName(fm.readString(nameLengths[i]));
      }

      fm.close();

      return resources;

    }
    catch (Throwable t) {
      logError(t);
      return null;
    }
  }

}
