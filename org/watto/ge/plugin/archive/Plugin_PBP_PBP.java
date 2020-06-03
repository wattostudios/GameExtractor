
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
public class Plugin_PBP_PBP extends ArchivePlugin {

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public Plugin_PBP_PBP() {

    super("PBP_PBP", "PBP_PBP");

    //         read write replace rename
    setProperties(true, false, false, false);

    setGames("PlayStation Portable Firmware");
    setExtensions("pbp");
    setPlatforms("PSP");

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
      if (fm.readString(4).equals((char) 0 + "PBP")) {
        rating += 50;
      }

      //long arcSize = fm.getLength();

      // Version
      if (fm.readByte() == 0 && fm.readByte() == 0 && fm.readByte() == 1 && fm.readByte() == 0) {
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

      // RESETTING THE GLOBAL VARIABLES

      FileManipulator fm = new FileManipulator(path, false);

      long arcSize = (int) fm.getLength();

      // 4 - Header (null + "PBP")
      // 4 - Version
      fm.skip(8);

      int numFiles = 8; // at most, 8 files

      Resource[] resources = new Resource[numFiles];

      TaskProgressManager.setMaximum(numFiles);

      int realNumFiles = 0;
      long currentOffset = 0;
      long offset = 0;

      // 4 - Offset To Param.sfo
      offset = fm.readInt();
      if (offset != currentOffset) {
        String filename = "param.sfo";

        //path,id,name,offset,length,decompLength,exporter
        resources[realNumFiles] = new Resource(path, filename, offset);
        TaskProgressManager.setValue(realNumFiles);

        currentOffset = offset;
        realNumFiles++;
      }

      // 4 - Offset To Icon0.png
      offset = fm.readInt();
      if (offset != currentOffset) {
        String filename = "icon0.png";

        //path,id,name,offset,length,decompLength,exporter
        resources[realNumFiles] = new Resource(path, filename, offset);
        TaskProgressManager.setValue(realNumFiles);

        currentOffset = offset;
        realNumFiles++;
      }

      // 4 - Offset To Icon1.pmf
      offset = fm.readInt();
      if (offset != currentOffset) {
        String filename = "icon1.pmf";

        //path,id,name,offset,length,decompLength,exporter
        resources[realNumFiles] = new Resource(path, filename, offset);
        TaskProgressManager.setValue(realNumFiles);

        currentOffset = offset;
        realNumFiles++;
      }

      // 4 - Offset To Pic0.png
      offset = fm.readInt();
      if (offset != currentOffset) {
        String filename = "pic0.png";

        //path,id,name,offset,length,decompLength,exporter
        resources[realNumFiles] = new Resource(path, filename, offset);
        TaskProgressManager.setValue(realNumFiles);

        currentOffset = offset;
        realNumFiles++;
      }

      // 4 - Offset To Pic1.png
      offset = fm.readInt();
      if (offset != currentOffset) {
        String filename = "pic1.png";

        //path,id,name,offset,length,decompLength,exporter
        resources[realNumFiles] = new Resource(path, filename, offset);
        TaskProgressManager.setValue(realNumFiles);

        currentOffset = offset;
        realNumFiles++;
      }

      // 4 - Offset To Snd0.at3
      offset = fm.readInt();
      if (offset != currentOffset) {
        String filename = "snd0.at3";

        //path,id,name,offset,length,decompLength,exporter
        resources[realNumFiles] = new Resource(path, filename, offset);
        TaskProgressManager.setValue(realNumFiles);

        currentOffset = offset;
        realNumFiles++;
      }

      // 4 - Offset To Data.psp
      offset = fm.readInt();
      if (offset != currentOffset) {
        String filename = "data.psp";

        //path,id,name,offset,length,decompLength,exporter
        resources[realNumFiles] = new Resource(path, filename, offset);
        TaskProgressManager.setValue(realNumFiles);

        currentOffset = offset;
        realNumFiles++;
      }

      // 4 - Offset To Data.psar
      offset = fm.readInt();
      if (offset != currentOffset) {
        String filename = "data.psar";

        //path,id,name,offset,length,decompLength,exporter
        resources[realNumFiles] = new Resource(path, filename, offset);
        TaskProgressManager.setValue(realNumFiles);

        currentOffset = offset;
        realNumFiles++;
      }

      resources = resizeResources(resources, realNumFiles);
      calculateFileSizes(resources, arcSize);

      fm.close();

      return resources;

    }
    catch (Throwable t) {
      logError(t);
      return null;
    }
  }

}
