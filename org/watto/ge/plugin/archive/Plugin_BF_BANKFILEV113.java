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
import org.watto.ge.plugin.exporter.BlockExporterWrapper;
import org.watto.ge.plugin.exporter.Exporter_ZLib;
import org.watto.io.FileManipulator;
import org.watto.task.TaskProgressManager;

/**
**********************************************************************************************

**********************************************************************************************
**/
public class Plugin_BF_BANKFILEV113 extends ArchivePlugin {

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public Plugin_BF_BANKFILEV113() {

    super("BF_BANKFILEV113", "BF_BANKFILEV113");

    //         read write replace rename
    setProperties(true, false, false, false);

    setGames("Ultimate Race Pro");
    setExtensions("bd", "bd4", "bf", "bf4", "bp", "bp4", "bv", "bv4"); // MUST BE LOWER CASE
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
      if (fm.readString(15).equals("Bank file v1.13")) {
        rating += 50;
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

      // 68 - Header ("Bank file v1.13" + nulls and junk to fill)
      // 188 - Unknown
      fm.skip(256);

      // 4 - Number Of Files
      int numFiles = fm.readInt();
      FieldValidator.checkNumFiles(numFiles);

      Resource[] resources = new Resource[numFiles];
      TaskProgressManager.setMaximum(numFiles);

      // Loop through directory
      String[] filenames = new String[numFiles];
      for (int i = 0; i < numFiles; i++) {

        // 256 - Description/Filename (null terminated, filled with nulls and junk)
        String filename = fm.readNullString(256);
        FieldValidator.checkFilename(filename);

        filenames[i] = filename;

      }

      fm.getBuffer().setBufferSize(10);
      fm.seek(0);
      fm.seek(260 + (numFiles * 256));

      int maxChunks = 1000;

      long[] offsets = new long[maxChunks];
      long[] lengths = new long[maxChunks];
      long[] decompLengths = new long[maxChunks];
      for (int i = 0; i < numFiles; i++) {

        int chunkNum = 0;
        long totalLength = 0;
        long totalDecompLength = 0;

        for (int c = 0; c < maxChunks; c++) {
          // 4 - Compressed Block Length
          int length = fm.readInt() - 6;
          FieldValidator.checkLength(length, arcSize);

          totalLength += length;

          // 2 - Unknown (12614)
          fm.skip(2);

          // 4 - Decompressed Length
          int decompLength = fm.readInt();
          FieldValidator.checkLength(decompLength);

          totalDecompLength += decompLength;

          offsets[chunkNum] = fm.getOffset();
          lengths[chunkNum] = length;
          decompLengths[chunkNum] = decompLength;
          chunkNum++;

          // X - Compressed Block
          fm.skip(length);

          if (decompLength == 32928) {
            // more chunks after this one
          }
          else {
            // last chunk

            long[] newOffsets = new long[chunkNum];
            System.arraycopy(offsets, 0, newOffsets, 0, chunkNum);
            long[] newLengths = new long[chunkNum];
            System.arraycopy(lengths, 0, newLengths, 0, chunkNum);
            long[] newDecompLengths = new long[chunkNum];
            System.arraycopy(decompLengths, 0, newDecompLengths, 0, chunkNum);

            BlockExporterWrapper wrapper = new BlockExporterWrapper(exporter, newOffsets, newLengths, newDecompLengths);

            String filename = filenames[i];
            long offset = newOffsets[0];

            //path,name,offset,length,decompLength,exporter
            Resource resource = new Resource(path, filename, offset, totalLength, totalDecompLength, wrapper);
            resources[i] = resource;

            // end of the chunks, move on to the next file
            break;
          }
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
