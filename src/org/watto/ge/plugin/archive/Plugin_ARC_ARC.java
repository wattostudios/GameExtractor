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
import org.watto.datatype.FileType;
import org.watto.datatype.Resource;
import org.watto.ge.helper.FieldValidator;
import org.watto.ge.plugin.ArchivePlugin;
import org.watto.ge.plugin.ExporterPlugin;
import org.watto.ge.plugin.exporter.BlockExporterWrapper;
import org.watto.ge.plugin.exporter.Exporter_ZLib;
import org.watto.io.FileManipulator;
import org.watto.io.buffer.ByteBuffer;
import org.watto.task.TaskProgressManager;

/**
**********************************************************************************************

**********************************************************************************************
**/
public class Plugin_ARC_ARC extends ArchivePlugin {

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public Plugin_ARC_ARC() {

    super("ARC_ARC", "ARC_ARC");

    //         read write replace rename
    setProperties(true, false, false, false);

    setGames("Titan Quest");
    setExtensions("arc");
    setPlatforms("PC");

    setFileTypes(new FileType("tex", "Texture Image", FileType.TYPE_IMAGE));

    //setTextPreviewExtensions("colours", "rat", "screen", "styles"); // LOWER CASE

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
      if (fm.readString(4).equals("ARC" + (char) 0)) {
        rating += 50;
      }

      // Version
      if (fm.readInt() == 1) {
        rating += 5;
      }

      // Number Of Files
      if (FieldValidator.checkNumFiles(fm.readInt())) {
        rating += 5;
      }

      // Number Of Files
      if (FieldValidator.checkNumFiles(fm.readInt())) {
        rating += 5;
      }

      fm.skip(4);

      long arcSize = fm.getLength();

      // Directory Length
      if (FieldValidator.checkLength(fm.readInt(), arcSize)) {
        rating += 5;
      }

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

      ExporterPlugin exporter = Exporter_ZLib.getInstance();

      // RESETTING GLOBAL VARIABLES

      FileManipulator fm = new FileManipulator(path, false);

      long arcSize = fm.getLength();

      // 4 - Header (ARC + null)
      // 4 - Version (1)
      fm.skip(8);

      // 4 - Number Of Files
      int numFiles = fm.readInt();
      FieldValidator.checkNumFiles(numFiles);
      if (numFiles < 3) {
        return null; // so it errors-out some other files that pick this up
      }

      // 4 - Number Of Compressed Blocks
      int numBlocks = fm.readInt();
      FieldValidator.checkNumFiles(numBlocks);
      if (numBlocks < 3) {
        return null; // so it errors-out some other files that pick this up
      }

      // 4 - Compression Directory Length
      int compressionDirLength = fm.readInt();
      FieldValidator.checkLength(compressionDirLength, arcSize);

      // 4 - Filename Directory Length
      int filenameDirLength = fm.readInt();
      FieldValidator.checkLength(filenameDirLength, arcSize);

      // 4 - Compression Directory Offset
      int compressionDirOffset = fm.readInt();
      FieldValidator.checkOffset(compressionDirOffset, arcSize);

      fm.seek(compressionDirOffset);

      int[] blockOffsets = new int[numBlocks];
      int[] blockLengths = new int[numBlocks];
      int[] blockDecompLengths = new int[numBlocks];

      for (int i = 0; i < numBlocks; i++) {
        // 4 - Compressed Block Offset
        int blockOffset = fm.readInt();
        FieldValidator.checkOffset(blockOffset, arcSize);
        blockOffsets[i] = blockOffset;

        // 4 - Compressed Block Length
        int blockLength = fm.readInt();
        FieldValidator.checkLength(blockLength, arcSize);
        blockLengths[i] = blockLength;

        // 4 - Decompressed Block Length
        int blockDecompLength = fm.readInt();
        FieldValidator.checkLength(blockDecompLength);
        blockDecompLengths[i] = blockDecompLength;
      }

      fm.seek(compressionDirOffset + compressionDirLength); // just in case

      byte[] nameDirBytes = fm.readBytes(filenameDirLength);
      FileManipulator nameFM = new FileManipulator(new ByteBuffer(nameDirBytes));

      Resource[] resources = new Resource[numFiles];
      TaskProgressManager.setMaximum(numFiles);

      // Loop through directory
      for (int i = 0; i < numFiles; i++) {
        // 4 - Unknown (3)
        fm.skip(4);

        // 4 - File Offset
        long offset = fm.readInt();
        FieldValidator.checkOffset(offset, arcSize);

        // 4 - Compressed File Length
        long length = fm.readInt();
        FieldValidator.checkLength(length, arcSize);

        // 4 - Decompressed File Length
        long decompLength = fm.readInt();
        FieldValidator.checkLength(decompLength);

        // 8 - CRC?
        // 4 - Unknown
        fm.skip(12);

        // 4 - Number of Compressed Blocks
        int numBlocksForFile = fm.readInt();
        FieldValidator.checkRange(numBlocksForFile, 0, numBlocks);

        // 4 - ID of First Compressed Block
        int firstBlockForFile = fm.readInt();
        FieldValidator.checkRange(firstBlockForFile, 0, numBlocks);

        // 4 - Filename Length
        int filenameLength = fm.readInt();
        FieldValidator.checkFilenameLength(filenameLength);

        // 4 - Filename Offset (relative to the start of the filename directory)
        long filenameOffset = fm.readInt();
        FieldValidator.checkOffset(filenameOffset, filenameDirLength);

        nameFM.seek(filenameOffset);
        String filename = nameFM.readNullString(filenameLength);

        if (length == decompLength) {
          // uncompressed

          //path,name,offset,length,decompLength,exporter
          resources[i] = new Resource(path, filename, offset, length, decompLength);
        }
        else if (numBlocksForFile == 1) {
          // single compressed block

          //path,name,offset,length,decompLength,exporter
          resources[i] = new Resource(path, filename, offset, length, decompLength, exporter);
        }
        else {
          // multiple compressed blocks
          long[] offsets = new long[numBlocksForFile];
          long[] lengths = new long[numBlocksForFile];
          long[] decompLengths = new long[numBlocksForFile];

          for (int b = 0; b < numBlocksForFile; b++) {
            int blockNum = firstBlockForFile + b;
            offsets[b] = blockOffsets[blockNum];
            lengths[b] = blockLengths[blockNum];
            decompLengths[b] = blockDecompLengths[blockNum];
          }

          BlockExporterWrapper blockExporter = new BlockExporterWrapper(exporter, offsets, lengths, decompLengths);

          //path,name,offset,length,decompLength,exporter
          resources[i] = new Resource(path, filename, offset, length, decompLength, blockExporter);
        }

        TaskProgressManager.setValue(i);
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

}
