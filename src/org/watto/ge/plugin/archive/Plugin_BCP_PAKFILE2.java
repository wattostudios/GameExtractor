/*
 * Application:  Game Extractor
 * Author:       wattostudios
 * Website:      http://www.watto.org
 * Copyright:    Copyright (c) 2002-2026 wattostudios
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
import org.watto.ge.plugin.exporter.Exporter_BZIP2;
import org.watto.io.FileManipulator;
import org.watto.io.converter.ByteConverter;
import org.watto.task.TaskProgressManager;

/**
**********************************************************************************************

**********************************************************************************************
**/
public class Plugin_BCP_PAKFILE2 extends ArchivePlugin {

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public Plugin_BCP_PAKFILE2() {

    super("BCP_PAKFILE2", "BCP_PAKFILE2");

    //         read write replace rename
    setProperties(true, false, false, false);

    setGames("Warrior Kings: Battles");
    setExtensions("bcp");
    setPlatforms("PC");

    // MUST BE LOWER CASE !!!
    //setFileTypes(new FileType("txt", "Text Document", FileType.TYPE_DOCUMENT),
    //             new FileType("bmp", "Bitmap Image", FileType.TYPE_IMAGE)
    //             );

    setTextPreviewExtensions("anim", "cam", "cpp", "dat", "font", "gsf", "h", "lvl", "mesh", "psh", "psystem", "rc", "vsh"); // LOWER CASE

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

      // Header
      if (fm.readString(44).equals("PAK File 2.01 (c) Black Cactus Games Limited")) {
        rating += 50;
      }

      fm.skip(4);

      long arcSize = fm.getLength();

      // Directory Offset
      if (FieldValidator.checkOffset(fm.readInt(), arcSize)) {
        rating += 5;
      }

      // Directory Length
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
  
  **********************************************************************************************
  **/
  @Override
  public Resource[] read(File path) {
    try {

      addFileTypes();

      ExporterPlugin exporterBZip2 = Exporter_BZIP2.getInstance();

      // RESETTING THE GLOBAL VARIABLES

      FileManipulator fm = new FileManipulator(path, false);
      long arcSize = fm.getLength();

      // 44 - Header (PAK File 2.01 (c) Black Cactus Games Limited)
      // 4 - Unknown
      fm.skip(48);

      // 4 - Directory Offset
      long dirOffset = fm.readInt();
      FieldValidator.checkOffset(dirOffset, arcSize);

      // 4 - Directory Length

      fm.seek(dirOffset);

      // 4 - Number Of Files
      int numFiles = fm.readInt();
      FieldValidator.checkNumFiles(numFiles);

      Resource[] resources = new Resource[numFiles];
      TaskProgressManager.setMaximum(numFiles);

      // Loop through directory
      for (int i = 0; i < numFiles; i++) {
        // 4 - File Offset
        int offset = fm.readInt();
        if (offset == -1) {
          offset = 0;
        }
        FieldValidator.checkOffset(offset, arcSize);

        // 4 - Offset To Next File
        int length = fm.readInt() - offset;
        if (length == -1) {
          length = 0;
        }
        FieldValidator.checkLength(length, arcSize);

        // 4 - Decompressed File Length
        int decompLength = fm.readInt();
        FieldValidator.checkLength(decompLength);

        // 4 - Unknown (Hash?)
        fm.skip(4);

        // 4 - Compression Type (1=uncompressed, 3=BZip2)
        int compressionType = fm.readInt();

        String filename = Resource.generateFilename(i);

        if (compressionType == 1) {
          // uncompressed

          //path,id,name,offset,length,decompLength,exporter
          resources[i] = new Resource(path, filename, offset, length, decompLength);
        }
        else if (compressionType == 2) {
          // Empty File

          //path,id,name,offset,length,decompLength,exporter
          resources[i] = new Resource(path, filename, offset, length, decompLength);
        }
        else if (compressionType == 3) {
          // BZip2

          //path,id,name,offset,length,decompLength,exporter
          resources[i] = new Resource(path, filename, offset, length, decompLength, exporterBZip2);
        }
        else if (compressionType == 4) {
          // Unknown compression

          //path,id,name,offset,length,decompLength,exporter
          resources[i] = new Resource(path, filename, offset, length, decompLength);
        }
        else {
          ErrorLogger.log("[BCP_PAKFILE2] Unknown compression type: " + compressionType + " for file number " + (i + 1));

          //path,id,name,offset,length,decompLength,exporter
          resources[i] = new Resource(path, filename, offset, length, decompLength);
        }

        TaskProgressManager.setValue(i);
      }

      // Loop through Filename directory

      // 4 - num files in the root directory
      int numInRoot = fm.readInt();
      for (int i = 0; i < numInRoot; i++) {
        // 4 - File ID (incremental, starting from 0)
        int fileID = fm.readInt();

        // 8 - Unknown
        fm.skip(8);

        // 1 - Filename Length [*2 for unicode]
        int filenameLength = ByteConverter.unsign(fm.readByte());

        // X - Filename (unicode text - 2-bytes per letter)
        String filename = fm.readString(filenameLength * 2);
        filename = new String(filename.getBytes(), "UTF-16LE");

        Resource resource = resources[fileID];
        resource.setName(filename);
        resource.setOriginalName(filename);
      }

      while (fm.getOffset() < fm.getLength()) {
        readDirectory(fm, resources, "");
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
  
  **********************************************************************************************
  **/
  public void readDirectory(FileManipulator fm, Resource[] resources, String dirName) throws Exception {
    // 4 - Number Of Sub-Directories In This Directory? /*(0 = go back a directory?)*/
    int numSubDirs = fm.readInt();

    // 1 - Directory name Length [*2 for unicode]
    int dirNameLength = ByteConverter.unsign(fm.readByte());

    // X - Directory name (unicode text - 2-bytes per letter)
    String thisDirName = fm.readString(dirNameLength * 2);
    thisDirName = new String(thisDirName.getBytes(), "UTF-16LE");

    // 4 - Number Of Files In Directory (not including sub-directories)
    int numFilesInDir = fm.readInt();

    // read the files in the directory
    for (int i = 0; i < numFilesInDir; i++) {
      // 4 - File ID (incremental, starting from 0)
      int fileID = fm.readInt();

      // 8 - Unknown
      fm.skip(8);

      // 1 - Filename Length [*2 for unicode]
      int filenameLength = ByteConverter.unsign(fm.readByte());

      // X - Filename (unicode text - 2-bytes per letter)
      String filename = fm.readString(filenameLength * 2);
      filename = new String(filename.getBytes(), "UTF-16LE");

      String newName = dirName + thisDirName + "\\" + filename;

      Resource resource = resources[fileID];
      resource.setName(newName);
      resource.setOriginalName(newName);
    }

    // read the sub-directories
    for (int i = 0; i < numSubDirs; i++) {
      readDirectory(fm, resources, dirName + thisDirName + "\\");
    }

  }

}
