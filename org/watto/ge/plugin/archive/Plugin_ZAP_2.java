
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
import org.watto.io.converter.ByteConverter;

/**
**********************************************************************************************

**********************************************************************************************
**/
public class Plugin_ZAP_2 extends ArchivePlugin {

  int realNumFiles = 0;

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public Plugin_ZAP_2() {

    super("ZAP_2", "ZAP_2");

    //         read write replace rename
    setProperties(true, false, false, false);

    setGames("Infernal");
    setExtensions("zap");
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

      fm.seek(arcSize - 20);

      // 4 - Folders Directory Offset
      long dirOffset = fm.readInt();
      FieldValidator.checkOffset(dirOffset, arcSize);

      fm.seek(dirOffset);

      int numFiles = Archive.getMaxFiles();

      Resource[] resources = new Resource[numFiles];
      TaskProgressManager.setMaximum(numFiles);

      readDirectory(resources, fm, "", path, arcSize);

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

    // 2 - Number Of Sub-Folders in this Folder
    short numSubDirs = fm.readShort();
    FieldValidator.checkNumFiles(numSubDirs);

    // 2 - Number Of Files in this Folder
    short numFilesInDir = fm.readShort();
    FieldValidator.checkNumFiles(numFilesInDir);

    // 4 - Offset to the FILES DIRECTORY that contains the details for the files in this folder (ignore if numFiles=0)
    long filesOffset = fm.readInt();
    FieldValidator.checkOffset(filesOffset, arcSize);

    // for each sub-folder in this folder
    long[] subDirOffsets = new long[numSubDirs];
    for (int i = 0; i < numSubDirs; i++) {
      // 4 - Folder Name Offset (relative to the start of the first field of this loop)
      long subDirOffset = fm.readInt();
      FieldValidator.checkOffset(subDirOffset, arcSize);
      subDirOffsets[i] = subDirOffset;
    }

    // for each sub-folder in this folder
    String[] subDirNames = new String[numSubDirs];
    for (int i = 0; i < numSubDirs; i++) {
      fm.seek(subDirOffsets[i]);

      // 1 - Folder Name Length
      int subDirNameLength = ByteConverter.unsign(fm.readByte());

      // X - Folder Name
      subDirNames[i] = parentDirName + fm.readString(subDirNameLength) + "\\";

      // 4 - Folders Directory Offset for the sub-folders in this folder
      long subDirOffset = fm.readInt();
      FieldValidator.checkOffset(subDirOffset, arcSize);
      subDirOffsets[i] = subDirOffset;
    }

    // loop through all the files in this directory
    fm.seek(filesOffset);
    readFiles(resources, fm, parentDirName, path, arcSize, numFilesInDir);

    // loop through all the subDirs in this directory
    for (int i = 0; i < numSubDirs; i++) {
      fm.seek(subDirOffsets[i]);
      readDirectory(resources, fm, subDirNames[i], path, arcSize);
    }

  }

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public void readFiles(Resource[] resources, FileManipulator fm, String parentDirName, File path, long arcSize, int numFilesInDir) throws Exception {
    long relOffset = fm.getOffset();

    // for each file in this folder
    long[] fileOffsets = new long[numFilesInDir];
    for (int i = 0; i < numFilesInDir; i++) {
      // 4 - Offset to file entry (relative to the start of the first field for this folder)
      long fileOffset = fm.readInt() + relOffset;
      FieldValidator.checkOffset(fileOffset, arcSize);
      fileOffsets[i] = fileOffset;
    }

    // for each file in this folder
    for (int i = 0; i < numFilesInDir; i++) {
      // 1 - Filename Length
      int filenameLength = ByteConverter.unsign(fm.readByte());

      // X - Filename
      String filename = parentDirName + fm.readString(filenameLength);

      // 4 - File Type Hash?
      fm.skip(4);

      // 4 - File Offset
      long offset = fm.readInt();
      FieldValidator.checkOffset(offset, arcSize);

      // 4 - Decompressed File Length
      long decompLength = fm.readInt();
      FieldValidator.checkLength(decompLength);

      // 4 - Compressed File Length
      long length = fm.readInt();
      FieldValidator.checkLength(length, arcSize);

      // 4 - Unknown (probably supposed to be a compression flag (0=Uncompressed, 1=Compressed) but isn't right)
      // 4 - File Type Hash?
      fm.skip(8);

      //path,name,offset,length,decompLength,exporter
      resources[realNumFiles] = new Resource(path, filename, offset, length, decompLength);

      TaskProgressManager.setValue(realNumFiles);
      realNumFiles++;
    }

  }

}
