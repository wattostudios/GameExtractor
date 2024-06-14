/*
 * Application:  Game Extractor
 * Author:       wattostudios
 * Website:      http://www.watto.org
 * Copyright:    Copyright (c) 2002-2023 wattostudios
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
import org.watto.datatype.Resource;
import org.watto.ge.helper.FieldValidator;
import org.watto.ge.plugin.ArchivePlugin;
import org.watto.io.FileManipulator;
import org.watto.task.TaskProgressManager;

/**
**********************************************************************************************

**********************************************************************************************
**/
public class Plugin_HOG_2 extends ArchivePlugin {

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public Plugin_HOG_2() {

    super("HOG_2", "HOG_2");

    //         read write replace rename
    setProperties(true, true, false, true);

    setGames("Disney's Chicken Little",
        "Tak 3: The Great Juju Challenge");
    setExtensions("hog");
    setPlatforms("XBox");

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

      // Version
      if (fm.readShort() == 2 && fm.readShort() == 1) {
        rating += 5;
      }

      // Number Of Files
      if (FieldValidator.checkNumFiles(fm.readInt())) {
        rating += 5;
      }

      long arcSize = fm.getLength();

      // Directory Length
      if (FieldValidator.checkLength(fm.readInt(), arcSize)) {
        rating += 5;
      }

      // Padding Size
      if (fm.readInt() == 2048) {
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

      // 2 - Version Major (2)
      // 2 - Version Minor (1)
      fm.skip(4);

      // 4 - Number Of Files?
      int numFiles = fm.readInt();
      FieldValidator.checkNumFiles(numFiles);

      // 4 - Directory Length
      // 4 - Padding Size (2048)
      fm.seek(2048 + numFiles * 12);

      // Loop through directory
      String[] names = new String[numFiles];
      for (int i = 0; i < numFiles; i++) {
        // X - Filename
        // 1 - null Filename Terminator
        String filename = fm.readNullString();
        FieldValidator.checkFilename(filename);
        names[i] = filename;
      }

      fm.seek(2048);

      Resource[] resources = new Resource[numFiles];

      TaskProgressManager.setMaximum(numFiles);

      // Loop through directory
      for (int i = 0; i < numFiles; i++) {
        // 4 - File ID?
        fm.skip(4);

        // 4 - File Offset
        long offset = fm.readInt();
        FieldValidator.checkOffset(offset, arcSize);

        // 4 - File Length
        long length = fm.readInt();
        FieldValidator.checkLength(length, arcSize);

        String filename = names[i];

        //path,id,name,offset,length,decompLength,exporter
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
   * Writes an [archive] File with the contents of the Resources
   **********************************************************************************************
   **/
  @Override
  public void write(Resource[] resources, File path) {
    try {

      FileManipulator fm = new FileManipulator(path, true);
      int numFiles = resources.length;
      TaskProgressManager.setMaximum(numFiles);

      // Calculations
      TaskProgressManager.setMessage(Language.get("Progress_PerformingCalculations"));

      long directoryLength = 0;
      for (int i = 0; i < numFiles; i++) {
        directoryLength += 12 + resources[i].getNameLength() + 1;
      }

      // Write Header Data

      // 2 - Version Major (2)
      fm.writeShort(2);

      // 2 - Version Minor (1)
      fm.writeShort(1);

      // 4 - Number Of Files
      fm.writeInt(numFiles);

      // 4 - Directory Length (length of Details + Filename directories, not including padding)
      fm.writeInt(directoryLength);

      // 4 - Padding Multiple (2048)
      fm.writeInt(2048);

      // 2032 - null Padding to a multiple of 2048 bytes
      for (int i = 0; i < 2032; i++) {
        fm.writeByte(0);
      }

      // Write Directory
      TaskProgressManager.setMessage(Language.get("Progress_WritingDirectory"));

      long offset = 2048 + directoryLength;
      offset += calculatePadding(offset, 2048);

      long filenameOffset = 2048 + (numFiles * 12);

      for (int i = 0; i < numFiles; i++) {
        Resource resource = resources[i];
        long decompLength = resource.getDecompressedLength();

        // 4 - Filename Offset (relative to the start of the Archive)
        fm.writeInt((int) filenameOffset);

        // 4 - File Offset
        fm.writeInt((int) offset);

        // 4 - File Length (not including padding)
        fm.writeInt((int) decompLength);

        filenameOffset += resource.getNameLength() + 1;

        offset += decompLength;
        offset += calculatePadding(offset, 2048);
      }

      for (int i = 0; i < numFiles; i++) {
        Resource resource = resources[i];

        // X - Filename
        fm.writeString(resource.getName());

        // 1 - null Filename Terminator
        fm.writeByte(0);
      }

      // 0-2047 - null Padding to a multiple of 2048 bytes
      int dirPaddingLength = calculatePadding(directoryLength, 2048);
      for (int p = 0; p < dirPaddingLength; p++) {
        fm.writeByte(0);
      }

      // Write Files
      TaskProgressManager.setMessage(Language.get("Progress_WritingFiles"));
      for (int i = 0; i < numFiles; i++) {
        Resource resource = resources[i];

        // X - File Data
        write(resource, fm);
        TaskProgressManager.setValue(i);

        // 0-2047 - null Padding to a multiple of 2048 bytes (this field doesn't exist for the last file in the archive)
        if (i != numFiles - 1) {
          int paddingSize = calculatePadding(resource.getDecompressedLength(), 2048);
          for (int p = 0; p < paddingSize; p++) {
            fm.writeByte(0);
          }
        }
      }

      fm.close();

    }
    catch (Throwable t) {
      logError(t);
    }
  }

}
