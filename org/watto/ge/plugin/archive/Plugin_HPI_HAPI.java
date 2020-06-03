
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
import org.watto.ge.plugin.exporter.Exporter_Custom_HPI_HAPI;
import org.watto.io.FileManipulator;
import org.watto.io.converter.IntConverter;

/**
**********************************************************************************************

**********************************************************************************************
**/
public class Plugin_HPI_HAPI extends ArchivePlugin {

  int realNumFiles = 0;
  int key = 0;

  /**
  **********************************************************************************************

  **********************************************************************************************
  **/
  public Plugin_HPI_HAPI() {

    super("HPI_HAPI", "HPI_HAPI");

    //         read write replace rename
    setProperties(true, false, false, false);

    setExtensions("hpi", "ufo", "ccx", "pck");
    setGames("Total Annihilation");
    setPlatforms("PC");

  }

  /**
  **********************************************************************************************

  **********************************************************************************************
  **/
  @Override
  public int getMatchRating(FileManipulator fm) {
    try {

      realNumFiles = 0;
      key = 0;

      int rating = 0;

      if (FieldValidator.checkExtension(fm, extensions)) {
        rating += 25;
      }

      // Header
      if (fm.readString(4).equals("HAPI")) {
        rating += 50;
      }

      long arcSize = fm.getLength();

      fm.skip(4);

      // 4 - Directory Length
      if (FieldValidator.checkLength(fm.readInt(), arcSize)) {
        rating += 5;
      }

      fm.skip(4);

      // 4 - Header Size (20)
      if (fm.readInt() == 20) {
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
      realNumFiles = 0;
      key = 0;

      FileManipulator fm = new FileManipulator(path, false);

      long arcSize = fm.getLength();

      // 4 - Header (HAPI)
      // 4 - Unknown
      fm.skip(8);

      // 4 - Directory Length
      int dirLength = fm.readInt();
      FieldValidator.checkLength(dirLength, arcSize);

      // 4 - Directory Key
      key = fm.readInt();

      // 4 - Directory Offset
      long dirOffset = fm.readInt();
      FieldValidator.checkOffset(dirOffset, arcSize);

      if (key != 0) {
        key = ~((key * 4) | (key >> 6));
      }

      Exporter_Custom_HPI_HAPI exporter = Exporter_Custom_HPI_HAPI.getInstance();
      exporter.setKey(key);

      fm.seek(dirOffset);

      int numFiles = Archive.getMaxFiles(4);

      Resource[] resources = new Resource[numFiles];
      TaskProgressManager.setMaximum(arcSize);

      readDirectory(fm, path, resources, key, "");

      resources = resizeResources(resources, realNumFiles);

      fm.close();

      return resources;

    }
    catch (Throwable t) {
      logError(t);
      return null;
    }
  }

  /**
  **********************************************************************************************

  **********************************************************************************************
  **/
  public byte readByte(FileManipulator fm, int key) throws Exception {
    if (key == 0) {
      return fm.readByte();
    }
    int tKey = (int) fm.getOffset() ^ key;
    return (byte) (tKey ^ ~(fm.readByte()));
  }

  /**
  **********************************************************************************************

  **********************************************************************************************
  **/
  public void readDirectory(FileManipulator fm, File path, Resource[] resources, int key, String name) throws Exception {

    Exporter_Custom_HPI_HAPI exporter = Exporter_Custom_HPI_HAPI.getInstance();

    long arcSize = fm.getLength();

    // 4 - Number Of Files In This Directory
    int numFiles = IntConverter.convertLittle(new byte[] { readByte(fm, key), readByte(fm, key), readByte(fm, key), readByte(fm, key) });
    FieldValidator.checkNumFiles(numFiles + 1);

    // 4 - Directory Offset
    long dirOffset = IntConverter.convertLittle(new byte[] { readByte(fm, key), readByte(fm, key), readByte(fm, key), readByte(fm, key) });
    FieldValidator.checkOffset(dirOffset, arcSize);

    int curPos = (int) fm.getOffset();
    fm.seek(dirOffset);

    for (int i = 0; i < numFiles; i++) {

      // 4 - Filename Offset
      int nameOffset = IntConverter.convertLittle(new byte[] { readByte(fm, key), readByte(fm, key), readByte(fm, key), readByte(fm, key) });
      FieldValidator.checkOffset(nameOffset, arcSize);

      // 4 - Offset
      long offset = IntConverter.convertLittle(new byte[] { readByte(fm, key), readByte(fm, key), readByte(fm, key), readByte(fm, key) });
      FieldValidator.checkOffset(offset, arcSize);

      // 1 - Directory/File Flag
      int fileType = readByte(fm, key);

      if (fileType == 1) {
        // directory

        int curPos2 = (int) fm.getOffset();

        fm.seek(nameOffset);
        String dirName = readName(fm, key);

        if (!name.equals("")) {
          dirName = name + "\\" + dirName;
        }

        fm.seek(offset);
        readDirectory(fm, path, resources, key, dirName);
        fm.seek(curPos2);

      }
      else {
        // file

        int curPos2 = (int) fm.getOffset();

        fm.seek(nameOffset);
        String filename = readName(fm, key);

        if (!name.equals("")) {
          filename = name + "\\" + filename;
        }

        fm.seek(offset);

        // 4 - Offset
        offset = IntConverter.convertLittle(new byte[] { readByte(fm, key), readByte(fm, key), readByte(fm, key), readByte(fm, key) });
        FieldValidator.checkOffset(offset, arcSize);

        // 4 - Length
        long length = IntConverter.convertLittle(new byte[] { readByte(fm, key), readByte(fm, key), readByte(fm, key), readByte(fm, key) });
        FieldValidator.checkLength(length, arcSize);

        // 1 - Flag
        fm.skip(1);

        fm.seek(curPos2);

        //path,id,name,offset,length,decompLength,exporter
        resources[realNumFiles] = new Resource(path, filename, offset, length, length, exporter);

        TaskProgressManager.setValue(offset);
        realNumFiles++;

      }

    }

    fm.seek(curPos);

  }

  /**
  **********************************************************************************************

  **********************************************************************************************
  **/
  public String readName(FileManipulator fm, int key) throws Exception {
    String name = "";

    byte nameByte = readByte(fm, key);
    while (nameByte != 0) {
      name += (char) nameByte;
      nameByte = readByte(fm, key);
    }

    return name;
  }

}