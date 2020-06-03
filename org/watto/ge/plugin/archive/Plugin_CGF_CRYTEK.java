
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
public class Plugin_CGF_CRYTEK extends ArchivePlugin {

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public Plugin_CGF_CRYTEK() {

    super("CGF_CRYTEK", "CGF_CRYTEK");

    //         read write replace rename
    setProperties(true, false, false, false);

    setExtensions("cgf", "cga");
    setGames("Far Cry");
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
      if (fm.readString(6).equals("CryTek")) {
        rating += 50;
      }

      fm.skip(10);

      long arcSize = fm.getLength();

      // DirectoryOffset
      if (FieldValidator.checkOffset(fm.readInt(), arcSize)) {
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
  
  **********************************************************************************************
  **/
  @SuppressWarnings("unused")
  @Override
  public Resource[] read(File path) {
    try {

      addFileTypes();

      FileManipulator fm = new FileManipulator(path, false);
      long arcSize = fm.getLength();

      // 6 - Header (CryTek)
      // 2 - Unknown
      // 2 - null
      // 2 - Unknown
      // 4 - Unknown
      fm.skip(16);

      // 4 - Directory Offset (can be all 255's - not sure what to do here if not a real value!)
      long dirOffset = fm.readInt();
      FieldValidator.checkOffset(dirOffset, arcSize);

      fm.seek(dirOffset);

      // 4 - numFiles (from dirOffset)
      int numFiles = fm.readInt();
      FieldValidator.checkNumFiles(numFiles);

      Resource[] resources = new Resource[numFiles];
      TaskProgressManager.setMaximum(numFiles);

      int realNumFiles = 0;
      for (int i = 0; i < numFiles; i++) {
        // 2 - ID?
        // 2 - Unknown (both 204)
        // 1 - Group ID?
        fm.skip(5);

        // 1 - File Type (0 = Archive Description, 9 = Root Directory?, 8 = Directory, 7 = File)
        int type = fm.readByte();

        // 2 - null
        fm.skip(2);

        // 4 - Data Offset
        long offset = fm.readInt();
        FieldValidator.checkOffset(offset, arcSize);

        // 4 - File Number (starting at 0)
        int fileID = fm.readInt();

        if (type == 7) {
          /*
           * int currentPos = (int)fm.getOffset(); fm.seek(offset);
           *
           * // 2 - ID? (same as ID in dirEntry) // 4 - Unknown // 2 - Unknown // 4 - Data Offset
           * (same as in dirEntry) // 4 - File Number (same as in dirEntry) fm.skip(16);
           *
           * // 128 - Description of item (null) String filename = fm.readNullString(128);
           * System.out.println(filename); FieldValidator.checkFilename(filename);
           *
           * offset = (int)fm.getOffset(); // X - File
           *
           * //path,id,name,offset,length,decompLength,exporter resources[i] = new
           * Resource(path,filename,offset);
           *
           * TaskProgressManager.setValue(i);
           *
           * fm.seek(currentPos);
           */

          offset += 144;
          String filename = Resource.generateFilename(realNumFiles);

          //path,id,name,offset,length,decompLength,exporter
          resources[realNumFiles] = new Resource(path, filename, offset);

          TaskProgressManager.setValue(i);
          realNumFiles++;
        }

      }

      fm.close();

      resources = resizeResources(resources, realNumFiles);
      calculateFileSizes(resources, arcSize);

      return resources;

    }
    catch (Throwable t) {
      logError(t);
      return null;
    }
  }

}