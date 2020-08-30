
package org.watto.ge.plugin.archive;

import java.io.File;
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
import org.watto.task.TaskProgressManager;

/**
**********************************************************************************************

**********************************************************************************************
**/
public class Plugin_BIN_4 extends ArchivePlugin {

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public Plugin_BIN_4() {

    super("BIN_4", "7th Level Demo Archive");

    //         read write replace rename
    setProperties(true, false, false, false);

    setGames("Battle Beast",
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
      if (fm.readByte() == 55 && fm.readByte() == 76 && fm.readByte() == -24 && fm.readByte() == 0) {
        rating += 50;
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

      long arcSize = fm.getLength();

      // 4 - Header (55 76 232 0)
      // 1 - Description Length
      // 79 - Description (null)
      // 4 - Unknown (2)
      // 4 - null
      // 4 - Unknown
      // 4 - null
      // 2 - Unknown (1)
      // 4 - Unknown (15)
      // 4 - Unknown
      // 2 - Unknown
      // 4 - Unknown
      // 12 - null
      fm.seek(128);

      // 4 - Normal Directory Offset
      long dirOffset = fm.readInt();
      FieldValidator.checkOffset(dirOffset, arcSize);

      // 4 - Normal Directory Length
      int dirLength = fm.readInt();
      FieldValidator.checkLength(dirLength, arcSize);

      // 24 - null
      fm.skip(24);

      // 4 - Filename Directory Offset
      int filenameDirOffset = fm.readInt();
      FieldValidator.checkOffset(filenameDirOffset, arcSize);

      // 4 - Filename Directory Length
      int filenameDirLength = fm.readInt();
      FieldValidator.checkLength(filenameDirLength, arcSize);

      // 4 - Unknown Directory Offset
      // 4 - Unknown Directory Length
      // 8 - null
      // 4 - Offsets Directory Offset
      // 4 - Offsets Directory Length
      // 40 - null

      fm.seek(filenameDirOffset);

      int numFiles = 0;
      String[] names = new String[Archive.getMaxFiles(4)];
      while (fm.getOffset() < filenameDirOffset + filenameDirLength) {
        // X - Filename (null)
        String filename = fm.readNullString();

        if (filename.length() > 3 && filename.substring(0, 3).equals("N:\\")) {
          filename = filename.substring(3);
        }
        else if (filename.equals("")) {
          filename = Resource.generateFilename(numFiles);
        }
        else if (filename.lastIndexOf(":") > -1) {
          filename = Resource.generateFilename(numFiles);
        }

        names[numFiles] = filename;

        numFiles++;
      }

      Resource[] resources = new Resource[numFiles];
      TaskProgressManager.setMaximum(numFiles);

      fm.seek(dirOffset);

      for (int i = 0; i < numFiles; i++) {
        // 2 - File Type? Filename ID? (1, 13, 14, 15, others?)
        fm.skip(2);

        // 4 - Data Offset [+232] (from end of header)
        long offset = fm.readInt() + 232;
        FieldValidator.checkOffset(offset, arcSize);

        // 4 - File Size
        long length = fm.readInt();
        FieldValidator.checkLength(length, arcSize);

        //path,id,name,offset,length,decompLength,exporter
        resources[i] = new Resource(path, names[i], offset, length);

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
