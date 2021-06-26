
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
import org.watto.io.converter.StringConverter;

/**
**********************************************************************************************

**********************************************************************************************
**/
public class Plugin_PAK_MASSIVE extends ArchivePlugin {

  /**
  **********************************************************************************************

  **********************************************************************************************
  **/
  public Plugin_PAK_MASSIVE() {

    super("PAK_MASSIVE", "PAK_MASSIVE");

    //         read write replace rename
    setProperties(true, false, false, false);

    setExtensions("pak");
    setGames("Spellforce");
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

      // Version
      if (fm.readInt() == 4) {
        rating += 5;
      }

      // Header
      if (fm.readString(24).equals("MASSIVE PAKFILE V 4.0 " + (char) 13 + (char) 10)) {
        rating += 50;
      }

      fm.skip(48);

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

  **********************************************************************************************
  **/
  @Override
  public Resource[] read(File path) {
    try {

      addFileTypes();

      FileManipulator fm = new FileManipulator(path, false);

      // 4 - Version (4)
      // 24 - header ("MASSIVE PAKFILE V 4.0" 13 10)
      // 44 - Unknown
      // 4 - Unknown
      fm.skip(76);

      // 4 - Number Of Files
      int numFiles = fm.readInt();
      FieldValidator.checkNumFiles(numFiles);

      long arcSize = fm.getLength();

      Resource[] resources = new Resource[numFiles];
      TaskProgressManager.setMaximum(numFiles);

      // 4 - Unknown
      fm.skip(4);

      // 4 - Data Offset
      int headDataOffset = fm.readInt();
      FieldValidator.checkOffset(headDataOffset, arcSize);

      // 4 - File Length
      fm.skip(4);

      for (int i = 0; i < numFiles; i++) {
        // 4 - File Length
        long length = fm.readInt();
        FieldValidator.checkLength(length, arcSize);

        // 4 - Data Offset + headDataOffset
        long offset = fm.readInt() + headDataOffset;
        FieldValidator.checkOffset(offset, arcSize);

        // 4 - Name Offset
        int filenameOffset = fm.readInt();

        // 4 - Unknown
        fm.skip(4);

        if ((filenameOffset & 0xFF000000) != 0) {
          filenameOffset = filenameOffset & 0xFFFFFF;
        }

        FieldValidator.checkOffset(filenameOffset, arcSize);

        fm.seek(filenameOffset + (94 + (numFiles * 16)));
        // X - filename (reversed)
        String filename = StringConverter.reverse(fm.readNullString());
        FieldValidator.checkFilename(filename);

        //path,id,name,offset,length,decompLength,exporter
        resources[i] = new Resource(path, filename, offset, length);

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