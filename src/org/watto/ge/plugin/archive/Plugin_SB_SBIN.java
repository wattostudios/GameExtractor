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
import org.watto.ErrorLogger;
import org.watto.datatype.Resource;
import org.watto.ge.helper.FieldValidator;
import org.watto.ge.plugin.ArchivePlugin;
import org.watto.io.FileManipulator;
import org.watto.io.converter.ShortConverter;
import org.watto.task.TaskProgressManager;

/**
**********************************************************************************************

**********************************************************************************************
**/
public class Plugin_SB_SBIN extends ArchivePlugin {

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public Plugin_SB_SBIN() {

    super("SB_SBIN", "SB_SBIN");

    //         read write replace rename
    setProperties(true, false, false, false);

    setGames("The Sims FreePlay");
    setExtensions("sb"); // MUST BE LOWER CASE
    setPlatforms("Android");

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
      if (fm.readString(4).equals("SBIN")) {
        rating += 50;
      }

      // Version (3)
      if (fm.readInt() == 3) {
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

      //Exporter_REFPACK exporter = Exporter_REFPACK.getInstance();
      //exporter.setReadDecompHeader(true);

      // RESETTING GLOBAL VARIABLES

      FileManipulator fm = new FileManipulator(path, false);

      long arcSize = fm.getLength();

      // 4 - Header (SBIN)
      // 4 - Version (3)
      fm.skip(8);

      int numFiles = -1;
      int numNames = -1;

      int[] nameIDs = new int[0];
      String[] names = new String[0];
      int[] lengths = new int[0];
      int[] offsets = new int[0];

      long relativeOffset = 0;

      while (fm.getOffset() < arcSize) {
        // 4 - Header
        String header = fm.readString(4);

        // 4 - Block Length (not including these 3 fields)
        int blockLength = fm.readInt();
        //System.out.println(header + "\t" + blockLength + "\t" + (fm.getOffset()-8));
        FieldValidator.checkLength(blockLength, arcSize);

        // 4 - Unknown
        fm.skip(4);

        // Branch according to the block type...
        if (header.equals("ENUM")) {
          // 4 - Unknown
          // 4 - Unknown
          fm.skip(blockLength);
        }
        else if (header.equals("ALGN")) {
          // X - null Padding
          fm.skip(blockLength);
        }
        else if (header.equals("STRU")) {
          // 2 - Unknown
          // 2 - Unknown
          // 2 - Unknown
          // 2 - Unknown
          // 2 - Unknown
          // 2 - Unknown
          fm.skip(blockLength);
        }
        else if (header.equals("FIEL")) {
          // for each entry
          //   2 - Unknown
          //   2 - Unknown
          //   2 - Unknown
          //   2 - Unknown
          fm.skip(blockLength);
        }
        else if (header.equals("OHDR")) {
          // 4 - Unknown (1)
          // 4 - Unknown (130)
          // 4 - Unknown

          // for each file
          //   4 - Unknown
          fm.skip(blockLength);
        }
        else if (header.equals("DATA")) {
          // 2 - Unknown (1)
          // 2 - Unknown (16)
          // 2 - Unknown (23)
          // 2 - Unknown (17)
          // 4 - Unknown (12)
          // 4 - Unknown (1)
          // 4 - Unknown (15)
          fm.skip(20);

          // 4 - Number of Files
          numFiles = fm.readInt();
          FieldValidator.checkNumFiles(numFiles);

          nameIDs = new int[numFiles];

          // for each file
          //   4 - File ID?
          fm.skip(numFiles * 4);

          // for each file (44 bytes per entry)
          for (int i = 0; i < numFiles; i++) {
            // 4 - null
            fm.skip(4);

            // 2 - Name ID
            int nameID = ShortConverter.unsign(fm.readShort());
            nameIDs[i] = nameID;

            // 2 - Unknown
            // 2 - Unknown
            // 2 - Unknown
            // 2 - Unknown
            // 2 - Unknown
            // 2 - Unknown
            // 2 - Unknown
            // 4 - Unknown (258)
            // 4 - Unknown (3)
            // 8 - null
            // 4 - Unknown (0/256)
            // 4 - File ID (incremental from zero)
            fm.skip(38);
          }

          // 4 - Unknown (13)
          // 4 - Unknown (4)
          // 8 - Unknown
          fm.skip(16);
        }
        else if (header.equals("CHDR")) {
          // for each file and property (8 bytes per entry)
          //   4 - Name Offset (relative to the start of the CDAT Block) [+12]
          //   4 - Name Length (not including null terminator) (can be null, for the root)
          fm.skip(blockLength);

          numNames = (blockLength / 8);
          FieldValidator.checkNumFiles(numNames);
        }
        else if (header.equals("CDAT")) {

          if (numNames == -1) {
            // don't know how many names, so skip it
            fm.skip(blockLength);
          }
          else {
            names = new String[numNames];

            // for each file and property
            for (int i = 0; i < numNames; i++) {
              // X - Name
              // 1 - null Name Terminator
              String name = fm.readNullString();
              names[i] = name;
            }
          }
        }
        else if (header.equals("BULK")) {

          if (numFiles == -1) {
            numFiles = blockLength / 8;
            FieldValidator.checkNumFiles(numFiles);
          }

          offsets = new int[numFiles];
          lengths = new int[numFiles];

          // for each file
          for (int i = 0; i < numFiles; i++) {
            // 4 - File Offset (relative to the start of the BARG Block) [+12]
            int offset = fm.readInt();
            FieldValidator.checkOffset(offset, arcSize);
            offsets[i] = offset;

            // 4 - File Length (not including padding)
            int length = fm.readInt();
            FieldValidator.checkLength(length, arcSize);
            lengths[i] = length;
          }

        }
        else if (header.equals("BARG")) {
          relativeOffset = fm.getOffset();
          // for each file
          //   X - File Data
          //   0-15 - null Padding to a multiple of 16 bytes
          fm.skip(blockLength);
        }
        else {
          ErrorLogger.log("[SB_SBIN]: Unknown Header Type: " + header);

          // X - Unknown
          fm.skip(blockLength);
        }
      }

      if (numFiles == -1 || offsets.length <= 0 || lengths.length <= 0 || (numNames != -1 && nameIDs.length <= 0)) {
        return null;
      }

      Resource[] resources = new Resource[numFiles];
      TaskProgressManager.setMaximum(numFiles);

      // Process all the data
      for (int i = 0; i < numFiles; i++) {
        long offset = relativeOffset + offsets[i];
        FieldValidator.checkOffset(offset, arcSize);

        int length = lengths[i];

        String filename = "";
        if (numNames != -1) {
          filename = names[nameIDs[i]];
        }

        //path,name,offset,length,decompLength,exporter
        resources[i] = new Resource(path, filename, offset, length);

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
