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
import org.watto.ge.plugin.resource.Resource_WAV_RawAudio_Chunks;
import org.watto.io.FileManipulator;
import org.watto.task.TaskProgressManager;

/**
**********************************************************************************************

**********************************************************************************************
**/
public class Plugin_SKX_SKEX extends ArchivePlugin {

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public Plugin_SKX_SKEX() {

    super("SKX_SKEX", "SKX_SKEX");

    //         read write replace rename
    setProperties(true, false, false, false);

    setGames("Monsters, Inc.");
    setExtensions("skx"); // MUST BE LOWER CASE
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

      // Header
      if (fm.readString(4).equals("SKEX")) {
        rating += 50;
      }

      fm.skip(4);

      long arcSize = fm.getLength();

      // Directory Offset
      if (FieldValidator.checkOffset(fm.readInt(), arcSize)) {
        rating += 5;
      }

      // Directory Length
      if (FieldValidator.checkLength(fm.readInt(), arcSize)) {
        rating += 5;
      }

      // Directory Offset
      if (FieldValidator.checkOffset(fm.readInt(), arcSize)) {
        rating += 5;
      }

      // Directory Length
      if (FieldValidator.checkLength(fm.readInt(), arcSize)) {
        rating += 5;
      }

      // Number Of Files
      if (FieldValidator.checkNumFiles(fm.readInt())) {
        rating += 4; // so we don't equal 25 by matching these 5 fields
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

      // 4 - Header (SKEX)
      // 4 - Unknown
      // 4 - Directory 2 Offset
      // 4 - Directory 2 Length
      fm.skip(16);

      // 4 - Directory 1 Offset
      int dirOffset = fm.readInt();
      FieldValidator.checkOffset(dirOffset, arcSize);

      // 4 - Directory 1 Length
      fm.skip(4);

      // 4 - Number of Files
      int numFiles = fm.readInt();
      FieldValidator.checkNumFiles(numFiles);

      fm.seek(dirOffset);

      Resource[] resources = new Resource[numFiles];
      TaskProgressManager.setMaximum(numFiles);

      // Loop through directory
      for (int i = 0; i < numFiles; i++) {

        // 4 - File Offset
        int offset = fm.readInt();
        FieldValidator.checkOffset(offset, arcSize);

        // 4 - Unknown (4)
        // 4 - null
        fm.skip(8);

        String filename = Resource.generateFilename(i) + ".wav";

        //path,name,offset,length,decompLength,exporter
        resources[i] = new Resource_WAV_RawAudio_Chunks(path, filename, offset);

        TaskProgressManager.setValue(i);
      }

      calculateFileSizes(resources, dirOffset);

      // For quicker reading below
      fm.getBuffer().setBufferSize(400);

      // Loop through the file, find the audio chunks, and set the exporter
      for (int i = 0; i < numFiles; i++) {
        Resource_WAV_RawAudio_Chunks resource = (Resource_WAV_RawAudio_Chunks) resources[i];

        long offset = resource.getOffset();
        long length = resource.getLength();
        long endOffset = offset + length;

        fm.seek(offset);

        // 4 - Header (STRM)
        // 2 - null
        // 2 - Unknown (257)
        fm.skip(8);

        // 4 - Chunk Size (16384)
        int chunkSize = fm.readInt();
        FieldValidator.checkLength(chunkSize, arcSize);

        int numBlocks = (int) (length / chunkSize);
        if (length % chunkSize != 0) {
          numBlocks++;
        }

        // 8 - null
        // 4 - Unknown (15)
        // 4 - null
        // 4 - Unknown (8339556)
        // 8 - null
        // 4 - Unknown (8191)
        // 16 - null
        // 2 - null
        // 2 - Unknown (-256)
        // 4 - Unknown (65535)
        // 4 - null
        // 4 - Unknown (1)
        // 4 - null
        // 4 - Audio Data Length (total of all actual Audio Data pieces)
        fm.skip(72);

        // 4 - Sample Rate (24000)
        int sampleRate = fm.readInt();

        // 4 - Number of Channels (1)
        int channelCount = fm.readInt();

        // 4 - null
        fm.skip(4);

        long[] blockOffsets = new long[numBlocks];
        long[] blockLengths = new long[numBlocks];

        int currentBlock = 0;
        while (offset < endOffset) {
          // 4 - Header (SDAT)
          String header = fm.readString(4);
          if (!header.equals("SDAT")) {
            // the last chunk is really really small, and doesn't need a header

            blockOffsets[currentBlock] = offset;
            blockLengths[currentBlock] = endOffset - offset;

            System.out.println("SKX at offset " + offset + " for file " + resource.getName() + " with length " + blockLengths[currentBlock]);

            currentBlock++;

            offset += chunkSize;
            continue;
          }

          // 4 - Piece Number (incremental from 0)
          // 4 - Unknown
          fm.skip(8);

          // 4 - Chunk Directory Length
          int chunkDirLength = fm.readInt();
          FieldValidator.checkLength(chunkDirLength, chunkSize);

          // for each ChunkDirectoryLength/8
          //   4 - Chunk Position ID
          //   4 - Unknown
          fm.skip(chunkDirLength);

          // X - Audio Data
          long blockOffset = fm.getOffset();
          blockOffsets[currentBlock] = blockOffset;

          long blockLength = chunkSize - (blockOffset - offset);
          FieldValidator.checkPositive(blockLength);
          blockLengths[currentBlock] = blockLength;

          currentBlock++;

          offset += chunkSize;
          if (offset < endOffset) {
            fm.seek(offset);
          }
        }

        resource.setAudioProperties(sampleRate, (short) 16, (short) channelCount);
        resource.setWriteHeader(false); // Just write a raw PS2 ADPCM file
        resource.setOffsets(blockOffsets);
        resource.setLengths(blockLengths);

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
