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
import org.watto.datatype.FileType;
import org.watto.datatype.Resource;
import org.watto.ge.helper.FieldValidator;
import org.watto.ge.plugin.ArchivePlugin;
import org.watto.io.FileManipulator;
import org.watto.task.TaskProgressManager;

/**
**********************************************************************************************

**********************************************************************************************
**/
public class Plugin_DAT_EDAT_2 extends ArchivePlugin {

  int realNumFiles = 0;

  long maxEndPos = 0;

  int dataOffset = 0;

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public Plugin_DAT_EDAT_2() {

    super("DAT_EDAT_2", "DAT_EDAT_2");

    //         read write replace rename
    setProperties(true, false, false, false);

    setGames("Wargame: Red Dragon");
    setExtensions("dat");
    setPlatforms("PC");

    setFileTypes(new FileType("tgv", "TGV Image", FileType.TYPE_IMAGE));

  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  @SuppressWarnings("unused")
  public void analyseDirectory(FileManipulator fm, File path, Resource[] resources, String dirName, long endPos) throws Exception {
    long arcSize = fm.getLength();

    boolean hasMoreFiles = true;
    //while (hasMoreFiles){
    while (fm.getOffset() < endPos) {
      //System.out.println(fm.getOffset() + "\t" + endPos);
      // 4 - Group/File Indicator (0=file, #=length of this group entry)
      int fileGroupID = fm.readInt();

      if (fileGroupID == 0) {
        // File

        // 4 - Last File Indicator (0=last file in this group, #=length of this file entry)
        if (fm.readInt() == 0) {
          hasMoreFiles = false;
        }

        // 8 - File Offset (relative to the start of the File Data)
        long offset = fm.readLong() + dataOffset;
        FieldValidator.checkOffset(offset, arcSize);

        // 8 - File Length
        long length = fm.readLong();
        FieldValidator.checkLength(length, arcSize);

        // 16 - CRC?
        fm.skip(16);

        // X - Filename (null)
        String filename = fm.readNullString();

        // 0-1 - null padding to a multiple of 2 bytes (ie only exists if filenameLength+2 is odd)
        if (filename.length() % 2 == 0) {
          fm.skip(1);
        }

        filename = dirName + filename;
        //System.out.println(filename);

        //path,id,name,offset,length,decompLength,exporter
        resources[realNumFiles] = new Resource(path, filename, offset, length);

        realNumFiles++;
        TaskProgressManager.setValue(offset);

      }
      else if (fileGroupID < 0) {
        // Re-using an old filename (jump back)

        int currentPos = (int) fm.getOffset();
        fm.seek(currentPos - 4 + fileGroupID);
        // X - Filename
        String filename = dirName + fm.readNullString();
        fm.seek(currentPos);

        // 4 - Last File Indicator (0=last file in this group, #=length of this file entry)
        if (fm.readInt() == 0) {
          hasMoreFiles = false;
        }

        // 8 - File Offset (relative to the start of the File Data)
        long offset = fm.readLong() + dataOffset;
        FieldValidator.checkOffset(offset, arcSize);

        // 8 - File Length
        long length = fm.readLong();
        FieldValidator.checkLength(length, arcSize);

        // 2 - null
        fm.skip(2);

        //path,id,name,offset,length,decompLength,exporter
        resources[realNumFiles] = new Resource(path, filename, offset, length);

        realNumFiles++;
        TaskProgressManager.setValue(offset);

      }
      else {
        // Group

        int groupNameLength = fileGroupID - 8;

        // 4 - Length of this group, including the file entries (ie relative offset to next group) (or null)
        int groupLength = fm.readInt();
        FieldValidator.checkLength(groupLength, arcSize);

        if (groupLength == 0) {
          groupLength = 8;
        }

        int groupEndPos = (int) fm.getOffset() + groupLength - 8;

        // X - Group Name (null)
        String groupName = fm.readNullString();

        // 0-1 - null padding (optional)
        if (groupName.length() % 2 == 0) {
          fm.skip(1);
        }

        groupName = dirName + groupName;

        if (groupEndPos < fm.getOffset()) {
          // basically, if we read the groupLength=0, this means the groupEndPos is the same as the existing endPos 
          groupEndPos = (int) endPos;
        }

        if (groupEndPos > maxEndPos) {
          groupEndPos = (int) maxEndPos;
        }

        analyseDirectory(fm, path, resources, groupName, groupEndPos);

      }

    }

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
      String header = fm.readString(4);
      if (header.equals("edat") || header.equals("EDAT")) {
        rating += 50;
      }

      // 4 - Version? (2)
      if (fm.readInt() == 2) {
        rating += 5;
      }

      fm.skip(17);

      long arcSize = fm.getLength();

      // 4 - Directory Offset
      if (FieldValidator.checkOffset(fm.readInt(), arcSize)) {
        rating += 5;
      }

      // 4 - Directory Length
      if (FieldValidator.checkLength(fm.readInt(), arcSize)) {
        rating += 5;
      }

      fm.skip(4);

      // 8 - File Data Length
      if (FieldValidator.checkLength(fm.readLong(), arcSize)) {
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

      // RESETTING THE GLOBAL VARIABLES
      realNumFiles = 0;
      dataOffset = 0;

      FileManipulator fm = new FileManipulator(path, false);

      long arcSize = fm.getLength();

      // 4 - Header (edat)
      // 1 - Version? (2)
      // 20 - null
      fm.skip(25);

      // 4 - Directory Offset
      long dirOffset = fm.readInt();
      FieldValidator.checkOffset(dirOffset, arcSize);

      // 4 - Directory Length
      int dirLength = fm.readInt();
      FieldValidator.checkLength(dirLength, arcSize);

      maxEndPos = dirOffset + dirLength; // so we can force-exit if we're reading too far in the directory

      // 4 - File Data Offset
      dataOffset = fm.readInt();
      FieldValidator.checkOffset(dataOffset, arcSize);

      // 8 - File Data Length
      // 4 - Padding Multiple (8192)
      // 16 - CRC?
      // 972 - null Padding
      fm.seek(dirOffset);

      int numFiles = Archive.getMaxFiles(4);

      Resource[] resources = new Resource[numFiles];
      TaskProgressManager.setMaximum(arcSize);

      analyseDirectory(fm, path, resources, "", maxEndPos);
      //while (realNumFiles<numFiles){
      //  analyseDirectory(fm,path,resources,"",1);
      //  }

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
