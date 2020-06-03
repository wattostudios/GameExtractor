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
import org.watto.datatype.Resource;
import org.watto.ge.helper.FieldValidator;
import org.watto.ge.plugin.ArchivePlugin;
import org.watto.ge.plugin.ExporterPlugin;
import org.watto.ge.plugin.exporter.BlockVariableExporterWrapper;
import org.watto.ge.plugin.exporter.Exporter_Default;
import org.watto.io.FileManipulator;
import org.watto.task.TaskProgressManager;

/**
**********************************************************************************************

**********************************************************************************************
**/
public class Plugin_SAR_CAR2 extends ArchivePlugin {

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public Plugin_SAR_CAR2() {

    super("SAR_CAR2", "SAR_CAR2");

    //         read write replace rename
    setProperties(true, false, false, false);

    setGames("SAP");
    setExtensions("sar", "car");
    setPlatforms("PC");

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
      String header = fm.readString(8);
      if (header.equals("CAR 2.00") || header.equals("CAR 2.01") || header.equals("CAR" + (byte) 0 + "2.00")) {
        rating += 50;
      }

      fm.skip(6);

      // Decompressed Size
      if (FieldValidator.checkLength(fm.readInt())) {
        rating += 5;
      }

      // null
      if (fm.readLong() == 0) {
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

      // RESETTING GLOBAL VARIABLES

      FileManipulator fm = new FileManipulator(path, false);

      long arcSize = fm.getLength();

      // 8 - Header (CAR 2.00)
      fm.skip(8);

      int numFiles = Archive.getMaxFiles();

      Resource[] resources = new Resource[numFiles];
      TaskProgressManager.setMaximum(arcSize);

      // Loop through directory
      int realNumFiles = 0;
      while (fm.getOffset() < arcSize) {
        // 2 - Header (RG)
        // 4 - Unknown (33206)
        fm.skip(6);

        // 4 - Decompressed Size
        int totalDecompLength = fm.readInt();
        FieldValidator.checkLength(totalDecompLength);

        // 8 - null
        // 4 - Timestamp? Hash?
        // 10 - null
        fm.skip(22);

        // 2 - Filename Length (including null terminator)
        short filenameLength = fm.readShort();
        FieldValidator.checkFilenameLength(filenameLength);

        // X - Filename
        // 1 - null Filename Terminator
        String filename = fm.readNullString(filenameLength);

        int maxBlocks = Archive.getMaxFiles();
        long[] offsets = new long[maxBlocks];
        long[] lengths = new long[maxBlocks];
        long[] decompLengths = new long[maxBlocks];
        ExporterPlugin[] exporters = new ExporterPlugin[maxBlocks];
        int realNumBlocks = 0;

        while (fm.getOffset() < arcSize && realNumBlocks < maxBlocks) {
          // 2 - Block Header (ED/DA/UD)
          String blockHeader = fm.readString(2);
          if (blockHeader.equals("UD")) {
            // raw block

            // 4 - Raw Block Length
            long length = fm.readInt();
            FieldValidator.checkLength(length, arcSize);

            // X - Block Data
            long offset = fm.getOffset();
            //System.out.println(blockHeader + "\t" + offset);
            fm.skip(length);

            offsets[realNumBlocks] = offset;
            lengths[realNumBlocks] = length;
            decompLengths[realNumBlocks] = length;
            exporters[realNumBlocks] = exporterDefault;
            realNumBlocks++;
          }
          else if (blockHeader.equals("DA")) {
            // normal compressed block

            // 4 - Compressed Block Length [-4]
            long length = fm.readInt() - 4;
            FieldValidator.checkLength(length, arcSize);

            // 4 - Decompressed Block Length
            long decompLength = fm.readInt();
            FieldValidator.checkLength(decompLength);

            // X - Block Data (Unknown Compression)
            long offset = fm.getOffset();
            //System.out.println(blockHeader + "\t" + offset);
            fm.skip(length);

            offsets[realNumBlocks] = offset;
            lengths[realNumBlocks] = length;
            decompLengths[realNumBlocks] = decompLength;
            exporters[realNumBlocks] = exporterDefault;
            realNumBlocks++;
          }
          else if (blockHeader.equals("ED")) {
            // last compressed block of this file

            // 4 - Compressed Block Length
            long length = fm.readInt();
            FieldValidator.checkLength(length, arcSize);

            // 4 - Decompressed Block Length
            long decompLength = fm.readInt();
            FieldValidator.checkLength(decompLength);

            // X - Block Data (Unknown Compression)
            long offset = fm.getOffset();
            //System.out.println(blockHeader + "\t" + offset);
            fm.skip(length);

            offsets[realNumBlocks] = offset;
            lengths[realNumBlocks] = length;
            decompLengths[realNumBlocks] = decompLength;
            exporters[realNumBlocks] = exporterDefault;
            realNumBlocks++;

            // last block - break the loop
            break;
          }
          else {
            ErrorLogger.log("[SAR_CAR2]: Unknown block type: " + blockHeader);
            return null;
          }

        }

        // resize the arrays
        long[] smallOffsets = new long[realNumBlocks];
        System.arraycopy(offsets, 0, smallOffsets, 0, realNumBlocks);
        long[] smallLengths = new long[realNumBlocks];
        System.arraycopy(lengths, 0, smallLengths, 0, realNumBlocks);
        long[] smallDecompLengths = new long[realNumBlocks];
        System.arraycopy(decompLengths, 0, smallDecompLengths, 0, realNumBlocks);
        ExporterPlugin[] smallExporters = new ExporterPlugin[realNumBlocks];
        System.arraycopy(exporters, 0, smallExporters, 0, realNumBlocks);

        BlockVariableExporterWrapper blockExporter = new BlockVariableExporterWrapper(smallExporters, smallOffsets, smallLengths, smallDecompLengths);

        //path,name,offset,length,decompLength,exporter
        resources[realNumFiles] = new Resource(path, filename, offsets[0], totalDecompLength, totalDecompLength, blockExporter);
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
