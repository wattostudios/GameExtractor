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
import org.watto.datatype.Resource;
import org.watto.ge.helper.FieldValidator;
import org.watto.ge.plugin.ArchivePlugin;
import org.watto.io.FileManipulator;
import org.watto.task.TaskProgressManager;

/**
**********************************************************************************************

**********************************************************************************************
**/
public class Plugin_ARC_NARC extends ArchivePlugin {

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public Plugin_ARC_NARC() {

    super("ARC_NARC", "ARC_NARC");

    //         read write replace rename
    setProperties(true, false, false, false);

    setGames("Nintendo DS");
    setExtensions("arc"); // MUST BE LOWER CASE
    setPlatforms("NDS");

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
      if (fm.readString(4).equals("NARC")) {
        rating += 50;
      }

      fm.skip(4);

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

      // 4 - Header (NARC)
      // 2 - Byte Order
      // 2 - Version
      // 4 - File Length
      // 2 - Chunk Size (16)
      // 2 - Number of Main Chunks (3)
      fm.skip(16);

      // 4 - Header (BTAF)
      // 4 - Chunk Size (including the header field)
      fm.skip(8);

      // 2 - Number of Files
      short numFiles = fm.readShort();
      FieldValidator.checkNumFiles(numFiles);

      // 2 - null
      fm.skip(2);

      Resource[] resources = new Resource[numFiles];
      TaskProgressManager.setMaximum(numFiles);

      // Loop through directory
      for (int i = 0; i < numFiles; i++) {

        // 4 - File Offset
        int offset = fm.readInt();
        FieldValidator.checkOffset(offset, arcSize);

        // 4 - End File Offset (length = EndFileOffset - FileOffset)
        int length = fm.readInt() - offset;
        FieldValidator.checkLength(length, arcSize);

        //path,name,offset,length,decompLength,exporter
        resources[i] = new Resource(path, "", offset, length);

        TaskProgressManager.setValue(i);
      }

      long fileDataOffset = fm.getOffset();

      // 4 - Header (BTNF)
      fm.skip(4);

      // 4 - Chunk Size (including the header field)
      int nameDirLength = fm.readInt();
      FieldValidator.checkLength(nameDirLength, arcSize);

      fileDataOffset += nameDirLength + 8;
      fileDataOffset += calculatePadding(fileDataOffset, 4); // padding to a multiple of 4 bytes

      long dirBaseOffset = fm.getOffset();

      // 4 - Sub-Directory Offset (relative to the start of the Filename Table) [+8]
      int dirLength = fm.readInt();

      // 2 - ID of first file in this sub-directory
      fm.skip(2);

      // 2 - ID of the Parent Directory (root = Total Number of Directories)
      short numDirectories = fm.readShort();
      FieldValidator.checkNumFiles(numDirectories);

      long[] dirOffsets = new long[numDirectories];
      int[] parentIDs = new int[numDirectories];
      int[] fileIDs = new int[numDirectories];

      dirOffsets[0] = dirBaseOffset + dirLength;
      parentIDs[0] = -1;
      fileIDs[0] = 0;
      for (int i = 1; i < numDirectories; i++) {
        // 4 - Sub-Directory Offset (relative to the start of the Filename Table) [+8]
        dirOffsets[i] = fm.readInt() + dirBaseOffset;
        // 2 - ID of first file in this sub-directory
        fileIDs[i] = fm.readShort();
        // 2 - ID of the Parent Directory (root = Total Number of Directories)
        parentIDs[i] = fm.readShort();
      }

      // start reading from the root directory

      String[] dirNames = new String[numDirectories];
      dirNames[0] = "";

      for (int i = 0; i < numDirectories; i++) {
        fm.seek(dirOffsets[i]);

        String thisDirName = dirNames[i];
        int firstFileID = fileIDs[i];

        // 1 - Entry Type
        int entryType = fm.readByte();
        while (entryType != 0) { // 0 = endOfSubDirectory
          if (entryType > 0) {
            // File
            // X - Filename (length = EntryType)
            String filename = thisDirName + fm.readString(entryType);

            Resource resource = resources[firstFileID];
            resource.setName(filename);
            resource.setOriginalName(filename);
            resource.setOffset(resource.getOffset() + fileDataOffset);
            resource.forceNotAdded(true);
            firstFileID++; // move to the next file
          }
          else if (entryType < 0) {
            // sub-directory
            // X - Directory Name (length = 1-EntryType)
            String subDirName = fm.readString(1 - entryType);

            // 2 - Sub-Directory ID
            int subDirID = fm.readShort();
            dirNames[subDirID] = thisDirName + subDirName + "\\";
          }

          // read the next entryType
          // 1 - Entry Type
          entryType = fm.readByte();
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

    /*
    if (headerInt1 == 2037149520) {
      return "js";
    }
    */

    return null;
  }

}
