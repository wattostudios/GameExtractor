
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
public class Plugin_XBR_XOBX extends ArchivePlugin {

  /**
  **********************************************************************************************

  **********************************************************************************************
  **/
  public Plugin_XBR_XOBX() {

    super("XBR_XOBX", "XBR_XOBX");

    //         read write replace rename
    setProperties(true, false, false, false);

    setGames("Azurik: Rise Of Perathia");
    setExtensions("xbr");
    setPlatforms("XBox");

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
      if (fm.readString(4).equals("xobx")) {
        rating += 50;
      }

      //long arcSize = fm.getLength();

      // Version? (4)
      if (fm.readInt() == 4) {
        rating += 5;
      }

      // null
      if (fm.readInt() == 0) {
        rating += 5;
      }

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

      // NOTE - Compressed file MUST know their DECOMPRESSED LENGTH
      //      - Uncompressed files MUST know their LENGTH

      addFileTypes();

      // RESETTING THE GLOBAL VARIABLES

      FileManipulator fm = new FileManipulator(path, false);

      long arcSize = fm.getLength();

      // 4 - Header (xobx)
      // 4 - Version? (4)
      // 4 - null
      fm.skip(12);

      // 4 - Number Of Files
      int numFiles = fm.readInt();
      FieldValidator.checkNumFiles(numFiles);

      // 4 - First File Offset (8192)
      int firstFileOffset = fm.readInt();
      FieldValidator.checkOffset(firstFileOffset, arcSize);

      // 4 - Number Of File Types
      // 4 - Offset To File Type Directory
      // 4 - Number Of Unknown Items
      // 4 - Offset To Unknown Item Directory
      // 4 - null
      // 4 - Filename Details Directory Offset
      fm.skip(24);

      // 4 - Number Of Filename Details
      int numFilenameDetails = fm.readInt();
      FieldValidator.checkNumFiles(numFilenameDetails);

      // 4 - Filename Details Directory Offset
      int filenameDetailsDirOffset = fm.readInt();
      FieldValidator.checkOffset(filenameDetailsDirOffset, arcSize);

      // 4 - Filename Directory Offset
      int filenameDirOffset = fm.readInt();
      FieldValidator.checkOffset(filenameDirOffset, arcSize);

      // 4 - Filename Directory Length
      fm.skip(4);

      long dirOffset = fm.getOffset();

      Resource[] resources = new Resource[numFiles];

      TaskProgressManager.setMaximum(numFiles);

      fm.seek(filenameDetailsDirOffset);

      // Loop through Filename Details directory
      int[] fileIDs = new int[numFilenameDetails];
      for (int i = 0; i < numFilenameDetails; i++) {
        // 4 - File ID that this filename belong to
        fileIDs[i] = fm.readInt();

        // 4 - Filename Offset (Relative to the start of the filename directory)
        fm.skip(4);
      }

      fm.seek(filenameDirOffset);

      // Loop through Filename directory
      String[] names = new String[numFiles];
      for (int i = 0; i < numFilenameDetails; i++) {
        // X - Filename (null)
        String filename = fm.readNullString();
        FieldValidator.checkFilename(filename);
        names[fileIDs[i]] = filename;
      }

      fm.seek(dirOffset);

      // Loop through directory
      for (int i = 0; i < numFiles; i++) {
        // 4 - File Offset (Relative to the first file offset)
        long offset = fm.readInt() + firstFileOffset;
        FieldValidator.checkOffset(offset, arcSize);

        // 4 - File Length
        long length = fm.readInt();
        FieldValidator.checkLength(length, arcSize);

        // 4 - File Type Name
        String fileType = fm.readString(4);

        // 4 - File Type ID (8=wave, 16=sdsr 128=surf)
        fm.skip(4);

        String filename = names[i];
        if (filename == null) {
          filename = Resource.generateFilename(i);
        }

        filename += "." + fileType;

        //path,id,name,offset,length,decompLength,exporter
        resources[i] = new Resource(path, filename, offset, length);

        TaskProgressManager.setValue(i);
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
