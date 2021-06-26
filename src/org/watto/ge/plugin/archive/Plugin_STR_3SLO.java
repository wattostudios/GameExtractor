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
import org.watto.datatype.Archive;
import org.watto.datatype.Resource;
import org.watto.ge.helper.FieldValidator;
import org.watto.ge.plugin.ArchivePlugin;
import org.watto.ge.plugin.ExporterPlugin;
import org.watto.ge.plugin.exporter.Exporter_REFPACK;
import org.watto.io.FileManipulator;
import org.watto.io.FilenameSplitter;
import org.watto.io.converter.IntConverter;
import org.watto.task.TaskProgressManager;

/**
**********************************************************************************************

**********************************************************************************************
**/
public class Plugin_STR_3SLO extends ArchivePlugin {

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public Plugin_STR_3SLO() {

    super("STR_3SLO", "STR_3SLO");

    //         read write replace rename
    setProperties(true, false, false, false);

    setGames("Dead Space");
    setExtensions("str"); // MUST BE LOWER CASE
    setPlatforms("PC");

    // MUST BE LOWER CASE !!!
    //setFileTypes(new FileType("txt", "Text Document", FileType.TYPE_DOCUMENT),
    //             new FileType("bmp", "Bitmap Image", FileType.TYPE_IMAGE)
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
      if (fm.readString(4).equals("3slo")) {
        rating += 50;
      }

      // 4 - Header Length (12)
      if (fm.readInt() == 12) {
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

      //ExporterPlugin exporter = new Exporter_QuickBMS_Decompression("DK2");
      ExporterPlugin exporter = Exporter_REFPACK.getInstance();

      // RESETTING GLOBAL VARIABLES

      FileManipulator fm = new FileManipulator(path, false);

      long arcSize = fm.getLength();

      fm.getBuffer().setBufferSize(32);

      // 4 - Header (3slo)
      // 4 - Header Length (12)
      // 2 - Unknown
      // 2 - Unknown
      fm.seek(12);

      int numFiles = Archive.getMaxFiles();

      Resource[] resources = new Resource[numFiles];
      TaskProgressManager.setMaximum(arcSize);

      // Loop through directory
      int realNumFiles = 0;
      String filename = "";
      while (fm.getOffset() < arcSize) {
        // 4 - File Header (COHS)
        fm.skip(4);

        // 4 - File Length (including these 2 header fields)
        int length = fm.readInt() - 8;
        FieldValidator.checkLength(length, arcSize);

        long offset = fm.getOffset();

        if (filename.equals("")) {
          filename = Resource.generateFilename(realNumFiles);
        }

        String fileHeader = fm.readString(4);
        if (fileHeader.equals("kapR")) {
          // compressed

          // 4 - Compressed Length
          //length = fm.readInt();
          //FieldValidator.checkLength(length, arcSize);
          fm.skip(4);
          length -= 8;

          offset += 8;

          // find the decompLength
          int headerSize = 5;

          // 2 bytes - Signature
          short signature = fm.readShort();
          if (signature > 0) { // top bit is 0
            // 3 bytes - Compressed Size
            fm.skip(3);
            headerSize = 8;
          }

          // 3 bytes - Decompressed Size
          int decompLength = IntConverter.convertBig(new byte[] { 0, fm.readByte(), fm.readByte(), fm.readByte() });

          fm.skip(length - headerSize);

          //path,name,offset,length,decompLength,exporter
          resources[realNumFiles] = new Resource(path, filename, offset, length, decompLength, exporter);

        }
        else if (fileHeader.equals("RDHS")) {
          // 32 - Junk
          fm.skip(32);

          // X - Filename
          // 1 - null Terminator
          String realFilename = fm.readNullString();

          // X - File Path
          // 1 - null Terminator
          filename = fm.readNullString();
          if (realFilename.indexOf(".") > 0) {
            filename = FilenameSplitter.getDirectory(filename) + "\\" + realFilename;
          }

          // X - Junk
          fm.seek(offset + length);
          continue;
        }
        else {
          // not compressed
          fm.skip(length - 4);

          //path,name,offset,length,decompLength,exporter
          resources[realNumFiles] = new Resource(path, filename, offset, length);
        }

        filename = "";

        TaskProgressManager.setValue(offset);
        realNumFiles++;
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
