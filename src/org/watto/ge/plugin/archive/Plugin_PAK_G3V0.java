
package org.watto.ge.plugin.archive;

import java.io.File;
import org.watto.component.WSPluginException;
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
import org.watto.io.converter.ByteConverter;
import org.watto.task.TaskProgressManager;

/**
**********************************************************************************************

**********************************************************************************************
**/
public class Plugin_PAK_G3V0 extends ArchivePlugin {

  int realNumFiles = 0;

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public Plugin_PAK_G3V0() {

    super("PAK_G3V0", "PAK_G3V0");

    //         read write replace rename
    setProperties(true, false, false, false);

    setGames("Gothic 3");
    setExtensions("pak");
    setPlatforms("PC");

    //setFileTypes("","",
    //             "",""
    //             );

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

      // Header
      if (fm.readString(4).equals("G3V0")) {
        rating += 50;
      }

      // null
      if (fm.readLong() == 0) {
        rating += 5;
      }

      fm.skip(8);

      long arcSize = fm.getLength();

      // Directory Offset
      if (FieldValidator.checkOffset(fm.readLong(), arcSize)) {
        rating += 5;
      }

      // Directory Offset
      if (FieldValidator.checkOffset(fm.readLong(), arcSize)) {
        rating += 5;
      }

      // Footer Offset
      if (FieldValidator.checkOffset(fm.readLong(), arcSize)) {
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

      //ExporterPlugin exporter = Exporter_ZLib.getInstance();

      // RESETTING GLOBAL VARIABLES
      realNumFiles = 0;

      FileManipulator fm = new FileManipulator(path, false);

      long arcSize = fm.getLength();

      // 4 - null
      // 4 - Header (G3V0)
      // 8 - null
      // 4 - Unknown (1)
      // 4 - Unknown
      fm.skip(24);

      // 8 - Directory Offset
      long dirOffset = fm.readLong();
      FieldValidator.checkOffset(dirOffset, arcSize);

      // 8 - Directory Offset
      // 8 - Footer Offset
      fm.seek(dirOffset);

      int numFiles = Archive.getMaxFiles();

      Resource[] resources = new Resource[numFiles];
      TaskProgressManager.setMaximum(numFiles);

      while (fm.getOffset() < arcSize - 4) {
        readDirectory(resources, fm, "", path, arcSize);
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
  public void readDirectory(Resource[] resources, FileManipulator fm, String parentDirName, File path, long arcSize) throws Exception {
    // 24 - Unknown
    // 8 - null
    fm.skip(32);

    // 1 - Entry Type (16=dir, 128=file)
    int type = ByteConverter.unsign(fm.readByte());

    // 1 - Compression Flag (0=Decompressed, 8=Compressed)
    // 2 - Unknown (2=Compressed OR Directory, 6=Decompressed)
    fm.skip(3);

    if (type == 16) {
      // directory

      // 4 - Directory Name Length (not including null)
      int dirNameLength = fm.readInt();
      FieldValidator.checkFilenameLength(dirNameLength);

      // X - Directory Name
      String dirName = parentDirName + fm.readString(dirNameLength) + "\\";

      // 1 - null Directory Name Terminator
      fm.skip(1);

      // 4 - Number of Sub Directories in this Directory
      int numSubDirs = fm.readInt();
      FieldValidator.checkNumFiles(numSubDirs + 1);

      int numFilesInDir = 0;
      if (numSubDirs == 0) {
        // 4 - Number of Files in this Directory
        numFilesInDir = fm.readInt();
      }

      for (int i = 0; i < numSubDirs; i++) {
        readDirectory(resources, fm, dirName, path, arcSize);
      }

      for (int i = 0; i < numFilesInDir; i++) {
        readDirectory(resources, fm, dirName, path, arcSize);
      }

    }
    else if (type == 128) {
      // file

      // 8 - File Offset
      long offset = fm.readLong();
      FieldValidator.checkOffset(offset, arcSize);

      // 8 - Compressed File Length
      long length = fm.readLong();
      FieldValidator.checkLength(length, arcSize);

      // 8 - Decompressed File Length
      long decompLength = fm.readLong();
      FieldValidator.checkLength(decompLength);

      // 4 - Filename Length (not including null)
      int filenameLength = fm.readInt();
      FieldValidator.checkFilenameLength(filenameLength);

      // X - Filename
      String filename = fm.readString(filenameLength);

      // 1 - null Filename Terminator
      fm.skip(1);

      // 4 - Absolute Filename Length (not including null)
      int absoluteFilenameLength = fm.readInt();
      FieldValidator.checkFilenameLength(absoluteFilenameLength);

      // X - Absolute Filename (eg D:\...)
      // 1 - null Absolute Filename Terminator
      fm.skip(absoluteFilenameLength + 1);

      //path,name,offset,length,decompLength,exporter
      resources[realNumFiles] = new Resource(path, filename, offset, length, decompLength);

      TaskProgressManager.setValue(realNumFiles);
      realNumFiles++;

    }
    else {
      throw new WSPluginException("Invalid Entry Type");
    }

  }

}
