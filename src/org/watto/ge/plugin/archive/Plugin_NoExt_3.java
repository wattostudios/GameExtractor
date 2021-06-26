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
import org.watto.ge.plugin.exporter.ContinuousBlockExporterWrapper;
import org.watto.ge.plugin.exporter.Exporter_ZLib_CompressedSizeOnly;
import org.watto.io.FileManipulator;
import org.watto.task.TaskProgressManager;

/**
**********************************************************************************************

**********************************************************************************************
**/
public class Plugin_NoExt_3 extends ArchivePlugin {

  /**
  **********************************************************************************************

  **********************************************************************************************
  **/
  public Plugin_NoExt_3() {

    super("NoExt_3", "NoExt_3");

    //         read write replace rename
    setProperties(true, false, false, false);

    setGames("Bloodsports.TV");
    setExtensions(""); // MUST BE LOWER CASE
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

      fm.skip(8);

      // 4 - null
      if (fm.readInt() == 0) {
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

      // 4 - Unknown
      // 4 - Unknown
      // 4 - null
      fm.skip(12);

      int numFiles = Archive.getMaxFiles();

      TaskProgressManager.setMaximum(arcSize);

      // Loop through directory
      int realNumFiles = 0;
      long[] offsets = new long[numFiles];
      long[] lengths = new long[numFiles];
      boolean[] startOfFile = new boolean[numFiles];
      while (fm.getOffset() < arcSize) {
        // 4 - Compressed File Length
        int length = fm.readInt();
        FieldValidator.checkLength(length, arcSize);
        lengths[realNumFiles] = length;

        // X - File Data
        long offset = fm.getOffset();
        offsets[realNumFiles] = offset;

        int compressionCheck = 0;
        if (length != 0) {
          compressionCheck = fm.readByte();
          fm.skip(length - 1);
        }

        if (compressionCheck == 120) {
          // start of a new file
          startOfFile[realNumFiles] = true;
        }
        else {
          startOfFile[realNumFiles] = false;
        }

        realNumFiles++;

        TaskProgressManager.setValue(offset);
      }

      numFiles = realNumFiles;

      Resource[] resources = new Resource[numFiles];
      TaskProgressManager.setMaximum(numFiles);

      // Now combine all the files that are split into blocks
      realNumFiles = 0;
      for (int i = 0; i < numFiles; i++) {
        if (startOfFile[i]) {

          String filename = Resource.generateFilename(realNumFiles);

          // go through and file the end of the file
          int lastEntry = numFiles;
          for (int e = i + 1; e < numFiles; e++) {
            if (startOfFile[e]) {
              lastEntry = e - 1;
              break;
            }
          }

          if (lastEntry == i) {
            // only 1 block in this file
            long offset = offsets[i];
            long length = lengths[i];

            //path,name,offset,length,decompLength,exporter
            resources[realNumFiles] = new Resource(path, filename, offset, length, length, exporter);
            realNumFiles++;
          }
          else {
            // multiple blocks, which all need to be joined together before being decompressed
            int numBlocks = lastEntry - i;

            long[] blockOffsets = new long[numBlocks];
            long[] blockLengths = new long[numBlocks];

            System.arraycopy(offsets, i, blockOffsets, 0, numBlocks);
            System.arraycopy(lengths, i, blockLengths, 0, numBlocks);

            int totalLength = 0;
            for (int b = 0; b < numBlocks; b++) {
              totalLength += blockLengths[b];
            }

            ContinuousBlockExporterWrapper blockExporter = new ContinuousBlockExporterWrapper(exporter, blockOffsets, blockLengths, blockLengths);

            //path,name,offset,length,decompLength,exporter
            resources[realNumFiles] = new Resource(path, filename, blockOffsets[0], totalLength, totalLength, blockExporter);
            realNumFiles++;

            i += numBlocks - 1; // -1 because it will still get incremented ++ as part of the loop
          }
        }

        TaskProgressManager.setValue(i);
      }

      resources = resizeResources(resources, realNumFiles);

      fm.close();

      return resources;

    }
    catch (Throwable t) {
      logError(t);
      return null;
    }
  }

}
