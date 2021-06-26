
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
import org.watto.io.FileManipulator;
import org.watto.io.converter.ByteConverter;

/**
**********************************************************************************************

**********************************************************************************************
**/
public class Plugin_BIG_WARBUILDER extends ArchivePlugin {

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public Plugin_BIG_WARBUILDER() {

    super("BIG_WARBUILDER", "BIG_WARBUILDER");

    //         read write replace rename
    setProperties(true, false, false, false);

    setGames("Rise Of Legends");
    setExtensions("big");
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

      // Version (10)
      if (fm.readInt() == 10) {
        rating += 5;
      }

      // Header Length
      if (ByteConverter.unsign(fm.readByte()) == 1 && fm.readInt() == 11) {
        rating += 5;
      }

      // Header
      if (fm.readUnicodeString(11).equals("WAR-BUILDER")) {
        rating += 50;
      }

      // null
      if (fm.readInt() == 0) {
        rating += 5;
      }

      // Number Of Files
      if (FieldValidator.checkNumFiles(fm.readInt())) {
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

      ExporterPlugin exporter = Exporter_ZLib.getInstance();

      // RESETTING GLOBAL VARIABLES

      FileManipulator fm = new FileManipulator(path, false);

      long arcSize = fm.getLength();

      // 4 - Version (10)
      // 1 - Number Of Headers (1)
      // 4 - Header Length (11) [*2 for unicode]
      // 22 - Header (WAR-BUILDER) (unicode)
      // 4 - null
      // 4 - Number Of Files
      fm.skip(39);

      // 4 - Number Of Files
      int numFiles = fm.readInt();
      FieldValidator.checkNumFiles(numFiles);

      // 3 - Padding (255,255,0)
      fm.skip(3);

      Resource[] resources = new Resource[numFiles];
      TaskProgressManager.setMaximum(numFiles);

      // Loop through directory
      for (int i = 0; i < numFiles; i++) {
        // 4 - Filename Length [*2 for unicode]
        int filenameLength = fm.readInt() * 2;
        FieldValidator.checkFilenameLength(filenameLength);

        // X - Filename (unicode) (including .\ at the start)
        String filename = fm.readUnicodeString(filenameLength);
        if (filename.length() > 2 && filename.substring(0, 2).equals(".\"")) {
          filename = filename.substring(2);
        }

        // 8 - null
        fm.skip(8);

        // 4 - File Offset
        long offset = fm.readInt();
        FieldValidator.checkOffset(offset, arcSize);

        // 4 - Decompressed Length
        long decompLength = fm.readInt();
        FieldValidator.checkLength(decompLength);

        // 4 - null
        // 4 - Hash?
        // 6 - null
        fm.skip(14);

        //path,name,offset,length,decompLength,exporter
        resources[i] = new Resource(path, filename, offset, 0, decompLength, exporter);

        TaskProgressManager.setValue(i);
      }

      // get the compressed lengths
      for (int i = 0; i < numFiles; i++) {
        Resource resource = resources[i];
        long offset = resource.getOffset();
        fm.seek(offset);

        // 4 - Compressed Length
        long length = fm.readInt();
        FieldValidator.checkLength(length, arcSize);
        resource.setLength(length);

        // X - File Data (ZLib compression)
        offset += 4;
        resource.setOffset(offset);
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
