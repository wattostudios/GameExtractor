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
public class Plugin_RES_0TSR_4 extends ArchivePlugin {

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public Plugin_RES_0TSR_4() {

    super("RES_0TSR_4", "RES_0TSR_4");

    //         read write replace rename
    setProperties(true, false, true, false);

    setGames("Test Drive: Eve of Destruction");
    setExtensions("res", "car", "trk"); // MUST BE LOWER CASE
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
      if (fm.readString(4).equals("0TSR")) {
        rating += 50;
      }

      // 4 - Header Length (not including padding) (40)
      if (fm.readInt() == 40) {
        rating += 5;
      }

      // 4 - Version? (6)
      if (fm.readInt() == 6) {
        rating += 5;
      }

      long arcSize = fm.getLength();

      // Archive Size
      if (FieldValidator.checkEquals(fm.readInt(), arcSize)) {
        rating += 5;
      }

      fm.skip(4);

      // 4 - Padding Multiple (128)
      if (fm.readInt() == 128) {
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

      // 4 - Header (0TSR) // Note the zero, not the letter "O"
      // 4 - Header Length (not including padding) (40)
      // 4 - Version? (6)
      // 4 - Archive Length
      // 4 - Hash? (maybe a date stamp?)
      // 4 - Padding Multiple (128)
      fm.skip(24);

      // 4 - Number Of Files
      int numFiles = fm.readInt();
      FieldValidator.checkNumFiles(numFiles);

      // 4 - Unknown
      // 4 - null
      // 4 - Unknown
      // 88 - null Padding to offset 128
      fm.relativeSeek(128);

      Resource[] resources = new Resource[numFiles];
      TaskProgressManager.setMaximum(numFiles);

      // Loop through directory
      for (int i = 0; i < numFiles; i++) {
        // 32 - Filename (null terminated, filled with nulls)
        String filename = fm.readNullString(32);
        FieldValidator.checkFilename(filename);

        // 4 - Type Code (reversed) ("TLAV", " XET", etc)
        // 4 - Type ID? (9="LDOM", 11="TLAV", 10=" XET", etc)
        fm.skip(8);

        // 4 - File Length
        long length = fm.readInt();
        FieldValidator.checkLength(length, arcSize);

        // 4 - File Offset (relative to the start of the Details Directory)
        long offset = fm.readInt() + 128;
        FieldValidator.checkOffset(offset, arcSize);

        // 4 - Hash?
        // 16 - null
        fm.skip(20);

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

      long headerSize = 128;

      long directorySize = numFiles * 68;
      directorySize += calculatePadding(directorySize, 128);

      long filesSize = 0;
      for (int i = 0; i < numFiles; i++) {
        long length = resources[i].getDecompressedLength();
        filesSize += length + calculatePadding(length, 128);
      }
      long archiveSize = headerSize + directorySize + filesSize;

      // Write Header Data

      // 4 - Header (0TSR) // Note the zero, not the letter "O"
      // 4 - Header Length (not including padding) (40)
      // 4 - Version? (6)
      fm.writeBytes(src.readBytes(12));

      // 4 - Archive Length
      fm.writeInt(archiveSize);
      src.skip(4);

      // 4 - Hash? (maybe a date stamp?)
      // 4 - Padding Multiple (128)
      // 4 - Number Of Files
      // 4 - Unknown
      // 4 - null
      // 4 - Unknown
      // 88 - null Padding to offset 128
      fm.writeBytes(src.readBytes(112));

      // Write Directory
      TaskProgressManager.setMessage(Language.get("Progress_WritingDirectory"));
      long offset = directorySize;
      for (int i = 0; i < numFiles; i++) {
        Resource resource = resources[i];
        long length = resource.getDecompressedLength();

        // 32 - Filename (null terminated, filled with nulls)
        // 4 - Type Code (reversed) ("TLAV", " XET", etc)
        // 4 - Type ID? (9="LDOM", 11="TLAV", 10=" XET", etc)
        fm.writeBytes(src.readBytes(40));

        // 4 - File Length
        fm.writeInt(length);
        src.skip(4);

        // 4 - File Offset (relative to the start of the Details Directory)
        fm.writeInt(offset);
        src.skip(4);

        // 4 - Hash?
        // 16 - null
        fm.writeBytes(src.readBytes(20));

        int paddingSize = calculatePadding(length, 128);

        offset += length + paddingSize;
      }

      int dirPaddingSize = calculatePadding(numFiles * 68, 128);
      fm.writeBytes(src.readBytes(dirPaddingSize));

      // Write Files
      TaskProgressManager.setMessage(Language.get("Progress_WritingFiles"));
      for (int i = 0; i < resources.length; i++) {
        Resource resource = resources[i];

        // X - File Data
        write(resource, fm);

        // 0-127 - null Padding to a multiple of 128 bytes
        long length = resource.getDecompressedLength();
        int paddingSize = calculatePadding(length, 128);
        for (int p = 0; p < paddingSize; p++) {
          fm.writeByte(0);
        }

        TaskProgressManager.setValue(i);
      }

      src.close();
      fm.close();

    }
    catch (Throwable t) {
      logError(t);
    }
  }

}
