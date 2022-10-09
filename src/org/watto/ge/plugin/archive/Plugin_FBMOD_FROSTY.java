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
import org.watto.ErrorLogger;
import org.watto.datatype.Resource;
import org.watto.ge.helper.FieldValidator;
import org.watto.ge.plugin.ArchivePlugin;
import org.watto.io.FileManipulator;
import org.watto.task.TaskProgressManager;

/**
**********************************************************************************************

**********************************************************************************************
**/
public class Plugin_FBMOD_FROSTY extends ArchivePlugin {

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public Plugin_FBMOD_FROSTY() {

    super("FBMOD_FROSTY", "FBMOD_FROSTY");

    //         read write replace rename
    setProperties(true, false, false, false);

    setGames("Frosty Tool Suite");
    setExtensions("fbmod", "fifamod"); // MUST BE LOWER CASE
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

      // Header
      if (fm.readString(6).equals("FROSTY")) {
        rating += 50;
      }

      fm.skip(1);

      // 1 - Version? (1)
      if (fm.readByte() == 1) {
        rating += 5;
      }

      fm.skip(4);

      long arcSize = fm.getLength();

      // 8 - Details Directory Offset
      if (FieldValidator.checkOffset(fm.readLong(), arcSize)) {
        rating += 5;
      }

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

      // 6 - HEADER (FROSTY)
      // 1 - null
      // 1 - Version? (1)
      fm.skip(8);

      // 4 - Version? (3/4)
      int version = fm.readInt();

      // 8 - Details Directory Offset
      long dirOffset = fm.readLong();
      FieldValidator.checkOffset(dirOffset, arcSize);

      // 4 - Number of Files
      int numFiles = fm.readInt();
      FieldValidator.checkNumFiles(numFiles);

      // 1 - Game Name Length (6)
      int gameNameLength = fm.readByte();
      FieldValidator.checkPositive(gameNameLength);

      // X - Game Name (FIFA20 / FIFA21)
      fm.skip(gameNameLength);

      // 4 - Unknown
      fm.skip(4);

      if (version == 3) {
        // X - Descriptive Name
        // 1 - null Name Terminator
        fm.readNullString();

        // X - Short/Code Name
        // 1 - null Name Terminator
        fm.readNullString();

        // X - Content Type Name
        // 1 - null Name Terminator
        fm.readNullString();

        // X - Version Name
        // 1 - null Name Terminator
        fm.readNullString();

        // X - Replacing Details Name
        // 1 - null Name Terminator
        fm.readNullString();
      }
      else if (version == 4) {
        // 1 - Descriptive Name Length
        int nameLength = fm.readByte();
        if (nameLength < 0) {
          nameLength = ((fm.readByte() << 7) | nameLength & 127);
        }
        FieldValidator.checkPositive(nameLength);

        // X - Descriptive Name
        fm.skip(nameLength);

        // 1 - Short/Code Name Length
        nameLength = fm.readByte();
        if (nameLength < 0) {
          nameLength = ((fm.readByte() << 7) | nameLength & 127);
        }
        FieldValidator.checkPositive(nameLength);

        // X - Short/Code Name
        fm.skip(nameLength);

        // 1 - Content Type Name Length
        nameLength = fm.readByte();
        if (nameLength < 0) {
          nameLength = ((fm.readByte() << 7) | nameLength & 127);
        }
        FieldValidator.checkPositive(nameLength);

        // X - Content Type Name
        fm.skip(nameLength);

        // 1 - Version Name Length
        nameLength = fm.readByte();
        if (nameLength < 0) {
          nameLength = ((fm.readByte() << 7) | nameLength & 127);
        }
        FieldValidator.checkPositive(nameLength);

        // X - Version Name
        fm.skip(nameLength);

        // 1 - Replacing Details Name Length
        nameLength = fm.readByte();
        if (nameLength < 0) {
          nameLength = ((fm.readByte() << 7) | nameLength & 127);
        }
        FieldValidator.checkPositive(nameLength);

        // X - Replacing Details Name
        fm.skip(nameLength);
      }
      else {
        ErrorLogger.log("[FBMOD_FROSTY] Unknown Version: " + version);
      }

      // 4 - Unknown
      fm.skip(4);

      Resource[] resources = new Resource[numFiles];
      TaskProgressManager.setMaximum(numFiles);

      // Loop through directory
      String[] names = new String[numFiles];

      int realNumFiles = 0;
      for (int i = 0; i < numFiles; i++) {
        // 1 - Type Flag (0/1/2)
        int typeFlag = fm.readByte();

        // 4 - File ID (incremental from 0)
        int fileID = fm.readInt();

        // X - Filename
        // 1 - null Filename Terminator
        String filename = fm.readNullString();
        //System.out.println(filename);

        if (fileID != -1) {
          FieldValidator.checkFilename(filename);

          // 20 - Hash?
          // 8 - Original File Length?
          // 4 - Unknown
          // 2 - null
          fm.skip(34);

          // 2 - Unknown Count (0/1/2/3)
          int unknownCount = fm.readShort();
          FieldValidator.checkPositive(unknownCount);

          // 2 - null
          fm.skip(2);

          // for each (unknownCount)
          //   4 - Unknown
          fm.skip(unknownCount * 4);

          if (typeFlag == 0 || typeFlag == 1) {
            // nothing else
          }
          else if (typeFlag == 2) {
            // 4 - null
            // 12 - Hash?
            // 4 - Unknown (16)
            // 4 - Unknown
            // 4 - Unknown
            // 4 - Unknown
            fm.skip(32);
          }

          else if (typeFlag == 3) {
            // 8 - null
            // 8 - Unknown
            // 4 - Unknown
            // 4 - Unknown
            fm.skip(24);
          }
          else {
            ErrorLogger.log("[FBMOD_FROSTY] Unknown Type Flag: " + typeFlag);
          }
        }

        // 4 - Unknown (0/-1)
        fm.skip(4);

        if (fileID != -1) {
          names[realNumFiles] = filename;
          TaskProgressManager.setValue(realNumFiles);
          realNumFiles++;
        }
        else {

          if (version == 4) {
            // 1 - null
            // 4 - Padding (-1)
            fm.skip(5);
          }

          i--; // to make it loop again - don't want to count this iteration, it was a padding one
        }
      }

      fm.relativeSeek(dirOffset);

      long dataOffset = dirOffset + (numFiles * 16);

      for (int i = 0; i < numFiles; i++) {
        // 8 - File Offset (relative to the start of the File Data)
        long offset = fm.readLong() + dataOffset;
        FieldValidator.checkOffset(offset, arcSize);

        // 8 - File Length
        long length = fm.readLong();
        FieldValidator.checkLength(length, arcSize);

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
