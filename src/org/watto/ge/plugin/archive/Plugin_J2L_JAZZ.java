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
import org.watto.datatype.Archive;
import org.watto.datatype.Resource;
import org.watto.ge.helper.FieldValidator;
import org.watto.ge.plugin.ArchivePlugin;
import org.watto.ge.plugin.ExporterPlugin;
import org.watto.ge.plugin.exporter.Exporter_ZLib;
import org.watto.io.FileManipulator;
import org.watto.task.TaskProgressManager;

/**
**********************************************************************************************

**********************************************************************************************
**/
public class Plugin_J2L_JAZZ extends ArchivePlugin {

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public Plugin_J2L_JAZZ() {

    super("J2L_JAZZ", "J2L_JAZZ");

    //         read write replace rename
    setProperties(true, false, false, false);

    setGames("Jazz Jackrabbit 2");
    setExtensions("j2l", "j2t");
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
      if (fm.readString(49).equals("                      Jazz Jackrabbit 2 Data File")) {
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
  
  **********************************************************************************************
  **/
  @Override
  public Resource[] read(File path) {
    try {

      addFileTypes();

      ExporterPlugin exporter = Exporter_ZLib.getInstance();

      FileManipulator fm = new FileManipulator(path, false);

      long arcSize = fm.getLength();

      // 180 - Header ("                      Jazz Jackrabbit 2 Data File" + (byte)(13 10 13 10) + "         Retail distribution of this data is prohibited without" + (byte)(13 10) + "             written permission from Epic MegaGames, Inc." + (byte)(13 10 13 10 26) )
      // 4 - Type Header (TILE) (LEVL)
      // 4 - Unknown
      // 32 - Archive Description
      // 2 - Unknown
      // 4 - Archive Size
      // 4 - Unknown
      fm.skip(230);

      int numFiles = Archive.getMaxFiles();

      Resource[] resources = new Resource[numFiles];
      TaskProgressManager.setMaximum(arcSize);

      // Files Directory
      int realNumFiles = 0;
      long readBytes = fm.getOffset();
      long offset = fm.getOffset();
      while (readBytes < arcSize) {

        // 4 - Compressed Size
        long length = fm.readInt();
        FieldValidator.checkLength(length, arcSize);

        // 4 - Decompressed Size
        int decompLength = fm.readInt();
        FieldValidator.checkLength(decompLength);

        String filename = Resource.generateFilename(realNumFiles);

        //path,id,name,offset,length,decompLength,exporter
        resources[realNumFiles] = new Resource(path, filename, 0, length, decompLength, exporter);
        realNumFiles++;

        TaskProgressManager.setValue(readBytes);
        readBytes += length + 8;
      }

      resources = resizeResources(resources, realNumFiles);

      // go back and set the offsets
      numFiles = realNumFiles;

      offset += numFiles * 8;

      for (int i = 0; i < numFiles - 1; i++) {
        Resource resource = resources[i];
        resource.setOffset(offset);
        offset += resource.getLength();
      }
      resources[numFiles - 1].setOffset(offset);

      fm.close();

      return resources;

    }
    catch (Throwable t) {
      logError(t);
      return null;
    }
  }

}
