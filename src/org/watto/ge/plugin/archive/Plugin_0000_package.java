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
import org.watto.task.TaskProgressManager;
import org.watto.datatype.Resource;
import org.watto.ge.helper.FieldValidator;
import org.watto.ge.plugin.ArchivePlugin;
import org.watto.io.FileManipulator;

/**
**********************************************************************************************

**********************************************************************************************
**/
public class Plugin_0000_package extends ArchivePlugin {

  /**
  **********************************************************************************************

  **********************************************************************************************
  **/
  public Plugin_0000_package() {

    super("0000_package", "0000_package");

    //         read write replace rename
    setProperties(true, false, true, true);

    setGames("80 Days",
        "Journey To The Center Of The Earth",
        "Adventures Of Sherlock Holmes: The Case Of The Silver Earring");
    setExtensions("0000");
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
      if (fm.readString(7).equals("package")) {
        rating += 50;
      }

      // Version
      if (fm.readInt() == 1) {
        rating += 5;
      }

      fm.skip(8);

      // Number Of Files
      if (FieldValidator.checkNumFiles(fm.readInt())) {
        rating += 5;
      }

      // Version
      if (fm.readInt() == 1) {
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

      // NOTE - Compressed file MUST know their DECOMPRESSED LENGTH
      //      - Uncompressed files MUST know their LENGTH

      addFileTypes();

      // RESETTING THE GLOBAL VARIABLES

      FileManipulator fm = new FileManipulator(path, false);

      // 7 - Header (package)
      // 4 - Version (1)
      // 8 - CRC?
      fm.skip(19);

      // 4 - Number Of Files?
      int numFiles = fm.readInt();
      FieldValidator.checkNumFiles(numFiles);

      // 1 - Directory Start Marker (1)
      // 4 - null
      fm.skip(5);

      long arcSize = (int) fm.getLength();

      Resource[] resources = new Resource[numFiles];

      TaskProgressManager.setMaximum(numFiles);

      // Loop through directory
      for (int i = 0; i < numFiles; i++) {
        // 4 - File Offset (relative to the end of the directory)
        long offset = fm.readInt();
        FieldValidator.checkOffset(offset, arcSize);

        // 4 - File Length
        long length = fm.readInt();
        FieldValidator.checkLength(length, arcSize);

        // 4 - Filename Length
        int filenameLength = fm.readInt();
        FieldValidator.checkFilenameLength(filenameLength);

        // X - Filename
        String filename = fm.readString(filenameLength);
        FieldValidator.checkFilename(filename);

        //path,id,name,offset,length,decompLength,exporter
        resources[i] = new Resource(path, filename, offset, length);

        TaskProgressManager.setValue(i);
      }

      // 4 - relOffset Extra Value
      int relExtra = 8 + fm.readInt();
      FieldValidator.checkLength(relExtra);
      fm.skip(relExtra);

      long relOffset = fm.getOffset();
      for (int i = 0; i < numFiles; i++) {
        resources[i].setOffset(resources[i].getOffset() + relOffset);
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

      // Write Header Data

      // 7 - Header (package)
      // 4 - Version (1)
      // 8 - CRC?
      // 4 - Number Of Files
      // 4 - Padding Size (1)
      // 1 - null Padding
      fm.writeBytes(src.readBytes(28));

      // Write Directory
      TaskProgressManager.setMessage(Language.get("Progress_WritingDirectory"));
      long offset = 0;
      for (int i = 0; i < numFiles; i++) {
        Resource fd = resources[i];
        long length = fd.getDecompressedLength();
        String filename = fd.getName();

        // 4 - File Offset (relative to the end of the directory)
        // 4 - File Length
        fm.writeInt((int) offset);
        fm.writeInt((int) length);
        src.skip(8);

        // 4 - Filename Length
        // X - Filename
        int fnLen = src.readInt();
        src.skip(fnLen);

        fm.writeInt(filename.length());
        fm.writeString(filename);

        offset += length;
      }

      // 4 - Padding Size (0-4)
      // X - null Padding
      src.skip(src.readInt());
      long padding = 4 - (fm.getOffset() % 4);
      if (padding == 4) {
        padding = 0;
      }
      fm.writeInt((int) padding);
      for (int i = 0; i < padding; i++) {
        fm.writeByte(0);
      }

      // 4 - Unknown
      fm.writeBytes(src.readBytes(4));

      // 4 - File Data Length
      long fileDataLength = offset;
      fm.writeInt((int) fileDataLength);

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
