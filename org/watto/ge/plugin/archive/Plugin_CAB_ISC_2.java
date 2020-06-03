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
import org.watto.datatype.Resource;
import org.watto.ge.helper.FieldValidator;
import org.watto.ge.plugin.ArchivePlugin;
import org.watto.ge.plugin.ExporterPlugin;
import org.watto.ge.plugin.exporter.Exporter_ZLibX;
import org.watto.io.FileManipulator;
import org.watto.task.TaskProgressManager;

/**
**********************************************************************************************

**********************************************************************************************
**/
public class Plugin_CAB_ISC_2 extends ArchivePlugin {

  /**
  **********************************************************************************************

  **********************************************************************************************
  **/
  public Plugin_CAB_ISC_2() {

    super("CAB_ISC_2", "InstallShield CAB Archive (without HDR Index) - CAB_ISC_2");

    //         read write replace rename
    setProperties(true, false, false, false);

    setGames("InstallShield");
    setExtensions("cab"); // MUST BE LOWER CASE
    setPlatforms("PC");

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

      // Header
      if (fm.readString(4).equals("ISc(")) {
        rating += 50;
      }

      long arcSize = fm.getLength();

      fm.skip(8);

      // 4 - File Data Offset
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

      ExporterPlugin exporter = Exporter_ZLibX.getInstance();
      //ExporterPlugin exporter = Exporter_ZLibX.getInstance();
      //ExporterPlugin exporter = Exporter_ZLib_CompressedSizeOnly.getInstance();

      // RESETTING GLOBAL VARIABLES

      long arcSize = (int) path.length();

      FileManipulator fm = new FileManipulator(path, false);

      // 4 - File Header ("ISc(")
      // 2 - Version? (4)
      // 2 - Unknown (256)
      // 4 - null
      fm.skip(12);

      // 4 - Unknown Data Block Offset (512)
      long dirOffset = fm.readInt();
      FieldValidator.checkOffset(dirOffset, arcSize);

      // 4 - Unknown Data Block Length (approximate only) [-8 or -16]
      // 4 - Unknown
      // 4 - null
      // 4 - null
      fm.skip(16);

      // 4 - Number of Files [+1] (including files of type=8)
      int numFiles = fm.readInt() + 1;
      FieldValidator.checkNumFiles(numFiles);

      fm.seek(dirOffset);

      // 4 - Unknown
      // 4 - null
      // 4 - Unknown
      fm.skip(12);

      // 4 - Unknown Data Block Length (including these header fields)
      dirOffset += fm.readInt();
      FieldValidator.checkOffset(dirOffset, arcSize);

      fm.seek(dirOffset);

      // 4 - Files Directory Length
      fm.skip(4);

      // for each file AND directory name...
      //   4 - Offset to File Details (relative to the start of the Files Directory) (not necessarily in offset order!) (or offset to directory name)
      // need to find the smallest offset so we know when we've finished reading this directory
      int smallestOffset = 999999999; // must be larger than bytesRead so that it enters the while() loop, and larger than the first offset, so that the smallest offset will be found
      int bytesRead = 4; // 4 for the directory length field above)
      while (smallestOffset > bytesRead) {
        // 4 - Offset to File Details
        int checkOffset = fm.readInt();
        if (checkOffset < smallestOffset) {
          smallestOffset = checkOffset;
        }
        bytesRead += 4;
      }
      // now we've read the whole directory above, as we've reached the smallestOffset

      Resource[] resources = new Resource[numFiles];
      TaskProgressManager.setMaximum(numFiles);

      int realNumFiles = 0;

      // Loop through directory
      int[] filenameOffsets = new int[numFiles];
      int[] directoryIndexes = new int[numFiles];
      int numDirectories = 0;
      int largestFilenameOffset = 0;
      for (int i = 0; i < numFiles; i++) {

        // 4 - Filename Offset (relative to the start of the Files Directory) (or null if not a file)
        int filenameOffset = fm.readInt();
        if (filenameOffset <= 0) {
          // not a file - skip it
          fm.skip(38);
          continue;
        }
        filenameOffset += dirOffset;
        FieldValidator.checkOffset(filenameOffset, arcSize);
        filenameOffsets[realNumFiles] = filenameOffset;

        // as we go through, find the largest filename offset, so we know where the directory names start
        if (filenameOffset > largestFilenameOffset) {
          largestFilenameOffset = filenameOffset;
        }

        // 4 - Directory Name ID Number (index starts at 0)
        int directoryNameIndex = fm.readInt();
        FieldValidator.checkRange(directoryNameIndex, 0, numFiles);
        directoryIndexes[realNumFiles] = directoryNameIndex;

        // as we go through, find the largest directory index so we know how many directories there are
        if (directoryNameIndex > numDirectories) {
          numDirectories = directoryNameIndex;
        }

        // 2 - Entry Type (8=Unknown, 4=File)
        fm.skip(2);

        // 4 - Decompressed File Length (or null if not a file)
        long decompLength = fm.readInt();
        FieldValidator.checkLength(decompLength);

        // 4 - Compressed File Length (or null if not a file)
        long length = fm.readInt();
        FieldValidator.checkLength(length, arcSize);

        // 4 - Unknown (32/33) (or null if not a file)
        // 4 - Unknown (or null if not a file)
        // 4 - Unknown (or null if not a file)
        // 4 - null
        // 4 - null
        fm.skip(20);

        // 4 - File Data Offset
        long offset = fm.readInt();
        FieldValidator.checkOffset(offset, arcSize);

        //path,name,offset,length,decompLength,exporter
        resources[realNumFiles] = new Resource(path, "", offset, length, decompLength, exporter);
        realNumFiles++;

        TaskProgressManager.setValue(i);
      }

      // Now loop through the filenames directory
      for (int i = 0; i < realNumFiles; i++) {
        fm.seek(filenameOffsets[i]);

        // X - Filename (null)
        String filename = fm.readNullString();
        FieldValidator.checkFilename(filename);
        resources[i].setName(filename);
      }

      // Now get all the directory names
      if (numDirectories != 0) {
        numDirectories++; // +1 as the indexes start at 0, so there is 1 extra directory name

        // go to the largest filename offset
        fm.seek(largestFilenameOffset);

        // read (skip) the last filename
        fm.readNullString();

        // now we're at the start of the directory names, so read them. Noting that a directory name can be empty
        String[] directoryNames = new String[numDirectories];
        for (int i = 0; i < numDirectories; i++) {
          // X - Directory Name (can be empty - ie 0 bytes)
          // 1 - null Directory Name Terminator
          String directoryName = fm.readNullString();
          //FieldValidator.checkFilename(filename); // don't want to check this, as it'll complain about the empty string
          directoryNames[i] = directoryName;
        }

        // now go through each file and set the directory name
        for (int i = 0; i < realNumFiles; i++) {
          int directoryNameIndex = directoryIndexes[i];
          String directoryName = directoryNames[directoryNameIndex];
          if (!directoryName.equals("")) {
            resources[i].setDirectory(directoryName);
          }
        }
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
