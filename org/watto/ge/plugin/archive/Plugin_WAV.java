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
public class Plugin_WAV extends ArchivePlugin {

  /**
  **********************************************************************************************

  **********************************************************************************************
  **/
  public Plugin_WAV() {

    super("WAV", "WAV");

    //         read write replace rename
    setProperties(true, false, false, false);

    setGames("Mini Ninjas");
    setExtensions("wav"); // MUST BE LOWER CASE
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

      getDirectoryFile(fm.getFile(), "WHD"); // NOTE: CAPITALS!
      rating += 25;

      // 4 - null
      if (fm.readInt() == 0) {
        rating += 5;
      }

      long arcSize = fm.getLength();

      // Archive Size
      if (FieldValidator.checkEquals(fm.readInt(), arcSize)) {
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

      //ExporterPlugin exporter = Exporter_Custom_WAV_RawAudio.getInstance();

      // RESETTING GLOBAL VARIABLES

      long arcSize = path.length();

      File sourcePath = getDirectoryFile(path, "WHD"); // NOTE: CAPITALS!
      FileManipulator fm = new FileManipulator(sourcePath, false);

      // 4 - Length of WHD File [+16]
      // 4 - Length of WHD File
      // 4 - Unknown (3)
      // 4 - Unknown (4)
      fm.skip(16);

      int numFiles = Archive.getMaxFiles();

      int dirSize = (int) (fm.getLength() - 32);

      Resource[] resources = new Resource[numFiles];
      TaskProgressManager.setMaximum(arcSize);

      // Loop through directory
      int realNumFiles = 0;
      while (fm.getOffset() < dirSize) {
        // X - Filename
        // 1 - null Filename Terminator
        String filename = fm.readNullString();
        if (filename.equals("")) {
          filename = Resource.generateFilename(realNumFiles) + ".wav";
        }
        //FieldValidator.checkFilename(filename);
        //System.out.println(filename);

        // 0-3 - null Padding to a multiple of 4 bytes
        fm.skip(calculatePadding(fm.getOffset(), 4));

        // 4 - null
        // 8 - null (OPTIONAL FIELD)
        while (fm.getOffset() < dirSize && fm.readByte() == 0) {
          // loop until the end of the nulls
        }
        // 4 - Unknown
        fm.skip(calculatePadding(fm.getOffset(), 4)); // finish reading the remaining bytes of this field, usually 3 bytes

        // 4 - Unknown (17)
        // 4 - Unknown
        // 4 - Unknown (4)
        // 4 - Unknown
        fm.skip(16);

        // 4 - File Length (including Padding)
        long length = fm.readInt();
        FieldValidator.checkLength(length, arcSize);

        // 4 - Unknown (1)
        fm.skip(4);

        // 4 - File Offset
        long offset = fm.readInt();
        if (offset > arcSize) {
          // the file is actually in an external file or something - not sure - ignore
          fm.skip(28);
          continue;
        }
        FieldValidator.checkOffset(offset, arcSize);

        // 4 - Decompressed Length?
        long decompLength = fm.readInt();
        FieldValidator.checkLength(decompLength);

        // 4 - Padding Multiple (1024)
        // 4 - Unknown (2041)
        // 4 - null
        // 8 - Unknown (all (byte)205)
        // 4 - null
        fm.skip(24);

        //path,name,offset,length,decompLength,exporter
        resources[realNumFiles] = new Resource(path, filename, offset, length, decompLength);
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
