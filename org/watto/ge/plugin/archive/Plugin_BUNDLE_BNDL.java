
package org.watto.ge.plugin.archive;

import java.io.File;
import org.watto.task.TaskProgressManager;
import org.watto.datatype.Archive;
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
import org.watto.io.converter.ByteConverter;

/**
**********************************************************************************************

**********************************************************************************************
**/
public class Plugin_BUNDLE_BNDL extends ArchivePlugin {

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public Plugin_BUNDLE_BNDL() {

    super("BUNDLE_BNDL", "BUNDLE_BNDL");

    //         read write replace rename
    setProperties(true, false, false, false);
    setCanImplicitReplace(true);

    setGames("Ghost Recon Advanced Warfighter",
        "Ghost Recon Advanced Warfighter 2");
    setExtensions("bundle");
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
      if (fm.readString(4).equals("BNDL")) {
        rating += 50;
      }

      // Version
      if (fm.readInt() == 2) {
        rating += 5;
      }

      long arcSize = fm.getLength();

      // First File Offset
      if (FieldValidator.checkOffset(fm.readInt(), arcSize)) {
        rating += 5;
      }

      // null
      if (fm.readInt() == 0) {
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

      // 4 - Header (BNDL)
      // 4 - Version (2)
      // 8 - First File Offset
      fm.skip(16);

      int numFiles = Archive.getMaxFiles();

      Resource[] resources = new Resource[numFiles];
      TaskProgressManager.setMaximum(numFiles);

      // Loop through directory
      String[] dirNames = new String[10];
      int numDirs = 0;

      int realNumFiles = 0;
      for (int i = 0; i < numFiles; i++) {
        // 1 - Entry Type
        int entryType = ByteConverter.unsign(fm.readByte());

        if (entryType == 1) {
          // Start of SubFolder

          // 1 - Unknown (1)
          fm.skip(1);

          // X - Folder Name
          // 1 - null Folder Name Terminator
          String dirName = fm.readNullString();
          FieldValidator.checkFilename(dirName);

          dirNames[numDirs] = dirName;
          numDirs++;

        }
        else if (entryType == 2) {
          // File Entry

          // 8 - File Offset
          long offsetPointerLocation = fm.getOffset();
          int offsetPointerLength = 8;

          long offset = fm.readLong();
          FieldValidator.checkOffset(offset, arcSize);

          // 4 - File Length
          long lengthPointerLocation = fm.getOffset();
          int lengthPointerLength = 4;

          long length = fm.readInt();
          FieldValidator.checkLength(length, arcSize);

          // 1 - Unknown (1)
          fm.skip(1);

          // X - Filename
          // 1 - null Filename Terminator
          String filename = fm.readNullString();
          FieldValidator.checkFilename(filename);

          // add the directory names to the start of the filename
          for (int n = numDirs - 1; n >= 0; n--) {
            filename = dirNames[n] + "\\" + filename;
          }

          //path,name,offset,length,decompLength,exporter
          resources[realNumFiles] = new ReplacableResource(path, filename, offset, offsetPointerLocation, offsetPointerLength, length, lengthPointerLocation, lengthPointerLength);
          realNumFiles++;

          TaskProgressManager.setValue(realNumFiles);

        }
        else if (entryType == 3) {
          // End of Current SubFolder
          numDirs--;
        }

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
