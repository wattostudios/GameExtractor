/*
 * Application:  Game Extractor
 * Author:       wattostudios
 * Website:      http://www.watto.org
 * Copyright:    Copyright (c) 2002-2022 wattostudios
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
import org.watto.datatype.FileType;
import org.watto.datatype.Resource;
import org.watto.ge.helper.FieldValidator;
import org.watto.ge.plugin.ArchivePlugin;
import org.watto.ge.plugin.ExporterPlugin;
import org.watto.ge.plugin.exporter.Exporter_Deflate;
import org.watto.io.FileManipulator;
import org.watto.task.TaskProgressManager;

/**
**********************************************************************************************

**********************************************************************************************
**/
public class Plugin_DATA_2 extends ArchivePlugin {

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public Plugin_DATA_2() {

    super("DATA_2", "DATA_2");

    //         read write replace rename
    setProperties(true, false, false, false);

    setGames("Shantae and the Pirates Curse");
    setExtensions("data"); // MUST BE LOWER CASE
    setPlatforms("PC");

    // MUST BE LOWER CASE !!!
    setFileTypes(new FileType("vol", "VOL Archive", FileType.TYPE_ARCHIVE));

    //setTextPreviewExtensions("colours", "rat", "screen", "styles"); // LOWER CASE

    //setCanScanForFileTypes(true);

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
      if (fm.readInt() == 418590482) {
        rating += 50;
      }

      fm.skip(8);

      // 4 - Unknown Header Directory Offset (48)
      if (fm.readInt() == 48) {
        rating += 5;
      }

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

      ExporterPlugin exporter = Exporter_Deflate.getInstance();

      // RESETTING GLOBAL VARIABLES

      FileManipulator fm = new FileManipulator(path, false);

      long arcSize = fm.getLength();

      // 4 - Unknown
      // 4 - Unknown
      // 4 - Unknown
      // 4 - Unknown Header Directory Offset (48)
      fm.skip(16);

      // 4 - Number of Files
      int numFiles = fm.readInt();
      FieldValidator.checkNumFiles(numFiles);

      // 4 - Filename Offsets Directory Offset
      int filenameOffsetDirOffset = fm.readInt();
      FieldValidator.checkOffset(filenameOffsetDirOffset, arcSize);

      // 4 - Hash Directory Offset
      fm.skip(4);

      // 4 - Compression Directory Offset
      int compressionDirOffset = fm.readInt();
      FieldValidator.checkOffset(compressionDirOffset, arcSize);

      // 4 - Compressed File Lengths Directory Offset
      int lengthDirOffset = fm.readInt();
      FieldValidator.checkOffset(lengthDirOffset, arcSize);

      // 4 - Decompressed File Lengths Directory Offset
      int decompLengthDirOffset = fm.readInt();
      FieldValidator.checkOffset(decompLengthDirOffset, arcSize);

      // 4 - File Offsets Directory Offset
      int offsetDirOffset = fm.readInt();
      FieldValidator.checkOffset(offsetDirOffset, arcSize);

      // 4 - File Data Offset

      Resource[] resources = new Resource[numFiles];
      TaskProgressManager.setMaximum(numFiles);

      fm.relativeSeek(filenameOffsetDirOffset);

      int[] filenameOffsets = new int[numFiles];

      // Loop through directory
      for (int i = 0; i < numFiles; i++) {
        // 4 - Filename Offset (absolute)
        int filenameOffset = fm.readInt();
        FieldValidator.checkOffset(filenameOffset, arcSize);
        filenameOffsets[i] = filenameOffset;

        TaskProgressManager.setValue(i);
      }

      String[] filenames = new String[numFiles];

      //Loop through directory
      for (int i = 0; i < numFiles; i++) {
        fm.relativeSeek(filenameOffsets[i]);

        // X - Filename
        // 1 - null Filename Terminator
        String filename = fm.readNullString();
        FieldValidator.checkFilename(filename);
        filenames[i] = filename;

        TaskProgressManager.setValue(i);
      }

      fm.relativeSeek(compressionDirOffset);

      byte[] compressions = new byte[numFiles];

      // Loop through directory
      for (int i = 0; i < numFiles; i++) {
        // 1 - Compression Flag (1=compressed, 0=uncompressed)
        byte compression = fm.readByte();
        compressions[i] = compression;

        TaskProgressManager.setValue(i);
      }

      fm.relativeSeek(lengthDirOffset);

      long[] lengths = new long[numFiles];

      // Loop through directory
      for (int i = 0; i < numFiles; i++) {
        // 8 - Compressed File Length
        long length = fm.readLong();
        FieldValidator.checkLength(length, arcSize);
        lengths[i] = length;

        TaskProgressManager.setValue(i);
      }

      fm.relativeSeek(decompLengthDirOffset);

      long[] decompLengths = new long[numFiles];

      // Loop through directory
      for (int i = 0; i < numFiles; i++) {
        // 8 - Decompressed File Length
        long decompLength = fm.readLong();
        FieldValidator.checkLength(decompLength, arcSize);
        decompLengths[i] = decompLength;

        TaskProgressManager.setValue(i);
      }

      fm.relativeSeek(offsetDirOffset);

      // Loop through directory
      for (int i = 0; i < numFiles; i++) {
        // 8 - File Offset
        long offset = fm.readLong();
        FieldValidator.checkOffset(offset, arcSize);

        String filename = filenames[i];

        long length = lengths[i];
        long decompLength = decompLengths[i];

        if (compressions[i] == 1) {
          // compressed
          //path,name,offset,length,decompLength,exporter
          resources[i] = new Resource(path, filename, offset, length, decompLength, exporter);
        }
        else {
          // uncompressed
          //path,name,offset,length,decompLength,exporter
          resources[i] = new Resource(path, filename, offset, decompLength);
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

  /**
  **********************************************************************************************
  If an archive doesn't have filenames stored in it, the scanner can come here to try to work out
  what kind of file a Resource is. This method allows the plugin to provide additional plugin-specific
  extensions, which will be tried before any standard extensions.
  @return null if no extension can be determined, or the extension if one can be found
  **********************************************************************************************
  **/
  @Override
  public String guessFileExtension(Resource resource, byte[] headerBytes, int headerInt1, int headerInt2, int headerInt3, short headerShort1, short headerShort2, short headerShort3, short headerShort4, short headerShort5, short headerShort6) {

    /*
    if (headerInt1 == 2037149520) {
      return "js";
    }
    */

    return null;
  }

}
