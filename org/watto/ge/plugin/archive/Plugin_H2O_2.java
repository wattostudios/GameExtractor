
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
import org.watto.io.converter.ByteConverter;

/**
**********************************************************************************************

**********************************************************************************************
**/
public class Plugin_H2O_2 extends ArchivePlugin {

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public Plugin_H2O_2() {

    super("H2O_2", "H2O_2");

    //         read write replace rename
    setProperties(true, false, false, false);

    setGames("Dragonshard");
    setExtensions("h2o");
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
      if (fm.readString(8).equals("LIQDLH2O")) {
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

      // 8 - Header (LIQDLH2O)
      // 4 - Version (8.0 - float)
      fm.skip(12);

      // X - Comments
      // 1 - Comments Terminator (26)
      while (ByteConverter.unsign(fm.readByte()) != 26) {
        // repeat read
      }

      // 4 - Version (8)
      fm.skip(4);

      // 4 - Number Of Files
      int numFiles = fm.readInt();
      FieldValidator.checkNumFiles(numFiles);

      String[] names = readFilenames(path, numFiles);

      // 8 - Unknown
      // 8 - Decompressed File Data Size
      // 8 - Decompressed File Data Size
      // 8 - null
      fm.skip(32);

      long arcSize = fm.getLength();

      Resource[] resources = new Resource[numFiles];
      TaskProgressManager.setMaximum(numFiles);

      // Loop through directory
      int realNumFiles = 0;
      for (int i = 0; i < numFiles; i++) {
        // 4 - Compression Tag (0=Uncompressed, #=Compressed)
        // 4 - Directory Name Index (-1 for no directory)
        fm.skip(8);

        // 4 - Filename Index (-1 for blank file entries)
        int filenameID = fm.readInt();

        // 4 - File ID (incremental from 0)
        fm.skip(4);

        // 4 - Decompressed File Size
        int decompLength = fm.readInt();

        // 4 - Compressed File Size
        long length = fm.readInt();
        FieldValidator.checkLength(length, arcSize);

        // 8 - File Offset
        long offset = fm.readInt();
        FieldValidator.checkOffset(offset, arcSize);

        // 4 - Checksum CRC32
        // 2 - Unknown
        // 2 - Unknown
        fm.skip(12);

        String filename;
        if (names != null && filenameID > -1) {
          filename = names[filenameID];
        }
        else {
          filename = Resource.generateFilename(realNumFiles);
        }

        if (length > 0) {
          //path,id,name,offset,length,decompLength,exporter
          resources[realNumFiles] = new Resource(path, filename, offset, length, decompLength);

          TaskProgressManager.setValue(i);
          realNumFiles++;
        }
      }

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
  public String[] readFilenames(File path, int numFiles) {
    try {
      path = getDirectoryFile(path, "lqr");

      FileManipulator fm = new FileManipulator(path, false);

      String[] names = new String[numFiles];

      fm.seek(16);
      int filenameDirLength = fm.readInt();
      FieldValidator.checkLength(filenameDirLength);

      // determining the size of the field before the filename length
      fm.seek(80);
      int beforeFieldSize = 20;

      if (fm.readInt() == 0) {
        beforeFieldSize = 20;
      }
      else if (fm.readInt() == 0) {
        beforeFieldSize = 24;
      }
      else {
        fm.seek(108);
        if (fm.readInt() == 0) {
          beforeFieldSize = 48;
        }
      }

      // 64 - Unknown
      fm.seek(64);

      for (int i = 0; i < numFiles; i++) {
        // 4 - Unknown
        // 4 - Unknown
        // 4 - Unknown
        // 4 - Unknown
        // 4 - Filename ID (incremental from 0)
        fm.skip(beforeFieldSize);

        // 2 - Unknown (1)
        while (ByteConverter.unsign(fm.readByte()) == 0 && fm.getOffset() < filenameDirLength) {
          fm.skip(4);
        }

        if (fm.getOffset() >= filenameDirLength) {
          return names;
        }

        fm.skip(1);

        // 4 - Filename Length [*2 for unicode] (including null terminator)
        int filenameLength = fm.readInt() - 1;
        FieldValidator.checkFilenameLength(filenameLength);

        // X - Filename (unicode text)
        names[i] = fm.readUnicodeString(filenameLength);
        //System.out.println(names[i]);

        // 2 - null Filename Terminator
        fm.skip(2);
      }

      fm.close();

      return names;
    }
    catch (Throwable t) {
      t.printStackTrace();
      return null;
    }
  }

}
