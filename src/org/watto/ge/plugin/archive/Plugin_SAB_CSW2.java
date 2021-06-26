
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
public class Plugin_SAB_CSW2 extends ArchivePlugin {

  /**
  **********************************************************************************************

  **********************************************************************************************
  **/
  public Plugin_SAB_CSW2() {

    super("SAB_CSW2", "SAB_CSW2");

    //         read write replace rename
    setProperties(true, false, false, false);

    setGames("Just Cause");
    setExtensions("sab");
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
      if (fm.readString(4).equals("CSW2")) {
        rating += 50;
      }

      fm.skip(4);

      // Number Of Files
      if (FieldValidator.checkNumFiles(fm.readInt())) {
        rating += 5;
      }

      // Padding Multiple
      if (fm.readInt() == 32) {
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

      // 4 - Header (CSW2)
      // 4 - Unknown
      fm.skip(8);

      // 4 - Number Of Files
      int numFiles = fm.readInt();
      FieldValidator.checkNumFiles(numFiles);

      // 4 - Padding Multiple (32)
      // 4 - Unknown
      // 4 - Unknown
      fm.skip(12);

      // get the start of the file data
      fm.skip(numFiles * 28);
      int paddingSize = 32 - ((int) fm.getOffset() % 32);
      if (paddingSize != 32) {
        fm.skip(paddingSize);
      }
      // 4 - Number Of Folders
      int numFolders = fm.readInt();
      FieldValidator.checkNumFiles(numFolders + 1);
      // 4 - null
      fm.skip(4);
      for (int i = 0; i < numFolders; i++) {
        // 4 - Folder ID?
        fm.skip(4);
        // 4 - Folder Name length
        int folderNameLength = fm.readInt();
        FieldValidator.checkFilenameLength(folderNameLength);
        // X - Folder Name
        fm.skip(folderNameLength);
      }
      paddingSize = 32 - ((int) fm.getOffset() % 32);
      if (paddingSize != 32) {
        fm.skip(paddingSize);
      }

      //long relOffset = fm.getOffset();

      Resource[] resources = new Resource[numFiles];
      TaskProgressManager.setMaximum(numFiles);

      fm.seek(24);

      // Loop through directory
      for (int i = 0; i < numFiles; i++) {
        // 4 - Unknown (1)
        // 4 - Number Of Channels in Audio? (1/2)
        // 4 - Audio Quality (22050)
        fm.skip(12);

        // 4 - File Length
        long length = fm.readInt();
        FieldValidator.checkLength(length, arcSize);

        // 8 - null
        fm.skip(8);

        // 4 - File Offset (relative to the start of the file data)
        long offset = fm.readInt();
        FieldValidator.checkOffset(offset, arcSize);

        String filename = Resource.generateFilename(i);

        //path,name,offset,length,decompLength,exporter
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
