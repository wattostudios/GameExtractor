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
import org.watto.ge.plugin.exporter.Exporter_QuickBMS_DLL;
import org.watto.ge.plugin.exporter.Exporter_REFPACK;
import org.watto.io.FileManipulator;
import org.watto.io.converter.IntConverter;
import org.watto.task.TaskProgressManager;

/**
**********************************************************************************************

**********************************************************************************************
**/
public class Plugin_TRE_XTRE_2 extends ArchivePlugin {

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public Plugin_TRE_XTRE_2() {

    super("TRE_XTRE_2", "TRE_XTRE_2");

    //         read write replace rename
    setProperties(true, false, false, false);

    setGames("Wing Commander 3");
    setExtensions("tre"); // MUST BE LOWER CASE
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
      if (fm.readString(4).equals("XTRE")) {
        rating += 50;
      }

      // 4 - Version (0)
      if (fm.readInt() == 0) {
        rating += 5;
      }

      // ID Directory Offset
      if (fm.readInt() == 24) {
        rating += 5;
      }

      fm.skip(4);

      long arcSize = fm.getLength();

      // Directory Offset
      if (FieldValidator.checkOffset(fm.readInt(), arcSize)) {
        rating += 5;
      }

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
  @SuppressWarnings("static-access")
  @Override
  public Resource[] read(File path) {
    try {

      // NOTE - Compressed files MUST know their DECOMPRESSED LENGTH
      //      - Uncompressed files MUST know their LENGTH

      addFileTypes();

      Exporter_REFPACK exporter = Exporter_REFPACK.getInstance();
      exporter.setSkipHeaders(true);

      //Exporter_LZWX exporterLZWX = Exporter_LZWX.getInstance();
      //ExporterPlugin exporterLZWX = new Exporter_QuickBMS_Decompression("UNLZWX");
      ExporterPlugin exporterLZWX = new Exporter_QuickBMS_DLL("UNLZWX");

      // RESETTING GLOBAL VARIABLES

      FileManipulator fm = new FileManipulator(path, false);

      long arcSize = fm.getLength();

      // 4 - Header (XTRE)
      // 4 - Version (0)
      // 4 - ID Directory Offset (24)
      // 4 - Filename Directory Offset (if no filename table, this field equals the next field)
      fm.skip(16);

      // 4 - Files Directory Offset
      int dirOffset = fm.readInt();
      FieldValidator.checkOffset(dirOffset, arcSize);

      // 4 - File Data Offset
      int fileDataOffset = fm.readInt();
      FieldValidator.checkOffset(fileDataOffset, arcSize);

      int numFiles = (fileDataOffset - dirOffset) / 8;
      FieldValidator.checkNumFiles(numFiles);

      fm.seek(dirOffset);

      Resource[] resources = new Resource[numFiles];
      TaskProgressManager.setMaximum(numFiles);

      // Loop through directory
      for (int i = 0; i < numFiles; i++) {
        // 4 - File Offset
        int offset = fm.readInt();
        FieldValidator.checkOffset(offset, arcSize);

        // 3 - Decompressed File Length
        // 1 - Compression Flag (0=uncompressed, 192=Compressed)
        byte[] lengthBytes = fm.readBytes(4);
        int decompLength = IntConverter.convertLittle(lengthBytes);

        boolean compressedRefpack = false;
        boolean compressedLZWX = false;
        if ((lengthBytes[3] & 192) == 192) {
          compressedRefpack = true;
          lengthBytes[3] &= 63;
          decompLength = IntConverter.convertLittle(lengthBytes);
        }
        else if ((lengthBytes[3] & 128) == 128) {
          compressedLZWX = true;
          lengthBytes[3] &= 127;
          decompLength = IntConverter.convertLittle(lengthBytes);
        }

        FieldValidator.checkLength(decompLength);

        String filename = Resource.generateFilename(i);

        if (compressedRefpack) {
          //path,name,offset,length,decompLength,exporter
          resources[i] = new Resource(path, filename, offset, decompLength, decompLength, exporter);
        }
        else if (compressedLZWX) {
          //path,name,offset,length,decompLength,exporter
          resources[i] = new Resource(path, filename, offset, decompLength, decompLength, exporterLZWX);
        }
        else {
          //path,name,offset,length,decompLength,exporter
          resources[i] = new Resource(path, filename, offset, decompLength, decompLength);
        }

        TaskProgressManager.setValue(i);
      }

      // Look through to set the compressed lengths
      for (int i = 0; i < numFiles - 1; i++) {
        resources[i].setLength(resources[i + 1].getOffset() - resources[i].getOffset());
      }
      resources[numFiles - 1].setLength(arcSize - resources[numFiles - 1].getOffset());

      fm.close();

      return resources;

    }
    catch (Throwable t) {
      logError(t);
      return null;
    }
  }

}
