
package org.watto.ge.plugin.archive;

import java.io.File;
import org.watto.Settings;
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
public class Plugin_BAG_3 extends ArchivePlugin {

  /**
  **********************************************************************************************

  **********************************************************************************************
  **/
  public Plugin_BAG_3() {

    super("BAG_3", "BAG_3");

    //         read write replace rename
    setProperties(true, false, false, false);

    setGames("Rugby Challenge 2006");
    setExtensions("bag");
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

      // Compression Tag
      int compression = fm.readInt();
      if (compression == 0 || compression == 2) {
        rating += 5;
      }

      if (compression == 0) {
        // Number Of Files
        if (FieldValidator.checkNumFiles(fm.readInt())) {
          rating += 5;
        }
      }
      else if (compression == 2) {
        // Decompressed Length
        if (FieldValidator.checkLength(fm.readInt())) {
          rating += 5;
        }

        // Compression tag
        if (fm.readString(1).equals("x")) {
          rating += 10;
        }
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

      ExporterPlugin exporter = Exporter_ZLib.getInstance();
      addFileTypes();

      // RESETTING THE GLOBAL VARIABLES

      FileManipulator fm = new FileManipulator(path, false);

      // 4 - Compression Header
      if (fm.readInt() == 2) {

        // 4 - decompressed length
        int decompLength = fm.readInt();
        FieldValidator.checkLength(decompLength);

        fm.close();

        //decompress the file first
        FileManipulator extDir = new FileManipulator(new File("temp" + File.separator + "bag_3_decompressed.dat"), true);
        String dirName = extDir.getFilePath();
        Resource directory = new Resource(path, dirName, 8, (int) path.length() - 8, decompLength);

        exporter.extract(directory, extDir);

        extDir.close();

        path = new File(dirName);

        // important for repacking!
        Settings.set("CurrentArchive", path.getAbsolutePath());

        fm = new FileManipulator(path, false);
        fm.skip(4); // skip the 4-byte compression header already read above
      }

      long arcSize = (int) fm.getLength();

      // 4 - Number Of Files
      int numFiles = fm.readInt();
      FieldValidator.checkNumFiles(numFiles);

      int relOffset = ((numFiles * 80) + 8);
      int padding = 32 - (relOffset % 32);
      if (padding < 32) {
        relOffset += padding;
      }

      Resource[] resources = new Resource[numFiles];

      TaskProgressManager.setMaximum(numFiles);

      // Loop through directory
      //int[] nameLengths = new int[numFiles];
      //for(int i=0;i<numFiles;i++){
      //  // 4 - Filename Length
      //  int nameLength = fm.readInt();
      //  FieldValidator.checkFilenameLength(nameLength);
      //  nameLengths[i] = nameLength;
      //  }
      fm.skip(numFiles * 4);

      // Loop through directory
      long[] lengths = new long[numFiles];
      for (int i = 0; i < numFiles; i++) {
        // 4 - File Length
        long length = fm.readInt();
        FieldValidator.checkLength(length, arcSize);
        lengths[i] = length;
      }

      // Loop through directory
      String[] names = new String[numFiles];
      for (int i = 0; i < numFiles; i++) {
        // X - Filename
        // 1 - null Filename Terminator
        // X - Filename including path
        // 1 - null Filename Terminator
        // 0-63 - null Padding to length 64
        String filename = fm.readNullString(64);
        FieldValidator.checkFilename(filename);
        names[i] = filename;
      }

      // Loop through directory
      for (int i = 0; i < numFiles; i++) {
        // 4 - File Offset
        long offset = fm.readInt() + relOffset;
        FieldValidator.checkOffset(offset, arcSize);

        long length = lengths[i];
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
