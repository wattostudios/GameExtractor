
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
import org.watto.io.FileManipulator;
import org.watto.io.converter.ByteConverter;

/**
**********************************************************************************************

**********************************************************************************************
**/
public class Plugin_DWX_DELPHIXWAVE extends ArchivePlugin {

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public Plugin_DWX_DELPHIXWAVE() {

    super("DWX_DELPHIXWAVE", "DelphiX Audio Collection");

    //         read write replace rename
    setProperties(true, false, false, false);

    setGames("Virtual Remote Control Racing");
    setExtensions("dwx");
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

      fm.skip(3);

      // Header
      if (fm.readString(21).equals("DELPHIXWAVECOLLECTION")) {
        rating += 50;
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

      //ExporterPlugin exporter = Exporter_ZLib.getInstance();

      // RESETTING GLOBAL VARIABLES

      FileManipulator fm = new FileManipulator(path, false);

      long arcSize = fm.getLength();

      // 1 - Unknown (-1)
      // 2 - Unknown (10)
      // 21 - Header (DELPHIXWAVECOLLECTION)
      // 1 - null
      // 2 - Unknown (48,16)
      // 4 - Archive Length (not including header fields - ie [+31])
      // 4 - TPF Descriptor (TPF0)
      fm.skip(35);

      // 1 - Component Descriptor Length (24)
      // X - Component Descriptor (TWaveCollectionComponent)
      // 1 - null Start Of Directory Marker
      fm.skip(ByteConverter.unsign(fm.readByte()) + 1);

      // 1 - List Descriptor Length (4)
      // 4 - List Descriptor (List)
      // 1 - Start Of List Marker (14)
      fm.skip(ByteConverter.unsign(fm.readByte()) + 1);

      int numFiles = Archive.getMaxFiles();

      Resource[] resources = new Resource[numFiles];
      TaskProgressManager.setMaximum(numFiles);

      // Loop through directory
      int realNumFiles = 0;
      String filename = "";
      while (fm.getOffset() < arcSize) {
        // 1 - Descriptor Type
        int type = ByteConverter.unsign(fm.readByte());
        if (type == 1) {
          // 1 - Name Descriptor Length (4)
          // 4 - Name Descriptor (Name)
          fm.skip(ByteConverter.unsign(fm.readByte()));
        }
        else if (type == 6) {
          // 1 - Filename Length
          int filenameLength = ByteConverter.unsign(fm.readByte());

          // X - Filename
          filename = fm.readString(filenameLength);

          // 1 - File Type Description Length (9)
          // X - File Type Description (Wave.WAVE)
          fm.skip(ByteConverter.unsign(fm.readByte()));
        }
        else if (type == 0) {
          // nothing - next file
        }
        else if (type == 14) {
          // 4 - File Length
          long length = fm.readInt();
          FieldValidator.checkLength(length, arcSize);

          // X - File Data
          long offset = fm.getOffset();
          fm.skip(length);

          //path,name,offset,length,decompLength,exporter
          resources[realNumFiles] = new Resource(path, filename, offset, length);

          TaskProgressManager.setValue(offset);
          realNumFiles++;
        }
        else {
          System.out.println("DWX_DELPHIXWAVE - Invalid type " + type + " at " + (fm.getOffset() - 1));
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
