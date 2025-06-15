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

import org.watto.datatype.Resource;
import org.watto.ge.helper.FieldValidator;
import org.watto.ge.plugin.ArchivePlugin;
import org.watto.ge.plugin.ExporterPlugin;
import org.watto.ge.plugin.exporter.Exporter_QuickBMS_DLL;
import org.watto.io.FileManipulator;
import org.watto.io.converter.ShortConverter;
import org.watto.task.TaskProgressManager;

/**
**********************************************************************************************

**********************************************************************************************
**/
public class Plugin_BIG_RBF1 extends ArchivePlugin {

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public Plugin_BIG_RBF1() {

    super("BIG_RBF1", "BIG_RBF1");

    //         read write replace rename
    setProperties(true, false, false, false);

    setGames("Homeworld Classic");
    setExtensions("big");
    setPlatforms("PC");

    setTextPreviewExtensions("bat", "cred", "dist", "ebg", "l", "level", "list", "lod", "lst", "mif", "mission", "missphere", "plug", "script", "shp"); // LOWER CASE

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
      if (fm.readString(7).equals("RBF1.23")) {
        rating += 50;
      }

      // Number Of Files
      if (FieldValidator.checkNumFiles(fm.readInt())) {
        rating += 5;
      }

      // Version (1)
      if (fm.readInt() == 1) {
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

      //ExporterPlugin exporter = new Exporter_QuickBMS_Decompression("TDCB_lzss");
      ExporterPlugin exporter = new Exporter_QuickBMS_DLL("TDCB_lzss");

      // RESETTING GLOBAL VARIABLES

      FileManipulator fm = new FileManipulator(path, false);

      long arcSize = fm.getLength();

      // 7 - Header (RBF1.23)
      fm.skip(7);

      // 4 - Number Of Files
      int numFiles = fm.readInt();
      FieldValidator.checkNumFiles(numFiles);

      // 4 - Version (1)
      fm.skip(4);

      Resource[] resources = new Resource[numFiles];
      TaskProgressManager.setMaximum(numFiles);

      // Loop through directory
      int realNumFiles = 0;
      int[] filenameLengths = new int[numFiles];
      long[] filenameOffsets = new long[numFiles];
      for (int i = 0; i < numFiles; i++) {
        // 4 - Filename CRC 1
        // 4 - Filename CRC 2
        fm.skip(8);

        // 4 - Filename Length
        int filenameLength = ShortConverter.unsign(fm.readShort());
        fm.skip(2);
        FieldValidator.checkFilenameLength(filenameLength);
        filenameLengths[i] = filenameLength;

        // 4 - Compressed Length?
        int length = fm.readInt();
        FieldValidator.checkLength(length, arcSize);

        // 4 - Decompressed Length?
        int decompLength = fm.readInt();
        FieldValidator.checkLength(decompLength);

        // 4 - File Offset
        int offset = fm.readInt();
        FieldValidator.checkOffset(offset, arcSize);

        // 4 - Timestamp
        // 4 - null
        // 4 - Unknown
        fm.skip(12);

        filenameOffsets[i] = offset;

        offset += filenameLength + 1;

        String filename = Resource.generateFilename(i);

        //path,name,offset,length,decompLength,exporter
        if (length == decompLength) {
          resources[i] = new Resource(path, filename, offset, length, decompLength);
        }
        else {
          resources[i] = new Resource(path, filename, offset, length, decompLength, exporter);
        }

        realNumFiles++;

        TaskProgressManager.setValue(i);
      }

      if (numFiles != realNumFiles) {
        resources = resizeResources(resources, realNumFiles);
        numFiles = realNumFiles;
      }

      // get the filenames
      fm.getBuffer().setBufferSize(256);
      for (int i = 0; i < numFiles; i++) {
        Resource resource = resources[i];

        long nameOffset = filenameOffsets[i];
        int nameLength = filenameLengths[i];

        fm.seek(nameOffset);

        byte[] nameBytes = fm.readBytes(nameLength);
        int last_char = 0xD5;
        for (int p = 0; p < nameLength; p++) {
          last_char ^= nameBytes[p];
          nameBytes[p] = (byte) last_char;
        }

        String filename = new String(nameBytes);
        resource.setName(filename);
        resource.setOriginalName(filename);
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
