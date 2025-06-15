/*
 * Application:  Game Extractor
 * Author:       wattostudios
 * Website:      http://www.watto.org
 * Copyright:    Copyright (c) 2002-2024 wattostudios
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
import org.watto.io.converter.IntConverter;
import org.watto.task.TaskProgressManager;

/**
**********************************************************************************************

**********************************************************************************************
**/
public class Plugin_TIGER_TAFS extends ArchivePlugin {

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public Plugin_TIGER_TAFS() {

    super("TIGER_TAFS", "TIGER_TAFS");

    //         read write replace rename
    setProperties(true, false, false, false);

    setGames("Lara Croft and the Temple of Osiris",
        "Rise of the Tomb Raider");
    setExtensions("tiger"); // MUST BE LOWER CASE
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
      if (fm.readString(4).equals("TAFS")) {
        rating += 50;
      }

      // 4 - Version? (4)
      if (fm.readInt() == 4) {
        rating += 5;
      }

      fm.skip(4);

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

      // 4 - Header (TAFS)
      fm.skip(4);

      // 4 - Version? (3/4)
      int version = fm.readInt();

      // 4 - Number of Split Archives (eg 8 = archives 000-007)
      int numSplit = fm.readInt();
      FieldValidator.checkNumFiles(numSplit);

      // 4 - Number of Files
      int numFiles = fm.readInt();
      FieldValidator.checkNumFiles(numFiles);

      // 4 - Unknown (1/10)
      // 32 - Description (null terminated, filled with nulls)
      fm.skip(36);

      // Find all the archives, and their sizes
      File[] archiveFiles = new File[numSplit];
      long[] archiveLengths = new long[numSplit];

      String basePath = path.getAbsolutePath();
      int dotPos = basePath.lastIndexOf(".tiger") - 3;
      if (dotPos < 0) {
        return null;
      }
      basePath = basePath.substring(0, dotPos);

      for (int i = 0; i < numSplit; i++) {
        String currentFile = basePath;
        if (i < 10) {
          currentFile += "00" + i + ".tiger";
        }
        else if (i < 100) {
          currentFile += "0" + i + ".tiger";
        }
        else {
          currentFile += i + ".tiger";
        }

        File arcFile = new File(currentFile);
        if (!arcFile.exists()) {
          return null;
        }

        archiveFiles[i] = arcFile;
        archiveLengths[i] = arcFile.length();
      }

      Resource[] resources = new Resource[numFiles];
      TaskProgressManager.setMaximum(numFiles);

      if (version == 3) {
        // VERSION 3

        // Loop through directory
        for (int i = 0; i < numFiles; i++) {
          // 4 - Hash
          // 4 - Unknown (-1)
          fm.skip(8);

          // 4 - File Length
          int length = fm.readInt();

          // 4 - File Offset
          long offset = IntConverter.unsign(fm.readInt());

          int arcNumber = 0;
          /*
          if (numSplit > 1) {
            arcNumber = (int) (offset >> 30);
            FieldValidator.checkRange(arcNumber, 0, numSplit);
          
            offset &= 1073741823;
          }
          */

          String filename = Resource.generateFilename(i);

          File thisArcFile = archiveFiles[arcNumber];
          long thisArcSize = archiveLengths[arcNumber];

          FieldValidator.checkOffset(offset, thisArcSize);
          FieldValidator.checkLength(length, thisArcSize);

          //path,name,offset,length,decompLength,exporter
          Resource resource = new Resource(thisArcFile, filename, offset, length);
          resource.forceNotAdded(true);

          resources[i] = resource;

          TaskProgressManager.setValue(i);
        }
      }
      else {
        // VERSION 4

        // Loop through directory
        for (int i = 0; i < numFiles; i++) {
          // 4 - Hash
          // 4 - Unknown (-1)
          fm.skip(8);

          // 4 - File Length
          int length = fm.readInt();

          // 4 - null
          fm.skip(4);

          // 2 - Archive Number
          int arcNumber = fm.readShort();
          FieldValidator.checkRange(arcNumber, 0, numSplit);

          // 2 - Unknown (1)
          fm.skip(2);

          // 4 - File Offset
          int offset = fm.readInt();

          String filename = Resource.generateFilename(i);

          File thisArcFile = archiveFiles[arcNumber];
          long thisArcSize = archiveLengths[arcNumber];

          FieldValidator.checkOffset(offset, thisArcSize);
          FieldValidator.checkLength(length, thisArcSize);

          //path,name,offset,length,decompLength,exporter
          Resource resource = new Resource(thisArcFile, filename, offset, length);
          resource.forceNotAdded(true);

          resources[i] = resource;

          TaskProgressManager.setValue(i);
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

    if (headerInt1 == 22050 || headerInt1 == 32000 || headerInt1 == 44100 || headerInt1 == 48000) {
      return "audio";
    }

    return null;
  }

}
