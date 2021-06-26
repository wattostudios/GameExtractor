/*
 * Application:  Game Extractor
 * Author:       wattostudios
 * Website:      http://www.watto.org
 * Copyright:    Copyright (c) 2002-2020 wattostudios
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
import org.watto.datatype.Archive;
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
public class Plugin_NSZ_NSZ extends ArchivePlugin {

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public Plugin_NSZ_NSZ() {

    super("NSZ_NSZ", "NSZ_NSZ");

    //         read write replace rename
    setProperties(true, false, false, false);

    setGames("Rumble Fighter");
    setExtensions("nsz");
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
      if (fm.readString(4).equals("NSZ!")) {
        rating += 50;
      }

      fm.skip(14);

      long arcSize = fm.getLength();

      // First File Comp Length
      if (FieldValidator.checkLength(fm.readInt(), arcSize)) {
        rating += 5;
      }

      // First File Dec.Comp Length
      if (FieldValidator.checkLength(fm.readInt())) {
        rating += 5;
      }

      // First File Filename Length
      if (FieldValidator.checkFilenameLength(fm.readShort())) {
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

      //ExporterPlugin exporter = new Exporter_Deflate();

      // RESETTING GLOBAL VARIABLES

      FileManipulator fm = new FileManipulator(path, false);

      long arcSize = fm.getLength();

      fm.seek(arcSize - 6);

      // 4 - Directory Offset
      long dirOffset = fm.readInt();
      FieldValidator.checkOffset(dirOffset, arcSize);

      fm.seek(dirOffset);

      int numFiles = Archive.getMaxFiles();

      Resource[] resources = new Resource[numFiles];
      TaskProgressManager.setMaximum(arcSize);

      // Loop through directory
      int realNumFiles = 0;
      while (fm.getOffset() < arcSize) {
        // 4 - Header (jal + (byte)2)
        fm.skip(3);
        if (ByteConverter.unsign(fm.readByte()) != 2) {
          break;
        }

        // 2 - Unknown (2839)
        // 2 - Unknown (20)
        // 2 - Unknown (4)
        // 2 - Hash Length (8)
        // 8 - Hash?
        fm.skip(16);

        // 4 - Compressed File Length
        long length = fm.readInt();
        FieldValidator.checkLength(length, arcSize);

        // 4 - Decompressed File Length
        long decompLength = fm.readInt();
        FieldValidator.checkLength(decompLength);

        // 2 - Filename Length
        short filenameLength = fm.readShort();
        FieldValidator.checkFilenameLength(filenameLength);

        // 2 - Block Length
        short blockLength = fm.readShort();
        FieldValidator.checkLength(blockLength); // check not negative

        // 4 - null
        // 2 - null
        // 2 - Unknown (32)
        // 2 - Unknown (-32330)
        fm.skip(10);

        // 4 - File Offset
        long offset = fm.readInt();
        FieldValidator.checkOffset(offset, arcSize);

        // X - Filename
        String filename = fm.readString(filenameLength);

        // X - Block
        fm.skip(blockLength);

        // skip the file header
        offset += 30 + filenameLength + 142;

        //path,name,offset,length,decompLength,exporter
        resources[realNumFiles] = new Resource(path, filename, offset, length, decompLength);

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
