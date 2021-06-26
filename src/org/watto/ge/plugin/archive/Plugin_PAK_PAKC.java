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
public class Plugin_PAK_PAKC extends ArchivePlugin {

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public Plugin_PAK_PAKC() {

    super("PAK_PAKC", "PAK_PAKC");

    //         read write replace rename
    setProperties(true, false, false, false);

    setGames("Commandos: Strike Force");
    setExtensions("pak");
    setPlatforms("PC");

    setTextPreviewExtensions("cmo", "dst", "psh", "txl", "vsh"); // LOWER CASE

    //setFileTypes("","",
    //             "",""
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
      String header = fm.readString(4);
      if (header.equals("PAKC") || header.equals("PAKA")) {
        rating += 50;
      }

      // Unknown
      int version1 = fm.readInt();
      if (version1 == 4 || version1 == 5) {
        rating += 5;
      }

      // Unknown
      if (fm.readInt() == 1) {
        rating += 5;
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

      ExporterPlugin exporter = Exporter_ZLib.getInstance();

      // RESETTING GLOBAL VARIABLES

      FileManipulator fm = new FileManipulator(path, false);

      long arcSize = fm.getLength();

      // 4 - Header (PAKC=Compressed Files, PAKA=Uncompressed Files)
      String header = fm.readString(4);
      boolean compression = false;
      if (header.equals("PAKC")) {
        compression = true;
      }

      // 4 - Version 1 (4/5)
      int version = fm.readInt();

      // 4 - Version 2 (1)
      fm.skip(4);

      // 4 - Number Of Files?
      int numFiles = fm.readInt();
      FieldValidator.checkNumFiles(numFiles);

      Resource[] resources = new Resource[numFiles];
      TaskProgressManager.setMaximum(numFiles);

      // Loop through directory
      for (int i = 0; i < numFiles; i++) {
        // X - Filename
        // 1 - null Filename Terminator
        String filename = fm.readNullString();
        FieldValidator.checkFilename(filename);

        // 4 - File Offset (relative to the start of the file data) [-13]
        long offset = fm.readInt();
        FieldValidator.checkOffset(offset, arcSize);

        // 4 - Decompressed File Length?
        long length = fm.readInt();
        FieldValidator.checkLength(length, arcSize);

        // 4 - Unknown
        // 4 - File Type Hash
        fm.skip(8);

        //path,name,offset,length,decompLength,exporter
        resources[i] = new Resource(path, filename, offset, length, length);

        TaskProgressManager.setValue(i);
      }

      // set the offsets correctly
      long relativeOffset = fm.getOffset();
      if (version == 5) { // version 4 is already correct
        relativeOffset -= 13;
      }

      for (int i = 0; i < numFiles; i++) {
        Resource resource = resources[i];
        long offset = resource.getOffset() + relativeOffset;
        FieldValidator.checkOffset(offset, arcSize);
        resource.setOffset(offset);
      }

      // work out the compressed lengths
      if (compression) {
        fm.getBuffer().setBufferSize(8); // small quick reads

        for (int i = 0; i < numFiles; i++) {
          Resource resource = resources[i];

          long offset = resource.getOffset();
          fm.seek(offset);

          int decompLength = (int) resource.getDecompressedLength();

          //System.out.println(offset + "\t" + decompLength);

          int numBlocks = decompLength / 4096;
          if (decompLength % 4096 != 0) {
            numBlocks++;
          }

          long[] blockOffsets = new long[numBlocks];
          long[] blockLengths = new long[numBlocks];
          long[] blockDecompLengths = new long[numBlocks];
          long compLengthTotal = 0;
          for (int b = 0; b < numBlocks; b++) {
            // 4 - Compressed Block Length
            int blockLength = fm.readInt();
            FieldValidator.checkLength(blockLength, arcSize);

            // 4 - Decompressed Block Length
            int blockDecompLength = fm.readInt();
            FieldValidator.checkLength(blockDecompLength);

            // X - Block Data
            long blockOffset = fm.getOffset();
            fm.skip(blockLength);

            blockOffsets[b] = blockOffset;
            blockLengths[b] = blockLength;
            blockDecompLengths[b] = blockDecompLength;

            compLengthTotal += blockLength + 8;
          }

          BlockExporterWrapper blockExporter = new BlockExporterWrapper(exporter, blockOffsets, blockLengths, blockDecompLengths);
          resource.setExporter(blockExporter);

          resource.setLength(compLengthTotal);
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
