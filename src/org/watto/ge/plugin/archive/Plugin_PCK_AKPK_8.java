/*
 * Application:  Game Extractor
 * Author:       wattostudios
 * Website:      http://www.watto.org
 * Copyright:    Copyright (c) 2002-2025 wattostudios
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

import org.watto.datatype.Archive;
import org.watto.datatype.Resource;
import org.watto.ge.helper.FieldValidator;
import org.watto.ge.plugin.ArchivePlugin;
import org.watto.io.FileManipulator;
import org.watto.io.converter.IntConverter;
import org.watto.task.TaskProgressManager;

/**
**********************************************************************************************

**********************************************************************************************
**/
public class Plugin_PCK_AKPK_8 extends ArchivePlugin {

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public Plugin_PCK_AKPK_8() {

    super("PCK_AKPK_8", "PCK_AKPK_8");

    //         read write replace rename
    setProperties(true, false, false, false);

    setGames(
        "Hardspace: Shipbreaker");
    setExtensions("pck"); // MUST BE LOWER CASE
    setPlatforms("PC");

    // MUST BE LOWER CASE !!!
    //setFileTypes(new FileType("txt", "Text Document", FileType.TYPE_DOCUMENT),
    //             new FileType("bmp", "Bitmap Image", FileType.TYPE_IMAGE)
    //             );

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
      if (fm.readString(4).equals("AKPK")) {
        rating += 50;
      }

      long arcSize = fm.getLength();

      // Directory Length
      if (FieldValidator.checkLength(IntConverter.changeFormat(fm.readInt()), arcSize)) {
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

      // 4 - Header (AKPK)
      // 4 - Directory Length (not including padding)
      // 4 - Unknown (1)
      // 4 - Size of Directory Header? [-4]
      // 4 - Unknown (4)
      // 4 - Directory Length
      fm.skip(24);

      // 4 - Number of Folders
      int numFolders = IntConverter.changeFormat(fm.readInt());
      FieldValidator.checkNumFiles(numFolders);

      // for each folder
      //   4 - Folder Name Offset (relative to the start of the NumberOfFolders field)
      //   4 - Parent Folder ID? (0 = no parent folder)
      fm.skip(numFolders * 8);

      // for each folder
      String[] folderNames = new String[numFolders];
      for (int i = 0; i < numFolders; i++) {
        // X - Folder Name
        // 2 - null Folder Name Terminator
        String folderName = fm.readNullString();
        FieldValidator.checkFilename(folderName);
        folderNames[i] = folderName;
      }

      int numFiles = Archive.getMaxFiles();

      Resource[] resources = new Resource[numFiles];
      TaskProgressManager.setMaximum(arcSize);

      // Loop through directory
      int realNumFiles = 0;
      for (int f = 0; f < numFolders; f++) {

        // 4 - null
        fm.skip(4);

        // 4 - Number of Files in this Folder
        numFiles = IntConverter.changeFormat(fm.readInt());
        FieldValidator.checkNumFiles(numFiles + 1); // +1 to allow empty folders

        String folderName = folderNames[f];

        // for each file in this folder
        for (int i = 0; i < numFiles; i++) {
          // 4 - Hash?
          // 4 - Padding Multiple (2048)
          // 4 - null
          fm.skip(12);

          // 4 - File Length
          int length = IntConverter.changeFormat(fm.readInt());
          FieldValidator.checkLength(length, arcSize);

          // 4 - File Offset [*2048]
          int offset = IntConverter.changeFormat(fm.readInt()) * 2048;
          FieldValidator.checkOffset(offset, arcSize);

          // 4 - null
          fm.skip(4);

          String filename = folderName + "\\" + Resource.generateFilename(realNumFiles);

          //path,name,offset,length,decompLength,exporter
          resources[realNumFiles] = new Resource(path, filename, offset, length);

          TaskProgressManager.setValue(offset);
          realNumFiles++;
        }

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

    if (headerInt1 == 1481001298) {
      return "wav"; // RIFX (big-endian RIFF)
    }

    return null;
  }

}
