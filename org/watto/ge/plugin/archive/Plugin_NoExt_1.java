
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
 * THIS PLUGIN READS OVER A BUNCH OF FILES THAT MAKE A SINGLE ARCHIVE. THE FILENAMES ARE NAMED IN
 * HEX - ie 0, 1, ... A, B, etc. IT IS POSSIBLE FOR A FILE TO BE HALF IN 1 SPAN AND HALF IN THE
 * NEXT, BUT THIS PLUGIN DOES NOT TEMPT TO CORRECT THIS - THERE WILL THEREFORE BE A FEW FILES
 * THAT ARE ONLY HALF CORRECT.
 * 
 * IT ALSO HAS A LOT OF BUGS AND STUFF, AS I HAVE NOT BEEN ABLE TO TEST IT PROPERLY (NO
 * ARCHIVES). I HAVE DISABLED THE LENGTH AND OFFSET CHECKS TO GET AROUND THIS.
 **********************************************************************************************
 **/
public class Plugin_NoExt_1 extends ArchivePlugin {

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public Plugin_NoExt_1() {

    super("NoExt_1", "NoExt_1");

    //         read write replace rename
    setProperties(true, false, false, false);

    setGames("ESPN NFL 2005");
    setExtensions("");
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
      else {
        return 0;
      }

      // Number Of Files
      if (FieldValidator.checkNumFiles(fm.readInt())) {
        rating += 5;
      }

      // null
      if (fm.readInt() == 0) {
        rating += 5;
      }

      // Number header blocks
      if (fm.readInt() == 16) {
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

      long arcSize = fm.getLength();

      // 4 - Number Of Files
      int numFiles = fm.readInt();
      FieldValidator.checkNumFiles(numFiles);

      // 4 - null
      // 4 - Number Of Header Blocks (16)
      // for each header block
      // 4 - Unknown (256000/1/188164)
      // 80 - null Padding to offset 156
      fm.skip(152);

      Resource[] resources = new Resource[numFiles];

      TaskProgressManager.setMaximum(numFiles);

      // Loop through directory
      // stuff for spanning over multiple files
      long relOffset = 0;
      String[] sourceFilenames = new String[] { "0", "1", "2", "3", "4", "5", "6", "7", "8", "9", "A", "B", "C", "D", "E", "F" };
      int sourceFilenamePos = 1;

      int realNumFiles = 0;
      for (int i = 0; i < numFiles; i++) {
        // 4 - Hash?
        fm.skip(4);

        // 4 - File Length?
        long length = fm.readInt();
        //FieldValidator.checkLength(length,arcSize);

        // 4 - File Offset [*2048]
        long offset = (fm.readInt());
        if (offset < 0) {
          offset = 4294967296L + offset;
        }
        offset = (offset * 2048) - relOffset;

        if (offset >= arcSize) {
          //break;
          relOffset += arcSize;
          path = new File(path.getParent() + File.separator + sourceFilenames[sourceFilenamePos]);
          //arcSize = path.length();
          sourceFilenamePos++;

          offset -= arcSize;
        }
        //FieldValidator.checkOffset(offset,arcSize);

        String filename = Resource.generateFilename(i);

        //path,id,name,offset,length,decompLength,exporter
        resources[i] = new Resource(path, filename, offset, length);

        TaskProgressManager.setValue(i);
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
