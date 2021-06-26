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
import org.watto.datatype.Archive;
import org.watto.datatype.Resource;
import org.watto.ge.helper.FieldValidator;
import org.watto.ge.plugin.ArchivePlugin;
import org.watto.io.FileManipulator;
import org.watto.io.converter.ByteConverter;
import org.watto.task.TaskProgressManager;

/**
**********************************************************************************************

**********************************************************************************************
**/
public class Plugin_WBR_WBR extends ArchivePlugin {

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public Plugin_WBR_WBR() {

    super("WBR_WBR", "WBR_WBR");

    //         read write replace rename
    setProperties(true, false, false, false);

    setGames("Construction Simulator 2015");
    setExtensions("wbr", "wbm", "wbs"); // MUST BE LOWER CASE
    setPlatforms("PC");

    // MUST BE LOWER CASE !!!
    //setFileTypes(new FileType("txt", "Text Document", FileType.TYPE_DOCUMENT),
    //             new FileType("bmp", "Bitmap Image", FileType.TYPE_IMAGE)
    //             );

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

      // 1 - Unknown (137)
      if (ByteConverter.unsign(fm.readByte()) == 137) {
        rating += 5;
      }

      // 7 - Header ("wbr"/"wbm"/"wbs" + (bytes)13,10,26,10)
      String header = fm.readString(3);
      if (header.equals("wbr") || header.equals("wbm") || header.equals("wbs")) {
        rating += 50;
      }
      fm.skip(4);

      // 4 - Unknown (1)
      // 4 - Unknown (1)
      // 2 - null
      // 4 - Offset to DIRECTORIES AND FILE DATA (30)
      // 4 - Offset to DIRECTORIES AND FILE DATA (30)
      fm.skip(18);

      long arcSize = fm.getLength();

      // Archive Size
      if (FieldValidator.checkEquals(fm.readInt(), arcSize)) {
        rating += 5;
      }

      return rating;

    }
    catch (Throwable t) {
      return 0;
    }
  }

  int realNumFiles = 0;

  int realNumDirs = 0;

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

      // 1 - Unknown (137)
      // 7 - Header ("wbr"/"wbm"/"wbs" + (bytes)13,10,26,10)
      // 4 - Unknown (1)
      // 4 - Unknown (1)
      // 2 - null
      // 4 - Offset to DIRECTORIES AND FILE DATA (30)
      // 4 - Offset to DIRECTORIES AND FILE DATA (30)
      // 4 - Archive Length
      fm.skip(30);

      int numFiles = Archive.getMaxFiles();
      realNumFiles = 0;
      realNumDirs = 0;

      Resource[] resources = new Resource[numFiles];
      TaskProgressManager.setMaximum(arcSize);

      readDirectory(fm, path, resources, "");

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
   
   **********************************************************************************************
   **/
  public Resource[] readDirectory(FileManipulator fm, File path, Resource[] resources, String dirName) {
    try {

      long arcSize = path.length();

      // 4 - Unknown ID
      fm.skip(4);

      // 4 - Entry Header ("LIST" or null)
      String entryType = fm.readString(4);

      if (entryType.equals("LIST")) {
        // a directory

        // 2 - Number of Files in this sub-directory
        short numFilesInDir = fm.readShort();
        FieldValidator.checkNumFiles(numFilesInDir);

        // 8 - null
        fm.skip(8);

        // for each file in this sub-directory
        int[] offsets = new int[numFilesInDir];
        for (int i = 0; i < numFilesInDir; i++) {
          // 4 - File Offset
          int offset = fm.readInt();
          FieldValidator.checkOffset(offset, arcSize);
          offsets[i] = offset;
        }

        // for each file in this sub-directory
        for (int i = 0; i < numFilesInDir; i++) {
          // repeat from DIRECTORIES AND FILE DATA
          fm.seek(offsets[i]);
          readDirectory(fm, path, resources, dirName + "Dir" + realNumDirs + "\\");
          realNumDirs++;
        }

      }
      else {
        // a file

        // 4 - File Data Offset
        int offset = fm.readInt();
        FieldValidator.checkOffset(offset, arcSize);

        // 4 - File Length
        int length = fm.readInt();
        FieldValidator.checkLength(length, arcSize);

        // 4 - null
        fm.skip(4);

        String filename = dirName + Resource.generateFilename(realNumFiles);

        // X - File Data
        resources[realNumFiles] = new Resource(path, filename, offset, length);
        TaskProgressManager.setValue(offset);
        realNumFiles++;
      }

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
