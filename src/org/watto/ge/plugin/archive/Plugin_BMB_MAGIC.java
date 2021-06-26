
package org.watto.ge.plugin.archive;

import java.io.File;
import org.watto.task.TaskProgressManager;
import org.watto.datatype.ReplacableResource;
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
public class Plugin_BMB_MAGIC extends ArchivePlugin {

  /**
  **********************************************************************************************

  **********************************************************************************************
  **/
  public Plugin_BMB_MAGIC() {

    super("BMB_MAGIC", "BMB_MAGIC");

    //         read write replace rename
    setProperties(true, false, false, false);

    allowImplicitReplacing = true;

    setGames("Armobiles");
    setExtensions("bmb", "dmd", "wmw");
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
      if (fm.readString(8).equals("magic  " + (byte) 0)) {
        rating += 50;
      }

      fm.skip(16);

      // Number Of Files
      if (FieldValidator.checkNumFiles(fm.readInt())) {
        rating += 5;
      }

      // null
      if (fm.readInt() == 0) {
        rating += 5;
      }

      // Number Of Files
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

      // 8 - Header ("magic  " + null)
      // 4 - Unknown
      // 4 - Hash?
      // 4 - null
      // 4 - Unknown (4)
      fm.skip(24);

      // 4 - Number Of Files (including blank files)
      int numFilesIncBlank = fm.readInt();
      FieldValidator.checkNumFiles(numFilesIncBlank);

      // 4 - null
      fm.skip(4);

      // 4 - Number Of Files
      int numFiles = fm.readInt();
      FieldValidator.checkNumFiles(numFiles);

      // 12 - null
      // 4 - Unknown (1)
      // 4 - File ID Starting Point [+1] (2199)
      // 4 - File ID Starting Point [+1] (2199)
      // 4 - Unknown (201)
      // 4 - File ID Starting Point [+1] (2199)
      // 4 - Unknown (219)
      // 4 - Unknown (-1)
      // 4 - null
      // 4 - Unknown (24)
      fm.skip(48);

      // 4 - Files Directory Offset (156)
      long dirOffset = fm.readInt();
      FieldValidator.checkOffset(dirOffset, arcSize);

      // 4 - null
      // 4 - File ID Starting Point [-1] (2201)
      // 4 - null
      // 4 - Unknown (16)
      // 4 - Files Directory Offset (156)
      // 4 - Archive Length [+156 for the Archive Header]
      // 8 - null
      // 4 - Unknown (15)
      // 4 - Archive Length
      // 4 - null
      // 4 - File ID Starting Point [-1] (2201)
      // 4 - null
      // 4 - Unknown (32)
      // 4 - Archive Length
      // 4 - null
      // 4 - File ID Starting Point [-1] (2201)
      fm.seek(dirOffset);

      Resource[] resources = new Resource[numFiles];

      TaskProgressManager.setMaximum(numFiles);

      // Loop through directory
      int realNumFiles = 0;
      for (int i = 0; i < numFilesIncBlank; i++) {
        // 4 - File Offset
        long offsetPointerLocation = fm.getOffset();
        long offsetPointerLength = 4;

        long offset = fm.readInt();
        FieldValidator.checkOffset(offset, arcSize);

        // 4 - File Length
        long lengthPointerLocation = fm.getOffset();
        long lengthPointerLength = 4;

        long length = fm.readInt();
        FieldValidator.checkLength(length, arcSize);

        // 4 - Unknown
        // 4 - Unknown
        // 4 - File ID (incremental from 2200)
        // 4 - File ID (incremental from 2200)
        // 4 - File ID (incremental from 2200)
        // 4 - File/Blank ID (0=blank file, 1=file)
        // 4 - File/Blank ID (0=blank file, 1=file)
        // 4 - null
        // 4 - File/Blank ID (0=blank file, 1=file)
        // 4 - null
        fm.skip(40);

        if (length == 0) {
          // blank file
        }
        else {

          String filename = Resource.generateFilename(realNumFiles);

          //path,id,name,offset,length,decompLength,exporter
          resources[realNumFiles] = new ReplacableResource(path, filename, offset, offsetPointerLocation, offsetPointerLength, length, lengthPointerLocation, lengthPointerLength);

          TaskProgressManager.setValue(i);
          realNumFiles++;
        }
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
