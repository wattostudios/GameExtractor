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
import org.watto.ErrorLogger;
import org.watto.datatype.Resource;
import org.watto.ge.helper.FieldValidator;
import org.watto.ge.plugin.ArchivePlugin;
import org.watto.io.FileManipulator;
import org.watto.io.converter.ByteConverter;
import org.watto.io.converter.ShortConverter;
import org.watto.task.TaskProgressManager;

/**
**********************************************************************************************

**********************************************************************************************
**/
public class Plugin_VOL_VOLN extends ArchivePlugin {

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public Plugin_VOL_VOLN() {

    super("VOL_VOLN", "VOL_VOLN");

    //         read write replace rename
    setProperties(true, false, false, false);

    setExtensions("vol");
    setGames("Earthsiege",
        "Earthsiege 2",
        "Starsiege: Tribes");
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
      if (fm.readString(4).equals("VOLN")) {
        rating += 50;
      }

      return rating;

    }
    catch (Throwable t) {
      return 0;
    }
  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  @Override
  public Resource[] read(File path) {
    try {

      addFileTypes();

      FileManipulator fm = new FileManipulator(path, false);
      long arcSize = fm.getLength();

      // 4 - Header (VOLN)
      // 4 - Flags?
      // 1 - Unknown (5)
      fm.skip(9);

      // 1 - Number of Directories
      int numDirectories = ByteConverter.unsign(fm.readByte());

      // 2 - Directory Listing Length
      fm.skip(2);

      String[] dirNames = new String[numDirectories];
      for (int i = 0; i < numDirectories; i++) {
        // X - Directory Name (including the trailing "\" character)
        // 1 - null Directory Name Terminator
        String directoryName = fm.readNullString();
        FieldValidator.checkFilename(directoryName);
        dirNames[i] = directoryName;
      }

      // 2 - Number of Files
      int numFiles = ShortConverter.unsign(fm.readShort());
      FieldValidator.checkNumFiles(numFiles);

      // 4 - Details Directory Length (not including these 2 header fields)
      fm.skip(4);

      Resource[] resources = new Resource[numFiles];
      TaskProgressManager.setMaximum(numFiles);

      for (int i = 0; i < numFiles; i++) {
        // 13 - Filename (null terminated, filled with nulls)
        String filename = fm.readNullString(12);
        FieldValidator.checkFilename(filename);
        fm.skip(1);

        // 1 - Directory ID Number that this file belongs to
        int dirNameIndex = ByteConverter.unsign(fm.readByte());
        FieldValidator.checkRange(dirNameIndex, 0, numDirectories);

        // 4 - File Offset
        long offset = fm.readInt();
        FieldValidator.checkOffset(offset, arcSize);

        filename = dirNames[dirNameIndex] + filename;

        //path,id,name,offset,length,decompLength,exporter
        resources[i] = new Resource(path, filename, offset);

        TaskProgressManager.setValue(i);
      }

      calculateFileSizes(resources, arcSize);

      fm.getBuffer().setBufferSize(9);

      for (int i = 0; i < numFiles; i++) {
        Resource resource = resources[i];

        fm.seek(resource.getOffset());

        // 1 - Unknown (2)
        if (fm.readByte() == 2) {
          // 4 - File Length
          int length = fm.readInt();
          if (length >= 0 && length < resource.getLength()) {
            resource.setLength(length);
          }

          // 4 - Unknown
          fm.skip(4);

          // X - File Data
          resource.setOffset(fm.getOffset());
        }
        else {
          // don't worry about it - it doesn't have the "2" byte file header
          ErrorLogger.log("VOL_VOLN: Missing the File Header for file " + resource.getFilename() + " at offset " + resource.getOffset());
        }

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