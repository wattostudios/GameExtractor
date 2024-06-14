/*
 * Application:  Game Extractor
 * Author:       wattostudios
 * Website:      http://www.watto.org
 * Copyright:    Copyright (c) 2002-2023 wattostudios
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
import org.watto.task.TaskProgressManager;

/**
**********************************************************************************************

**********************************************************************************************
**/
public class Plugin_STG_STG2 extends ArchivePlugin {

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public Plugin_STG_STG2() {

    super("STG_STG2", "STG_STG2");

    //         read write replace rename
    setProperties(true, false, false, false);

    setGames("Elven Mists",
        "Green Valley: Fun on the Farm");
    setExtensions("stg"); // MUST BE LOWER CASE
    setPlatforms("PC");

    // MUST BE LOWER CASE !!!
    //setFileTypes(new FileType("txt", "Text Document", FileType.TYPE_DOCUMENT),
    //             new FileType("bmp", "Bitmap Image", FileType.TYPE_IMAGE)
    //             );

    setTextPreviewExtensions("lp", "crc", "animation", "binds", "comics", "composite", "desc", "interface", "level", "options", "scenario", "sound", "style", "stylesheet", "tutorial"); // LOWER CASE

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
      if (fm.readString(4).equals("stg2")) {
        rating += 50;
      }

      // 4 - Number of Sub-Directories in the Root (1)
      if (fm.readInt() == 1) {
        rating += 5;
      }

      // 4 - Header Length (12)
      if (fm.readInt() == 12) {
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
      realNumFiles = 0;

      FileManipulator fm = new FileManipulator(path, false);

      long arcSize = fm.getLength();

      // 4 - Header (stg2)
      // 4 - Number of Sub-Directories in the Root (1)
      // 4 - Header Length (12)
      fm.skip(12);

      int numFiles = Archive.getMaxFiles();

      Resource[] resources = new Resource[numFiles];
      TaskProgressManager.setMaximum(numFiles);

      readDirectory(fm, path, resources, arcSize, "");

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

  public void readDirectory(FileManipulator fm, File path, Resource[] resources, long arcSize, String dirName) {
    try {

      //System.out.println("Reading directory " + dirName);

      // 2 - Unknown (4/6)
      // 4 - Short Directory Offset
      fm.skip(6);

      // 4 - Details Directory Offset
      int dirOffset = fm.readInt();
      FieldValidator.checkOffset(dirOffset, arcSize);

      // 4 - Number of Files? (not including blank entries in the Short Directory)
      int numSubFiles = fm.readInt();
      FieldValidator.checkNumFiles(numSubFiles);

      // 2 - Details Directory Entry Length
      short dirEntryLength = fm.readShort();
      FieldValidator.checkRange(dirEntryLength, 16, 256);

      // SHORT DIRECTORY
      // for each file in this sub-directory
      //  4 - Details Directory Offset
      //  4 - Unknown ID (0 = not a file - an empty entry)
      fm.relativeSeek(dirOffset);

      int filenameLength = dirEntryLength - 15;

      // DETAILS DIRECTORY
      // for each file in this sub-directory
      int numSubs = 0;
      int[] subOffsets = new int[numSubFiles];
      String[] subNames = new String[numSubFiles];
      for (int i = 0; i < numSubFiles; i++) {

        //  2 - Unknown
        //  4 - Hash?
        //  1 - null
        fm.skip(7);

        //  4 - File Length (0 = sub-directory entry)
        int length = fm.readInt();
        FieldValidator.checkLength(length, arcSize);

        //  4 - File Offset
        int offset = fm.readInt();
        FieldValidator.checkOffset(offset, arcSize);

        //  X - Filename
        //  X - null Padding to DirectoryEntryLength
        String filename = fm.readNullString(filenameLength);
        FieldValidator.checkFilename(filename);

        //System.out.println(filename);

        if (length == 0) {
          // a sub-directory

          String subDirName = dirName + filename + File.separatorChar;

          subOffsets[numSubs] = offset;
          subNames[numSubs] = subDirName;

          numSubs++;
        }
        else {
          // a file

          //path,name,offset,length,decompLength,exporter
          resources[realNumFiles] = new Resource(path, dirName + filename, offset, length);

          TaskProgressManager.setValue(realNumFiles);
          realNumFiles++;
        }
      }

      for (int i = 0; i < numSubs; i++) {
        fm.relativeSeek(subOffsets[i]);
        readDirectory(fm, path, resources, arcSize, subNames[i]);
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
