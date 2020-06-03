
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
public class Plugin_BIN_7L extends ArchivePlugin {

  /**
  **********************************************************************************************

  **********************************************************************************************
  **/
  public Plugin_BIN_7L() {

    super("BIN_7L", "7th Level Engine - Original");

    //         read write replace rename
    setProperties(true, false, false, false);

    allowImplicitReplacing = true;

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

      // Version
      if (fm.readShort() == 1) {
        rating += 5;
      }

      fm.skip(79);

      // null
      if (fm.readByte() == 0) {
        rating += 5;
      }

      // Number Of Files
      int numFiles = fm.readShort();
      if (FieldValidator.checkNumFiles(numFiles)) {
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
  @SuppressWarnings("unused")
  @Override
  public Resource[] read(File path) {
    try {

      addFileTypes();

      FileManipulator fm = new FileManipulator(path, false);

      long arcSize = fm.getLength();

      // 2 - Header (7L)
      // 2 - Version (1)
      // 1 - Description Length
      // 79 - Description (null)
      fm.skip(84);

      // 2 - Number Of Files
      int numFiles = fm.readShort();
      FieldValidator.checkNumFiles(numFiles);

      // 2 - Unknown
      // 2 - Unknown
      // 2 - Unknown
      // 4 - Unknown
      // 4 - null
      fm.skip(14);

      // 2 - Filename Directory Length
      int filenameDirLength = fm.readShort();
      FieldValidator.checkLength(filenameDirLength, arcSize);

      // 4 - N/A (all 255's)
      // 4 - Unknown
      // 8 - null
      fm.skip(16);

      Resource[] resources = new Resource[numFiles];
      TaskProgressManager.setMaximum(numFiles);

      /*
       * long dirOffset = (int)fm.getOffset(); fm.skip(numFiles*10);
       *
       * String[] names = new String[numFiles]; // Loop through directory for(int
       * i=0;i<numFiles;i++){ names[i] = fm.readNullString(); }
       *
       * fm.seek(dirOffset);
       */

      // Loop through directory
      int relOffset = numFiles * 10 + filenameDirLength + 118;
      for (int i = 0; i < numFiles; i++) {
        // 2 - File Type ID? (1,3,5,11,12)
        String ext = "." + fm.readShort();

        // 4 - Offset (relative to the end of the directory)
        long offsetPointerLocation = fm.getOffset();
        long offsetPointerLength = 4;

        long offset = fm.readInt();
        FieldValidator.checkOffset(offset, arcSize);

        // 4 - Length
        long lengthPointerLocation = fm.getOffset();
        long lengthPointerLength = 4;

        long length = fm.readInt();
        FieldValidator.checkLength(length, arcSize);

        //String filename = names[i] + ext;
        String filename = Resource.generateFilename(i) + ext;

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
