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
public class Plugin_MAIN extends ArchivePlugin {

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public Plugin_MAIN() {

    super("MAIN", "MAIN");

    //         read write replace rename
    setProperties(true, false, false, false);

    setGames("Total Overdose");
    setExtensions("main"); // MUST BE LOWER CASE
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

      fm.skip(12);

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

      // 12 - Unknown
      // 4 - Number of Files?
      // 4 - Unknown
      // 2 - null
      // 2 - Unknown (16)
      fm.skip(24);

      int numFiles = Archive.getMaxFiles();

      Resource[] resources = new Resource[numFiles];
      TaskProgressManager.setMaximum(arcSize);

      // Loop through directory
      int realNumFiles = 0;
      while (fm.getOffset() < arcSize) {

        // 4 - File Type? (7/8)
        int fileType = fm.readInt();
        for (int i = 0; i < 4; i++) {
          if (fileType == 0) {
            fileType = fm.readInt();
          }
          else {
            break;
          }
        }

        if (fileType == 0) {
          // premature end
          break;
        }

        long startOffset = fm.getOffset() - 4;
        System.out.println("File Type " + fileType + " at offset " + startOffset);

        if (fileType == 1) {
          // 4 - Filename Offset (relative to the start of this field) (36/84)
          int filenameOffset = fm.readInt() - 12;
          FieldValidator.checkOffset(filenameOffset, arcSize);

          fm.skip(filenameOffset);

          // 4 - File Length
          int length = fm.readInt() + 52;
          FieldValidator.checkLength(length, arcSize);

          // 4 - Max Filename Length (64/80)
          int filenameLength = fm.readInt();
          FieldValidator.checkFilenameLength(filenameLength);

          // X - Filename
          // X - Padding to fill to Max Filename Length
          String filename = fm.readNullString(filenameLength);
          FieldValidator.checkFilename(filename);

          int readSize = (int) (fm.getOffset() - startOffset);
          length -= readSize;

          // X - File Data
          long offset = fm.getOffset();
          fm.skip(length);

          //path,name,offset,length,decompLength,exporter
          resources[realNumFiles] = new Resource(path, filename, offset, length);
          realNumFiles++;

          TaskProgressManager.setValue(offset);

          continue;
        }
        else if (fileType == 2) {
          // 4 - Filename Offset (relative to the start of this field) (36/84)
          int filenameOffset = (int) (fm.getOffset() + fm.readInt());
          FieldValidator.checkOffset(filenameOffset, arcSize);

          // 4 - null
          // 4 - Unknown
          // 10 - null
          // 4 - Unknown
          // 2 - null
          // 4 - Unknown
          // 8 - null
          // 4 - Unknown
          fm.skip(40);

          // 4 - File Length (part 1)
          int length = fm.readInt();

          // 4 - File Length (part 2)
          length += fm.readInt();

          // 4 - File Length (part 3)
          length += fm.readInt();

          length += 48;

          FieldValidator.checkLength(length, arcSize);

          fm.relativeSeek(filenameOffset);

          // X - Filename
          // X - Padding to fill to Max Filename Length
          String filename = fm.readNullString();
          FieldValidator.checkFilename(filename);
          fm.skip(calculatePadding((int) fm.getOffset(), 4));

          int readSize = (int) (fm.getOffset() - startOffset);
          length -= readSize;

          // X - File Data
          long offset = fm.getOffset();
          fm.skip(length);

          //path,name,offset,length,decompLength,exporter
          resources[realNumFiles] = new Resource(path, filename, offset, length);
          realNumFiles++;

          TaskProgressManager.setValue(offset);

          // 0-3 - null Padding to a multiple of 4 bytes
          fm.skip(calculatePadding((int) fm.getOffset(), 4));

          continue;
        }

        // 4 - Filename Offset (relative to the start of this field) (36/84)
        int filenameOffset = fm.readInt() - 12;
        FieldValidator.checkOffset(filenameOffset, arcSize);

        // 4 - null
        // 4 - Unknown
        // 10 - null
        // 2 - Unknown
        // 4 - Unknown (24)
        fm.skip(filenameOffset);

        // 4 - Max Filename Length (64/80)
        int filenameLength = fm.readInt();
        FieldValidator.checkFilenameLength(filenameLength);

        // (Max Filename Length contains...){
        // 4 - Unknown
        // X - Filename
        // 1 - null Filename Terminator
        // 0-3 - Padding (byte 171) to a multiple of 4 bytes
        // X - null (to fill out to Max Filename Length
        // }
        fm.skip(4);
        String filename = fm.readNullString(filenameLength - 4);
        FieldValidator.checkFilename(filename);

        if (fileType == 7) {
          // 4 - Sample Rate (44100)
          // 2 - Channels? (1)
          // 2 - Unknown
          // 4 - Unknown
          fm.skip(12);

          // 4 - File Length
          int length = fm.readInt();
          FieldValidator.checkLength(length, arcSize);

          // 6 - null
          // 4 - Unknown (16384)
          // 4 - Unknown (50432)
          // 2 - null
          // 4 - Unknown (44)
          // 4 - File Length (approx)
          // 2 - null
          // 4 - Unknown (32768)
          // 4 - Unknown (3712)
          // 18 - null
          // 4 - Unknown (-1162872064)
          // 4 - null
          fm.skip(60);

          // X - File Data
          long offset = fm.getOffset();
          fm.skip(length);

          // 0-3 - null Padding to a multiple of 4 bytes
          fm.skip(calculatePadding((int) fm.getOffset(), 4));

          //path,name,offset,length,decompLength,exporter
          resources[realNumFiles] = new Resource(path, filename, offset, length);
          realNumFiles++;

          TaskProgressManager.setValue(offset);
        }
        else if (fileType == 8) {
          long offset = fm.getOffset();

          // 4 - Unknown
          // 4 - Unknown
          // 4 - Unknown
          // 8 - null
          // 4 - Sample Rate (44100)
          // 4 - Unknown
          // 4 - Unknown (1)
          // 4 - Unknown
          int length = 36;
          fm.skip(length);

          //path,name,offset,length,decompLength,exporter
          resources[realNumFiles] = new Resource(path, filename, offset, length);
          realNumFiles++;

          TaskProgressManager.setValue(offset);
        }
        else {
          ErrorLogger.log("[MAIN] Unknown file type: " + fileType + " at offset " + startOffset);
        }

        // 4 - Filename Length
        // 4 - Unknown (12)
        fm.skip(8);

        // 4 - Filename Length
        filenameLength = fm.readInt();
        FieldValidator.checkFilenameLength(filenameLength);

        // 4 - Unknown
        fm.skip(4);

        // 4 - null
        if (fm.readInt() == 0) {
          // X - Filename
          // 1 - null Filename Terminator
          fm.skip(filenameLength + 1);
        }
        else {
          // the null field didn't actually exist

          // X - Filename
          // 1 - null Filename Terminator
          fm.skip(filenameLength - 4 + 1);
        }

        // 0-3 - null Padding to a multiple of 4 bytes
        fm.skip(calculatePadding((int) fm.getOffset(), 4));

        // 8 - null
        //fm.skip(8);

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
