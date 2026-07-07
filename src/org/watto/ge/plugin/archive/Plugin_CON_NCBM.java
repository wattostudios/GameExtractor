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
import org.watto.io.FileManipulator;
import org.watto.task.TaskProgressManager;

/**
**********************************************************************************************

**********************************************************************************************
**/
public class Plugin_CON_NCBM extends ArchivePlugin {

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public Plugin_CON_NCBM() {

    super("CON_NCBM", "CON_NCBM");

    //         read write replace rename
    setProperties(true, false, false, false);

    setGames("Advanced Strategic Command");
    setExtensions("con"); // MUST BE LOWER CASE
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
      if (fm.readString(4).equals("NCBM")) {
        rating += 50;
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

      // RESETTING GLOBAL VARIABLES

      FileManipulator fm = new FileManipulator(path, false);

      long arcSize = fm.getLength();

      // 4 - Header (NCBM)
      fm.skip(4);

      // 4 - Directory Offset
      int dirOffset = fm.readInt();
      FieldValidator.checkOffset(dirOffset, arcSize);

      fm.seek(dirOffset);

      // 4 - Number Of Files
      int numFiles = fm.readInt();
      FieldValidator.checkNumFiles(numFiles);

      Resource[] resources = new Resource[numFiles];
      TaskProgressManager.setMaximum(numFiles);

      // Loop through directory
      for (int i = 0; i < numFiles; i++) {
        // 4 - Unknown (1)
        fm.skip(4);

        // 4 - File Offset
        int offset = fm.readInt();
        FieldValidator.checkOffset(offset, arcSize);

        // 4 - Offset to the last byte of the file
        // File Length = LastByteOffset-FileOffset+1
        int length = fm.readInt() - offset + 1;
        FieldValidator.checkLength(length, arcSize);

        // X - Filename
        // 1 - null Filename Terminator
        String filename = fm.readNullString();
        FieldValidator.checkFilename(filename);

        //path,name,offset,length,decompLength,exporter
        resources[i] = new Resource(path, filename, offset, length);

        TaskProgressManager.setValue(i);
      }

      /*
      // check for compression
      fm.getBuffer().setBufferSize(12);
      
      ExporterPlugin exporterBZip = Exporter_BZip2.getInstance();
      
      for (int i = 0; i < numFiles; i++) {
        Resource resource = resources[i];
      
        long offset = resource.getOffset();
        fm.seek(offset);
      
        String header = fm.readString(8);
        if (header.equals("MBZLB2X!")) {
          resource.setOffset(offset + 12);
      
          long length = resource.getDecompressedLength();
          length -= 12;
      
          resource.setLength(length);
          resource.setDecompressedLength(length);
          resource.setExporter(exporterBZip);
        }
      }
      */

      fm.close();

      return resources;

    }
    catch (Throwable t) {
      logError(t);
      return null;
    }
  }

}
