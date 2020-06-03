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
import org.watto.ge.plugin.ExporterPlugin;
import org.watto.ge.plugin.exporter.BlockExporterWrapper;
import org.watto.ge.plugin.exporter.Exporter_LZO_SingleBlock;
import org.watto.io.FileManipulator;
import org.watto.io.converter.ShortConverter;
import org.watto.task.TaskProgressManager;

/**
**********************************************************************************************

**********************************************************************************************
**/
public class Plugin_FORGE_SCIMITAR extends ArchivePlugin {

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public Plugin_FORGE_SCIMITAR() {

    super("FORGE_SCIMITAR", "FORGE_SCIMITAR");

    //         read write replace rename
    setProperties(true, false, false, false);

    setGames("For Honor",
        "Tom Clancy's Ghost Recon Wildlands");
    setExtensions("forge"); // MUST BE LOWER CASE
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
      if (fm.readString(8).equals("scimitar")) {
        rating += 50;
      }

      fm.skip(5);

      long arcSize = fm.getLength();

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
  @Override
  public Resource[] read(File path) {
    try {

      // NOTE - Compressed files MUST know their DECOMPRESSED LENGTH
      //      - Uncompressed files MUST know their LENGTH

      addFileTypes();

      ExporterPlugin exporterLZO = Exporter_LZO_SingleBlock.getInstance();
      //ExporterPlugin exporterLZO = Exporter_LZO2.getInstance();

      // RESETTING GLOBAL VARIABLES

      FileManipulator fm = new FileManipulator(path, false);

      long arcSize = fm.getLength();

      // 8 - Header (scimitar)
      // 1 - null
      // 4 - Unknown (27)
      fm.skip(13);

      // 4 - Details Directory Offset
      int dirOffset = fm.readInt();
      FieldValidator.checkOffset(dirOffset, arcSize);

      fm.seek(dirOffset);

      // 4 - Number Of Files
      int numFiles = fm.readInt();
      FieldValidator.checkNumFiles(numFiles);

      // 4 - Unknown (1)
      // 12 - null
      // 8 - Unknown (-1)
      // 4 - Number of Files (including the blank entry)
      // 4 - Unknown (1)
      // 4 - This Offset [-8]
      // 4 - null
      // 4 - Number of Files
      // 4 - Unknown (1)
      // 8 - Offset to the Start of the File Entries Loop
      // 8 - Unknown (-1)
      // 4 - null
      // 4 - Number of Files
      fm.skip(72);

      // 8 - Filename Directory Offset
      long filenameDirOffset = fm.readLong();
      FieldValidator.checkOffset(filenameDirOffset, arcSize);

      // 8 - Offset to the End of the Filename Directory (including the blank entry)
      fm.skip(8);

      Resource[] resources = new Resource[numFiles];
      TaskProgressManager.setMaximum(numFiles);

      // Loop through directory
      for (int i = 0; i < numFiles; i++) {
        // 8 - File Offset
        long offset = fm.readLong();
        FieldValidator.checkOffset(offset, arcSize);

        // 4 - Hash?
        // 4 - null
        fm.skip(8);

        // 4 - File Length
        int length = fm.readInt();
        FieldValidator.checkLength(length, arcSize);

        //path,name,offset,length,decompLength,exporter
        resources[i] = new Resource(path, "", offset, length);

        TaskProgressManager.setValue(i);
      }

      fm.seek(filenameDirOffset);

      // Loop through filename directory
      for (int i = 0; i < numFiles; i++) {
        // 4 - File Length
        // 8 - Hash?
        // 16 - null
        // 4 - File ID (Incremental from 1)
        // 4 - File ID (Incremental from -1)
        // 4 - null
        // 2 - File Type ID?
        // 2 - Unknown
        fm.skip(44);

        // 128 - Filename (null terminated, filled with nulls)
        String filename = fm.readNullString(128);
        FieldValidator.checkFilename(filename);

        // 4 - Unknown (4)
        // 8 - null
        // 4 - Unknown (4)
        // 4 - null
        fm.skip(20);

        //path,name,offset,length,decompLength,exporter
        Resource resource = resources[i];
        resource.setName(filename);
        resource.setOriginalName(filename);

        TaskProgressManager.setValue(i);
      }

      // go to each file and check for compression
      fm.getBuffer().setBufferSize(128);

      for (int i = 0; i < numFiles; i++) {
        Resource resource = resources[i];

        long resourceOffset = resource.getOffset();
        fm.seek(resourceOffset);
        long endOffset = resourceOffset + resource.getLength();

        int maxChunks = Archive.getMaxFiles();
        long[] chunkOffsets = new long[maxChunks];
        long[] chunkLengths = new long[maxChunks];
        long[] chunkDecompLengths = new long[maxChunks];
        int currentChunk = 0;

        long totalDecompLength = 0;

        // there can be multiple compressed blocks in each file, and each compressed block is made up of smaller compressed chunks
        while (fm.getOffset() < endOffset) {
          // 8 - Compression Header
          if (fm.readLong() != 1154322941026740787l) {
            // not compressed
            continue;
          }

          // 2 - Version (1)
          if (fm.readShort() != 1) {
            // unsupported version
            continue;
          }

          // 1 - Compression Type (0=LZO1 1=LZO1, 2=LZO2, 3=zstd, 4=oodle, 5=lzo1c, all others are oodle)
          if (fm.readByte() != 1) {
            // unsupported compression
            continue;
          }

          // 2 - Maximum Decompressed Chunk Size
          // 2 - Maximum Compressed Chunk Size
          fm.skip(4);

          // 4 - Number of Chunks
          int numChunks = fm.readInt();
          FieldValidator.checkNumFiles(numChunks);

          long offset = fm.getOffset() + numChunks * 4;
          for (int c = 0; c < numChunks; c++) {
            // 2 - Decompressed Chunk Length
            int decompLength = ShortConverter.unsign(fm.readShort());
            chunkDecompLengths[currentChunk] = decompLength;

            // 2 - Compressed Chunk Length
            int length = ShortConverter.unsign(fm.readShort());
            chunkLengths[currentChunk] = length;

            offset += 4; // skip the CRC on the compressed chunk
            chunkOffsets[currentChunk] = offset;

            totalDecompLength += decompLength;
            offset += length; // ready for the next chunk
            currentChunk++;
          }

          // move to the end of the compressed chunks, in case there's more compressed chunks to process afterwards
          fm.seek(offset);

        }

        if (currentChunk > 0) {
          // shrink the 3 arrays to the correct number of chunks
          long[] cutChunkOffsets = new long[currentChunk];
          System.arraycopy(chunkOffsets, 0, cutChunkOffsets, 0, currentChunk);
          long[] cutChunkLengths = new long[currentChunk];
          System.arraycopy(chunkLengths, 0, cutChunkLengths, 0, currentChunk);
          long[] cutChunkDecompLengths = new long[currentChunk];
          System.arraycopy(chunkDecompLengths, 0, cutChunkDecompLengths, 0, currentChunk);

          BlockExporterWrapper blockExporter = new BlockExporterWrapper(exporterLZO, cutChunkOffsets, cutChunkLengths, cutChunkDecompLengths);
          resource.setDecompressedLength(totalDecompLength);
          resource.setExporter(blockExporter);
        }

        TaskProgressManager.setValue(i);

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
