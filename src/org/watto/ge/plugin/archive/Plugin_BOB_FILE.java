/*
 * Application:  Game Extractor
 * Author:       wattostudios
 * Website:      http://www.watto.org
 * Copyright:    Copyright (c) 2002-2021 wattostudios
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
import org.watto.datatype.FileType;
import org.watto.datatype.Resource;
import org.watto.ge.helper.FieldValidator;
import org.watto.ge.plugin.ArchivePlugin;
import org.watto.io.FileManipulator;
import org.watto.io.buffer.ByteBuffer;
import org.watto.task.TaskProgressManager;

/**
**********************************************************************************************

**********************************************************************************************
**/
public class Plugin_BOB_FILE extends ArchivePlugin {

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public Plugin_BOB_FILE() {

    super("BOB_FILE", "BOB_FILE");

    //         read write replace rename
    setProperties(true, false, false, false);

    setGames("Lego Island 2");
    setExtensions("bob"); // MUST BE LOWER CASE
    setPlatforms("PC");

    setFileTypes(new FileType("msh", "MSH Mesh", FileType.TYPE_MODEL));

    setTextPreviewExtensions("app", "emi", "par", "mov", "str", "gob", "h", "swl", "wbt"); // LOWER CASE

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

      getDirectoryFile(fm.getFile(), "bod");
      rating += 25;

      // Header
      if (fm.readString(4).equals("FILE")) {
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

      long arcSize = (int) path.length();

      File sourcePath = getDirectoryFile(path, "bod");
      FileManipulator fm = new FileManipulator(sourcePath, false);

      long bodLength = sourcePath.length();
      long footerOffset = bodLength - 16;

      fm.seek(footerOffset);

      // 4 - Filename Directory Length
      int filenameDirLength = fm.readInt();
      FieldValidator.checkLength(filenameDirLength, bodLength);

      // 4 - Number of Files + Directories
      int numFiles = fm.readInt();
      FieldValidator.checkNumFiles(numFiles);

      // 4 - null
      fm.skip(4);

      // 4 - Details Directory Offset
      long dirOffset = fm.readInt();
      FieldValidator.checkOffset(dirOffset, bodLength);

      fm.seek(0);

      byte[] nameBytes = fm.readBytes(filenameDirLength);
      FileManipulator nameFM = new FileManipulator(new ByteBuffer(nameBytes));

      fm.seek(dirOffset);

      Resource[] allResources = new Resource[numFiles];
      Resource[] resources = new Resource[numFiles];

      TaskProgressManager.setMaximum(numFiles);

      // Loop through directory, READ ONLY THE FILES
      int realNumFiles = 0;
      for (int i = 0; i < numFiles; i++) {
        // 4 - Header (DIRY = Directory, FILE = File)
        String entryType = fm.readString(4);

        if (entryType.equals("DIRY")) {
          // 4 - Number of Files in this Directory
          // 4 - Entry ID of the First File in this Directory (starts at 0)
          // 4 - Directory Name Offset (relative to the start of the Names Directory)
          fm.skip(12);
        }
        else if (entryType.equals("FILE")) {
          // 4 - File Length
          long length = fm.readInt();
          FieldValidator.checkLength(length, arcSize);

          // 4 - File Offset (points to the File Data, not the FILE header)
          long offset = fm.readInt();
          FieldValidator.checkOffset(offset, arcSize);

          // 4 - Filename Offset (relative to the start of the Names Directory)
          int filenameOffset = fm.readInt();
          FieldValidator.checkOffset(filenameOffset, filenameDirLength);

          nameFM.seek(filenameOffset);
          String filename = nameFM.readNullString();

          //path,name,offset,length,decompLength,exporter
          Resource resource = new Resource(path, filename, offset, length);
          allResources[i] = resource;
          resources[realNumFiles] = resource;
          realNumFiles++;

          TaskProgressManager.setValue(i);
        }
        else {
          ErrorLogger.log("[BOB_FILE] Unknown entry type: " + entryType);
        }
      }

      fm.seek(dirOffset);

      String[] dirNames = new String[numFiles];

      // Loop through directory, PROCESS ONLY THE DIRECTORIES
      for (int i = 0; i < numFiles; i++) {
        // 4 - Header (DIRY = Directory, FILE = File)
        String entryType = fm.readString(4);

        if (entryType.equals("DIRY")) {
          // 4 - Number of Files in this Directory
          int numFilesInDir = fm.readInt();
          FieldValidator.checkRange(numFilesInDir, 0, numFiles);

          // 4 - Entry ID of the First File in this Directory (starts at 0)
          int firstEntryID = fm.readInt();
          FieldValidator.checkRange(firstEntryID, 0, numFiles);

          int lastEntryID = firstEntryID + numFilesInDir;

          // 4 - Directory Name Offset (relative to the start of the Names Directory)
          int dirNameOffset = fm.readInt();
          FieldValidator.checkOffset(dirNameOffset, filenameDirLength);

          nameFM.seek(dirNameOffset);
          String dirName = nameFM.readNullString() + "/";

          if (dirNames[i] != null) {
            dirName = dirNames[i] + dirName;
          }

          for (int e = firstEntryID; e < lastEntryID; e++) {
            Resource resource = allResources[e];
            if (resource == null) {
              // a nested directory
              if (dirNames[e] == null) {
                dirNames[e] = dirName;
              }
              else {
                dirNames[e] = dirNames[e] + dirName;
              }
            }
            else {
              String newName = dirName + resource.getName();
              resource.setName(newName);
              resource.setOriginalName(newName);
            }
          }

        }
        else if (entryType.equals("FILE")) {
          // 4 - File Length
          // 4 - File Offset (points to the File Data, not the FILE header)
          // 4 - Filename Offset (relative to the start of the Names Directory)
          fm.skip(12);
        }
        else {
          ErrorLogger.log("[BOB_FILE] Unknown entry type: " + entryType);
        }

      }

      resources = resizeResources(resources, realNumFiles);

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
