
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
public class Plugin_XWC_MOS extends ArchivePlugin {

  /**
  **********************************************************************************************

  **********************************************************************************************
  **/
  public Plugin_XWC_MOS() {

    super("XWC_MOS", "XWC_MOS");

    //         read write replace rename
    setProperties(true, false, false, false);

    setGames("The Chronicles of Riddick: Escape From Butcher Bay");
    setExtensions("xwc", "xtc");
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
      if (fm.readString(15).equals("MOS DATAFILE2.0")) {
        rating += 50;
      }

      // null
      if (fm.readByte() == 0) {
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

      // 16 - Header (MOS DATAFILE2.0 + null)
      // 8 - null
      // 4 - Unknown
      // 20 - null
      fm.skip(48);

      // Check for padding
      boolean padding = true;
      if (fm.readString(16).equals("1234567890123456")) {
        // has padding
        fm.seek(2048);
        padding = true;
      }
      else {
        fm.seek(48);
        padding = false;
      }

      int numFiles = Archive.getMaxFiles(4);

      long arcSize = fm.getLength();

      Resource[] resources = new Resource[numFiles];
      TaskProgressManager.setMaximum(numFiles);

      // Loop through directory
      int realNumFiles = 0;
      while (fm.getOffset() < arcSize) {

        // 4 - Unknown (768)
        // 4 - Unknown (36864)
        fm.skip(8);

        // 4 - Length (excluding all header data - ie the length of the file data only)
        long length = fm.readInt();
        if (!padding && realNumFiles == 0) {
          length -= 76;
        }
        FieldValidator.checkLength(length, arcSize);

        // 4 - Unknown (1)
        // 4 - Unknown
        // 4 - Unknown (36864)
        // 4 - null
        fm.skip(16);

        // X - File Data
        long offset = (int) fm.getOffset();
        fm.skip(length);

        String filename = Resource.generateFilename(realNumFiles);

        //path,id,name,offset,length,decompLength,exporter
        resources[realNumFiles] = new Resource(path, filename, offset, length);

        TaskProgressManager.setValue(offset);
        realNumFiles++;
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

}
