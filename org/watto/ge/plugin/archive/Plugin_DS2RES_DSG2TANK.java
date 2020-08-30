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
import org.watto.ge.plugin.exporter.BlockVariableExporterWrapper;
import org.watto.ge.plugin.exporter.Exporter_Default;
import org.watto.ge.plugin.exporter.Exporter_ZLib;
import org.watto.io.FileManipulator;
import org.watto.task.TaskProgressManager;

/**
**********************************************************************************************

**********************************************************************************************
**/
public class Plugin_DS2RES_DSG2TANK extends ArchivePlugin {

  long dirOffset = 0;

  int dir2Offset = 0;

  int realNumFiles = 0;

  int firstFileOffset = 0;

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public Plugin_DS2RES_DSG2TANK() {

    super("DS2RES_DSG2TANK", "DS2RES_DSG2TANK");

    //         read write replace rename
    setProperties(true, false, false, false);

    setGames("Dungeon Siege 2",
        "Space Siege");
    setExtensions("ds2res", "ssres", "ssmap");
    setPlatforms("PC");

    setFileTypes("gas", "GAS Programming Script",
        "inc", "Node Information",
        "lqd22", "Directory Placeholder",
        "skrit", "Formula Script",
        "tnp", "Tuning Data",
        "nnk", "Naming Key",
        "ldc6", "Dictionary Lookup Table",
        "gpg", "Startup Verification Script",
        "fx", "Graphic Effects Script",
        "flick", "Animation Script",
        "kfc", "Model Pose?",
        "flm", "Film Mesh Motion?",
        "asp", "3D Mesh",
        "wart", "Unknown",
        "sno", "SNOD file",
        "db", "Windows Thumbnail File",
        "prs", "Animation File");

    setTextPreviewExtensions("gas", "gpg", "nnk", "skrit", "bat", "as", "hlsl"); // LOWER CASE

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
      String header = fm.readString(8);
      if (header.equals("DSg2Tank") || header.equals("SSg1Tank")) {
        rating += 50;
      }

      fm.skip(4);

      long arcSize = fm.getLength();

      // Directory 1 Offset
      if (FieldValidator.checkOffset(fm.readInt(), arcSize)) {
        rating += 5;
      }

      // Directory 2 Offset
      if (FieldValidator.checkOffset(fm.readInt(), arcSize)) {
        rating += 5;
      }

      fm.skip(4);

      // First File Offset
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

      // NOTE - Compressed file MUST know their DECOMPRESSED LENGTH
      //      - Uncompressed files MUST know their LENGTH

      addFileTypes();
      //ExporterPlugin exporter = Exporter_Custom_DS2RES.getInstance();

      // RESETTING THE GLOBAL VARIABLES
      dirOffset = 0;
      dir2Offset = 0;
      realNumFiles = 0;
      firstFileOffset = 0;

      FileManipulator fm = new FileManipulator(path, false);

      long arcSize = (int) fm.getLength();

      // 8 - Header (DSg2Tank)
      // 4 - Unknown
      fm.skip(12);

      // 4 - Directory 1 Offset
      dirOffset = fm.readInt();
      FieldValidator.checkOffset(dirOffset, arcSize);

      // 4 - Directory 2 Offset
      dir2Offset = fm.readInt();
      FieldValidator.checkOffset(dir2Offset, arcSize);

      // 4 - Length Of Unknown Directory?
      fm.skip(4);

      // 4 - First File Offset
      firstFileOffset = fm.readInt();
      FieldValidator.checkOffset(firstFileOffset, arcSize);

      // 4 - Directory Length
      fm.seek(dir2Offset);

      // 4 - Number Of Files (from dir2offset)
      int numFiles = fm.readInt();
      FieldValidator.checkNumFiles(numFiles);

      Resource[] resources = new Resource[numFiles];

      TaskProgressManager.setMaximum(numFiles);

      fm.seek(dirOffset);

      // 4 - Number Of Sub-Directories
      int numSubDirs = fm.readInt();
      FieldValidator.checkNumFiles(numSubDirs);
      fm.skip(numSubDirs * 4);

      readDirectory(resources, fm, path, "");

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
  public void readDirectory(Resource[] resources, FileManipulator fm, File path, String parentDirName) throws Exception {

    //ExporterPlugin exporter = Exporter_Custom_DS2RES.getInstance();

    ExporterPlugin exporterZLib = Exporter_ZLib.getInstance();
    ExporterPlugin exporterDefault = Exporter_Default.getInstance();

    long arcSize = (int) fm.getLength();

    // 4 - Offset To Parent Directory
    fm.skip(4);

    // 4 - Number Of Files
    int numFiles = fm.readInt();
    FieldValidator.checkNumFiles(numFiles);

    // 8 - Hash?
    fm.skip(8);

    // 2 - dirName Length
    short dirNameLength = fm.readShort();
    FieldValidator.checkFilenameLength(dirNameLength + 1);

    // X - dirName
    String dirName = parentDirName + fm.readString(dirNameLength) + "\\";
    //System.out.println(fm.getOffset() + " - " + dirName);

    // 0-3 - null Padding to a multiple of 4 bytes
    int paddingSize = 4 - ((dirNameLength + 2) % 4);
    //if (paddingSize < 4){
    fm.skip(paddingSize);
    //  }

    long[] subOffsets = new long[numFiles];
    for (int i = 0; i < numFiles; i++) {
      // 4 - Offset to sub-directory details or file details (pointer into dir1 or dir3)
      long subDirOffset = fm.readInt() + dirOffset;
      FieldValidator.checkOffset(subDirOffset);
      subOffsets[i] = subDirOffset;
    }

    long curOffset = fm.getOffset();

    for (int i = 0; i < numFiles; i++) {
      fm.seek(subOffsets[i]);

      if (subOffsets[i] < dir2Offset) {
        // sub-directory
        readDirectory(resources, fm, path, dirName);
      }
      else {
        // file
        // 4 - Offset To Parent Directory (relative to Directory 1 Offset)
        fm.skip(4);

        // 4 - Decompressed Length
        int decompLength = fm.readInt();
        long length = decompLength;

        // 4 - File Offset
        long offset = fm.readInt() + firstFileOffset;
        FieldValidator.checkOffset(offset, arcSize);

        // 4 - CRC32
        // 8 - Timestamp
        fm.skip(12);

        // 4 - Compressed Tag? (0/1)
        boolean compressed = (fm.readInt() == 1);

        // 2 - Filename Length
        int filenameLength = fm.readShort();
        FieldValidator.checkFilenameLength(filenameLength);

        // X - Filename
        String filename = fm.readString(filenameLength);
        FieldValidator.checkFilename(filename);
        //System.out.println(fm.getOffset() + " - " + filename);

        filename = dirName + filename;

        // 0-3 - null Padding to a multiple of 4 bytes (including the filename length)
        int filenamePaddingSize = 4 - ((filenameLength + 2) % 4);
        //if (filenamePaddingSize != 4) {
        fm.skip(filenamePaddingSize);
        //}

        if (compressed) {
          /*
          // 1-4 - null Padding so filenameField+filenameLengthField is a multiple of 4 bytes
          fm.skip(4 - ((filenameLength + 2) % 4));
          
          // 4 - Compressed Length
          length = fm.readInt();
          
          // 4 - Chunk Size
          int chunkSize = fm.readInt();
          int numChunks = (decompLength / chunkSize) + 1;
          if (decompLength % chunkSize == 0) {
            numChunks++;
          }
          
          long[] offsets = new long[numChunks];
          long[] lengths = new long[numChunks];
          long[] decompLengths = new long[numChunks];
          
          boolean hasMore = true;
          int chunkNum = 0;
          int totalDecompLength = 0;
          while (hasMore) {
            // 4 - Decompressed Size Of Chunk
            int chunkDecompLength = fm.readInt();
          
            // 4 - Compressed Size Of Chunk [+16 if moreDetailsMarker == 16]
            int chunkLength = fm.readInt();
          
            // 4 - More Details Marker (0=no more comp data, 16=another comp data follows)
            int moreDetails = fm.readInt();
            if (moreDetails == 0) {
              hasMore = false;
            }
            else if (totalDecompLength >= decompLength) {
              chunkDecompLength = 0;
              chunkLength = 0;
              hasMore = false;
            }
            else {
              //chunkLength += 16;
              chunkDecompLength -= 16;
            }
          
            // 4 - Unknown
            long blockOffset = fm.readInt() + offset;
            FieldValidator.checkLength(blockOffset, arcSize);
          
            offsets[chunkNum] = blockOffset;
            lengths[chunkNum] = chunkLength;
            decompLengths[chunkNum] = chunkDecompLength;
          
            chunkNum++;
          
            if (moreDetails != 0) {
              chunkLength += 16;
            }
          
            //offset += chunkLength;
            totalDecompLength += chunkDecompLength + 16;
          
          }
          
          
          //path,id,name,offset,length,decompLength,exporter
          ExporterPlugin zlibExporter = Exporter_ZLib.getInstance();
          BlockExporterWrapper blockExporter = new BlockExporterWrapper(zlibExporter, offsets, lengths, decompLengths);
          //resources[realNumFiles] = new SplitChunkResource(path, filename, offsets, lengths, decompLengths, exporter);
          resources[realNumFiles] = new Resource(path, filename, offset, length, decompLength, blockExporter);
          */

          // 4 - Compressed Length
          int length2 = fm.readInt();
          FieldValidator.checkLength(length2, arcSize);
          if (length2 != 0) {
            length = length2;
          }

          // 4 - Block Size (16384)
          int blockSize = fm.readInt();
          //blockSize = 16384;
          FieldValidator.checkLength(blockSize, arcSize);

          int numBlocks = (int) (decompLength / blockSize);
          if (decompLength % blockSize != 0) {
            numBlocks++;
          }
          numBlocks *= 2; // the last few blocks might be empty - that's OK

          //System.out.println(fm.getOffset() + "\t" + blockSize + "\t" + decompLength);

          ExporterPlugin[] exporters = new ExporterPlugin[numBlocks];
          long[] blockOffsets = new long[numBlocks];
          long[] blockLengths = new long[numBlocks];
          long[] blockDecompLengths = new long[numBlocks];

          for (int b = 0; b < numBlocks; b++) {
            // fill the array with default exporters
            exporters[b] = exporterDefault;
          }

          long totalDecomp = 0;
          for (int b = 0; b < numBlocks; b += 2) {
            if (totalDecomp >= decompLength) {
              break;
            }

            // 4 - Decompressed Block Length
            int blockDecompLength = fm.readInt();
            FieldValidator.checkLength(blockDecompLength);

            // 4 - Compressed Block Length
            int blockLength = fm.readInt();
            FieldValidator.checkLength(blockLength, arcSize);

            // 4 - More Blocks Flag? (16 = more blocks, 0 = last block)
            int moreBlocksFlag = fm.readInt();

            // 4 - Decompressed Block Offset
            long blockOffset = fm.readInt() + offset;
            FieldValidator.checkLength(blockOffset, arcSize);

            if (moreBlocksFlag != 0) {
              // the last 16 bytes of uncompressed data are just stored uncompressed in the space between blocks.
              blockDecompLength -= moreBlocksFlag;
            }

            blockDecompLengths[b] = blockDecompLength;
            blockLengths[b] = blockLength;
            blockOffsets[b] = blockOffset;

            if (blockLength - 16 == blockDecompLength) {
              // an uncompressed block
              exporters[b] = exporterDefault;
            }
            else {
              // compressed block
              exporters[b] = exporterZLib;
            }

            totalDecomp += blockDecompLength;

            if (moreBlocksFlag != 0) {
              // now add the 16-byte uncompressed block
              blockOffset += blockLength;
              blockLength = moreBlocksFlag;
              blockDecompLength = moreBlocksFlag;

              blockDecompLengths[b + 1] = blockDecompLength;
              blockLengths[b + 1] = blockLength;
              blockOffsets[b + 1] = blockOffset;
              exporters[b + 1] = exporterDefault;

              totalDecomp += moreBlocksFlag;
            }

          }

          BlockVariableExporterWrapper blockExporter = new BlockVariableExporterWrapper(exporters, blockOffsets, blockLengths, blockDecompLengths);

          //path,name,offset,length,decompLength,exporter
          resources[realNumFiles] = new Resource(path, filename, offset, length, decompLength, blockExporter);
        }
        else {
          //path,id,name,offset,length,decompLength,exporter
          resources[realNumFiles] = new Resource(path, filename, offset, length);
        }

        TaskProgressManager.setValue(realNumFiles);
        realNumFiles++;
      }

    }

    fm.seek(curOffset);

  }

}
