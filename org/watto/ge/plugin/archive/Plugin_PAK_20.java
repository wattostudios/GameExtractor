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
import org.watto.datatype.Resource;
import org.watto.ge.helper.FieldValidator;
import org.watto.ge.plugin.ArchivePlugin;
import org.watto.ge.plugin.ExporterPlugin;
import org.watto.ge.plugin.exporter.Exporter_Custom_PAK_20;
import org.watto.io.FileManipulator;
import org.watto.task.TaskProgressManager;

/**
**********************************************************************************************

**********************************************************************************************
**/
public class Plugin_PAK_20 extends ArchivePlugin {

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public Plugin_PAK_20() {

    super("PAK_20", "PAK_20");

    //         read write replace rename
    setProperties(true, false, false, false);

    setGames("The Movies");
    setExtensions("pak");
    setPlatforms("PC");

    setTextPreviewExtensions("csv", "in", "lst", "rob"); // LOWER CASE

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

      // Version (5)
      if (fm.readInt() == 5) {
        rating += 5;
      }

      long arcSize = fm.getLength();

      // First File Offset
      if (FieldValidator.checkOffset(fm.readInt(), arcSize)) {
        rating += 5;
      }

      // null
      if (fm.readInt() == 0) {
        rating += 5;
      }

      // Number Of Files
      if (FieldValidator.checkNumFiles(fm.readInt())) {
        rating += 5;
      }

      fm.skip(24);

      // Directory Offset (52)
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
      ExporterPlugin exporter = Exporter_Custom_PAK_20.getInstance();

      // RESETTING THE GLOBAL VARIABLES

      FileManipulator fm = new FileManipulator(path, false);

      long arcSize = (int) fm.getLength();

      // 4 - Version (5)
      // 4 - First File Offset
      // 4 - null
      fm.skip(12);

      // 4 - Number Of Files
      int numFiles = fm.readInt();
      FieldValidator.checkNumFiles(numFiles);

      // 4 - Number Of Offset Pairs (256)
      fm.skip(4);

      // 4 - Number Of Folder Names (43)
      int numNames = fm.readInt();
      FieldValidator.checkNumFiles(numNames);

      // 4 - Unknown (752)
      // 12 - null
      fm.skip(16);

      // 4 - File Directory Offset (52)
      long dirOffset = fm.readInt();
      FieldValidator.checkOffset(dirOffset, arcSize);

      // 4 - Folder Directory Offset
      fm.skip(4);

      // 4 - Folder Names Directory Offset
      int namesDirOffset = fm.readInt();
      FieldValidator.checkOffset(namesDirOffset, arcSize);

      java.util.Hashtable<String, String> dirNames = new java.util.Hashtable<String, String>(numNames);
      fm.seek(namesDirOffset);

      int nameOffset = 0;
      for (int i = 0; i < numNames; i++) {
        // X - Directory Name (null)
        String name = fm.readNullString();
        //FieldValidator.checkFilename(name);

        dirNames.put("" + nameOffset, name);
        nameOffset += name.length() + 1;
      }

      fm.seek(dirOffset);

      Resource[] resources = new Resource[numFiles];

      TaskProgressManager.setMaximum(numFiles);

      // Loop through directory
      for (int i = 0; i < numFiles; i++) {
        // 4 - Unknown
        // 4 - Hash?
        fm.skip(8);

        // 4 - File Offset
        long offset = fm.readInt();
        FieldValidator.checkOffset(offset, arcSize);

        // 4 - Compressed File Length (including file data header fields and null padding)
        long length = fm.readInt();
        FieldValidator.checkLength(length, arcSize);

        // 4 - Decompressed File Length
        int decompLength = fm.readInt();
        FieldValidator.checkLength(decompLength);

        // 4 - Hash?
        int dirNameOffset = ((fm.readInt() & 32767) >> 1);
        //FieldValidator.checkOffset(dirNameOffset);
        //System.out.println(dirNameOffset);
        Object dirName = dirNames.get("" + dirNameOffset);

        // 32 - Filename (null terminated)
        String filename = fm.readNullString(32);
        FieldValidator.checkFilename(filename);

        if (dirName == null) {
        }
        else {
          filename = ((String) dirName) + filename;
        }

        if (decompLength != length) {
          //path,id,name,offset,length,decompLength,exporter
          resources[i] = new Resource(path, filename, offset, length, decompLength, exporter);
        }
        else {
          //path,id,name,offset,length,decompLength,exporter
          resources[i] = new Resource(path, filename, offset, length, decompLength);
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
