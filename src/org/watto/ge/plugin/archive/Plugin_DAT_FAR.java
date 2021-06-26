
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
import org.watto.ge.plugin.ExporterPlugin;
import org.watto.ge.plugin.exporter.Exporter_Custom_DAT_FAR;
import org.watto.io.FileManipulator;

/**
**********************************************************************************************

**********************************************************************************************
**/
public class Plugin_DAT_FAR extends ArchivePlugin {

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public Plugin_DAT_FAR() {

    super("DAT_FAR", "The Sims Online DAT (FAR)");

    //         read write replace rename
    setProperties(true, false, false, false);

    setGames("The Sims Online");
    setExtensions("dat");
    setPlatforms("PC");

    setFileTypes("anim", "Animation File",
        "mesh", "Object Mesh",
        "bnd", "Object Binding",
        "apr", "Appearance File",
        "otf", "Outfit File",
        "po", "Purchasable Object",
        "col", "Collection File",
        "hag", "Grouping File");

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

      // Header (FAR!byAZ)
      if (fm.readString(8).equals("FAR!byAZ")) {
        rating += 50;
      }

      // Version (1)
      if (fm.readInt() == 1) {
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

      ExporterPlugin exporter = Exporter_Custom_DAT_FAR.getInstance();
      addFileTypes();

      FileManipulator fm = new FileManipulator(path, false);
      long arcSize = fm.getLength();

      // 8 - Header (FAR!byAZ)
      // 4 - Version (1)
      fm.skip(12);

      // 4 - Directory Offset
      long dirOffset = fm.readInt();
      FieldValidator.checkOffset(dirOffset, arcSize);

      // 4 (From Directory Offset) - Number Of Files
      fm.seek(dirOffset);
      int numFiles = fm.readInt();
      FieldValidator.checkNumFiles(numFiles);

      Resource[] resources = new Resource[numFiles];
      TaskProgressManager.setMaximum(numFiles);

      // Loop through directory
      for (int i = 0; i < numFiles; i++) {
        // 4 Bytes - Raw File Size
        int rawSize = fm.readInt();
        //FieldValidator.checkLength(rawSize,arcSize);

        // 2 Bytes - Compressed File Size
        int compressedSize = fm.readShort();

        // 1 Byte - Power Value
        int powerValue = fm.readByte();

        // Calculation of file sizes
        if (compressedSize < 0) {
          compressedSize = (65536 * (powerValue + 1)) + compressedSize;
        }
        else {
          if (powerValue > 0) {
            compressedSize = (65536 * powerValue) + compressedSize;
          }
        }

        FieldValidator.checkLength(compressedSize, arcSize);

        // 1 Byte - Blank Filler
        fm.skip(1);

        // 4 Bytes - Data Offset
        long offset = fm.readInt();
        FieldValidator.checkOffset(offset, arcSize);

        // 2 Bytes - Unknown Filler (always = 1)
        fm.skip(2);

        // 2 Bytes - Filename Length
        int filenameLength = fm.readShort();
        FieldValidator.checkFilenameLength(filenameLength);

        // 4 Bytes - File Type ID
        // 4 Bytes - File ID
        fm.skip(8);

        // X Bytes - Filename
        String filename = fm.readString(filenameLength);
        FieldValidator.checkFilename(filename);

        //path,id,name,offset,length,decompLength,exporter
        resources[i] = new Resource(path, filename, offset, compressedSize);

        if (rawSize != compressedSize) {
          resources[i].setDecompressedLength(rawSize);
          resources[i].setExporter(exporter);
        }

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
