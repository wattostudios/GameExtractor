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
import org.watto.datatype.FileType;
import org.watto.datatype.Resource;
import org.watto.ge.helper.FieldValidator;
import org.watto.ge.plugin.ArchivePlugin;
import org.watto.ge.plugin.ExporterPlugin;
import org.watto.ge.plugin.exporter.Exporter_ZLib;
import org.watto.io.FileManipulator;
import org.watto.task.TaskProgressManager;

/**
**********************************************************************************************

**********************************************************************************************
**/
public class Plugin_PAK_PAK_2 extends ArchivePlugin {

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public Plugin_PAK_PAK_2() {

    super("PAK_PAK_2", "PAK_PAK_2");

    //         read write replace rename
    setProperties(true, false, false, false);

    setGames("The Club");
    setExtensions("pak"); // MUST BE LOWER CASE
    setPlatforms("PC");

    // MUST BE LOWER CASE !!!
    setFileTypes(new FileType("pct", "PCT Image", FileType.TYPE_IMAGE));

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
      if (fm.readString(4).equals("PAK" + (char) 0)) {
        rating += 50;
      }

      fm.skip(4);

      //long arcSize = fm.getLength();

      // Directory Offset
      if (fm.readInt() == 2048) {
        rating += 5;
      }

      fm.skip(4);

      // Directory Offset
      if (fm.readInt() == 2048) {
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

      // 4 - Header ("PAK" + null)
      // 2 - Unknown (32)
      // 2 - Unknown (2)
      // 4 - Directory Offset (2048)
      // 4 - null
      // 4 - Directory Offset (2048)
      // 4 - Unknown (0/2)
      // X - null padding and junk to offset 2048
      fm.seek(2048);

      // 4 - Number Of Files and Directories
      int numFiles = fm.readInt();
      FieldValidator.checkNumFiles(numFiles);

      Resource[] resources = new Resource[numFiles];
      TaskProgressManager.setMaximum(numFiles);

      // Loop through directory
      int realNumFiles = 0;
      for (int i = 0; i < numFiles; i++) {
        // X - Filename (null)
        String filename = fm.readNullString();
        FieldValidator.checkFilename(filename);

        // 4 - File Offset [*2048]
        int offset = fm.readInt();
        if (offset == -1) {
          // directory
          fm.skip(32);
          continue;
        }
        offset *= 2048;
        FieldValidator.checkOffset(offset, arcSize);

        // 4 - Decompressed File Length?
        int decompressedLength = fm.readInt();
        FieldValidator.checkLength(decompressedLength);

        // 4 - File Type ID? (1=txt,6=ini,13=loc/bin)
        // 4 - Unknown
        // 20 - Hash?
        fm.skip(28);

        //path,name,offset,length,decompLength,exporter
        resources[realNumFiles] = new Resource(path, filename, offset, decompressedLength, decompressedLength);

        TaskProgressManager.setValue(i);
        realNumFiles++;
      }

      fm.getBuffer().setBufferSize(8);

      // get the compressed lengths
      for (int i = 0; i < realNumFiles; i++) {
        Resource resource = resources[i];

        fm.seek(resource.getOffset());

        // 8 - Compressed File Length
        int length = fm.readInt();
        FieldValidator.checkLength(length, arcSize);
        fm.skip(4);

        resource.setLength(length);

        if (length != resource.getDecompressedLength()) {
          resource.setExporter(exporter);
        }

        // X - File Data (ZLib Compression)
        resource.setOffset(fm.getOffset());
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
