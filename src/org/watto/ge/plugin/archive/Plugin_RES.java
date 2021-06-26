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
public class Plugin_RES extends ArchivePlugin {

  /**
  **********************************************************************************************

  **********************************************************************************************
  **/
  public Plugin_RES() {

    super("RES", "RES");

    //         read write replace rename
    setProperties(true, false, true, true);

    setExtensions("res");
    setGames("Evil Islands: Curse Of The Lost Soul",
        "Etherlords",
        "Etherlords 2");
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
      if (fm.readString(4).equals(new String(new byte[] { (byte) 60, (byte) 226, (byte) 156, (byte) 1 }))) {
        rating += 50;
      }

      // Number Of Files
      if (FieldValidator.checkNumFiles(fm.readInt())) {
        rating += 5;
      }

      long arcSize = fm.getLength();

      // Directory Offset
      if (FieldValidator.checkOffset(fm.readInt(), arcSize)) {
        rating += 5;
      }

      // Filename Directory Length
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

      FileManipulator fm = new FileManipulator(path, false);

      long arcSize = fm.getLength();

      // 4 - Header? ((bytes)60,226,156,1)
      fm.skip(4);

      // 4 - Number Of Files
      int numFiles = fm.readInt();
      FieldValidator.checkNumFiles(numFiles);

      // 4 - Directory Offset
      long dirOffset = fm.readInt();
      FieldValidator.checkOffset(dirOffset, arcSize);

      // 4 - Filename Directory Length

      fm.seek(dirOffset);

      Resource[] resources = new Resource[numFiles];
      TaskProgressManager.setMaximum(numFiles);

      long[] nameOffsets = new long[numFiles];
      long[] nameLengths = new long[numFiles];

      long relNameOffset = dirOffset + (numFiles * 22);

      for (int i = 0; i < numFiles; i++) {
        // 4 - Unknown
        fm.skip(4);

        // 4 - File Length
        long length = fm.readInt();
        FieldValidator.checkLength(length, arcSize);

        // 4 - File Offset
        long offset = fm.readInt();
        FieldValidator.checkOffset(offset, arcSize);

        // 4 - Unknown
        fm.skip(4);

        // 2 - Filename Length
        int filenameLength = fm.readShort();
        nameLengths[i] = filenameLength;

        // 4 - Filename Offset (relative to the start of the filename directory)
        long filenameOffset = relNameOffset + fm.readInt();
        FieldValidator.checkOffset(filenameOffset, arcSize);
        nameOffsets[i] = filenameOffset;

        //path,id,name,offset,length,decompLength,exporter
        resources[i] = new Resource(path, offset, length);

        TaskProgressManager.setValue(i);
      }

      // READ THROUGH THE FILENAME DIRECTORY
      for (int i = 0; i < numFiles; i++) {
        Resource resource = resources[i];

        fm.seek(nameOffsets[i]);

        // X - Filename
        String filename = fm.readString((int) nameLengths[i]);
        FieldValidator.checkFilename(filename);

        resource.setOriginalName(filename);
        resource.setName(filename);
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
  @Override
  public void write(Resource[] resources, File path) {
    try {

      FileManipulator fm = new FileManipulator(path, true);
      FileManipulator src = new FileManipulator(new File(Settings.getString("CurrentArchive")), false);

      int numFiles = resources.length;
      TaskProgressManager.setMaximum(numFiles);

      // Calculations
      TaskProgressManager.setMessage(Language.get("Progress_PerformingCalculations"));

      long dirOffset = 16;
      int filenameDirLength = 0;
      for (int i = 0; i < numFiles; i++) {
        dirOffset += resources[i].getDecompressedLength();
        filenameDirLength += resources[i].getNameLength();
      }

      // Write Header Data

      // 4 - Header? ((bytes)60,226,156,1)
      // 4 - Number Of Files
      fm.writeBytes(src.readBytes(8));

      // 4 - Directory Offset
      fm.writeInt((int) dirOffset);
      int oldDirOffset = src.readInt();

      // 4 - Filename Directory Length
      fm.writeInt(filenameDirLength);
      src.skip(4);

      // Write Files
      TaskProgressManager.setMessage(Language.get("Progress_WritingFiles"));
      write(resources, fm);

      src.seek(oldDirOffset);

      // Write Directory
      TaskProgressManager.setMessage(Language.get("Progress_WritingDirectory"));
      long offset = 16;
      int relFilenameOffset = 0;
      for (int i = 0; i < numFiles; i++) {
        Resource fd = resources[i];
        long length = fd.getDecompressedLength();
        int filenameLength = fd.getNameLength();

        // 4 - Unknown
        fm.writeBytes(src.readBytes(4));

        // 4 - File Length
        fm.writeInt((int) length);
        src.skip(4);

        // 4 - File Offset
        fm.writeInt((int) offset);
        src.skip(4);

        // 4 - Unknown
        fm.writeBytes(src.readBytes(4));

        // 2 - Filename Length
        fm.writeShort((short) filenameLength);
        src.skip(2);

        // 4 - Filename Offset (relative to the start of the filename directory)
        fm.writeInt(relFilenameOffset);
        src.skip(4);

        offset += length;
        relFilenameOffset += filenameLength;
      }

      for (int i = 0; i < numFiles; i++) {
        // X - Filename
        fm.writeString(resources[i].getName());
      }

      src.close();
      fm.close();

    }
    catch (Throwable t) {
      logError(t);
    }
  }

}