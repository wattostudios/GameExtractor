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
import org.watto.io.FileManipulator;
import org.watto.task.TaskProgressManager;

/**
**********************************************************************************************

**********************************************************************************************
**/
public class Plugin_ZAL_ZDIR extends ArchivePlugin {

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public Plugin_ZAL_ZDIR() {

    super("ZAL_ZDIR", "ZAL_ZDIR");

    //         read write replace rename
    setProperties(true, false, false, false);

    setGames("X-Men: The Official Game");
    setExtensions("zal"); // MUST BE LOWER CASE
    setPlatforms("PC");

    // MUST BE LOWER CASE !!!
    //setFileTypes(new FileType("txt", "Text Document", FileType.TYPE_DOCUMENT),
    //             new FileType("bmp", "Bitmap Image", FileType.TYPE_IMAGE)
    //             );

    setTextPreviewExtensions("script"); // LOWER CASE

    setCanScanForFileTypes(true);

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
      if (fm.readString(4).equals("ZDIR")) {
        rating += 50;
      }

      long arcSize = fm.getLength();

      // Directory Length
      if (FieldValidator.checkLength(fm.readInt(), arcSize)) {
        rating += 5;
      }

      // Header
      if (fm.readString(4).equals("DIDF")) {
        rating += 5;
      }

      // 4 - Block Length (8)
      if (fm.readInt() == 8) {
        rating += 5;
      }

      // 4 - Number of Files
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

      // 4 - Header (ZDIR)
      // 4 - Directory Length [+12]
      // 4 - Header (DIDF)
      // 4 - Block Length (8)
      fm.skip(16);

      // 4 - Number of Files
      int numFiles = fm.readInt();
      FieldValidator.checkNumFiles(numFiles);

      // 4 - Unknown (2)
      fm.skip(4);

      // GRID DIRECTORY
      fm.skip(8 + (numFiles * 4));

      // LENGTHS DIRECTORY
      fm.skip(8 + (numFiles * 4));

      // 4 - Header (OFST)
      // 4 - Offsets Directory Length (not including these 2 header fields)
      fm.skip(8);

      Resource[] resources = new Resource[numFiles];
      TaskProgressManager.setMaximum(numFiles);

      // Loop through directory
      int[] offsets = new int[numFiles];
      for (int i = 0; i < numFiles; i++) {
        // 4 - File Offset
        int offset = fm.readInt();
        FieldValidator.checkOffset(offset, arcSize);
        offsets[i] = offset;

        TaskProgressManager.setValue(i);
      }

      // 4 - Header (NAME)
      if (!fm.readString(4).equals("NAME")) {
        // no filenames

        // Loop through directory
        for (int i = 0; i < numFiles; i++) {

          String filename = Resource.generateFilename(i);

          int offset = offsets[i];

          //path,name,offset,length,decompLength,exporter
          resources[i] = new Resource(path, filename, offset);

          TaskProgressManager.setValue(i);
        }
      }
      else {
        // filenames

        // 4 - Filename Directory Length (not including these 2 header fields)
        fm.skip(4);

        // Loop through directory
        for (int i = 0; i < numFiles; i++) {
          // X - Filename (null)
          String filename = fm.readNullString();
          FieldValidator.checkFilename(filename);
          filename = filename.replace('.', '_');

          int offset = offsets[i];

          //path,name,offset,length,decompLength,exporter
          resources[i] = new Resource(path, filename, offset);

          TaskProgressManager.setValue(i);
        }
      }

      calculateFileSizes(resources, arcSize);

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

    if (headerInt1 == 1279544646) {
      return "fmdl";
    }
    else if (headerInt1 == 1112099925) {
      return "ulib";
    }
    else if (headerShort2 == 12079) {
      return "script";
    }

    return null;
  }

}
