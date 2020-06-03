
package org.watto.ge.plugin.archive;

import java.io.File;
import org.watto.component.WSPopup;
import org.watto.task.TaskProgressManager;
import org.watto.datatype.Resource;
import org.watto.ge.helper.FieldValidator;
import org.watto.ge.plugin.ArchivePlugin;
////////////////////////////////////////////////////////////////////////////////////////////////
//                                                                                            //
//                                       GAME EXTRACTOR                                       //
//                               Extensible Game Archive Editor                               //
//                                http://www.watto.org/extract                                //
//                                                                                            //
//                           Copyright (C) 2002-2009  WATTO Studios                           //
//                                                                                            //
// This program is free software; you can redistribute it and/or modify it under the terms of //
// the GNU General Public License published by the Free Software Foundation; either version 2 //
// of the License, or (at your option) any later versions. This program is distributed in the //
// hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranties //
// of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License //
// at http://www.gnu.org for more details. For updates and information about this program, go //
// to the WATTO Studios website at http://www.watto.org or email watto@watto.org . Thanks! :) //
//                                                                                            //
////////////////////////////////////////////////////////////////////////////////////////////////
import org.watto.io.FileManipulator;

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

    setGames("Half-Life 2");
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

      WSPopup.showMessage("ZipConvertSuccess", false);

      Resource[] resources = new Resource[1];

      //path,id,name,offset,length,decompLength,exporter
      resources[0] = new Resource(zipPath, zipPath.getName(), 0, (int) zipPath.length());

      return resources;

    }
    catch (Throwable t) {
      logError(t);
      return null;
    }
  }

}
