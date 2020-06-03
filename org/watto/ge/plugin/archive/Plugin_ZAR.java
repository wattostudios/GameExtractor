
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
public class Plugin_ZAR extends ArchivePlugin {

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public Plugin_ZAR() {

    super("ZAR", "ZAR");

    //         read write replace rename
    setProperties(true, false, false, false);

    setGames("SOCOM US Navy Seals");
    setExtensions("zar");
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

      // null
      if (fm.readInt() == 0) {
        rating += 5;
      }

      fm.skip(4);

      long arcSize = fm.getLength();

      // 4 - Length Of Type Names Directory
      if (FieldValidator.checkLength(fm.readInt(), arcSize)) {
        rating += 5;
      }

      fm.skip(4);

      // 4 - Padding Length (16)
      if (fm.readInt() == 16) {
        rating += 5;
      }

      // null
      if (fm.readLong() == 0) {
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

      // 4 - null
      // 4 - Number Of Type Names [/2 +-1]
      fm.skip(8);

      // 4 - Length Of Type Names Directory
      int typeDirLength = fm.readInt();
      FieldValidator.checkLength(typeDirLength, arcSize);

      // 4 - Hash?
      // 4 - Padding Length (16)
      // 64 - null
      fm.skip(72);

      // 4 - File Data Length (length of everything in //FILE DATA)
      long firstFileOffset = arcSize - fm.readInt();
      FieldValidator.checkOffset(firstFileOffset, arcSize);

      // 4 - null
      // 4 - Padding (null OR all 255's)
      // 2 - Unknown (2)
      // 2 - Unknown (2)
      fm.skip(12 + typeDirLength + 12);

      // 4 - Number Of Files
      int numFiles = fm.readInt();
      FieldValidator.checkNumFiles(numFiles);

      Resource[] resources = new Resource[numFiles];

      TaskProgressManager.setMaximum(numFiles);

      // Loop through directory
      int realNumFiles = 0;
      for (int i = 0; i < numFiles; i++) {
        // 4 - Hash?
        fm.skip(4);

        // 4 - File Offset (relative to the start of the file data)
        long offset = fm.readInt() + firstFileOffset;
        FieldValidator.checkOffset(offset, arcSize);

        // 4 - File Length
        long length = fm.readInt();
        FieldValidator.checkLength(length, arcSize);

        // 4 - Number Of Descriptors? (3)
        int numDescriptors = fm.readInt() * 16;
        FieldValidator.checkLength(numDescriptors, arcSize);
        fm.skip(numDescriptors);

        // FOR EACH DESCRIPTOR
        // 4 - Hash?
        // 4 - Descriptor Offset
        // 4 - Descriptor Length (4)
        // 4 - null

        if (offset > 0 && length > 0) {
          String filename = Resource.generateFilename(realNumFiles);

          //path,id,name,offset,length,decompLength,exporter
          resources[realNumFiles] = new Resource(path, filename, offset, length);

          TaskProgressManager.setValue(realNumFiles);
          realNumFiles++;
        }
      }

      resources = resizeResources(resources, realNumFiles);

      // This is not usually the case - i guess the *.pic files must just have their filename
      // stored at the beginning of them, whereas the other files don't have their names stored.

      for (int i = 0; i < realNumFiles; i++) {
        fm.seek(resources[i].getOffset());

        // X - Filename
        // 1 - null Filename Terminator
        // 0-3 - null Padding to a multiple of 4 bytes
        String filename = fm.readNullString();

        if (filename.length() > 4 && filename.charAt(filename.length() - 4) == '.') {
          resources[i].setName(filename);
        }

        /*
         * try { FieldValidator.checkFilename(filename);
         * 
         * int filenameLength = filename.length() + 1; int padding = 4 - ((filenameLength)%4); if
         * (padding < 4){ fm.skip(padding); filenameLength += padding; }
         * 
         * resources[i].setName(filename); resources[i].setOffset(fm.getOffset());
         * resources[i].setLength(resources[i].getLength() - filenameLength);
         * resources[i].setDecompressedLength(resources[i].getLength()); } catch (Throwable t){ }
         */
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
