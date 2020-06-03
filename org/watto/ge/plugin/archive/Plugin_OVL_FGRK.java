
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
public class Plugin_OVL_FGRK extends ArchivePlugin {

  /**
  **********************************************************************************************

  **********************************************************************************************
  **/
  public Plugin_OVL_FGRK() {

    super("OVL_FGRK", "RollerCoaster Tycoon 3 OVL");

    //         read write replace rename
    setProperties(true, false, false, false);

    setGames("RollerCoaster Tycoon 3");
    setExtensions("ovl");
    setPlatforms("PC");

    setFileTypes("spl", "Spline",
        "trr", "Tracked Ride",
        "gsi", "GUI Skin Item",
        "fct", "Font File",
        "flic", "Flic Animation",
        "tex", "Texture Image",
        "shs", "Static Shape",
        "snd", "Sound File",
        "ftx", "Flexi Texture",
        "svd", "Visual Scenery Item",
        "mms", "Morph Mesh",
        "cid", "Carried Item",
        "ced", "Extra Carried Item",
        "ban", "Bone Animation",
        "ric", "Ride Car",
        "rit", "Ride Train",
        "bsh", "Bone Shape",
        "prt", "Person Part",
        "ppg", "Person Part Group",
        "phd", "Person Head",
        "ssk", "Simple Skin",
        "san", "Simple Animation",
        "sal", "Simple Animation List",
        "rcg", "Research Category",
        "sid", "Scenery Item",
        "psi", "Particle Skin",
        "ptd", "Path Type",
        "qtd", "Queue Type",

        // one I made up
        "dir", "Directory Details");

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
      if (fm.readString(8).equals("FGRK")) {
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

      FileManipulator fm = new FileManipulator(path, false);
      //long arcSize = fm.getLength();

      // 4 - Header (FGRK)
      // 4 - null
      // 4 - Version? (1)
      // 8 - null
      fm.skip(20);

      // 4 - Number Of Directories
      int numDirs = fm.readInt();
      FieldValidator.checkNumFiles(numDirs);

      String[] ext = new String[4];

      // A temporary bug fix - i need to figure out the format properly before I can fix it!
      if (numDirs > 4) {
        ext = new String[numDirs];
      }

      java.util.Arrays.fill(ext, "unk");
      for (int i = 0; i < numDirs; i++) {
        // 2 - Header Length
        // 4 - Header
        fm.skip(fm.readShort());

        // 2 - Directory Name Length
        // X - Directory Name
        fm.skip(fm.readShort());

        // 4 - Type of Directory? (1=RCT3, 2=FGDK)
        fm.skip(4);

        // 2 - Extension Length
        // X - Extension
        ext[i] = fm.readString(fm.readShort());
      }

      // 8 - Number Of Filename Directories (1)
      //int numDirectories = (int)fm.readLong();
      //FieldValidator.checkNumFiles(numDirectories);

      // 8 - Number Of Files of Type 0
      int numFiles0 = (int) fm.readLong();

      // 8 - Number Of Files of Type 1
      int numFiles1 = (int) fm.readLong();

      // 8 - Number Of Files of Type 2
      int numFiles2 = (int) fm.readLong();

      // 8 - Number Of Files of Type 3
      int numFiles3 = (int) fm.readLong();

      //int numFiles = numDirectories + numFiles1 + numFiles2 + numFiles3;
      int numFiles = numFiles0 + numFiles1 + numFiles2 + numFiles3 + 1;
      FieldValidator.checkNumFiles(numFiles);

      // 4 - null
      fm.skip(4);

      Resource[] resources = new Resource[numFiles];

      TaskProgressManager.setMaximum(numFiles);

      int readPos = 0;

      // Filename Directory
      for (int i = 0; i < 1; i++) {
        // 4 - File Size
        long length = fm.readInt();

        // X - File Data
        long offset = (int) fm.getOffset();
        fm.skip(length);

        String filename = Resource.generateFilename(readPos) + ".dir";

        //path,id,name,offset,length,decompLength,exporter
        resources[readPos] = new Resource(path, filename, offset, length);
        TaskProgressManager.setValue(readPos);
        readPos++;
      }

      // Directory for files of Type 0
      for (int i = 0; i < numFiles0; i++) {
        // 4 - File Size
        long length = fm.readInt();

        // X - File Data
        long offset = (int) fm.getOffset();
        fm.skip(length);

        String filename = Resource.generateFilename(readPos) + "." + ext[0];

        //path,id,name,offset,length,decompLength,exporter
        resources[readPos] = new Resource(path, filename, offset, length);
        TaskProgressManager.setValue(readPos);
        readPos++;
      }

      // Directory for files of Type 1
      for (int i = 0; i < numFiles1; i++) {
        // 4 - File Size
        long length = fm.readInt();

        // X - File Data
        long offset = (int) fm.getOffset();
        fm.skip(length);

        String filename = Resource.generateFilename(readPos) + "." + ext[1];

        //path,id,name,offset,length,decompLength,exporter
        resources[readPos] = new Resource(path, filename, offset, length);
        TaskProgressManager.setValue(readPos);
        readPos++;
      }

      // Directory for files of Type 2
      for (int i = 0; i < numFiles2; i++) {
        // 4 - File Size
        long length = fm.readInt();

        // X - File Data
        long offset = (int) fm.getOffset();
        fm.skip(length);

        String filename = Resource.generateFilename(readPos) + "." + ext[2];

        //path,id,name,offset,length,decompLength,exporter
        resources[readPos] = new Resource(path, filename, offset, length);
        TaskProgressManager.setValue(readPos);
        readPos++;
      }

      // Directory for files of Type 3
      for (int i = 0; i < numFiles3; i++) {
        // 4 - File Size
        long length = fm.readInt();

        // X - File Data
        long offset = (int) fm.getOffset();
        fm.skip(length);

        String filename = Resource.generateFilename(readPos) + "." + ext[3];

        //path,id,name,offset,length,decompLength,exporter
        resources[readPos] = new Resource(path, filename, offset, length);
        TaskProgressManager.setValue(readPos);
        readPos++;
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
