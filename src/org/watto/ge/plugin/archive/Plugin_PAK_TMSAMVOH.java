/*
 * Application:  Game Extractor
 * Author:       wattostudios
 * Website:      http://www.watto.org
 * Copyright:    Copyright (c) 2002-2022 wattostudios
 *
 * License Information:
 * This program is free software; you can redistribute it and/or modify it under the terms of the GNU General Public License
 * published by the Free Software Foundation; either version 2 of the License, or (at your option) any later versions. This
 * program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranties
 * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License at http://www.gnu.org for more
 * details. For further information on this application, refer to the authors' website.
 */

package org.watto.ge.plugin.archive;

import java.io.File;
import org.watto.datatype.Archive;
import org.watto.datatype.Resource;
import org.watto.ge.helper.FieldValidator;
import org.watto.ge.plugin.ArchivePlugin;
import org.watto.io.FileManipulator;
import org.watto.io.converter.ByteConverter;
import org.watto.task.TaskProgressManager;

/**
**********************************************************************************************

**********************************************************************************************
**/
public class Plugin_PAK_TMSAMVOH extends ArchivePlugin {

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public Plugin_PAK_TMSAMVOH() {

    super("PAK_TMSAMVOH", "PAK_TMSAMVOH");

    //         read write replace rename
    setProperties(true, false, false, false);

    setGames("3D Ultra Minigolf Adventures",
        "Airport Tycoon 3",
        "Blowout",
        "Crusades: Quest For Power",
        "Daemonica",
        "Mob Enforcer",
        "Mystery Case Files: Prime Suspects",
        "Mystery Case Files: Ravenhearst",
        "Republic: The Revolution",
        "Sprint Cars: Road To Knoxville");
    setExtensions("pak");
    setPlatforms("PC");

    //setFileTypes("","",
    //             "",""
    //             );

    //setCanScanForFileTypes(true);

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
      if (fm.readByte() == 0 && fm.readString(8).equals("TMSAMVOH")) {
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

      // 1 - null
      // 8 - Header (TMSAMVOH)
      fm.skip(9);

      int numFiles = Archive.getMaxFiles();

      Resource[] resources = new Resource[numFiles];
      TaskProgressManager.setMaximum(arcSize);

      // Loop through directory
      int realNumFiles = 0;
      while (fm.getOffset() < arcSize) {
        System.out.println(fm.getOffset());

        // 4 - Unknown (164,155,253,255)
        int checkVal = fm.readInt();
        if (checkVal != 2) {

          boolean finishedLoop = false;
          while (!finishedLoop && fm.getOffset() < arcSize) {
            // 1 - Block Start Tag (38)
            int blockType = ByteConverter.unsign(fm.readByte());
            if (blockType != 38) {
              // finished - found the end directories and other weird stuff
              resources = resizeResources(resources, realNumFiles);
              fm.close();
              return resources;
            }

            // 1 - Block Length (need to subtract the number as indicated in the if-else)
            int blockLength = ByteConverter.unsign(fm.readByte());

            if (blockLength == 33) {
              //System.out.println(fm.getOffset());
              blockLength -= 1;
              finishedLoop = true;
            }
            else {
              //System.out.println("New Block " + blockLength);
              blockLength -= 6;
            }

            fm.skip(blockLength);
          }
        }

        // 4 - Unknown (2)
        fm.skip(4);

        // 1 - Compression Flag? (1=compressed)
        fm.skip(1);

        // 4 - Compressed Length
        int length = fm.readInt();
        FieldValidator.checkLength(length, arcSize);

        //// 2 - null
        //fm.skip(2);
        //
        //// 2 - Unknown (1)
        //int typeFlag = fm.readShort();
        //if (typeFlag != 1){
        //  // 8 - Unknown (65536)
        //  fm.skip(8);
        //  }
        fm.skip(4);

        // 4 - Decompressed Length?
        int decompLength = fm.readInt();
        FieldValidator.checkLength(decompLength);

        //if (typeFlag != 1){
        //  // 4 - Unknown
        //  fm.skip(4);
        //  }

        // X - File Data (Unknown Compression - similar to ZLib?)
        long offset = fm.getOffset();
        //System.out.println(fm.getOffset() + " - " + length);
        fm.skip(length);

        String filename = Resource.generateFilename(realNumFiles);

        //path,name,offset,length,decompLength,exporter
        resources[realNumFiles] = new Resource(path, filename, offset, length, decompLength);

        TaskProgressManager.setValue(offset);
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
