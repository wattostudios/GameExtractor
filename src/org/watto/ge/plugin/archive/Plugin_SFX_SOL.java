/*
 * Application:  Game Extractor
 * Author:       wattostudios
 * Website:      http://www.watto.org
 * Copyright:    Copyright (c) 2002-2020 wattostudios
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
import org.watto.io.FilenameSplitter;
import org.watto.task.TaskProgressManager;

/**
**********************************************************************************************

**********************************************************************************************
**/
public class Plugin_SFX_SOL extends ArchivePlugin {

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public Plugin_SFX_SOL() {

    super("SFX_SOL", "SFX_SOL");

    //         read write replace rename
    setProperties(true, false, false, false);

    setGames("Leisure Suit Larry 6: Shape Up Or Slip Out",
        "Leisure Suit Larry 7: Love For Sail",
        "Police Quest: SWAT");
    setExtensions("sfx", "aud"); // MUST BE LOWER CASE
    setPlatforms("PC");

    // MUST BE LOWER CASE !!!
    //setFileTypes(new FileType("txt", "Text Document", FileType.TYPE_DOCUMENT),
    //             new FileType("bmp", "Bitmap Image", FileType.TYPE_IMAGE)
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
      short id = fm.readShort();
      if (id == 3213 || id == 3085) {
        rating += 5;
      }

      if (fm.readString(3).equals("SOL")) {
        rating += 50;
      }

      fm.skip(4);

      long arcSize = fm.getLength();

      // File Data Length
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

      FileManipulator fm = new FileManipulator(path, false, 14);

      boolean skipIDs = false;
      if (FilenameSplitter.getExtension(path).equalsIgnoreCase("aud")) {
        skipIDs = true;
      }

      //fm.getBuffer().setBufferSize(14);

      long arcSize = fm.getLength();

      int numFiles = Archive.getMaxFiles();

      Resource[] resources = new Resource[numFiles];
      TaskProgressManager.setMaximum(arcSize);

      // Loop through directory
      int realNumFiles = 0;
      while (fm.getOffset() < arcSize) {
        //System.out.println(fm.getOffset());

        boolean alreadyDoneFile = false;

        boolean noIDs = false;
        if (skipIDs) {
          boolean found = false;
          for (int i = 0; i < 50000; i++) {
            // 2 - ID Number
            short idNumber = fm.readShort();
            if (idNumber == -1) {
              found = true;
              break;
            }
            else if (idNumber == 3085) {
              noIDs = true;
              found = true;
              break;
            }
            else if (idNumber == 3213) {
              noIDs = true;
              found = true;
              skipIDs = false;
              break;
            }
            else if (idNumber == 21260) {
              noIDs = true;
              found = true;

              fm.relativeSeek(fm.getOffset() - 1);
              break;
            }
            else if (idNumber == 18770) {
              // RIFF Header
              long offset = fm.getOffset() - 2;
              fm.skip(2);
              int length = fm.readInt();
              FieldValidator.checkLength(length, arcSize);
              fm.skip(length);
              length += 8;

              String filename = Resource.generateFilename(realNumFiles) + ".wav";

              //path,name,offset,length,decompLength,exporter
              resources[realNumFiles] = new Resource(path, filename, offset, length);
              realNumFiles++;

              alreadyDoneFile = true;

              TaskProgressManager.setValue(offset);
              break;
            }
          }

          if (alreadyDoneFile) {
            continue;
          }

          if (!found) {
            return null;
          }
        }

        // 2 - Unknown (3213)
        if (!noIDs) {
          fm.skip(2);
        }

        // 3 - Type? (SOL)
        String fileType = fm.readString(3);

        // 4 - Unknown (223748608)
        fm.skip(4);

        // 4 - File Data Length
        int length = fm.readInt();
        FieldValidator.checkLength(length, arcSize);

        // 1 - null
        int padder = fm.readByte();
        //System.out.println(fm.getOffset() + "\t" + length + "\t" + padder);
        if (padder != 0) {
          length -= 1;
        }

        // X - File Data
        long offset = fm.getOffset();
        fm.skip(length);

        String filename = Resource.generateFilename(realNumFiles) + "." + fileType;

        //path,name,offset,length,decompLength,exporter
        resources[realNumFiles] = new Resource(path, filename, offset, length);
        realNumFiles++;

        TaskProgressManager.setValue(offset);
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
