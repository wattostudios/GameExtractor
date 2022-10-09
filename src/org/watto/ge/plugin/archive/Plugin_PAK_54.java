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
import org.watto.ge.plugin.exporter.Exporter_ZLib;
import org.watto.io.FileManipulator;
import org.watto.io.converter.ByteConverter;
import org.watto.task.TaskProgressManager;

/**
**********************************************************************************************

**********************************************************************************************
**/
public class Plugin_PAK_54 extends ArchivePlugin {

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public Plugin_PAK_54() {

    super("PAK_54", "PAK_54");

    //         read write replace rename
    setProperties(true, false, false, false);

    setGames("Zuma's Revenge");
    setExtensions("pak"); // MUST BE LOWER CASE
    setPlatforms("PS3");

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

      // 4 - Header
      byte[] headerBytes = fm.readBytes(4);
      if (ByteConverter.unsign(headerBytes[0]) == 192 && ByteConverter.unsign(headerBytes[1]) == 74 && ByteConverter.unsign(headerBytes[2]) == 192 && ByteConverter.unsign(headerBytes[3]) == 186) {
        rating += 50;
      }

      // 5 - null
      byte[] nullBytes = fm.readBytes(5);
      if (nullBytes[0] == 0 && nullBytes[1] == 0 && nullBytes[2] == 0 && nullBytes[3] == 0 && nullBytes[4] == 0) {
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

      ExporterPlugin exporter = Exporter_ZLib.getInstance();

      // RESETTING GLOBAL VARIABLES

      FileManipulator fm = new FileManipulator(path, false);

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
        if (filenameLength == 0) {
          return null;
        }

        // X - Filename
        String filename = fm.readString(filenameLength);

        // 4 - Compressed File Length
        int length = fm.readInt();
        FieldValidator.checkLength(length, arcSize);

        // 4 - Decompressed File Length
        int decompLength = fm.readInt();
        FieldValidator.checkLength(decompLength);

        // 4 - Hash?
        // 4 - Unknown (30110874)
        fm.skip(8);

        if (decompLength == 0) {
          // no compression

          //path,name,offset,length,decompLength,exporter
          resources[realNumFiles] = new Resource(path, filename, 0, length);
        }
        else {
          // ZLib Compression

          //path,name,offset,length,decompLength,exporter
          resources[realNumFiles] = new Resource(path, filename, 0, length, decompLength, exporter);
        }

        realNumFiles++;

        TaskProgressManager.setValue(realNumFiles);

        // 1 - End Of Directory Marker? (0=More Files, 128 = This Was The Last File)
        int eofMarker = fm.readByte();
        if (eofMarker != 0) {
          break;
        }
      }

      resources = resizeResources(resources, realNumFiles);

      numFiles = realNumFiles;

      // resize the buffer
      long fileDataOffset = fm.getOffset();
      fm.seek(1);
      fm.getBuffer().setBufferSize(32);
      fm.seek(fileDataOffset);

      // calculate the offsets and skip over the extra header details
      for (int i = 0; i < numFiles; i++) {
        Resource resource = resources[i];

        // 2 - Extra Header Data Length
        // X - Extra Header Data
        short extraLength = fm.readShort();
        FieldValidator.checkRange(extraLength, 0, 129);
        fm.skip(extraLength);

        // X - File Data
        long offset = fm.getOffset();
        resource.setOffset(offset);

        // ready for the next file
        fm.skip(resource.getLength());
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
