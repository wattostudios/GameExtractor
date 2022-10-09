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
import org.watto.task.TaskProgressManager;

/**
**********************************************************************************************

**********************************************************************************************
**/
public class Plugin_MSSCMP_KNAB extends ArchivePlugin {

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public Plugin_MSSCMP_KNAB() {

    super("MSSCMP_KNAB", "MSSCMP_KNAB");

    //         read write replace rename
    setProperties(true, false, false, false);

    setGames("Galactic Civilizations 3");
    setExtensions("msscmp"); // MUST BE LOWER CASE
    setPlatforms("PC");

    // MUST BE LOWER CASE !!!
    //setFileTypes(new FileType("txt", "Text Document", FileType.TYPE_DOCUMENT),
    //             new FileType("bmp", "Bitmap Image", FileType.TYPE_IMAGE)
    //             );

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
      if (fm.readString(4).equals("KNAB")) {
        rating += 50;
      }

      fm.skip(4);

      long arcSize = fm.getLength();

      // 8 - File Data Offset
      if (FieldValidator.checkOffset(fm.readLong(), arcSize)) {
        rating += 5;
      }

      fm.skip(60);

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

      //ExporterPlugin exporter = Exporter_ZLib.getInstance();

      // RESETTING GLOBAL VARIABLES

      FileManipulator fm = new FileManipulator(path, false);

      long arcSize = fm.getLength();

      // 4 - Header (KNAB)
      // 4 - Unknown (8)
      // 8 - File Data Offset
      // 8 - null
      // 8 - Filename Details Directory Offset (96)
      // 8 - Empty Directory Offset 1
      // 8 - Empty Directory Offset 2
      fm.skip(48);

      // 8 - File Details Directory Offset
      long dirOffset = fm.readLong();
      FieldValidator.checkOffset(dirOffset, arcSize);

      // 8 - Filename Directory Offset
      long filenameDirOffset = fm.readLong();
      FieldValidator.checkOffset(filenameDirOffset, arcSize);

      // 4 - Number of Filenames
      // 4 - null
      // 4 - null
      fm.skip(12);

      // 4 - Number of Files
      int numFiles = fm.readInt();
      FieldValidator.checkNumFiles(numFiles);

      // 16 - Filename (no extension) (null terminated, filled with nulls)
      fm.seek(dirOffset);

      Resource[] resources = new Resource[numFiles];
      TaskProgressManager.setMaximum(numFiles);

      // Loop through directory
      int[] offsets = new int[numFiles];
      int[] lengths = new int[numFiles];
      int[] nameOffsets = new int[numFiles];
      for (int i = 0; i < numFiles; i++) {
        // 4 - Filename Offset
        int nameOffset = fm.readInt();
        FieldValidator.checkOffset(nameOffset, arcSize);
        nameOffsets[i] = nameOffset;

        // 4 - File Metadata Offset
        int offset = fm.readInt();
        FieldValidator.checkOffset(offset, arcSize);
        offsets[i] = offset;
      }

      for (int i = 1; i < numFiles; i++) {
        lengths[i - 1] = offsets[i] - offsets[i - 1];
      }
      lengths[numFiles - 1] = (int) (filenameDirOffset - offsets[numFiles - 1]);

      // Loop through directory
      String[] names = new String[numFiles];
      for (int i = 0; i < numFiles; i++) {
        fm.relativeSeek(nameOffsets[i]);

        // X - Directory Name
        // 1 - null Directory Name Terminator
        // X - Filename
        // 1 - null Filename Terminator
        String filename = fm.readNullString() + "/" + fm.readNullString();
        names[i] = filename;
      }

      // Loop through directory
      int realNumFiles = 0;
      for (int i = 0; i < numFiles; i++) {
        fm.relativeSeek(offsets[i]);

        int entryLength = lengths[i];

        if (entryLength == 60) {
          // 4 - Filename Offset
          // 4 - Unknown
          fm.skip(8);

          // 4 - File Data Offset
          int offset = fm.readInt();
          FieldValidator.checkOffset(offset, arcSize);

          // 4 - Unknown (2)
          // 4 - Unknown (-1)
          // 4 - Frequency (44100)
          fm.skip(12);

          // 4 - File Data Length
          int length = fm.readInt();
          FieldValidator.checkLength(length, arcSize);

          // 8 - null
          // 4 - Unknown
          // 12 - null
          // 4 - Unknown
          // 4 - null

          String filename = names[i];

          //path,name,offset,length,decompLength,exporter
          resources[realNumFiles] = new Resource(path, filename, offset, length);
          realNumFiles++;

        }
        else {
          continue;
        }

        TaskProgressManager.setValue(i);
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
