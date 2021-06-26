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
import org.watto.io.converter.IntConverter;
import org.watto.task.TaskProgressManager;

/**
**********************************************************************************************

**********************************************************************************************
**/
public class Plugin_DSPACK extends ArchivePlugin {

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public Plugin_DSPACK() {

    super("DSPACK", "DSPACK");

    //         read write replace rename
    setProperties(true, false, false, false);

    setGames("FEAR 3");
    setExtensions("dspack"); // MUST BE LOWER CASE
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

      boolean bigEndian = false;

      // 4 - Magic Number 1 ((bytes)109,103,102,32)
      // 4 - Magic Number 2 ((bytes)8,1,90,90)
      byte[] headerBytes = fm.readBytes(8);
      if (headerBytes[0] == 109 && headerBytes[1] == 103 && headerBytes[2] == 102 && headerBytes[3] == 32 && headerBytes[4] == 8 && headerBytes[5] == 1 && headerBytes[6] == 90 && headerBytes[7] == 90) {
        rating += 50;
      }
      else if (headerBytes[0] == 32 && headerBytes[1] == 102 && headerBytes[2] == 103 && headerBytes[3] == 109 && headerBytes[4] == 90 && headerBytes[5] == 90 && headerBytes[6] == 1 && headerBytes[7] == 8) {
        bigEndian = true;
        rating += 50;
      }

      // 4 - null
      fm.skip(4);

      // 4 - Number of Folders
      if (bigEndian) {
        if (FieldValidator.checkNumFiles(IntConverter.changeFormat(fm.readInt()))) {
          rating += 5;
        }
      }
      else {
        if (FieldValidator.checkNumFiles(fm.readInt())) {
          rating += 5;
        }
      }

      long arcSize = fm.getLength();

      // 4 - Folder Directory Length
      if (bigEndian) {
        if (FieldValidator.checkLength(IntConverter.changeFormat(fm.readInt()), arcSize)) {
          rating += 5;
        }
      }
      else {
        if (FieldValidator.checkLength(fm.readInt(), arcSize)) {
          rating += 5;
        }
      }

      // 4 - Folder Directory Offset
      if (bigEndian) {
        if (FieldValidator.checkOffset(IntConverter.changeFormat(fm.readInt()), arcSize)) {
          rating += 5;
        }
      }
      else {
        if (FieldValidator.checkOffset(fm.readInt(), arcSize)) {
          rating += 5;
        }
      }

      // 4 - Number of Files
      if (bigEndian) {
        if (FieldValidator.checkNumFiles(IntConverter.changeFormat(fm.readInt()))) {
          rating += 5;
        }
      }
      else {
        if (FieldValidator.checkNumFiles(fm.readInt())) {
          rating += 5;
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

      //ExporterPlugin exporter = Exporter_ZLib.getInstance();

      // RESETTING GLOBAL VARIABLES

      FileManipulator fm = new FileManipulator(path, false);

      long arcSize = fm.getLength();

      // 4 - Magic Number 1 ((bytes)109,103,102,32)
      // 4 - Magic Number 2 ((bytes)8,1,90,90)
      boolean bigEndian = false;
      byte[] headerBytes = fm.readBytes(8);
      if (headerBytes[0] == 109 && headerBytes[1] == 103 && headerBytes[2] == 102 && headerBytes[3] == 32 && headerBytes[4] == 8 && headerBytes[5] == 1 && headerBytes[6] == 90 && headerBytes[7] == 90) {
        // little endian
      }
      else if (headerBytes[0] == 32 && headerBytes[1] == 102 && headerBytes[2] == 103 && headerBytes[3] == 109 && headerBytes[4] == 90 && headerBytes[5] == 90 && headerBytes[6] == 1 && headerBytes[7] == 8) {
        bigEndian = true;
      }

      // 4 - null
      fm.skip(4);

      // 4 - Number of Files
      int numFiles = fm.readInt();
      if (bigEndian) {
        numFiles = IntConverter.changeFormat(numFiles);
      }
      FieldValidator.checkNumFiles(numFiles);

      // 4 - File Details Directory Length
      fm.skip(4);

      // 4 - File Details Directory Offset
      int dirOffset = fm.readInt();
      if (bigEndian) {
        dirOffset = IntConverter.changeFormat(dirOffset);
      }
      FieldValidator.checkOffset(dirOffset, arcSize);

      // 4 - Number of Folders
      int numFolders = fm.readInt();
      if (bigEndian) {
        numFolders = IntConverter.changeFormat(numFolders);
      }
      FieldValidator.checkNumFiles(numFolders);

      // 4 - Folder Directory Length
      fm.skip(4);

      // 4 - Folder Directory Offset
      int folderDirOffset = fm.readInt();
      if (bigEndian) {
        folderDirOffset = IntConverter.changeFormat(folderDirOffset);
      }
      FieldValidator.checkOffset(folderDirOffset, arcSize);

      // 4 - Names Directory Length
      int namesDirLength = fm.readInt();
      if (bigEndian) {
        namesDirLength = IntConverter.changeFormat(namesDirLength);
      }
      FieldValidator.checkLength(namesDirLength, arcSize);

      // 4 - Names Directory Offset
      int namesDirOffset = fm.readInt();
      if (bigEndian) {
        namesDirOffset = IntConverter.changeFormat(namesDirOffset);
      }
      FieldValidator.checkOffset(namesDirOffset, arcSize);

      // read the names directory into memory for quick access
      fm.seek(namesDirOffset);
      byte[] filenameBytes = fm.readBytes(namesDirLength);
      FileManipulator nameFM = new FileManipulator(new ByteBuffer(filenameBytes));

      Resource[] resources = new Resource[numFiles];
      TaskProgressManager.setMaximum(numFiles);

      // Loop through the FILES directory
      fm.seek(dirOffset);
      for (int i = 0; i < numFiles; i++) {
        // 4 - File Name Offset (relative to the start of the Names Directory)
        int filenameOffset = fm.readInt();
        if (bigEndian) {
          filenameOffset = IntConverter.changeFormat(filenameOffset);
        }
        FieldValidator.checkOffset(filenameOffset, arcSize);

        nameFM.seek(filenameOffset);
        String filename = nameFM.readNullString();
        FieldValidator.checkFilename(filename);

        // 4 - Parent Folder ID?
        fm.skip(4);

        // 4 - Decompressed File Length
        int decompLength = fm.readInt();
        if (bigEndian) {
          decompLength = IntConverter.changeFormat(decompLength);
        }
        FieldValidator.checkLength(decompLength);

        // 4 - Compressed File Length
        int length = fm.readInt();
        if (bigEndian) {
          length = IntConverter.changeFormat(length);
        }
        FieldValidator.checkLength(length, arcSize);

        // 4 - Unknown
        fm.skip(4);

        // 4 - File Offset
        int offset = fm.readInt();
        if (bigEndian) {
          offset = IntConverter.changeFormat(offset);
        }
        FieldValidator.checkOffset(offset, arcSize);

        //path,name,offset,length,decompLength,exporter
        resources[i] = new Resource(path, filename, offset, length, decompLength);

        TaskProgressManager.setValue(i);
      }

      // Loop through the FOLDERS directory
      fm.seek(folderDirOffset);
      String[] folderNames = new String[numFolders];
      for (int i = 0; i < numFolders; i++) {
        // 4 - Folder Name Offset (relative to the start of the Names Directory)
        int filenameOffset = fm.readInt();
        if (bigEndian) {
          filenameOffset = IntConverter.changeFormat(filenameOffset);
        }
        FieldValidator.checkOffset(filenameOffset, arcSize);

        nameFM.seek(filenameOffset);
        String folderName = nameFM.readNullString();
        FieldValidator.checkFilename(folderName);

        // 4 - Parent Folder ID? (-1 if no parent)
        int parentFolderID = fm.readInt();
        if (bigEndian) {
          parentFolderID = IntConverter.changeFormat(parentFolderID);
        }
        FieldValidator.checkRange(parentFolderID, -1, numFolders);
        if (parentFolderID != -1) {
          folderNames[i] = folderNames[parentFolderID] + folderName;
        }
        else {
          folderNames[i] = folderName;
        }

        //System.out.println(i + "\tparent=" + parentFolderID + "\t" + folderName);

        // 4 - Folder ID of the Last Sub-Folder in this Folder (or -1 if only 1 or 0 sub folders)
        // 4 - Folder ID of the First Sub-Folder in this Folder (or -1 if no sub-folders)
        fm.skip(8);

        // 4 - File ID of the First File in this Folder
        int firstFileID = fm.readInt();
        if (bigEndian) {
          firstFileID = IntConverter.changeFormat(firstFileID);
        }
        FieldValidator.checkRange(firstFileID, -1, numFiles);

        // 4 - File ID of the Last File in this Folder
        int lastFileID = fm.readInt();
        if (bigEndian) {
          lastFileID = IntConverter.changeFormat(lastFileID);
        }
        FieldValidator.checkRange(lastFileID, -1, numFiles);

        folderName = folderNames[i]; // so we can add it to the filenames, including all sub-folders

        for (int j = firstFileID; j < lastFileID; j++) {
          Resource resource = resources[j];
          String name = folderName + resource.getName();

          resource.setName(name);
          resource.setOriginalName(name);
        }

      }

      nameFM.close();

      fm.close();

      return resources;

    }
    catch (Throwable t) {
      logError(t);
      return null;
    }
  }

}
