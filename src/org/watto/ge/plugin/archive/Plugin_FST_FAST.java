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
import org.watto.ge.plugin.ExporterPlugin;
import org.watto.ge.plugin.exporter.Exporter_ZLib_CompressedSizeOnly;
import org.watto.io.FileManipulator;
import org.watto.io.converter.ByteConverter;
import org.watto.task.TaskProgressManager;

/**
**********************************************************************************************

**********************************************************************************************
**/
public class Plugin_FST_FAST extends ArchivePlugin {

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public Plugin_FST_FAST() {

    super("FST_FAST", "FST_FAST");

    //         read write replace rename
    setProperties(true, false, false, false);

    setGames("Crazy Machines: Elements",
        "Crazy Machines: Golden Gears");
    setExtensions("fst"); // MUST BE LOWER CASE
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

      // Header
      if (fm.readString(4).equals("FAST")) {
        rating += 50;
      }

      int version1 = fm.readShort();
      int version2 = fm.readShort();

      if (version1 == 1 && version2 == 1) {
        rating += 5;
      }

      fm.skip(8);

      // Number Of Files
      if (FieldValidator.checkNumFiles(fm.readInt())) {
        rating += 5;
      }

      long arcSize = fm.getLength();

      // Directory Offset
      if (FieldValidator.checkOffset(fm.readLong(), arcSize)) {
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

      ExporterPlugin exporter = Exporter_ZLib_CompressedSizeOnly.getInstance();

      // RESETTING GLOBAL VARIABLES

      FileManipulator fm = new FileManipulator(path, false);

      long arcSize = fm.getLength();

      // 4 - Header (FAST)
      // 2 - Unknown (1)
      // 2 - Unknown (1)
      // 4 - Unknown
      // 4 - Unknown
      fm.skip(16);

      // 4 - Number of Files?
      int numFiles = fm.readInt();
      FieldValidator.checkNumFiles(numFiles);

      // 8 - Directory Offset
      int dirOffset = fm.readInt();
      FieldValidator.checkOffset(dirOffset, arcSize);

      // 4 - Format 1 Length
      // X - Format 1 (FCodecZip or FCodecRaw)
      // 4 - Format 2 Length
      // X - Format 2 (FCodecZip or FCodecRaw)
      // 1 - Filename Length
      // X - Filename (not including extension)
      fm.seek(dirOffset);

      Resource[] resources = new Resource[numFiles];
      TaskProgressManager.setMaximum(numFiles);

      // Loop through directory
      for (int i = 0; i < numFiles; i++) {
        // 4 - Filename Length
        int filenameLength = fm.readInt();
        FieldValidator.checkFilenameLength(filenameLength);

        // X - Filename
        String filename = fm.readString(filenameLength);

        // 4 - Directory Name Length
        int dirNameLength = fm.readInt();
        FieldValidator.checkFilenameLength(dirNameLength);

        // X - Directory Name
        String dirName = fm.readString(dirNameLength);

        // 1 - File Type Length
        int typeNameLength = ByteConverter.unsign(fm.readByte());

        // X - File Type (eg FText)
        String fileType = fm.readString(typeNameLength);

        // 8 - File Offset
        long offset = fm.readLong();
        FieldValidator.checkOffset(offset, arcSize);

        // 8 - Compressed File Length
        long length = fm.readLong();
        FieldValidator.checkLength(length, arcSize);

        // 8 - File Group ID?
        int groupID = fm.readInt();
        fm.skip(4);

        filename = dirName + "\\" + filename + "(" + groupID + ")." + fileType;

        //path,name,offset,length,decompLength,exporter
        resources[i] = new Resource(path, filename, offset, length);

        TaskProgressManager.setValue(i);
      }

      // Check for compression
      fm.getBuffer().setBufferSize(1);
      // Loop through directory
      for (int i = 0; i < numFiles; i++) {
        Resource resource = resources[i];
        fm.seek(resource.getOffset());
        if (fm.readByte() == 120) { // ZLib Compression
          resource.setExporter(exporter);
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
