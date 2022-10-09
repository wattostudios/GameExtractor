/*
 * Application:  Game Extractor
 * Author:       wattostudios
 * Website:      http://www.watto.org
 * Copyright:    Copyright (c) 2002-2021 wattostudios
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
public class Plugin_DBF_STATICDATABASE extends ArchivePlugin {

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public Plugin_DBF_STATICDATABASE() {

    super("DBF_STATICDATABASE", "DBF_STATICDATABASE");

    //         read write replace rename
    setProperties(true, false, true, false);

    setGames("NHL 2001");
    setExtensions("dbf"); // MUST BE LOWER CASE
    setPlatforms("PS1");

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

      fm.skip(4);

      // Header
      if (fm.readString(15).equals("STATIC_DATABASE")) {
        rating += 50;
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

      // 4 - Length of the Database File
      // 4 - Unknown
      fm.skip(8);

      // 4 - Number of Tables
      int numFiles = fm.readInt();
      FieldValidator.checkNumFiles(numFiles);

      // 4 - null
      // 16 - Description ("STATIC DATABASE" + null)
      // 16 - null
      fm.skip(36);

      Resource[] resources = new Resource[numFiles];
      TaskProgressManager.setMaximum(numFiles);

      // Loop through directory
      for (int i = 0; i < numFiles; i++) {

        // 4 - Table Data Length
        long length = fm.readInt();
        FieldValidator.checkLength(length, arcSize);

        // 4 - Table ID (incremental from 0)
        // 4 - Number of Records
        // 4 - Record Length
        fm.skip(12);

        // 4 - Offset to the start of the Table
        long offset = fm.readInt();
        FieldValidator.checkOffset(offset, arcSize);

        // 12 - null
        fm.skip(12);

        String filename = Resource.generateFilename(i) + ".table";

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

      int headerSize = 48;

      long directorySize = numFiles * 32;
      long directoryPaddingSize = calculatePadding(directorySize, 512);
      directorySize += directoryPaddingSize;

      long filesSize = 0;
      for (int i = 0; i < numFiles; i++) {
        filesSize += resources[i].getDecompressedLength();
      }

      long archiveSize = headerSize + filesSize + directorySize;

      // Write Header Data

      // 4 - Length of the Database File
      fm.writeInt(archiveSize);
      src.skip(4);

      // 4 - Unknown
      // 4 - Number of Tables
      // 4 - null
      // 16 - Description ("STATIC DATABASE" + null)
      // 16 - null
      fm.writeBytes(src.readBytes(44));

      // Write Directory
      TaskProgressManager.setMessage(Language.get("Progress_WritingDirectory"));
      long offset = headerSize + directorySize;
      for (int i = 0; i < numFiles; i++) {
        Resource resource = resources[i];
        long length = resource.getDecompressedLength();

        // 4 - Table Data Length
        fm.writeInt(length);
        src.skip(4);

        // 4 - Table ID (incremental from 0)
        // 4 - Number of Records
        // 4 - Record Length
        fm.writeBytes(src.readBytes(12));

        // 4 - Offset to the start of the Table
        fm.writeInt(offset);
        src.skip(4);

        // 12 - null
        fm.writeBytes(src.readBytes(12));

        offset += length;
      }

      // X - null Padding so that the TABLES DIRECTORY is a multiple of 512 bytes
      for (int p = 0; p < directoryPaddingSize; p++) {
        fm.writeByte(0);
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
