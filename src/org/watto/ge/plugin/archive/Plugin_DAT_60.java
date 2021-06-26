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
import org.watto.ErrorLogger;
import org.watto.component.WSPluginManager;
import org.watto.datatype.Resource;
import org.watto.ge.helper.FieldValidator;
import org.watto.ge.plugin.ArchivePlugin;
import org.watto.ge.plugin.ViewerPlugin;
import org.watto.io.FileManipulator;
import org.watto.io.converter.ByteConverter;
import org.watto.task.TaskProgressManager;

/**
**********************************************************************************************

**********************************************************************************************
**/
public class Plugin_DAT_60 extends ArchivePlugin {

  /**
  **********************************************************************************************

  **********************************************************************************************
  **/
  public Plugin_DAT_60() {

    super("DAT_60", "DAT_60");

    //         read write replace rename
    setProperties(true, false, false, false);

    setGames("7 Wonders: Ancient Alien Makeover",
        "7 Wonders: Magical Mystery Tour",
        "Midnight Mysteries: Witches of Abraham");
    setExtensions("dat"); // MUST BE LOWER CASE
    setPlatforms("PC");

    // MUST BE LOWER CASE !!!
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
      byte[] headerBytes = fm.readBytes(4);
      if (ByteConverter.unsign(headerBytes[0]) == 206 && ByteConverter.unsign(headerBytes[1]) == 222 && ByteConverter.unsign(headerBytes[2]) == 237 && ByteConverter.unsign(headerBytes[3]) == 236) {
        rating += 50;
      }

      long arcSize = fm.getLength();

      // version (1)
      if (fm.readInt() == 1) {
        rating += 5;
      }

      // Directory Offset
      if (FieldValidator.checkOffset(fm.readInt(), arcSize)) {
        rating += 5;
      }

      return rating;

    }
    catch (

    Throwable t) {
      return 0;
    }
  }

  /**
   **********************************************************************************************
   Provide hints to the previewer so that certain document types are displayed appropriately
   **********************************************************************************************
   **/
  @Override
  public ViewerPlugin previewHint(Resource resource) {
    String extension = resource.getExtension();
    if (extension.equalsIgnoreCase("lua") || extension.equalsIgnoreCase("attributes") || extension.equalsIgnoreCase("fnt") || extension.equalsIgnoreCase("scene") || extension.equalsIgnoreCase("skin")) {
      return (ViewerPlugin) WSPluginManager.getPlugin("Viewer", "TXT");
    }
    return null;
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

      // 4 - Header? ((bytes)206,222,237,236)
      // 4 - Version (1)
      fm.skip(8);

      // 4 - Directory Offset
      int dirOffset = fm.readInt();
      FieldValidator.checkOffset(dirOffset, arcSize);

      // 12 - null
      fm.seek(dirOffset);

      // 4 - Number of Files and Folders
      int numFiles = fm.readInt();
      FieldValidator.checkNumFiles(numFiles);

      long relNameOffset = fm.getOffset() + (numFiles * 24) + 4;

      int realNumFiles = 0;

      TaskProgressManager.setMaximum(numFiles);

      // Loop through directory
      long[] offsets = new long[numFiles];
      long[] lengths = new long[numFiles];
      boolean[] dirEntries = new boolean[numFiles];
      long[] filenameOffsets = new long[numFiles];
      try {
        for (int i = 0; i < numFiles; i++) {

          // 8 - File Offset
          long offset = fm.readLong();
          FieldValidator.checkOffset(offset, arcSize);
          offsets[i] = offset;

          // 8 - File Length
          long length = fm.readLong();
          FieldValidator.checkLength(length, arcSize);
          lengths[i] = length;

          // 4 - Entry Type Flag (0=File, 1=Folder)
          int entryType = fm.readInt();
          if (entryType == 1) {
            dirEntries[i] = true;
          }
          else if (entryType == 0) {
            dirEntries[i] = false;
            realNumFiles++;
          }
          else {
            ErrorLogger.log("[DAT_60]: Unknown Entry Type: " + entryType);
            return null;
          }

          // 4 - Filename Offset (relative to the start of the filename directory) [+8]
          long filenameOffset = fm.readInt() + relNameOffset;
          FieldValidator.checkOffset(filenameOffset, arcSize);
          filenameOffsets[i] = filenameOffset;
          TaskProgressManager.setValue(i);
        }
      }
      catch (Throwable t) {
        fm.seek(dirOffset + 4);

        relNameOffset = fm.getOffset() + (numFiles * 16) + 4;

        for (int i = 0; i < numFiles; i++) {
          // 4 - Entry Type Flag (0=File, 1=Folder)
          int entryType = fm.readInt();
          if (entryType == 1) {
            dirEntries[i] = true;
          }
          else if (entryType == 0) {
            dirEntries[i] = false;
            realNumFiles++;
          }
          else {
            ErrorLogger.log("[DAT_60]: Unknown Entry Type: " + entryType);
            return null;
          }

          // 4 - File Offset
          int offset = fm.readInt();
          FieldValidator.checkOffset(offset, arcSize);
          offsets[i] = offset;

          // 4 - File Length
          int length = fm.readInt();
          FieldValidator.checkLength(length, arcSize);
          lengths[i] = length;

          // 4 - Filename Offset (relative to the start of the filename directory) [+8]
          long filenameOffset = fm.readInt() + relNameOffset;
          FieldValidator.checkOffset(filenameOffset, arcSize);
          filenameOffsets[i] = filenameOffset;
          TaskProgressManager.setValue(i);
        }
      }

      // Now get all the names
      String[] names = new String[numFiles];
      for (int i = 0; i < numFiles; i++) {
        fm.seek(filenameOffsets[i]);

        // X - Filename (null)
        String filename = fm.readNullString();
        FieldValidator.checkFilename(filename);
        names[i] = filename;
      }

      // Now go and create all the file entries
      Resource[] resources = new Resource[realNumFiles];
      realNumFiles = 0;
      String dirName = "";
      for (int i = 0; i < numFiles; i++) {
        if (dirEntries[i]) {
          // a directory
          dirName = names[i] + "\\";
        }
        else {
          long offset = offsets[i];
          long length = lengths[i];
          String filename = dirName + names[i];

          //path,name,offset,length,decompLength,exporter
          resources[realNumFiles] = new Resource(path, filename, offset, length);
          realNumFiles++;
        }

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

}
