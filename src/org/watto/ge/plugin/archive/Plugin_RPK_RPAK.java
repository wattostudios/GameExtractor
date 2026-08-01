/*
 * Application:  Game Extractor
 * Author:       wattostudios
 * Website:      http://www.watto.org
 * Copyright:    Copyright (c) 2002-2026 wattostudios
 *
 * License Information:
 * This program is free software; you can redistribute it and/or modify it under the terms of the GNU General Public License
 * published by the Free Software Foundation; either version 2 of the License, or (at your option) any later versions. This
 * program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranties
 * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License at http://www.gnu.org for more
 * details. For further information on this application, refer to the authors' website.
 */

package org.watto.ge.plugin.archive;

import java.io.File;

import org.watto.datatype.Resource;
import org.watto.ge.helper.FieldValidator;
import org.watto.ge.plugin.ArchivePlugin;
import org.watto.io.FileManipulator;
import org.watto.io.converter.ByteConverter;
import org.watto.task.TaskProgressManager;

/**
**********************************************************************************************

**********************************************************************************************
**/
public class Plugin_RPK_RPAK extends ArchivePlugin {

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public Plugin_RPK_RPAK() {

    super("RPK_RPAK", "RPK_RPAK");

    //         read write replace rename
    setProperties(true, false, false, false);

    setGames("Cross Racing Championship 2005");
    setExtensions("rpk");
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

      // Header
      if (fm.readString(4).equals("RPAK")) {
        rating += 50;
      }

      // Unknown (512)
      if (fm.readInt() == 512) {
        rating += 5;
      }

      // Number Of Source Packs
      if (FieldValidator.checkNumFiles(fm.readInt() + 1)) {
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

      // NOTE - Compressed file MUST know their DECOMPRESSED LENGTH
      //      - Uncompressed files MUST know their LENGTH

      addFileTypes();

      // RESETTING THE GLOBAL VARIABLES

      FileManipulator fm = new FileManipulator(path, false);

      long arcSize = fm.getLength();

      // 4 - Header (RPAK)
      // 4 - Unknown (512)
      fm.skip(8);

      // 4 - Number Of Source Packs
      int numSourcePacks = fm.readInt();
      FieldValidator.checkNumFiles(numSourcePacks + 1);

      // 4 - null
      fm.skip(4);

      // for each source pack
      // 2 - null
      // 2 - Pack ID (incremental from 1)
      // 60 - Pack filename (null)
      fm.skip(64 * numSourcePacks);

      // 4 - Directory Length
      fm.skip(4);

      // 4 - Total Number Of Files
      int numFiles = fm.readInt();
      FieldValidator.checkNumFiles(numFiles);

      // 4 - Number Of Internally-referenced files (files in this archive)
      int numInternalFiles = fm.readInt();
      FieldValidator.checkNumFiles(numInternalFiles + 1); // +1 to allow nulls

      // 4 - Number Of Externally-referenced files (pointers to other archives)
      int numExternalFiles = fm.readInt();
      FieldValidator.checkNumFiles(numExternalFiles + 1); // +1 to allow nulls

      Resource[] resources = new Resource[numFiles];

      TaskProgressManager.setMaximum(numFiles);

      // Loop through internal files directory
      for (int i = 0; i < numInternalFiles; i++) {
        // 2 - Unknown (1/8)
        // 2 - Unknown (0/1)
        // 4 - Unknown
        // 4 - Unknown
        // 2 - Unknown (16256)
        fm.skip(14);

        // 4 - File Offset
        long offset = fm.readInt();
        FieldValidator.checkOffset(offset, arcSize);

        // 4 - File Length
        long headerLength = fm.readInt();
        FieldValidator.checkLength(headerLength, arcSize);

        // 1 - Filename Length (including null)
        int filenameLength = ByteConverter.unsign(fm.readByte()) - 1;
        FieldValidator.checkFilenameLength(filenameLength);

        // X - Filename
        String filename = fm.readString(filenameLength);
        FieldValidator.checkFilename(filename);

        // 1 - null Filename Terminator
        fm.skip(1);

        //path,id,name,offset,length,decompLength,exporter
        resources[i] = new Resource(path, filename, offset);

        TaskProgressManager.setValue(i);
      }

      if (numInternalFiles != 0) {
        // 13 - null
        // 4 - Unknown
        // 4 - Unknown
        // 4 - Unknown
        // 23 - null
        fm.skip(48);
      }

      // Loop through external files directory
      for (int i = numInternalFiles; i < numFiles; i++) {
        // 2 - Unknown (1/8)
        // 2 - Unknown (0/1)
        // 4 - Unknown
        // 4 - Unknown
        // 2 - Unknown (16256)
        fm.skip(14);

        // 4 - File Offset
        long offset = fm.readInt();
        FieldValidator.checkOffset(offset, arcSize);

        // 4 - File Length
        long headerLength = fm.readInt();
        FieldValidator.checkLength(headerLength, arcSize);

        // 1 - Filename Length (including null)
        int filenameLength = ByteConverter.unsign(fm.readByte()) - 1;
        FieldValidator.checkFilenameLength(filenameLength);

        // X - Filename
        String filename = fm.readString(filenameLength) + ".ref";

        // 1 - null Filename Terminator
        fm.skip(1);

        //path,id,name,offset,length,decompLength,exporter
        resources[i] = new Resource(path, filename, offset);

        TaskProgressManager.setValue(i);
      }

      calculateFileSizes(resources, arcSize);

      fm.close();

      return resources;

    }
    catch (Throwable t) {
      logError(t);
      return null;
    }
  }

}
