/*
 * Application:  Game Extractor
 * Author:       wattostudios
 * Website:      http://www.watto.org
 * Copyright:    Copyright (c) 2002-2022 wattostudios
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
import org.watto.Language;
import org.watto.datatype.Resource;
import org.watto.ge.helper.FieldValidator;
import org.watto.ge.plugin.ArchivePlugin;
import org.watto.ge.plugin.ExporterPlugin;
import org.watto.ge.plugin.exporter.BlockExporterWrapper;
import org.watto.ge.plugin.exporter.Exporter_ZLib;
import org.watto.ge.plugin.exporter.Exporter_ZLib_CompressedSizeOnly;
import org.watto.io.FileManipulator;
import org.watto.task.TaskProgressManager;

/**
**********************************************************************************************

**********************************************************************************************
**/
public class Plugin_FCL extends ArchivePlugin {

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public Plugin_FCL() {

    super("FCL", "FCL");

    //         read write replace rename
    setProperties(true, true, false, false);

    setGames("Lead and Gold: Gangs of the Wild West");
    setExtensions("fcl"); // MUST BE LOWER CASE
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

      // Number Of Files
      if (FieldValidator.checkNumFiles(fm.readInt())) {
        rating += 5;
      }

      // 4 - Unknown (65536)
      if (fm.readInt() == 65536) {
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

      ExporterPlugin exporter = Exporter_ZLib_CompressedSizeOnly.getInstance();

      // RESETTING GLOBAL VARIABLES

      FileManipulator fm = new FileManipulator(path, false);

      long arcSize = fm.getLength();

      // 4 - Number Of Files
      int numFiles = fm.readInt();
      FieldValidator.checkNumFiles(numFiles);

      // 4 - Unknown (65536)
      int blockSize = fm.readInt();
      FieldValidator.checkLength(blockSize, arcSize);

      TaskProgressManager.setMaximum(numFiles);

      long[] offsets = new long[numFiles];
      long[] lengths = new long[numFiles];
      long[] decompLengths = new long[numFiles];

      // Loop through directory
      for (int i = 0; i < numFiles; i++) {

        // 4 - File Offset
        int offset = fm.readInt();
        FieldValidator.checkOffset(offset, arcSize);
        offsets[i] = offset;

        // 4 - File Length
        int length = fm.readInt();
        FieldValidator.checkLength(length, arcSize);
        lengths[i] = length;

        decompLengths[i] = blockSize;

        TaskProgressManager.setValue(i);
      }

      String filename = path.getName() + "_uncompressed";

      long offset = (numFiles + 1) * 8;
      long compLength = arcSize - offset;
      long decompLength = numFiles * blockSize;

      BlockExporterWrapper blockExporter = new BlockExporterWrapper(exporter, offsets, lengths, decompLengths);

      //path,name,offset,length,decompLength,exporter
      Resource[] resources = new Resource[1];
      resources[0] = new Resource(path, filename, offset, compLength, decompLength, blockExporter);

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
   * Writes an [archive] File with the contents of the Resources
   **********************************************************************************************
   **/
  @Override
  public void write(Resource[] resources, File path) {
    try {

      FileManipulator fm = new FileManipulator(path, true);
      int numFiles = resources.length;

      // Calculations
      TaskProgressManager.setMessage(Language.get("Progress_PerformingCalculations"));

      long filesSize = 0;
      for (int i = 0; i < numFiles; i++) {
        filesSize += resources[i].getDecompressedLength() + 4; // +4 for the block decomp length at the end of each block
      }

      int blockSize = 65536;
      int numBlocks = (int) (filesSize / blockSize);
      if (filesSize % blockSize != 0) {
        numBlocks++;
      }

      TaskProgressManager.setMaximum(numBlocks);

      // Write Header Data

      // 4 - Number Of Blocks
      fm.writeInt(numBlocks);

      // 4 - Decompressed Block Size (65536)
      fm.writeInt(65536);

      // Write Directory
      TaskProgressManager.setMessage(Language.get("Progress_WritingDirectory"));
      for (int i = 0; i < numBlocks; i++) {
        // Just write dummies for now - we'll come back and fix this later, after we write the compressed lengths 

        // 4 - File Offset
        fm.writeInt(0);

        // 4 - File Length
        fm.writeInt(0);

      }

      // Write Files
      TaskProgressManager.setMessage(Language.get("Progress_WritingFiles"));

      // make dummy resources for the raw file, each of <blocksize>, so we can feed it easily into the exporter below

      Resource[] blocks = new Resource[numBlocks];
      long remainingLength = filesSize;
      long offset = 0;
      File sourcePath = resources[0].getSource();
      for (int i = 0; i < numBlocks; i++) {
        long length = remainingLength;
        if (remainingLength > blockSize) {
          length = blockSize;
        }
        blocks[i] = new Resource(sourcePath, "", offset, length);

        remainingLength -= blockSize;
        offset += blockSize;
      }

      ExporterPlugin exporter = new Exporter_ZLib();
      //long[] compressedLengths = write(exporter, blocks, fm);

      long[] compressedLengths = new long[blocks.length];
      remainingLength = filesSize;
      for (int i = 0; i < numBlocks; i++) {
        // X - File Data (ZLib Compressed)
        long compLength = write(exporter, blocks[i], fm);
        TaskProgressManager.setValue(i);
        compressedLengths[i] = compLength + 4; // +4 for the block size after the file

        // 4 - Block Size
        long length = remainingLength;
        if (remainingLength > blockSize) {
          length = blockSize;
        }

        fm.writeInt((int) length);

        remainingLength -= blockSize;

      }

      // go back and write the directory
      fm.seek(8);

      // Write Directory
      TaskProgressManager.setMessage(Language.get("Progress_WritingDirectory"));
      offset = 8 + (numBlocks * 8);
      for (int i = 0; i < numBlocks; i++) {
        long length = compressedLengths[i];

        // 4 - File Offset
        fm.writeInt((int) offset);

        // 4 - File Length
        fm.writeInt((int) length);

        offset += length;
      }

      fm.close();

    }
    catch (Throwable t) {
      logError(t);
    }
  }

}
