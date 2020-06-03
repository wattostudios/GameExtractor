
package org.watto.ge.plugin.archive;

import java.io.File;
import org.watto.component.WSPluginException;
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
public class Plugin_ARK_3 extends ArchivePlugin {

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public Plugin_ARK_3() {

    super("ARK_3", "ARK_3");

    //         read write replace rename
    setProperties(true, false, false, false);

    setGames("Guitar Hero");
    setExtensions("ark");
    setPlatforms("PS2");

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

      if (new File(fm.getFile().getParent() + File.separator + "MAIN.HDR").exists()) {
        rating += 25;
      }
      else {
        throw new WSPluginException("Missing Index File");
      }

      // Number Of Directories
      if (fm.readInt() == 3) {
        rating += 5;
      }

      // Version
      if (fm.readInt() == 1) {
        rating += 5;
      }

      // Version
      if (fm.readInt() == 1) {
        rating += 5;
      }

      long arcSize = fm.getLength();

      // Archive Size
      if (FieldValidator.checkEquals(fm.readInt(), arcSize)) {
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

      long arcSize = path.length();

      File sourcePath = new File(path.getParent() + File.separator + "MAIN.HDR");
      FileManipulator fm = new FileManipulator(sourcePath, false);

      // 4 - Number Of Directoies (3)
      // 4 - Version Major (1)
      // 4 - Version Minor (1)
      // 4 - Archive Length
      fm.skip(16);

      // 4 - Filename Directory Length (not including this field)
      int filenameDirLength = fm.readInt();
      fm.skip(filenameDirLength);

      // 4 - Number Of Files And Folders
      int numFilenames = fm.readInt();
      FieldValidator.checkNumFiles(numFilenames);

      int[] nameOffsets = new int[numFilenames];
      // Loop through directory
      for (int i = 0; i < numFilenames; i++) {
        // 4 - Filename Offset
        int filenameOffset = fm.readInt() + 20;
        FieldValidator.checkOffset(filenameOffset, arcSize);

        nameOffsets[i] = filenameOffset;
      }

      long dirOffset = fm.getOffset();

      String[] names = new String[numFilenames];
      for (int i = 0; i < numFilenames; i++) {
        int nameOffset = nameOffsets[i];

        String filename;
        if (nameOffset == 20) {
          filename = "";
        }
        else {
          fm.seek(nameOffset);
          // X - Filename (null)
          filename = fm.readNullString();
          FieldValidator.checkFilename(filename);

          if (filename.indexOf("../../") == 0) {
            filename = filename.substring(6);
          }
        }

        names[i] = filename;
      }

      fm.seek(dirOffset);

      int numFiles = fm.readInt();
      FieldValidator.checkNumFiles(numFiles);

      Resource[] resources = new Resource[numFiles];

      TaskProgressManager.setMaximum(numFiles);

      // Loop through directory
      for (int i = 0; i < numFiles; i++) {
        // 4 - File Offset
        long offset = fm.readInt();
        if (offset < 0) {
          offset = 4294967296L + offset;
        }

        // 4 - Filename Number
        int filenameNumber = fm.readInt();
        FieldValidator.checkLength(filenameNumber, numFilenames);

        // 4 - Directory Name Number
        int dirNameNumber = fm.readInt();
        FieldValidator.checkLength(dirNameNumber, numFilenames);

        // 4 - File Length?
        long length = fm.readInt();
        FieldValidator.checkLength(length, arcSize);

        // 4 - null
        fm.skip(4);

        String filename = names[dirNameNumber] + names[filenameNumber];

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
