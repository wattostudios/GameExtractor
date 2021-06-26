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
import org.watto.io.converter.IntConverter;
import org.watto.task.TaskProgressManager;

/**
**********************************************************************************************

**********************************************************************************************
**/
public class Plugin_ALD extends ArchivePlugin {

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public Plugin_ALD() {

    super("ALD", "ALD");

    //         read write replace rename
    setProperties(true, false, false, false);

    setGames("War of the Human Tanks");
    setExtensions("ald"); // MUST BE LOWER CASE
    setPlatforms("PC");

    // MUST BE LOWER CASE !!!
    //setFileTypes(new FileType("txt", "Text Document", FileType.TYPE_DOCUMENT),
    //             new FileType("bmp", "Bitmap Image", FileType.TYPE_IMAGE)
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

      if (fm.readShort() == 1) {
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

      // 3 - Unknown (1)
      fm.skip(3);

      int numFiles = Archive.getMaxFiles();

      int[] offsets = new int[numFiles];
      int realNumFiles = 0;
      while (fm.getOffset() < arcSize) {
        // 3 - File Offset
        byte[] readBytes = fm.readBytes(3);
        byte[] offsetBytes = new byte[] { 0, readBytes[0], readBytes[1], readBytes[2] };
        int offset = IntConverter.convertLittle(offsetBytes);
        //System.out.println(offsetBytes[0] + "\t" + offsetBytes[1] + "\t" + offsetBytes[2] + "\t" + offsetBytes[3] + "\t" + offset);
        if (offset == 0) {
          break;
        }
        offsets[realNumFiles] = offset;
        realNumFiles++;
      }

      numFiles = realNumFiles - 1; // the last file is the end of the archive

      fm.getBuffer().setBufferSize(32);

      Resource[] resources = new Resource[numFiles];
      TaskProgressManager.setMaximum(numFiles);

      // Loop through directory
      for (int i = 0; i < numFiles; i++) {
        fm.seek(offsets[i]);
        //System.out.println(offsets[i]);

        // 4 - Header Length (32)
        fm.skip(4);

        // 4 - File Length [+32]
        int length = fm.readInt();
        FieldValidator.checkLength(length, arcSize);

        // 8 - CRC?
        fm.skip(8);

        // 16 - Filename (null terminated, filled with nulls)
        String filename = fm.readNullString(16);
        //System.out.println(filename);
        FieldValidator.checkFilename(filename);

        // X - File Data
        long offset = fm.getOffset();
        fm.skip(length);

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
