
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
public class Plugin_DAT_29 extends ArchivePlugin {

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public Plugin_DAT_29() {

    super("DAT_29", "DAT_29");

    //         read write replace rename
    setProperties(true, false, false, false);

    setGames("Far Cry: Instincts");
    setExtensions("dat");
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

      fm.skip(32);

      long arcSize = fm.getLength();

      // 4 - Length Of File Header (ie relative pointer to the file data)
      if (FieldValidator.checkLength(fm.readInt(), arcSize)) {
        rating += 5;
      }

      fm.skip(52);

      // 4 - File Length (including all the following fields, but not including this field)
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

      // RESETTING THE GLOBAL VARIABLES

      FileManipulator fm = new FileManipulator(path, false);

      long arcSize = (int) fm.getLength();

      int numFiles = Archive.getMaxFiles();

      Resource[] resources = new Resource[numFiles];

      TaskProgressManager.setMaximum(numFiles);

      // Loop through directory
      int realNumFiles = 0;
      while (fm.getOffset() < arcSize) {
        long offset = fm.getOffset();
        //System.out.println(offset);

        int type = fm.readInt();
        if (type == 16845056) {

          // 32 - Unknown
          fm.skip(28);

          // 4 - Length Of File Header (ie relative pointer to the file data)
          int fileHeaderLength = fm.readInt();

          offset += fileHeaderLength;
          FieldValidator.checkOffset(offset, arcSize);

          // 52 - Unknown
          fm.skip(52);

          // 4 - File Length (including all the following fields, but not including this field)
          long length = fm.readInt() - fileHeaderLength + 4;
          FieldValidator.checkLength(length, arcSize);

          // 12 - Unknown
          fm.skip(12);

          // 4 - Filename Offset (relative to the start of this file entry)
          long filenameOffset = fm.getOffset() + fm.readInt() - 104;
          FieldValidator.checkOffset(filenameOffset, arcSize);

          // 156 - Unknown & extra data
          fm.seek(filenameOffset);

          // X - Filename
          // 1 - null Filename Terminator
          //System.out.println("off:" + fm.getOffset());
          String filename = fm.readNullString();
          FieldValidator.checkFilename(filename);
          //System.out.println(filename);

          // X - Group 1 Name
          // 1 - null Group 1 Name Terminator
          // X - Group 2 Name
          // 1 - null Group 2 Name Terminator
          // 1 - File Header End Tag ((byte)205)
          // X - File Data
          fm.seek(offset + length);

          //path,id,name,offset,length,decompLength,exporter
          resources[realNumFiles] = new Resource(path, filename, offset, length);

          TaskProgressManager.setValue(realNumFiles);
          realNumFiles++;
        }
        else if (type == 50594048) {
          // 4 - Unknown
          // 4 - Unknown
          fm.skip(8);

          // 4 - Length
          long length = fm.readInt();
          FieldValidator.checkLength(length, arcSize);

          // 16 - Unknown
          fm.skip(16);

          fm.skip(length);

        }
        else if (type == 100691810) {
          fm.skip(104);

          // 4 - Length
          long length = fm.readInt();
          FieldValidator.checkLength(length, arcSize);

          fm.skip(length + 20);

        }
        else if (type == 795435637) {
          fm.skip(993);
        }
        else {
          int filenameLength = fm.readInt();
          FieldValidator.checkFilenameLength(filenameLength);

          fm.skip(filenameLength);

          // 4 - Length
          long length = fm.readInt();
          FieldValidator.checkLength(length, arcSize);

          fm.skip(length);
        }
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
