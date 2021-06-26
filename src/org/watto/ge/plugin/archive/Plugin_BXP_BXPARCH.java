
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
public class Plugin_BXP_BXPARCH extends ArchivePlugin {

  int realNumFiles = 0;
  long firstFileOffset = 0;

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public Plugin_BXP_BXPARCH() {

    super("BXP_BXPARCH", "BXP_BXPARCH");

    //         read write replace rename
    setProperties(true, false, false, false);

    setGames("3D Sex Villa");
    setExtensions("bxp");
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
      if (fm.readString(8).equals("BXP" + (byte) 2 + "ARCH")) {
        rating += 50;
      }

      long arcSize = fm.getLength();

      // Archive Size
      if (fm.readLong() + 32 == arcSize) {
        rating += 5;
      }

      fm.skip(8);

      // null
      if (fm.readLong() == 0) {
        rating += 5;
      }

      // Header 2
      if (fm.readString(8).equals("BXP" + (byte) 2 + "INDX")) {
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

      // 8 - Header ("BXP" + (byte)2 + "ARCH")
      // 8 - Archive Length [+32]
      // 8 - Padding (88,39,255,135,206,225,196,1)
      // 8 - null

      // 8 - Header ("BXP" + (byte)2 + "INDX")
      // 8 - Unknown
      // 8 - Padding (88,39,255,135,206,225,196,1)
      // 8 - null
      // 16 - CRC?
      // 16 - CRC? (same as above field)

      // 8 - Header ("BXP" + (byte)2 + "TREE")
      fm.skip(104);

      // 8 - Offset to the "// FILE DATA" [+128] (ie relative to "// DIRECTORIES")
      firstFileOffset = fm.readLong() + 128;
      FieldValidator.checkOffset(firstFileOffset, arcSize);

      // 8 - Padding (88,39,255,135,206,225,196,1)
      // 8 - null
      fm.skip(16);

      int numFiles = Archive.getMaxFiles();

      Resource[] resources = new Resource[numFiles];

      TaskProgressManager.setMaximum(arcSize);

      readDirectory(path, fm, resources, "");

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
  public void readDirectory(File path, FileManipulator fm, Resource[] resources, String dirName) throws Exception {

    long arcSize = fm.getLength();

    // 8 - Header ("BXP" + (byte)2 + "SDIR")
    // 8 - Unknown Length/Offset
    // 16 - null
    fm.skip(32);

    // 4 - Number Of Sub-Directories in this directory
    int numDirs = fm.readInt();
    FieldValidator.checkNumFiles(numDirs + 1);

    // 4 - Number Of Files in this directory
    int numFiles = fm.readInt();
    FieldValidator.checkNumFiles(numFiles + 1);

    // 8 - Length Of Filename Directory Component (length of subDirName+null + fileNames+nulls)
    // 16 - null
    fm.skip(24);

    // for each sub-directory in this directory
    long[] dirOffsets = new long[numDirs];
    String[] dirNames = new String[numDirs];
    for (int i = 0; i < numDirs; i++) {
      long offset = fm.getOffset();

      // 4 - null
      fm.skip(4);

      // 4 - Relative Offset To This Directory? (32) (relative to the start of this subDirEntry)
      offset += fm.readInt();
      FieldValidator.checkOffset(offset, arcSize);
      dirOffsets[i] = offset;

      // 4 - Hash?
      fm.skip(4);

      // X - Sub-Directory Name
      // 1 - null Sub-Directory Name Terminator
      String subDirName = fm.readNullString();
      FieldValidator.checkFilename(subDirName);
      dirNames[i] = dirName + subDirName + "\\";

      // X - null Padding to a multiple of 32 bytes
      int paddingSize = 32 - ((dirName.length() + 13) % 32);
      if (paddingSize < 32) {
        fm.skip(paddingSize);
      }
    }

    // for each file in this directory
    long filenameDirOffset = fm.getOffset() + (numFiles * 4) + (numFiles * 64);
    long[] nameOffsets = new long[numFiles];
    int startRealNumFiles = realNumFiles;
    for (int i = 0; i < numFiles; i++) {
      // 8 - Filename Offset (relative to the start of the filename directory)
      long filenameOffset = (int) (fm.readLong()) + filenameDirOffset;
      FieldValidator.checkOffset(filenameOffset, arcSize);
      nameOffsets[i] = filenameOffset;

      // 8 - null
      // 16 - CRC?
      // 4 - null
      // 4 - Unknown (1)
      // 8 - null
      fm.skip(40);

      // 8 - File Length (not including the file header fields?)
      long length = (int) fm.readLong();
      FieldValidator.checkLength(length, arcSize);

      // 8 - File Offset (relative to the start of the file data)
      long offset = fm.readLong() + firstFileOffset + 64 + 32;
      FieldValidator.checkOffset(offset, arcSize);

      //path,id,name,offset,length,decompLength,exporter
      resources[realNumFiles] = new Resource(path, dirName, offset, length);

      TaskProgressManager.setValue((int) offset);
      realNumFiles++;
    }

    for (int i = 0; i < numFiles; i++) {
      fm.seek(nameOffsets[i]);

      // X - Filename (null)
      String filename = fm.readNullString();
      FieldValidator.checkFilename(filename);

      resources[startRealNumFiles + i].setFilename(filename); // setFilename so we keep the directory name
    }

    for (int i = 0; i < numDirs; i++) {
      fm.seek(dirOffsets[i]);
      readDirectory(path, fm, resources, dirNames[i]);
    }

  }

}
