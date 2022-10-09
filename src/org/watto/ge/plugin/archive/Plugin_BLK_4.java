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
import org.watto.io.converter.IntConverter;
import org.watto.task.TaskProgressManager;

/**
**********************************************************************************************

**********************************************************************************************
**/
public class Plugin_BLK_4 extends ArchivePlugin {

  /**
  **********************************************************************************************
  MISSIONS.BLK specifically
  **********************************************************************************************
  **/
  public Plugin_BLK_4() {

    super("BLK_4", "BLK_4");

    //         read write replace rename
    setProperties(true, false, true, false);

    setGames("Driver 2");
    setExtensions("blk"); // MUST BE LOWER CASE
    setPlatforms("PSX");

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
      if (fm.readInt() == 0) {
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

      int numFiles = 2048 / 4; // max
      int realNumFiles = 0;

      Resource[] resources = new Resource[numFiles];
      TaskProgressManager.setMaximum(numFiles);

      // Loop through directory
      for (int i = 0; i < numFiles; i++) {

        // 4 - Combined Offset+Length
        long comboValue = IntConverter.unsign(fm.readInt());
        if (comboValue == 0) {
          continue;
        }

        int offset = (int) (comboValue & 0x7ffff);
        FieldValidator.checkOffset(offset, arcSize);

        int length = (int) (comboValue >> 19);
        FieldValidator.checkLength(length, arcSize);

        String filename = "M" + i + ".D2MS"; // matches the format of "modified" missions in OpenDriver2 at https://github.com/OpenDriver2/REDRIVER2

        //path,name,offset,length,decompLength,exporter
        resources[realNumFiles] = new Resource(path, filename, offset, length);
        realNumFiles++;

        TaskProgressManager.setValue(i);
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
      TaskProgressManager.setMessage(Language.get("Progress_WritingDirectory"));

      // Write Header Data

      int maxSourceFiles = 2048 / 4;

      int currentPos = 0;
      int offset = 2048;
      for (int i = 0; i < maxSourceFiles; i++) {
        // 4 - Combined Offset+Length
        int comboValue = src.readInt();
        if (comboValue == 0) {
          fm.writeInt(0);
          continue;
        }

        // if we're here, it's a non-null, so it corresponds to the next file in the archive
        Resource resource = resources[currentPos];

        int length = (int) resource.getDecompressedLength();

        comboValue = (offset & 0x7ffff) | (length << 19);
        fm.writeInt(comboValue);

        int paddingSize = calculatePadding(length, 2048);
        offset += length + paddingSize;

        currentPos++;
      }

      // Write Files
      TaskProgressManager.setMessage(Language.get("Progress_WritingFiles"));
      for (int i = 0; i < numFiles; i++) {
        Resource resource = resources[i];

        // X - File Data
        write(resource, fm);

        // X - Padding to a multiple of 2048 bytes (using byte 33)
        int paddingSize = calculatePadding(resource.getDecompressedLength(), 2048);
        for (int p = 0; p < paddingSize; p++) {
          fm.writeByte(33);
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
