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
import org.watto.ge.plugin.exporter.Exporter_ZLibX;
import org.watto.io.FileManipulator;
import org.watto.task.TaskProgressManager;

/**
**********************************************************************************************

**********************************************************************************************
**/
public class Plugin_CAB_ISC extends ArchivePlugin {

  /**
  **********************************************************************************************

  **********************************************************************************************
  **/
  public Plugin_CAB_ISC() {

    super("CAB_ISC", "InstallShield CAB Archive (with HDR Index) - CAB_ISC");

    //         read write replace rename
    setProperties(true, false, false, false);

    setGames("InstallShield");
    setExtensions("cab"); // MUST BE LOWER CASE
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

      getDirectoryFile(fm.getFile(), "hdr");
      rating += 25;

      // Header
      if (fm.readString(4).equals("ISc(")) {
        rating += 50;
      }

      long arcSize = fm.getLength();

      fm.skip(8);

      // 4 - File Data Offset
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

      ExporterPlugin exporter = Exporter_ZLibX.getInstance();
      //ExporterPlugin exporter = Exporter_ZLibX.getInstance();
      //ExporterPlugin exporter = Exporter_ZLib_CompressedSizeOnly.getInstance();

      // RESETTING GLOBAL VARIABLES

      long arcSize = (int) path.length();

      File sourcePath = getDirectoryFile(path, "hdr");
      FileManipulator fm = new FileManipulator(sourcePath, false);

      long dirSize = sourcePath.length();

      // 4 - File Header ("ISc(")
      // 4 - Unknown (16798209)
      // 4 - null
      fm.skip(12);

      // 4 - Strings Directory Offset (512)
      long dirOffset = fm.readInt();
      FieldValidator.checkOffset(dirOffset, dirSize);

      // 4 - Strings Directory Length
      dirOffset += fm.readInt();
      FieldValidator.checkOffset(dirOffset, dirSize);

      fm.seek(dirOffset + 4);

      // 4 - Number Of Files
      int fileDetailsOffset = fm.readInt();
      int numFiles = (fileDetailsOffset - 4) / 4;
      FieldValidator.checkNumFiles(numFiles);

      fileDetailsOffset += dirOffset;
      fm.seek(fileDetailsOffset);

      Resource[] resources = new Resource[numFiles];
      TaskProgressManager.setMaximum(numFiles);

      int realNumFiles = 0;

      // Loop through directory
      int[] filenameOffsets = new int[numFiles];
      for (int i = 0; i < numFiles; i++) {

        // 4 - Filename Offset (relative to the start of the Files Directory) (or null if not a file)
        int filenameOffset = fm.readInt();
        if (filenameOffset == 0) {
          // not a file - skip it
          fm.skip(54);
          continue;
        }
        filenameOffset += dirOffset;
        FieldValidator.checkOffset(filenameOffset, dirSize);
        filenameOffsets[realNumFiles] = filenameOffset;

        // 4 - null
        // 2 - Entry Type (12=Unknown, 4=File)
        fm.skip(6);

        // 4 - Decompressed File Length (or null if not a file)
        long decompLength = fm.readInt();
        FieldValidator.checkLength(decompLength);

        // 4 - Compressed File Length (or null if not a file)
        long length = fm.readInt();
        FieldValidator.checkLength(length, arcSize);

        // 4 - Unknown (128) (or null if not a file)
        // 4 - Unknown (or null if not a file)
        // 4 - Unknown (or null if not a file)
        // 4 - null
        // 4 - null
        fm.skip(20);

        // 4 - File Data Offset in CAB file (first entry starts at 512, non-file entries retain the previous offset value)
        long offset = fm.readInt();
        FieldValidator.checkOffset(offset, arcSize);

        // 8 - Chechsum? Hash? Time?
        // 8 - Chechsum? Hash? Time?
        fm.skip(16);

        //path,name,offset,length,decompLength,exporter
        resources[realNumFiles] = new Resource(path, "", offset, length, decompLength, exporter);
        realNumFiles++;

        TaskProgressManager.setValue(i);
      }

      // Now loop through the filenames directory
      for (int i = 0; i < realNumFiles; i++) {
        fm.seek(filenameOffsets[i]);

        // X - Filename (null)
        String filename = fm.readNullString();
        FieldValidator.checkFilename(filename);
        resources[i].setName(filename);
      }

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
