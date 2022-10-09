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
import org.watto.ge.plugin.ExporterPlugin;
import org.watto.ge.plugin.exporter.Exporter_Default;
import org.watto.ge.plugin.exporter.Exporter_ZLib;
import org.watto.io.FileManipulator;
import org.watto.io.FilenameSplitter;
import org.watto.io.converter.IntConverter;
import org.watto.task.TaskProgressManager;

/**
**********************************************************************************************

**********************************************************************************************
**/
public class Plugin_UCAS extends ArchivePlugin {

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public Plugin_UCAS() {

    super("UCAS", "UCAS");

    //         read write replace rename
    setProperties(true, false, false, false);

    setGames("Unreal Engine 4",
        "Godfall",
        "Splitgate");
    setExtensions("ucas", "utoc"); // MUST BE LOWER CASE
    setPlatforms("PC");

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

      String extension = FilenameSplitter.getExtension(fm.getFile()).toLowerCase();
      if (extension.equals("ucas")) {
        getDirectoryFile(fm.getFile(), "utoc");
        rating += 25;
      }
      else if (extension.equals("utoc")) {
        getDirectoryFile(fm.getFile(), "ucas");
        rating += 25;
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

      ExporterPlugin exporterDefault = Exporter_Default.getInstance();

      // RESETTING GLOBAL VARIABLES

      File sourcePath = path;

      String extension = FilenameSplitter.getExtension(path).toLowerCase();
      if (extension.equals("ucas")) {
        sourcePath = getDirectoryFile(path, "utoc");
      }
      else if (extension.equals("utoc")) {
        path = getDirectoryFile(path, "ucas");
      }

      long arcSize = path.length();

      FileManipulator fm = new FileManipulator(sourcePath, false);

      // 16 - Unknown
      // 4 - Unknown (3)
      fm.skip(20);

      // 4 - Directory 1 Offset (144)
      int dir1Offset = fm.readInt();
      FieldValidator.checkOffset(dir1Offset, arcSize);

      // 4 - Number of Entries in Directory 1, 2, and 4
      int numFiles124 = fm.readInt();
      FieldValidator.checkNumFiles(numFiles124);

      // 4 - Number of Entries in Directory 3
      int numFiles = fm.readInt();
      FieldValidator.checkNumFiles(numFiles / 10);

      // 4 - Unknown (12)
      fm.skip(4);

      // 4 - Number of Compression Formats (0/1)
      int numCompressions = fm.readInt();
      FieldValidator.checkRange(numCompressions, 0, 5);

      // 4 - Unknown (32)
      // 4 - Unknown
      // 4 - Unknown
      // 4 - Unknown (1)
      // 8 - Unknown
      // 16 - null
      // 8 - Unknown (15)
      // 8 - Unknown (-1)
      // 48 - null

      int dir3Offset = dir1Offset + numFiles124 * 12 + numFiles124 * 10;
      FieldValidator.checkOffset(dir3Offset, arcSize);

      int compressionDirOffset = dir3Offset + numFiles * 12;
      FieldValidator.checkOffset(compressionDirOffset, arcSize);

      fm.seek(compressionDirOffset);

      ExporterPlugin[] exporters = new ExporterPlugin[numCompressions + 1];
      exporters[0] = exporterDefault;

      for (int i = 0; i < numCompressions; i++) {
        // 32 - Compression Format ("Oodle" + nulls to fill)
        String compression = fm.readNullString(32);
        if (compression.equalsIgnoreCase("Oodle")) {
          exporters[i + 1] = new Exporter_Default();
          exporters[i + 1].setName("Oodle Compression");
          //exporters[i + 1] = new Exporter_QuickBMS_Decompression("oodle");
          //exporters[i + 1] = new Exporter_Encryption_AES(ByteArrayConverter.convertLittle(new Hex("D73A797940208F2FB29256BE81A7CBC7B74CBF899441BB277F357F7F4577DBBB")));
        }
        else if (compression.equalsIgnoreCase("Zlib")) {
          exporters[i + 1] = Exporter_ZLib.getInstance();
        }
        else {
          exporters[i + 1] = exporterDefault;
        }
      }

      fm.seek(dir3Offset);

      Resource[] resources = new Resource[numFiles];
      TaskProgressManager.setMaximum(numFiles);

      // Loop through directory
      //long multipleFactor = IntConverter.unsign((long)Integer.MAX_VALUE);
      long multipleFactor = ((long) 1) << 32;
      for (int i = 0; i < numFiles; i++) {
        // 4 - File Offset (into the UCAS file)
        long offset = IntConverter.unsign(fm.readInt());

        // 1 - Multiple of 4GB
        int multiple = fm.readByte();
        if (multiple != 0) {
          offset += (multiple * multipleFactor);
        }

        FieldValidator.checkOffset(offset, arcSize);

        // 3 - Compressed File Length
        byte[] lengthBytes = new byte[] { fm.readByte(), fm.readByte(), fm.readByte(), 0 };
        int length = IntConverter.convertLittle(lengthBytes);
        FieldValidator.checkLength(length, arcSize);

        // 2 - Decompressed File Length?
        // 1 - Unknown (0/5/16)
        byte[] decompLengthBytes = new byte[] { fm.readByte(), fm.readByte(), fm.readByte(), 0 };
        int decompLength = IntConverter.convertLittle(decompLengthBytes);
        FieldValidator.checkLength(decompLength);

        if (decompLength == 0) {
          decompLength = length;
        }

        // 1 - Compression Flag (0/1)
        int compressionFlag = fm.readByte();
        FieldValidator.checkRange(compressionFlag, 0, numCompressions);

        ExporterPlugin exporter = exporters[compressionFlag];

        String filename = Resource.generateFilename(i);

        //path,name,offset,length,decompLength,exporter
        Resource resource = new Resource(path, filename, offset, length, decompLength, exporter);
        resource.forceNotAdded(true);
        resources[i] = resource;

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
