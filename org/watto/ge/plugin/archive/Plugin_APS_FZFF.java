
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
import org.watto.ge.plugin.exporter.Exporter_ZLib;
import org.watto.ge.plugin.exporter.Exporter_ZLib_CompressedSizeOnly;
import org.watto.io.FileManipulator;

/**
**********************************************************************************************

**********************************************************************************************
**/
public class Plugin_APS_FZFF extends ArchivePlugin {

  /**
  **********************************************************************************************

  **********************************************************************************************
  **/
  public Plugin_APS_FZFF() {

    super("APS_FZFF", "APS_FZFF");

    //         read write replace rename
    setProperties(true, false, false, false);

    setGames("Hidden Strike 2");
    setExtensions("aps");
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
      if (fm.readString(4).equals("FZFF")) {
        rating += 50;
      }

      long arcSize = fm.getLength();

      // Directory Offset
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

      // 4 - Header (FZFF)
      fm.skip(4);

      // 4 - Directory Offset
      long dirOffset = fm.readInt();
      FieldValidator.checkOffset(dirOffset, arcSize);

      fm.seek(dirOffset);

      // 4 - Decompressed Directory Length? (including Directory 1, 2, etc.)
      // 4 - Unknown
      fm.skip(8);

      // 4 - Length Of Compressed Directory 1 Data
      int dir1Length = fm.readInt();
      FieldValidator.checkLength(dir1Length, arcSize);

      // 4 - Number Of Files in Directory 1 and 2
      // 4 - Directory Header Length (20)
      fm.skip(8 + dir1Length);

      // 4 - Length Of Compressed Directory 1 Data
      int dir2Length = fm.readInt();
      FieldValidator.checkLength(dir2Length, arcSize);

      // 4 - Unknown (1)
      // 4 - Decompressed Directory 2 Length?
      fm.skip(8 + dir2Length);

      // 4 - Length Of Compressed Directory 3 Data
      int dirLength = fm.readInt();
      FieldValidator.checkLength(dirLength, arcSize);

      // 4 - Number Of Files in Directory 3
      int numFiles = fm.readInt();
      FieldValidator.checkNumFiles(numFiles);

      // 4 - Unknown (4)
      fm.skip(4);

      Resource[] resources = new Resource[numFiles];

      TaskProgressManager.setMaximum(numFiles);

      fm.close();

      // Decompress the directory
      FileManipulator extDir = new FileManipulator(new File("temp" + File.separator + "aps_directory_decompressed.dat"), true);
      String dirName = extDir.getFilePath();
      Resource directory = new Resource(path, dirName, fm.getOffset(), dirLength, dirLength * 20);

      ExporterPlugin exporter = Exporter_ZLib.getInstance();
      exporter.extract(directory, extDir);

      extDir.close();

      // Now open the directory and read it
      fm = new FileManipulator(new File(dirName), false);

      // Loop through directory
      int realNumFiles = 0;
      long prevOffset = 0;
      ExporterPlugin exporterComp = Exporter_ZLib_CompressedSizeOnly.getInstance();
      for (int i = 0; i < numFiles; i++) {
        // 4 - File Offset
        long offset = fm.readInt();
        FieldValidator.checkOffset(offset, arcSize);

        if (offset != prevOffset) {
          String filename = Resource.generateFilename(realNumFiles);

          //path,id,name,offset,length,decompLength,exporter
          resources[realNumFiles] = new Resource(path, filename, offset);
          resources[realNumFiles].setExporter(exporterComp);

          TaskProgressManager.setValue(i);
          realNumFiles++;
        }

        prevOffset = offset;
      }

      resources = resizeResources(resources, realNumFiles);
      calculateFileSizes(resources, arcSize);

      fm.close();

      return resources;

    }
    catch (Throwable t) {
      logError(t);
      return null;
    }
  }

}
