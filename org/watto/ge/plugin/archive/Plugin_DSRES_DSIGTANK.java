
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
public class Plugin_DSRES_DSIGTANK extends ArchivePlugin {

  long dirOffset;
  long filesDirOffset;
  int realNumFiles;

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public Plugin_DSRES_DSIGTANK() {

    super("DSRES_DSIGTANK", "DSRES_DSIGTANK");

    //         read write replace rename
    setProperties(true, false, false, false);

    setGames("Copperhead Retaliation");
    setExtensions("dsres");
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

      // Header
      if (fm.readString(8).equals("DSigTank")) {
        rating += 50;
      }

      // Version
      if (fm.readShort() == 2) {
        rating += 5;
      }

      // Version
      if (fm.readShort() == 1) {
        rating += 5;
      }

      long arcSize = fm.getLength();

      // Directory Offset
      if (FieldValidator.checkOffset(fm.readInt(), arcSize)) {
        rating += 5;
      }

      // Directory Offset
      if (FieldValidator.checkOffset(fm.readInt(), arcSize)) {
        rating += 5;
      }

      fm.skip(4);

      // Number Of Files
      if (FieldValidator.checkNumFiles(fm.readInt())) {
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
      dirOffset = 0;
      filesDirOffset = 0;
      realNumFiles = 0;

      FileManipulator fm = new FileManipulator(path, false);

      long arcSize = fm.getLength();

      // 8 - Header (DSigTank)
      // 2 - Version Major (2)
      // 2 - Version Minor (1)
      fm.skip(12);

      // 4 - Folders Directory Offset
      dirOffset = fm.readInt();
      FieldValidator.checkOffset(dirOffset, arcSize);

      // 4 - Files Directory Offset?
      filesDirOffset = fm.readInt();
      FieldValidator.checkOffset(dirOffset, arcSize);

      // 4 - Length of all Directories (ie ArchiveSize - FoldersDirectoryOffset)
      fm.skip(4);

      // 4 - Number Of Files?
      int numFiles = fm.readInt();
      FieldValidator.checkNumFiles(numFiles);

      Resource[] resources = new Resource[numFiles];
      TaskProgressManager.setMaximum(numFiles);

      fm.seek(dirOffset);

      // 4 - Number Of Folders
      int numFolders = fm.readInt();
      FieldValidator.checkNumFiles(numFolders);

      // Loop through the folder offsets
      int[] offsets = new int[numFolders];
      for (int i = 0; i < numFolders; i++) {
        // 4 - Folder Offset (relative to the folders directory offset)
        int offset = (int) (fm.readInt() + dirOffset);
        FieldValidator.checkOffset(offset, arcSize);
        offsets[i] = offset;
      }

      // read the folders
      for (int i = 0; i < numFolders; i++) {
        readDirectory(resources, fm, path, "");
      }

      /*
       * // Loop through directory for(int i=0;i<numFiles;i++){
       * 
       * // 4 - File Offset
       * 
       * 
       * // 4 - File Length
       * 
       * 
       * // X - Filename (null) String filename = fm.readNullString(); FieldValidator.checkFilename(filename);
       * 
       * 
       * String filename = Resource.generateFilename(i);
       * 
       * //path,name,offset,length,decompLength,exporter resources[i] = new
       * Resource(path,filename,offset,length);
       * 
       * TaskProgressManager.setValue(i); }
       * 
       * 
       * resources = resizeResources(resources,realNumFiles);
       * calculateFileSizes(resources,arcSize);
       */

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
  public void readDirectory(Resource[] resources, FileManipulator fm, File path, String parentDirName) throws Exception {

    long arcSize = (int) fm.getLength();

    // 4 - Parent Directory Offset (relative to the folders directory offset)
    fm.skip(4);

    // 4 - Number Of Files in this folder
    int numFilesInFolder = fm.readInt();
    FieldValidator.checkNumFiles(numFilesInFolder);

    // 8 - Hash
    fm.skip(8);

    // 2 - Folder Name Length
    short folderNameLength = fm.readShort();
    FieldValidator.checkFilenameLength(folderNameLength);

    // X - Folder Name
    String folderName = fm.readString(folderNameLength);

    // 0-3 - null Padding to a multiple of 4 bytes (including the folder name length)
    int paddingSize = 4 - ((folderNameLength + 2) % 4);
    if (paddingSize != 4) {
      fm.skip(paddingSize);
    }

    // for each file in this folder
    int[] innerOffsets = new int[numFilesInFolder];
    for (int j = 0; j < numFilesInFolder; j++) {
      // 4 - File Entry Offset (relative to the folders directory offset)
      int innerOffset = (int) (fm.readInt() + dirOffset);
      FieldValidator.checkOffset(innerOffset, arcSize);
      innerOffsets[j] = innerOffset;
    }

    // go to each offset
    for (int j = 0; j < numFilesInFolder; j++) {
      int innerOffset = innerOffsets[j];
      fm.seek(innerOffset);

      if (innerOffset < filesDirOffset) {
        // directory
        readDirectory(resources, fm, path, parentDirName + folderName + "\\");
      }
      else {
        // file

        // 4 - Parent Directory Offset (relative to the folders directory offset)
        fm.skip(4);

        // 4 - File Length?
        long length = fm.readInt();
        FieldValidator.checkLength(length, arcSize);

        // 4 - File Offset?
        long offset = fm.readInt();
        FieldValidator.checkOffset(offset, arcSize);

        // 12 - Hash
        fm.skip(12);

        // 4 - File Type? (0/1)
        int fileType = fm.readInt();

        // 2 - Filename Length
        short filenameLength = fm.readShort();
        FieldValidator.checkFilenameLength(filenameLength);

        // X - Filename
        String filename = parentDirName + folderName + "\\" + fm.readString(filenameLength);

        // 0-3 - null Padding to a multiple of 4 bytes (including the filename length)
        int filenamePaddingSize = 4 - ((filenameLength + 2) % 4);
        if (filenamePaddingSize != 4) {
          fm.skip(filenamePaddingSize);
        }

        if (fileType == 1) {
          // 4 - Unknown
          // 4 - Unknown (16384)
          // 4 - Unknown
          // 4 - Unknown
          // 8 - null
          fm.skip(24);
        }

        //path,name,offset,length,decompLength,exporter
        resources[realNumFiles] = new Resource(path, filename, offset, length);

        TaskProgressManager.setValue(realNumFiles);
        realNumFiles++;

      }
    }

  }

}
