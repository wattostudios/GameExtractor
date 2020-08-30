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
import org.watto.task.TaskProgressManager;

/**
**********************************************************************************************

**********************************************************************************************
**/
public class Plugin_BSP_VBSP extends ArchivePlugin {

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public Plugin_BSP_VBSP() {

    super("BSP_VBSP", "BSP_VBSP");

    //         read write replace rename
    setProperties(true, false, false, false);

    setGames("Half-Life 2",
        "Hidden");
    setExtensions("bsp");
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
      if (fm.readString(4).equals("VBSP")) {
        rating += 50;
      }

      // Version
      if (fm.readInt() == 19) {
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

      // convert the file to a ZIP archive
      File zipPath = new File(path.getAbsolutePath() + ".zip");

      FileManipulator fm = new FileManipulator(path, false);
      FileManipulator outfm = new FileManipulator(zipPath, true);

      fm.skip(8);

      long length = (int) fm.getLength() - 8;
      TaskProgressManager.setMaximum(length);

      outfm.writeString("PK");

      for (int i = 0; i < length; i++) {
        outfm.writeByte(fm.readByte());
        TaskProgressManager.setValue(i);
      }

      fm.close();
      outfm.close();

      /*
      WSPopup.showMessage("ZipConvertSuccess", false);
      
      Resource[] resources = new Resource[1];
      
      //path,id,name,offset,length,decompLength,exporter
      resources[0] = new Resource(zipPath, zipPath.getName(), 0, (int) zipPath.length());
      
      return resources;
      */

      // Now open the ZIP archive
      return new Plugin_ZIP_PK().read(zipPath);

    }
    catch (Throwable t) {
      logError(t);
      return null;
    }
  }

}
