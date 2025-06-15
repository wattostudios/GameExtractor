/*
 * Application:  Game Extractor
 * Author:       wattostudios
 * Website:      http://www.watto.org
 * Copyright:    Copyright (c) 2002-2025 wattostudios
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
import org.watto.datatype.FileType;
import org.watto.datatype.Resource;
import org.watto.ge.helper.FieldValidator;
import org.watto.ge.plugin.ArchivePlugin;
import org.watto.ge.plugin.exporter.BlockExporterWrapper;
import org.watto.ge.plugin.exporter.Exporter_LZO_SingleBlock;
import org.watto.io.FileManipulator;
import org.watto.task.TaskProgressManager;

/**
**********************************************************************************************

**********************************************************************************************
**/
public class Plugin_TFC extends ArchivePlugin {

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public Plugin_TFC() {

    super("TFC", "TFC");

    //         read write replace rename
    setProperties(true, false, false, false);

    setGames("Styx: Master of Shadows");
    setExtensions("tfc"); // MUST BE LOWER CASE
    setPlatforms("PC");

    // MUST BE LOWER CASE !!!
    setFileTypes(new FileType("tfc_tex", "TFC_TEX Image", FileType.TYPE_IMAGE));

    //setTextPreviewExtensions("colours", "rat", "screen", "styles"); // LOWER CASE

    setCanScanForFileTypes(true);

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
      if (fm.readInt() == -1641380927) {
        rating += 5; // only +5 so it doesn't auto-match to generic Unreal archives
      }

      if (fm.readInt() == 131072) {
        rating += 5;
      }

      long arcSize = fm.getLength();

      // File Length
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

      // NOTE - Compressed files MUST know their DECOMPRESSED LENGTH
      //      - Uncompressed files MUST know their LENGTH

      addFileTypes();

      //ExporterPlugin exporterDefault = Exporter_Default.getInstance();
      Exporter_LZO_SingleBlock exporterLZO = Exporter_LZO_SingleBlock.getInstance();

      // RESETTING GLOBAL VARIABLES

      FileManipulator fm = new FileManipulator(path, false, 64); // small quick reads

      long arcSize = fm.getLength();

      int numFiles = Archive.getMaxFiles();
      int realNumFiles = 0;

      Resource[] resources = new Resource[numFiles];
      TaskProgressManager.setMaximum(arcSize);

      // Loop through directory
      while (fm.getOffset() < arcSize) {
        //System.out.println(fm.getOffset());

        // 4 - Unreal Header (193,131,42,158)
        fm.skip(4);

        // 4 - Unknown (131072)
        int blockSize = fm.readInt();
        FieldValidator.checkLength(blockSize);

        // 4 - Total Compressed File Length
        int totalCompLength = fm.readInt();
        FieldValidator.checkLength(totalCompLength, arcSize);

        // 4 - Total Decompressed File Length
        int totalDecompLength = fm.readInt();
        FieldValidator.checkLength(totalDecompLength);

        int numBlocks = totalDecompLength / blockSize;
        if (totalDecompLength % blockSize != 0) {
          numBlocks++;
        }

        long[] blockOffsets = new long[numBlocks];
        long[] blockLengths = new long[numBlocks];
        long[] blockDecompLengths = new long[numBlocks];

        long offset = fm.getOffset() + (numBlocks * 8);

        for (int b = 0; b < numBlocks; b++) {
          // 4 - Block Compressed File Length
          int blockLength = fm.readInt();
          FieldValidator.checkLength(blockLength, arcSize);

          // 4 - Block Decompressed File Length
          int blockDecompLength = fm.readInt();
          FieldValidator.checkLength(blockDecompLength, blockSize);

          blockOffsets[b] = offset;
          blockLengths[b] = blockLength;
          blockDecompLengths[b] = blockDecompLength;

          offset += blockLength;
        }

        // X - File Data
        fm.skip(totalCompLength);

        String filename = Resource.generateFilename(realNumFiles);

        BlockExporterWrapper blockExporter = new BlockExporterWrapper(exporterLZO, blockOffsets, blockLengths, blockDecompLengths);

        //path,name,offset,length,decompLength,exporter
        resources[realNumFiles] = new Resource(path, filename, offset, totalCompLength, totalDecompLength, blockExporter);

        TaskProgressManager.setValue(offset);
        realNumFiles++;
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

  /**
  **********************************************************************************************
  If an archive doesn't have filenames stored in it, the scanner can come here to try to work out
  what kind of file a Resource is. This method allows the plugin to provide additional plugin-specific
  extensions, which will be tried before any standard extensions.
  @return null if no extension can be determined, or the extension if one can be found
  **********************************************************************************************
  **/
  @Override
  public String guessFileExtension(Resource resource, byte[] headerBytes, int headerInt1, int headerInt2, int headerInt3, short headerShort1, short headerShort2, short headerShort3, short headerShort4, short headerShort5, short headerShort6) {

    /*
    if (headerInt1 == 2037149520) {
      return "js";
    }
    */

    // determine if it's a DXT5 or DXT1 square image
    int length = (int) resource.getDecompressedLength();

    // DXT5
    double squareRoot = Math.sqrt(length);
    if (squareRoot == (int) squareRoot) {
      return "tfc_tex";
    }

    // DXT1
    squareRoot = Math.sqrt(length / 2);
    if (squareRoot == (int) squareRoot) {
      return "tfc_tex";
    }

    return null;
  }

}
