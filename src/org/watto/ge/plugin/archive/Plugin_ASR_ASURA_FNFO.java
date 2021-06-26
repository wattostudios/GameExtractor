
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
public class Plugin_ASR_ASURA_FNFO extends ArchivePlugin {

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public Plugin_ASR_ASURA_FNFO() {

    super("ASR_ASURA_FNFO", "ASR_ASURA_FNFO");

    //         read write replace rename
    setProperties(true, false, false, false);

    setGames("PRISM Guard Shield");
    setExtensions("asr");
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
      if (fm.readString(8).equals("Asura   ")) {
        rating += 50;
      }

      // Reference Header
      if (fm.readString(4).equals("FNFO")) {
        rating += 5;
      }

      // Entry Length (24)
      if (fm.readInt() == 24) {
        rating += 5;
      }

      // Unknown (1)
      if (fm.readInt() == 1) {
        rating += 5;
      }

      // null
      if (fm.readLong() == 0) {
        rating += 5;
      }

      // Unknown (7)
      if (fm.readInt() == 7) {
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

      // 8 - Header (Asura   )
      fm.skip(8);

      int numFiles = Archive.getMaxFiles();

      Resource[] resources = new Resource[numFiles];
      TaskProgressManager.setMaximum(arcSize);

      // Loop through directory
      int realNumFiles = 0;
      while (fm.getOffset() < arcSize) {
        // 4 - Header
        String headerType = fm.readString(4);

        // 4 - Entry Length
        long entryLength = fm.readInt() - 8;
        FieldValidator.checkLength(entryLength, arcSize);

        if (headerType.equals("RSCF")) {
          // 4 - Unknown (1)
          // 4 - null
          // 4 - Unknown (2)
          // 4 - File ID?
          fm.skip(16);

          // 4 - File Data Length
          long length = fm.readInt();
          FieldValidator.checkLength(length, arcSize);

          // X - Filename
          // 1 - null Filename Terminator
          String filename = fm.readNullString();
          FieldValidator.checkFilename(filename);

          // 0-3 - null Padding so filenameLength+1null is a multiple of 4 bytes
          int filenameLength = filename.length() + 1;
          int paddingSize = 4 - (filenameLength % 4);
          if (paddingSize != 4) {
            fm.skip(paddingSize);
          }

          // X - File Data
          long offset = fm.getOffset();
          fm.skip(length);

          //path,name,offset,length,decompLength,exporter
          resources[realNumFiles] = new Resource(path, filename, offset, length);

          TaskProgressManager.setValue(offset);
          realNumFiles++;

        }
        else {
          // skip over the other entry types
          fm.skip(entryLength);
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
