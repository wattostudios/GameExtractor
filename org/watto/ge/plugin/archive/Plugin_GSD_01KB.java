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
public class Plugin_GSD_01KB extends ArchivePlugin {

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public Plugin_GSD_01KB() {

    super("GSD_01KB", "GSD_01KB");

    //         read write replace rename
    setProperties(true, false, true, false);

    setGames("Driver 3");
    setExtensions("gsd");
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
      if (fm.readString(4).equals("01KB")) {
        rating += 50;
      }

      // Number Of Files
      int numFiles = fm.readInt();
      if (FieldValidator.checkNumFiles(numFiles)) {
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

      // 4 - Header (01KB)
      fm.skip(4);

      // 4 - Number Of Files
      int numFiles = fm.readInt();
      FieldValidator.checkNumFiles(numFiles);

      long arcSize = fm.getLength();

      Resource[] resources = new Resource[numFiles];
      TaskProgressManager.setMaximum(numFiles);

      // Loop through directory
      for (int i = 0; i < numFiles; i++) {
        // 4 - Data Offset
        long offset = fm.readInt();
        FieldValidator.checkOffset(offset, arcSize);

        String filename = Resource.generateFilename(i);

        //path,id,name,offset,length,decompLength,exporter
        resources[i] = new Resource(path, filename, offset);

        TaskProgressManager.setValue(i);
      }

      // Calculate the file sizes
      for (int i = 0; i < numFiles; i++) {
        fm.seek(resources[i].getOffset() + 4);

        // 4 - Header Size
        // 4 - File Length
        long length = fm.readInt() + fm.readInt();
        FieldValidator.checkLength(length, arcSize);

        resources[i].setLength(length);
        resources[i].setDecompressedLength(length);
        resources[i].setOffset((int) fm.getOffset() + 4);
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
      TaskProgressManager.setMessage(Language.get("Progress_PerformingCalculations"));

      long fileOffset = 8 + (numFiles * 4) + 4;
      long paddingSize = 2048 - (fileOffset % 2048);
      if (paddingSize != 2048) {
        fileOffset += paddingSize;
      }

      // Write Header Data

      // 4 - Header (01KB)
      // 4 - Number Of Files
      fm.writeBytes(src.readBytes(8));

      // for each file
      int[] oldOffsets = new int[numFiles];
      // 4 - File Offset
      for (int i = 0; i < numFiles; i++) {
        oldOffsets[i] = src.readInt();
      }

      // 4 - Archive Length
      // X - null Padding to multiple of 2048 bytes
      fm.setLength(fileOffset);
      fm.seek(fileOffset);

      // Write Files
      long[] offsets = new long[numFiles];

      TaskProgressManager.setMessage(Language.get("Progress_WritingFiles"));
      for (int i = 0; i < numFiles; i++) {
        Resource fd = resources[i];
        long length = fd.getDecompressedLength();

        offsets[i] = fm.getOffset();

        src.seek(oldOffsets[i]); // jump to src file offset

        // 4 - Number Of Trailing 4-byte Fields
        int numTrailing = src.readInt();
        fm.writeInt(numTrailing);

        // 4 - Header Size
        int headerSize = src.readInt();
        //System.out.println(headerSize);
        fm.writeInt(headerSize);

        // because the "length" of the file, we read in as length + headerSize, so when writing back, we need to remove the headerSize
        // but ONLY if this is an original file - if it's a file we added, the length is actually correct, as it's the length of the source file
        if (!fd.isReplaced()) {
          length -= headerSize;
        }

        // 4 - File Length
        int oldLength = src.readInt();
        fm.writeInt((int) length);

        // 4 - null
        fm.writeBytes(src.readBytes(4));

        // X - Header (size of field = headerSize-16)
        fm.writeBytes(src.readBytes(headerSize - 16));

        // X - File Data
        write(resources[i], fm);
        src.skip(oldLength);

        for (int p = 0; p < numTrailing; p++) {
          // 4 - Unknown
          fm.writeBytes(src.readBytes(4));
        }

        // X - null Padding to a multiple of 2048 bytes
        paddingSize = 2048 - (fm.getOffset() % 2048);
        if (paddingSize != 2048) {
          for (int k = 0; k < paddingSize; k++) {
            fm.writeByte(0);
          }
        }

      }

      long arcSize = fm.getOffset();

      // Write Directory (go back to the beginning and start writing)
      TaskProgressManager.setMessage(Language.get("Progress_WritingDirectory"));

      fm.seek(8);

      for (int i = 0; i < numFiles; i++) {
        // 4 - File Offset
        fm.writeInt((int) offsets[i]);
      }

      // 4 - Archive Length
      fm.writeInt((int) arcSize);

      src.close();
      fm.close();

    }
    catch (Throwable t) {
      logError(t);
    }
  }

}
