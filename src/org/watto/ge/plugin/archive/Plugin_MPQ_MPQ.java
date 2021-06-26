
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
import org.watto.ge.plugin.ExporterPlugin;
import org.watto.ge.plugin.exporter.Exporter_ZLib_CompressedSizeOnly;
import org.watto.io.FileManipulator;

/**
**********************************************************************************************

**********************************************************************************************
**/
public class Plugin_MPQ_MPQ extends ArchivePlugin {

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public Plugin_MPQ_MPQ() {

    super("MPQ_MPQ", "MPQ_MPQ");

    //         read write replace rename
    setProperties(true, false, false, false);

    setGames("World Of Warcraft");
    setExtensions("mpq");
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
      if (fm.readString(4).equals("MPQ" + (byte) 26)) {
        rating += 50;
      }

      long arcSize = fm.getLength();

      // DirOffset
      if (FieldValidator.checkOffset(fm.readInt(), arcSize)) {
        rating += 5;
      }

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
      ExporterPlugin exporter = Exporter_ZLib_CompressedSizeOnly.getInstance();

      // RESETTING THE GLOBAL VARIABLES

      FileManipulator fm = new FileManipulator(path, false);

      long arcSize = (int) fm.getLength();

      // 4 - Header ("MPQ" + (byte)26)
      fm.skip(4);

      // 4 - Directory Offset (32)
      long dirOffset = fm.readInt();
      FieldValidator.checkOffset(dirOffset);

      // 4 - Archive Size
      // 4 - Unknown (196608)
      // 4 - Unknown Offset/Length
      // 4 - Unknown Offset/Length
      // 4 - Unknown (65536)
      // 4 - Unknown
      fm.seek(dirOffset);

      int numFiles = Archive.getMaxFiles();

      Resource[] resources = new Resource[numFiles];

      TaskProgressManager.setMaximum(arcSize);

      int realNumFiles = 0;
      while (fm.getOffset() < arcSize) {
        long relOffset = fm.getOffset();

        long firstOffset = fm.readInt();
        if (firstOffset == 2042834995) {
          break;
        }
        FieldValidator.checkOffset(firstOffset, arcSize);
        int innerNumFiles = (int) ((firstOffset - 4) / 4);

        firstOffset += relOffset;

        // read through this directory
        for (int i = 0; i < innerNumFiles; i++) {
          long nextOffset = fm.readInt() + relOffset;
          FieldValidator.checkOffset(nextOffset, arcSize);

          long length = nextOffset - firstOffset;
          FieldValidator.checkLength(length, arcSize);

          String filename = Resource.generateFilename(realNumFiles);

          //path,id,name,offset,length,decompLength,exporter
          resources[realNumFiles] = new Resource(path, filename, firstOffset, length);

          TaskProgressManager.setValue(nextOffset);

          firstOffset = nextOffset;
          realNumFiles++;
        }

        // Offset to the next directory;
        fm.seek(firstOffset);
      }

      resources = resizeResources(resources, realNumFiles);

      TaskProgressManager.setMaximum(realNumFiles);

      for (int i = 0; i < realNumFiles; i++) {
        Resource fd = resources[i];
        fm.seek(fd.getOffset());

        // 1 - Compression Tag
        int compTag = fm.readByte();
        if (compTag == 2) {
          // compressed file
          fd.setOffset(fm.getOffset());
          fd.setExporter(exporter);
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
