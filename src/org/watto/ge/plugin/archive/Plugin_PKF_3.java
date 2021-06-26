
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
public class Plugin_PKF_3 extends ArchivePlugin {

  int realNumFiles = 0;

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public Plugin_PKF_3() {

    super("PKF_3", "PKF_3");

    //         read write replace rename
    setProperties(true, false, false, false);

    setGames("In-Fishermen Freshwater Trophies");
    setExtensions("pkf");
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

      long arcSize = fm.getLength();

      // Directory Length
      if (FieldValidator.checkLength(fm.readInt(), arcSize)) {
        rating += 5;
      }

      fm.skip(63);

      // null
      if (fm.readByte() == 0) {
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

      // 4 - Directory Length
      fm.skip(4);

      int numFiles = Archive.getMaxFiles();

      Resource[] resources = new Resource[numFiles];

      TaskProgressManager.setMaximum(arcSize);

      // 64 - Folder Name (null) (root = null)
      fm.skip(64);

      // 4 - Number of Files in this Folder
      int numFilesInDir = fm.readInt();
      FieldValidator.checkNumFiles(numFilesInDir);

      // 4 - Number of Sub-Folders in this Folder
      int numFoldersInDir = fm.readInt();
      FieldValidator.checkNumFiles(numFoldersInDir);

      // 4 - Offset to the First File in this Folder
      int filesOffset = fm.readInt();
      FieldValidator.checkOffset(filesOffset, arcSize);

      // 4 - Offset to the First Sub-Folder in this Folder
      int foldersOffset = fm.readInt();
      FieldValidator.checkOffset(foldersOffset, arcSize);

      readFolders(path, fm, resources, "", foldersOffset, numFoldersInDir);
      readFiles(path, fm, resources, "", filesOffset, numFilesInDir);

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
  public void readFiles(File path, FileManipulator fm, Resource[] resources, String dirName, int thisFilesOffset, int thisNumFilesInDir) throws Exception {
    long curPos = fm.getOffset();
    fm.seek(thisFilesOffset);

    long arcSize = (int) fm.getLength();

    // Loop through directory
    for (int i = 0; i < thisNumFilesInDir; i++) {
      // 64 - Filename (null)
      String filename = fm.readNullString(64);
      FieldValidator.checkFilename(filename);

      // 4 - File Length
      long length = fm.readInt();
      FieldValidator.checkLength(length, arcSize);

      // 4 - File Offset
      long offset = fm.readInt();
      FieldValidator.checkOffset(offset, arcSize);

      filename = dirName + filename;

      //path,id,name,offset,length,decompLength,exporter
      resources[realNumFiles] = new Resource(path, filename, offset, length);

      TaskProgressManager.setValue(offset);
      realNumFiles++;
    }

    fm.seek(curPos);
  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public void readFolders(File path, FileManipulator fm, Resource[] resources, String dirName, int thisFoldersOffset, int thisNumFoldersInDir) throws Exception {
    long curPos = fm.getOffset();
    fm.seek(thisFoldersOffset);

    long arcSize = (int) fm.getLength();

    // Loop through directory
    for (int i = 0; i < thisNumFoldersInDir; i++) {

      // 64 - Folder Name (null) (root = null)
      String folderName = fm.readNullString(64);
      FieldValidator.checkFilename(folderName);

      // 4 - Number of Files in this Folder
      int numFilesInDir = fm.readInt();
      FieldValidator.checkNumFiles(numFilesInDir);

      // 4 - Number of Sub-Folders in this Folder
      int numFoldersInDir = fm.readInt();
      FieldValidator.checkNumFiles(numFoldersInDir);

      // 4 - Offset to the First File in this Folder
      int filesOffset = fm.readInt();
      FieldValidator.checkOffset(filesOffset, arcSize);

      // 4 - Offset to the First Sub-Folder in this Folder
      int foldersOffset = fm.readInt();
      FieldValidator.checkOffset(foldersOffset, arcSize);

      readFolders(path, fm, resources, dirName + folderName + "\\", foldersOffset, numFoldersInDir);
      readFiles(path, fm, resources, dirName + folderName + "\\", filesOffset, numFilesInDir);
    }

    fm.seek(curPos);

  }

}
