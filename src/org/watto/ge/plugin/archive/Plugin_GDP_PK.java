
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
public class Plugin_GDP_PK extends ArchivePlugin {

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public Plugin_GDP_PK() {

    super("GDP_PK", "GDP_PK");

    //         read write replace rename
    setProperties(true, false, false, false);

    setGames("Exmachina");
    setExtensions("gdp");
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
      if (fm.readString(2).equals("PK") && fm.readShort() == 4) {
        rating += 50;
      }

      fm.skip(4);

      // null
      if (fm.readInt() == 0) {
        rating += 5;
      }

      // Version
      if (fm.readInt() == 1) {
        rating += 5;
      }

      fm.skip(4);

      // null
      if (fm.readInt() == 0) {
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

      // 2 - Header (PK)
      // 2 - Version (4)
      // 4 - Unknown
      // 4 - null
      // 4 - Version (1)
      // 8 - CRC?
      // 4 - null
      // 4 - Unknown
      // 12 - null
      fm.skip(44);

      int numFiles = Archive.getMaxFiles();

      Resource[] resources = new Resource[numFiles];

      TaskProgressManager.setMaximum(arcSize);

      // Loop through directory
      int realNumFiles = 0;
      for (int i = 0; i < numFiles; i++) {
        // 4 - Unknown
        // 8 - CRC?
        fm.skip(12);

        // 4 - Unknown (1)
        int checkByte = fm.readInt();
        if (checkByte != 1) {
          break;
        }

        // 4 - null
        fm.skip(4);

        // 4 - File Offset [-XXXX]
        long offset = fm.readInt();
        //FieldValidator.checkOffset(offset,arcSize);

        // 4 - Unknown
        // 4 - Unknown
        // 20 - null
        fm.skip(28);

        // X - Filename
        // 2 - null Filename Terminator
        String filename = fm.readNullString();
        FieldValidator.checkFilename(filename);
        fm.skip(1);

        //path,id,name,offset,length,decompLength,exporter
        resources[realNumFiles] = new Resource(path, filename, offset);
        resources[realNumFiles].setExporter(exporter);

        TaskProgressManager.setValue(offset);
        realNumFiles++;
      }

      resources = resizeResources(resources, realNumFiles);

      long relPos = fm.getOffset() - 16;

      for (int i = 0; i < realNumFiles - 1; i++) {
        long length = (int) (resources[i + 1].getOffset() - resources[i].getOffset());
        FieldValidator.checkOffset(relPos, arcSize);
        resources[i].setOffset(relPos);
        resources[i].setLength(length);
        resources[i].setDecompressedLength(length);
        relPos += length;
      }

      resources[realNumFiles - 1].setOffset(relPos);
      resources[realNumFiles - 1].setLength((arcSize - 8) - relPos);
      resources[realNumFiles - 1].setDecompressedLength((arcSize - 8) - relPos);

      //calculateFileSizes(resources,arcSize-8);

      fm.close();

      return resources;

    }
    catch (Throwable t) {
      logError(t);
      return null;
    }
  }

}
