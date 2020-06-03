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
import org.watto.ge.plugin.exporter.BlockExporterWrapper;
import org.watto.ge.plugin.exporter.Exporter_ZLib;
import org.watto.io.FileManipulator;
import org.watto.task.TaskProgressManager;

/**
**********************************************************************************************

**********************************************************************************************
**/
public class Plugin_HPK_BPUL extends ArchivePlugin {

  int realNumFiles = 0;
  int numFiles = 0;
  int[] offsets;
  int[] lengths;
  String[] names;
  boolean[] hasName;

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public Plugin_HPK_BPUL() {

    super("HPK_BPUL", "HPK_BPUL");

    //         read write replace rename
    setProperties(true, false, false, false);

    setGames("Grand Ages: Rome",
        "Tropico 3",
        "Tropico 4",
        "Tropico 5");
    setExtensions("hpk"); // MUST BE LOWER CASE
    setPlatforms("PC");

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

      // Header
      if (fm.readString(4).equals("BPUL")) {
        rating += 50;
      }

      // 4 - Header Length (36)
      if (fm.readInt() == 36) {
        rating += 5;
      }

      fm.skip(20);

      long arcSize = fm.getLength();

      // Directory Offset
      if (FieldValidator.checkOffset(fm.readInt(), arcSize)) {
        rating += 5;
      }

      // Directory Length
      if (FieldValidator.checkLength(fm.readInt(), arcSize)) {
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
      numFiles = 0;
      realNumFiles = 0;

      FileManipulator fm = new FileManipulator(path, false);

      long arcSize = fm.getLength();

      // 4 - Header (BPUL)
      // 4 - Header Length (36)
      // 4 - Unknown (1)
      // 4 - Unknown (-1)
      // 8 - null
      // 4 - Unknown (1)
      fm.skip(28);

      // 4 - Details Directory Offset
      int dirOffset = fm.readInt();
      FieldValidator.checkOffset(dirOffset, arcSize);

      // 4 - Details Directory Length
      int dirLength = fm.readInt();
      FieldValidator.checkLength(dirLength, arcSize);

      fm.seek(dirOffset);

      numFiles = dirLength / 8; // includes directories

      hasName = new boolean[numFiles];

      Resource[] resources = new Resource[numFiles];
      TaskProgressManager.setMaximum(numFiles);

      // Read all the Offsets and Lengths for files+directories
      offsets = new int[numFiles];
      lengths = new int[numFiles];
      names = new String[numFiles];

      for (int i = 0; i < numFiles; i++) {
        // 4 - File Offset
        int offset = fm.readInt();
        FieldValidator.checkOffset(offset, arcSize);
        offsets[i] = offset;

        // 4 - File Length
        int length = fm.readInt();
        FieldValidator.checkLength(length, arcSize);
        lengths[i] = length;

        hasName[i] = false;

        TaskProgressManager.setValue(i);
      }

      // Set a small buffer cause we're gonna jump around a lot
      fm.getBuffer().setBufferSize(256);

      // Now go to the filenames for the first entry, read them and if they're directories, iterate in to them.
      // Also repeats until all filenames are accounted for
      //for (int i = 0; i < numFiles; i++) {
      for (int i = 0; i < 1; i++) { // the other files are funny, not sure how to read them - lets just fail them and read "proper" archives only
        if (!hasName[i]) {
          hasName[i] = true;
          names[i] = null; // the root dir - don't want it added as a real file later on

          int offset = offsets[i];
          int length = lengths[i];

          if (offset != 0 && length != 0) {
            //System.out.println("Start processing names from " + i);
            readFilenames(fm, offset, length, "");
          }
          else {
            //System.out.println("Skipping empty " + i);
          }
        }
      }

      // Now go through and filter out all the directories and create the Resources

      // Loop through directory
      realNumFiles = 0;
      for (int i = 0; i < numFiles; i++) {
        String name = names[i];

        if (name == null) {
          // Directory - skip it
          continue;
        }

        int offset = offsets[i];
        int length = lengths[i];

        if (offset != 0 && length != 0) {
          resources[realNumFiles] = new Resource(path, name, offset, length);
          realNumFiles++;
        }
      }

      resources = resizeResources(resources, realNumFiles);

      // Now go through and work out if the files are compressed or not
      for (int i = 0; i < realNumFiles; i++) {
        Resource resource = resources[i];

        long relativeOffset = resource.getOffset();

        fm.seek(relativeOffset);

        // 4 - Compression Header
        if (fm.readString(4).equals("ZLIB")) {
          // compressed

          // 4 - Decompressed Length
          int decompLength = fm.readInt();
          FieldValidator.checkLength(decompLength);

          // 4 - Decompressed Block Size
          int blockSize = fm.readInt();
          FieldValidator.checkLength(blockSize);

          int numBlocks = decompLength / blockSize;
          int lastBlockSize = decompLength % blockSize;

          if (lastBlockSize != 0) {
            numBlocks++;
          }

          long[] offsets = new long[numBlocks];
          long[] lengths = new long[numBlocks];
          long[] decompLengths = new long[numBlocks];

          for (int b = 0; b < numBlocks; b++) {
            // 4 - Compressed Block Offset
            long offset = relativeOffset + fm.readInt();
            FieldValidator.checkOffset(offset, arcSize);
            offsets[b] = offset;
            decompLengths[b] = blockSize;
          }

          if (lastBlockSize != 0) {
            decompLengths[numBlocks - 1] = lastBlockSize;
          }

          for (int b = 1; b < numBlocks; b++) {
            lengths[b - 1] = offsets[b] - offsets[b - 1];
          }
          lengths[numBlocks - 1] = resource.getLength() - (offsets[numBlocks - 1] - relativeOffset);

          // Set the properties on the resource
          resource.setDecompressedLength(decompLength);
          resource.setExporter(new BlockExporterWrapper(exporter, offsets, lengths, decompLengths));

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

   **********************************************************************************************
   **/

  public void readFilenames(FileManipulator fm, int offset, int length, String dirName) {
    try {

      fm.seek(offset);

      int bytesRead = 0;

      int[] dirFileIDs = new int[numFiles];
      String[] dirNames = new String[numFiles];
      int numDirs = 0;

      while (bytesRead < length) {
        // 4 - File ID
        int fileID = fm.readInt() - 1; // -1, as the first file is "1"
        FieldValidator.checkRange(fileID, 0, numFiles);

        // 4 - Entry Type (0=File, 1=Directory)
        int entryType = fm.readInt();
        FieldValidator.checkRange(entryType, 0, 1);

        // 2 - Filename Length
        short filenameLength = fm.readShort();
        FieldValidator.checkFilenameLength(filenameLength + 1); // +1 to allow empty filenames

        // X - Filename
        String name = fm.readString(filenameLength);

        bytesRead += 10 + filenameLength;

        if (entryType == 0) {
          // File
          names[fileID] = dirName + name;
          hasName[fileID] = true;

          //System.out.println("File " + fileID + " has name " + dirName + name);
        }
        else {
          // Directory
          dirFileIDs[numDirs] = fileID;
          dirNames[numDirs] = dirName + name + "\\";
          numDirs++;

          // Clear the name from the names[] so we know it's a directory, not a real file
          names[fileID] = null;
          hasName[fileID] = true;

          //System.out.println("File " + fileID + " is a Directory with name " + dirName + name);
        }

        TaskProgressManager.setValue(fileID);
      }

      // Now go through all the directories and read those filenames
      for (int i = 0; i < numDirs; i++) {
        int dirFileID = dirFileIDs[i];
        readFilenames(fm, offsets[dirFileID], lengths[dirFileID], dirNames[i]);
      }

    }
    catch (Throwable t) {
      //logError(t);
    }
  }

}
