/*
 * Application:  Game Extractor
 * Author:       wattostudios
 * Website:      http://www.watto.org
 * Copyright:    Copyright (c) 2002-2026 wattostudios
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
public class Plugin_COD_UNIQUE extends ArchivePlugin {

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public Plugin_COD_UNIQUE() {

    super("COD_UNIQUE", "COD_UNIQUE");

    //         read write replace rename
    setProperties(true, true, true, true);

    setGames("Screamer 4x4");
    setExtensions("cod");
    setPlatforms("PC");

    // MUST BE LOWER CASE !!!
    setFileTypes(new FileType("32", "32-Bit Texture Image", FileType.TYPE_IMAGE),
        new FileType("32a", "32-Bit Texture Image", FileType.TYPE_IMAGE),
        new FileType("tex", "Texture Image", FileType.TYPE_IMAGE));

    //setTextPreviewExtensions("colours", "rat", "screen", "styles"); // LOWER CASE

    //setCanScanForFileTypes(true);

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
      if (fm.readString(21).equals("->Unique Pc HUNGARY<-")) {
        rating += 50;
      }

      // null
      if (fm.readByte() == 0) {
        rating += 5;
      }

      // null
      if (fm.readByte() == 0) {
        rating += 5;
      }

      // null
      if (fm.readByte() == 0) {
        rating += 5;
      }

      // First File Offset
      if (FieldValidator.checkOffset(fm.readInt() + 28)) {
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

      // 21 - Header (->Unique Pc HUNGARY<-)
      // 3 - null
      fm.skip(24);

      // 4 - Directory Length
      int numFiles = fm.readInt() / 136;
      FieldValidator.checkNumFiles(numFiles);

      long arcSize = fm.getLength();

      Resource[] resources = new Resource[numFiles];
      TaskProgressManager.setMaximum(numFiles);

      // Loop through directory
      for (int i = 0; i < numFiles; i++) {
        // 128 - Filename (null terminated, then filled with "." for the remaining bytes)
        String filename = fm.readNullString(128);

        // 4 - Offset
        long offset = fm.readInt();
        FieldValidator.checkOffset(offset, arcSize);

        // 4 - Length
        long length = fm.readInt();
        FieldValidator.checkLength(length, arcSize);

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
  
  **********************************************************************************************
  **/
  @Override
  public void write(Resource[] resources, File path) {
    try {

      FileManipulator fm = new FileManipulator(path, true);
      int numFiles = resources.length;
      TaskProgressManager.setMaximum(numFiles);

      // Write Header Data

      // 21 - Header (->Unique Pc HUNGARY<-)
      fm.writeString("->Unique Pc HUNGARY<-");

      // 3 - null
      fm.writeByte(0);
      fm.writeByte(0);
      fm.writeByte(0);

      // 4 - Directory Length
      fm.writeInt(numFiles * 136);

      // Write Directory
      TaskProgressManager.setMessage(Language.get("Progress_WritingDirectory"));
      long offset = 28 + (numFiles * 136);
      for (int i = 0; i < numFiles; i++) {
        // 128 - Filename (null terminated, then filled with "." for the remaining bytes)
        String filename = resources[i].getName();
        if (filename.length() > 127) {
          filename = filename.substring(0, 127);
        }
        fm.writeString(filename);
        fm.writeByte(0);

        int paddingSize = 127 - filename.length();
        for (int j = 0; j < paddingSize; j++) {
          fm.writeString(".");
        }

        // 4 - Offset
        fm.writeInt((int) offset);

        long length = resources[i].getDecompressedLength();

        // 4 - Length
        fm.writeInt((int) length);

        offset += length;
      }

      // Write Files
      TaskProgressManager.setMessage(Language.get("Progress_WritingFiles"));
      write(resources, fm);

      fm.close();

    }
    catch (Throwable t) {
      logError(t);
    }
  }

}
