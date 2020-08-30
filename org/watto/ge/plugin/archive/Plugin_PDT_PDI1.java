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
import org.watto.task.TaskProgressManager;

/**
**********************************************************************************************

**********************************************************************************************
**/
public class Plugin_PDT_PDI1 extends ArchivePlugin {

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public Plugin_PDT_PDI1() {

    super("PDT_PDI1", "PDT_PDI1");

    //         read write replace rename
    setProperties(true, false, false, false);

    setGames("Rally Masters",
        "Swedish Touring Car Championship 2",
        "Test Drive Rally");
    setExtensions("pdt");
    setPlatforms("PC");

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
      if (fm.readString(4).equals("PDI1")) {
        rating += 50;
      }

      // Number Of Files
      if (FieldValidator.checkNumFiles(fm.readInt())) {
        rating += 5;
      }

      // Number Of Files
      if (FieldValidator.checkNumFiles(fm.readInt())) {
        rating += 5;
      }

      fm.skip(4);

      // Directory Offset
      if (fm.readInt() == 46) {
        rating += 5;
      }

      long arcSize = fm.getLength();

      // File Data Offset
      if (FieldValidator.checkOffset(fm.readInt(), arcSize)) {
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

      // RESETTING THE GLOBAL VARIABLES

      FileManipulator fm = new FileManipulator(path, false);

      long arcSize = (int) fm.getLength();

      // 4 - Header (PDI1)
      // 4 - Number Of Files (including Padding files) (128)
      fm.skip(8);

      // 4 - Number Of Files
      int numFiles = fm.readInt();
      FieldValidator.checkNumFiles(numFiles);

      // 4 - Unknown (30)
      fm.skip(4);

      // 4 - Directory Offset (46)
      long dirOffset = fm.readInt();
      FieldValidator.checkOffset(dirOffset, arcSize);

      // 4 - CDI1 Data Offset
      int relOffset = fm.readInt() + 4;
      FieldValidator.checkOffset(relOffset, arcSize);

      // 4 - Length Of CDI1 Data
      // 2 - null
      // 14 - Padding (all 255's)
      // 2 - Unknown (3)
      fm.seek(dirOffset);

      Resource[] resources = new Resource[numFiles];

      TaskProgressManager.setMaximum(numFiles);

      // Loop through directory
      int realNumFiles = 0;

      String[] directories = new String[20]; // max 20 nested directories
      int[] lastFileIDs = new int[20];
      int numDirs = 0;
      String dirName = "";
      for (int i = 0; i < numFiles; i++) {

        // see if any directories have ended
        boolean changed = false;
        /*
        for (int d = numDirs - 1; d >= 0; d--) {
          if (i >= lastFileIDs[d]) {
            numDirs--;
            changed = true;
          }
          else {
            break;
          }
        }
        */
        for (int d = 0; d < numDirs; d++) {
          if (i >= lastFileIDs[d]) {
            numDirs = d;
            changed = true;
            break;
          }
        }

        if (changed) {
          dirName = "";
          for (int d = 0; d < numDirs; d++) {
            dirName += directories[d];
          }
        }

        // 2 - File/Directory ID (4=directory, 24/25=file, 0=padding file)
        int entryType = fm.readShort();
        if (entryType == 4) {
          // directory

          // 2 - Parent File ID?
          fm.skip(2);

          // 2 - Last File ID (of the Last File in this directory, or -1 means until the end of the archive)
          int lastFileID = fm.readShort();

          // 2 - Unknown
          int nextID = fm.readShort();

          if (lastFileID == -1) {
            if (nextID == -1) {
              lastFileID = numFiles; // max
            }
            else {
              lastFileID = nextID;
            }
          }

          // 2 - File ID of this Directory
          // 4 - Unknown
          // 6 - null
          fm.skip(12);

          // 44 - Directory Name (null)
          String filename = fm.readNullString(44);
          FieldValidator.checkFilename(filename);

          filename += "\\";

          directories[numDirs] = filename;
          lastFileIDs[numDirs] = lastFileID;
          numDirs++;

          dirName += filename;

          // 4 - null
          fm.skip(4);

        }
        else if (entryType != 24 && entryType != 25) {
          fm.skip(66);
        }
        else {
          // file

          // 2 - File ID (incremental from -1)(some are -1, which skip over the number for this loop)
          // 2 - File ID (incremental from 1)(some are -1, which skip over the number for this loop)
          // 6 - Padding (all 255's)
          fm.skip(10);

          // 4 - File Offset (relative to the start of the file data)
          long offset = fm.readInt() + relOffset;
          FieldValidator.checkOffset(offset, arcSize);

          // 4 - File Length
          long length = fm.readInt();
          FieldValidator.checkLength(length, arcSize);

          // 44 - Filename (null)
          String filename = fm.readNullString(44);
          FieldValidator.checkFilename(filename);

          filename = dirName + filename;

          // 4 - Unknown
          fm.skip(4);

          //path,id,name,offset,length,decompLength,exporter
          resources[realNumFiles] = new Resource(path, filename, offset, length);
          realNumFiles++;

        }
        TaskProgressManager.setValue(i);
      }

      resources = resizeResources(resources, realNumFiles);

      // Now go through and work out if files are compressed...
      fm.getBuffer().setBufferSize(10); // small and quick

      for (int i = 0; i < realNumFiles; i++) {
        Resource resource = resources[i];

        fm.seek(resource.getOffset());

        // 4 - Decompressed File Length
        int decompLength = fm.readInt();
        if (decompLength < 0) {
          continue;
        }

        // 4 - Compression Flag? (1114112)
        int empty = fm.readShort();
        fm.skip(2);
        if (empty != 0) {
          continue;
        }

        // X - Compressed Data
        // 1 - null
        // 4 - Footer (CDI1)
        resource.setOffset(fm.getOffset());
        resource.setLength(resource.getLength() - 13);
        resource.setDecompressedLength(decompLength);

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
