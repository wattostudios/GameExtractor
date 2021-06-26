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
import org.watto.io.FilenameSplitter;
import org.watto.io.converter.StringConverter;
import org.watto.task.TaskProgressManager;

/**
**********************************************************************************************

**********************************************************************************************
**/
public class Plugin_LAB_LABN extends ArchivePlugin {

  /**
  **********************************************************************************************

  **********************************************************************************************
  **/
  public Plugin_LAB_LABN() {

    super("LAB_LABN", "LAB_LABN");

    //         read write replace rename
    setProperties(true, false, true, true);

    setExtensions("lab");
    setGames("Star Wars Episode 1: The Phantom Menace",
        "Outlaws",
        "Escape From Monkey Island",
        "Grim Fandango",
        "Grim Fandango Remastered");
    setPlatforms("PC");

    setFileTypes("bsnd", "Sound File");

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
      if (fm.readString(4).equals("LABN")) {
        rating += 50;
      }

      fm.skip(4);

      // Number Of Files
      if (FieldValidator.checkNumFiles(fm.readInt())) {
        rating += 5;
      }

      long arcSize = fm.getLength();

      // 4 - Filename Directory Length
      if (FieldValidator.checkLength(fm.readInt(), arcSize)) {
        rating += 5;
      }

      // 4 - null
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

  **********************************************************************************************
  **/
  @Override
  public Resource[] read(File path) {
    try {

      addFileTypes();

      FileManipulator fm = new FileManipulator(path, false);

      // 4 - Header
      // 4 - Unknown (65644)
      fm.skip(8);

      // 4 - Number Of Files
      int numFiles = fm.readInt();
      FieldValidator.checkNumFiles(numFiles);

      long arcSize = fm.getLength();

      Resource[] resources = new Resource[numFiles];
      TaskProgressManager.setMaximum(numFiles);

      // 4 - Filename Directory Length
      int nameLength = fm.readInt();
      FieldValidator.checkLength(nameLength, arcSize);

      // 4 - null
      fm.skip(4);

      fm.skip(((numFiles - 1) * 16) + 12);

      String[] filenames = new String[numFiles];
      if (nameLength == 0) {
        for (int i = 0; i < numFiles; i++) {
          filenames[i] = Resource.generateFilename(i);
        }
      }
      else {
        for (int i = 0; i < numFiles; i++) {
          // X - Filename
          // 1 - null Filename Terminator
          String filename = fm.readNullString();
          FieldValidator.checkFilename(filename);
          filenames[i] = filename;
        }
      }

      fm.seek(20);

      for (int i = 0; i < numFiles; i++) {
        // 4 - File Offset
        long offset = fm.readInt();
        FieldValidator.checkOffset(offset, arcSize);

        // 4 - File Length
        long length = fm.readInt();
        FieldValidator.checkLength(length, arcSize);

        // 4 - File Extension
        byte[] extensionBytes = fm.readBytes(4);
        String fileExt = "";

        if (extensionBytes[0] != 0) {
          fileExt = "." + StringConverter.reverse(StringConverter.convertLittle(extensionBytes));
        }

        // 4 - Filename Length
        int filenameLength = fm.readInt();

        String filename = filenames[i];
        if (filenameLength <= 0) {
          filename += fileExt;
        }

        //path,id,name,offset,length,decompLength,exporter
        resources[i] = new Resource(path, filename, offset, length);

        TaskProgressManager.setValue(i);
      }

      /*
      // Go through and check out the WAV files
      for (int i = 0; i < numFiles; i++) {
        Resource resource = resources[i];
      
        if (resource.getExtension().equalsIgnoreCase("wav")) {
          long offset = resource.getOffset();
          fm.seek(offset);
      
          // 4 - MCMP Header
          if (fm.readString(4).equals("MCMP")) {
      
            // 1 - Unknown
            fm.skip(1);
      
            // 1 - Number of Extra Fields
            int numExtra = fm.readByte();
      
            // 12 - Unknown
            // X - Extra Data (num*9)
      
            int headerSize = 18 + (numExtra * 9);
      
            long length = resource.getLength();
            length -= headerSize;
            if (length > 0) {
              offset += headerSize;
      
              resource.setOffset(offset);
              resource.setLength(length);
              resource.setDecompressedLength(length);
            }
      
          }
      
        }
      }
      */

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

      long filenameDirLength = 0;
      for (int i = 0; i < numFiles; i++) {
        filenameDirLength += resources[i].getNameLength() + 1;
      }

      // Write Header Data

      // 4 - Header (LABN)
      // 4 - Flags
      // 4 - Number Of Files
      fm.writeBytes(src.readBytes(12));

      // 4 - Filename Directory Length
      fm.writeInt(filenameDirLength);
      src.skip(4);

      // 4 - null
      fm.writeBytes(src.readBytes(4));

      // Write Directory
      TaskProgressManager.setMessage(Language.get("Progress_WritingDirectory"));
      long offset = 20 + numFiles * 16 + filenameDirLength;
      for (int i = 0; i < numFiles; i++) {
        Resource fd = resources[i];
        long length = fd.getDecompressedLength();

        // 4 - File Offset
        fm.writeInt(offset);

        // 4 - File Length
        fm.writeInt(length);

        String filename = fd.getFilename();
        String fileExtension = StringConverter.reverse(FilenameSplitter.getExtension(filename));
        if (fileExtension.length() > 4) {
          fileExtension = fileExtension.substring(0, 4);
        }
        while (fileExtension.length() < 4) {
          fileExtension += " ";
        }

        // 4 - Type/Extension (or null)
        fm.writeString(fileExtension);

        // 4 - Filename Length (including null terminator)
        fm.writeInt(filename.length() + 1);

        offset += length;
      }

      // Write Filename Directory
      TaskProgressManager.setMessage(Language.get("Progress_WritingDirectory"));
      for (int i = 0; i < numFiles; i++) {
        Resource fd = resources[i];

        // X - Filename
        // 1 - null Filename Terminator
        fm.writeString(fd.getFilename());
        fm.writeByte(0);
      }

      // Write Files
      TaskProgressManager.setMessage(Language.get("Progress_WritingFiles"));
      write(resources, fm);

      src.close();
      fm.close();

    }
    catch (Throwable t) {
      logError(t);
    }
  }

}