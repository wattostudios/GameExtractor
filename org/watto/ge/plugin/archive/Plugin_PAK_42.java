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
import org.watto.component.WSPluginManager;
import org.watto.datatype.Archive;
import org.watto.datatype.Resource;
import org.watto.ge.helper.FieldValidator;
import org.watto.ge.plugin.ArchivePlugin;
import org.watto.ge.plugin.ExporterPlugin;
import org.watto.ge.plugin.ViewerPlugin;
import org.watto.ge.plugin.exporter.Exporter_ZLib;
import org.watto.io.FileManipulator;
import org.watto.io.buffer.ByteBuffer;
import org.watto.task.TaskProgressManager;

/**
**********************************************************************************************

**********************************************************************************************
**/
public class Plugin_PAK_42 extends ArchivePlugin {

  int realNumFiles = 0;
  int fileDataOffset = 0;

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public Plugin_PAK_42() {

    super("PAK_42", "PAK_42");

    //         read write replace rename
    setProperties(true, false, false, false);

    setGames("Gray Matter");
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

      long arcSize = fm.getLength();

      // 4 - Decompressed Directory Length
      if (FieldValidator.checkLength(fm.readInt())) {
        rating += 5;
      }

      // 4 - Compressed Directory Length
      if (FieldValidator.checkLength(fm.readInt(), arcSize)) {
        rating += 5;
      }

      // X - Compressed Directory (ZLib Compression)
      if (fm.readString(1).equals("x")) {
        rating += 25;
      }

      return rating;

    }
    catch (Throwable t) {
      return 0;
    }
  }

  /**
   **********************************************************************************************
   Provide hints to the previewer so that certain document types are displayed appropriately
   **********************************************************************************************
   **/
  @Override
  public ViewerPlugin previewHint(Resource resource) {
    String extension = resource.getExtension();
    if (extension.equalsIgnoreCase("mat") || extension.equalsIgnoreCase("fx") || extension.equalsIgnoreCase("inc") || extension.equalsIgnoreCase("lua")) {
      return (ViewerPlugin) WSPluginManager.getPlugin("Viewer", "TXT");
    }
    return null;
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

      // RESETTING GLOBAL VARIABLES

      FileManipulator fm = new FileManipulator(path, false);

      long arcSize = fm.getLength();

      // 4 - Decompressed Directory Length
      int dirDecompLength = fm.readInt();
      FieldValidator.checkLength(dirDecompLength);

      // 4 - Compressed Directory Length
      int dirLength = fm.readInt();
      FieldValidator.checkLength(dirLength, arcSize);

      fileDataOffset = dirLength;

      // X - Compressed Directory (ZLib Compression)
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

      int numFiles = Archive.getMaxFiles();

      Resource[] resources = new Resource[numFiles];
      TaskProgressManager.setMaximum(arcSize);

      realNumFiles = 0;

      // 4 - Number of Sub-Directories in the Root
      int numDirs = fm.readInt();
      FieldValidator.checkNumFiles(numDirs + 1); // allow zero directories

      for (int i = 0; i < numDirs; i++) {
        readDirectory(fm, resources, path, "", arcSize);
      }

      // 4 - Number of Files in the root
      int numFilesInRoot = fm.readInt();
      FieldValidator.checkNumFiles(numFilesInRoot + 1); // allow zero files

      // for each file in this directory
      for (int i = 0; i < numFilesInRoot; i++) {
        // 4 - Filename Length
        int filenameLength = fm.readInt();
        FieldValidator.checkFilenameLength(filenameLength);

        // X - Filename
        String filename = fm.readString(filenameLength);

        // 4 - File Offset (relative to the start of the FILE DATA)
        int offset = fm.readInt() + fileDataOffset;
        FieldValidator.checkOffset(offset, arcSize);

        // 4 - Compressed File Length
        int length = fm.readInt();
        FieldValidator.checkLength(length, arcSize);

        // 4 - Decompressed File Length
        int decompLength = fm.readInt();
        FieldValidator.checkLength(decompLength);

        //path,name,offset,length,decompLength,exporter
        resources[realNumFiles] = new Resource(path, filename, offset, length, decompLength, exporter);
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

  /**
   **********************************************************************************************
   *
   **********************************************************************************************
   **/
  public void readDirectory(FileManipulator fm, Resource[] resources, File path, String parentDirName, long arcSize) {
    try {

      ExporterPlugin exporter = Exporter_ZLib.getInstance();

      // 4 - Directory Name Length
      int dirNameLength = fm.readInt();
      FieldValidator.checkFilenameLength(dirNameLength);

      // X - Directory Name
      String dirName = parentDirName + fm.readString(dirNameLength) + "\\";

      // 4 - Number of Sub-Directories in this Directory
      int numDirs = fm.readInt();
      FieldValidator.checkNumFiles(numDirs + 1); // allow zero subdirectories

      // for each sub-directory in this directory
      for (int i = 0; i < numDirs; i++) {
        // repeat from "//for each sub-directory"
        readDirectory(fm, resources, path, dirName, arcSize);
      }

      // 4 - Number of Files in this Directory
      int numFiles = fm.readInt();
      FieldValidator.checkNumFiles(numFiles);

      // for each file in this directory
      for (int i = 0; i < numFiles; i++) {
        // 4 - Filename Length
        int filenameLength = fm.readInt();
        FieldValidator.checkFilenameLength(filenameLength);

        // X - Filename
        String filename = dirName + fm.readString(filenameLength);

        // 4 - File Offset (relative to the start of the FILE DATA)
        int offset = fm.readInt() + fileDataOffset;
        FieldValidator.checkOffset(offset, arcSize);

        // 4 - Compressed File Length
        int length = fm.readInt();
        FieldValidator.checkLength(length, arcSize);

        // 4 - Decompressed File Length
        int decompLength = fm.readInt();
        FieldValidator.checkLength(decompLength);

        //path,name,offset,length,decompLength,exporter
        resources[realNumFiles] = new Resource(path, filename, offset, length, decompLength, exporter);
        realNumFiles++;

        TaskProgressManager.setValue(offset);
      }

    }
    catch (Throwable t) {
      logError(t);
    }
  }

}
