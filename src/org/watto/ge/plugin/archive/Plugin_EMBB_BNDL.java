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
import org.watto.io.buffer.ByteBuffer;
import org.watto.task.TaskProgressManager;

/**
**********************************************************************************************

**********************************************************************************************
**/
public class Plugin_EMBB_BNDL extends ArchivePlugin {

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public Plugin_EMBB_BNDL() {

    super("EMBB_BNDL", "EMBB_BNDL");

    //         read write replace rename
    setProperties(true, false, false, false);

    setGames("Middle Earth: Shadow Of Mordor");
    setExtensions("embb"); // MUST BE LOWER CASE
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
      if (fm.readString(4).equals("BNDL")) {
        rating += 50;
      }

      // Version (4)
      if (fm.readInt() == 4) {
        rating += 5;
      }

      long arcSize = fm.getLength();

      // Filename Directory Length
      if (FieldValidator.checkLength(fm.readInt(), arcSize)) {
        rating += 5;
      }

      fm.skip(12);

      // Number Of Files
      if (FieldValidator.checkNumFiles(fm.readInt())) {
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

      // 4 - Header (BNDL)
      // 4 - Version (4)
      // 4 - Filename Directory Length
      fm.skip(12);

      // 4 - Filename Directory Length + Scope Directory Length
      int dirLength = fm.readInt();
      FieldValidator.checkLength(dirLength, arcSize);

      // 8 - null
      fm.skip(8);

      // 4 - Number of Files
      int numFiles = fm.readInt();
      FieldValidator.checkNumFiles(numFiles);

      // read in the filename directory
      byte[] filenameBytes = fm.readBytes(dirLength);
      FileManipulator nameFM = new FileManipulator(new ByteBuffer(filenameBytes));
      nameFM.seek(0); // just in case

      Resource[] resources = new Resource[numFiles];
      TaskProgressManager.setMaximum(numFiles);

      long thisOffset = fm.getOffset();
      fm.getBuffer().setBufferSize(8); // quick reads
      fm.seek(thisOffset - 8);
      fm.seek(thisOffset);

      // Loop through directory
      for (int i = 0; i < numFiles; i++) {
        // 4 - Filename Offset (relative to the start of the Filename Directory)
        int filenameOffset = fm.readInt();
        FieldValidator.checkLength(filenameOffset, dirLength);

        nameFM.seek(filenameOffset);
        // X - Filename (null)
        String filename = nameFM.readNullString();
        FieldValidator.checkFilename(filename);

        // 4 - File Length
        int length = fm.readInt();
        FieldValidator.checkLength(length, arcSize);

        // X - File Data
        long offset = fm.getOffset();
        fm.skip(length);

        //path,name,offset,length,decompLength,exporter
        resources[i] = new Resource(path, filename, offset, length);

        TaskProgressManager.setValue(i);
      }

      nameFM.close();

      fm.close();

      return resources;

    }
    catch (Throwable t) {
      logError(t);
      return null;
    }
  }

}
