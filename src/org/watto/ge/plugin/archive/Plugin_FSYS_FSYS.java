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
import org.watto.ge.plugin.exporter.Exporter_LZSS;
import org.watto.io.FileManipulator;
import org.watto.io.converter.IntConverter;
import org.watto.task.TaskProgressManager;

/**
**********************************************************************************************

**********************************************************************************************
**/
public class Plugin_FSYS_FSYS extends ArchivePlugin {

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public Plugin_FSYS_FSYS() {

    super("FSYS_FSYS", "FSYS_FSYS");

    //         read write replace rename
    setProperties(true, false, false, false);

    setGames("Pokemon Battle Revolution");
    setExtensions("fsys"); // MUST BE LOWER CASE
    setPlatforms("Wii");

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
      if (fm.readString(4).equals("FSYS")) {
        rating += 50;
      }

      fm.skip(8);

      // Number Of Files
      if (FieldValidator.checkNumFiles(IntConverter.changeFormat(fm.readInt()))) {
        rating += 5;
      }

      fm.skip(16);

      long arcSize = fm.getLength();

      // Archive Size
      if (FieldValidator.checkEquals(IntConverter.changeFormat(fm.readInt()), arcSize)) {
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

      ExporterPlugin exporter = Exporter_LZSS.getInstance();

      // RESETTING GLOBAL VARIABLES

      FileManipulator fm = new FileManipulator(path, false);

      long arcSize = fm.getLength();

      // 4 - Header (FSYS)
      // 4 - Unknown
      // 4 - null
      fm.skip(12);

      // 4 - Number of Files
      int numFiles = IntConverter.changeFormat(fm.readInt());
      FieldValidator.checkNumFiles(numFiles);

      // 4 - Unknown
      // 4 - Unknown (3)
      // 4 - Unknown (64)
      // 4 - File Data Offset
      // 4 - Archive Length
      // 28 - null
      fm.skip(48);

      // 4 - File Entry List Offset (96)
      int dirOffset = IntConverter.changeFormat(fm.readInt());
      FieldValidator.checkOffset(dirOffset, arcSize);

      // 4 - Filename List Offset
      // 4 - File Data Offset
      // 20 - null
      fm.seek(dirOffset);

      int[] entryOffsets = new int[numFiles];
      for (int i = 0; i < numFiles; i++) {
        int entryOffset = IntConverter.changeFormat(fm.readInt());
        FieldValidator.checkOffset(entryOffset, arcSize);
        entryOffsets[i] = entryOffset;
      }

      Resource[] resources = new Resource[numFiles];
      TaskProgressManager.setMaximum(numFiles);

      // Loop through directory
      for (int i = 0; i < numFiles; i++) {
        fm.relativeSeek(entryOffsets[i]);

        // 4 - Unknown
        fm.skip(4);

        // 4 - File Offset
        int offset = IntConverter.changeFormat(fm.readInt()) + 16; // +16 to skip the file header
        FieldValidator.checkOffset(offset, arcSize);

        // 4 - Decompressed File Length
        int decompLength = IntConverter.changeFormat(fm.readInt());
        FieldValidator.checkLength(decompLength);

        // 4 - Unknown
        // 4 - null
        fm.skip(8);

        // 4 - Compressed File Length (including file headers, not including padding)
        int length = IntConverter.changeFormat(fm.readInt()) - 16; // -16 to remove the file header
        FieldValidator.checkLength(length, arcSize);

        // 8 - null
        // 4 - Unknown (2)
        // 4 - Filename Offset
        // 72 - null
        fm.skip(88);

        String filename = Resource.generateFilename(i);

        //path,name,offset,length,decompLength,exporter
        resources[i] = new Resource(path, filename, offset, length, decompLength, exporter);

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
