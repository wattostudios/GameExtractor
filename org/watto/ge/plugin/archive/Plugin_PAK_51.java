/*
 * Application:  Game Extractor
 * Author:       wattostudios
 * Website:      http://www.watto.org
 * Copyright:    Copyright (c) 2002-2020 wattostudios
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
public class Plugin_PAK_51 extends ArchivePlugin {

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public Plugin_PAK_51() {

    super("PAK_51", "PAK_51");

    //         read write replace rename
    setProperties(true, false, false, false);

    setGames("Star Wars: Episode 3: Revenge Of The Sith");
    setExtensions("pak"); // MUST BE LOWER CASE
    setPlatforms("PS2");

    // MUST BE LOWER CASE !!!
    //setFileTypes(new FileType("txt", "Text Document", FileType.TYPE_DOCUMENT),
    //             new FileType("bmp", "Bitmap Image", FileType.TYPE_IMAGE)
    //             );

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

      fm.skip(4);

      if (fm.readInt() == 0) {
        rating += 5;
      }

      if (fm.readInt() == 1) {
        rating += 4;
      }

      // Number Of Files
      if (FieldValidator.checkNumFiles(fm.readInt())) {
        rating += 5;
      }

      // Number Of Files
      if (FieldValidator.checkNumFiles(fm.readInt())) {
        rating += 5;
      }

      long arcSize = fm.getLength();

      // Directory Offset
      if (FieldValidator.checkOffset(fm.readInt(), arcSize)) {
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

      // 4 - Unknown
      // 4 - null
      // 4 - Unknown (1)
      // 4 - Number of Files in Block 1?
      // 4 - Number of Files in Block 2?
      fm.skip(20);

      // 4 - File Data 1 Offset
      int dirOffset = fm.readInt();
      FieldValidator.checkOffset(dirOffset, arcSize);

      // 4 - File Data 1 Length
      int dirLength = fm.readInt();
      FieldValidator.checkLength(dirLength, arcSize);

      // 4 - Filename Directory Length
      // 4 - null
      // 4 - Filename Directory Length
      // 4 - Number of Filenames?
      // 4 - Number of Filename Directory Offsets
      fm.seek(dirOffset);

      int numFiles = Archive.getMaxFiles();

      Resource[] resources = new Resource[numFiles];
      TaskProgressManager.setMaximum(arcSize);

      long endOfDir = dirOffset + dirLength;

      /*
      // Loop through directory
      int realNumFiles = 0;
      while (fm.getOffset() < endOfDir) {
      
        // 4 - File Length
        int length = fm.readInt();
        FieldValidator.checkLength(length, arcSize);
      
        // X - File Data (starts with the name, null terminated, but it actually part of the file data)
        long offset = fm.getOffset();
        String filename = fm.readNullString();
        if (filename.equals("")) {
          // not sure what's in the rest of this area - move to the File Data 2 section
          fm.seek(endOfDir);
          continue;
        }
        fm.seek(offset + length);
      
        // 12 - null
        fm.skip(12);
      
        // X - padding to 16 bytes
        fm.skip(calculatePadding(fm.getOffset(), 16));
      
        //path,name,offset,length,decompLength,exporter
        resources[realNumFiles] = new Resource(path, filename, offset, length);
        realNumFiles++;
      
        TaskProgressManager.setValue(offset);
      }
      */

      // just in case
      fm.seek(endOfDir);

      // Loop through directory
      int realNumFiles = 0;
      while (fm.getOffset() < arcSize) {
        long thisOffset = fm.getOffset();

        // 4 - Unknown (1)
        // 4 - This Offset [+4]
        fm.skip(8);

        // 4 - File Length (including all these header fields)
        int lengthPlusHeaders = fm.readInt();
        FieldValidator.checkLength(lengthPlusHeaders, arcSize);

        // 4 - File Length (data only)
        int length = fm.readInt();
        FieldValidator.checkLength(length, arcSize);

        // 4 - null
        fm.skip(4);

        // 4 - Filename Length
        int filenameLength = fm.readInt();
        FieldValidator.checkFilenameLength(filenameLength);

        // X - Filename
        String filename = fm.readString(filenameLength);

        // 8 - null
        // 4 - Unknown
        // 4 - Padding (all byte 255)
        fm.skip(16);

        // 4 - File Offset (relative to the start of File Data 1)
        int fileData1Offset = dirOffset + fm.readInt() + 4; // +4 to skip the 4-byte header

        // 4 - File Length (including the 4-byte header, but not including the padding)
        int fileData1Length = fm.readInt() - 4;

        if (length == 0) {
          // calculate the file data length
          length = (int) (lengthPlusHeaders - (fm.getOffset() - thisOffset));
          FieldValidator.checkLength(length, arcSize);
        }

        // X - File Data
        long offset = fm.getOffset();
        fm.skip(length);

        if (length == 0) {
          // this file is stored in the File Data 1 area

          FieldValidator.checkOffset(fileData1Offset);
          offset = fileData1Offset;

          FieldValidator.checkLength(fileData1Length, arcSize);
          length = fileData1Length;
        }

        //System.out.println(offset + "\t" + filename);

        //path,name,offset,length,decompLength,exporter
        resources[realNumFiles] = new Resource(path, filename, offset, length);
        realNumFiles++;

        TaskProgressManager.setValue(offset);
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

}
