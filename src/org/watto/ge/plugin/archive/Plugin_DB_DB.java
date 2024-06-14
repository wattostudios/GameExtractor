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
import org.watto.io.converter.IntConverter;
import org.watto.io.converter.ShortConverter;
import org.watto.task.TaskProgressManager;

/**
**********************************************************************************************

**********************************************************************************************
**/
public class Plugin_DB_DB extends ArchivePlugin {

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public Plugin_DB_DB() {

    super("DB_DB", "DB_DB");

    //         read write replace rename
    setProperties(true, false, true, false);

    setGames("NHL 10");
    setExtensions("db"); // MUST BE LOWER CASE
    setPlatforms("PS3");

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
      if (fm.readString(2).equals("DB")) {
        if (ShortConverter.changeFormat(fm.readShort()) == 8) {
          rating += 50;
        }
      }
      else {
        fm.skip(2);
      }

      fm.skip(4);

      long arcSize = fm.getLength();

      // 4 - Database File Length
      if (FieldValidator.checkEquals(IntConverter.changeFormat(fm.readInt()), arcSize)) {
        rating += 5;
      }

      fm.skip(4);

      // Number Of Tables
      if (FieldValidator.checkNumFiles(IntConverter.changeFormat(fm.readInt()))) {
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

      // 2 - Header (DB)
      // 2 - Unknown (8)
      // 2 - Unknown (256)
      // 2 - null
      // 4 - Database File Length
      // 4 - null
      fm.skip(16);

      // 4 - Number of Tables
      int numFiles = IntConverter.changeFormat(fm.readInt());
      FieldValidator.checkNumFiles(numFiles);

      // 4 - Hash?
      fm.skip(4);

      Resource[] resources = new Resource[numFiles];
      TaskProgressManager.setMaximum(numFiles);

      // Loop through directory
      int relOffset = 24 + (numFiles * 8);
      for (int i = 0; i < numFiles; i++) {

        // 4 - Table Code
        String filename = fm.readString(4) + ".table";

        // 4 - Table Offset (relative to the start of the table data)
        long offset = IntConverter.changeFormat(fm.readInt()) + relOffset;
        FieldValidator.checkOffset(offset, arcSize);

        //path,name,offset,length,decompLength,exporter
        resources[i] = new Resource(path, filename, offset);

        TaskProgressManager.setValue(i);
      }

      calculateFileSizes(resources, arcSize);

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

      long archiveSize = 24 + (numFiles * 8);
      for (int i = 0; i < numFiles; i++) {
        archiveSize += resources[i].getDecompressedLength();
      }

      // Write Header Data

      // 2 - Header (DB)
      // 2 - Unknown (8)
      // 2 - Unknown (256)
      // 2 - null
      fm.writeBytes(src.readBytes(8));

      // 4 - Database File Length
      fm.writeInt(IntConverter.changeFormat((int) archiveSize));
      src.skip(4);

      // 4 - null
      fm.writeBytes(src.readBytes(4));

      // 4 - Number of Tables
      fm.writeInt(IntConverter.changeFormat(numFiles));
      src.skip(4);

      // 4 - Hash?
      fm.writeBytes(src.readBytes(4));

      // Write Directory
      TaskProgressManager.setMessage(Language.get("Progress_WritingDirectory"));
      long offset = 0;
      for (int i = 0; i < numFiles; i++) {
        Resource resource = resources[i];
        long length = resource.getDecompressedLength();

        // 4 - Table Code
        fm.writeBytes(src.readBytes(4));

        // 4 - Table Offset (relative to the start of the table data)
        fm.writeInt(IntConverter.changeFormat((int) offset));
        src.skip(4);

        offset += length;
      }

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
