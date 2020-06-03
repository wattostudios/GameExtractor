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
import org.watto.ge.plugin.ExporterPlugin;
import org.watto.ge.plugin.exporter.Exporter_XOR;
import org.watto.io.FileManipulator;
import org.watto.io.buffer.XORBufferWrapper;
import org.watto.io.converter.ByteConverter;
import org.watto.task.TaskProgressManager;

/**
**********************************************************************************************

**********************************************************************************************
**/
public class Plugin_PAK_41 extends ArchivePlugin {

  /**
  **********************************************************************************************

  **********************************************************************************************
  **/
  public Plugin_PAK_41() {

    super("PAK_41", "PopCap PAK Archive (PAK_41)");

    //         read write replace rename
    setProperties(true, false, false, false);

    setGames("Bejeweled 3",
        "Beyond the Invisible: Darkness Came",
        "Cooking Academy",
        "Cooking Academy 2",
        "Cooking Academy 3",
        "Peggle Deluxe",
        "Plants vs. Zombies",
        "Zuma's Revenge");
    setExtensions("pak"); // MUST BE LOWER CASE
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

      // 4 - Unknown
      fm.skip(4);

      // 5 - null (XOR with 247)
      if (fm.readByte() == -9 && fm.readByte() == -9 && fm.readByte() == -9 && fm.readByte() == -9) {
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
   * Reads an [archive] File into the Resources
   **********************************************************************************************
   **/
  @Override
  public Resource[] read(File path) {
    try {

      // NOTE - Compressed files MUST know their DECOMPRESSED LENGTH
      //      - Uncompressed files MUST know their LENGTH

      addFileTypes();

      ExporterPlugin exporter = new Exporter_XOR(247);

      // RESETTING GLOBAL VARIABLES

      FileManipulator fm = new FileManipulator(path, false);
      fm.setBuffer(new XORBufferWrapper(fm.getBuffer(), 247));

      long arcSize = fm.getLength();

      // 4 - Unknown
      // 5 - null
      fm.skip(9);

      int numFiles = Archive.getMaxFiles();

      Resource[] resources = new Resource[numFiles];
      TaskProgressManager.setMaximum(numFiles);

      // Loop through directory
      int realNumFiles = 0;
      while (fm.getOffset() < arcSize) {

        // 1 - Filename Length
        int filenameLength = ByteConverter.unsign(fm.readByte());

        // X - Filename
        String filename = fm.readString(filenameLength);

        // 4 - File Length
        int length = fm.readInt();
        FieldValidator.checkLength(length, arcSize);

        // 4 - Hash?
        // 4 - Unknown (30110874)
        fm.skip(8);

        //path,name,offset,length,decompLength,exporter
        resources[realNumFiles] = new Resource(path, filename, 0, length, length, exporter);
        realNumFiles++;

        TaskProgressManager.setValue(realNumFiles);

        // 1 - End Of Directory Marker? (0=More Files, 128 = This Was The Last File)
        int eofMarker = fm.readByte();
        if (eofMarker != 0) {
          break;
        }
      }

      resources = resizeResources(resources, realNumFiles);

      // calculate the offsets
      long offset = fm.getOffset();
      for (int i = 0; i < realNumFiles; i++) {
        Resource resource = resources[i];

        resource.setOffset(offset);
        offset += resource.getLength();
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
