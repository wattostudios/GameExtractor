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
import org.watto.ge.plugin.exporter.Exporter_XOR;
import org.watto.io.FileManipulator;
import org.watto.io.converter.ByteConverter;
import org.watto.task.TaskProgressManager;

/**
**********************************************************************************************

**********************************************************************************************
**/
public class Plugin_ARF_AR extends ArchivePlugin {

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public Plugin_ARF_AR() {

    super("ARF_AR", "ARF_AR");

    //         read write replace rename
    setProperties(true, false, false, false);
    setCanImplicitReplace(true);

    setGames("Snowy: Fish Frenzy",
        "The Apprentice: Los Angeles");
    setExtensions("arf");
    setPlatforms("PC");

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
      if (fm.readString(2).equals("AR")) {
        rating += 50;
      }

      // null
      if (fm.readShort() == 0) {
        rating += 5;
      }

      // Number Of Files
      if (FieldValidator.checkNumFiles(fm.readInt())) {
        rating += 5;
      }

      fm.skip(4);

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

      // RESETTING THE GLOBAL VARIABLES

      FileManipulator fm = new FileManipulator(path, false);

      long arcSize = (int) fm.getLength();

      // 2 - Header (AR)
      // 2 - null
      fm.skip(4);

      // 4 - Number Of Files
      int numFiles = fm.readInt();
      FieldValidator.checkNumFiles(numFiles);

      // 4 - Filename Directory XOR Key
      byte[] xorBytes = fm.readBytes(4);

      // 4 - Directory Offset
      long dirOffset = fm.readInt();
      FieldValidator.checkOffset(dirOffset, arcSize);

      Resource[] resources = new Resource[numFiles];

      TaskProgressManager.setMaximum(numFiles);

      // Loop through directory
      String[] names = new String[numFiles];
      for (int i = 0; i < numFiles; i++) {
        // 1 - Filename Length
        int filenameLength = ByteConverter.unsign(fm.readByte());

        int numRemaining = filenameLength % 4;
        int numXored = filenameLength - numRemaining;

        // X - Filename (Encrypted)
        byte[] filenameBytes = fm.readBytes(numXored);
        for (int f = 0; f < numXored; f += 4) {
          filenameBytes[f] ^= xorBytes[0];
          filenameBytes[f + 1] ^= xorBytes[1];
          filenameBytes[f + 2] ^= xorBytes[2];
          filenameBytes[f + 3] ^= xorBytes[3];
        }

        names[i] = new String(filenameBytes) + fm.readString(numRemaining);

      }

      fm.seek(dirOffset);

      // Loop through directory
      for (int i = 0; i < numFiles; i++) {
        // 2 - File ID (incremental from 1)
        // 2 - Unknown
        fm.skip(4);

        // 4 - File Offset
        long offset = fm.readInt();
        FieldValidator.checkOffset(offset, arcSize);

        // 4 - File Length
        long length = fm.readInt();
        FieldValidator.checkLength(length, arcSize);

        // 1 - File Data XOR Value
        // 3 - Unknown
        //int fileID = IntConverter.changeFormat(fm.readInt());
        int xorValue = ByteConverter.unsign(fm.readByte());
        fm.skip(3);

        String filename = names[i];

        long[] blockOffsets = new long[] { offset, offset + 20 };
        long[] blockLengths = new long[] { 20, length - 20 };
        long[] blockDecompLengths = new long[] { 20, length - 20 };
        ExporterPlugin[] exporters = new ExporterPlugin[] { new Exporter_XOR(xorValue), exporterDefault };
        BlockVariableExporterWrapper blockExporter = new BlockVariableExporterWrapper(exporters, blockOffsets, blockLengths, blockDecompLengths);

        //path,id,name,offset,length,decompLength,exporter
        resources[i] = new Resource(path, filename, offset, length, length, blockExporter);

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
