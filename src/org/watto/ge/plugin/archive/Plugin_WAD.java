
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

/**
**********************************************************************************************

**********************************************************************************************
**/
public class Plugin_WAD extends ArchivePlugin {

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public Plugin_WAD() {

    super("WAD", "WAD");

    //         read write replace rename
    setProperties(true, false, false, false);

    setExtensions("wad");
    setGames("Thunderhawk",
        "Firestorm: Thunderhawk 2");
    setPlatforms("PC");

    setFileTypes("pal", "Image Color Palette",
        "dat", "Data File",
        "map", "Map Image?",
        "bkg", "Background Image",
        "mdt", "Map Data?",
        "img", "Image File",
        "spr", "Image Sprite");

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

      // Number Of Files
      if (FieldValidator.checkNumFiles(IntConverter.changeFormat(fm.readInt()) / 12)) {
        rating += 5;
      }

      long arcSize = fm.getLength();

      // First File Offset
      if (FieldValidator.checkOffset(IntConverter.changeFormat(fm.readInt()), arcSize)) {
        rating += 5;
      }

      // First File Length
      if (FieldValidator.checkLength(IntConverter.changeFormat(fm.readInt()), arcSize)) {
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
      long arcSize = fm.getLength();

      // 4 - directoryLength (big)
      int dirLength = IntConverter.changeFormat(fm.readInt());
      FieldValidator.checkLength(dirLength, arcSize);

      int numFiles = dirLength / 12;
      FieldValidator.checkNumFiles(numFiles);

      // JUMP TO FILENAME DIRECTORY
      fm.seek(dirLength);

      String[] names = new String[numFiles];
      for (int i = 0; i < numFiles; i++) {
        // X - Filename (null)
        String filename = fm.readNullString();
        FieldValidator.checkFilename(filename);
        names[i] = filename;
      }

      // JUMP TO NORMAL DIRECTORY
      fm.seek(0);

      Resource[] resources = new Resource[numFiles];
      TaskProgressManager.setMaximum(numFiles);

      for (int i = 0; i < numFiles; i++) {
        // 4 - FilenameOffset (big)
        fm.skip(4);

        // 4 - offset (big)
        long offset = IntConverter.changeFormat(fm.readInt());
        FieldValidator.checkOffset(offset, arcSize);

        // 4 - fileLength (big)
        long length = IntConverter.changeFormat(fm.readInt());
        FieldValidator.checkLength(length, arcSize);

        String filename = names[i];

        //path,id,name,offset,length,decompLength,exporter
        resources[i] = new Resource(path, filename, offset, length);

        TaskProgressManager.setValue(i);
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