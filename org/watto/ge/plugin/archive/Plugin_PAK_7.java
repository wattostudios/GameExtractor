
package org.watto.ge.plugin.archive;

import java.io.File;
import org.watto.task.TaskProgressManager;
import org.watto.datatype.Archive;
import org.watto.datatype.ReplacableResource;
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
public class Plugin_PAK_7 extends ArchivePlugin {

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public Plugin_PAK_7() {

    super("PAK_7", "PAK_7");

    //         read write replace rename
    setProperties(true, false, false, false);
    setCanImplicitReplace(true);

    setExtensions("pak");
    setGames("Codename: Panzers",
        "Codename: Panzers - Phase 2",
        "S.W.I.N.E.");
    setPlatforms("PC");

    setFileTypes("font", "Font Image",
        "sub", "Unknown",
        "fx", "Effects Script",
        "4d", "Unknown",
        "unit", "Unit Script",
        "fpc", "Unknown",
        "dxt", "DirectX Image");

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

      // Header (PACK)
      if (fm.readString(4).equals("PACK")) {
        rating += 50;
      }

      long arcSize = fm.getLength();

      // Directory Length
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
  
  **********************************************************************************************
  **/
  @Override
  public Resource[] read(File path) {
    try {

      addFileTypes();

      FileManipulator fm = new FileManipulator(path, false);

      long arcSize = fm.getLength();

      // 12 - Header (Sr + 26 27 13 10 135 10 + PACK)
      fm.skip(12);

      // 4 - Directory Length
      int dirLength = fm.readInt();
      FieldValidator.checkLength(dirLength, arcSize);

      dirLength += 16;

      int numFiles = Archive.getMaxFiles(4);

      Resource[] resources = new Resource[numFiles];
      TaskProgressManager.setMaximum(dirLength);

      int realNumFiles = 0;
      String oldFilename = "";
      while (fm.getOffset() < dirLength) {
        // 1 - New Filename Length
        int fullNameLength = ByteConverter.unsign(fm.readByte());

        // 1 - Append Filename size
        int filenameLength = ByteConverter.unsign(fm.readByte());

        String filename = "";
        if (fullNameLength > 0) {
          int oldCopyNameLength = fullNameLength - filenameLength;
          if (oldCopyNameLength <= oldFilename.length() && oldCopyNameLength > 0) {
            filename = oldFilename.substring(0, oldCopyNameLength);
          }
          else {
            filename = oldFilename;
          }
        }

        // X - Filename
        filename += fm.readString(filenameLength);
        FieldValidator.checkFilename(filename);

        if (realNumFiles == 0) {
          oldFilename = filename;
        }

        // 4 - Offset
        long offsetPointerLocation = fm.getOffset();
        long offsetPointerLength = 4;

        long offset = fm.readInt() + dirLength;
        FieldValidator.checkOffset(offset, arcSize);

        // 4 - Length
        long lengthPointerLocation = fm.getOffset();
        long lengthPointerLength = 4;

        long length = fm.readInt();
        FieldValidator.checkLength(length, arcSize);

        //if (length == 0){
        //  oldFilename += "/";
        //  }

        // 5 - Unknown
        fm.skip(5);

        if (length > 0) {
          //path,id,name,offset,length,decompLength,exporter
          resources[realNumFiles] = new ReplacableResource(path, filename, offset, offsetPointerLocation, offsetPointerLength, length, lengthPointerLocation, lengthPointerLength);

          TaskProgressManager.setValue((int) fm.getOffset());
          realNumFiles++;
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