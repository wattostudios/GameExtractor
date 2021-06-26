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
import org.watto.ge.plugin.exporter.Exporter_Deflate;
import org.watto.io.FileManipulator;
import org.watto.io.converter.StringConverter;
import org.watto.task.TaskProgressManager;

/**
**********************************************************************************************

**********************************************************************************************
**/
public class Plugin_RES_15 extends ArchivePlugin {

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public Plugin_RES_15() {

    super("RES_15", "RES_15");

    //         read write replace rename
    setProperties(true, false, false, false);

    setGames("Alpha Prime");
    setExtensions("res"); // MUST BE LOWER CASE
    setPlatforms("PC");

    // MUST BE LOWER CASE !!!
    //setFileTypes(new FileType("txt", "Text Document", FileType.TYPE_DOCUMENT),
    //             new FileType("bmp", "Bitmap Image", FileType.TYPE_IMAGE)
    //             );

    setTextPreviewExtensions("ase", "def", "des", "dpl", "eff", "ent", "h", "nav", "ptc", "s", "scn", "spr", "xoc"); // LOWER CASE

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

      fm.skip(4);

      if (fm.readShort() == 20 && fm.readShort() == 2) {
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

      ExporterPlugin exporter = Exporter_Deflate.getInstance();

      // RESETTING GLOBAL VARIABLES

      FileManipulator fm = new FileManipulator(path, false);

      long arcSize = fm.getLength();

      fm.seek(arcSize - 6);

      // 4 - Directory Offset
      int dirOffset = fm.readInt();
      FieldValidator.checkOffset(dirOffset, arcSize);

      fm.seek(dirOffset);

      int numFiles = Archive.getMaxFiles();

      Resource[] resources = new Resource[numFiles];
      TaskProgressManager.setMaximum(arcSize);

      // Loop through directory
      long endDirOffset = arcSize - 22;
      int realNumFiles = 0;
      while (fm.getOffset() < endDirOffset) {
        // 4 - Unknown
        // 2 - Unknown (20)
        // 2 - Unknown (20)
        // 2 - Unknown (2)
        fm.skip(10);

        // 2 - Entry Type (0=Directory, 8=File)
        int entryType = fm.readShort();

        int length = 0;
        int decompLength = 0;
        if (entryType == 8) {
          // 8 - CRC?
          fm.skip(8);

          // 4 - File Length
          length = fm.readInt();
          FieldValidator.checkLength(length, arcSize);

          // 4 - Decompressed File Length?
          decompLength = fm.readInt();
          FieldValidator.checkLength(decompLength);
        }
        else if (entryType == 0) {
          // 4 - Unknown
          // 12 - null
          fm.skip(16);
        }
        else {
          ErrorLogger.log("[RES_15]: Unknown entry type: " + entryType);
          return null;
        }

        // 4 - Filename Length
        int filenameLength = fm.readInt();
        FieldValidator.checkFilenameLength(filenameLength);

        // 6 - null
        // 4 - Unknown (32/48)
        fm.skip(10);

        // 4 - File Offset
        int offset = fm.readInt();
        FieldValidator.checkOffset(offset, arcSize);

        // X - Encrypted Filename (XOR with repeating key (bytes)105,110,115,97,110,105,116,121)
        byte[] filenameBytes = fm.readBytes(filenameLength);

        int[] key = new int[] { 105, 110, 115, 97, 110, 105, 116, 121 };
        int keyPos = 0;
        for (int f = 0; f < filenameLength; f++) {
          filenameBytes[f] ^= key[keyPos];
          keyPos++;
          if (keyPos >= 8) {
            keyPos = 0;
          }
        }

        //String filename = Resource.generateFilename(realNumFiles);
        String filename = StringConverter.convertLittle(filenameBytes);

        if (entryType == 8) {

          offset += (30 + filenameLength); // skip over the file header

          //path,name,offset,length,decompLength,exporter
          resources[realNumFiles] = new Resource(path, filename, offset, length, decompLength, exporter);
          realNumFiles++;

          TaskProgressManager.setValue(offset);
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
