/*
 * Application:  Game Extractor
 * Author:       wattostudios
 * Website:      http://www.watto.org
 * Copyright:    Copyright (c) 2002-2021 wattostudios
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
import org.watto.io.converter.IntConverter;
import org.watto.task.TaskProgressManager;

/**
**********************************************************************************************

**********************************************************************************************
**/
public class Plugin_RES_LG extends ArchivePlugin {

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public Plugin_RES_LG() {

    super("RES_LG", "RES_LG");

    //         read write replace rename
    setProperties(true, false, false, false);

    setGames("System Shock",
        "System Shock: Enhanced Edition");
    setExtensions("res");
    setPlatforms("PC");

    setFileTypes("2", "Image",
        "7", "Creative Voice File",
        "17", "Movie");

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
      if (fm.readString(14).equals("LG Res File v2")) {
        rating += 50;
      }

      // Unknown (26)
      if (fm.readInt() == 1706509) {
        rating += 5;
      }

      fm.skip(106);

      long arcSize = fm.getLength();

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

      // 16 - Header (LG Res File v2 + (byte)13,10)
      // 4 - Unknown (26)
      // 104 - null
      fm.skip(124);

      // 4 - Directory Offset
      int dirOffset = fm.readInt();
      FieldValidator.checkOffset(dirOffset, arcSize);

      fm.seek(dirOffset);

      // 2 - Number Of Files
      short numFiles = fm.readShort();
      FieldValidator.checkNumFiles(numFiles);

      // 4 - First File Offset
      int offset = fm.readInt();
      FieldValidator.checkOffset(offset);

      Resource[] resources = new Resource[numFiles];
      TaskProgressManager.setMaximum(numFiles);

      // Loop through directory
      for (int i = 0; i < numFiles; i++) {
        // 2 - File ID (incremental)
        fm.skip(2);

        // 4 - File Length
        byte[] decompLengthBytes = fm.readBytes(4);
        int compressionType = ByteConverter.unsign(decompLengthBytes[3]);
        decompLengthBytes[3] = 0;
        int decompLength = IntConverter.convertLittle(decompLengthBytes);
        FieldValidator.checkLength(decompLength);

        // 3 - File Length
        // 1 - File Type ID? (17=Movie, 7=Creative Voice File, 2=Image)
        byte[] lengthBytes = fm.readBytes(4);
        int fileType = ByteConverter.unsign(lengthBytes[3]);
        lengthBytes[3] = 0;
        int length = IntConverter.convertLittle(lengthBytes);
        FieldValidator.checkLength(length, arcSize);

        String extension;
        if (fileType == 0) {
          extension = ".palette";
        }
        else if (fileType == 1) {
          extension = ".text";
        }
        else if (fileType == 2) {
          extension = ".image";
        }
        else if (fileType == 3) {
          extension = ".font";
        }
        else if (fileType == 4) {
          extension = ".video";
        }
        else if (fileType == 7) {
          extension = ".voc";
        }
        else if (fileType == 15) {
          extension = ".model";
        }
        else if (fileType == 17) {
          extension = ".movie";
        }
        else if (fileType == 48) {
          extension = ".map";
        }
        else {
          extension = "." + fileType;
        }

        String filename = Resource.generateFilename(i) + extension;

        FieldValidator.checkOffset(offset, arcSize);

        if (compressionType == 1 || compressionType == 3) {
          // compressed
          //path,name,offset,length,decompLength,exporter
          resources[i] = new Resource(path, filename, offset, length, decompLength);
        }
        else {
          // uncompressed
          //path,name,offset,length,decompLength,exporter
          resources[i] = new Resource(path, filename, offset, length);
        }

        TaskProgressManager.setValue(i);

        offset += length;

        offset += calculatePadding(offset, 4);
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
