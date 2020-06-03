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
import org.watto.io.FileManipulator;
import org.watto.io.FilenameSplitter;
import org.watto.task.TaskProgressManager;

/**
**********************************************************************************************

**********************************************************************************************
**/
public class Plugin_NAR extends ArchivePlugin {

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public Plugin_NAR() {

    super("NAR", "NAR");

    //         read write replace rename
    setProperties(true, false, false, false);

    setGames("Eternam");
    setExtensions("nar", "pak"); // MUST BE LOWER CASE
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

      long arcSize = fm.getLength();

      // First File Offset
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

      //ExporterPlugin exporter = Exporter_ZLib.getInstance();

      // RESETTING GLOBAL VARIABLES

      FileManipulator fm = new FileManipulator(path, false);

      long arcSize = fm.getLength();

      // 4 - File Offset (can be null)
      int firstFileOffset = fm.readInt();
      while (firstFileOffset == 0 && fm.getOffset() < 1000) {
        firstFileOffset = fm.readInt();
      }
      int numFiles = firstFileOffset / 4;
      FieldValidator.checkNumFiles(numFiles);

      fm.seek(0);

      int realNumFiles = 0;

      Resource[] resources = new Resource[numFiles];
      TaskProgressManager.setMaximum(numFiles);

      // Loop through directory
      for (int i = 0; i < numFiles; i++) {

        // 4 - File Offset (can be null)
        int offset = fm.readInt();
        FieldValidator.checkOffset(offset, arcSize);

        if (offset != 0) {
          String filename = Resource.generateFilename(realNumFiles);

          //path,name,offset,length,decompLength,exporter
          resources[realNumFiles] = new Resource(path, filename, offset);

          TaskProgressManager.setValue(i);
          realNumFiles++;
        }
      }

      resources = resizeResources(resources, realNumFiles);

      if (FilenameSplitter.getExtension(fm.getFile()).equalsIgnoreCase("pak")) {
        fm.getBuffer().setBufferSize(36);

        for (int i = 0; i < realNumFiles; i++) {
          Resource resource = resources[i];

          // 4 - null
          fm.seek(resource.getOffset() + 4);

          // 4 - Compressed File Length
          int length = fm.readInt();
          FieldValidator.checkLength(length, arcSize);

          // 4 - Decompressed File Length?
          int decompLength = fm.readInt();
          FieldValidator.checkLength(decompLength);

          // 2 - Unknown (1)
          // 2 - Maximum Filename Length (20)
          // 2 - Unknown (5193)
          fm.skip(6);

          // 18 - Filename (null terminated, filled with junk)
          String filename = fm.readNullString(18);
          FieldValidator.checkFilename(filename);

          // X - File Data
          resource.setOffset(fm.getOffset());
          resource.setLength(length);
          resource.setDecompressedLength(decompLength);
          resource.setName(filename);
          resource.setOriginalName(filename);
        }
      }
      else {
        calculateFileSizes(resources, arcSize);
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
