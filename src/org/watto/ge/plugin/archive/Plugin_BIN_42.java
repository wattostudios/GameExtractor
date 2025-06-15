/*
 * Application:  Game Extractor
 * Author:       wattostudios
 * Website:      http://www.watto.org
 * Copyright:    Copyright (c) 2002-2025 wattostudios
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
import org.watto.ge.plugin.exporter.Exporter_ZLib;
import org.watto.io.FileManipulator;
import org.watto.io.buffer.ByteBuffer;
import org.watto.task.TaskProgressManager;

/**
**********************************************************************************************

**********************************************************************************************
**/
public class Plugin_BIN_42 extends ArchivePlugin {

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public Plugin_BIN_42() {

    super("BIN_42", "BIN_42");

    //         read write replace rename
    setProperties(true, false, false, false);

    setGames("Asteroids: Outpost");
    setExtensions("bin"); // MUST BE LOWER CASE
    setPlatforms("PC");

    // MUST BE LOWER CASE !!!
    //setFileTypes(new FileType("txt", "Text Document", FileType.TYPE_DOCUMENT),
    //             new FileType("bmp", "Bitmap Image", FileType.TYPE_IMAGE)
    //             );

    setTextPreviewExtensions("csv", "dat", "ibl", "info", "lang", "mat", "phx", "pt2", "repx", "tst"); // LOWER CASE

    //setCanScanForFileTypes(true);

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

      // check for a "_00" file
      String filePath = fm.getFile().getAbsolutePath();
      int underscorePos = filePath.lastIndexOf('_');
      if (underscorePos <= 0) {
        return 0;
      }
      filePath = filePath.substring(0, underscorePos) + "_00.bin";
      File file00 = new File(filePath);
      if (file00.exists()) {
        rating += 25;
      }
      else {
        return 0;
      }

      // Header
      String header = fm.readString(4);
      if (header.equals("arfl") || header.equals("arfd")) {
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

      // find the "_00" file
      String filePath = path.getAbsolutePath();
      int underscorePos = filePath.lastIndexOf('_');
      if (underscorePos <= 0) {
        return null;
      }
      filePath = filePath.substring(0, underscorePos) + "_00.bin";
      File dirFile = new File(filePath);
      if (!dirFile.exists()) {
        return null;
      }

      FileManipulator fm = new FileManipulator(dirFile, false);

      // find the archive files
      File[] archiveFiles = new File[5];
      long[] archiveLengths = new long[5];
      int numArchives = 0;

      for (int i = 0; i < 5; i++) {
        File archiveFile = new File(filePath.substring(0, underscorePos) + "_0" + (i + 1) + ".bin");
        if (archiveFile.exists()) {
          archiveFiles[i] = archiveFile;
          archiveLengths[i] = archiveFile.length();
          numArchives++;
        }
        else {
          break;
        }
      }

      long arcSize = fm.getLength();

      // 4 - Header (arfl)
      // 4 - Version? (1)
      // 4 - Unknown
      fm.skip(12);

      // 4 - Number of Files
      int numFiles = fm.readInt();
      FieldValidator.checkNumFiles(numFiles);

      // 4 - Compressed Directory Length
      int compDirLength = fm.readInt();
      FieldValidator.checkLength(compDirLength, arcSize);

      // 4 - Unknown
      fm.skip(4);

      // X - Compressed Directory (ZLib Compression)
      int decompDirLength = (numFiles * 296);

      byte[] dirBytes = new byte[decompDirLength];
      int decompWritePos = 0;
      Exporter_ZLib exporter = Exporter_ZLib.getInstance();
      exporter.open(fm, compDirLength, decompDirLength);

      for (int b = 0; b < decompDirLength; b++) {
        if (exporter.available()) { // make sure we read the next bit of data, if required
          dirBytes[decompWritePos++] = (byte) exporter.read();
        }
      }

      // open the decompressed data for processing
      fm.close();
      fm = new FileManipulator(new ByteBuffer(dirBytes));

      Resource[] resources = new Resource[numFiles];
      TaskProgressManager.setMaximum(numFiles);

      // Loop through directory
      for (int i = 0; i < numFiles; i++) {
        // 256 - Filename (null terminated, filled with nulls)
        String filename = fm.readNullString(256);
        FieldValidator.checkFilename(filename);

        // 4 - Unknown (0)
        fm.skip(4);

        // 1 - Compression Type (1=Uncompressed, 2=ZLib)
        int compression = fm.readByte();

        // 1 - Archive Number? [+1]
        int archiveNumber = fm.readByte();
        FieldValidator.checkRange(archiveNumber, 0, numArchives);

        File archiveFile = archiveFiles[archiveNumber];
        arcSize = archiveLengths[archiveNumber];

        // 4 - File Offset
        int offset = fm.readInt();
        FieldValidator.checkOffset(offset, arcSize);

        // 4 - Decompressed File Length
        int decompLength = fm.readInt();
        FieldValidator.checkLength(decompLength);

        // 4 - Compressed File Length
        int length = fm.readInt();
        FieldValidator.checkLength(length, arcSize);

        // 4 - Hash?
        // 4 - Unknown (0/1)
        // 14 - null
        fm.skip(22);

        // skip the header around the file data
        offset += 4;
        length -= 4;

        if (compression == 2) {
          // ZLib
          //path,name,offset,length,decompLength,exporter
          Resource resource = new Resource(archiveFile, filename, offset, length, decompLength, exporter);
          resource.forceNotAdded(true);
          resources[i] = resource;
        }
        else if (compression == 1) {
          // Raw
          //path,name,offset,length,decompLength,exporter
          Resource resource = new Resource(archiveFile, filename, offset, length);
          resource.forceNotAdded(true);
          resources[i] = resource;
        }
        else {
          ErrorLogger.log("[BIN_42] Unknown compression type: " + compression);
          //path,name,offset,length,decompLength,exporter
          Resource resource = new Resource(archiveFile, filename, offset, length);
          resource.forceNotAdded(true);
          resources[i] = resource;
        }

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

  /**
  **********************************************************************************************
  If an archive doesn't have filenames stored in it, the scanner can come here to try to work out
  what kind of file a Resource is. This method allows the plugin to provide additional plugin-specific
  extensions, which will be tried before any standard extensions.
  @return null if no extension can be determined, or the extension if one can be found
  **********************************************************************************************
  **/
  @Override
  public String guessFileExtension(Resource resource, byte[] headerBytes, int headerInt1, int headerInt2, int headerInt3, short headerShort1, short headerShort2, short headerShort3, short headerShort4, short headerShort5, short headerShort6) {

    /*
    if (headerInt1 == 2037149520) {
      return "js";
    }
    */

    return null;
  }

}
