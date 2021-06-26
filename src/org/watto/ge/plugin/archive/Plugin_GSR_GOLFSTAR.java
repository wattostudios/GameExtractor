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
import org.watto.ge.plugin.exporter.Exporter_XOR;
import org.watto.io.FileManipulator;
import org.watto.io.buffer.ManipulatorBuffer;
import org.watto.io.buffer.XORBufferWrapper;
import org.watto.task.TaskProgressManager;

/**
**********************************************************************************************

**********************************************************************************************
**/
public class Plugin_GSR_GOLFSTAR extends ArchivePlugin {

  /**
  **********************************************************************************************

  **********************************************************************************************
  **/
  public Plugin_GSR_GOLFSTAR() {

    super("GSR_GOLFSTAR", "GSR_GOLFSTAR");

    //         read write replace rename
    setProperties(true, false, false, false);

    setGames("GolfStar");
    setExtensions("gsr"); // MUST BE LOWER CASE
    setPlatforms("PC");

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

      // 36 - Header ("GolfStar Resource File, Version 1.1" + null)
      if (fm.readString(35).equals("GolfStar Resource File, Version 1.1")) {
        rating += 50;
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

      // 36 - Header ("GolfStar Resource File, Version 1.1" + null)
      // 4 - Unknown
      fm.skip(40);

      // 4 - Directory Offset
      int dirOffset = fm.readInt();
      FieldValidator.checkOffset(dirOffset, arcSize);

      // 4 - Directory Length?
      int dirLength = fm.readInt();
      FieldValidator.checkLength(dirLength, arcSize);

      fm.seek(dirOffset);

      int numFiles = dirLength / 260;

      Resource[] resources = new Resource[numFiles];
      TaskProgressManager.setMaximum(numFiles);

      //
      //
      // THE DIRECTORY IS XOR WITH (byte)69
      //
      //
      ManipulatorBuffer buffer = fm.getBuffer();
      XORBufferWrapper bufferWrapper = new XORBufferWrapper(buffer, 69);
      fm.setBuffer(bufferWrapper);

      // Loop through directory
      for (int i = 0; i < numFiles; i++) {

        // 248 - Filename (null terminated, filled with nulls)
        String filename = fm.readNullString(248);
        FieldValidator.checkFilename(filename);

        // 4 - File Offset
        int offset = fm.readInt();
        FieldValidator.checkOffset(offset, arcSize);

        // 4 - Compressed File Length
        int length = fm.readInt();
        FieldValidator.checkLength(length, arcSize);

        // 4 - Decompressed File Length
        int decompLength = fm.readInt();
        FieldValidator.checkLength(decompLength);

        //path,name,offset,length,decompLength,exporter
        resources[i] = new Resource(path, filename, offset, length, decompLength);

        TaskProgressManager.setValue(i);
      }

      // Finished reading the directory - don't need to XOR the FileManipulator any more
      fm.setBuffer(buffer);

      // Loop through the file data and get the XOR values
      for (int i = 0; i < numFiles; i++) {
        Resource resource = resources[i];

        long offset = resource.getOffset();
        fm.seek(offset);

        // 1 - XOR Value (XOR this value with (byte)12, and this gives the XOR value to use for the rest of the file)
        int xorValue = (fm.readByte()) ^ 12;

        //resource.setOffset(offset + 1);
        //resource.setLength(resource.getLength() - 1);
        resource.setExporter(new Exporter_XOR(xorValue));
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
