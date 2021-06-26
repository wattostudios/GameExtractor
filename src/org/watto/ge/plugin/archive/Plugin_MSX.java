
package org.watto.ge.plugin.archive;

import java.io.File;
import org.watto.task.TaskProgressManager;
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
public class Plugin_MSX extends ArchivePlugin {

  /**
  **********************************************************************************************

  **********************************************************************************************
  **/
  public Plugin_MSX() {

    super("MSX", "MSX");

    //         read write replace rename
    setProperties(true, false, false, false);

    setGames("Mortal Kombat Deceptions");
    setExtensions("msx");
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

      // Version (2)
      if (fm.readInt() == 2) {
        rating += 5;
      }

      // Number Of Files
      if (FieldValidator.checkNumFiles(fm.readInt())) {
        rating += 5;
      }

      // Padding Size (2048)
      if (fm.readInt() == 2048) {
        rating += 5;
      }

      long arcSize = fm.getLength();

      // Directory Size
      if (FieldValidator.checkLength(fm.readInt(), arcSize)) {
        rating += 5;
      }

      // Padding Size (2048)
      if (fm.readInt() == 2048) {
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
  @SuppressWarnings("unused")
  @Override
  public Resource[] read(File path) {
    try {

      // NOTE - Compressed file MUST know their DECOMPRESSED LENGTH
      //      - Uncompressed files MUST know their LENGTH

      addFileTypes();

      // RESETTING THE GLOBAL VARIABLES

      FileManipulator fm = new FileManipulator(path, false);

      long arcSize = (int) fm.getLength();

      // 4 - Version (2)
      fm.skip(4);

      // 4 - Number Of Files [-1]
      int numFiles = fm.readInt() - 1;
      FieldValidator.checkNumFiles(numFiles);

      // 4 - File Padding Size (2048)
      // 4 - Directory Size (including this archive header)
      fm.skip(8);

      // 4 - Directory Padding Size (2048)
      long offset = fm.readInt();
      FieldValidator.checkOffset(offset, arcSize);

      // 12 - null
      // 4 - Archive Size
      // 16 - null
      fm.skip(32);

      SplitChunkResource[] resources = new SplitChunkResource[numFiles];

      TaskProgressManager.setMaximum(numFiles);

      // Loop through directory
      long offsetRIFF = 52 + (numFiles * 24);
      for (int i = 0; i < numFiles; i++) {
        // 2 - File ID (starting at 1, incremental)
        int fileID = fm.readShort();
        //System.out.println(fileID);

        // 1 - null
        // 2 - Unknown (32)
        // 2 - Unknown
        // 1 - null
        fm.skip(6);

        // 4 - File Size (including the 40 bytes of the header in the next loop)
        long length = fm.readInt() - 40;
        FieldValidator.checkLength(length, arcSize);

        // 4 - Padding Length?
        // 4 - Unknown (40)
        // 4 - null
        fm.skip(12);

        long[] offsets = new long[2];
        long[] lengths = new long[2];

        offsets[0] = offsetRIFF;
        offsets[1] = offset;

        lengths[0] = 40;
        lengths[1] = length;

        offsetRIFF += 40;

        String filename = Resource.generateFilename(i) + ".wav";

        //path,id,name,offset,length,decompLength,exporter
        resources[i] = new SplitChunkResource(path, filename, offsets, lengths);

        TaskProgressManager.setValue(i);

        offset += length;

        long paddingSize = 2048 - (length % 2048);
        if (paddingSize < 2048) {
          offset += paddingSize;
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
