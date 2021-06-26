
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
public class Plugin_PKG_4 extends ArchivePlugin {

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public Plugin_PKG_4() {

    super("PKG_4", "PKG_4");

    //         read write replace rename
    setProperties(true, false, false, false);

    setGames("Spider-Man: The Movie");
    setExtensions("pkg"); // MUST BE LOWER CASE
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

      long arcSize = fm.getLength();

      // 4 - Number Of Folders
      if (FieldValidator.checkNumFiles(fm.readInt())) {
        rating += 5;
      }

      // 4 - Number Of Files
      if (FieldValidator.checkNumFiles(fm.readInt())) {
        rating += 5;
      }

      // 4 - File Data Length
      if (FieldValidator.checkLength(fm.readInt(), arcSize)) {
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

      FileManipulator fm = new FileManipulator(path, false);

      long arcSize = fm.getLength();

      // 4 - Number Of Folders
      int numFolders = fm.readInt();
      FieldValidator.checkNumFiles(numFolders);

      // 4 - Number Of Files
      int numFiles = fm.readInt();
      FieldValidator.checkNumFiles(numFiles);

      // 4 - File Data Length
      fm.skip(4);

      Resource[] resources = new Resource[numFiles];
      TaskProgressManager.setMaximum(numFiles);

      // Skip the folders directory, to get the files first
      fm.skip(numFolders * 68 + numFiles * 4);

      int relOffset = 12 + (numFolders * 68) + (numFiles * 52);

      // Loop through the files directory
      for (int i = 0; i < numFiles; i++) {

        // 40 - Filename (null terminated, filled with junk)
        String filename = fm.readNullString(40);
        FieldValidator.checkFilename(filename);

        // 4 - File Length
        int length = fm.readInt();
        FieldValidator.checkLength(length, arcSize);

        // 4 - File Offset (relative to the start of the file data)
        int offset = fm.readInt() + relOffset;
        FieldValidator.checkOffset(offset, arcSize);

        //path,name,offset,length,decompLength,exporter
        resources[i] = new Resource(path, filename, offset, length);

        TaskProgressManager.setValue(i);
      }

      // Go back and work out the directories
      fm.seek(12);

      for (int i = 0; i < numFolders; i++) {
        // 64 - Folder Name (null terminated, filled with junk)
        String folderName = fm.readNullString(64);
        FieldValidator.checkFilename(folderName);

        // 4 - Number Of Files in this Folder
        int numFilesInFolder = fm.readInt();
        FieldValidator.checkNumFiles(numFilesInFolder);

        for (int j = 0; j < numFilesInFolder; j++) {
          // 4 - File Entry Number (0 = the first file entry)
          int entryNumber = fm.readInt();
          FieldValidator.checkLength(entryNumber, numFiles);

          Resource resource = resources[entryNumber];
          resource.setName(folderName + resource.getName());
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
