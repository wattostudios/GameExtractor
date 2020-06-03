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
public class Plugin_DAT_65 extends ArchivePlugin {

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public Plugin_DAT_65() {

    super("DAT_65", "DAT_65");

    //         read write replace rename
    setProperties(true, true, true, true);

    setGames("Brian Lara International Cricket 2005");
    setExtensions("dat"); // MUST BE LOWER CASE
    setPlatforms("PS2");

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

      // Archive Size
      if (FieldValidator.checkEquals(fm.readInt(), arcSize)) {
        rating += 5;
      }

      // 4 - Offset to the end of the Details Directory (not including padding)
      if (FieldValidator.checkOffset(fm.readInt(), arcSize)) {
        rating += 5;
      }

      // Number Of Files
      if (FieldValidator.checkNumFiles(fm.readInt())) {
        rating += 5;
      }

      fm.skip(20);

      // 64 - Game Details ("GAME : Brian Lara Cricket 2" + nulls to fill)
      if (fm.readString(4).equals("GAME")) {
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

      FileManipulator fm = new FileManipulator(path, false);

      long arcSize = fm.getLength();

      // 4 - Archive Length
      // 4 - Offset to the end of the Details Directory (not including padding)
      fm.skip(8);

      // 4 - Number of Files
      int numFiles = fm.readInt();
      FieldValidator.checkNumFiles(numFiles);

      // 20 - null
      // 64 - Game Details ("GAME : Brian Lara Cricket 2" + nulls to fill)
      // 16 - Date ("DATE : 28/6/5" + nulls to fill)
      // 48 - Time ("TIME : 14:43" + nulls to fill)
      fm.skip(148);

      Resource[] resources = new Resource[numFiles];
      TaskProgressManager.setMaximum(numFiles);

      // Loop through directory
      for (int i = 0; i < numFiles; i++) {
        // 4 - Entry Length (80)
        fm.skip(4);

        // 4 - File Offset
        long offset = fm.readInt();
        FieldValidator.checkOffset(offset, arcSize);

        // 4 - File Length
        long length = fm.readInt();
        FieldValidator.checkLength(length, arcSize);

        // 4 - Filename Length
        fm.skip(4);

        // 64 - Filename (null terminated, filled with nulls)
        String filename = fm.readNullString(64);
        FieldValidator.checkFilename(filename);

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

      long endOfDirectory = 160 + numFiles * 80;

      long archiveSize = endOfDirectory + calculatePadding(endOfDirectory, 32768);
      for (int i = 0; i < numFiles; i++) {
        long length = resources[i].getDecompressedLength();
        length += calculatePadding(length, 32768);
        archiveSize += length;
      }

      // Write Header Data

      // 4 - Archive Length
      fm.writeInt((int) archiveSize);

      // 4 - Offset to the end of the Details Directory (not including padding)
      fm.writeInt((int) endOfDirectory);

      // 4 - Number of Files
      fm.writeInt(numFiles);

      // 20 - null
      for (int i = 0; i < 20; i++) {
        fm.writeByte(0);
      }

      // 64 - Game Details ("GAME : Brian Lara Cricket 2" + nulls to fill)
      fm.writeNullString("GAME : Brian Lara Cricket 2", 64);

      // 16 - Date ("DATE : 28/6/5" + nulls to fill)
      fm.writeNullString("DATE : 28/6/5", 16);

      // 48 - Time ("TIME : 14:43" + nulls to fill)
      fm.writeNullString("TIME : 14:43", 48);

      // Write Directory
      TaskProgressManager.setMessage(Language.get("Progress_WritingDirectory"));
      long offset = endOfDirectory + calculatePadding(endOfDirectory, 32768);
      for (int i = 0; i < numFiles; i++) {
        Resource resource = resources[i];
        long decompLength = resource.getDecompressedLength();

        // 4 - Entry Length (80)
        fm.writeInt(80);

        // 4 - File Offset
        fm.writeInt((int) offset);

        // 4 - File Length
        fm.writeInt((int) decompLength);

        String filename = resource.getName();
        if (filename.length() > 64) {
          filename = filename.substring(0, 64);
        }

        // 4 - Filename Length
        fm.writeInt(filename.length());

        // 64 - Filename (null terminated, filled with nulls)
        fm.writeNullString(filename, 64);

        offset += decompLength + calculatePadding(decompLength, 32768);
      }

      // X - null Padding to a multiple of 32768? bytes
      int dirPadding = calculatePadding(endOfDirectory, 32768);
      for (int i = 0; i < dirPadding; i++) {
        fm.writeByte(0);
      }

      // Write Files
      TaskProgressManager.setMessage(Language.get("Progress_WritingFiles"));

      for (int i = 0; i < numFiles; i++) {
        Resource resource = resources[i];

        // X - File Data
        write(resource, fm);

        // X - null Padding to a multiple of 32768? bytes
        int filePadding = calculatePadding(resource.getDecompressedLength(), 32768);
        for (int p = 0; p < filePadding; p++) {
          fm.writeByte(0);
        }

        TaskProgressManager.setValue(i);
      }

      fm.close();

    }
    catch (Throwable t) {
      logError(t);
    }
  }

}
