
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
import org.watto.ge.plugin.ExporterPlugin;
import org.watto.ge.plugin.exporter.Exporter_ZLib;
import org.watto.io.FileManipulator;

/**
**********************************************************************************************

**********************************************************************************************
**/
public class Plugin_PKR_PKR3 extends ArchivePlugin {

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public Plugin_PKR_PKR3() {

    super("PKR_PKR3", "PKR_PKR3");

    //         read write replace rename
    setProperties(true, false, false, false);

    setExtensions("pkr");
    setGames("Spiderman");
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
      if (fm.readString(4).equals("PKR3")) {
        rating += 50;
      }

      long arcSize = fm.getLength();

      // Directory Offset
      if (FieldValidator.checkOffset(fm.readInt(), arcSize)) {
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

      ExporterPlugin exporter = Exporter_ZLib.getInstance();
      addFileTypes();

      FileManipulator fm = new FileManipulator(path, false);
      long arcSize = fm.getLength();

      // 4 - Header (PKR3)
      fm.skip(4);

      // 4 - Directory Offset
      long dirOffset = fm.readInt();
      FieldValidator.checkOffset(dirOffset, arcSize);

      fm.seek(dirOffset);

      // 4 - Unknown (4)
      fm.skip(4);

      // 4 - numDirectories
      int numDirectories = fm.readInt();
      FieldValidator.checkNumFiles(numDirectories);

      // 4 - total number of files in all directories
      int numFiles = fm.readInt();
      FieldValidator.checkNumFiles(numFiles);

      Resource[] resources = new Resource[numFiles];
      TaskProgressManager.setMaximum(numFiles);

      // for each directory
      String[] dirNames = new String[numDirectories];
      int[] numInDir = new int[numDirectories];
      for (int i = 0; i < numDirectories; i++) {
        // 32 - dirName
        dirNames[i] = fm.readNullString(32);
        FieldValidator.checkFilename(dirNames[i]);

        // 4 - subDirOffset [x52 + dirOffset]
        fm.skip(4);

        // 4 - numFiles in the directory
        numInDir[i] = fm.readInt();
        FieldValidator.checkNumFiles(numInDir[i]);
      }

      int fileNumber = 0;
      for (int i = 0; i < numDirectories; i++) {
        for (int j = 0; j < numInDir[i]; j++) {
          // 32 - filename
          String filename = dirNames[i] + fm.readNullString(32);

          // 4 - Unknown
          // 4 - Unknown
          fm.skip(8);

          // 4 - Data Offset
          long offset = fm.readInt();
          FieldValidator.checkOffset(offset, arcSize);

          // 4 - Raw File Length
          long length = fm.readInt();
          FieldValidator.checkLength(length, arcSize);

          // 4 - Compressed File Length
          int compLength = fm.readInt();
          FieldValidator.checkLength(compLength, arcSize);

          //path,id,name,offset,length,decompLength,exporter
          resources[fileNumber] = new Resource(path, filename, offset, compLength, length);
          if (compLength != length) {
            resources[fileNumber].setExporter(exporter);
          }

          TaskProgressManager.setValue(offset);
          fileNumber++;

        }

      }

      fm.close();

      return resources;

    }
    catch (Throwable t) {
      logError(t);
      return null;
    }
  }

}