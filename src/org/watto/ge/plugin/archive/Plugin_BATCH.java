/*
 * Application:  Game Extractor
 * Author:       wattostudios
 * Website:      http://www.watto.org
 * Copyright:    Copyright (c) 2002-2026 wattostudios
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
import org.watto.ge.plugin.exporter.Exporter_Custom_BATCH;
import org.watto.ge.plugin.exporter.Exporter_Custom_BATCH_FIN;
import org.watto.io.FileManipulator;
import org.watto.task.TaskProgressManager;

/**
**********************************************************************************************

**********************************************************************************************
**/
public class Plugin_BATCH extends ArchivePlugin {

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public Plugin_BATCH() {

    super("BATCH", "BATCH");

    //         read write replace rename
    setProperties(true, false, false, false);

    setGames("Switchball");
    setExtensions("batch");
    setPlatforms("PC");

    //setFileTypes("","",
    //             "",""
    //             );

    setTextPreviewExtensions("fx", "vna", "vng", "vnl", "vnp", "vns", "xui"); // LOWER CASE

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
      if (fm.readString(20).equals("THIS IS A BATCH FILE")) {
        rating += 50;
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

      //ExporterPlugin exporterXMemDecompress = new Exporter_QuickBMS_Decompression("xmemdecompress");
      ExporterPlugin exporter = Exporter_Custom_BATCH.getInstance();
      ExporterPlugin exporterFIN = Exporter_Custom_BATCH_FIN.getInstance();

      // RESETTING GLOBAL VARIABLES

      FileManipulator fm = new FileManipulator(path, false);

      long arcSize = fm.getLength();

      // 32 - Header ("THIS IS A BATCH FILE" + nulls to fill)
      fm.skip(32);

      // 4 - Number Of Files
      int numFiles = fm.readInt();
      FieldValidator.checkNumFiles(numFiles);

      Resource[] resources = new Resource[numFiles];
      TaskProgressManager.setMaximum(numFiles);

      // Loop through directory
      int[] offsets = new int[numFiles];
      int[] lengths = new int[numFiles];
      String[] names = new String[numFiles];

      for (int i = 0; i < numFiles; i++) {
        // 4 - File Offset
        int offset = fm.readInt();
        FieldValidator.checkOffset(offset, arcSize);
        offsets[i] = offset;

        // 4 - File Length (not including the 3 File Data Header Fields)
        int length = fm.readInt();
        FieldValidator.checkLength(length, arcSize);
        lengths[i] = length;

        // 4 - Filename Length
        int filenameLength = fm.readInt();
        FieldValidator.checkFilenameLength(filenameLength);

        // X - Filename
        String filename = fm.readString(filenameLength);
        names[i] = filename;
      }

      fm.getBuffer().setBufferSize(11);

      for (int i = 0; i < numFiles; i++) {
        long offset = offsets[i];

        fm.seek(offset);

        // 3 - Compression Header (VNZ)
        String compression = fm.readString(3);
        if (compression.equals("VNZ")) {

          // 4 - Compressed Length
          long length = fm.readInt();
          FieldValidator.checkLength(length, arcSize);

          // 4 - Decompressed Length
          long decompLength = fm.readInt();
          FieldValidator.checkLength(decompLength, arcSize);

          offset += 11;

          //path,name,offset,length,decompLength,exporter
          resources[i] = new Resource(path, names[i], offset, length, decompLength, exporterFIN);//, exporterXMemDecompress);
        }
        else {
          int length = lengths[i];

          //path,name,offset,length,decompLength,exporter
          resources[i] = new Resource(path, names[i], offset, length, length, exporter);
        }

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
