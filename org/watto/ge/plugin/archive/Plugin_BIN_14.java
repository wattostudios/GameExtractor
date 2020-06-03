
package org.watto.ge.plugin.archive;

import java.io.File;
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
import org.watto.task.TaskProgressManager;

/**
**********************************************************************************************

**********************************************************************************************
**/
public class Plugin_BIN_14 extends ArchivePlugin {

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public Plugin_BIN_14() {

    super("BIN_14", "BIN_14");

    //         read write replace rename
    setProperties(true, false, false, false);
    setEnabled(false);

    allowImplicitReplacing = true;

    setGames("Need For Speed: Most Wanted", "Need For Speed: Pro Street");
    setExtensions("bin");
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

      // 2 - null
      if (fm.readShort() == 0) {
        rating += 5;
      }

      fm.skip(2);

      long arcSize = (int) fm.getLength();

      // Archive Size
      if (fm.readInt() + 8 == arcSize) {
        rating += 5;
      }

      // null
      if (fm.readInt() == 0) {
        rating += 5;
      }

      // number of nulls (48)
      if (fm.readInt() == 48) {
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
  @SuppressWarnings("unused")
  public Resource[] read(File path) {
    try {

      // NOTE - Compressed files MUST know their DECOMPRESSED LENGTH
      //      - Uncompressed files MUST know their LENGTH

      addFileTypes();

      // RESETTING THE GLOBAL VARIABLES

      FileManipulator fm = new FileManipulator(path, false);

      long arcSize = (int) fm.getLength();

      // 2 - null
      // 2 - Unknown
      // 4 - Archive Length [+8]
      // 4 - null
      // 4 - Number Of Nulls (48)
      // 48 - null
      // 2 - null
      // 2 - Unknown
      // 4 - Directory Length?
      // 2 - Number Of Directories (1)
      // 2 - Unknown
      fm.skip(76);

      // 4 - Directory Offset [+80]
      long dirOffset = fm.readInt() + 80;

      // 4 - Directory Name Length (not including nulls)
      fm.skip(4);

      // 28 - Directory Name (null terminated)
      String dirName = fm.readNullString(28) + "\\";
      //System.out.println(dirName);

      // 64 - Filename
      // 4 - Unknown
      // 24 - null

      // 2 - Unknown (2)
      // 2 - Unknown
      fm.skip(96);

      // 4 - Directory Length (not including these 3 fields)
      int numFiles = fm.readInt() / 8;
      FieldValidator.checkNumFiles(numFiles);

      fm.skip(numFiles * 8 + 8);

      Resource[] resources = new Resource[numFiles];

      TaskProgressManager.setMaximum(numFiles);

      // Loop through directory
      for (int i = 0; i < numFiles; i++) {
        // 4 - Hash?
        fm.skip(4);

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

        // 4 - Unknown (263324)
        // 4 - Padding Multiple? (256)
        // 4 - null
        fm.skip(12);

        String filename = dirName + Resource.generateFilename(i);

        //path,id,name,offset,length,decompLength,exporter
        resources[i] = new ReplacableResource(path, filename, offset, offsetPointerLocation, offsetPointerLength, length, lengthPointerLocation, lengthPointerLength);

        TaskProgressManager.setValue(i);
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
