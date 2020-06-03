
package org.watto.ge.plugin.archive;

import java.io.File;
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
import org.watto.ge.plugin.ExporterPlugin;
import org.watto.ge.plugin.exporter.Exporter_Custom_JDLZ;
import org.watto.io.FileManipulator;
import org.watto.io.converter.ByteConverter;
import org.watto.task.TaskProgressManager;

/**
**********************************************************************************************

**********************************************************************************************
**/
public class Plugin_BIN_18 extends ArchivePlugin {

  /**
  **********************************************************************************************

  **********************************************************************************************
  **/
  public Plugin_BIN_18() {

    super("BIN_18", "BIN_18");

    //         read write replace rename
    setProperties(true, false, false, false);

    setGames("Need For Speed: Pro Street",
        "Need For Speed: Most Wanted");
    setExtensions("bin"); // MUST BE LOWER CASE
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
      int byte1 = ByteConverter.unsign(fm.readByte());
      int byte2 = ByteConverter.unsign(fm.readByte());
      int byte3 = ByteConverter.unsign(fm.readByte());
      int byte4 = ByteConverter.unsign(fm.readByte());
      if (byte1 == 0 && byte2 == 0 && byte3 == 48 && byte4 == 179) {
        rating += 50;
      }

      long arcSize = fm.getLength();

      // Archive Size
      if (fm.readInt() + 8 == arcSize) {
        rating += 5;
      }

      // null
      if (fm.readInt() == 0) {
        rating += 5;
      }

      // Unknown (8)
      if (fm.readInt() == 8) {
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

      ExporterPlugin exporter = Exporter_Custom_JDLZ.getInstance();

      // RESETTING GLOBAL VARIABLES

      FileManipulator fm = new FileManipulator(path, false);

      long arcSize = fm.getLength();

      // 4 - Header (0,0,48,179)
      // 4 - Archive Length [+8]
      // 4 - null
      // 4 - Unknown (8)
      // 48 - null

      // 4 - Header (0,0,49,179)
      // 4 - Details Length [+8 for these 2 fields]
      // 4 - Filename Header (1,0,49,51)
      // 4 - Filename Length (124)
      // 4 - Unknown (8)
      // 28 - Archive Name (not including extension) (upper case) (null terminated)
      // 64 - Archive Path and Filename (null terminated)
      // 4 - Unknown
      // 24 - null

      // 4 - Block Header (2,0,49,51)
      fm.skip(64 + 140 + 4);

      // 4 - Block Length
      int blockLength = fm.readInt();
      FieldValidator.checkLength(blockLength, arcSize);

      // for each file (8 bytes per entry)
      // 4 - Hash?
      // 4 - null
      fm.skip(blockLength);

      // 4 - Block Header (3,0,49,51)
      fm.skip(4);

      // 4 - Block Length
      blockLength = (int) (fm.readInt() + fm.getOffset());
      FieldValidator.checkOffset(blockLength, arcSize);

      int numFiles = Archive.getMaxFiles();

      Resource[] resources = new Resource[numFiles];
      TaskProgressManager.setMaximum(numFiles);

      // Loop through directory
      int realNumFiles = 0;
      while (fm.getOffset() < blockLength) {
        // 4 - Hash? (same as in above directory)
        if (fm.readInt() == 0) {
          break; // padding - no more files
        }

        // 4 - File Offset
        int offset = fm.readInt();
        FieldValidator.checkOffset(offset, arcSize);

        // 4 - File Length (not including padding)
        int length = fm.readInt();
        FieldValidator.checkLength(length, arcSize);

        // 4 - Decompressed Length?
        int decompLength = fm.readInt();
        FieldValidator.checkLength(decompLength);

        // 4 - Unknown (512)
        // 4 - null
        fm.skip(8);

        String filename = Resource.generateFilename(realNumFiles);

        //path,name,offset,length,decompLength,exporter
        resources[realNumFiles] = new Resource(path, filename, offset, length, decompLength, exporter);

        TaskProgressManager.setValue(realNumFiles);
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
