
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
public class Plugin_XMB_XMBF extends ArchivePlugin {

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public Plugin_XMB_XMBF() {

    super("XMB_XMBF", "XMB_XMBF");

    //         read write replace rename
    setProperties(true, false, false, false);

    setGames("Test Drive Unlimited");
    setExtensions("xmb"); // MUST BE LOWER CASE
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
      if (fm.readString(4).equals("XMBF")) {
        rating += 50;
      }

      fm.skip(4);

      long arcSize = fm.getLength();

      // Directory Length
      if (FieldValidator.checkLength(fm.readInt(), arcSize)) {
        rating += 5;
      }

      // Directory Offset
      if (FieldValidator.checkOffset(fm.readInt(), arcSize)) {
        rating += 5;
      }

      // Directory Offset
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

      // NOTE - Compressed files MUST know their DECOMPRESSED LENGTH
      //      - Uncompressed files MUST know their LENGTH

      addFileTypes();

      //ExporterPlugin exporter = Exporter_ZLib.getInstance();

      // RESETTING GLOBAL VARIABLES

      FileManipulator fm = new FileManipulator(path, false);

      long arcSize = fm.getLength();

      // 4 - Header (XMBF)
      // 4 - Unknown
      // 4 - ID Directory Entry Length (numEntries = idDirLength/thisValue)
      // 4 - ID Directory Offset
      fm.skip(16);

      // 4 - Entries Directory Offset
      long dirOffset = fm.readInt();
      FieldValidator.checkOffset(dirOffset, arcSize);

      // 4 - null
      // 4 - Unknown (10)
      fm.seek(dirOffset);

      // 4 - Number Of Entries in Block 1
      int numFiles = fm.readInt();
      FieldValidator.checkNumFiles(numFiles);

      // 4 - Block 1 Offset (relative to the start of the entries directory) (16)
      // 4 - Number Of Entries in Block 2
      // 4 - Block 2 Offset (relative to the start of the entries directory)
      fm.skip(12);

      Resource[] resources = new Resource[numFiles];
      TaskProgressManager.setMaximum(numFiles);

      long[] offsets = new long[numFiles];
      int[] entryTypes = new int[numFiles];

      // Loop through directory
      for (int i = 0; i < numFiles; i++) {

        // 4 - File ID (incremental from 0)
        fm.skip(4);

        // 4 - Filename 1 Offset (relative to the start of the entries directory)
        long offset = fm.readInt() + dirOffset;
        FieldValidator.checkOffset(offset, arcSize);
        offsets[i] = offset;

        // 4 - File ID (incremental from 0)
        // 4 - File Type ID?
        // 4 - Unknown
        // 1 - Flag (0/1)
        // 4 - Filename 2 Offset (relative to the start of the entries directory)
        fm.skip(17);

        // 4 - Entry Type (0=file, #=Directory?)
        entryTypes[i] = fm.readInt();

        // 4 - Unknown
        // 4 - Unknown
        // 4 - Unknown
        // 4 - Unknown
        // 1 - Unknown
        // 4 - Unknown
        // 4 - Unknown
        // 4 - Unknown
        // 4 - Unknown
        // 4 - null
        // 4 - Unknown (0/2/4)
        // 4 - Filename 3 Offset (relative to the start of the entries directory)
        fm.skip(45);
      }

      // Loop through directory
      int realNumFiles = 0;
      for (int i = 0; i < numFiles; i++) {
        fm.seek(offsets[i]);

        if (entryTypes[i] != 0) {
          continue;
        }

        // X - Filename 1
        // 1 - null Filename 1 Terminator
        String filename = fm.readNullString();
        FieldValidator.checkFilename(filename);

        // X - Filename 2
        // 1 - null Filename 2 Terminator
        fm.readNullString();

        // 4 - Unknown
        // 8 - null
        fm.skip(12);

        // 4 - File Length
        long length = fm.readInt();
        FieldValidator.checkLength(length, arcSize);

        // 4 - File Offset (relative to the start of the entries directory)
        long offset = fm.readInt() + dirOffset;
        FieldValidator.checkOffset(offset, arcSize);

        // 4 - Unknown
        // 8 - null
        // 4 - File Length
        // 4 - File Offset (relative to the start of the entries directory)

        //path,name,offset,length,decompLength,exporter
        resources[realNumFiles] = new Resource(path, filename, offset, length);
        realNumFiles++;

        TaskProgressManager.setValue(i);
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
