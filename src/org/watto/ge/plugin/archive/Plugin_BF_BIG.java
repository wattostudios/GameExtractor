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
import org.watto.ge.plugin.exporter.Exporter_LZO_SingleBlock;
import org.watto.io.FileManipulator;
import org.watto.task.TaskProgressManager;

/**
**********************************************************************************************

**********************************************************************************************
**/
public class Plugin_BF_BIG extends ArchivePlugin {

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public Plugin_BF_BIG() {

    super("BF_BIG", "BF_BIG");

    //         read write replace rename
    setProperties(true, false, false, false);

    setExtensions("bf");
    setGames("Beyond Good and Evil",
        "Prince Of Persia: Sands Of Time",
        "Prince Of Persia: Warrior Within",
        "Prince Of Persia: The Two Thrones",
        "TMNT (Teenage Mutant Ninja Turtles)");
    setPlatforms("PC");

    // MUST BE LOWER CASE !!!
    setFileTypes(new FileType("waa", "Ambiance Audio File", FileType.TYPE_AUDIO),
        new FileType("wac", "Audio File", FileType.TYPE_AUDIO),
        new FileType("wad", "Dialog Audio File", FileType.TYPE_AUDIO),
        new FileType("wam", "Music Audio File", FileType.TYPE_AUDIO));

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
      if (fm.readString(4).equals("BIG" + (char) 0)) {
        rating += 50;
      }

      fm.skip(4);

      // Number Of Files
      int numFiles = fm.readInt();
      if (FieldValidator.checkNumFiles(numFiles)) {
        rating += 5;
      }

      // 4 - Unknown (5)
      // 8 - null
      // 8 - Unknown (all 255s)
      // 4 - Unknown
      // 4 - Unknown (1)
      // 4 - Unknown
      // 4 - numFiles
      // 4 - Unknown (5)
      fm.skip(40);

      // 4 - Header Length (68)
      if (fm.readInt() == 68) {
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
  
  **********************************************************************************************
  **/
  @Override
  public Resource[] read(File path) {
    try {

      addFileTypes();

      FileManipulator fm = new FileManipulator(path, false);

      // 4 - Header ("BIG" + null)
      fm.skip(4);

      // 4 - Unknown (34)
      int version = fm.readInt();

      // 4 - Number Of Files (NOT including the empty padding entries)
      int numFiles = fm.readInt();
      FieldValidator.checkNumFiles(numFiles);

      // 4 - Number Of Folders (NOT including the empty padding entries)
      int numFolders = fm.readInt();
      FieldValidator.checkNumFiles(numFolders / 5);

      // 8 - null
      // 8 - Unknown (-1)
      fm.skip(16);

      // 4 - Number of Files (including the empty padding entries)
      int numFilesIncluding = fm.readInt();
      FieldValidator.checkNumFiles(numFilesIncluding / 5);

      // 4 - Unknown (1)
      // 4 - Unknown
      // 4 - Number Of Files (NOT including the empty padding entries)
      // 4 - Number Of Folders (NOT including the empty padding entries)
      // 4 - Header Length (68)
      // 4 - Unknown (-1)
      // 4 - null
      fm.skip(28);

      // 4 - Number of Folders (including the empty padding entries)
      int numFoldersIncluding = fm.readInt();
      FieldValidator.checkNumFiles(numFoldersIncluding / 5);

      long arcSize = fm.getLength();

      Resource[] resources = new Resource[numFiles];
      TaskProgressManager.setMaximum(numFiles);

      // Read the Offsets Directory
      int[] offsets = new int[numFiles];
      for (int i = 0; i < numFiles; i++) {
        // 4 - File Offset
        int offset = fm.readInt() + 4; // +4 to skip the 4-byte header on each file
        FieldValidator.checkOffset(offset, arcSize);
        offsets[i] = offset;

        // 2 - Unknown
        // 2 - Unknown
        fm.skip(4);
      }

      // skip the padding
      int paddingSize = (numFilesIncluding - numFiles) * 8;
      fm.skip(paddingSize);

      // Read the Filename Directory
      int[] folderIDs = new int[numFiles];
      for (int i = 0; i < numFiles; i++) {
        // 4 - File Length
        int length = fm.readInt();
        FieldValidator.checkLength(length, arcSize);

        // 4 - File ID? (incremental from 1)
        // 4 - Unknown
        fm.skip(8);

        // 4 - Parent Folder ID?
        int parentID = fm.readInt();
        folderIDs[i] = parentID;

        // 4 - Hash?
        fm.skip(4);

        // 64 - Filename (null terminated)
        String filename = fm.readNullString(64);

        if (version == 42) {
          // 4 - Unknown (3)
          // 36 - Checksum String (null terminated, filled with nulls)
          fm.skip(40);
        }

        int offset = offsets[i];

        //path,id,name,offset,length,decompLength,exporter
        resources[i] = new Resource(path, filename, offset, length);

        TaskProgressManager.setValue(i);
      }

      // skip the padding
      if (version == 42) {
        paddingSize = (numFilesIncluding - numFiles) * 124;
      }
      else {
        paddingSize = (numFilesIncluding - numFiles) * 84;
      }
      fm.skip(paddingSize);

      // Read the Folders Directory
      String[] folderNames = new String[numFolders];
      for (int i = 0; i < numFolders; i++) {
        // 4 - Unknown
        // 4 - Number of sub-folder entries under this Folder
        // 4 - Unknown
        // 4 - Unknown
        fm.skip(16);

        // 4 - Parent Folder ID (-1 for no parent)
        int parentID = fm.readInt();

        // 64 - Folder Name (null terminated)
        String folderName = fm.readNullString(64) + "\\";
        if (parentID == -1) {
          folderName = ""; // root
        }
        else {
          folderName = folderNames[parentID] + folderName;
        }

        folderNames[i] = folderName;
      }

      // Now set the folder names for each file
      for (int i = 0; i < numFiles; i++) {
        int parentID = folderIDs[i];
        if (parentID != -1) {
          Resource resource = resources[i];
          String name = folderNames[parentID] + resource.getName();
          resource.setName(name);
          resource.setOriginalName(name);
        }

      }

      /*
      // Calculate File Sizes
      for (int j = 0; j < numFiles - 1; j++) {
        Resource resource = resources[j];
        long length = resources[j + 1].getOffset() - resource.getOffset();
        FieldValidator.checkLength(length, arcSize);
        resource.setLength(length);
        resource.setDecompressedLength(length);
      }
      long length = arcSize - resources[numFiles - 1].getOffset();
      resources[numFiles - 1].setLength(length);
      resources[numFiles - 1].setDecompressedLength(length);
      */

      // Now go to each file offset, work out if it's compressed, and if so, decompress it
      fm.getBuffer().setBufferSize(17);

      ExporterPlugin exporterLZO = Exporter_LZO_SingleBlock.getInstance();
      ExporterPlugin exporterDefault = Exporter_Default.getInstance();

      for (int i = 0; i < numFiles; i++) {
        TaskProgressManager.setValue(i);

        Resource resource = resources[i];
        long offset = resource.getOffset();
        long endOffset = offset + resource.getLength() - 8; // slightly smaller, to account for padding at the end of each file

        // seek+skip, so the relativeSeek later is very quick
        fm.seek(offset);
        fm.skip(13);

        if (fm.readInt() == -285228903) {
          // compressed
        }
        else {
          // uncompressed
          continue;
        }

        fm.relativeSeek(offset);

        int maxBlocks = 50000;
        long[] blockOffsets = new long[maxBlocks];
        long[] blockLengths = new long[maxBlocks];
        long[] blockDecompLengths = new long[maxBlocks];
        ExporterPlugin[] blockExporters = new ExporterPlugin[maxBlocks];

        int totalDecompLength = 0;

        int numBlocks = 0;
        while (fm.getOffset() < endOffset) {
          // 4 - Decompressed Block Length
          int blockDecompLength = fm.readInt();
          FieldValidator.checkLength(blockDecompLength);

          if (blockDecompLength == 0) {
            break; // padding at end of file
          }

          // 4 - Compressed Block Length
          int blockCompLength = fm.readInt();
          FieldValidator.checkLength(blockCompLength, arcSize);

          // X - Compressed Data
          long blockOffset = fm.getOffset();
          fm.skip(blockCompLength);

          blockOffsets[numBlocks] = blockOffset;
          blockDecompLengths[numBlocks] = blockDecompLength;
          blockLengths[numBlocks] = blockCompLength;

          if (blockDecompLength == blockCompLength) {
            // raw block
            blockExporters[numBlocks] = exporterDefault;
          }
          else {
            // compressed block
            blockExporters[numBlocks] = exporterLZO;
          }

          totalDecompLength += blockDecompLength;

          numBlocks++;
        }

        // shink the arrays
        long[] tempLongArray = blockOffsets;
        blockOffsets = new long[numBlocks];
        System.arraycopy(tempLongArray, 0, blockOffsets, 0, numBlocks);

        tempLongArray = blockDecompLengths;
        blockDecompLengths = new long[numBlocks];
        System.arraycopy(tempLongArray, 0, blockDecompLengths, 0, numBlocks);

        tempLongArray = blockLengths;
        blockLengths = new long[numBlocks];
        System.arraycopy(tempLongArray, 0, blockLengths, 0, numBlocks);

        ExporterPlugin[] tempPluginArray = blockExporters;
        blockExporters = new ExporterPlugin[numBlocks];
        System.arraycopy(tempPluginArray, 0, blockExporters, 0, numBlocks);

        BlockVariableExporterWrapper exporter = new BlockVariableExporterWrapper(blockExporters, blockOffsets, blockLengths, blockDecompLengths);
        resource.setExporter(exporter);

        resource.setDecompressedLength(totalDecompLength);
      }

      fm.close();

      return resources;

    }
    catch (

    Throwable t) {
      logError(t);
      return null;

    }
  }

}