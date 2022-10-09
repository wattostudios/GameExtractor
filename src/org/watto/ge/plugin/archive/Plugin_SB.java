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
import org.watto.ErrorLogger;
import org.watto.datatype.Archive;
import org.watto.datatype.Resource;
import org.watto.ge.helper.FieldValidator;
import org.watto.ge.plugin.ArchivePlugin;
import org.watto.io.FileManipulator;
import org.watto.io.FilenameSplitter;
import org.watto.io.converter.ByteConverter;
import org.watto.task.TaskProgressManager;

/**
**********************************************************************************************

**********************************************************************************************
**/
public class Plugin_SB extends ArchivePlugin {

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public Plugin_SB() {

    super("SB", "SB");

    //         read write replace rename
    setProperties(true, false, false, false);

    setGames("Dragon Age: Inquisition");
    setExtensions("sb"); // MUST BE LOWER CASE
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

      if (FilenameSplitter.getExtension(fm.getFile()).equalsIgnoreCase("toc")) {
        // double-clicked the TOC file, don't match here (causes issues for other plugins)
        rating = 0;
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

      long sourceSize = fm.getLength();

      // 8 - Unknown
      // 548 - Hash? (nulls to fill)
      // 4 - Unknown
      // 1 - Property ID (1)
      // 7 - Bundle Header ("bundles")
      // 1 - null Bundle Header Terminator
      // 2 - Unknown
      // 1 - Number of Bundles (1)
      fm.skip(569);
      while (ByteConverter.unsign(fm.readByte()) != 130) {
        // repeat
      }
      fm.seek(fm.getOffset() - 1);

      int numFiles = Archive.getMaxFiles();

      Resource[] resources = new Resource[numFiles];
      TaskProgressManager.setMaximum(sourceSize);

      // Loop through directory
      int realNumFiles = 0;
      while (fm.getOffset() < sourceSize) {
        // 2 - Unknown
        int firstByte = ByteConverter.unsign(fm.readByte());
        if (firstByte == 1 || firstByte == 0) {
          // Found the Archive Footer - break
          break;
        }
        fm.skip(1);

        String filename = null;
        long offset = -1;
        int length = -1;

        // 1 - Property ID
        int propertyByte = fm.readByte();
        if (propertyByte == 1) {
          propertyByte = fm.readByte(); // actually the next byte
        }
        while (propertyByte != 0) {
          if (propertyByte == 7) {
            // 2 - ID Header ("id")
            // 1 - null ID Header Terminator
            // 1 - Filename Length (including Null Terminator)
            fm.skip(4);

            // X - Filename
            // 1 - null Filename Terminator
            filename = fm.readNullString();
            FieldValidator.checkFilename(filename);
          }
          else if (propertyByte == 9) {
            // 6 - Offset Header ("offset")
            // 1 - null Offset Header Terminator
            fm.skip(7);

            // 8 - File Offset
            offset = fm.readLong();
            FieldValidator.checkOffset(offset, arcSize);
          }
          else if (propertyByte == 8) {
            // 4 - Size Header ("size")
            // 1 - null Offset Header Terminator
            fm.skip(5);

            // 4 - File Length
            length = fm.readInt();
            FieldValidator.checkLength(length, arcSize);
          }
          else {
            ErrorLogger.log("[SB]: Unknown property ID: " + propertyByte + " at offset " + (fm.getOffset() - 1));
            break;
          }
          propertyByte = fm.readByte();
        }

        System.out.println(fm.getOffset() + "\t" + filename);
        resources[realNumFiles] = new Resource(path, filename, offset, length);
        realNumFiles++;

        TaskProgressManager.setValue(fm.getOffset());
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
