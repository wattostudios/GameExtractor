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
import org.watto.task.TaskProgressManager;
import org.watto.datatype.Archive;
import org.watto.datatype.Resource;
import org.watto.ge.helper.FieldValidator;
import org.watto.ge.plugin.ArchivePlugin;
import org.watto.ge.plugin.ExporterPlugin;
import org.watto.ge.plugin.exporter.Exporter_LZ4;
import org.watto.io.FileManipulator;
import org.watto.io.converter.ByteConverter;
import org.watto.io.converter.IntConverter;

/**
**********************************************************************************************

**********************************************************************************************
**/
public class Plugin_GLA2 extends ArchivePlugin {

  /**
  **********************************************************************************************

  **********************************************************************************************
  **/
  public Plugin_GLA2() {

    super("GLA2", "Sniper Fury GLA2 Archive");

    //         read write replace rename
    setProperties(true, false, false, false);

    setGames("Sniper Fury");
    setExtensions("gla2"); // MUST BE LOWER CASE
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

      // 4 - Header ((bytes)208 238 168 154)
      if (ByteConverter.unsign(fm.readByte()) == 208 && ByteConverter.unsign(fm.readByte()) == 238 && ByteConverter.unsign(fm.readByte()) == 168 && ByteConverter.unsign(fm.readByte()) == 154) {
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

      ExporterPlugin exporter = Exporter_LZ4.getInstance();

      // RESETTING GLOBAL VARIABLES

      FileManipulator fm = new FileManipulator(path, false);

      long arcSize = fm.getLength();

      // GO TO THE END OF THE ARCHIVE, AND READ BACKWARDS THROUGH THE DIRECTORY
      long endOfArcBlockSize = arcSize - fm.getBuffer().getBufferSize();
      fm.seek(endOfArcBlockSize); // first, go here, to trick it into loading a full buffer of data
      fm.seek(arcSize - 24); // then seek forward to the real spot, which should already be in the buffer
      // this makes at least the first 100ish files fast to read

      int numFiles = Archive.getMaxFiles();
      Resource[] resources = new Resource[numFiles];
      TaskProgressManager.setMaximum(numFiles);

      int realNumFiles = 0;
      while (fm.getOffset() > 0) { // worst case - we should break earlier than this!
        long existingPos = fm.getOffset();

        // 4 - Filename Hash
        fm.skip(4);

        // 4 - File Offset BIG ENDIAN
        int offset = IntConverter.changeFormat(fm.readInt());
        FieldValidator.checkOffset(offset, arcSize);

        // 4 - Decompressed Length BIG ENDIAN
        int decompLength = IntConverter.changeFormat(fm.readInt());
        FieldValidator.checkLength(decompLength);

        // 4 - Compressed Length BIG ENDIAN
        int length = IntConverter.changeFormat(fm.readInt());
        FieldValidator.checkLength(length, arcSize);

        // 4 - null
        // 4 - File Type (0/1/8) BIG ENDIAN

        String filename = Resource.generateFilename(realNumFiles);

        if (length == 0) {
          // NO COMPRESSION
          length = decompLength;

          //path,name,offset,length,decompLength,exporter
          resources[realNumFiles] = new Resource(path, filename, offset, length);
        }
        else {
          // LZ4 COMPRESSION

          //path,name,offset,length,decompLength,exporter
          resources[realNumFiles] = new Resource(path, filename, offset, length, decompLength, exporter);
        }

        realNumFiles++;

        TaskProgressManager.setValue(realNumFiles);

        // if the file offset is 16, we've found the first file, so exit the loop
        if (offset == 16) {
          break;
        }

        fm.seek(existingPos - 24); // go back an entry
      }

      if (realNumFiles == 0) {
        return null; // not a valid archive - try a different plugin
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
