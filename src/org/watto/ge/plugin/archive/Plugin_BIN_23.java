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
import org.watto.ge.plugin.exporter.Exporter_ZLib;
import org.watto.io.FileManipulator;
import org.watto.io.converter.IntConverter;
import org.watto.task.TaskProgressManager;

/**
**********************************************************************************************

**********************************************************************************************
**/
public class Plugin_BIN_23 extends ArchivePlugin {

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public Plugin_BIN_23() {

    super("BIN_23", "BIN_23");

    //         read write replace rename
    setProperties(true, false, false, false);

    setGames("Mario Party 4",
        "Mario Party 5",
        "Mario Party 6",
        "Mario Party 7");
    setExtensions("bin"); // MUST BE LOWER CASE
    setPlatforms("GameCube");

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

      // Number Of Files
      if (FieldValidator.checkNumFiles(IntConverter.changeFormat(fm.readInt()))) {
        rating += 5;
      }

      long arcSize = fm.getLength();

      // First File Offset
      if (FieldValidator.checkOffset(IntConverter.changeFormat(fm.readInt()), arcSize)) {
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

      ExporterPlugin zlibExporter = Exporter_ZLib.getInstance();
      //ExporterPlugin lzssExporter = Exporter_LZSS.getInstance();

      // RESETTING GLOBAL VARIABLES

      FileManipulator fm = new FileManipulator(path, false);

      long arcSize = fm.getLength();

      // 4 - Number Of Files
      int numFiles = IntConverter.changeFormat(fm.readInt());
      FieldValidator.checkNumFiles(numFiles);

      Resource[] resources = new Resource[numFiles];
      TaskProgressManager.setMaximum(numFiles);

      // Loop through directory
      long[] offsets = new long[numFiles];
      for (int i = 0; i < numFiles; i++) {

        // 4 - File Offset
        int offset = IntConverter.changeFormat(fm.readInt());
        FieldValidator.checkOffset(offset, arcSize);
        offsets[i] = offset;
      }

      fm.getBuffer().setBufferSize(16); // for quick reads

      for (int i = 0; i < numFiles; i++) {
        long offset = offsets[i];
        fm.seek(offset);

        // 4 - File Length
        int length = IntConverter.changeFormat(fm.readInt());
        //System.out.println(offset + "\t" + length);

        FieldValidator.checkLength(length);

        // 4 - Flags
        int flags = IntConverter.changeFormat(fm.readInt());

        offset += 8;

        String filename = Resource.generateFilename(i);

        if ((flags & 6) == 6) {
          // 4 - Decompressed File Length
          int decompLength = IntConverter.changeFormat(fm.readInt());
          FieldValidator.checkLength(decompLength);

          // 4 - Compressed File Length
          length = IntConverter.changeFormat(fm.readInt());
          FieldValidator.checkLength(length, arcSize);

          // X - File Data (ZLib Compression)
          offset += 8;

          //path,name,offset,length,decompLength,exporter
          resources[i] = new Resource(path, filename, offset, length, decompLength, zlibExporter);

        }
        else {
          int decompLength = length;

          if (i == numFiles - 1) {
            length = (int) (arcSize - offset);
          }
          else {
            length = (int) (offsets[i + 1] - offset);
          }

          // X - File Data (unknown compression - maybe an RLE?)
          //path,name,offset,length,decompLength,exporter
          resources[i] = new Resource(path, filename, offset, length, decompLength);//, lzssExporter);

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
