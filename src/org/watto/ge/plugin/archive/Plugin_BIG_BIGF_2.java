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
import org.watto.datatype.Resource;
import org.watto.ge.helper.FieldValidator;
import org.watto.ge.plugin.ArchivePlugin;
import org.watto.io.FileManipulator;
import org.watto.task.TaskProgressManager;

/**
**********************************************************************************************

**********************************************************************************************
**/
public class Plugin_BIG_BIGF_2 extends ArchivePlugin {

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public Plugin_BIG_BIGF_2() {

    super("BIG_BIGF_2", "BIG_BIGF_2");

    //         read write replace rename
    setProperties(true, true, false, true);

    setGames("Toca Race Driver 3: Honda Civic 2006",
        "Colin McRae Rally 04",
        "Colin McRae Rally 05",
        "Colin McRae Rally 3",
        "Race Driver 2: V8 Supercars 2",
        "Race Driver 3: V8 Supercars 3",
        "Toca Race Driver",
        "Toca Race Driver 2",
        "Toca Race Driver 3",
        "Sensible Soccer 2006");
    setExtensions("big", "b2k", "b64");
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
      if (fm.readString(4).equals("BIGF")) {
        rating += 50;
      }

      // Number Of Files
      if (FieldValidator.checkNumFiles(fm.readInt())) {
        rating += 5;
      }

      long arcSize = fm.getLength();

      // First File Offset
      if (FieldValidator.checkOffset(fm.readInt(), arcSize)) {
        rating += 5;
      }

      // Padding Multiple
      int paddingSize = fm.readInt();
      if (paddingSize == 2048 || paddingSize == 64) {
        rating += 5;
      }

      fm.skip(19);

      // null
      if (fm.readByte() == 0) {
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

      // 4 - Header (BIGF)
      fm.skip(4);

      // 4 - Number Of Files
      int numFiles = fm.readInt();
      FieldValidator.checkNumFiles(numFiles);

      // 4 - First File Offset
      int relOffset = fm.readInt();
      FieldValidator.checkOffset(relOffset, arcSize);

      // 4 - Padding Multiple (2048)
      // 20 - Description? Directory Name? (null)
      fm.skip(24);

      Resource[] resources = new Resource[numFiles];

      TaskProgressManager.setMaximum(numFiles);

      // Loop through directory
      for (int i = 0; i < numFiles; i++) {
        // 16 - Filename (null)
        String filename = fm.readNullString(16);
        FieldValidator.checkFilename(filename);

        // 4 - File Length
        long length = fm.readInt();
        FieldValidator.checkLength(length, arcSize);

        // 4 - File Offset (relative to the start of the file data)
        long offset = fm.readInt() + relOffset;
        FieldValidator.checkOffset(offset, arcSize + 1);

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

      long directorySize = 36 + (numFiles * 24);
      directorySize += calculatePadding(directorySize, 2048);

      // Write Header Data

      // 4 - Header (BIGF)
      fm.writeString("BIGF");

      // 4 - Number Of Files
      fm.writeInt(numFiles);

      // 4 - First File Offset
      fm.writeInt((int) directorySize);

      // 4 - Padding Multiple (2048)
      fm.writeInt(2048);

      // 20 - Description? Directory Name? (null)
      for (int i = 0; i < 20; i++) {
        fm.writeByte(0);
      }

      // Write Directory
      TaskProgressManager.setMessage(Language.get("Progress_WritingDirectory"));
      long offset = 0;
      for (int i = 0; i < numFiles; i++) {
        Resource resource = resources[i];
        long decompLength = resource.getDecompressedLength();

        // 16 - Filename (null)
        fm.writeNullString(resource.getName(), 16);

        // 4 - File Length
        fm.writeInt((int) decompLength);

        // 4 - File Offset (relative to the start of the file data)
        fm.writeInt((int) offset);

        offset += decompLength;
        offset += calculatePadding(offset, 2048);
      }

      // 0-2047 - null Padding to a multiple of 2048 bytes
      int dirPaddingSize = calculatePadding(fm.getOffset(), 2048);
      for (int p = 0; p < dirPaddingSize; p++) {
        fm.writeByte(0);
      }

      // Write Files
      TaskProgressManager.setMessage(Language.get("Progress_WritingFiles"));
      for (int i = 0; i < numFiles; i++) {
        Resource resource = resources[i];
        long decompLength = resource.getDecompressedLength();

        if (decompLength == 0) {
          continue;
        }

        // X - File Data
        write(resource, fm);

        // 0-2047 - Padding (with byte 63) to a multiple of 2048 bytes
        int paddingSize = calculatePadding(fm.getOffset(), 2048);
        for (int p = 0; p < paddingSize; p++) {
          fm.writeByte(63);
        }

        TaskProgressManager.setValue(i);

      }

      //ExporterPlugin exporter = new Exporter_ZLib();
      //long[] compressedLengths = write(exporter,resources,fm);

      fm.close();

    }
    catch (Throwable t) {
      logError(t);
    }
  }

}
