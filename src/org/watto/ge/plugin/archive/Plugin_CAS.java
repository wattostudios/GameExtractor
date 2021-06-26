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
import org.watto.datatype.Resource;
import org.watto.ge.helper.FieldValidator;
import org.watto.ge.plugin.ArchivePlugin;
import org.watto.ge.plugin.ExporterPlugin;
import org.watto.ge.plugin.exporter.Exporter_ZLib_CompressedSizeOnly;
import org.watto.io.FileManipulator;
import org.watto.io.converter.ByteConverter;
import org.watto.task.TaskProgressManager;

/**
**********************************************************************************************

**********************************************************************************************
**/
public class Plugin_CAS extends ArchivePlugin {

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public Plugin_CAS() {

    super("CAS", "CAS");

    //         read write replace rename
    setProperties(true, false, false, false);

    setGames("Dragon Age: Inquisition");
    setExtensions("cas"); // MUST BE LOWER CASE
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
      byte[] headerBytes = fm.readBytes(4);
      if (ByteConverter.unsign(headerBytes[0]) == 250 && ByteConverter.unsign(headerBytes[1]) == 206 && ByteConverter.unsign(headerBytes[2]) == 15 && ByteConverter.unsign(headerBytes[3]) == 240) {
        rating += 50;
      }

      String filePath = fm.getFile().getAbsolutePath();
      int underscorePos = filePath.lastIndexOf('_');
      if (underscorePos > 0) {
        filePath = filePath.substring(0, underscorePos) + ".cat";
        if (new File(filePath).exists()) {
          rating += 25;
        }
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

      // GET THE DIRECTORY FILE
      File sourcePath = null;

      String filePath = path.getAbsolutePath();
      String basePath = "";
      int underscorePos = filePath.lastIndexOf('_');
      if (underscorePos > 0) {
        basePath = filePath.substring(0, underscorePos);
        filePath = basePath + ".cat";
        basePath += "_";
        sourcePath = new File(filePath);
      }

      if (sourcePath == null || !sourcePath.exists()) {
        return null;
      }

      long arcSize = 0;

      FileManipulator fm = new FileManipulator(sourcePath, false);

      // 16 - Header (NyanNyanNyanNyan)
      fm.skip(16);

      int numFiles = (int) ((fm.getLength() - 16) / 32);
      FieldValidator.checkNumFiles(numFiles / 10);

      Resource[] resources = new Resource[numFiles];
      TaskProgressManager.setMaximum(numFiles);

      // Loop through directory
      int previousCasNumber = -1;
      for (int i = 0; i < numFiles; i++) {
        // 20 - Hash?
        fm.skip(20);

        // 4 - File Offset (Relative to the start of the referenced ##.cas file. Points to the "256" field in the CAS file)
        int offset = fm.readInt();

        // 4 - File Length
        int length = fm.readInt();

        // 4 - CAS File Number (1/2/3...)
        int casNumber = fm.readInt();
        FieldValidator.checkPositive(casNumber);

        if (casNumber != previousCasNumber) {
          String zeroPadding = "";
          if (casNumber < 10) {
            zeroPadding = "0";
          }

          // Find the kit file - ensure it exists
          path = new File(basePath + zeroPadding + casNumber + ".cas");
          if (!path.exists()) {
            ErrorLogger.log("[CAS]: Missing CAS file number " + casNumber);
            return null;
          }

          // get the length of the cas file, for field validation
          arcSize = path.length();

          previousCasNumber = casNumber; // so we can re-use this for the next file
        }

        FieldValidator.checkOffset(offset, arcSize);
        FieldValidator.checkLength(length, arcSize);

        String filename = Resource.generateFilename(i);

        // Each file has a 8-byte header and then the ZLib-compressed data
        offset += 8;
        length -= 8;

        //path,name,offset,length,decompLength,exporter
        resources[i] = new Resource(path, filename, offset, length, length, exporter);
        resources[i].forceNotAdded(true);

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
