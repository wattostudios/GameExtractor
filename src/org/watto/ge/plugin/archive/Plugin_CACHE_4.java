/*
 * Application:  Game Extractor
 * Author:       wattostudios
 * Website:      http://www.watto.org
 * Copyright:    Copyright (c) 2002-2024 wattostudios
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
import org.watto.ge.plugin.exporter.Exporter_ZLib;
import org.watto.io.FileManipulator;
import org.watto.io.buffer.ByteBuffer;
import org.watto.task.TaskProgressManager;

/**
**********************************************************************************************

**********************************************************************************************
**/
public class Plugin_CACHE_4 extends ArchivePlugin {

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public Plugin_CACHE_4() {

    super("CACHE_4", "CACHE_4");

    //         read write replace rename
    setProperties(true, false, false, false);

    setGames("Witcher 3: Wild Hunt");
    setExtensions("cache"); // MUST BE LOWER CASE
    setPlatforms("PC");

    // MUST BE LOWER CASE !!!
    //setFileTypes(new FileType("txt", "Text Document", FileType.TYPE_DOCUMENT),
    //             new FileType("bmp", "Bitmap Image", FileType.TYPE_IMAGE)
    //             );

    //setTextPreviewExtensions("colours", "rat", "screen", "styles"); // LOWER CASE

    //setCanScanForFileTypes(true);

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

      long arcSize = fm.getLength();

      // Compressed Length
      if (FieldValidator.checkLength(fm.readInt(), arcSize)) {
        rating += 5;
      }

      // Decomp Length (32768)
      if (fm.readInt() == 32768) {
        rating += 5;
      }

      // Compression Flag (3) and ZLib Flag (x)
      if (fm.readByte() == 3 && fm.readByte() == 120) {
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

      fm.seek(arcSize - 20);

      // 4 - Number of Files
      int numFiles = fm.readInt();
      FieldValidator.checkNumFiles(numFiles);

      // 4 - Filename Directory Length
      int filenameDirLength = fm.readInt();
      FieldValidator.checkLength(filenameDirLength, arcSize);

      // 4 - Number of Blocks
      // 4 - Footer (HCXT)
      // 4 - Unknown (6)

      long dirOffset = arcSize - 32 - filenameDirLength - (numFiles * 52);
      FieldValidator.checkOffset(dirOffset, arcSize);
      fm.seek(dirOffset);

      byte[] filenameBytes = fm.readBytes(filenameDirLength);
      FileManipulator nameFM = new FileManipulator(new ByteBuffer(filenameBytes));

      Resource[] resources = new Resource[numFiles];
      TaskProgressManager.setMaximum(numFiles);

      // Loop through directory
      long offset = 0;
      int[] blockCount = new int[numFiles];
      for (int i = 0; i < numFiles; i++) {

        // 4 - Hash?
        fm.skip(4);

        // 4 - Filename Offset (relative to the start of the Filename Directory)
        int nameOffset = fm.readInt();
        FieldValidator.checkOffset(nameOffset, filenameDirLength);

        // 4 - Unknown ID
        fm.skip(4);

        // 4 - Compressed File Length
        int length = fm.readInt();
        FieldValidator.checkLength(length, arcSize);

        // 4 - Decompressed File Length
        int decompLength = fm.readInt();
        FieldValidator.checkLength(decompLength);

        // 4 - Unknown (16)
        // 4 - Unknown
        // 4 - Unknown
        // 4 - Start Block Offset
        fm.skip(16);

        // 4 - Number of Blocks
        int numBlocks = fm.readInt() + 1;
        blockCount[i] = numBlocks;

        // 8 - null
        // 4 - Unknown
        fm.skip(12);

        // X - Filename (null)
        nameFM.seek(nameOffset);
        String filename = nameFM.readNullString();
        FieldValidator.checkFilename(filename);

        //path,name,offset,length,decompLength,exporter
        resources[i] = new Resource(path, filename, offset, length, decompLength);

        TaskProgressManager.setValue(i);

        offset += length;
        offset += calculatePadding(length, 4096);
      }

      nameFM.close();

      // now go through each file, find the blocks, and create the BlockExtractor for them.
      fm.getBuffer().setBufferSize(9);

      for (int i = 0; i < numFiles; i++) {
        Resource resource = resources[i];
        fm.seek(resource.getOffset());

        int numBlocks = blockCount[i];

        long[] blockOffsets = new long[numBlocks];
        long[] blockLengths = new long[numBlocks];
        long[] blockDecompLengths = new long[numBlocks];

        int remainingLength = (int) resource.getLength();
        int b = 0;
        while (remainingLength > 0) {
          // 4 - Compressed Block Length
          int compLength = fm.readInt();
          FieldValidator.checkLength(compLength, arcSize);

          // 4 - Decompressed Block Length
          int decompLength = fm.readInt();
          FieldValidator.checkLength(decompLength);

          // 1 - Compression Flag (3)
          fm.skip(1);

          // X - Block Data (ZLib Compression)
          blockOffsets[b] = fm.getOffset();
          blockLengths[b] = compLength;
          blockDecompLengths[b] = decompLength;
          b++;

          remainingLength -= (9 + compLength);

          fm.skip(compLength);
        }

        if (b < numBlocks) {
          ErrorLogger.log("[CACHE_4] Only used " + b + " blocks instead of " + numBlocks);
        }

        resource.setExporter(new BlockExporterWrapper(exporter, blockOffsets, blockLengths, blockDecompLengths));

      }

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

    return null;
  }

}
