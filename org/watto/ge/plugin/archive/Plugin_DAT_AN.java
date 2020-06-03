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
import org.watto.task.TaskProgressManager;
import org.watto.datatype.Resource;
import org.watto.ge.helper.FieldValidator;
import org.watto.ge.plugin.ArchivePlugin;
import org.watto.io.FileManipulator;
import org.watto.io.converter.ByteConverter;

/**
**********************************************************************************************

**********************************************************************************************
**/
public class Plugin_DAT_AN extends ArchivePlugin {

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public Plugin_DAT_AN() {

    super("DAT_AN", "Lineage Eternal DAT Archive");

    //         read write replace rename
    setProperties(true, false, false, false);

    setGames("Lineage Eternal");
    setExtensions("dat"); // MUST BE LOWER CASE
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

      // Header
      int headerByte1 = ByteConverter.unsign(fm.readByte());
      int headerByte2 = ByteConverter.unsign(fm.readByte());
      int headerByte3 = ByteConverter.unsign(fm.readByte());
      int headerByte4 = ByteConverter.unsign(fm.readByte());
      if (headerByte1 == 152 && headerByte2 == 65 && headerByte3 == 78 && headerByte4 == 26) {
        rating += 50;
      }

      // 4 - Header Length (40)
      if (FieldValidator.checkEquals(fm.readInt(), 40)) {
        rating += 5;
      }

      fm.skip(4);

      // 4 - Directory 1 Offset (512)
      if (FieldValidator.checkEquals(fm.readInt(), 512)) {
        rating += 5;
      }

      // 4 - Unknown (MDFO)
      if (fm.readString(4).equals("MDFO")) {
        rating += 5;
      }

      fm.skip(12);

      long arcSize = fm.getLength();

      // File Directory Length
      if (FieldValidator.checkLength(fm.readInt(), arcSize)) {
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

      // 4 - Header? (152,65,78,26)
      // 4 - Header Length (40)
      // 2 - Unknown (2)
      // 2 - Unknown
      // 4 - Directory 1 Offset (512)
      // 4 - Unknown (MDFO)
      // 4 - Unknown
      // 4 - Unknown
      // 4 - Unknown (MDFO)
      fm.skip(32);

      // 4 - File Directory Length
      int dirLength = fm.readInt();
      FieldValidator.checkLength(dirLength, arcSize);

      // skip over DIRECTORY 1
      fm.seek(1024);

      // 4 - Directory Header? (Mft*)
      // 4 - Unknown
      // 4 - null
      fm.skip(12);

      // 4 - Number Of Files
      int numFiles = fm.readInt();
      FieldValidator.checkNumFiles(numFiles);

      // 4 - null
      // 4 - null
      fm.skip(8);

      Resource[] resources = new Resource[numFiles];
      TaskProgressManager.setMaximum(numFiles);

      int relOffset = 1048 + dirLength; // length of directory + 512 for ArcHeader + 512 for Directory1 + 24 for FileDirectoryHeader

      // Loop through directory
      int realNumFiles = 0;
      for (int i = 0; i < numFiles; i++) {

        // 4 - File Offset? (relative to the end of the File Directory)
        int offset = fm.readInt() + relOffset;
        FieldValidator.checkOffset(offset, arcSize);

        // 4 - null
        fm.skip(4);

        // 4 - File Length
        int length = fm.readInt();
        FieldValidator.checkLength(length, arcSize);

        // 2 - Unknown (8/0)
        // 2 - Unknown (3)
        // 4 - null
        // 4 - Unknown (File Type Hash?)
        fm.skip(12);

        if (offset == relOffset && length == 0) {
          // an empty file
          continue;
        }

        String filename = Resource.generateFilename(realNumFiles);

        //path,name,offset,length,decompLength,exporter
        resources[realNumFiles] = new Resource(path, filename, offset, length);

        TaskProgressManager.setValue(i);
        realNumFiles++;
      }

      // remove all the blank entries
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
