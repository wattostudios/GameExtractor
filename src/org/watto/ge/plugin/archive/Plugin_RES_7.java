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

import org.watto.Settings;
import org.watto.datatype.Resource;
import org.watto.ge.helper.FieldValidator;
import org.watto.ge.plugin.ArchivePlugin;
import org.watto.ge.plugin.ExporterPlugin;
import org.watto.ge.plugin.exporter.Exporter_XOR;
import org.watto.io.FileManipulator;
import org.watto.task.TaskProgressManager;

/**
**********************************************************************************************

**********************************************************************************************
**/
public class Plugin_RES_7 extends ArchivePlugin {

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public Plugin_RES_7() {

    super("RES_7", "RES_7");

    //         read write replace rename
    setProperties(true, false, false, false);

    setGames("Cybermercs");
    setExtensions("pak", "res");
    setPlatforms("PC");

    // MUST BE LOWER CASE !!!
    //setFileTypes(new FileType("txt", "Text Document", FileType.TYPE_DOCUMENT),
    //             new FileType("bmp", "Bitmap Image", FileType.TYPE_IMAGE)
    //             );

    setTextPreviewExtensions("brj", "h", "inc", "scr"); // LOWER CASE

    //setCanScanForFileTypes(true);

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

      // Number Of Files
      if (FieldValidator.checkNumFiles(fm.readShort())) {
        rating += 5;
      }

      // Encryption Flag (0/1)
      int unknownField = fm.readShort();
      if (unknownField == 0 || unknownField == 1) {
        rating += 5;
      }

      fm.skip(12);

      // filename length
      if (FieldValidator.checkLength(fm.readShort(), Settings.getInt("MaxFilenameLength"))) {
        rating += 5;
      }

      // Encryption (0/101)
      int encryption = fm.readShort();
      if (encryption == 0 || encryption == 101 || encryption == 118) {
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

      ExporterPlugin exporterXOR = new Exporter_XOR(170);

      // 2 - Number Of Files
      short numFiles = fm.readShort();
      FieldValidator.checkNumFiles(numFiles);

      // 2 - Unknown (0/1)
      int encryptionFlag = fm.readShort();

      // 4 - Unknown
      // 4 - Unknown
      fm.skip(8);

      long arcSize = fm.getLength();

      Resource[] resources = new Resource[numFiles];
      TaskProgressManager.setMaximum(numFiles);

      // Loop through directory
      int[] filenameLengths = new int[numFiles];
      int[] entryOffsets = new int[numFiles];
      for (int i = 0; i < numFiles; i++) {
        // 4 - File Entry Offset (pointer into the Details Directory)
        int entryOffset = fm.readInt();
        FieldValidator.checkOffset(entryOffset, arcSize);
        entryOffsets[i] = entryOffset;

        // 2 - Filename Length
        filenameLengths[i] = fm.readShort();

        // 2 - Unknown
        fm.skip(2);
      }

      for (int i = 0; i < numFiles; i++) {
        fm.relativeSeek(entryOffsets[i]);

        // X - Filename (the length is obtained from the directory above)
        String filename = fm.readString(filenameLengths[i]);
        FieldValidator.checkFilename(filename);

        // 4 - File Offset
        int offset = fm.readInt();
        FieldValidator.checkOffset(offset, arcSize);

        // 4 - File Length
        int length = fm.readInt();
        FieldValidator.checkLength(length, arcSize);

        //path,id,name,offset,length,decompLength,exporter
        Resource resource = new Resource(path, filename, offset, length);
        resources[i] = resource;

        if (encryptionFlag == 1) {
          resource.setExporter(exporterXOR);
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
