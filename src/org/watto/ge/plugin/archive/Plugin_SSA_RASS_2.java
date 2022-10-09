/*
 * Application:  Game Extractor
 * Author:       wattostudios
 * Website:      http://www.watto.org
 * Copyright:    Copyright (c) 2002-2022 wattostudios
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
import org.watto.ge.plugin.exporter.Exporter_Custom_SSA_RASS_2;
import org.watto.ge.plugin.exporter.Exporter_Default;
import org.watto.ge.plugin.exporter.Exporter_XOR_RepeatingKey;
import org.watto.ge.plugin.exporter.Exporter_ZLib;
import org.watto.io.FileManipulator;
import org.watto.io.buffer.ByteBuffer;
import org.watto.io.converter.ByteConverter;
import org.watto.io.converter.IntConverter;
import org.watto.task.TaskProgressManager;

/**
**********************************************************************************************

**********************************************************************************************
**/
public class Plugin_SSA_RASS_2 extends ArchivePlugin {

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public Plugin_SSA_RASS_2() {

    super("SSA_RASS_2", "SSA_RASS_2");

    //         read write replace rename
    setProperties(true, false, false, false);

    setExtensions("ssa");
    setGames("Rise and Fall: Civilizations at War");
    setPlatforms("PC");

    setFileTypes("scn", "Scenario",
        "dat", "Data File",
        "udf", "Unit Data File",
        "edf", "Effect Data File",
        "env", "Environment",
        "cem", "Object Model",
        "psh", "Pixel Shaders",
        "vsh", "Vertex Shaders",
        "scc", "Shortcut",
        "sdf", "Sound Data File",
        "sdd", "SDD Texture",
        "tai", "AI Script",
        "gr2", "Granny Model");

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
      if (fm.readString(4).equals("rass")) {
        rating += 50;
      }

      // Version (3)
      if (fm.readInt() == 3) {
        rating += 5;
      }

      // null
      if (fm.readInt() == 0) {
        rating += 5;
      }

      // Decompressed Directory Length
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
  
  **********************************************************************************************
  **/
  @Override
  public Resource[] read(File path) {
    try {

      addFileTypes();

      FileManipulator fm = new FileManipulator(path, false);

      Exporter_ZLib exporterZLib = Exporter_ZLib.getInstance();
      Exporter_Default exporterDefault = Exporter_Default.getInstance();

      long arcSize = fm.getLength();

      // 4 - Header (rass)
      // 4 - Version (3)
      // 4 - null
      fm.skip(12);

      // 4 - Decompressed Directory Length
      int decompDirLength = fm.readInt();
      FieldValidator.checkLength(decompDirLength);

      int[] xorKey = new int[] { 100, 52, 104, 130, 85, 135, 73, 50, 211, 2, 1, 178, 18, 115, 19, 255, 89, 33, 57, 87, 84, 48, 239, 163, 86, 35, 153, 72, 152, 177, 169, 72 };

      // XOR the next 4 bytes
      // 4 - Compressed Directory Length
      byte[] dirLengthBytes = fm.readBytes(4);
      dirLengthBytes[0] ^= xorKey[0];
      dirLengthBytes[1] ^= xorKey[1];
      dirLengthBytes[2] ^= xorKey[2];
      dirLengthBytes[3] ^= xorKey[3];
      int dirLength = IntConverter.convertLittle(dirLengthBytes);
      FieldValidator.checkLength(dirLength, arcSize);

      // Read in the compressed directory, un-XOR the first 32-4 bytes, then decompress it
      byte[] compressedDirBytes = fm.readBytes(dirLength);
      for (int i = 0, j = 4; i < 28; i++, j++) {
        compressedDirBytes[i] ^= xorKey[j];
      }

      fm.close();
      fm = new FileManipulator(new ByteBuffer(compressedDirBytes));

      byte[] dirBytes = new byte[decompDirLength];
      //Exporter_Deflate exporterDeflate = Exporter_Deflate.getInstance();
      exporterZLib.open(fm, dirLength, decompDirLength);

      for (int b = 0; b < decompDirLength; b++) {
        if (exporterZLib.available()) { // make sure we read the next bit of data, if required
          dirBytes[b] = (byte) exporterZLib.read();
        }
      }

      // open the decompressed data for processing
      fm.close();
      fm = new FileManipulator(new ByteBuffer(dirBytes));

      int numFiles = dirLength / 40;// guessed
      FieldValidator.checkNumFiles(numFiles);

      Resource[] resources = new Resource[numFiles];
      TaskProgressManager.setMaximum(decompDirLength);

      int realNumFiles = 0;
      //for (int i = 0; i < numFiles; i++) {
      while (fm.getRemainingLength() > 0) {
        // 4 - Filename Length
        int filenameLength = fm.readInt();
        FieldValidator.checkFilenameLength(filenameLength);

        // X - Filename
        String filename = fm.readNullString(filenameLength);
        FieldValidator.checkFilename(filename);

        // 4 - File Offset
        long offset = fm.readInt();
        FieldValidator.checkOffset(offset, arcSize);

        // 4 - End-of-File Offset
        int endByte = fm.readInt();

        // 4 - File Length
        //fm.skip(4);
        int length = fm.readInt();

        // 32 - XOR Key for this File Data
        int[] fileXorKey = new int[32];
        for (int x = 0; x < 32; x++) {
          fileXorKey[x] = ByteConverter.unsign(fm.readByte());
        }

        //long length = endByte - offset + 1;
        FieldValidator.checkLength(length, arcSize);

        Exporter_XOR_RepeatingKey exporterXOR = new Exporter_XOR_RepeatingKey(fileXorKey);
        if (length <= 32) {
          // the whole file is XOR'd

          ExporterPlugin[] blockExporters = new ExporterPlugin[] { exporterXOR };
          long[] blockOffsets = new long[] { offset };
          long[] blockLengths = new long[] { length };

          BlockVariableExporterWrapper blockExporter = new BlockVariableExporterWrapper(blockExporters, blockOffsets, blockLengths, blockLengths);
          // We need to use a custom exporter, because we need to read from 2 exporters in blockExporter (XOR followed by Default), then we need
          // to run either Explode or ZLib decompression on that result, which isn't something we can do by chaining normal exporters together.
          Exporter_Custom_SSA_RASS_2 ssaExporter = new Exporter_Custom_SSA_RASS_2(blockExporter);

          //path,id,name,offset,length,decompLength,exporter
          resources[realNumFiles] = new Resource(path, filename, offset, length, length, ssaExporter);
        }
        else {
          // the first 32 bytes are XOR'd, the rest is raw

          ExporterPlugin[] blockExporters = new ExporterPlugin[] { exporterXOR, exporterDefault };
          long[] blockOffsets = new long[] { offset, offset + 32 };
          long[] blockLengths = new long[] { 32, length - 32 };

          BlockVariableExporterWrapper blockExporter = new BlockVariableExporterWrapper(blockExporters, blockOffsets, blockLengths, blockLengths);
          // We need to use a custom exporter, because we need to read from 2 exporters in blockExporter (XOR followed by Default), then we need
          // to run either Explode or ZLib decompression on that result, which isn't something we can do by chaining normal exporters together.
          Exporter_Custom_SSA_RASS_2 ssaExporter = new Exporter_Custom_SSA_RASS_2(blockExporter);

          //path,id,name,offset,length,decompLength,exporter
          resources[realNumFiles] = new Resource(path, filename, offset, length, length, ssaExporter);
        }

        TaskProgressManager.setValue(fm.getOffset());

        realNumFiles++;
        //System.out.println(endByte + " of " + (arcSize-40));
        if (endByte >= arcSize - 40) {
          break;
        }
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