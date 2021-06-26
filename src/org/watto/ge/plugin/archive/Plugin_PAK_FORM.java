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
import org.watto.datatype.Archive;
import org.watto.datatype.Resource;
import org.watto.ge.helper.FieldValidator;
import org.watto.ge.plugin.ArchivePlugin;
import org.watto.ge.plugin.ExporterPlugin;
import org.watto.ge.plugin.exporter.Exporter_ZLib;
import org.watto.io.FileManipulator;
import org.watto.io.converter.ByteConverter;
import org.watto.io.converter.IntConverter;
import org.watto.task.TaskProgressManager;

/**
**********************************************************************************************

**********************************************************************************************
**/
public class Plugin_PAK_FORM extends ArchivePlugin {

  int realNumFiles = 0;

  /**
  **********************************************************************************************

  **********************************************************************************************
  **/
  public Plugin_PAK_FORM() {

    super("PAK_FORM", "PAK_FORM");

    //         read write replace rename
    setProperties(true, false, false, false);

    setGames("Project Lucie");
    setExtensions("pak"); // MUST BE LOWER CASE
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
      if (fm.readString(4).equals("FORM")) {
        rating += 50;
      }

      long arcSize = fm.getLength();

      // Archive Size
      if (FieldValidator.checkEquals(IntConverter.changeFormat(fm.readInt()) + 8, arcSize)) {
        rating += 5;
      }

      // Header
      if (fm.readString(8).equals("PAC1HEAD")) {
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
      realNumFiles = 0;

      FileManipulator fm = new FileManipulator(path, false);

      long arcSize = fm.getLength();

      // 4 - Header (FORM)
      // 4 - Archive Length [+8]
      // 8 - Header 2 (PAC1HEAD)
      // 4 - Header 2 Length (35)
      // 4 - Unknown (256)
      // 12 - Hash?
      // 4 - Unknown
      // 4 - Unknown
      // 4 - null
      // 7 - Unknown
      // 4 - Header (DATA)
      fm.skip(59);

      // 4 - File Data Length (not including these 2 fields)
      int dataLength = IntConverter.changeFormat(fm.readInt());
      FieldValidator.checkLength(dataLength, arcSize);

      fm.skip(dataLength);

      // 4 - Header (FILE)
      // 4 - Details Directory Length (not including these 2 fields)
      fm.skip(8);

      // 4 - Number of Entries?
      fm.skip(4);
      int numFiles = Archive.getMaxFiles();

      // 2 - null
      fm.skip(2);

      Resource[] resources = new Resource[numFiles];
      TaskProgressManager.setMaximum(numFiles);

      while (fm.getOffset() < arcSize) {
        readDirectory(fm, path, resources, 1, "");

        /*
        // 1 - Entry Type (0=Folder, 1=File)
        fm.skip(1);
        
        // 1 - File/Folder Name Length
        int nameLength = ByteConverter.unsign(fm.readByte());
        
        // X - File/Folder Name
        String filename = fm.readString(nameLength);
        
        // 4 - Number of Entries in this Folder
        int numEntries = fm.readInt();
        FieldValidator.checkNumFiles(numEntries);
        
        
        readDirectory(fm, path, resources, numEntries, filename);
        */
      }

      resources = resizeResources(resources, realNumFiles);

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

  public void readDirectory(FileManipulator fm, File path, Resource[] resources, int numEntries, String dirName) {
    try {

      ExporterPlugin exporter = Exporter_ZLib.getInstance();
      long arcSize = path.length();

      // Loop through directory
      for (int i = 0; i < numEntries; i++) {
        // 1 - Entry Type (0=Folder, 1=File)
        int entryType = fm.readByte();

        // 1 - File/Folder Name Length
        int nameLength = ByteConverter.unsign(fm.readByte());

        // X - File/Folder Name
        String filename = fm.readString(nameLength);
        FieldValidator.checkFilename(filename);

        filename = dirName + filename;

        if (entryType == 0) {
          // folder

          // 4 - Number of Entries in this Folder
          int numSubEntries = fm.readInt();
          FieldValidator.checkNumFiles(numSubEntries);

          readDirectory(fm, path, resources, numSubEntries, filename + "\\");
        }
        else if (entryType == 1) {
          // file

          // 4 - File Offset
          int offset = fm.readInt();
          FieldValidator.checkOffset(offset, arcSize);

          // 4 - Compressed File Length
          int length = fm.readInt();
          FieldValidator.checkLength(length, arcSize);

          // 4 - Decompressed File Length
          int decompLength = fm.readInt();
          FieldValidator.checkLength(decompLength);

          // 4 - null
          // 2 - null
          // 2 - Compression Type? ((bytes)1,6) if compressed, null if not compressed)
          fm.skip(8);

          if (length == decompLength) {
            //path,name,offset,length,decompLength,exporter
            resources[realNumFiles] = new Resource(path, filename, offset, length);
            realNumFiles++;
          }
          else {
            //path,name,offset,length,decompLength,exporter
            resources[realNumFiles] = new Resource(path, filename, offset, length, decompLength, exporter);
            realNumFiles++;
          }
        }
        else {
          ErrorLogger.log("[PAK_FORM] Unknown entry type: " + entryType);
          return;
        }

        TaskProgressManager.setValue(realNumFiles);
      }

    }
    catch (Throwable t) {
      logError(t);
    }
  }

}
