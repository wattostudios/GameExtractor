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
public class Plugin_RES_0TSR_2 extends ArchivePlugin {

  /**
  **********************************************************************************************

  **********************************************************************************************
  **/
  public Plugin_RES_0TSR_2() {

    super("RES_0TSR_2", "RES_0TSR_2");

    //         read write replace rename
    setProperties(true, false, false, false);

    setGames("Excitebots: Trick Racing");
    setExtensions("res"); // MUST BE LOWER CASE
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

      getDirectoryFile(fm.getFile(), "toc");
      rating += 25;

      // Header
      if (fm.readString(4).equals("0TSR")) {
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

      long arcSize = (int) path.length();

      File sourcePath = getDirectoryFile(path, "toc");
      FileManipulator fm = new FileManipulator(sourcePath, false);

      // 8 - Header (0SERCOTE) // Note the zero, not the letter "O"
      // 4 - Version? (3)
      // 4 - null
      // 4 - Header (!IGM)
      // 4 - Version? (3)
      // 4 - Unknown (32)
      // 4 - Unknown (same as in RES file)
      fm.skip(32);

      // 4 - Number Of Files
      int numFiles = fm.readInt();
      FieldValidator.checkNumFiles(numFiles);

      // 4 - Length of RES file [+128]
      // 4 - Length of RES file [+128]
      // 4 - null
      fm.skip(12);

      // 4 - Filename Directory Length
      int dirLength = fm.readInt();
      FieldValidator.checkLength(dirLength, sourcePath.length());

      // skip over the directory
      fm.skip(numFiles * 40);

      // read the filename directory
      byte[] filenameBytes = fm.readBytes(dirLength);

      // Open the filename bytes for reading
      FileManipulator filenameFM = new FileManipulator(new ByteBuffer(filenameBytes));

      // go back to the start of the directory, ready to read the file data
      fm.seek(52);

      Resource[] resources = new Resource[numFiles];
      TaskProgressManager.setMaximum(numFiles);

      // Loop through directory
      for (int i = 0; i < numFiles; i++) {
        // 4 - Filename Offset (Relative to the start of the Filename Directory)
        int filenameOffset = fm.readInt();
        FieldValidator.checkLength(filenameOffset, dirLength);

        // 4 - Type Code (GAMI=*.img, TYAL=*.lyt, PMTS=*.fnt, etc.)
        // 4 - Unknown (3)
        fm.skip(8);

        // 4 - File Length
        long length = fm.readInt();
        FieldValidator.checkLength(length, arcSize);

        // 4 - File Offset
        long offset = fm.readInt() + 128;
        FieldValidator.checkOffset(offset, arcSize);

        // 4 - Hash?
        // 16 - null
        fm.skip(20);

        // X - Filename (null) (read from the filename directory)
        filenameFM.seek(filenameOffset);
        String filename = filenameFM.readNullString();
        FieldValidator.checkFilename(filename);

        //path,name,offset,length,decompLength,exporter
        resources[i] = new Resource(path, filename, offset, length);

        TaskProgressManager.setValue(i);
      }

      // close the filename buffer
      filenameFM.close();

      fm.close();

      return resources;

    }
    catch (Throwable t) {
      logError(t);
      return null;
    }
  }

}
