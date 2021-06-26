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
import org.watto.io.FileManipulator;
import org.watto.io.converter.ByteConverter;
import org.watto.task.TaskProgressManager;

/**
**********************************************************************************************

**********************************************************************************************
**/
public class Plugin_XPR_XPR1 extends ArchivePlugin {

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public Plugin_XPR_XPR1() {

    super("XPR_XPR1", "XPR_XPR1");

    //         read write replace rename
    setProperties(true, false, false, false);

    setGames("Generic XBox Archive",
        "Unreal Tournament 2");
    setExtensions("xpr");
    setPlatforms("XBox");

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
      if (fm.readString(4).equals("XPR1")) {
        rating += 50;
      }

      long arcSize = fm.getLength();

      // Archive Size
      if (FieldValidator.checkEquals(fm.readInt(), arcSize)) {
        rating += 5;
      }

      // first file offset
      if (FieldValidator.checkOffset(fm.readInt(), arcSize)) {
        rating += 5;
      }

      // Number Of Files
      int numFiles = fm.readInt();
      if (FieldValidator.checkNumFiles(numFiles)) {
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

      long arcSize = fm.getLength();

      // 4 - Header (XPR1)
      // 4 - Archive Size
      fm.skip(8);

      // 4 - First File Offset (2048)
      int relOffset = fm.readInt();
      FieldValidator.checkOffset(relOffset, arcSize);

      // 4 - Number Of Files
      int numFiles = fm.readInt();
      FieldValidator.checkNumFiles(numFiles);

      Resource[] resources = new Resource[numFiles];
      TaskProgressManager.setMaximum(numFiles);

      // X - pointer directory
      fm.skip(4);
      int firstDataEntryOffset = fm.readInt() + 12;
      fm.skip((numFiles - 1) * 8 + 4);

      // Loop through filename directory
      String[] names = new String[numFiles];
      for (int i = 0; i < numFiles; i++) {
        // X - Filename (null)
        names[i] = fm.readNullString();
      }

      // X - null padding
      fm.seek(firstDataEntryOffset);

      // Loop through directory
      int[] powers = new int[] { 1, 2, 4, 8, 16, 32, 64, 128, 256, 512, 1024, 2048, 4096 };
      for (int i = 0; i < numFiles; i++) {
        // 2 - Unknown (1)
        // 2 - Unknown (4)
        fm.skip(4);

        // 8 - Offset [+firstFileOffset]
        long offset = (int) fm.readLong() + relOffset;
        FieldValidator.checkOffset(offset, arcSize);

        // 4 - Flags
        fm.skip(2);

        int width = powers[(ByteConverter.unsign(fm.readByte()) >> 4)];
        int height = powers[(ByteConverter.unsign(fm.readByte()) & 15)];

        // 4 - Unknown
        fm.skip(4);

        String filename = names[i];

        //System.out.println(width + "\t" + height + "\t" + filename);

        //path,id,name,offset,length,decompLength,exporter
        Resource resource = new Resource(path, filename, offset);
        resource.addProperty("Width", width);
        resource.addProperty("Height", height);
        resources[i] = resource;

        TaskProgressManager.setValue(i);
      }

      fm.close();

      calculateFileSizes(resources, arcSize);

      return resources;

    }
    catch (Throwable t) {
      logError(t);
      return null;
    }
  }

}
