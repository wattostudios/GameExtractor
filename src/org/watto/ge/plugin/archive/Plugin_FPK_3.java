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

import org.watto.datatype.Resource;
import org.watto.ge.helper.FieldValidator;
import org.watto.ge.plugin.ArchivePlugin;
import org.watto.io.FileManipulator;
import org.watto.task.TaskProgressManager;

/**
**********************************************************************************************

**********************************************************************************************
**/
public class Plugin_FPK_3 extends ArchivePlugin {

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public Plugin_FPK_3() {

    super("FPK_3", "FPK_3");

    //         read write replace rename
    setProperties(true, false, false, false);

    setGames("WWE Raw");
    setExtensions("fpk"); // MUST BE LOWER CASE
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
      if (fm.readInt() == -268304385) {
        rating += 25;
      }
      if (fm.readInt() == 536940583) {
        rating += 25;
      }

      // Number Of Files
      if (FieldValidator.checkNumFiles(fm.readInt())) {
        rating += 5;
      }

      long arcSize = fm.getLength();

      // Directory Length
      if (FieldValidator.checkLength(fm.readInt(), arcSize)) {
        rating += 5;
      }

      // File Data Length
      if (FieldValidator.checkLength(fm.readInt(), arcSize)) {
        rating += 5;
      }

      if (fm.readString(4).equals("Root")) {
        rating += 5;
      }

      return rating;

    }
    catch (Throwable t) {
      return 0;
    }
  }

  int realNumFiles = 0;

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

      // 8 - Unknown
      fm.skip(8);

      // 4 - Number of Files
      int numFiles = fm.readInt();
      FieldValidator.checkNumFiles(numFiles);

      // 4 - File Details Directory Length
      // 4 - File Data Length

      // 20 - Root Directory Name (null terminated, filled with nulls) ("Root")
      // 4 - Number of Folders in the Root Directory
      // 4 - Unknown
      // 4 - null
      // 4 - Unknown
      // 4 - null
      fm.skip(48);

      realNumFiles = 0;

      Resource[] resources = new Resource[numFiles];
      TaskProgressManager.setMaximum(numFiles);

      // 20 - Directory Name (null terminated, filled with nulls)
      String name = fm.readNullString(20); // "Root"
      FieldValidator.checkFilename(name);
      //name += "\\";
      name = "";

      // 2 - Number of Folders in this Directory
      short numDirsInRoot = fm.readShort();
      FieldValidator.checkNumFiles(numDirsInRoot + 1); // +1 to allow for nulls

      // 2 - Number of Files in this Directory
      short numFilesInRoot = fm.readShort();
      FieldValidator.checkNumFiles(numFilesInRoot + 1); // +1 to allow for nulls

      // 4 - Unknown
      // 4 - Unknown
      fm.skip(8);

      readDirectory(path, fm, resources, name, arcSize, numDirsInRoot, numFilesInRoot);

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
  public void readDirectory(File path, FileManipulator fm, Resource[] resources, String dirName, long arcSize, int numDirs, int numFiles) {
    try {

      // Loop through directory
      for (int i = 0; i < numFiles; i++) {
        // 20 - Filename (null terminated, filled with nulls)
        String filename = fm.readNullString(20);
        FieldValidator.checkFilename(filename);
        filename = dirName + filename;

        // 4 - File Offset
        int offset = fm.readInt();
        FieldValidator.checkOffset(offset, arcSize);

        // 4 - File Length
        int length = fm.readInt();
        FieldValidator.checkLength(length, arcSize);

        //path,name,offset,length,decompLength,exporter
        resources[realNumFiles] = new Resource(path, filename, offset, length);
        TaskProgressManager.setValue(realNumFiles);
        realNumFiles++;
      }

      String[] dirNames = new String[numDirs];
      int[] numDirsInFolders = new int[numDirs];
      int[] numFilesInFolders = new int[numDirs];
      for (int i = 0; i < numDirs; i++) {
        // 20 - Directory Name (null terminated, filled with nulls)
        String name = fm.readNullString(20);
        FieldValidator.checkFilename(name);
        name = dirName + name + "\\";
        dirNames[i] = name;

        // 2 - Number of Folders in this Directory
        short numDirsInFolder = fm.readShort();
        FieldValidator.checkNumFiles(numDirs + 1); // +1 to allow for nulls
        numDirsInFolders[i] = numDirsInFolder;

        // 2 - Number of Files in this Directory
        short numFilesInFolder = fm.readShort();
        FieldValidator.checkNumFiles(numFiles + 1); // +1 to allow for nulls
        numFilesInFolders[i] = numFilesInFolder;

        // 4 - Unknown
        // 4 - Unknown
        fm.skip(8);
      }

      for (int i = 0; i < numDirs; i++) {
        readDirectory(path, fm, resources, dirNames[i], arcSize, numDirsInFolders[i], numFilesInFolders[i]);
      }

    }
    catch (Throwable t) {
      logError(t);
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
