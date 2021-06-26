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
import org.watto.ge.plugin.exporter.Exporter_ZLib;
import org.watto.io.FileManipulator;
import org.watto.io.buffer.ByteBuffer;
import org.watto.task.TaskProgressManager;

/**
**********************************************************************************************

**********************************************************************************************
**/
public class Plugin_PAK_PAK_3 extends ArchivePlugin {

  /**
  **********************************************************************************************

  **********************************************************************************************
  **/
  public Plugin_PAK_PAK_3() {

    super("PAK_PAK_3", "PAK_PAK_3");

    //         read write replace rename
    setProperties(true, false, false, false);

    setGames("Eschalon: Book 1");
    setExtensions("pak"); // MUST BE LOWER CASE
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
      if (fm.readString(4).equals("!PAK")) {
        rating += 50;
      }

      // 2 - Version Major? (1)
      if (fm.readShort() == 1) {
        rating += 5;
      }

      // 2 - Version Minor? (1)
      if (fm.readShort() == 1) {
        rating += 5;
      }

      // 4 - Number of Files
      if (FieldValidator.checkNumFiles(fm.readInt())) {
        rating += 5;
      }

      long arcSize = fm.getLength();

      // 4 - Compressed Directory Length
      if (FieldValidator.checkLength(fm.readInt(), arcSize)) {
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

      // 4 - Header ("!PAK")
      // 2 - Version Major? (1)
      // 2 - Version Minor? (1)
      fm.skip(8);

      // 4 - Number of Files
      int numFiles = fm.readInt();
      FieldValidator.checkNumFiles(numFiles);

      // 4 - Compressed Directory Length
      int dirLength = fm.readInt();
      FieldValidator.checkLength(dirLength, arcSize);

      // 4 - Decompressed Directory Length
      int dirDecompLength = fm.readInt();
      FieldValidator.checkLength(dirDecompLength);

      // X - Compressed Directory Data (ZLib)
      byte[] dirBytes = new byte[dirDecompLength];
      int decompWritePos = 0;
      Exporter_ZLib exporter = Exporter_ZLib.getInstance();
      exporter.open(fm, dirLength, dirDecompLength);

      for (int b = 0; b < dirDecompLength; b++) {
        if (exporter.available()) { // make sure we read the next bit of data, if required
          dirBytes[decompWritePos++] = (byte) exporter.read();
        }
      }

      // open the decompressed data for processing
      fm.close();
      fm = new FileManipulator(new ByteBuffer(dirBytes));

      int fileDataOffset = dirLength + 20;

      Resource[] resources = new Resource[numFiles];
      TaskProgressManager.setMaximum(numFiles);

      // Loop through directory
      for (int i = 0; i < numFiles; i++) {
        // 4 - Compressed File Length
        int length = fm.readInt();
        FieldValidator.checkLength(length, arcSize);

        // 4 - File Offset (relative to the start of the FILE DATA)
        int offset = fm.readInt() + fileDataOffset;
        FieldValidator.checkOffset(offset, arcSize);

        // 4 - Decompressed File Length?
        int decompLength = fm.readInt();
        FieldValidator.checkLength(decompLength);

        // 4 - Compression Type? (2=ZLib)
        fm.skip(4);

        // 256 - Filename (null terminated, filled with nulls)
        String filename = fm.readNullString(256);
        FieldValidator.checkFilename(filename);

        //path,name,offset,length,decompLength,exporter
        resources[i] = new Resource(path, filename, offset, length, decompLength, exporter);

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
