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
import org.watto.ge.plugin.exporter.BlockVariableExporterWrapper;
import org.watto.ge.plugin.exporter.Exporter_Default;
import org.watto.ge.plugin.exporter.Exporter_Deflate;
import org.watto.io.FileManipulator;
import org.watto.io.converter.ShortConverter;
import org.watto.task.TaskProgressManager;

/**
**********************************************************************************************

**********************************************************************************************
**/
public class Plugin_WAD_FFCS extends ArchivePlugin {

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public Plugin_WAD_FFCS() {

    super("WAD_FFCS", "WAD_FFCS");

    //         read write replace rename
    setProperties(true, false, false, false);

    setGames("Mercenaries 2");
    setExtensions("wad"); // MUST BE LOWER CASE
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

      // Header
      if (fm.readString(4).equals("FFCS")) {
        rating += 50;
      }

      fm.skip(8);

      // Header
      if (fm.readString(4).equals("INDX")) {
        rating += 5;
      }

      long arcSize = fm.getLength();

      // Directory Offset
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

      ExporterPlugin exporterDefault = Exporter_Default.getInstance();
      ExporterPlugin exporterDeflate = Exporter_Deflate.getInstance();

      // RESETTING GLOBAL VARIABLES

      FileManipulator fm = new FileManipulator(path, false, 128);

      long arcSize = fm.getLength();

      // 4 - Header (FFCS)
      // 4 - Version (2)
      // 4 - Unknown (7)
      // 4 - Index Directory Header (INDX)
      // 4 - Index Directory Offset (32768)
      // 4 - Number Of Entries
      // 4 - Data Header (DATA)
      fm.skip(28);

      // 4 - File Data Offset (2129920)
      int fileDataOffset = fm.readInt();
      FieldValidator.checkOffset(fileDataOffset, arcSize);

      // 4 - Unknown (36)
      // 4 - Checksum Header (CSUM)
      // 4 - Checksum
      // 4 - Unknown
      // 4 - Assets Directory Header (ASET)
      // 4 - Assets Directory Offset
      // 4 - Number Of Assets
      // 4 - Filename Directory Header (PTHS)
      fm.skip(32);

      // 4 - Filename Directory Offset
      int filenameDirOffset = fm.readInt();
      FieldValidator.checkOffset(filenameDirOffset, arcSize);

      // 4 - Number Of Filenames?
      int numFiles = fm.readInt();
      FieldValidator.checkNumFiles(numFiles);

      // 144 - Unknown
      // X - null Padding to offset 32768

      // read the names
      fm.seek(filenameDirOffset);

      String[] names = new String[numFiles];
      for (int i = 0; i < numFiles; i++) {
        // X - Filename
        // 1 - null Filename Terminator
        String filename = fm.readNullString();
        FieldValidator.checkFilename(filename);
        names[i] = filename;
      }

      // read the files
      fm.getBuffer().setBufferSize(256);
      fm.seek(fileDataOffset);

      Resource[] resources = new Resource[numFiles];
      TaskProgressManager.setMaximum(numFiles);

      // Loop through directory
      for (int i = 0; i < numFiles; i++) {
        long startOffset = fm.getOffset();

        // 4 - Header (sges)
        // 2 - Unknown (4)
        fm.skip(6);

        // 2 - Number Of Blocks
        short numBlocks = fm.readShort();
        FieldValidator.checkNumFiles(numBlocks);

        // 4 - Decompressed Length
        int decompLength = fm.readInt();
        FieldValidator.checkLength(decompLength);

        // 4 - Compressed Length
        int length = fm.readInt();
        FieldValidator.checkLength(length, arcSize);

        // for each block
        ExporterPlugin[] blockExporters = new ExporterPlugin[numBlocks];
        long[] blockOffsets = new long[numBlocks];
        long[] blockLengths = new long[numBlocks];
        long[] blockDecompLengths = new long[numBlocks];
        for (int b = 0; b < numBlocks; b++) {

          int blockLength = 0;
          int blockDecompLength = 65536;
          if (b == numBlocks - 1) {
            // 2 - Block Length
            // 2 - Block Decomp Length
            blockLength = ShortConverter.unsign(fm.readShort());
            blockDecompLength = ShortConverter.unsign(fm.readShort());
          }
          else {
            // 4 - Block Length
            blockLength = fm.readInt();
          }
          FieldValidator.checkLength(blockLength, arcSize);
          blockLengths[b] = blockLength;

          // 4 - Block Offset (relative to the start of this file data)
          long blockOffset = fm.readInt() + startOffset - 1;
          FieldValidator.checkOffset(blockOffset, arcSize);
          blockOffsets[b] = blockOffset;

          blockDecompLengths[b] = blockDecompLength;

          if (blockLength == blockDecompLength) {
            // raw block
            blockExporters[b] = exporterDefault;
          }
          else {
            // compressed block
            blockExporters[b] = exporterDeflate;
          }
        }

        // for each block
        // X - Block of File Data (compressed)
        // X - null Padding to a multiple of 16? bytes

        // X - null Padding to a multiple of 32768 bytes
        long endOfFile = startOffset + length;
        endOfFile += calculatePadding(endOfFile, 32768);
        fm.seek(endOfFile);

        String filename = names[i];

        long offset = startOffset;

        BlockVariableExporterWrapper blockExporter = new BlockVariableExporterWrapper(blockExporters, blockOffsets, blockLengths, blockDecompLengths);

        //path,name,offset,length,decompLength,exporter
        resources[i] = new Resource(path, filename, offset, length, decompLength, blockExporter);

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
