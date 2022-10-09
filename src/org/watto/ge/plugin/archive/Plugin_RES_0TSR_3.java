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
public class Plugin_RES_0TSR_3 extends ArchivePlugin {

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public Plugin_RES_0TSR_3() {

    super("RES_0TSR_3", "RES_0TSR_3");

    //         read write replace rename
    setProperties(true, false, true, false);

    setGames("Dirt To Daytona");
    setExtensions("res"); // MUST BE LOWER CASE
    setPlatforms("PS2");

    setFileTypes(new FileType("tex", "TEX Image", FileType.TYPE_IMAGE),
        new FileType("img", "IMG Image", FileType.TYPE_IMAGE),
        new FileType("val", "Values Document", FileType.TYPE_DOCUMENT),
        new FileType("lyt", "Layout Document", FileType.TYPE_OTHER));

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

      if (fm.readInt() == 2) {
        rating += 5;
      }

      if (fm.readInt() == 24) {
        rating += 5;
      }

      long arcSize = fm.getLength();

      // Archive Size
      if (FieldValidator.checkEquals(fm.readInt(), arcSize)) {
        rating += 5;
      }

      fm.skip(4);

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

      //FileManipulator tempOut = new FileManipulator(new File("C:\\temp.out_null.txt"), true);
      //tempOut.writeBytes(fm.readBytes((int) arcSize - 4));
      //tempOut.writeInt(0);
      //tempOut.close();

      fm.relativeSeek(0);

      // 4 - Header (0TSR) // Note the zero, not the letter "O"
      // 4 - Version? (2)
      // 4 - Header Length (24)
      // 4 - Archive Length
      // 4 - Hash?
      fm.skip(20);

      // 4 - Number Of Files
      int numFiles = fm.readInt();
      FieldValidator.checkNumFiles(numFiles);

      Resource[] resources = new Resource[numFiles];
      TaskProgressManager.setMaximum(numFiles);

      // Loop through directory
      long offset = (numFiles * 56) + 4;
      offset += calculatePadding(offset, 128);
      offset += 20;

      for (int i = 0; i < numFiles; i++) {
        // 36 - Filename (null terminated, filled with nulls)
        String filename = fm.readNullString(36);
        FieldValidator.checkFilename(filename);

        // 4 - Type Code (reversed) ("TLAV", " XET", etc)
        // 4 - Type ID? (5="TYAL", 6="TLAV", 9=" XET", etc)
        fm.skip(8);

        // 4 - File Length [+4]
        int length = fm.readInt() + 8;
        FieldValidator.checkLength(length, arcSize);

        // 8 - null
        fm.skip(8);

        //path,name,offset,length,decompLength,exporter
        resources[i] = new Resource(path, filename, offset, length);

        TaskProgressManager.setValue(i);

        offset += length;
        offset += calculatePadding(length, 128);
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

      long dirSize = numFiles * 56;
      int dirPadding = calculatePadding(dirSize + 4, 128);
      dirSize += dirPadding;

      long filesSize = 0;
      for (int i = 0; i < numFiles; i++) {
        Resource resource = resources[i];
        if (resource.isReplaced()) {
          int length = (int) resource.getDecompressedLength();
          length += calculatePadding(length + 4, 128);
          filesSize += (length + 4);
        }
        else {
          int length = (int) resource.getDecompressedLength();
          length += calculatePadding(length, 128);
          filesSize += length;
        }
      }

      long archiveSize = 20 + filesSize + dirSize + 8;

      // Write Header Data

      // 4 - Header (0TSR) // Note the zero, not the letter "O"
      // 4 - Version? (2)
      // 4 - Header Length (24)
      fm.writeBytes(src.readBytes(12));

      // 4 - Archive Length
      fm.writeInt(archiveSize);
      src.skip(4);

      // 4 - Hash?
      // 4 - Number Of Files
      fm.writeBytes(src.readBytes(8));

      // Write Directory
      TaskProgressManager.setMessage(Language.get("Progress_WritingDirectory"));
      for (int i = 0; i < numFiles; i++) {
        Resource resource = resources[i];
        long length = resource.getDecompressedLength();
        if (resource.isReplaced()) {
          length -= 4;
        }
        else {
          length -= 8;
        }

        // 36 - Filename (null terminated, filled with nulls)
        fm.writeNullString(resource.getName(), 36);
        src.skip(36);

        // 4 - Type Code (reversed) ("TLAV", " XET", etc)
        // 4 - Type ID? (5="TYAL", 6="TLAV", 9=" XET", etc)
        fm.writeBytes(src.readBytes(8));

        // 4 - File Length [+4]
        fm.writeInt(length);
        src.skip(4);

        // 8 - null
        fm.writeBytes(src.readBytes(8));
      }

      // 0-127 - null Padding so the (DetailsDirectory+4) is a multiple of 128 bytes
      for (int p = 0; p < dirPadding; p++) {
        fm.writeByte(0);
      }

      // Write Files
      TaskProgressManager.setMessage(Language.get("Progress_WritingFiles"));
      for (int i = 0; i < resources.length; i++) {
        Resource resource = resources[i];
        if (resource.isReplaced()) {
          // X - File Data
          write(resource, fm);

          // 4 - null Padding (not on the last file)
          if (i != numFiles - 1) {
            fm.writeInt(0);
          }

          // 0-127 - null Padding so the (FileData+4) is a multiple of 128 bytes
          int length = (int) resource.getDecompressedLength();
          int padding = calculatePadding(length + 4, 128);
          for (int p = 0; p < padding; p++) {
            fm.writeByte(0);
          }
        }
        else {
          // X - File Data (INCLUDES 4-byte NULL PADDING)
          int length = (int) resource.getDecompressedLength();

          if (i == numFiles - 1) { // last file doesn't have the 4-byte null at the end
            resource.setDecompressedLength(length - 4);
            resource.setLength(length - 4);
          }

          write(resource, fm);

          if (i == numFiles - 1) { // last file doesn't have the 4-byte null at the end
            resource.setDecompressedLength(length);
            resource.setLength(length);
          }

          // 0-127 - null Padding so the (FileData+4) is a multiple of 128 bytes

          int padding = calculatePadding(length, 128);
          for (int p = 0; p < padding; p++) {
            fm.writeByte(0);
          }
        }
        TaskProgressManager.setValue(i);
      }

      // FOOTER
      src.seek(src.getLength() - 4);

      // 4 - Footer (0TSR)
      fm.writeString("0TSR");

      // 4 - Hash?
      fm.writeBytes(src.readBytes(4));

      src.close();
      fm.close();

    }
    catch (Throwable t) {
      logError(t);
    }
  }

}
