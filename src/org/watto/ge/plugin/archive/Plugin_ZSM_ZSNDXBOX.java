
package org.watto.ge.plugin.archive;

import java.io.File;
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
import org.watto.ge.plugin.ExporterPlugin;
import org.watto.ge.plugin.exporter.Exporter_Custom_ZSM_ZSNDXBOX;
import org.watto.ge.plugin.resource.Resource_FileID;
import org.watto.io.FileManipulator;
import org.watto.task.TaskProgressManager;

/**
**********************************************************************************************

**********************************************************************************************
**/
public class Plugin_ZSM_ZSNDXBOX extends ArchivePlugin {

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public Plugin_ZSM_ZSNDXBOX() {

    super("ZSM_ZSNDXBOX", "X-Men ZSM");

    //         read write replace rename
    setProperties(true, false, false, false);

    setGames("X-Men: Legends",
        "X-Men Legends 2: Rise Of Apocalypse",
        "X-Men: The Official Game");
    setExtensions("zsm", "zss");
    setPlatforms("PC", "XBox");

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
      if (fm.readString(4).equals("ZSND")) {
        rating += 25;
      }
      String type = fm.readString(4);
      if (type.equals("XBOX") || type.equals("PC  ")) {
        rating += 25;
      }

      fm.skip(4);

      long arcSize = fm.getLength();

      // Directory Length
      if (FieldValidator.checkLength(fm.readInt(), arcSize)) {
        rating += 5;
      }

      // Number Of Files
      if (FieldValidator.checkNumFiles(fm.readInt() + 1)) {
        rating += 5;
      }

      // Dir Offset 1
      if (FieldValidator.checkOffset(fm.readInt(), arcSize)) {
        rating += 5;
      }

      // Dir Offset 2
      if (FieldValidator.checkOffset(fm.readInt(), arcSize)) {
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

      // NOTE - Compressed file MUST know their DECOMPRESSED LENGTH
      //      - Uncompressed files MUST know their LENGTH

      addFileTypes();
      ExporterPlugin exporter = Exporter_Custom_ZSM_ZSNDXBOX.getInstance();

      // RESETTING THE GLOBAL VARIABLES

      FileManipulator fm = new FileManipulator(path, false);

      long arcSize = (int) fm.getLength();

      // 8 - Header (ZSNDXBOX)
      // 4 - Archive Size [+/- a few bytes]
      // 4 - Directory Length
      // 4 - Number Of Items In Directory 1
      // 4 - Offset To Hash Directory 1
      // 4 - Offset To Directory Data 1
      // 4 - Number Of Items In Directory 2
      // 4 - Offset To Hash Directory 2
      fm.skip(36);

      // 4 - Offset To Directory Data 2 (Sound Details)
      int audioDataOffset = fm.readInt();

      // 4 - Number Of Items In Directory 3
      int numFiles = fm.readInt();
      FieldValidator.checkNumFiles(numFiles);

      // 4 - Offset To Hash Directory 3
      fm.skip(4);

      // 4 - Offset To Directory Data 3 (Filenames/Offsets)
      long dirOffset = fm.readInt();
      FieldValidator.checkOffset(dirOffset, arcSize);

      // 4 - Number Of Items In Directory 4 (null)
      // 4 - Offset To Hash Directory 4
      // 4 - Offset To Directory Data 4
      // 4 - Number Of Items In Directory 5 (sometimes null)
      // 4 - Offset To Hash Directory 5
      // 4 - Offset To Directory Data 5
      // 4 - Number Of Items In Directory 6 (null)
      // 4 - Offset To Hash Directory 6
      // 4 - Offset To Directory Data 6
      // 4 - Number Of Items In Directory 7 (null)
      // 4 - Offset To Hash Directory 7
      // 4 - Offset To Directory Data 7
      fm.seek(dirOffset);

      Resource[] resources = new Resource[numFiles];

      TaskProgressManager.setMaximum(numFiles);

      // Loop through directory
      for (int i = 0; i < numFiles; i++) {
        // 4 - File Offset
        long offset = fm.readInt();
        FieldValidator.checkOffset(offset, arcSize);

        // 4 - File Length
        long length = fm.readInt();
        FieldValidator.checkLength(length, arcSize);

        // 4 - Unknown (1/3/106)
        fm.skip(4);

        if (length == 0 && offset == 0) {
          // not all the files are used - ie blank entries for the rest of the directory
          // I THINK THIS WAS ONLY BECAUSE THE ARCHIVE HEADER WAS CUT SHORT BY THE FILE CUTTER!
          resources = resizeResources(resources, i);
          i = numFiles;
        }
        else {
          // optional null padding
          byte testByte = fm.readByte();
          while (testByte == 0) {
            testByte = fm.readByte();
          }

          // 64 - Filename (null terminated)
          String filename = ((char) testByte) + fm.readNullString(63);
          FieldValidator.checkFilename(filename);

          int fileID = audioDataOffset;

          //path,id,name,offset,length,decompLength,exporter
          resources[i] = new Resource_FileID(path, fileID, filename, offset, length, length, exporter);

          audioDataOffset += 28;

          TaskProgressManager.setValue(i);
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
