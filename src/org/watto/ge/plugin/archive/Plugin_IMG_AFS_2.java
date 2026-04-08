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

import org.watto.datatype.Resource;
import org.watto.ge.helper.FieldValidator;
import org.watto.ge.plugin.ArchivePlugin;
import org.watto.io.FileManipulator;
import org.watto.task.TaskProgressManager;

/**
**********************************************************************************************

**********************************************************************************************
**/
public class Plugin_IMG_AFS_2 extends ArchivePlugin {

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public Plugin_IMG_AFS_2() {

    super("IMG_AFS_2", "IMG_AFS_2");

    //         read write replace rename
    setProperties(true, false, false, false);

    setGames("Bleach: Erabareshi Tamashii");
    setExtensions("img", "pck"); // MUST BE LOWER CASE
    setPlatforms("PS2");

    // MUST BE LOWER CASE !!!
    //setFileTypes(new FileType("txt", "Text Document", FileType.TYPE_DOCUMENT),
    //             new FileType("bmp", "Bitmap Image", FileType.TYPE_IMAGE)
    //             );

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
      String headerString = fm.readString(3);
      int headerByte = fm.readByte();
      if (headerString.equals("AFS") && headerByte == 0) {
        rating += 50;
      }
      else {
        rating -= 10;
      }

      // Number Of Files
      if (FieldValidator.checkNumFiles(fm.readInt())) {
        rating += 5;
      }

      fm.skip(4);

      // 4 - First File Length
      if (FieldValidator.checkLength(fm.readInt())) {
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
      //ExporterPlugin exporterDefault = Exporter_Default.getInstance();

      // RESETTING GLOBAL VARIABLES

      FileManipulator fm = new FileManipulator(path, false);

      long arcSize = fm.getLength();

      // 4 - Header ("AFS" + null)
      fm.skip(4);

      // 4 - Number Of Files
      int numFiles = fm.readInt();
      FieldValidator.checkNumFiles(numFiles);

      // Skip to the directory file and read the filenames
      fm.skip(numFiles * 8);

      // 4 - Directory Offset
      int dirOffset = fm.readInt();
      FieldValidator.checkOffset(dirOffset, arcSize);

      // 4 - Directory Length
      int dirLength = fm.readInt();
      FieldValidator.checkLength(dirLength, arcSize);

      int numDirEntries = dirLength / 48;
      FieldValidator.checkNumFiles(numDirEntries);

      fm.seek(dirOffset);

      /*
      Hashtable<Long, String> nameMap = new Hashtable<Long, String>(numDirEntries);
      
      // filename directory
      for (int i = 0; i < numDirEntries; i++) {
        // 32 - Filename (terminated by byte 32, then filled with nulls)
        String filename = fm.readNullString(32).trim();
        FieldValidator.checkFilename(filename);
      
        // 4 - Unknown
        // 4 - Unknown
        // 4 - Unknown
        fm.skip(12);
      
        // 4 - File Offset
        long offset = IntConverter.unsign(fm.readInt());
        FieldValidator.checkOffset(offset, arcSize + 1);
      
        nameMap.put(offset, filename);
      }
      */

      String[] names = new String[numDirEntries];
      for (int i = 0; i < numDirEntries; i++) {
        // 32 - Filename (terminated by byte 32, then filled with nulls)
        String filename = fm.readNullString(32).trim();
        FieldValidator.checkFilename(filename);

        // 4 - Unknown
        // 4 - Unknown
        // 4 - Unknown
        // 4 - File Offset / File Length
        fm.skip(16);

        names[i] = filename;
      }

      // go back and read the file entries
      fm.seek(8);

      Resource[] resources = new Resource[numFiles];
      TaskProgressManager.setMaximum(numFiles);

      // Loop through directory
      for (int i = 0; i < numFiles; i++) {

        // 4 - File Offset
        int offset = fm.readInt();
        FieldValidator.checkOffset(offset, arcSize + 1);

        // 4 - File Length
        int length = fm.readInt();
        FieldValidator.checkLength(length, arcSize);

        /*
        String filename = nameMap.get((long) offset);
        if (filename == null) {
          filename = Resource.generateFilename(i);
        }
        */
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

}
