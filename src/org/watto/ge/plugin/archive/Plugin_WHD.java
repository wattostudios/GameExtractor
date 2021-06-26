
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
public class Plugin_WHD extends ArchivePlugin {

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public Plugin_WHD() {

    super("WHD", "WHD");

    //         read write replace rename
    setProperties(true, false, false, false);

    setEnabled(false); // DO NOT ALLOW THIS PLUGIN TO BE USED!

    setExtensions("wav");
    setGames("Hitman Contracts");
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

      getDirectoryFile(fm.getFile(), "whd");
      rating += 25;

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

      long arcSize = (int) path.length();

      File sourcePath = getDirectoryFile(path, "whd");

      FileManipulator fm = new FileManipulator(sourcePath, false);

      // 4 - Archive Length (without header)
      // 4 - Archive Length (with header)
      // 4 - Unknown
      // 4 - Unknown
      fm.skip(16);

      int numFiles = Archive.getMaxFiles(4);

      Resource[] resources = new Resource[numFiles];
      TaskProgressManager.setMaximum(arcSize);

      int i = 0;
      long offset = 32;
      while (fm.getOffset() < fm.getLength() - 24) {

        // X - Filename (null)
        String filename = fm.readNullString();
        FieldValidator.checkFilename(filename);

        // 0-3 - Padding to multiple of 4 bytes
        int paddingLength = 4 - ((filename.length() + 1) % 4);
        if (paddingLength < 4) {
          fm.skip(paddingLength);
        }

        // 4 - Unknown (6)
        // 4 - Unknown
        // 4 - Unknown (17)
        fm.skip(12);

        // 4 - Sampling Rate, khz (22050)
        FieldValidator.checkEquals(fm.readInt(), 22050);

        // 4 - Unknown (4)
        // 4 - Unknown
        fm.skip(8);

        // 4 - File Length
        long length = fm.readInt();
        FieldValidator.checkLength(length, arcSize);

        // 4 - Number Of Channels?, mono (1)
        // 4 - Unknown
        // 4 - Unknown
        // 4 - Unknown
        // 4 - Unknown
        fm.skip(20);

        //path,id,name,offset,length,decompLength,exporter
        resources[i] = new Resource(path, filename, offset, length);

        TaskProgressManager.setValue(offset);
        offset += length;
        i++;
      }

      resources = resizeResources(resources, i - 1);

      fm.close();

      return resources;

    }
    catch (Throwable t) {
      logError(t);
      return null;
    }
  }

}