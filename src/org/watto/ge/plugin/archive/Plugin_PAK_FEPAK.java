
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
import org.watto.ge.plugin.exporter.Exporter_ZLib;
import org.watto.io.FileManipulator;

/**
**********************************************************************************************

**********************************************************************************************
**/
public class Plugin_PAK_FEPAK extends ArchivePlugin {

  int realNumFiles = 0;

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public Plugin_PAK_FEPAK() {

    super("PAK_FEPAK", "PAK_FEPAK");

    //         read write replace rename
    setProperties(true, false, false, false);

    setGames("Lagsters");
    setExtensions("pak");
    setPlatforms("PC");

  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public void analyseDirectory(FileManipulator fm, File path, Resource[] resources, String dirName) throws Exception {
    long arcSize = fm.getLength();

    // 64 - Filename / Directory Name (null)
    String filename = fm.readNullString(64);
    FieldValidator.checkFilename(filename);

    // 4 - Directory/File Identifier
    int entryType = fm.readInt();

    if (entryType == 3) {
      // Directory

      // 4 - null
      // 4 - null
      // 4 - null
      fm.skip(12);

      // 4 - Number Of Files In This Directory
      int numFilesInDir = fm.readInt();
      FieldValidator.checkNumFiles(numFilesInDir);

      for (int i = 0; i < numFilesInDir; i++) {
        analyseDirectory(fm, path, resources, dirName + filename + "/");
      }

    }

    else {
      // File

      // 4 - Decompressed File Length
      int decompLength = fm.readInt();

      // 4 - Compressed File Length
      long length = fm.readInt();
      FieldValidator.checkLength(length, arcSize);

      // 4 - File Offset
      long offset = fm.readInt();
      FieldValidator.checkOffset(offset, arcSize);

      //path,id,name,offset,length,decompLength,exporter
      resources[realNumFiles] = new Resource(path, dirName + filename, offset, length, decompLength);

      if (decompLength != length) {
        resources[realNumFiles].setExporter(Exporter_ZLib.getInstance());
      }

      realNumFiles++;
      TaskProgressManager.setValue(offset);

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
      if (fm.readString(5).equals("FEPAK")) {
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
      realNumFiles = 0;

      FileManipulator fm = new FileManipulator(path, false);

      long arcSize = (int) fm.getLength();

      // 5 - Header (FEPAK)
      fm.skip(5);

      // 4 - Directory Offset
      long dirOffset = fm.readInt();
      FieldValidator.checkOffset(dirOffset);

      int numFiles = Archive.getMaxFiles();

      fm.seek(dirOffset + 80);

      // 4 - numberOfFilesInDirectory
      int numFilesInDir = fm.readInt();
      FieldValidator.checkNumFiles(numFilesInDir);

      Resource[] resources = new Resource[numFiles];

      TaskProgressManager.setMaximum(arcSize);

      for (int i = 0; i < numFilesInDir; i++) {
        analyseDirectory(fm, path, resources, "");
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

}
