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
import org.watto.datatype.Archive;
import org.watto.datatype.FileType;
import org.watto.datatype.Resource;
import org.watto.ge.helper.FieldValidator;
import org.watto.ge.plugin.ArchivePlugin;
import org.watto.ge.plugin.ExporterPlugin;
import org.watto.ge.plugin.exporter.BlockVariableExporterWrapper;
import org.watto.ge.plugin.exporter.Exporter_Default;
import org.watto.ge.plugin.exporter.Exporter_LZO_MiniLZO;
import org.watto.io.FileManipulator;
import org.watto.task.TaskProgressManager;

/**
**********************************************************************************************

**********************************************************************************************
**/
public class Plugin_000_8 extends ArchivePlugin {

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public Plugin_000_8() {

    super("000_8", "000_8");

    //         read write replace rename
    setProperties(true, false, false, false);

    setGames("Batman Vengeance");
    setExtensions("000"); // MUST BE LOWER CASE
    setPlatforms("PC");

    setFileTypes(new FileType("tsd", "TSD Image", FileType.TYPE_IMAGE));

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

      getDirectoryFile(fm.getFile(), "fat");
      rating += 25;

      long arcSize = fm.getLength();

      // Decompressed Length
      if (FieldValidator.checkLength(fm.readInt())) {
        rating += 5;
      }

      // Compressed Length
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

      //Exporter_LZO_MiniLZO exporterLZO = Exporter_LZO_MiniLZO.getInstance();
      //exporterLZO.setForceDecompress(true);
      Exporter_Default exporterDefault = Exporter_Default.getInstance();

      // RESETTING GLOBAL VARIABLES

      long arcSize = (int) path.length();

      File sourcePath = getDirectoryFile(path, "fat");
      FileManipulator fm = new FileManipulator(sourcePath, false);

      long fatLength = sourcePath.length();

      // 1 - Unknown (1)
      fm.skip(1);

      // 4 - Number Of Files
      int numFiles = Archive.getMaxFiles();

      Resource[] resources = new Resource[numFiles];
      TaskProgressManager.setMaximum(fatLength);

      // Loop through directory
      int realNumFiles = 0;
      while (fm.getOffset() < fatLength) {

        // 4 - Offset of This Field
        fm.skip(4);

        // 4 - File Offset
        long offset = fm.readInt();
        FieldValidator.checkOffset(offset, arcSize);

        if (offset == 1) {
          // directory entry

          // 4 - null
          // 4 - Directory Header (cali)
          // 8 - Unknown
          fm.skip(16);

          // 4 - Directory Name Length (including null)
          int dirNameLength = fm.readInt();
          FieldValidator.checkFilenameLength(dirNameLength);

          // X - Directory Name
          // 1 - null Directory Name Terminator
          // 4 - null
          fm.skip(dirNameLength + 4);
        }
        else {
          // file entry
          //offset += 13; // to skip the 13-byte file header

          // 4 - Decompressed Length
          long decompLength = fm.readInt();
          FieldValidator.checkLength(decompLength);

          // 4 - Compressed Length
          long length = fm.readInt();
          FieldValidator.checkLength(length, arcSize);

          // 8 - Unknown
          fm.skip(8);

          // 4 - Filename Length (including null)
          int filenameLength = fm.readInt();
          FieldValidator.checkFilenameLength(filenameLength);

          // X - Filename
          // 1 - null Filename Terminator
          String filename = fm.readNullString(filenameLength);
          FieldValidator.checkFilename(filename);

          // 2 - Unknown (256)
          // 2 - Unknown (0/47)
          fm.skip(4);

          //path,name,offset,length,decompLength,exporter
          resources[realNumFiles] = new Resource(path, filename, offset, length, decompLength);
          realNumFiles++;
        }

        TaskProgressManager.setValue(fm.getOffset());
      }

      resources = resizeResources(resources, realNumFiles);

      // now open the original archive, go to each offset, and work out the compressed blocks
      fm.close();
      fm = new FileManipulator(path, false, 13); // only reading the block headers

      for (int i = 0; i < realNumFiles; i++) {
        Resource resource = resources[i];

        fm.seek(resource.getOffset());

        long decompLength = resource.getDecompressedLength();
        int numBlocks = (int) (decompLength / 8192);
        if (decompLength % 8192 != 0) {
          numBlocks++;
        }

        ExporterPlugin[] exporters = new ExporterPlugin[numBlocks];
        long[] offsets = new long[numBlocks];
        long[] lengths = new long[numBlocks];
        long[] decompLengths = new long[numBlocks];

        // we need to create a NEW exporter here, so that each file will reset the buffer at the start, but keep the buffer for each block
        Exporter_LZO_MiniLZO exporterLZO = new Exporter_LZO_MiniLZO();
        exporterLZO.setForceDecompress(true);
        exporterLZO.setResetBuffer(false);

        for (int b = 0; b < numBlocks; b++) {
          // 4 - Decompressed Length
          int blockDecompLength = fm.readInt();
          FieldValidator.checkLength(blockDecompLength);

          // 4 - Compressed Length
          int blockLength = fm.readInt();
          FieldValidator.checkLength(blockLength, arcSize);

          // 4 - Unknown (190,186,173,222)
          fm.skip(4);

          // 1 - Unknown (3)
          int compressionType = fm.readByte();

          long blockOffset = fm.getOffset();
          fm.skip(blockLength);

          offsets[b] = blockOffset;
          lengths[b] = blockLength;
          decompLengths[b] = blockDecompLength;

          if (compressionType == 3) {
            exporters[b] = exporterLZO;
          }
          else if (compressionType == 2) {
            exporters[b] = exporterDefault;
          }
          else {
            ErrorLogger.log("[000_8] Unknown compression type: " + compressionType);
            exporters[b] = exporterLZO;
          }
        }

        BlockVariableExporterWrapper blockExporter = new BlockVariableExporterWrapper(exporters, offsets, lengths, decompLengths);
        resource.setExporter(blockExporter);
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
