/*
 * Application:  Game Extractor
 * Author:       wattostudios
 * Website:      http://www.watto.org
 * Copyright:    Copyright (c) 2002-2024 wattostudios
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
import org.watto.datatype.FileType;
import org.watto.datatype.Resource;
import org.watto.ge.helper.FieldValidator;
import org.watto.ge.plugin.ArchivePlugin;
import org.watto.io.FileManipulator;
import org.watto.io.converter.StringConverter;
import org.watto.task.TaskProgressManager;

/**
**********************************************************************************************

**********************************************************************************************
**/
public class Plugin_CA_BINARYARCHIVE00 extends ArchivePlugin {

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public Plugin_CA_BINARYARCHIVE00() {

    super("CA_BINARYARCHIVE00", "CA_BINARYARCHIVE00");

    //         read write replace rename
    setProperties(true, false, true, false);

    setGames("The Nations");
    setExtensions("ca"); // MUST BE LOWER CASE
    setPlatforms("PC");

    setTextPreviewExtensions("text"); // LOWER CASE

    setFileTypes(new FileType("anim", "Animation", FileType.TYPE_OTHER),
        new FileType("bmap", "Bitmap", FileType.TYPE_OTHER),
        new FileType("daba", "Database", FileType.TYPE_OTHER),
        new FileType("imag", "Image", FileType.TYPE_OTHER),
        new FileType("stab", "String Table", FileType.TYPE_OTHER),
        new FileType("test", "Text Document", FileType.TYPE_DOCUMENT),
        new FileType("wave", "WAV or MP3 Audio", FileType.TYPE_AUDIO));

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
      if (fm.readString(16).equals("binary.archive00")) {
        rating += 50;
      }

      if (fm.readLong() == 0) {
        rating += 5;
      }

      long arcSize = fm.getLength();

      // Directory Offset
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

      //ExporterPlugin exporter = Exporter_ZLib.getInstance();

      // RESETTING GLOBAL VARIABLES

      FileManipulator fm = new FileManipulator(path, false);

      long arcSize = fm.getLength();

      // 16 - Header (binary.archive00)
      // 8 - null
      fm.skip(24);

      // 4 - Directory Offset
      long dirOffset = fm.readInt();
      FieldValidator.checkOffset(dirOffset, arcSize);

      // 28 - null
      // 8 - Hash?
      fm.seek(dirOffset);

      int numFiles = (int) (arcSize - dirOffset) / 64;
      FieldValidator.checkNumFiles(numFiles);

      Resource[] resources = new Resource[numFiles];
      TaskProgressManager.setMaximum(numFiles);

      // Loop through directory
      for (int i = 0; i < numFiles; i++) {

        // 16 - Filename (null terminated, filled with nulls, may be truncated)
        String filename = fm.readNullString(16);
        FieldValidator.checkFilename(filename);

        // 4 - File Type (String, reversed - eg evaw, gami, ...)
        String fileType = StringConverter.reverse(fm.readString(4));

        // 4 - File Length
        long length = fm.readInt();
        FieldValidator.checkLength(length, arcSize);

        // 4 - File Offset
        long offset = fm.readInt();
        FieldValidator.checkOffset(offset, arcSize);

        // 20 - null
        // 8 - Hash?
        // 8 - null
        fm.skip(36);

        int dotPos = filename.lastIndexOf(".");
        if (dotPos > 0) {
          filename = filename.substring(0, dotPos);
        }
        filename += "." + fileType;

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

      long filesSize = 0;
      for (int i = 0; i < numFiles; i++) {
        filesSize += resources[i].getDecompressedLength();
      }
      long dirOffset = 64 + filesSize;

      // Write Header Data

      // 16 - Header (binary.archive00)
      // 8 - null
      fm.writeBytes(src.readBytes(24));

      // 4 - Directory Offset
      fm.writeInt(dirOffset);
      int srcDirOffset = src.readInt();

      // 28 - null
      // 8 - Hash?
      fm.writeBytes(src.readBytes(36));

      // Write Files
      TaskProgressManager.setMessage(Language.get("Progress_WritingFiles"));
      write(resources, fm);

      src.seek(srcDirOffset);

      // Write Directory
      TaskProgressManager.setMessage(Language.get("Progress_WritingDirectory"));
      long offset = 64;
      for (int i = 0; i < numFiles; i++) {
        Resource resource = resources[i];
        long length = resource.getDecompressedLength();

        // 16 - Filename (null terminated, filled with nulls, may be truncated)
        // 4 - File Type (String, reversed - eg evaw, gami, ...)
        fm.writeBytes(src.readBytes(20));

        // 4 - File Length
        fm.writeInt(length);
        src.skip(4);

        // 4 - File Offset
        fm.writeInt(offset);
        src.skip(4);

        // 20 - null
        // 8 - Hash?
        // 8 - null
        fm.writeBytes(src.readBytes(36));

        offset += length;
      }

      src.close();
      fm.close();

    }
    catch (Throwable t) {
      logError(t);
    }
  }

}
