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
import org.watto.datatype.FileType;
import org.watto.datatype.Resource;
import org.watto.ge.helper.FieldValidator;
import org.watto.ge.plugin.ArchivePlugin;
import org.watto.ge.plugin.ExporterPlugin;
import org.watto.ge.plugin.exporter.BlockVariableExporterWrapper;
import org.watto.ge.plugin.exporter.Exporter_Default;
import org.watto.ge.plugin.exporter.Exporter_ZLib_CompressedSizeOnly;
import org.watto.io.FileManipulator;
import org.watto.io.converter.IntConverter;
import org.watto.task.TaskProgressManager;

/**
**********************************************************************************************

**********************************************************************************************
**/
public class Plugin_GOB_STBL extends ArchivePlugin {

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public Plugin_GOB_STBL() {

    super("GOB_STBL", "GOB_STBL");

    //         read write replace rename
    setProperties(true, false, false, false);

    setGames("Crash Nitro Racing",
        "Doom 3");
    setExtensions("gob"); // MUST BE LOWER CASE
    setPlatforms("PS2", "XBox");

    setTextPreviewExtensions("asd", "csv", "int", "psh", "vim", "script", "pd", "gui", "md5camera", "md5mesh"); // LOWER CASE

    setFileTypes(new FileType("vag", "VAG Audio", FileType.TYPE_AUDIO));

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

      getDirectoryFile(fm.getFile(), "gfc");
      rating += 25;

      // Header
      if (fm.readString(4).equals("STBL")) {
        rating += 50;
      }

      // Compression Header
      if (fm.readString(2).equals("zx")) {
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

      long arcSize = (int) path.length();

      File sourcePath = getDirectoryFile(path, "gfc");
      FileManipulator fm = new FileManipulator(sourcePath, false);

      // 4 - Unknown
      // 4 - Length of GOB File
      fm.skip(8);

      // 4 - Number of Blocks
      int numBlocks = IntConverter.changeFormat(fm.readInt());
      FieldValidator.checkNumFiles(numBlocks);

      // 4 - Number of Files
      int numFiles = IntConverter.changeFormat(fm.readInt());
      FieldValidator.checkNumFiles(numFiles);

      // Read the blocks
      int[] blockOffsets = new int[numBlocks];
      int[] blockLengths = new int[numBlocks];

      for (int i = 0; i < numBlocks; i++) {

        // 4 - Block Compressed Length (including the block headers and footer)
        int blockLength = IntConverter.changeFormat(fm.readInt());
        FieldValidator.checkLength(blockLength, arcSize);

        blockLengths[i] = blockLength;

        // 4 - Block Offset
        int blockOffset = IntConverter.changeFormat(fm.readInt());
        FieldValidator.checkOffset(blockOffset, arcSize);

        blockOffsets[i] = blockOffset;

        // 4 - Max Block Decompressed Length? (32767)
        fm.skip(4);
      }

      // Open the GOB file so we can get the compression types
      FileManipulator gobFM = new FileManipulator(path, false, 5); // small quick reads
      String[] blockCompType = new String[numBlocks];
      for (int i = 0; i < numBlocks; i++) {
        gobFM.seek(blockOffsets[i]);

        // 4 - Compression Header
        String header = gobFM.readString(4);

        if (header.equals("STBL")) {
          blockCompType[i] = gobFM.readString(1);
          blockOffsets[i] += 5;
          blockLengths[i] -= 9;
        }
        else {
          blockCompType[i] = header.substring(0, 1);
          blockOffsets[i] += 1;
          blockLengths[i] -= 1;
        }
      }
      gobFM.close();

      // HASH DIRECTORY
      fm.skip(numBlocks * 4);

      Resource[] resources = new Resource[numFiles];
      TaskProgressManager.setMaximum(numFiles);

      int[] decompLengths = new int[numFiles];
      int[] blockIDs = new int[numFiles + 1]; // +1 so we can add the numBlocks to this array to easily calculate file sizes

      // Loop through the details directory
      for (int i = 0; i < numFiles; i++) {

        // 4 - Hash
        fm.skip(4);

        // 4 - Decompressed File Length
        int decompLength = IntConverter.changeFormat(fm.readInt());
        FieldValidator.checkLength(decompLength);

        decompLengths[i] = decompLength;

        // 4 - First Block ID of this File
        int blockID = IntConverter.changeFormat(fm.readInt());
        FieldValidator.checkRange(blockID, 0, numBlocks);

        blockIDs[i] = blockID;

        TaskProgressManager.setValue(i);
      }

      // add the numBlocks to the array, to easily calculate file sizes later on
      blockIDs[numFiles] = numBlocks;

      ExporterPlugin exporterZLib = Exporter_ZLib_CompressedSizeOnly.getInstance();
      ExporterPlugin exporterDefault = Exporter_Default.getInstance();

      // Loop through the filename directory
      long namesOffset = fm.getOffset();
      try {
        for (int i = 0; i < numFiles; i++) {
          // 72 - Filename (null)
          String filename = fm.readNullString(72);
          FieldValidator.checkFilename(filename);

          if (filename.startsWith(".\\")) {
            filename = filename.substring(2);
          }

          int firstBlock = blockIDs[i];
          int nextBlock = blockIDs[i + 1];

          int blockCount = nextBlock - firstBlock;

          long[] thisBlockOffsets = new long[blockCount];
          long[] thisBlockLengths = new long[blockCount];
          ExporterPlugin[] thisBlockExporters = new ExporterPlugin[blockCount];

          int compLength = 0;
          for (int b = 0; b < blockCount; b++) {
            thisBlockOffsets[b] = blockOffsets[firstBlock + b];
            if (blockCompType[firstBlock + b].equals("z")) {
              thisBlockExporters[b] = exporterZLib;
            }
            else {
              //System.out.println(blockCompType[firstBlock + b]);
              thisBlockExporters[b] = exporterDefault;
            }

            int thisBlockLength = blockLengths[firstBlock + b];
            thisBlockLengths[b] = thisBlockLength;
            compLength += thisBlockLength;
          }

          int offset = (int) thisBlockOffsets[0];
          int length = compLength;
          int decompLength = decompLengths[i];

          BlockVariableExporterWrapper exporter = new BlockVariableExporterWrapper(thisBlockExporters, thisBlockOffsets, thisBlockLengths, thisBlockLengths);

          //path,name,offset,length,decompLength,exporter
          resources[i] = new Resource(path, filename, offset, length, decompLength, exporter);

          TaskProgressManager.setValue(i);
        }
      }
      catch (Throwable t) {
        // Maybe filenames have a length of 264
        fm.seek(namesOffset);

        for (int i = 0; i < numFiles; i++) {
          // 264 - Filename (null)
          String filename = fm.readNullString(264);
          FieldValidator.checkFilename(filename);

          if (filename.startsWith(".\\")) {
            filename = filename.substring(2);
          }

          int firstBlock = blockIDs[i];
          int nextBlock = blockIDs[i + 1];

          int blockCount = nextBlock - firstBlock;

          long[] thisBlockOffsets = new long[blockCount];
          long[] thisBlockLengths = new long[blockCount];
          ExporterPlugin[] thisBlockExporters = new ExporterPlugin[blockCount];

          int compLength = 0;
          for (int b = 0; b < blockCount; b++) {
            thisBlockOffsets[b] = blockOffsets[firstBlock + b];
            if (blockCompType[firstBlock + b].equals("z")) {
              thisBlockExporters[b] = exporterZLib;
            }
            else {
              //System.out.println(blockCompType[firstBlock + b]);
              thisBlockExporters[b] = exporterDefault;
            }

            int thisBlockLength = blockLengths[firstBlock + b];
            thisBlockLengths[b] = thisBlockLength;
            compLength += thisBlockLength;
          }

          int offset = (int) thisBlockOffsets[0];
          int length = compLength;
          int decompLength = decompLengths[i];

          BlockVariableExporterWrapper exporter = new BlockVariableExporterWrapper(thisBlockExporters, thisBlockOffsets, thisBlockLengths, thisBlockLengths);

          //path,name,offset,length,decompLength,exporter
          resources[i] = new Resource(path, filename, offset, length, decompLength, exporter);

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

}
