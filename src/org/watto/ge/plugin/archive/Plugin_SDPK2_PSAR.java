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
import java.util.Arrays;
import org.watto.datatype.Resource;
import org.watto.ge.helper.FieldValidator;
import org.watto.ge.plugin.ArchivePlugin;
import org.watto.ge.plugin.ExporterPlugin;
import org.watto.ge.plugin.exporter.Exporter_ZLib_CompressedSizeOnly;
import org.watto.io.FileManipulator;
import org.watto.io.converter.IntConverter;
import org.watto.io.converter.ShortConverter;
import org.watto.task.TaskProgressManager;

/**
**********************************************************************************************

**********************************************************************************************
**/
public class Plugin_SDPK2_PSAR extends ArchivePlugin {

  /**
  **********************************************************************************************

  **********************************************************************************************
  **/
  public Plugin_SDPK2_PSAR() {

    super("SDPK2_PSAR", "SDPK2_PSAR");

    //         read write replace rename
    setProperties(true, false, false, false);

    setGames("Brink");
    setExtensions("sdpk2"); // MUST BE LOWER CASE
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
      if (fm.readString(4).equals("PSAR")) {
        rating += 50;
      }

      // 2 - Version Major? (1)
      if (ShortConverter.changeFormat(fm.readShort()) == 1) {
        rating += 5;
      }

      // 2 - Version Minor? (4)
      if (ShortConverter.changeFormat(fm.readShort()) == 4) {
        rating += 5;
      }

      // 4 - Compression Algorithm (zlib)
      if (fm.readString(4).equals("zlib")) {
        rating += 5;
      }

      fm.skip(8);

      // Number Of Files
      if (FieldValidator.checkNumFiles(IntConverter.changeFormat(fm.readInt()))) {
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

      ExporterPlugin exporter = Exporter_ZLib_CompressedSizeOnly.getInstance();

      // RESETTING GLOBAL VARIABLES

      FileManipulator fm = new FileManipulator(path, false);

      long arcSize = fm.getLength();

      // 4 - Header (PSAR)
      // 2 - Version Major? (1)
      // 2 - Version Minor? (4)
      // 4 - Compression Algorithm (zlib)
      // 4 - Unknown
      // 4 - Directory Entry Size (30)
      fm.skip(20);

      // 4 - Number of Files
      int numFiles = IntConverter.changeFormat(fm.readInt());
      FieldValidator.checkNumFiles(numFiles);

      // 4 - Unknown
      // 4 - Unknown (2)
      fm.skip(8);

      // Skip the first (blank) entry
      fm.skip(30);
      numFiles--;

      Resource[] resources = new Resource[numFiles];
      TaskProgressManager.setMaximum(numFiles);

      // Loop through directory
      long[] offsets = new long[numFiles];
      for (int i = 0; i < numFiles; i++) {
        // 16 - Hash?
        fm.skip(16);

        /*
        // 4 - Decompressed Length?
        int decompLength = IntConverter.changeFormat(fm.readInt());
        FieldValidator.checkLength(decompLength);

        // 4 - Compressed Length?
        int length = IntConverter.changeFormat(fm.readInt());
        FieldValidator.checkLength(length, arcSize);
        */
        fm.skip(8);

        // 2 - Unknown
        fm.skip(2);

        // 4 - File Offset
        long offset = IntConverter.unsign(IntConverter.changeFormat(fm.readInt()));
        FieldValidator.checkOffset(offset, arcSize);
        offsets[i] = offset;

        String filename = Resource.generateFilename(i);

        //path,name,offset,length,decompLength,exporter
        resources[i] = new Resource(path, filename, offset);
        resources[i].setExporter(exporter);

        TaskProgressManager.setValue(i);
      }

      Arrays.sort(offsets);

      for (int i = 0; i < numFiles; i++) {
        Resource resource = resources[i];

        long thisOffset = resource.getOffset();
        int arrayPos = Arrays.binarySearch(offsets, thisOffset);

        if (arrayPos == numFiles - 1) {
          long length = arcSize - thisOffset;
          resource.setLength(length);
          resource.setDecompressedLength(length);
        }
        else {
          long length = offsets[arrayPos + 1] - offsets[arrayPos];
          resource.setLength(length);
          resource.setDecompressedLength(length);
        }

      }

      //calculateFileSizes(resources, arcSize);

      fm.close();

      return resources;

    }
    catch (Throwable t) {
      logError(t);
      return null;
    }
  }

}
