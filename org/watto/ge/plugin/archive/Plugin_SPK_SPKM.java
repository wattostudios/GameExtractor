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
import org.watto.io.FileManipulator;
import org.watto.task.TaskProgressManager;

/**
**********************************************************************************************

**********************************************************************************************
**/
public class Plugin_SPK_SPKM extends ArchivePlugin {

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public Plugin_SPK_SPKM() {

    super("SPK_SPKM", "SPK_SPKM");

    //         read write replace rename
    setProperties(true, false, false, false);

    setGames("Blade and Sword 2");
    setExtensions("spk"); // MUST BE LOWER CASE
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

      // 4 - Header
      if (fm.readString(4).equals("spkm")) {
        rating += 50;
      }

      // 4 - Version ("1.0" + null)
      if (fm.readString(3).equals("1.0")) {
        rating += 5;
      }
      fm.skip(1);

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

      //ExporterPlugin exporter = Exporter_ZLib.getInstance();

      // RESETTING GLOBAL VARIABLES

      FileManipulator fm = new FileManipulator(path, false);

      long arcSize = fm.getLength();

      fm.getBuffer().setBufferSize(256);

      // 4 - Header ("spkm")
      // 4 - Version ("1.0" + null)
      // 4 - Version? (1)
      // 4 - Padding Multiple (256)
      // 16 - Checksum?
      // 4 - Unknown
      // 4 - Unknown
      // 216 - null Padding to offset 256
      fm.seek(256);

      int numFiles = Archive.getMaxFiles();

      Resource[] resources = new Resource[numFiles];
      TaskProgressManager.setMaximum(arcSize);

      // Loop through directory
      int realNumFiles = 0;
      while (fm.getOffset() < arcSize) {

        // 4 - Header ("spkm")
        if (!(fm.readString(4).equals("spkm"))) {
          fm.skip(252);// Skip over a 256-block of unknown data
          continue;
        }
        // 2 - File Header ("rd")
        // 4 - Unknown (768)
        // 12 - Checksum?
        // 12 - null
        // 2 - Unknown (128)
        // 2 - File ID? (Incremental from 1)
        fm.skip(32);

        // 4 - File Length?
        int length = fm.readInt();
        FieldValidator.checkLength(length, arcSize);

        // 4 - File Length (including all these header fields and any padding) [*256]
        int paddedLength = fm.readInt() * 256;
        FieldValidator.checkLength(length, arcSize);

        if (paddedLength < length) {
          length = paddedLength - 48 - 16;
        }

        // 4 - Unknown
        fm.skip(4);

        // X - File Data (compressed/encrypted?)
        long offset = fm.getOffset();
        fm.skip(length);

        // 16 - Checksum?
        fm.skip(16);

        // 0-255 - null Padding to a multiple of 256 bytes
        fm.skip(calculatePadding(fm.getOffset(), 256));

        String filename = Resource.generateFilename(realNumFiles);

        //path,name,offset,length,decompLength,exporter
        resources[realNumFiles] = new Resource(path, filename, offset, length);
        realNumFiles++;

        TaskProgressManager.setValue(offset);
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
