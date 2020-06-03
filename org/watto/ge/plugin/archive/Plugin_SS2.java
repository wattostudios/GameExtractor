
package org.watto.ge.plugin.archive;

import java.io.File;
import org.watto.task.TaskProgressManager;
import org.watto.datatype.Archive;
import org.watto.datatype.Resource;
import org.watto.datatype.SplitChunkResource;
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
public class Plugin_SS2 extends ArchivePlugin {

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public Plugin_SS2() {

    super("SS2", "SS2");

    //         read write replace rename
    setProperties(true, false, false, false);

    setGames("Rainbow Six: Black Arrow");
    setExtensions("ss2");
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

      fm.skip(4);

      // Number Of Parts In Each Group (3)
      if (fm.readInt() == 3) {
        rating += 5;
      }

      fm.skip(4);

      // Maximum Number Of Groups
      if (FieldValidator.checkNumFiles(fm.readInt())) {
        rating += 5;
      }

      // Group Header Size (24)
      if (fm.readInt() == 24) {
        rating += 5;
      }

      long arcSize = fm.getLength();

      // Maximum Length Of Each Group
      if (FieldValidator.checkLength(fm.readInt(), arcSize)) {
        rating += 5;
      }

      // Average Size of Each Part (482)
      if (fm.readInt() == 482) {
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

      // 4 - Unknown (4)
      // 4 - Number Of Parts In Each Group (3)
      // 4 - Unknown
      // 4 - Maximum Number Of Groups (too large by about 300)
      // 4 - Group Header Size (24)
      // 4 - Maximum Length Of Each Group (including Header)?
      // 4 - Average Size of Each Part (482)

      // 100 - Unknown to offset 128

      fm.seek(128);

      int numFiles = Archive.getMaxFiles();

      Resource[] resources = new Resource[numFiles];

      TaskProgressManager.setMaximum(numFiles);

      // Loop through directory
      int realNumFiles = 0;
      while (fm.getOffset() < arcSize) {
        // 4 - Part ID (incremental from 1)
        // 4 - This Offset [+4]
        fm.skip(8);

        // 4 - Number Of Parts
        int numParts = fm.readInt();
        FieldValidator.checkNumFiles(numParts);

        long[] lengths = new long[numParts];
        long[] offsets = new long[numParts];
        long offset = fm.getOffset() + (numParts * 4);
        long totalLength = 0;
        for (int p = 0; p < numParts; p++) {
          // 4 - Part Length
          long length = fm.readInt();
          FieldValidator.checkLength(length, arcSize);

          lengths[p] = length;
          offsets[p] = offset;

          offset += length;
          totalLength += length;
        }

        // X - Part Data
        fm.skip(totalLength);

        String filename = Resource.generateFilename(realNumFiles) + ".part";

        //path,id,name,offset,length,decompLength,exporter
        resources[realNumFiles] = new SplitChunkResource(path, filename, offsets, lengths);

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
