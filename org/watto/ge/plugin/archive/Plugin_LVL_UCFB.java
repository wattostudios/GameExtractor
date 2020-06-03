
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
public class Plugin_LVL_UCFB extends ArchivePlugin {

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public Plugin_LVL_UCFB() {

    super("LVL_UCFB", "LVL_UCFB");

    //         read write replace rename
    setProperties(true, false, false, false);

    setGames("Star Wars Battlefront");
    setExtensions("lvl");
    setPlatforms("PC");

    setFileTypes("tex_", "Texture Images",
        "skel", "3D Skeleton",
        "modl", "3D Model",
        "gmod", "3D Model Details",
        "coll", "Collection"
    //"wpnc","",
    //"zaa_","",
    //"zaf_","",
    //"entc",""
    );

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
      if (fm.readString(4).equals("ucfb")) {
        rating += 50;
      }

      long arcSize = fm.getLength();

      // Archive Size
      if (fm.readInt() + 8 == arcSize) {
        rating += 5;
      }

      // Archive Extension
      if (fm.readString(4).equals("lvl_")) {
        rating += 5;
      }

      // Archive Size
      if (fm.readInt() + 16 == arcSize) {
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

      // 4 - Header (ucfb)
      // 4 - Archive Size [+8]
      // 4 - File Extension (lvl_)
      // 4 - Archive Size [+16]
      // 4 - Unknown
      // 4 - Archive Size [+24]
      fm.skip(24);

      int numFiles = Archive.getMaxFiles();

      Resource[] resources = new Resource[numFiles];

      TaskProgressManager.setMaximum(numFiles);

      // Loop through directory
      int realNumFiles = 0;
      while (fm.getOffset() < arcSize) {
        // 4 - File Type Code/Extension ("coll","entc","gmod","modl","tex_","skel","wpnc","zaa_","zaf_" etc.)
        String extension = fm.readString(4);

        // 4 - File Size (not including this and the previous field)
        long length = fm.readInt();
        FieldValidator.checkLength(length, arcSize);

        // 4 - Filename Header (NAME)
        fm.skip(4);

        // 4 - Filename Length (including 1 null)
        int filenameLength = fm.readInt();
        FieldValidator.checkFilenameLength(filenameLength);

        // X - Filename (not including extension)
        String filename = fm.readString(filenameLength - 1) + "." + extension;
        //filename = fm.removeNonFilename(filename);
        FieldValidator.checkFilename(filename);

        // 1 - null Filename Terminator
        fm.skip(1);

        // 0-3 - null Padding to a multiple of 4 bytes
        int padSize = 4 - (filenameLength % 4);
        if (padSize < 4) {
          fm.skip(padSize);
          length -= padSize;
        }

        length -= (filenameLength + 8);

        // X - File Data
        long offset = fm.getOffset();
        fm.skip(length);

        //path,id,name,offset,length,decompLength,exporter
        resources[realNumFiles] = new Resource(path, filename, offset, length);

        TaskProgressManager.setValue(realNumFiles);
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
