/*
 * Application:  Game Extractor
 * Author:       wattostudios
 * Website:      http://www.watto.org
 * Copyright:    Copyright (c) 2002-2022 wattostudios
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
public class Plugin_DAT_DAT extends ArchivePlugin {

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public Plugin_DAT_DAT() {

    super("DAT_DAT", "DAT_DAT");

    //         read write replace rename
    setProperties(true, false, true, false);

    setGames("ESPN NHL Hockey");
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

      // Header
      String headerString = fm.readString(3);
      int headerByte = fm.readByte();
      if (headerString.equals("DAT") && headerByte == 0) {
        rating += 50;
      }

      fm.skip(4);

      // 4 - Padding Multiple? (16)
      if (fm.readInt() == 16) {
        rating += 5;
      }

      long arcSize = fm.getLength();

      // 4 - File Data Offset
      if (FieldValidator.checkOffset(fm.readInt(), arcSize)) {
        rating += 5;
      }

      fm.skip(16);

      // Number Of Files
      if (FieldValidator.checkNumFiles(fm.readInt())) {
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

      // 4 - Header ("DAT" + null)
      // 4 - File Data Offset [+30]
      // 4 - Padding Multiple? (16)
      fm.skip(12);

      // 4 - File Data Offset
      int dataOffset = fm.readInt();
      FieldValidator.checkOffset(dataOffset, arcSize);

      // 8 - null
      // 4 - Unknown (25)
      // 4 - null
      fm.skip(16);

      // 4 - Number of Files
      int numFiles = fm.readInt();
      FieldValidator.checkNumFiles(numFiles);

      // 4 - Unknown
      // 4 - Hash?
      // 4 - null
      fm.skip(12);

      Resource[] resources = new Resource[numFiles];
      TaskProgressManager.setMaximum(numFiles);

      String[] names = new String[numFiles];

      fm.skip(numFiles * 24);

      // 1 - Root Filename ("/")
      // 1 - null Terminator
      fm.readNullString();

      // Loop through directory
      for (int i = 0; i < numFiles; i++) {
        // X - Filename
        // 1 - null Filename Terminator
        String filename = fm.readNullString();
        FieldValidator.checkFilename(filename);

        names[i] = filename;
        TaskProgressManager.setValue(i);
      }

      fm.relativeSeek(48);

      // Loop through directory
      for (int i = 0; i < numFiles; i++) {
        // 8 - File Data Offset (relative to the start of the File Data)
        long offset = fm.readLong() + dataOffset;
        FieldValidator.checkOffset(offset, arcSize);

        // 4 - File Length
        int length = fm.readInt();
        FieldValidator.checkLength(length, arcSize);

        // 4 - Unknown (somehow related to the filename directory?)
        // 4 - Hash?
        // 4 - null
        fm.skip(12);

        String filename = names[i];

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

      long dataOffset = 48 + (numFiles * 24) + 2; // +2 for the root directory name
      for (int i = 0; i < numFiles; i++) {
        dataOffset += resources[i].getNameLength() + 1;
      }
      // add the padding
      int dirPadding = calculatePadding(dataOffset, 16);
      dataOffset += dirPadding;

      // Write Header Data

      // 4 - Header ("DAT" + null)
      fm.writeBytes(src.readBytes(4));

      // 4 - File Data Offset [+30]
      fm.writeInt(dataOffset - 30);
      src.skip(4);

      // 4 - Padding Multiple? (16)
      fm.writeBytes(src.readBytes(4));

      // 4 - File Data Offset
      fm.writeInt(dataOffset);
      src.skip(4);

      // 8 - null
      // 4 - Unknown (25)
      // 4 - null
      // 4 - Number of Files
      // 4 - Unknown
      // 4 - Hash?
      // 4 - null
      fm.writeBytes(src.readBytes(32));

      // Write Directory
      TaskProgressManager.setMessage(Language.get("Progress_WritingDirectory"));
      long offset = 0;
      for (int i = 0; i < numFiles; i++) {
        Resource fd = resources[i];
        long length = fd.getDecompressedLength();

        // 8 - File Data Offset (relative to the start of the File Data)
        fm.writeLong(offset);

        // 4 - File Length
        fm.writeInt(length);

        src.skip(12);

        // 4 - Unknown (somehow related to the filename directory?)
        // 4 - Hash?
        // 4 - null
        fm.writeBytes(src.readBytes(12));

        offset += length;
        offset += calculatePadding(length, 16);
      }

      // 1 - Root Filename ("/")
      fm.writeString("/");

      // 1 - null Terminator
      fm.writeByte(0);

      // Write Directory
      for (int i = 0; i < numFiles; i++) {
        Resource fd = resources[i];

        // X - Filename
        fm.writeString(fd.getName());

        // 1 - null Filename Terminator
        fm.writeByte(0);

      }

      // 0-15 - null Padding to a multiple of 16 bytes
      for (int p = 0; p < dirPadding; p++) {
        fm.writeByte(0);
      }

      // Write Files
      TaskProgressManager.setMessage(Language.get("Progress_WritingFiles"));
      for (int i = 0; i < numFiles; i++) {
        Resource resource = resources[i];

        write(resource, fm);
        TaskProgressManager.setValue(i);

        int paddingSize = calculatePadding(resource.getDecompressedLength(), 16);

        // 0-15 - null Padding to a multiple of 16 bytes
        for (int p = 0; p < paddingSize; p++) {
          fm.writeByte(0);
        }

      }

      src.close();
      fm.close();

    }
    catch (Throwable t) {
      logError(t);
    }
  }

}
