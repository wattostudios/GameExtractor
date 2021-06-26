
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
public class Plugin_BBA_BAF extends ArchivePlugin {

  /**
  **********************************************************************************************

  **********************************************************************************************
  **/
  public Plugin_BBA_BAF() {

    super("BBA_BAF", "BBA_BAF");

    //         read write replace rename
    setProperties(true, false, false, false);

    setGames("Settlers: Heritage Of Kings");
    setExtensions("bba");
    setPlatforms("PC");

    setFileTypes("anm", "Animation",
        "bik", "Bink Video",
        "dds", "Direct X Image",
        "dff", "Model",
        "fdb", "Texture Database",
        "fx", "Visual Effects",
        "lua", "LUA Script",
        "met", "Font Metrics",
        "uva", "Animation Helper",
        "bin", "Map Information");

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
      if (fm.readString(3).equals("BAF")) {
        rating += 50;
      }

      // version
      if (fm.readByte() == 2) {
        rating += 5;
      }

      long arcSize = fm.getLength();

      // archive size [+8]
      if (fm.readInt() + 8 == arcSize) {
        rating += 5;
      }

      // BAH Header
      if (fm.readString(3).equals("BAH")) {
        rating += 5;
      }

      // version
      if (fm.readByte() == 2) {
        rating += 5;
      }

      // BAH length (8)
      if (fm.readInt() == 8) {
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

      // 3 - Header (BAF)
      // 1 - Version (2)
      // 4 - Archive Length [+8]

      // 3 - BAH Header (BAH)
      // 1 - BAH Version (2)
      // 4 - BAH Length (8)
      // 8 - Unknown (BAH Data)

      // 3 - File Data Header (BAf)
      // 1 - File Data Version (2)
      fm.skip(28);

      // 4 - File Data Length
      // X - File Data
      int fileDataSize = fm.readInt();
      FieldValidator.checkLength(fileDataSize, arcSize);
      fm.skip(fileDataSize);

      // 3 - Directory Header (BAd)
      // 1 - Directory Version (2)
      // 4 - Directory Length
      // X - Directory Data

      //   3 - File Entries Header (BAe)
      //   1 - File Entries Version (2)
      //   4 - File Entries Length

      //   X - File Entries Data
      //     4 - Compression Header (104329917)
      //     4 - Compressed Data Length (File Entries Length - 8)
      //     4 - Compressed Data Length [+2]
      fm.skip(28);

      //     4 - Decompressed Size
      int dirLength = fm.readInt();
      //System.out.println(dirLength);
      FieldValidator.checkLength(dirLength, arcSize);

      //     4 - Unknown
      fm.skip(4);

      //     X - Compressed Directory Data
      long dirOffset = (int) fm.getOffset();
      fm.close();

      // Decompress the directory
      FileManipulator extDir = new FileManipulator(new File("temp" + File.separator + "bba_directory_decompressed.dat"), true);
      String dirName = extDir.getFilePath();
      Resource directory = new Resource(path, dirName, dirOffset, dirLength, dirLength * 20);

      ExporterPlugin exporter = Exporter_ZLib.getInstance();
      exporter.extract(directory, extDir);

      extDir.close();

      // Now open the directory and read it
      fm = new FileManipulator(new File(dirName), false);

      //System.out.println(dirName);

      int numFiles = fm.readInt();
      FieldValidator.checkNumFiles(numFiles);

      Resource[] resources = new Resource[numFiles];
      TaskProgressManager.setMaximum(numFiles);

      // Loop through directory
      int realNumFiles = 0;
      for (int i = 0; i < numFiles; i++) {
        // 4 - File/Directory ID
        int id = fm.readInt();

        if (id == 1) {
          // directory

          // 8 - null
          fm.skip(8);

          // 2 - Directory Name Length
          int dirNameLength = fm.readShort();
          int padding = (4 - (dirNameLength % 4));

          // 2 - Number of characters from the start of the dirname that provide the parent directory
          // 4 - null
          // 4 - Unknown
          // 8 - null
          fm.skip(18 + dirNameLength + padding);

          // X - Directory Name (null)
          //System.out.println(fm.readString(dirNameLength));

          // 0-3 - Padding to multiple of 4 bytes
          //fm.skip(padding);

        }
        else {
          // file

          // 4 - Offset
          long offset = fm.readInt();

          // 4 - Decompressed File Size
          long length = fm.readInt();

          // 2 - Filename Length
          int filenameLength = fm.readShort();
          int padding = (4 - (filenameLength % 4));

          // 2 - Number of characters from the start of the filename that provide the directory
          // 4 - Padding (all 255's)
          // 4 - Unknown (something to do with the offset to the next file entry)
          // 8 - Unknown
          fm.skip(18);

          // X - Filename (null)
          String filename = fm.readString(filenameLength);
          //System.out.println(filename);

          // 0-3 - Padding to multiple of 4 bytes
          fm.skip(padding);

          if (id == 2) {
            // compressed
            offset += 20;

            //path,id,name,offset,length,decompLength,exporter
            resources[realNumFiles] = new Resource(path, filename, offset, length, length, exporter);
          }
          else {
            // uncompressed

            //path,id,name,offset,length,decompLength,exporter
            resources[realNumFiles] = new Resource(path, filename, offset, length);
          }

          TaskProgressManager.setValue(i);
          realNumFiles++;
        }
      }

      if (realNumFiles < numFiles) {
        resources = resizeResources(resources, realNumFiles);
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
