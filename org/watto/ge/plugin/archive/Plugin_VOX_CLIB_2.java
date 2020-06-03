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
import org.watto.datatype.Resource;
import org.watto.ge.helper.FieldValidator;
import org.watto.ge.plugin.ArchivePlugin;
import org.watto.io.FileManipulator;
import org.watto.io.converter.ByteConverter;
import org.watto.io.converter.StringConverter;
import org.watto.task.TaskProgressManager;

/**
**********************************************************************************************

**********************************************************************************************
**/
public class Plugin_VOX_CLIB_2 extends ArchivePlugin {

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public Plugin_VOX_CLIB_2() {

    super("VOX_CLIB_2", "VOX_CLIB_2");

    //         read write replace rename
    setProperties(true, false, false, false);

    setGames("Kathy Rain",
        "Kings Quest 1: Quest for the Crown",
        "Kings Quest 2: Romancing the Stones",
        "Quest For Glory 2",
        "Reality Falls",
        "Symploke");
    setExtensions("vox"); // MUST BE LOWER CASE
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
      if (fm.readString(4).equals("CLIB")) {
        rating += 50;
      }

      fm.skip(4);

      // Number Of Files
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

      // 4 - Header (CLIB)
      // 4 - Unknown
      fm.skip(8);

      // 4 - Number Of Files
      int numFiles = fm.readInt();
      FieldValidator.checkNumFiles(numFiles);

      // 11 - null
      fm.skip(11);

      Resource[] resources = new Resource[numFiles];
      TaskProgressManager.setMaximum(numFiles);

      String[] filenames = new String[numFiles];

      // Loop through filenames directory
      for (int i = 0; i < numFiles; i++) {
        // 13 - Filename [subtract 20 from each byte, except for null bytes] (null terminated)
        byte[] filenameBytes = fm.readBytes(13);

        int filenameLength = 13;
        for (int b = 0; b < 13; b++) {
          if (filenameBytes[b] == 0) {
            // found the end of the filename
            filenameLength = b;
            break;
          }
          else {
            // decrypt the byte
            filenameBytes[b] = (byte) (ByteConverter.unsign(filenameBytes[b]) - 20);
          }

        }

        if (filenameLength <= 0) {
          filenames[i] = Resource.generateFilename(i);
        }
        else {
          byte[] oldFilenameBytes = filenameBytes;
          filenameBytes = new byte[filenameLength];
          System.arraycopy(oldFilenameBytes, 0, filenameBytes, 0, filenameLength);
          filenames[i] = StringConverter.convertLittle(filenameBytes);
        }

        TaskProgressManager.setValue(i);
      }

      long offset = fm.getOffset() + (numFiles * 4) + (numFiles * 2);

      // Loop through directory
      for (int i = 0; i < numFiles; i++) {

        // 4 - File Length
        int length = fm.readInt();
        FieldValidator.checkLength(length, arcSize);

        String filename = filenames[i];

        //path,name,offset,length,decompLength,exporter
        resources[i] = new Resource(path, filename, offset, length);

        TaskProgressManager.setValue(i);
        offset += length;
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
