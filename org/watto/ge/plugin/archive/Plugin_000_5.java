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
import org.watto.task.TaskProgressManager;

/**
**********************************************************************************************

**********************************************************************************************
**/
public class Plugin_000_5 extends ArchivePlugin {

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public Plugin_000_5() {

    super("000_5", "000_5");

    //         read write replace rename
    setProperties(true, false, false, false);

    setGames("Ice Age 2: The Meltdown",
        "Pirates Of The Caribbean: At Worlds End");
    setExtensions("000");
    setPlatforms("PC");

    //setFileTypes("","",
    //             "",""
    //             );

    setTextPreviewExtensions("h"); // LOWER CASE

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

      getDirectoryFile(fm.getFile(), "bin");
      rating += 25;

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

      long arcSize = (int) path.length();

      File sourcePath = getDirectoryFile(path, "bin");
      FileManipulator fm = new FileManipulator(sourcePath, false);

      // 4 - Unknown (7)
      // 4 - Length of BIN file
      fm.skip(8);

      // 4 - Number Of Files
      int numFiles = fm.readInt();
      FieldValidator.checkNumFiles(numFiles);

      // 4 - Unknown (1)
      // 4 - Directory Length (including this field)
      fm.skip(8);

      Resource[] resources = new Resource[numFiles];
      TaskProgressManager.setMaximum(numFiles);

      // Loop through directory
      for (int i = 0; i < numFiles; i++) {
        // 4 - File Length
        long length = fm.readInt();
        FieldValidator.checkLength(length, arcSize);

        // 2 - Unknown
        // 2 - Unknown
        // 4 - Unknown (6)
        // 4 - null
        // 4 - Unknown (1)
        fm.skip(16);

        // 4 - File Offset
        long offset = fm.readInt();
        FieldValidator.checkOffset(offset, arcSize);

        // 4 - null
        fm.skip(4);

        String filename = Resource.generateFilename(i);

        //path,name,offset,length,decompLength,exporter
        resources[i] = new Resource(path, filename, offset, length);

        TaskProgressManager.setValue(i);
      }

      // read the filename offset directory

      int[] nameOffsets = new int[numFiles];
      for (int i = 0; i < numFiles; i++) {
        // 4 - Filename Offset (relative to the start of this field)
        int nameOffset = (int) (fm.readInt() + fm.getOffset() - 4);
        FieldValidator.checkOffset(nameOffset, arcSize);
        nameOffsets[i] = nameOffset;
      }

      // reach and decrypt the filenames
      boolean encrypted = false;
      for (int i = 0; i < numFiles; i++) {
        int offset = nameOffsets[i];
        fm.seek(offset);

        String filename = "";

        if (i == 0) {
          int check = fm.readByte();
          if (check != 0 && check != 120) {
            encrypted = true;
          }
          else {
            encrypted = false;
          }
          fm.seek(offset);
        }

        int j = 0;
        int character = -1;
        do {
          character = ByteConverter.unsign(fm.readByte());

          if (encrypted) {
            int CL = 0x16 - i - j; // 22
            character += CL;
            character &= 0xff;
          }

          if (character != 0) {
            filename += (char) (byte) character;
          }

          j++;
        }
        while (character != 0);

        int slashPos = filename.indexOf(":\\");
        if (slashPos > 0) {
          filename = filename.substring(slashPos + 2);
        }

        Resource resource = resources[i];
        resource.setName(filename);
        resource.setOriginalName(filename);
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
