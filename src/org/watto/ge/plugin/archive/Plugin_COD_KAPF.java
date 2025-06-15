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

import org.watto.datatype.FileType;
import org.watto.datatype.Resource;
import org.watto.ge.helper.FieldValidator;
import org.watto.ge.plugin.ArchivePlugin;
import org.watto.io.FileManipulator;
import org.watto.io.buffer.ByteBuffer;
import org.watto.task.TaskProgressManager;

/**
**********************************************************************************************

**********************************************************************************************
**/
public class Plugin_COD_KAPF extends ArchivePlugin {

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public Plugin_COD_KAPF() {

    super("COD_KAPF", "COD_KAPF");

    //         read write replace rename
    setProperties(true, false, false, false);

    setGames("Call of Duty 3");
    setExtensions("cod"); // MUST BE LOWER CASE
    setPlatforms("XBox 360",
        "PS3");

    // MUST BE LOWER CASE !!!
    setFileTypes(new FileType("nch", "Compressed Archive", FileType.TYPE_OTHER),
        new FileType("apkf", "APKF Archive", FileType.TYPE_ARCHIVE));

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
      if (fm.readString(4).equals("KAPF")) {
        rating += 50;
      }

      // 4 - Version? (Float) (2.06)
      if (fm.readFloat() == 2.06) {
        rating += 5;
      }

      long arcSize = fm.getLength();

      // 4 - Directory List Offset
      if (FieldValidator.checkOffset(fm.readInt(), arcSize)) {
        rating += 5;
      }

      // 4 - Number of Directories
      if (FieldValidator.checkNumFiles(fm.readInt())) {
        rating += 5;
      }

      // 4 - Details Directory Offset
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
  @SuppressWarnings("unused")
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

      // 4 - Header (KAPF)
      // 4 - Version? (Float) (2.06)
      fm.skip(8);

      // 4 - Directory List Offset
      int dirOffset = fm.readInt();
      FieldValidator.checkOffset(dirOffset, arcSize);

      // 4 - Number of Directories
      int numDirs = fm.readInt();
      FieldValidator.checkNumFiles(numDirs);

      // 4 - Details Directory Offset
      fm.skip(4);

      // 4 - Source Filename Directory Offset
      int nameDirOffset = fm.readInt();
      FieldValidator.checkOffset(nameDirOffset, arcSize);

      // 4 - Directory End Offset
      int nameDirLength = fm.readInt() - nameDirOffset;
      FieldValidator.checkLength(nameDirLength, arcSize);

      // 20 - Description ("CODAUTO30" + nulls to fill)
      fm.seek(nameDirOffset);

      byte[] nameDirBytes = fm.readBytes(nameDirLength);
      FileManipulator nameFM = new FileManipulator(new ByteBuffer(nameDirBytes));

      fm.seek(dirOffset);

      //
      // Read Directory List
      //

      String[] dirNames = new String[numDirs];
      int[] dirNumBlocks = new int[numDirs];
      int[] dirNumFiles = new int[numDirs];
      int[] dirBlockOffsets = new int[numDirs];
      int[] dirDetailsOffsets = new int[numDirs];
      int numFiles = 0; // to total up all the files in each directory

      for (int d = 0; d < numDirs; d++) {
        // 12 - Directory Name (null terminated, filled with nulls) (can be null)
        String dirName = fm.readNullString(12);

        // 2 - Number of Blocks in this Directory
        short numBlocks = fm.readShort();
        FieldValidator.checkNumFiles(numBlocks);
        dirNumBlocks[d] = numBlocks;

        // 2 - Number of Files in this Directory
        short numDirFiles = fm.readShort();
        FieldValidator.checkNumFiles(numDirFiles);
        dirNumFiles[d] = numDirFiles;

        if (numBlocks > 1) { // a single APKF file (indicated by the APKF header) or a single compressed APKF file (with NCH header)
          numFiles += numBlocks;
        }
        else {
          numFiles += numDirFiles;
        }

        // 4 - Blocks Directory Offset for this Directory
        int blockOffset = fm.readInt();
        FieldValidator.checkOffset(blockOffset, arcSize);
        dirBlockOffsets[d] = blockOffset;

        // 4 - Details Directory Offset for this Directory
        int detailsOffset = fm.readInt();
        FieldValidator.checkOffset(detailsOffset, arcSize);
        dirDetailsOffsets[d] = detailsOffset;
      }

      FieldValidator.checkNumFiles(numFiles);

      //
      // Read Blocks Directory
      //

      int[][] blockOffsets = new int[numDirs][0];
      int[][] blockLengths = new int[numDirs][0];
      int[][] blockDecompLengths = new int[numDirs][0];
      boolean[][] blockCompressed = new boolean[numDirs][0];

      for (int d = 0; d < numDirs; d++) {
        fm.relativeSeek(dirBlockOffsets[d]); // should already be here, but just in case
        int numBlocks = dirNumBlocks[d];
        blockOffsets[d] = new int[numBlocks];
        blockLengths[d] = new int[numBlocks];
        blockDecompLengths[d] = new int[numBlocks];
        blockCompressed[d] = new boolean[numBlocks];

        for (int b = 0; b < numBlocks; b++) {
          // 4 - Relative File Data Offset for the Files in this Directory
          int blockOffset = fm.readInt();
          FieldValidator.checkOffset(blockOffset, arcSize);
          blockOffsets[d][b] = blockOffset;

          // 4 - Decompressed Length of all File Data for all Files in this Directory
          int decompLength = fm.readInt();
          FieldValidator.checkLength(decompLength);

          // 4 - Decompressed Length of all File Data for all Files in this Directory (including padding) (or -1 if not compressed)
          // 4 - Unknown (0/128/130/132)
          fm.skip(8);

          // 4 - Compressed Length of all File Data for all Files in this Directory (0 = not compressed)
          int compLength = fm.readInt();
          FieldValidator.checkLength(compLength, arcSize);

          if (compLength == 0) {
            blockLengths[d][b] = decompLength;
            blockDecompLengths[d][b] = decompLength;
            blockCompressed[d][b] = false;
          }
          else {
            blockLengths[d][b] = compLength;
            blockDecompLengths[d][b] = decompLength;
            blockCompressed[d][b] = true;
          }

          // 12 - null
          fm.skip(12);
        }
      }

      //
      // Read Details Directory
      //

      Resource[] resources = new Resource[numFiles];
      TaskProgressManager.setMaximum(numFiles);

      int realNumFiles = 0;

      for (int d = 0; d < numDirs; d++) {
        fm.relativeSeek(dirDetailsOffsets[d]);
        int numFilesInDir = dirNumFiles[d];
        int numBlocks = dirNumBlocks[d];

        if (numBlocks > 1) {
          // a single APKF file (indicated by the APKF header) or a single compressed APKF file (with NCH header)

          for (int b = 0; b < numBlocks; b++) {
            // list each block separately
            int offset = blockOffsets[d][b];
            int length = blockLengths[d][b];
            int decompLength = blockDecompLengths[d][b];
            boolean compressed = blockCompressed[d][b];

            String filename = Resource.generateFilename(realNumFiles);
            if (compressed) {
              filename += ".nch";
            }
            else {
              filename += ".apkf";
            }

            //path,name,offset,length,decompLength,exporter
            resources[realNumFiles] = new Resource(path, filename, offset, length, decompLength);

            TaskProgressManager.setValue(realNumFiles);

            realNumFiles++;
          }

          continue;
        }

        for (int f = 0; f < numFilesInDir; f++) {
          // 4 - Filename Offset
          fm.skip(4);

          // 4 - Source Filename Offset
          int nameOffset = fm.readInt() - nameDirOffset;
          FieldValidator.checkOffset(nameOffset, nameDirLength);

          nameFM.seek(nameOffset);
          String filename = nameFM.readNullString();
          FieldValidator.checkFilename(filename);

          /*
          int assetIndex = filename.indexOf("\\assets\\");
          if (assetIndex > 0) {
            assetIndex += 8;
            filename = filename.substring(assetIndex);
          }
          */
          if (filename.toLowerCase().startsWith("c:\\")) {
            filename = filename.substring(3);
          }

          //filename = dirName + filename;

          // 4 - File Length
          int length = fm.readInt();
          FieldValidator.checkLength(length, arcSize);

          // 4 - File Data Block Number for the Block that contains this file data (0-based index)
          int blockNumber = fm.readInt();
          FieldValidator.checkRange(blockNumber, 0, numBlocks);

          int offset = 0;
          if (numBlocks == 1) {
            //   4 - File Offset
            offset = fm.readInt() + blockOffsets[d][blockNumber];
            FieldValidator.checkOffset(offset, arcSize);
          }
          else { //has multiple data blocks for this directory
            //   2 - File ID Number of the file in this directory (ie the order) (0-based index)
            //   2 - Part ID Number (if a file is broken into multiple parts) (0-based index, for the entries with the same File ID Number)
            fm.skip(4);

            offset = blockOffsets[d][blockNumber];
            FieldValidator.checkOffset(offset, arcSize);
          }

          //path,name,offset,length,decompLength,exporter
          resources[realNumFiles] = new Resource(path, filename, offset, length);

          TaskProgressManager.setValue(realNumFiles);

          realNumFiles++;

        }
      }

      nameFM.close();

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
