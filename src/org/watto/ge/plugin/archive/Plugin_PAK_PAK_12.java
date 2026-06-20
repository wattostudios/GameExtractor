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

import org.watto.datatype.Archive;
import org.watto.datatype.FileType;
import org.watto.datatype.Resource;
import org.watto.ge.helper.FieldValidator;
import org.watto.ge.plugin.ArchivePlugin;
import org.watto.ge.plugin.ExporterPlugin;
import org.watto.ge.plugin.exporter.Exporter_ZLib;
import org.watto.io.FileManipulator;
import org.watto.task.TaskProgressManager;

/**
**********************************************************************************************

**********************************************************************************************
**/
public class Plugin_PAK_PAK_12 extends ArchivePlugin {

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public Plugin_PAK_PAK_12() {

    super("PAK_PAK_12", "PAK_PAK_12");

    //         read write replace rename
    setProperties(true, false, false, false);

    setGames("Shrek Smash n' Crash Racing");
    setExtensions("pak"); // MUST BE LOWER CASE
    setPlatforms("GameCube");

    // MUST BE LOWER CASE !!!
    setFileTypes(new FileType("thp", "THP Audio", FileType.TYPE_AUDIO),
        new FileType("fsb", "FSB Audio Archive", FileType.TYPE_ARCHIVE));

    setTextPreviewExtensions("ent", "sounddescriptor", "uvanim"); // LOWER CASE

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
      if (fm.readInt() == 4931920) { // "PAK" + null
        rating += 50;
      }

      long arcSize = fm.getLength();

      // Directory Length
      if (FieldValidator.checkLength(fm.readInt(), arcSize)) {
        rating += 5;
      }

      if (fm.readInt() == 12) {
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

      ExporterPlugin exporter = Exporter_ZLib.getInstance();

      // RESETTING GLOBAL VARIABLES

      FileManipulator fm = new FileManipulator(path, false);

      long arcSize = fm.getLength();

      // 4 - Header ("PAK" + null)
      // 4 - File Data Offset [+padding]
      // 4 - Header Length (12)
      fm.skip(12);

      int numFolders = Archive.getMaxFiles();
      int numFiles = Archive.getMaxFiles();

      int realNumFolders = 0;
      int realNumFiles = 0;

      // Read the Folders
      String[] folderNames = new String[numFolders];
      int[] folderOffsets = new int[numFolders];

      boolean endOfFolders = false;
      while (!endOfFolders) {
        // 4 - Next Folder Entry Offset
        int nextFolderOffset = fm.readInt();
        FieldValidator.checkOffset(nextFolderOffset, arcSize);

        if (nextFolderOffset == 0) {
          endOfFolders = true;
        }

        // 4 - Offset to the File Entries for the files in this Folder
        int folderEntriesOffset = fm.readInt();
        FieldValidator.checkOffset(folderEntriesOffset, arcSize);
        folderOffsets[realNumFolders] = folderEntriesOffset;

        // X - Folder Name
        // 1 - null Folder Name Terminator
        // 0-3 - null Padding to a multiple of 4 bytes
        String folderName = fm.readNullString();
        FieldValidator.checkFilename(folderName);
        folderNames[realNumFolders] = folderName;

        realNumFolders++;

        if (!endOfFolders) {
          fm.relativeSeek(nextFolderOffset);
        }
      }

      // Read the files in each folder
      String[] filenames = new String[numFiles];
      int[] fileLengths = new int[numFiles];
      int[] fileOffsets = new int[numFiles];

      for (int i = 0; i < realNumFolders; i++) {
        fm.relativeSeek(folderOffsets[i]);

        String dirName = folderNames[i];

        boolean endOfFiles = false;
        while (!endOfFiles) {
          // 4 - Next File Entry Offset
          int nextFileOffset = fm.readInt();
          FieldValidator.checkOffset(nextFileOffset, arcSize);

          if (nextFileOffset == 0) {
            endOfFiles = true;
          }

          // 4 - File Length (not including padding)(if the file is compressed, this includes the compression header fields)
          int length = fm.readInt();
          FieldValidator.checkLength(length, arcSize);
          fileLengths[realNumFiles] = length;

          // 4 - Flags
          fm.skip(4);

          // 4 - File Offset Entry Offset (pointer into the Offsets Directory for this file)
          int offset = fm.readInt();
          FieldValidator.checkOffset(offset, arcSize);
          fileOffsets[realNumFiles] = offset;

          // X - Filename
          // 1 - null Filename Terminator
          // 0-3 - null Padding to a multiple of 4 bytes
          String filename = fm.readNullString();
          FieldValidator.checkFilename(filename);
          filenames[realNumFiles] = dirName + filename;

          realNumFiles++;

          if (!endOfFiles) {
            fm.relativeSeek(nextFileOffset);
          }
        }
      }

      // now read all the file offsets
      for (int i = 0; i < realNumFiles; i++) {
        fm.relativeSeek(fileOffsets[i]);

        // 4 - File Data Offset
        int offset = fm.readInt();
        FieldValidator.checkOffset(offset, arcSize);
        fileOffsets[i] = offset;
      }

      // now go through and look for compressed files
      fm.getBuffer().setBufferSize(8); // small quick reads

      Resource[] resources = new Resource[realNumFiles];
      TaskProgressManager.setMaximum(realNumFiles);

      for (int i = 0; i < realNumFiles; i++) {
        int offset = fileOffsets[i];
        int length = fileLengths[i];
        String filename = filenames[i];

        fm.relativeSeek(offset);

        // 4 - Compression Header (!CMP)
        String header = fm.readString(4);
        if (header.equals("!CMP")) {
          // compressed file

          // 4 - Decompressed Length
          int decompLength = fm.readInt();
          FieldValidator.checkLength(decompLength);

          // X - File Data (ZLib Compression)
          offset += 8;
          length -= 8;

          //path,name,offset,length,decompLength,exporter
          resources[i] = new Resource(path, filename, offset, length, decompLength, exporter);
        }
        else {
          // uncompressed

          //path,name,offset,length,decompLength,exporter
          resources[i] = new Resource(path, filename, offset, length);
        }

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
