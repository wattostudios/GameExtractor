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
import org.watto.ge.plugin.exporter.Exporter_ZLib;
import org.watto.io.FileManipulator;
import org.watto.task.TaskProgressManager;

/**
**********************************************************************************************

**********************************************************************************************
**/
public class Plugin_ASR_ASURAZBB extends ArchivePlugin {

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public Plugin_ASR_ASURAZBB() {

    super("ASR_ASURAZBB", "ASR_ASURAZBB");

    //         read write replace rename
    setProperties(true, false, false, false);

    setGames("Strange Brigade");
    setExtensions("asr", "gui"); // MUST BE LOWER CASE
    setPlatforms("PC");

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

      // 8 - Header (AsuraZbb)
      if (fm.readString(8).equals("AsuraZbb")) {
        rating += 50;
      }

      fm.skip(8);

      long arcSize = fm.getLength();

      // Compressed Length
      if (FieldValidator.checkLength(fm.readInt(), arcSize)) {
        rating += 5;
      }

      // Decompressed Length
      if (FieldValidator.checkLength(fm.readInt())) {
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

      // 8 - Header (AsuraZbb)
      // 8 - Hash?
      fm.skip(16);

      int numBlocks = Archive.getMaxFiles();
      long[] blockOffsets = new long[numBlocks];
      long[] blockLengths = new long[numBlocks];
      long[] blockDecompLengths = new long[numBlocks];
      int realNumBlocks = 0;

      TaskProgressManager.setMaximum(arcSize);
      long totalDecompLength = 0;
      while (fm.getOffset() < arcSize) {

        // 4 - Compressed Length
        int length = fm.readInt();
        FieldValidator.checkLength(length, arcSize);
        blockLengths[realNumBlocks] = length;

        // 4 - Decompressed Length
        int decompLength = fm.readInt();
        FieldValidator.checkLength(decompLength);
        blockDecompLengths[realNumBlocks] = decompLength;
        totalDecompLength += decompLength;

        // X - Compressed File Data
        long offset = fm.getOffset();
        blockOffsets[realNumBlocks] = offset;

        fm.skip(length);

        TaskProgressManager.setValue(offset);
        realNumBlocks++;
      }

      long[] oldBlockOffsets = blockOffsets;
      long[] oldBlockLengths = blockLengths;
      long[] oldBlockDecompLengths = blockDecompLengths;

      blockOffsets = new long[realNumBlocks];
      blockLengths = new long[realNumBlocks];
      blockDecompLengths = new long[realNumBlocks];

      System.arraycopy(oldBlockOffsets, 0, blockOffsets, 0, realNumBlocks);
      System.arraycopy(oldBlockLengths, 0, blockLengths, 0, realNumBlocks);
      System.arraycopy(oldBlockDecompLengths, 0, blockDecompLengths, 0, realNumBlocks);

      Resource[] resources = new Resource[1];
      TaskProgressManager.setMaximum(1);

      String filename = "CompressedFile1.zlb";

      BlockExporterWrapper blockExporter = new BlockExporterWrapper(exporter, blockOffsets, blockLengths, blockDecompLengths);

      //path,name,offset,length,decompLength,exporter
      resources[0] = new Resource(path, filename, 16, arcSize, totalDecompLength, blockExporter);

      fm.close();

      return resources;

    }
    catch (Throwable t) {
      logError(t);
      return null;
    }
  }

}
