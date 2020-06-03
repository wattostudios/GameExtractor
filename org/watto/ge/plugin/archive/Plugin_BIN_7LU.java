
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
public class Plugin_BIN_7LU extends ArchivePlugin {

  /**
  **********************************************************************************************

  **********************************************************************************************
  **/
  public Plugin_BIN_7LU() {

    super("BIN_7LU", "7th Level Engine - Format U");

    //         read write replace rename
    setProperties(true, false, false, false);

    setGames("Battle Beast",
        "Take Your Best Shot",
        "Arcade America",
        "Monty Python's Complete Waste Of Time",
        "Monty Python And The Quest For The Holy Grail",
        "Monty Pythons Meaning Of Life",
        "Krondor",
        "G-Nome",
        "Tuneland",
        "The Great Word Adventure",
        "The Universe According To Virgil");
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

      // Header
      if (fm.readString(2).equals("7L")) {
        rating += 50;
      }

      // Version 1
      if (fm.readShort() == 476) {
        rating += 5;
      }

      // Version 2
      if (fm.readInt() == 2) {
        rating += 5;
      }

      getDirectoryFile(fm.getFile(), "000");
      rating += 5;

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

      FileManipulator fm = new FileManipulator(path, false);

      File sourcePath = path;
      path = getDirectoryFile(path, "000");

      long arcSize = (int) path.length();

      // 4 - Header (7L + (byte)220 + (byte)1) // (byte) 220 is a U with double dots above it
      // 2 - Unknown
      // 4 - Version (2)
      // 76 - Description (null)
      // 4 - Unknown (258)
      // 2 - null
      // 4 - Unknown
      // 188 - null
      // 4 - Unknown (1)
      // 4 - Unknown (236)
      // 4 - null
      // 4 - Unknown (150)
      // 4 - Unknown (42)
      // 4 - Unknown (268)
      // 4 - Unknown (274)
      // 32 - null
      // 2 - Unknown (1)
      // 4 - Unknown
      // 2 - null
      // 4 - null
      fm.skip(356);

      // 4 - Directory Offset
      long dirOffset = fm.readInt();
      FieldValidator.checkOffset(dirOffset, arcSize);

      // 4 - Directory Length
      int numFiles = fm.readInt() / 10;
      FieldValidator.checkNumFiles(numFiles);

      // 16 - null
      // 4 - Counter Offset
      // 4 - Counter Length
      // 4 - Filename Directory Offset
      // 4 - Filename Directory Length
      // 4 - File Data Offset (ie. offset to the start of the first file)
      // 4 - File Data Length
      // 4 - Image Offset
      // 4 - Image Length
      // 24 - null
      // 4 - Filename Directory 2 Offset
      // 4 - Filename Directory 2 Length
      // 4 - Filename Directory 3 Offset
      // 4 - Filename Directory 3 Length
      // 4 - File ID Directory Offset
      // 4 - File ID Directory Length
      // 8 - null
      // 4 - Source Filename Offset
      // 4 - Source Filename Length

      Resource[] resources = new Resource[numFiles];
      TaskProgressManager.setMaximum(numFiles);

      fm.seek(dirOffset);

      // Loop through directory
      for (int i = 0; i < numFiles; i++) {
        // 2 - File Type ID? (1,3,5,11,12)
        String ext = "." + fm.readShort();

        // 4 - Offset (relative to the end of the directory)
        long offset = fm.readInt();
        FieldValidator.checkOffset(offset, arcSize);

        // 4 - Length
        long length = fm.readInt();
        FieldValidator.checkLength(length, arcSize);

        //String filename = names[i] + ext;
        String filename = Resource.generateFilename(i) + ext;

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
