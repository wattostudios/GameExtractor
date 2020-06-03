
package org.watto.ge.plugin.archive;

import java.io.File;
import org.watto.task.TaskProgressManager;
import org.watto.datatype.Resource;
import org.watto.ge.helper.FieldValidator;
import org.watto.ge.helper.ResourceSorter_OffsetName;
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
public class Plugin_SFL_SFL10 extends ArchivePlugin {

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public Plugin_SFL_SFL10() {

    super("SFL_SFL10", "SFL_SFL10");

    //         read write replace rename
    setProperties(true, false, false, false);

    setGames("AGON");
    setExtensions("sfl");
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
      if (fm.readString(5).equals("SFL10")) {
        rating += 50;
      }

      long arcSize = fm.getLength();

      // Dir Offset
      if (FieldValidator.checkOffset(fm.readInt(), arcSize)) {
        rating += 5;
      }

      // Dir Offset
      if (FieldValidator.checkOffset(fm.readInt(), arcSize)) {
        rating += 5;
      }

      // Dir Offset
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

      // RESETTING THE GLOBAL VARIABLES

      FileManipulator fm = new FileManipulator(path, false);

      long arcSize = (int) fm.getLength();

      // 5 - Header (SFL10)
      fm.skip(5);

      // 4 - Folders Directory Offset
      int foldersDirOffset = fm.readInt();
      FieldValidator.checkOffset(foldersDirOffset, arcSize);

      // 4 - Files Directory Offset
      long dirOffset = fm.readInt();
      FieldValidator.checkOffset(dirOffset, arcSize);

      // 4 - Filename Directory Offset
      int filenameDirOffset = fm.readInt();
      FieldValidator.checkOffset(filenameDirOffset, arcSize);

      // 4 - Number Of Folders
      int numFolders = fm.readInt();
      FieldValidator.checkNumFiles(numFolders);

      // 4 - Number Of Files
      int numFiles = fm.readInt();
      FieldValidator.checkNumFiles(numFiles);

      // 4 - Filename Directory Length
      fm.seek(foldersDirOffset);

      Resource[] resources = new Resource[numFiles];

      TaskProgressManager.setMaximum(numFiles);

      // Loop through directory
      ResourceSorter_OffsetName[] folderPointers = new ResourceSorter_OffsetName[numFolders];
      for (int i = 0; i < numFolders; i++) {
        // 4 - Folder Name Offset (relative to the start of the filename directory)
        int nameOffset = fm.readInt() + filenameDirOffset;
        FieldValidator.checkOffset(nameOffset);

        // 4 - File ID (can be all 255's) (incremental from -1)
        // 4 - Parent Directory ID? (mostly all 255's)
        long currentPos = fm.getOffset() + 8;
        fm.seek(nameOffset);

        // X - Filename
        String name = fm.readNullString();
        FieldValidator.checkFilename(name);

        fm.seek(currentPos);

        // 4 - ID? of the first file in this folder (this field does not exist for the last folder entry)
        int firstFile = fm.readInt();
        if (i == numFolders - 1) {
          firstFile = 0;
        }

        folderPointers[i] = new ResourceSorter_OffsetName(firstFile, name);
      }

      java.util.Arrays.sort(folderPointers);

      fm.seek(dirOffset);

      // Loop through directory
      int[] nameOffsets = new int[numFiles];
      for (int i = 0; i < numFiles; i++) {
        // 4 - Filename Offset (relative to the start of the filename directory)
        int filenameOffset = fm.readInt() + filenameDirOffset;
        FieldValidator.checkOffset(filenameOffset);
        nameOffsets[i] = filenameOffset;

        // 1 - null
        // 4 - File ID (can be all 255's) (incremental from -1)
        fm.skip(5);

        // 4 - File Offset
        long offset = fm.readInt();
        FieldValidator.checkOffset(offset, arcSize);

        // 4 - File Length
        long length = fm.readInt();
        FieldValidator.checkLength(length, arcSize);

        //path,id,name,offset,length,decompLength,exporter
        resources[i] = new Resource(path, "", offset, length);

        TaskProgressManager.setValue(i);
      }

      int currentFolder = 0;
      int numLeft = numFiles;
      if (numFolders > 1) {
        numLeft = (int) folderPointers[1].getOffset();
      }
      String dirName = folderPointers[0].getName();

      // Loop through directory
      for (int i = 0; i < numFiles; i++) {
        fm.seek(nameOffsets[i]);

        // X - Filename
        // 1 - null Filename Terminator
        String filename = fm.readNullString();
        FieldValidator.checkFilename(filename);
        filename = dirName + "\\" + filename;

        resources[i].setName(filename);

        numLeft--;
        if (numLeft <= 0) {
          currentFolder++;
          if (currentFolder < numFolders) {
            dirName = folderPointers[currentFolder].getName();
          }
          else {
            dirName = "";
          }
          if (currentFolder + 1 < numFolders) {
            numLeft = (int) folderPointers[currentFolder + 1].getOffset();
          }
          else {
            numLeft = numFiles;
          }
        }
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
