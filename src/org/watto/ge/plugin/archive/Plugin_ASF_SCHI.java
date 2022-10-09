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
import org.watto.ge.plugin.exporter.Exporter_Default;
import org.watto.io.FileManipulator;
import org.watto.task.TaskProgressManager;

/**
**********************************************************************************************

**********************************************************************************************
**/
public class Plugin_ASF_SCHI extends ArchivePlugin {

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public Plugin_ASF_SCHI() {

    super("ASF_SCHI", "ASF_SCHI");

    //         read write replace rename
    setProperties(true, false, false, false);

    setGames("NHL 2001");
    setExtensions("asf"); // MUST BE LOWER CASE
    setPlatforms("PS1");

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

      // 4 - Header (SCHl)
      if (fm.readString(4).equals("SCHl")) {
        rating += 50;
      }

      // 4 - Header Length (including these 2 fields) (48)
      if (fm.readInt() == 48) {
        rating += 5;
      }

      // 2 - Part Header ("PT")
      if (fm.readString(2).equals("PT") && fm.readShort() == 1) {
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

      ExporterPlugin exporter = Exporter_Default.getInstance();

      // RESETTING GLOBAL VARIABLES

      FileManipulator fm = new FileManipulator(path, false);

      long arcSize = fm.getLength();

      int numFiles = Archive.getMaxFiles();

      Resource[] resources = new Resource[numFiles];
      TaskProgressManager.setMaximum(arcSize);

      // Loop through directory
      int realNumFiles = 0;
      while (fm.getOffset() < arcSize) {

        // 4 - Header (SCHl)
        fm.skip(4);

        // 4 - Header Length (including these 2 fields) (48)
        int headerLength = fm.readInt();

        // 2 - Part Header ("PT")
        // 2 - Unknown (1)
        // 24 - Unknown
        // 4 - null
        // 4 - Unknown (1)
        // 4 - Chunks Header (SCCl)
        // 4 - Chunks Length (including these 2 fields) (16)
        fm.skip(headerLength);

        // 4 - Unknown
        fm.skip(4);

        // 4 - Number of Chunks
        int numChunks = fm.readInt();
        FieldValidator.checkNumFiles(numChunks);

        long[] chunkLengths = new long[numChunks];
        long[] chunkOffsets = new long[numChunks];
        int totalLength = 0;
        long startOffset = fm.getOffset();
        for (int i = 0; i < numChunks; i++) {
          //System.out.println(i + " of " + numChunks + "\t" + fm.getOffset());

          // 4 - Data Chunk Header (SCDl)
          fm.skip(4);

          // 4 - Data Chunk Length (including these 2 fields, but NOT including the next 2 fields)
          int length = fm.readInt() - 8;
          FieldValidator.checkLength(length, arcSize);

          // 4 - Unknown
          // 4 - Unknown

          // X - Chunk Data
          long offset = fm.getOffset();
          fm.skip(length);

          length -= 8;
          offset += 8;

          chunkLengths[i] = length;
          chunkOffsets[i] = offset;
          totalLength += length;
        }
        if (numChunks > 0) {
          startOffset = chunkOffsets[0];
        }

        // 4 - End Header (SCEl)
        // 4 - End Length (12)
        // 4 - Unknown
        fm.skip(12);

        // 0-255 - null Padding to a multiple of 256 bytes
        //fm.skip(calculatePadding(fm.getOffset(), 256));

        String filename = Resource.generateFilename(realNumFiles);

        BlockExporterWrapper blockExporter = new BlockExporterWrapper(exporter, chunkOffsets, chunkLengths, chunkLengths);

        //path,name,offset,length,decompLength,exporter
        resources[realNumFiles] = new Resource(path, filename, startOffset, totalLength, totalLength, blockExporter);
        realNumFiles++;

        TaskProgressManager.setValue(fm.getOffset());
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
