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

import org.watto.ErrorLogger;
import org.watto.datatype.Archive;
import org.watto.datatype.Resource;
import org.watto.ge.helper.FieldValidator;
import org.watto.ge.plugin.ArchivePlugin;
import org.watto.io.FileManipulator;
import org.watto.task.TaskProgressManager;

/**
**********************************************************************************************

**********************************************************************************************
**/
public class Plugin_DAT_GHPKG extends ArchivePlugin {

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public Plugin_DAT_GHPKG() {

    super("DAT_GHPKG", "DAT_GHPKG");

    //         read write replace rename
    setProperties(true, false, false, false);

    setGames("Delicious: Emily's Home Sweet Home");
    setExtensions("dat"); // MUST BE LOWER CASE
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
      if (fm.readString(5).equals("GHPKG")) {
        rating += 50;
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

      // 5 - Header (GHPKG)
      fm.skip(5);

      int numFiles = Archive.getMaxFiles();

      Resource[] resources = new Resource[numFiles];
      TaskProgressManager.setMaximum(numFiles);

      // Loop through directory
      int realNumFiles = 0;
      String[] dirNames = new String[10]; // guess max 10 sub-directories
      int numDirs = 0;
      String currentDir = "";
      long firstFileOffset = arcSize;
      while (fm.getOffset() < firstFileOffset) {

        //System.out.println(fm.getOffset());

        // 1 - Entry Type (10=File, 13=Folder, 9=Go back a directory, 63=End Of Directory, 0=padding byte?)
        int entryType = fm.readByte();

        if (entryType == 13) {
          // folder

          // 2 - Folder Name Length
          int folderNameLength = fm.readShort();
          FieldValidator.checkFilenameLength(folderNameLength);

          // X - Folder Name
          String folderName = fm.readString(folderNameLength) + "\\";

          dirNames[numDirs] = folderName;
          numDirs++;
          currentDir += folderName;
        }
        else if (entryType == 10) {
          // file

          // 2 - Filename Length
          int filenameLength = fm.readShort();
          FieldValidator.checkFilenameLength(filenameLength);

          // X - Filename
          String filename = currentDir + fm.readString(filenameLength);

          // 4 - File Offset (relative to the start of the File Data)
          int offset = fm.readInt();
          FieldValidator.checkOffset(offset, arcSize);

          if (offset < firstFileOffset) {
            firstFileOffset = offset;
          }

          // 4 - File Length
          int length = fm.readInt();
          FieldValidator.checkLength(length, arcSize);

          //path,name,offset,length,decompLength,exporter
          resources[realNumFiles] = new Resource(path, filename, offset, length);
          realNumFiles++;

          TaskProgressManager.setValue(offset);
        }
        else if (entryType == 9) {
          // go back a directory
          numDirs--;
          currentDir = "";
          for (int n = 0; n < numDirs; n++) {
            currentDir += dirNames[n];
          }
        }
        else if (entryType == 63) {
          // End of Directory
        }
        else if (entryType == 0) {
          // padding?
        }
        else {
          ErrorLogger.log("[DAT_GHPKG] Unknown entry type: " + entryType + " at " + (fm.getOffset() - 1));
          return null;
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

    /*
    if (headerInt1 == 2037149520) {
      return "js";
    }
    */

    return null;
  }

}
