
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

/**
**********************************************************************************************

**********************************************************************************************
**/
public class Plugin_DAT_14 extends ArchivePlugin {

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public Plugin_DAT_14() {

    super("DAT_14", "DAT_14");

    //         read write replace rename
    setProperties(true, false, false, false);

    setExtensions("dat");
    setGames("Settlers 3");
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

      // null
      if (fm.readInt() == 0) {
        rating += 5;
      }

      // Version
      int version = fm.readInt();
      if (version == 2 || version == 12) {
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

      // 4 - null
      fm.skip(4);

      // 4 - Version
      int version = fm.readInt();

      Resource[] resources = new Resource[0];

      if (version == 2) {
        resources = read2(fm, path);
      }
      else if (version == 12) {
        resources = read12(fm, path);
      }

      return resources;
    }
    catch (Throwable t) {
      logError(t);
      return null;
    }
  }

  public Resource[] read12(FileManipulator fm, File path) {
    try {
      // 116 - Unknown
      fm.skip(116);

      int numFiles = (fm.readInt() - 128) / 4;

      long arcSize = fm.getLength();

      Resource[] resources = new Resource[numFiles];
      TaskProgressManager.setMaximum(numFiles);

      fm.seek(124);

      for (int i = 0; i < numFiles; i++) {
        // 4 - Data Offset
        long offset = fm.readInt();
        FieldValidator.checkOffset(offset, arcSize);

        String filename = Resource.generateFilename(i);

        //path,id,name,offset,length,decompLength,exporter
        resources[i] = new Resource(path, filename, offset);

        TaskProgressManager.setValue(i);
      }

      // Calculate File Sizes
      for (int j = 0; j < numFiles - 1; j++) {
        resources[j].setLength((int) (resources[j + 1].getOffset() - resources[j].getOffset()));
      }
      resources[numFiles - 1].setLength((int) (arcSize - resources[numFiles - 1].getOffset()));

      fm.close();

      return resources;

    }
    catch (Throwable t) {
      logError(t);
      return null;
    }
  }

  public Resource[] read2(FileManipulator fm, File path) {
    try {
      // 4 - null
      // 4 - Unknown
      // 4 - Unknown
      // 4 - Unknown
      // 4 - Header Length? (28)
      // 4 - Unknown
      // 4 - Unknown
      fm.skip(28);

      int numFiles = (fm.readInt() - 40) / 4;

      long arcSize = fm.getLength();

      Resource[] resources = new Resource[numFiles];
      TaskProgressManager.setMaximum(numFiles);

      fm.seek(36);

      for (int i = 0; i < numFiles; i++) {
        // 4 - Data Offset
        long offset = fm.readInt();
        FieldValidator.checkOffset(offset, arcSize);

        String filename = Resource.generateFilename(i);

        //path,id,name,offset,length,decompLength,exporter
        resources[i] = new Resource(path, filename, offset);

        TaskProgressManager.setValue(i);
      }

      // Calculate File Sizes
      for (int j = 0; j < numFiles - 1; j++) {
        resources[j].setLength((int) (resources[j + 1].getOffset() - resources[j].getOffset()));
        FieldValidator.checkLength(resources[j].getLength(), arcSize);
      }
      resources[numFiles - 1].setLength((int) (arcSize - resources[numFiles - 1].getOffset()));

      fm.close();

      return resources;

    }
    catch (Throwable t) {
      logError(t);
      return null;
    }
  }

}