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
import org.watto.Language;
import org.watto.Settings;
import org.watto.datatype.Resource;
import org.watto.ge.helper.FieldValidator;
import org.watto.ge.plugin.ArchivePlugin;
import org.watto.io.FileManipulator;
import org.watto.task.TaskProgressManager;

/**
**********************************************************************************************

**********************************************************************************************
**/
public class Plugin_PCK_AKPK extends ArchivePlugin {

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public Plugin_PCK_AKPK() {

    super("PCK_AKPK", "PCK_AKPK");

    //         read write replace rename
    setProperties(true, false, true, false);

    setGames("APB Reloaded",
        "Bioshock Infinite",
        "Borderlands: The Pre-Sequel",
        "Borderlands 2",
        "Darksiders 2",
        "Middle Earth: Shadow Of Mordor",
        "Resident Evil 2");
    setExtensions("pck"); // MUST BE LOWER CASE
    setPlatforms("PC");

    // MUST BE LOWER CASE !!!
    //setFileTypes(new FileType("txt", "Text Document", FileType.TYPE_DOCUMENT),
    //             new FileType("bmp", "Bitmap Image", FileType.TYPE_IMAGE)
    //             );

    setCanScanForFileTypes(true);

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
      if (fm.readString(4).equals("AKPK")) {
        rating += 50;
      }

      long arcSize = fm.getLength();

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

      // 4 - Header (AKPK)
      // 4 - Directory Length
      // 4 - Unknown (1)
      // 4 - Size of Directory Header? [+??]
      // 4 - Unknown (4)
      // 4 - Directory Length
      // 4 - Unknown (4)
      fm.skip(28);

      // 4 - Number of Folders
      int directoryCount = fm.readInt();
      FieldValidator.checkNumFiles(directoryCount);

      String[] dirNames = new String[directoryCount];
      int[] parentDirs = new int[directoryCount];
      for (int i = 0; i < directoryCount; i++) {
        // 4 - Folder Name Offset (relative to the start of the NumberOfFolders field)
        fm.skip(4);

        // 4 - Parent Folder ID? (0 = no parent folder)
        int parentDir = fm.readInt();
        FieldValidator.checkRange(parentDir, 0, directoryCount);

        parentDirs[i] = parentDir;
      }

      for (int i = 0; i < directoryCount; i++) {
        // X - Folder Name (unicode)
        // 2 - null Unicode Folder Name Terminator
        String directoryName = fm.readNullUnicodeString();
        FieldValidator.checkFilename(directoryName);

        dirNames[i] = directoryName;
      }

      // set the parent directory names
      for (int i = 0; i < directoryCount; i++) {
        int parentDir = parentDirs[i];
        if (parentDir != 0) {
          dirNames[i] = dirNames[parentDir] + "\\" + dirNames[i];
        }
      }

      // 4 - null
      fm.skip(4);

      // 4 - Number of Files
      int numFiles = fm.readInt();
      FieldValidator.checkNumFiles(numFiles);

      Resource[] resources = new Resource[numFiles];
      TaskProgressManager.setMaximum(numFiles);

      // Loop through directory
      for (int i = 0; i < numFiles; i++) {
        // 4 - Unknown
        // 4 - Unknown (1)
        fm.skip(8);

        // 4 - File Length
        int length = fm.readInt();
        FieldValidator.checkLength(length, arcSize);

        // 4 - File Offset
        int offset = fm.readInt();
        FieldValidator.checkOffset(offset, arcSize);

        // 4 - Folder ID?
        int directoryID = fm.readInt();
        FieldValidator.checkRange(directoryID, 0, directoryCount);

        String dirName = dirNames[directoryID];
        String filename = dirName + "\\" + Resource.generateFilename(i);

        //path,name,offset,length,decompLength,exporter
        resources[i] = new Resource(path, filename, offset, length);

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
   * Writes an [archive] File with the contents of the Resources. The archive is written using
   * data from the initial archive - it isn't written from scratch.
   **********************************************************************************************
   **/
  @Override
  public void replace(Resource[] resources, File path) {
    try {

      FileManipulator fm = new FileManipulator(path, true);
      FileManipulator src = new FileManipulator(new File(Settings.getString("CurrentArchive")), false);

      int numFiles = resources.length;
      TaskProgressManager.setMaximum(numFiles);

      // Calculations
      /*
      TaskProgressManager.setMessage(Language.get("Progress_PerformingCalculations"));
      
      long archiveSize = 16;
      long directorySize = 0;
      long filesSize = 0;
      for (int i = 0; i < numFiles; i++) {
        filesSize += resources[i].getDecompressedLength();
        directorySize += 8 + resources[i].getNameLength() + 1;
      }
      archiveSize += filesSize + directorySize;
      */

      // Write Header Data

      // 4 - Header (AKPK)
      fm.writeBytes(src.readBytes(4));

      // 4 - Directory Length
      int dirLength = src.readInt();
      fm.writeInt(dirLength);

      // 4 - Unknown (1)
      // 4 - Size of Directory Header? [+??]
      // 4 - Unknown (4)
      // 4 - Directory Length
      // 4 - Unknown (4)
      fm.writeBytes(src.readBytes(20));

      // 4 - Number of Folders
      int numFolders = src.readInt();
      FieldValidator.checkNumFiles(numFolders);
      fm.writeInt(numFolders);

      // for each folder
      //   4 - Folder Name Offset (relative to the start of the NumberOfFolders field)
      //   4 - Parent Folder ID? (0 = no parent folder)
      fm.writeBytes(src.readBytes(numFolders * 8));

      // for each folder
      for (int i = 0; i < numFolders; i++) {
        // X - Folder Name (unicode)
        // 2 - null Unicode Folder Name Terminator
        String directoryName = src.readNullUnicodeString();
        fm.writeUnicodeString(directoryName);
        fm.writeShort(0);
      }

      // 4 - null
      // 4 - Number of Files
      fm.writeBytes(src.readBytes(8));

      // Write Directory
      TaskProgressManager.setMessage(Language.get("Progress_WritingDirectory"));
      long offset = dirLength + 8;
      int srcFileDataLength = 0;
      for (int i = 0; i < numFiles; i++) {
        Resource fd = resources[i];
        long length = fd.getDecompressedLength();

        // 4 - Unknown
        // 4 - Unknown (1)
        fm.writeBytes(src.readBytes(8));

        // 4 - File Length
        fm.writeInt(length);
        srcFileDataLength += src.readInt();

        // 4 - File Offset
        fm.writeInt(offset);
        src.skip(4);

        // 4 - Folder ID?
        fm.writeBytes(src.readBytes(4));

        offset += length;
      }

      // 4 - null
      fm.writeBytes(src.readBytes(4));

      // Write Files
      TaskProgressManager.setMessage(Language.get("Progress_WritingFiles"));
      write(resources, fm);

      src.skip(srcFileDataLength);

      // X - Unknown ARCHIVE FOOTER
      int remainingLength = (int) (src.getLength() - src.getOffset());
      for (int f = 0; f < remainingLength; f++) {
        fm.writeByte(src.readByte());
      }

      //ExporterPlugin exporter = Exporter_ZLib.getInstance();
      //long[] compressedLengths = write(exporter,resources,fm);

      src.close();
      fm.close();

    }
    catch (Throwable t) {
      logError(t);
    }
  }

}
