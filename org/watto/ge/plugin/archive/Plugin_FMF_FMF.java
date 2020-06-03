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
import org.watto.ge.plugin.exporter.Exporter_ZLib_CompressedSizeOnly;
import org.watto.io.FileManipulator;
import org.watto.io.buffer.ByteBuffer;
import org.watto.io.converter.ShortConverter;
import org.watto.task.TaskProgressManager;

/**
**********************************************************************************************

**********************************************************************************************
**/
public class Plugin_FMF_FMF extends ArchivePlugin {

  int realNumFiles = 0;

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public Plugin_FMF_FMF() {

    super("FMF_FMF", "FMF_FMF");

    //         read write replace rename
    setProperties(true, false, false, false);

    setGames("Football Manager 2011");
    setExtensions("fmf"); // MUST BE LOWER CASE
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

      // 1 - Version Major? (2)
      if (fm.readByte() == 2) {
        rating += 5;
      }

      // 1 - Version Minor? (1)
      if (fm.readByte() == 1) {
        rating += 5;
      }

      // Header
      if (fm.readString(3).equals("fmf")) {
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

      // 1 - Version Major? (2)
      // 1 - Version Minor? (1)
      // 3 - Header ("fmf")
      // 4 - Unknown (814)
      fm.skip(9);

      // 4 - Directory Offset [+9]
      int dirOffset = fm.readInt() + 9;
      FieldValidator.checkOffset(dirOffset, arcSize);

      fm.seek(dirOffset);

      // 1 - Version Major? (2)
      // 1 - Version Minor? (1)
      // 3 - Header ("fmf")
      // 4 - Unknown (814)
      fm.skip(9);

      // X - Compressed Directory (ZLib Compression)
      int compLength = (int) (arcSize - fm.getOffset());
      FieldValidator.checkLength(compLength, arcSize);

      int decompLength = compLength * 100; // guess only

      byte[] dirBytes = new byte[decompLength];
      int decompWritePos = 0;
      Exporter_ZLib_CompressedSizeOnly exporter = Exporter_ZLib_CompressedSizeOnly.getInstance();
      exporter.open(fm, compLength, compLength);

      for (int b = 0; b < decompLength; b++) {
        if (exporter.available()) { // make sure we read the next bit of data, if required
          dirBytes[decompWritePos++] = (byte) exporter.read();
        }
        else {
          break;
        }
      }

      // open the decompressed data for processing
      fm.close();
      fm = new FileManipulator(new ByteBuffer(dirBytes));

      int numFiles = Archive.getMaxFiles();

      Resource[] resources = new Resource[numFiles];
      TaskProgressManager.setMaximum(arcSize);

      realNumFiles = 0;

      readDirectory(fm, resources, path, "", arcSize);

      resources = resizeResources(resources, realNumFiles);

      fm.close();

      return resources;

    }
    catch (Throwable t) {
      logError(t);
      return null;
    }
  }

  /**
   **********************************************************************************************
   *
   **********************************************************************************************
   **/
  public void readDirectory(FileManipulator fm, Resource[] resources, File path, String parentDirName, long arcSize) {
    try {

      // 4 - Directory Name Length [*2 for unicode] (not including the null terminator bytes)
      int dirNameLength = fm.readInt();

      String dirName = parentDirName;
      if (dirNameLength != 0) {
        FieldValidator.checkFilenameLength(dirNameLength);

        // X - Directory Name (unicode)
        dirName = parentDirName + fm.readUnicodeString(dirNameLength) + "\\";

        // 2 - null Directory Name Terminator (this field doesn't exist if the directory name length is null - ie for the root directory)
        fm.skip(2);
      }

      // 2 - Number of Files in this Directory
      int numFiles = ShortConverter.unsign(fm.readShort());

      // for each file in this directory
      for (int i = 0; i < numFiles; i++) {
        // 4 - Filename Length [*2 for unicode] (not including the null terminator bytes)
        int filenameLength = fm.readInt();
        FieldValidator.checkFilenameLength(filenameLength);

        // X - Filename (without extension) (unicode)
        String filename = dirName + fm.readUnicodeString(filenameLength);

        // 2 - null Filename Terminator
        fm.skip(2);

        // 4 - File Extension Length [*2 for unicode] (not including the null terminator bytes)
        int fileExtensionLength = fm.readInt();
        FieldValidator.checkFilenameLength(fileExtensionLength);

        // X - File Extension (including the ".") (unicode)
        filename += fm.readUnicodeString(fileExtensionLength);

        // 2 - null File Extension Terminator
        fm.skip(2);

        // 4 - File Offset (relative to the start of the FILE DATA)
        int offset = fm.readInt() + 18; // FILE DATA starts at offset 18
        FieldValidator.checkOffset(offset, arcSize);

        // 4 - File Length
        int length = fm.readInt();
        FieldValidator.checkLength(length, arcSize);

        // 4 - File Length
        fm.skip(4);

        //path,name,offset,length,decompLength,exporter
        resources[realNumFiles] = new Resource(path, filename, offset, length);
        realNumFiles++;

        TaskProgressManager.setValue(offset);
      }

      // 2 - Number of Sub-Directories in this Directory
      int numDirectories = fm.readShort();
      FieldValidator.checkNumFiles(numDirectories);

      for (int i = 0; i < numDirectories; i++) {
        readDirectory(fm, resources, path, dirName, arcSize);
      }

    }
    catch (Throwable t) {
      logError(t);
    }
  }

}
