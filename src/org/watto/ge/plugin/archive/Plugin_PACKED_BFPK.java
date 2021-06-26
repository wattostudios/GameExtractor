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
import org.watto.ErrorLogger;
import org.watto.datatype.Resource;
import org.watto.ge.helper.FieldValidator;
import org.watto.ge.plugin.ArchivePlugin;
import org.watto.ge.plugin.ExporterPlugin;
import org.watto.ge.plugin.exporter.BlockExporterWrapper;
import org.watto.ge.plugin.exporter.Exporter_LZ4;
import org.watto.io.FileManipulator;
import org.watto.task.TaskProgressManager;

/**
**********************************************************************************************

**********************************************************************************************
**/
public class Plugin_PACKED_BFPK extends ArchivePlugin {

  /**
  **********************************************************************************************

  **********************************************************************************************
  **/
  public Plugin_PACKED_BFPK() {

    super("PACKED_BFPK", "PACKED_BFPK");

    //         read write replace rename
    setProperties(true, false, false, false);

    setGames("Raiders of the Broken Planet");
    setExtensions("packed"); // MUST BE LOWER CASE
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
      if (fm.readString(4).equals("BFPK")) {
        rating += 50;
      }

      // 4 - Unknown (1280)
      int versionByte = fm.readByte();
      if (fm.readByte() == 5) {
        rating += 5;
      }
      fm.skip(2);

      if (versionByte == 2) {
        fm.skip(4);
      }

      // Number Of Files
      if (FieldValidator.checkNumFiles(fm.readInt())) {
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

      ExporterPlugin exporter = Exporter_LZ4.getInstance();

      // RESETTING GLOBAL VARIABLES

      FileManipulator fm = new FileManipulator(path, false);

      long arcSize = fm.getLength();

      // 4 - Header (BFPK)
      fm.skip(4);

      // 1 - Unknown (0/2)
      int version = fm.readByte();

      // 1 - Unknown (5)
      // 2 - null
      fm.skip(3);

      if (version == 2) {
        // 4 - Unknown
        fm.skip(4);
      }

      // 4 - Number Of Files
      int numFiles = fm.readInt();
      FieldValidator.checkNumFiles(numFiles);

      Resource[] resources = new Resource[numFiles];
      TaskProgressManager.setMaximum(numFiles);

      // Loop through directory
      long[] offsets = new long[numFiles + 1];
      int[] lengths = new int[numFiles];
      for (int i = 0; i < numFiles; i++) {

        // 4 - Filename Length
        int filenameLength = fm.readInt();
        FieldValidator.checkFilenameLength(filenameLength);

        // X - Filename
        String filename = fm.readString(filenameLength);

        // 4 - Decompressed File Length
        int length = fm.readInt();
        FieldValidator.checkLength(length, arcSize);
        lengths[i] = length;

        // 8 - File Offset
        long offset = fm.readLong();
        FieldValidator.checkOffset(offset, arcSize);
        offsets[i] = offset;

        // 4 - Hash?
        // 4 - null
        fm.skip(8);

        //path,name,offset,length,decompLength,exporter
        resources[i] = new Resource(path, filename, offset, length, length);

        TaskProgressManager.setValue(i);
      }

      fm.getBuffer().setBufferSize(16); // quick skipping for reading the compressed block details
      fm.flush();

      if (version == 2) {
        // files use compression

        offsets[numFiles] = arcSize; // to make it easier to calculate sizes in the next loop

        // calculate the real file lengths, and set the exporter if the file is compressed
        for (int i = 0; i < numFiles; i++) {
          long actualLength = offsets[i + 1] - offsets[i];

          if (actualLength != lengths[i]) {

            // compressed file
            Resource resource = resources[i];
            int decompLength = (int) resource.getDecompressedLength();

            fm.seek(offsets[i]);

            // 4 - Compressed Length
            actualLength = fm.readInt();
            FieldValidator.checkLength(actualLength);

            resource.setOffset(offsets[i] + 4);
            resource.setLength(actualLength);
            resource.setDecompressedLength(lengths[i]);

            // for each chunk (262144 decompressed)
            int numBlocks = decompLength / 262144;
            int lastBlockSize = decompLength - (numBlocks * 262144);
            if (lastBlockSize != 0) {
              numBlocks += 1;
            }

            long[] blockOffsets = new long[numBlocks];
            long[] blockLengths = new long[numBlocks];
            long[] blockDecompLengths = new long[numBlocks];

            long remainingLength = actualLength - 4; // -4 for the 4-byte compression header
            for (int b = 0; b < numBlocks; b++) {
              // 4 - Compressed Length
              int compLength = fm.readInt();

              // 4 - Hash?
              fm.skip(4);

              // X - Compressed Data
              blockOffsets[b] = fm.getOffset();
              blockLengths[b] = compLength;
              blockDecompLengths[b] = 262144;

              fm.skip(compLength);
              remainingLength -= (compLength + 8);
            }
            if (lastBlockSize != 0) {
              blockDecompLengths[numBlocks - 1] = lastBlockSize;
            }

            if (remainingLength > 0) {
              ErrorLogger.log("[PACKED_BFPK]: More compressed data remaining");
            }

            BlockExporterWrapper wrapper = new BlockExporterWrapper(exporter, blockOffsets, blockLengths, blockDecompLengths);
            resource.setExporter(wrapper);
          }

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
