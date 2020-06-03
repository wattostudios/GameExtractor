
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
public class Plugin_ARC_IGA extends ArchivePlugin {

  /**
  **********************************************************************************************

  **********************************************************************************************
  **/
  public Plugin_ARC_IGA() {

    super("ARC_IGA", "ARC_IGA");

    //         read write replace rename
    setProperties(true, false, false, false);

    setGames("Madagascar: Escape 2 Africa");
    setExtensions("arc", "bld"); // MUST BE LOWER CASE
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

      // 4 - Header ("IGA" + (byte)26)
      if (fm.readString(4).equals("IGA" + (char) 26)) {
        rating += 50;
      }

      // 4 - Version (2)
      if (fm.readInt() == 2) {
        rating += 5;
      }

      long arcSize = fm.getLength();

      // 4 - Directory Length (not including null padding)
      if (FieldValidator.checkLength(fm.readInt(), arcSize)) {
        rating += 5;
      }

      // 4 - Number Of Files
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

      // 4 - Header ("IGA" + (byte)26)
      // 4 - Version (2)
      // 4 - Directory Length (not including null padding)
      fm.skip(12);

      // 4 - Number Of Files
      int numFiles = fm.readInt();
      FieldValidator.checkNumFiles(numFiles);

      // 4 - Unknown
      // 4 - Unknown (13)
      fm.skip(8);

      // 4 - Filename Directory Offset
      int dirOffset = fm.readInt();
      FieldValidator.checkOffset(dirOffset, arcSize);

      // 4 - Filename Directory Length
      // 16 - null
      fm.seek(dirOffset);

      // Loop through the names directories
      int[] filenameOffsets = new int[numFiles];
      int relOffset = dirOffset + numFiles * 4;
      for (int i = 0; i < numFiles; i++) {
        // 4 - Filename Offset (relative to the start of the filename directory)
        int filenameOffset = fm.readInt() + relOffset;
        FieldValidator.checkOffset(filenameOffset, arcSize);
        filenameOffsets[i] = filenameOffset;
      }

      String[] filenames = new String[numFiles];
      for (int i = 0; i < numFiles - 1; i++) {
        fm.seek(filenameOffsets[i]);

        int filenameLength = filenameOffsets[i + 1] - filenameOffsets[i];

        // X - Filename (starting with c:/)
        String filename = fm.readString(filenameLength);
        FieldValidator.checkFilename(filename);

        if (filename.startsWith("c:/")) {
          filename = filename.substring(3);
        }

        filenames[i] = filename;
      }

      // the last filename needs to be read differently because there is no numFiles+1 to use
      // to determine the length of the filename
      fm.seek(filenameOffsets[numFiles - 1]);

      int filenameLength = (int) (arcSize - filenameOffsets[numFiles - 1]);

      // X - Filename (starting with c:/)
      String filename = fm.readString(filenameLength);
      FieldValidator.checkFilename(filename);

      if (filename.startsWith("c:/")) {
        filename = filename.substring(3);
      }

      filenames[numFiles - 1] = filename;

      // now go back and read the file details directory
      fm.seek(48 + numFiles * 4);

      Resource[] resources = new Resource[numFiles];
      TaskProgressManager.setMaximum(numFiles);

      // Loop through directory
      for (int i = 0; i < numFiles; i++) {
        // 4 - File Offset
        int offset = fm.readInt();
        FieldValidator.checkOffset(offset, arcSize);

        // 4 - File Length (not including null padding)
        int length = fm.readInt();
        FieldValidator.checkLength(length, arcSize);

        // 4 - Padding (-1)
        fm.skip(4);

        filename = filenames[i];

        //path,name,offset,length,decompLength,exporter
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
