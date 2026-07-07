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
public class Plugin_BIN_48 extends ArchivePlugin {

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public Plugin_BIN_48() {

    super("BIN_48", "BIN_48");

    //         read write replace rename
    setProperties(true, false, false, false);

    setGames("Hokuto no Ken: Shinpan no Sousousei Kengou Retsuden");
    setExtensions("bin"); // MUST BE LOWER CASE
    setPlatforms("PS2");

    setFileTypes(new FileType("vag", "VAG Audio", FileType.TYPE_AUDIO));

    //setTextPreviewExtensions("colours", "rat", "screen", "styles"); // LOWER CASE

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

      String basePath = fm.getFile().getAbsolutePath();
      int slashPos = basePath.lastIndexOf('_');
      if (slashPos > 0) {
        basePath = basePath.substring(0, slashPos + 1);
      }
      File indexFile = new File(basePath + "I.IDX");
      if (indexFile.exists() && indexFile.isFile()) {
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

      //ExporterPlugin exporter = Exporter_ZLib.getInstance();

      // RESETTING GLOBAL VARIABLES

      long arcSize = path.length();

      String basePath = path.getAbsolutePath();
      int slashPos = basePath.lastIndexOf('_');
      if (slashPos <= 0) {
        return null;
      }

      basePath = basePath.substring(0, slashPos + 1);
      File sourcePath = new File(basePath + "I.IDX");
      if (!sourcePath.exists() || !sourcePath.isFile()) {
        return null;
      }

      FileManipulator fm = new FileManipulator(sourcePath, false);

      long indexSize = sourcePath.length();

      // 4 - Header (FARC)
      // 4 - Unknown (1)
      fm.skip(8);

      // 4 - Number of Folders
      int numFolders = fm.readInt();
      FieldValidator.checkNumFiles(numFolders);

      // 4 - Number of Files
      int numFiles = fm.readInt();
      FieldValidator.checkNumFiles(numFiles);

      int[] offsets = new int[numFolders];
      for (int i = 0; i < numFolders; i++) {
        // 4 - Folder Entry Offset
        int offset = fm.readInt();
        FieldValidator.checkOffset(offset, indexSize);
        offsets[i] = offset;
      }

      for (int i = 0; i < numFolders; i++) {
        fm.relativeSeek(offsets[i]);

        // 4 - Folder Name
        fm.skip(4);

        // 4 - Folder Entries Offset (offset to the entries for the files in this folder)
        int offset = fm.readInt();
        FieldValidator.checkOffset(offset, indexSize);
        offsets[i] = offset;

        // 4 - Folder Entries Length (length of all the entries in this folder)
        // 4 - null
        fm.skip(8);
      }

      Resource[] resources = new Resource[numFiles];
      TaskProgressManager.setMaximum(numFiles);

      // Loop through directory
      int realNumFiles = 0;
      for (int i = 0; i < numFolders; i++) {
        fm.relativeSeek(offsets[i]);

        // 4 - Folder Name
        String folderName = fm.readString(4) + "\\";

        // 4 - Number of Files in this Folder
        int numFilesInFolder = fm.readInt();
        FieldValidator.checkNumFiles(numFilesInFolder + 1); // +1 to allow nulls

        // 4 - Offset in the BIN to the File Data for this Folder Entries
        // 4 - Length in the BIN of the File Data for this Folder Entries
        fm.skip(8);

        for (int j = 0; j < numFilesInFolder; j++) {

          // 4 - File Offset
          long offset = fm.readInt();
          FieldValidator.checkOffset(offset, arcSize);

          // 4 - File Length
          long length = fm.readInt();
          FieldValidator.checkLength(length, arcSize);

          String filename = folderName + Resource.generateFilename(j);

          //path,name,offset,length,decompLength,exporter
          resources[realNumFiles] = new Resource(path, filename, offset, length);
          realNumFiles++;

          TaskProgressManager.setValue(realNumFiles);
        }
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

    if (headerInt1 == 1112756051) {
      return "sosb";
    }
    else if (headerInt1 == 1129464134) {
      return "idx";
    }
    else if (headerInt1 == 1212957008 || headerInt1 == 1213744468) {
      return "texh";
    }
    else if (headerInt1 == 1414286675) {
      return "smlt";
    }
    else if (headerInt1 == 1448232019) {
      return "sdrv";
    }
    else if (headerInt1 == 1883717974) {
      return "vag";
    }

    return null;
  }

}
