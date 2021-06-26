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
import java.util.Arrays;
import org.watto.datatype.ReplacableResource;
import org.watto.datatype.Resource;
import org.watto.ge.helper.FieldValidator;
import org.watto.ge.helper.ResourceSorter_Offset;
import org.watto.ge.plugin.ArchivePlugin;
import org.watto.io.FileManipulator;
import org.watto.task.TaskProgressManager;

/**
**********************************************************************************************

**********************************************************************************************
**/
public class Plugin_SHP_110 extends ArchivePlugin {

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public Plugin_SHP_110() {

    super("SHP_110", "SHP_110");

    //         read write replace rename
    setProperties(true, false, false, false);
    setCanImplicitReplace(true);

    setExtensions("shp");
    setGames("Panzer General",
        "Steel Panthers 2",
        "Steel Panthers 3");
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
      if (fm.readString(4).equals("1.10")) {
        rating += 50;
      }

      // Number Of Files
      if (FieldValidator.checkNumFiles(fm.readInt())) {
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

      // 4 - Version (1.10) - read as string
      fm.skip(4);

      // 4 - numFiles
      int numFiles = fm.readInt();
      FieldValidator.checkNumFiles(numFiles);

      long arcSize = fm.getLength();

      Resource[] resources = new Resource[numFiles];
      TaskProgressManager.setMaximum(numFiles);

      ResourceSorter_Offset[] sorter = new ResourceSorter_Offset[numFiles]; // for calculating the file lengths
      for (int i = 0; i < numFiles; i++) {
        // 4 - offset
        long offsetPointerLocation = fm.getOffset();
        long offsetPointerLength = 4;

        long offset = fm.readInt();
        FieldValidator.checkOffset(offset, arcSize);

        // 4 - ID? type?
        fm.skip(4);

        String filename = Resource.generateFilename(i) + ".shpimg";

        //path,id,name,offset,length,decompLength,exporter
        Resource resource = new ReplacableResource(path, filename, offset, offsetPointerLocation, offsetPointerLength);
        resources[i] = resource;

        sorter[i] = new ResourceSorter_Offset(resource);

        TaskProgressManager.setValue(i);
      }

      // Calculate File Sizes
      Arrays.sort(sorter);

      for (int j = 0; j < numFiles - 1; j++) {
        sorter[j].getResource().setLength((int) (sorter[j + 1].getResource().getOffset() - sorter[j].getResource().getOffset()));
        FieldValidator.checkLength(sorter[j].getResource().getLength(), arcSize);
      }
      sorter[numFiles - 1].getResource().setLength((int) (arcSize - sorter[numFiles - 1].getResource().getOffset()));

      fm.close();

      return resources;

    }
    catch (Throwable t) {
      logError(t);
      return null;
    }
  }

}