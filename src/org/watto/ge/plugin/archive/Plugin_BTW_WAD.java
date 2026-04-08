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
public class Plugin_BTW_WAD extends ArchivePlugin {

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public Plugin_BTW_WAD() {

    super("BTW_WAD", "BTW_WAD");

    //         read write replace rename
    setProperties(true, false, false, false);

    setGames("Starsky & Hutch");
    setExtensions("btw"); // MUST BE LOWER CASE
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

      if (fm.readLong() == 0) {
        rating += 5;
      }

      // Header
      if (fm.readString(4).equals("WAD!")) {
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

      // 8 - null
      // 4 - Header ("WAD!")
      // 4 - Unknown (4)
      // 8 - Checksum?
      // 128 - Archive Description

      // 4 - Directories Offset
      // 4 - Directories Length (not including Padding in the Footer)

      // 4 - Number of Entries in Directory 1
      // 4 - Directory 1 Offset
      // 4 - null
      fm.skip(172);

      // 4 - Number of Files
      int numFiles = fm.readInt();
      FieldValidator.checkNumFiles(numFiles);

      // 4 - Directory 2 Offset
      int dirOffset = fm.readInt();
      FieldValidator.checkOffset(dirOffset, arcSize);

      // 4 - Number of Entries in Directory 3
      // 4 - Directory 3 Offset

      // 4 - Unknown
      // 4 - Directory 4 Offset
      // 4 - Number of Extensions

      // 4 - Directory 5 Offset
      // 4 - Number of Entries in Directory 5

      // 4 - Filename Directory Offset
      // 4 - Filename Directory Length
      fm.seek(dirOffset);

      Resource[] resources = new Resource[numFiles];
      TaskProgressManager.setMaximum(numFiles);

      // Loop through directory
      for (int i = 0; i < numFiles; i++) {

        // 4 - File Length
        int length = fm.readInt();
        FieldValidator.checkLength(length, arcSize);

        // 4 - File Offset
        int offset = fm.readInt();
        if (offset == -1) {
          offset = 0; // empty file
        }
        FieldValidator.checkOffset(offset, arcSize);

        String filename = Resource.generateFilename(i);

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
    if (headerBytes[1] == 86 && headerBytes[2] == 52) {
      return "bsp";
    }
    */
    //else if (headerInt2 + 12 == resource.getDecompressedLength()) {
    if (headerInt3 == 268828671 || headerInt3 == 335609855 || headerInt3 == 268697599 || headerInt3 == 134283263 || headerInt3 == 201523199 || headerInt3 == 268763135) {
      return "dff";
    }
    else if (headerInt1 == 1229476947) {
      return "st"; // STHI header - a language file
    }
    else {
      // look for all Ascii
      boolean ascii = true;
      for (int i = 0; i < headerBytes.length; i++) {
        byte currentByte = headerBytes[i];
        if (currentByte == 13 || currentByte == 10 || currentByte == 9 || currentByte >= 32) {
          // ok
        }
        else {
          ascii = false;
          break;
        }
      }
      if (ascii) {
        return "txt";
      }
    }

    return null;
  }

}
