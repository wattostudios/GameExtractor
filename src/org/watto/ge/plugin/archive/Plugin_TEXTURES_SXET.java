
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
public class Plugin_TEXTURES_SXET extends ArchivePlugin {

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public Plugin_TEXTURES_SXET() {

    super("TEXTURES_SXET", "TEXTURES_SXET");

    //         read write replace rename
    setProperties(true, false, false, false);

    setGames("Pure");
    setExtensions("textures"); // MUST BE LOWER CASE
    setPlatforms("PC");

    //setFileTypes("","",
    //             "",""
    //             );

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
      if (fm.readString(4).equals("SXET")) {
        rating += 50;
      }

      fm.skip(8);

      // Number Of Files
      if (FieldValidator.checkNumFiles(fm.readInt())) {
        rating += 5;
      }

      fm.skip(4);

      // Number Of Files
      if (FieldValidator.checkNumFiles(fm.readInt())) {
        rating += 5;
      }

      long arcSize = fm.getLength();

      // File Data Length
      if (FieldValidator.checkLength(fm.readInt(), arcSize)) {
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

      //ExporterPlugin exporter = Exporter_ZLib.getInstance();

      // RESETTING GLOBAL VARIABLES

      FileManipulator fm = new FileManipulator(path, false);

      long arcSize = fm.getLength();

      // 4 - Header (SXET)
      // 4 - Unknown (11)
      // 4 - null
      fm.skip(12);

      // 4 - Number Of Images
      int numFiles = fm.readInt();
      FieldValidator.checkNumFiles(numFiles);

      // 4 - Unknown (1)
      // 4 - Number Of Images
      fm.skip(8);

      // 4 - File Data Length
      int dirOffset = fm.readInt() + (numFiles * 28) + 28;
      FieldValidator.checkOffset(dirOffset, arcSize);

      Resource[] resources = new Resource[numFiles];
      TaskProgressManager.setMaximum(numFiles);

      // Loop through directory
      long[] offsets = new long[numFiles];
      int relOffset = 28 + (numFiles * 28);
      for (int i = 0; i < numFiles; i++) {
        // 4 - Hash (matches with the Hash in the Filename Directory)
        // 4 - Unknown (33)
        // 4 - Unknown (1)
        // 4 - Number Of Mipmaps? (1/10/11)
        // 4 - Image Width/Height
        // 4 - Image Width/Height
        fm.skip(24);

        // 4 - File Offset
        int offset = fm.readInt() + relOffset;
        FieldValidator.checkOffset(offset, arcSize);

        offsets[i] = offset;
      }

      // Loop through to get the lengths
      long[] lengths = new long[numFiles];
      for (int i = 0; i < numFiles; i++) {
        fm.seek(offsets[i]);

        // 4 - File Length
        int length = fm.readInt();
        FieldValidator.checkLength(length, arcSize);

        lengths[i] = length;
        offsets[i] += 4;
      }

      fm.seek(dirOffset + 4); // +4 to skip the filename directory header

      // Loop through to get the filenames
      for (int i = 0; i < numFiles; i++) {

        // 4 - Hash (matches with the Hash in the Directory)
        fm.skip(4);

        // 4 - Filename Length
        int filenameLength = fm.readInt();
        FieldValidator.checkFilenameLength(filenameLength);

        // X - Filename
        String filename = fm.readString(filenameLength);

        //path,name,offset,length,decompLength,exporter
        resources[i] = new Resource(path, filename, offsets[i], lengths[i]);

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
