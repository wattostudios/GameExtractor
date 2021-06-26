/*
 * Application:  Game Extractor
 * Author:       wattostudios
 * Website:      http://www.watto.org
 * Copyright:    Copyright (c) 2002-2021 wattostudios
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
import org.watto.ge.plugin.exporter.BlockVariableExporterWrapper;
import org.watto.ge.plugin.exporter.Exporter_Default;
import org.watto.ge.plugin.exporter.Exporter_LZMA_BSP;
import org.watto.io.FileManipulator;
import org.watto.task.TaskProgressManager;

/**
**********************************************************************************************

**********************************************************************************************
**/
public class Plugin_PC_BOLB extends ArchivePlugin {

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public Plugin_PC_BOLB() {

    super("PC_BOLB", "PC_BOLB");

    //         read write replace rename
    setProperties(true, false, false, false);

    setGames("Masquerade: The Baubles of Doom");
    setExtensions("pc"); // MUST BE LOWER CASE
    setPlatforms("PC");

    // MUST BE LOWER CASE !!!
    //setFileTypes(new FileType("txt", "Text Document", FileType.TYPE_DOCUMENT),
    //             new FileType("bmp", "Bitmap Image", FileType.TYPE_IMAGE)
    //             );

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

      fm.skip(20);

      // Header
      if (fm.readString(4).equals("BOLB")) {
        rating += 50;
      }

      // Number Of Archive Files
      if (FieldValidator.checkNumFiles(fm.readInt())) {
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

      ExporterPlugin exporter = Exporter_LZMA_BSP.getInstance();
      ExporterPlugin exporterDefault = Exporter_Default.getInstance();

      // RESETTING GLOBAL VARIABLES

      FileManipulator fm = new FileManipulator(path, false);

      long arcSize = fm.getLength();

      // 20 - Unknown
      // 4 - Header (BOLB)
      fm.skip(24);

      // 4 - Number of Archive Files? (2)
      int numArchives = fm.readInt();
      FieldValidator.checkNumFiles(numArchives);

      // 4 - Number of Files
      int numFiles = fm.readInt();
      FieldValidator.checkNumFiles(numFiles);

      // find the split archive files
      File[] archiveFiles = new File[numArchives];
      long[] archiveLengths = new long[numArchives];

      String basePath = path.getAbsolutePath();
      int dashPos = basePath.lastIndexOf('-');
      if (dashPos <= 0) {
        return null;
      }
      basePath = basePath.substring(0, dashPos);

      for (int i = 0; i < numArchives; i++) {
        File arcFile = new File(basePath + "-" + i + ".blobset.pc");
        if (!arcFile.exists()) {
          return null;
        }
        archiveFiles[i] = arcFile;
        archiveLengths[i] = arcFile.length();
      }

      // now read the directory

      Resource[] resources = new Resource[numFiles];
      TaskProgressManager.setMaximum(numFiles);

      // Loop through directory
      int previousArcNumber = -1;
      boolean[] compressed = new boolean[numFiles];
      for (int i = 0; i < numFiles; i++) {

        // 4 - File Offset
        int offset = fm.readInt();

        // 4 - File Length
        int length1 = fm.readInt();
        FieldValidator.checkLength(length1, arcSize);

        // 4 - File Length
        int length2 = fm.readInt();
        FieldValidator.checkLength(length2, arcSize);

        // 4 - Offset to File Block Table
        int blockOffset = fm.readInt();

        // 4 - Compressed Length
        int length = fm.readInt();
        FieldValidator.checkLength(length, arcSize);

        // 4 - Decompressed Length
        int decompLength = fm.readInt();
        FieldValidator.checkLength(decompLength);

        // 4 - Unknown
        fm.skip(4);

        // 4 - Archive Number (0/1)
        int arcNumber = fm.readInt();
        FieldValidator.checkRange(arcNumber, 0, numArchives);

        String filename = Resource.generateFilename(i);

        if (arcNumber != previousArcNumber) {
          previousArcNumber = arcNumber;
          arcSize = archiveLengths[arcNumber];
          path = archiveFiles[arcNumber];
        }

        FieldValidator.checkOffset(offset, arcSize);
        FieldValidator.checkOffset(blockOffset, arcSize + 1); // end offset can be the end of the archive

        if (length == 0 && decompLength == 0) {

          if (length1 == length2) {
            // uncompressed
            compressed[i] = false;

            //path,name,offset,length,decompLength,exporter
            Resource resource = new Resource(path, filename, offset, length1);
            resource.forceNotAdded(true);
            resources[i] = resource;
          }
          else {
            // compressed blocks
            compressed[i] = true;

            //path,name,offset,length,decompLength,exporter
            Resource resource = new Resource(path, filename, blockOffset, length1, length2);
            resource.forceNotAdded(true);
            resources[i] = resource;
          }

        }
        else {
          // compressed blocks
          compressed[i] = true;

          //path,name,offset,length,decompLength,exporter
          Resource resource = new Resource(path, filename, blockOffset, length, decompLength);
          resource.forceNotAdded(true);
          resources[i] = resource;
        }

        TaskProgressManager.setValue(i);
      }

      // now go through and work out the compressed blocks
      fm.getBuffer().setBufferSize(64);

      File openedFile = fm.getFile();
      for (int i = 0; i < numFiles; i++) {
        try {

          TaskProgressManager.setValue(i);

          Resource resource = resources[i];
          File arcFile = resource.getSource();
          if (arcFile != openedFile) {
            fm.close();
            fm = new FileManipulator(arcFile, false);
          }

          long offset = resource.getOffset();
          fm.seek(offset);
          //System.out.println(offset);

          int totalLength = (int) resource.getLength();

          // 4 - Number of Blocks
          int numBlocks = fm.readInt();
          if (numBlocks == 1347236685) {
            // M3MP file

            // 4 - Number of Files
            int numInnerFiles = fm.readInt();
            if (numInnerFiles != 1) {
              // multiple files - can't handle that here
              continue;
            }

            // 4 - Number of Blocks
            numBlocks = fm.readInt();
            FieldValidator.checkNumFiles(numBlocks);

            // 4 - Data Offset
            int dataOffset = fm.readInt() - 16; // -16 because we've already read 16 bytes from the file
            FieldValidator.checkOffset(dataOffset, totalLength);

            fm.skip(dataOffset);

            long[] blockOffsets = new long[numBlocks];
            long[] blockLengths = new long[numBlocks];
            long[] blockDecompLengths = new long[numBlocks];
            ExporterPlugin[] blockExporters = new ExporterPlugin[numBlocks];

            int blockLengthsTotal = 0;
            int blockDecompLengthsTotal = 0;
            for (int b = 0; b < numBlocks; b++) {
              // 4 - Block Offset
              int blockOffset = fm.readInt();
              //FieldValidator.checkOffset(blockOffset, totalLength);
              FieldValidator.checkOffset(blockOffset);

              // 4 - Compressed Block Length
              int blockLength = fm.readInt();
              //FieldValidator.checkLength(blockLength, totalLength);
              FieldValidator.checkLength(blockLength);

              // 4 - Decompressed Block Length
              int blockDecompLength = fm.readInt();
              FieldValidator.checkLength(blockDecompLength);

              blockOffsets[b] = offset + blockOffset;
              blockLengths[b] = blockLength;
              blockDecompLengths[b] = blockDecompLength;

              blockLengthsTotal += blockLength;
              blockDecompLengthsTotal += blockDecompLength;

              if (blockLength == blockDecompLength) {
                blockExporters[b] = exporterDefault;
              }
              else {
                blockExporters[b] = exporter;
              }
            }

            BlockVariableExporterWrapper blockExporter = new BlockVariableExporterWrapper(blockExporters, blockOffsets, blockLengths, blockDecompLengths);

            //System.out.println(totalLength + "\t" + blockLengthsTotal);

            resource.setLength(blockLengthsTotal);
            resource.setDecompressedLength(blockDecompLengthsTotal);
            resource.setExporter(blockExporter);

            continue;
          }
          else if (numBlocks == 1415073867) {
            // KPXT file
            continue;
          }
          else if (!compressed[i]) {
            // a normal uncompressed file
            continue;
          }
          // otherwise a normal compressed file
          FieldValidator.checkNumFiles(numBlocks);

          offset += 4 + (numBlocks * 4);

          long[] blockOffsets = new long[numBlocks];
          long[] blockLengths = new long[numBlocks];
          long[] blockDecompLengths = new long[numBlocks];
          for (int b = 0; b < numBlocks; b++) {
            // 4 - Compressed Block Length
            int blockLength = fm.readInt();
            FieldValidator.checkLength(blockLength, totalLength);

            blockOffsets[b] = offset;
            blockLengths[b] = blockLength;

            offset += blockLength;
          }

          for (int b = 0; b < numBlocks; b++) {
            fm.seek(blockOffsets[b]);

            // 4 - Decompressed Length (max 32768)
            int blockDecompLength = fm.readInt();
            FieldValidator.checkLength(blockDecompLength);
            blockDecompLengths[b] = blockDecompLength;
            blockOffsets[b] += 4;
          }

          BlockExporterWrapper blockExporter = new BlockExporterWrapper(exporter, blockOffsets, blockLengths, blockDecompLengths);

          resource.setExporter(blockExporter);
        }
        catch (Throwable t) {
          //ErrorLogger.log(t);
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

    if (headerInt1 == 893539142) {
      return "fsb";
    }
    else if (headerInt1 == 1347236685) {
      return "m3mp";
    }
    else if (headerInt1 == 1415073867) {
      return "kpxt";
    }
    else if (headerInt1 == 1196643152) {
      return "pssg";
    }

    return null;
  }

}
