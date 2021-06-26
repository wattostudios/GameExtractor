
package org.watto.ge.plugin.archive;

import java.io.File;
import org.watto.task.TaskProgressManager;
import org.watto.datatype.Archive;
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
public class Plugin_DNI_DIRT extends ArchivePlugin {

  int i = 0;

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public Plugin_DNI_DIRT() {

    super("DNI_DIRT", "DNI_DIRT");

    //         read write replace rename
    setProperties(true, false, false, false);

    setExtensions("dni");
    setGames("Real Myst");
    setPlatforms("PC");

  }

  public void DNIDirectory(FileManipulator fm, File path, Resource[] resources, String cdir, long coffset, long FTOffset) throws Exception {

    long arcSize = fm.getLength();

    fm.seek(coffset);

    // 4 - dir Offset
    long dirOffset = fm.readInt();
    FieldValidator.checkOffset(dirOffset, arcSize);

    // 4 - numFiles
    int numFiles = fm.readInt();
    FieldValidator.checkNumFiles(numFiles);

    fm.seek(dirOffset);

    //X - filename (cdir2)
    String cdir2 = fm.readNullString();

    String cdirp = cdir + cdir2 + "\\";
    if (cdirp == "\\") {
      cdirp = "";
    }

    coffset += 8;

    int readLength = 0;
    for (int i = 0; i < numFiles; i++) {
      fm.seek(coffset);

      // 4 - Object Offset
      int objOffset = fm.readInt();
      FieldValidator.checkOffset(objOffset, arcSize);

      if (objOffset < FTOffset) {
        DNIDirectory(fm, path, resources, cdirp, objOffset, FTOffset);
      }
      else {
        fm.seek(objOffset);

        // 4 - Offset Name (int)
        int nameOffset = fm.readInt();
        FieldValidator.checkOffset(nameOffset, arcSize);

        // 4 - Offset Name Next (int)
        fm.skip(4);

        // 4 - File Length
        long length = fm.readInt();
        FieldValidator.checkLength(length, arcSize);

        // 4 - Data Offset
        long offset = fm.readInt();
        FieldValidator.checkOffset(offset, arcSize);

        // 4 - Empty
        fm.skip(4);

        fm.seek(nameOffset);
        // X - cdir + filename (null)
        String filename = cdir + fm.readNullString();

        //path,id,name,offset,length,decompLength,exporter
        resources[i] = new Resource(path, filename, offset, length);

        TaskProgressManager.setValue(readLength);
        readLength += length;

        i++;
      }
      coffset += 4;
    }

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
      if (fm.readString(4).equals("Dirt")) {
        rating += 50;
      }

      // Directory Offset
      if (FieldValidator.checkOffset(fm.readInt())) {
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

      // RESETTING THE GLOBAL VARIABLES
      i = 0;

      FileManipulator fm = new FileManipulator(path, false);
      long arcSize = fm.getLength();

      // 4 - Header (Dirt)
      // 4 - Version (65536)
      fm.skip(8);

      // 4 - DirOffset
      long dirOffset = fm.readInt();
      FieldValidator.checkOffset(dirOffset, arcSize);

      // 4 - FT Offset
      int FTOffset = fm.readInt();
      FieldValidator.checkOffset(FTOffset, arcSize);

      // 4 - NL Offset
      // 4 - Data Offset
      // 4 - FT Offset A
      fm.skip(12);

      int numFiles = Archive.getMaxFiles(4);//guess

      Resource[] resources = new Resource[numFiles];
      TaskProgressManager.setMaximum(arcSize);

      DNIDirectory(fm, path, resources, "", dirOffset, FTOffset);

      resources = resizeResources(resources, i);

      fm.close();

      return resources;

    }
    catch (Throwable t) {
      logError(t);
      return null;
    }
  }

}