
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
public class Plugin_Z extends ArchivePlugin {

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public Plugin_Z() {

    super("Z", "Z");

    //         read write replace rename
    setProperties(true, false, false, false);

    setGames("F-22 Total Air War");
    setExtensions("z");
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

      fm.skip(8);

      // null
      if (fm.readInt() == 0) {
        rating += 5;
      }

      fm.skip(6);

      long arcSize = fm.getLength();

      // Archive Size
      if (FieldValidator.checkEquals(fm.readInt(), arcSize)) {
        rating += 5;
      }

      fm.skip(4);

      // First File Offset
      if (fm.readInt() == 255) {
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

      // 4 - Header?
      // 4 - Unknown
      // 4 - null
      // 2 - Unknown (76)
      // 4 - Unknown
      // 4 - Archive Length
      // 4 - Unknown
      // 4 - First File Offset (255)
      // 3 - null
      // 4 - First File Offset (255)
      // 4 - null
      fm.skip(41);

      // 4 - Folders Directory Offset
      int folderDirOffset = fm.readInt();
      FieldValidator.checkOffset(folderDirOffset, arcSize);

      // 4 - Folders Directory Length
      fm.skip(4);

      // 2 - Number Of Folders
      short numFolders = fm.readShort();
      FieldValidator.checkNumFiles(numFolders);

      // 4 - Files Directory Offset
      long dirOffset = fm.readInt();
      FieldValidator.checkOffset(dirOffset, arcSize);

      // 4 - Files Directory Length
      int dirLength = fm.readInt();
      FieldValidator.checkLength(dirLength, arcSize);

      // 196 - null Padding to offset 255
      fm.seek(folderDirOffset);

      // Loop through directory
      String[] dirNames = new String[numFolders];
      for (int i = 0; i < numFolders; i++) {
        // 2 - Unknown
        // 2 - Unknown
        fm.skip(4);

        // 2 - Folder Name Length
        int folderNameLength = fm.readShort();

        // X - Folder Name
        String folderName = fm.readString(folderNameLength);
        if (folderNameLength > 0) {
          folderName += "\\";
        }
        dirNames[i] = folderName;

        // 5 - null
        fm.skip(5);
      }

      fm.seek(dirOffset);

      int numFiles = Archive.getMaxFiles();

      Resource[] resources = new Resource[numFiles];

      TaskProgressManager.setMaximum(arcSize);

      // Loop through directory
      int realNumFiles = 0;
      long dirEndPos = dirOffset + dirLength;
      while (fm.getOffset() < dirEndPos) {
        // 1 - null
        fm.skip(1);

        // 2 - Folder ID (ID of the folder that this file belongs to)
        int dirID = fm.readShort();

        // 4 - Decompressed Length
        int decompLength = fm.readInt();
        FieldValidator.checkLength(decompLength);

        // 4 - Compressed Length
        long length = fm.readInt();
        FieldValidator.checkLength(length, arcSize);

        // 4 - File Offset
        long offset = fm.readInt();
        FieldValidator.checkOffset(offset, arcSize);

        // 4 - Unknown
        // 4 - Unknown (32/128)
        // 4 - Unknown
        // 2 - null
        fm.skip(14);

        // 1 - Filename Length
        int filenameLength = ByteConverter.unsign(fm.readByte());

        // X - Filename
        String filename = dirNames[dirID] + fm.readString(filenameLength);

        // 5 - null
        // 4 - null
        // 4 - null
        fm.skip(13);

        //path,id,name,offset,length,decompLength,exporter
        resources[realNumFiles] = new Resource(path, filename, offset, length);

        TaskProgressManager.setValue(offset);
        realNumFiles++;
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
