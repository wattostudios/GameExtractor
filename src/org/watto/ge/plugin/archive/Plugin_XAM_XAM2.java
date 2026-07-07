/*
 * Application:  Game Extractor
 * Author:       wattostudios
 * Website:      http://www.watto.org
 * Copyright:    Copyright (c) 2002-2026 wattostudios
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
import org.watto.io.FileManipulator;
import org.watto.task.TaskProgressManager;

/**
**********************************************************************************************

**********************************************************************************************
**/
public class Plugin_XAM_XAM2 extends ArchivePlugin {

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public Plugin_XAM_XAM2() {

    super("XAM_XAM2", "XAM_XAM2");

    //         read write replace rename
    setProperties(true, false, false, false);

    setGames("Deadly Premonition");
    setExtensions("xam"); // MUST BE LOWER CASE
    setPlatforms("PC");

    // MUST BE LOWER CASE !!!
    setFileTypes(new FileType("xmd", "XMD Mesh", FileType.TYPE_MODEL));

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
      if (fm.readString(4).equals("XAM2")) {
        rating += 50;
      }

      long arcSize = fm.getLength();

      // Archive Size
      if (FieldValidator.checkEquals(fm.readInt(), arcSize)) {
        rating += 5;
      }

      fm.skip(36);

      // Directory Offset
      if (FieldValidator.checkOffset(fm.readInt(), arcSize)) {
        rating += 5;
      }

      // File Data Offset
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

      // 4 - Header (XAM2)
      // 4 - File Length
      // 2 - Unknown
      // 2 - Unknown
      // 4 - Unknown
      // 4 - Unknown (256)
      // 12 - null
      // 4 - Unknown
      // 4 - Unknown
      // 4 - Unknown
      fm.skip(44);

      // 4 - Directory Offset (80)
      int dirOffset = fm.readInt();
      FieldValidator.checkOffset(dirOffset, arcSize);

      // 4 - File Data Offset
      int fileDataOffset = fm.readInt();
      FieldValidator.checkOffset(fileDataOffset, arcSize);

      int numFiles = (fileDataOffset - dirOffset) / 16;
      FieldValidator.checkNumFiles(numFiles);

      // 24 - null
      // 4 - Unknown
      fm.seek(dirOffset);

      Resource[] resources = new Resource[numFiles];
      TaskProgressManager.setMaximum(numFiles);

      // Loop through directory
      String[] names = new String[numFiles];
      for (int i = 0; i < numFiles; i++) {
        // 15 - Filename (null terminated, filled with nulls)
        String filename = fm.readNullString(15);
        FieldValidator.checkFilename(filename);
        filename += ".XMD";

        // 1 - Unknown ID?
        fm.skip(1);

        names[i] = filename;
      }

      fm.getBuffer().setBufferSize(8);
      fm.seek(1);
      fm.seek(fileDataOffset);

      for (int i = 0; i < numFiles; i++) {
        long offset = fm.getOffset();

        // 4 - Header (XZP1)
        fm.skip(4);

        // 4 - File Length (including these header fields)
        int length = fm.readInt();
        FieldValidator.checkLength(length, arcSize);

        fm.skip(length - 8); // -8 because we've already read 8 bytes from the file

        String filename = names[i];

        //path,name,offset,length,decompLength,exporter
        resources[i] = new Resource(path, filename, offset, length);

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
