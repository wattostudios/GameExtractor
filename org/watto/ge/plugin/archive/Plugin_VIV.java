
package org.watto.ge.plugin.archive;

import java.io.File;
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
import org.watto.io.converter.IntConverter;
import org.watto.io.converter.ShortConverter;

/**
**********************************************************************************************

**********************************************************************************************
**/
public class Plugin_VIV extends ArchivePlugin {

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public Plugin_VIV() {

    super("VIV", "VIV");

    //         read write replace rename
    setProperties(true, false, false, false);

    setGames("Triple Play 98: Home Run Derby");
    setExtensions("big", "viv");
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
      if (fm.readString(2).equals(new String(new byte[] { (byte) 192, (byte) 251 }))) {
        rating += 50;
      }

      fm.skip(2);

      // Number Of Files
      int numFiles = ShortConverter.changeFormat(fm.readShort());
      if (FieldValidator.checkNumFiles(numFiles)) {
        rating += 5;
      }

      // null
      if (fm.readByte() == 0) {
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

      // BIG ENDIAN FORMAT!

      FileManipulator fm = new FileManipulator(path, false);

      // 2 - Header (192,251)
      // 2 - Directory Size [+ 0-7]
      fm.skip(4);

      // 2 - Number Of Files
      int numFiles = ShortConverter.changeFormat(fm.readShort());
      FieldValidator.checkNumFiles(numFiles);

      // 6 - Unknown
      //fm.skip(6);

      long arcSize = fm.getLength();

      Resource[] resources = new Resource[numFiles];
      TaskProgressManager.setMaximum(numFiles);

      // Loop through directory
      for (int i = 0; i < numFiles; i++) {

        // 4 - Offset
        //long offset = IntConverter.changeFormat(fm.readInt());
        byte[] offsetData = new byte[4];
        offsetData[0] = 0;
        offsetData[1] = fm.readByte();
        offsetData[2] = fm.readByte();
        offsetData[3] = fm.readByte();
        long offset = IntConverter.convertBig(offsetData);
        FieldValidator.checkOffset(offset, arcSize);

        // 1 - Unknown
        fm.skip(1);

        // 2 - Length
        //long length = ShortConverter.changeFormat(fm.readShort());
        //FieldValidator.checkLength(length,arcSize);
        fm.skip(2);

        // X - Filename (null)
        String filename = fm.readNullString();

        //path,id,name,offset,length,decompLength,exporter
        resources[i] = new Resource(path, filename, offset);

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
