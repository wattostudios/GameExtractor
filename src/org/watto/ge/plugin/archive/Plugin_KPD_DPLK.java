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

import org.watto.ErrorLogger;
import org.watto.datatype.Archive;
import org.watto.datatype.FileType;
import org.watto.datatype.Resource;
import org.watto.ge.helper.FieldValidator;
import org.watto.ge.plugin.ArchivePlugin;
import org.watto.io.FileManipulator;
import org.watto.io.converter.ShortConverter;
import org.watto.task.TaskProgressManager;

/**
**********************************************************************************************

**********************************************************************************************
**/
public class Plugin_KPD_DPLK extends ArchivePlugin {

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public Plugin_KPD_DPLK() {

    super("KPD_DPLK", "KPD_DPLK");

    //         read write replace rename
    setProperties(true, false, false, false);

    setGames("Ultimate Ghosts 'n Goblins");
    setExtensions("kpd"); // MUST BE LOWER CASE
    setPlatforms("PSP");

    // MUST BE LOWER CASE !!!
    setFileTypes(new FileType("at3", "AT3 Audio", FileType.TYPE_AUDIO),
        new FileType("kpd", "KPD Archive", FileType.TYPE_ARCHIVE),
        new FileType("efa", "EFA Archive", FileType.TYPE_ARCHIVE),
        new FileType("gim", "PlayStation GIM Image", FileType.TYPE_IMAGE));

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
      if (fm.readString(4).equals("DPLK")) {
        rating += 50;
      }

      fm.skip(4);

      long arcSize = fm.getLength();

      // Archive Size
      if (FieldValidator.checkEquals(fm.readLong(), arcSize)) {
        rating += 5;
      }

      if (fm.readLong() == 2048) {
        rating += 5;
      }

      if (fm.readLong() == 2048) {
        rating += 5;
      }

      // Directory Length
      if (FieldValidator.checkLength(fm.readLong(), arcSize)) {
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

      // 4 - Header (DPLK)
      // 4 - Unknown (256)
      // 8 - Archive Length
      // 8 - Padding Multiple? (2048)
      fm.skip(24);

      // 8 - Details Directory Offset
      long dirOffset = fm.readLong();
      FieldValidator.checkOffset(dirOffset, arcSize);

      // 8 - Details Directory Length
      fm.skip(8);

      // 8 - File Data Offset
      long dataOffset = fm.readLong();
      FieldValidator.checkOffset(dataOffset, arcSize);

      // 8 - File Data Length
      // X - null Padding to a multiple of 2048 bytes
      fm.seek(dirOffset);

      int numFiles = Archive.getMaxFiles();
      realNumFiles = 0;

      Resource[] resources = new Resource[numFiles];
      TaskProgressManager.setMaximum(arcSize);

      // read the root directory
      readDirectory(path, fm, resources, "", dataOffset, arcSize);

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

  public void readDirectory(File path, FileManipulator fm, Resource[] resources, String dirName, long dataOffset, long arcSize) {
    try {
      long entryOffset = fm.getOffset();

      //System.out.println("Reading dir " + dirName + " at " + entryOffset);

      // 4 - Entry Contains Files (0=no files, 1=files)
      fm.skip(4);

      // 4 - This Entry Header Length (2048)
      int headerLength = fm.readInt();
      FieldValidator.checkLength(headerLength, arcSize);

      // 8 - Length of Entries Data in this Sub-Directory
      fm.skip(8);

      // 8 - This Entry Length
      long entryLength = fm.readLong();
      FieldValidator.checkLength(entryLength, arcSize);

      // 8 - Unknown
      fm.skip(8);

      // 8 - File Data Offset (relative to the start of the File Data)
      long relativeOffset = dataOffset + fm.readLong();

      // 8 - File Data Length? (for all files in this sub-directory, and all files in sub-directories under this one)
      fm.skip(8);

      // 2 - Number of Entries in this Directory
      int numEntries = ShortConverter.unsign(fm.readShort());
      FieldValidator.checkNumFiles(numEntries + 1); // +1 to allow zero entries

      if (numEntries != 0) {
        FieldValidator.checkOffset(relativeOffset, arcSize);
      }

      // 4 - Unknown
      // X - Padding (junk) to a multiple of 2048 bytes
      fm.relativeSeek(entryOffset + headerLength);

      //System.out.println("Reading " + numEntries + " entries at " + (entryOffset + headerLength));

      // Loop through directory
      long[] dirOffsets = new long[numEntries];
      String[] dirNames = new String[numEntries];
      int numDirs = 0;

      for (int i = 0; i < numEntries; i++) {
        // 8 - Entry Type (0=directory, 1=file)
        long entryType = fm.readLong();

        if (entryType == 0) {
          // directory

          // 8 - Sub-Directory Offset (relative to the end of this directory entry
          long offset = fm.readLong() + entryOffset + entryLength;
          FieldValidator.checkOffset(offset, arcSize);

          // 8 - Sub-Directory Length (including padding)
          // 2 - Unknown (-1)
          fm.skip(10);

          // 102 - Sub-Directory Name (null terminated, filled with nulls)
          String subDirName = dirName + fm.readNullString(102) + "/";

          dirOffsets[numDirs] = offset;
          dirNames[numDirs] = subDirName;
          numDirs++;
        }
        else if (entryType == 1) {
          // file

          // 8 - File Offset (relative to the start of the File Data for the Entries in this Sub-Directory)
          long offset = fm.readLong() + relativeOffset;
          FieldValidator.checkOffset(offset, arcSize);

          // 8 - File Length (not including padding)
          long length = fm.readLong();
          FieldValidator.checkLength(length, arcSize);

          // 2 - Unknown (-1)
          fm.skip(2);

          // 102 - Filename (null terminated, filled with nulls)
          String filename = dirName + fm.readNullString(102);

          //path,name,offset,length,decompLength,exporter
          resources[realNumFiles] = new Resource(path, filename, offset, length);
          realNumFiles++;

          TaskProgressManager.setValue(offset);
        }
        else {
          ErrorLogger.log("[KPD_DPLK] Unknown entry type: " + entryType);
          return;
        }

      }

      // read the sub-directories
      for (int i = 0; i < numDirs; i++) {
        fm.seek(dirOffsets[i]);
        readDirectory(path, fm, resources, dirNames[i], dataOffset, arcSize);
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
