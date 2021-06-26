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
import org.watto.task.TaskProgressManager;

/**
**********************************************************************************************

**********************************************************************************************
**/
public class Plugin_OPF_OUTFORCE extends ArchivePlugin {

  /**
  **********************************************************************************************

  **********************************************************************************************
  **/
  public Plugin_OPF_OUTFORCE() {

    super("OPF_OUTFORCE", "OPF_OUTFORCE");

    //         read write replace rename
    setProperties(true, false, false, false);

    setGames("The Outforce");
    setExtensions("opf"); // MUST BE LOWER CASE
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
      if (fm.readString(23).equals("Outforce Packed Content")) {
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

      // 23 - Header (Outforce Packed Content)
      // 4 - Version? (29)
      fm.skip(27);

      // 2 - Project Name Length (21)
      short nameLength = fm.readShort();
      FieldValidator.checkFilenameLength(nameLength);

      // 21 - Project Name (Outforce Base Project)
      fm.skip(nameLength);

      // 2 - Developer Name Length (8)
      nameLength = fm.readShort();
      FieldValidator.checkFilenameLength(nameLength);

      // 8 - Developer Name (O3 Games)
      fm.skip(nameLength);

      // 2 - Email Length (16)
      nameLength = fm.readShort();
      FieldValidator.checkFilenameLength(nameLength);

      // 16 - Email (Info@o3games.com)
      fm.skip(nameLength);

      // 4 - Description Length (including null terminator) (132)
      nameLength = (short) fm.readInt();
      FieldValidator.checkFilenameLength(nameLength);

      // 131 - Description
      // 1 - null Terminator
      fm.skip(nameLength);

      // 2 - Unknown (1)
      // 8 - null
      fm.skip(10);

      int numFiles = Archive.getMaxFiles();
      int realNumFiles = 0;

      Resource[] resources = new Resource[numFiles];
      TaskProgressManager.setMaximum(arcSize);

      char nullChar = (char) 0;

      // 4 - Number of Images
      int numImages = fm.readInt() + 6;
      FieldValidator.checkNumFiles(numImages);

      // Loop through directory (IMAGES)
      for (int i = 0; i < numImages; i++) {
        long startOffset = fm.getOffset();

        // 2 - Filename Length
        nameLength = fm.readShort();
        FieldValidator.checkFilenameLength(nameLength);

        // X - Filename
        String filename = fm.readString(nameLength) + ".jpg";

        //System.out.println(startOffset + "\t" + filename);

        if (filename.charAt(0) == nullChar) {
          // not real names
          filename = Resource.generateFilename(realNumFiles) + ".jpg";

          // short-hand skip
          // 41 - Unknown
          //fm.seek(startOffset + 53);

          fm.seek(startOffset + 17);
          int remainingPadding = fm.readInt();
          FieldValidator.checkLength(remainingPadding);
          fm.skip(remainingPadding);

          /*
          
          // 4 - Number of Colors (can be null)
          int numColors1 = fm.readInt();
          
          // 4 - Number of Colors (can be null)
          int numColors2 = fm.readInt();
          
          // X - Source Color Palette
          if (numColors1 == 0) {
            if (numColors2 == 0) {
              // no color palette
            }
            else {
              fm.skip(numColors2 * 4);
            }
          }
          else {
            fm.skip(numColors1 * 4);
          }
          */

        }
        else {

          // 2 - Source File Path Length
          nameLength = fm.readShort();
          FieldValidator.checkFilenameLength(nameLength);

          // X - Source File Path
          fm.skip(nameLength);

          // 2 - Drive Length
          nameLength = fm.readShort();
          //FieldValidator.checkFilenameLength(nameLength); // no check - can be null

          // X - Drive
          fm.skip(nameLength);

          // 4 - Image Width
          // 4 - Image Height
          // 4 - Red Bits?
          // 4 - Green Bits?
          // 4 - Blue Bits?
          // 4 - Alpha Bits?
          // 4 - Unknown (257)
          // 4 - Unknown
          // 4 - Unknown (32)

          // 70 - Unknown
          fm.skip(70);

          int remainingPadding = fm.readInt();
          FieldValidator.checkLength(remainingPadding);
          fm.skip(remainingPadding);
        }

        // 4 - File Length
        int length = fm.readInt();
        FieldValidator.checkLength(length, arcSize);

        // X - File Data (JPEG Image)
        long offset = fm.getOffset();
        fm.skip(length);

        //path,name,offset,length,decompLength,exporter
        resources[realNumFiles] = new Resource(path, filename, offset, length);
        realNumFiles++;

        TaskProgressManager.setValue(offset);
      }

      // 4 - Number of Scripts
      int numScripts = fm.readInt();
      FieldValidator.checkNumFiles(numScripts);

      // Loop through directory (SCRIPTS)
      for (int i = 0; i < numScripts; i++) {
        // 2 - Filename Length
        nameLength = fm.readShort();
        FieldValidator.checkFilenameLength(nameLength);

        // X - Filename
        String filename = fm.readString(nameLength) + ".unknown";

        //System.out.println(fm.getOffset() + "\t" + filename);

        // 899 - File Data
        int length = 899;
        long offset = fm.getOffset();
        fm.skip(length);

        //path,name,offset,length,decompLength,exporter
        resources[realNumFiles] = new Resource(path, filename, offset, length);
        realNumFiles++;

        TaskProgressManager.setValue(offset);
      }

      // 4 - Number of Classes
      int numClasses = fm.readInt() * 2;
      FieldValidator.checkNumFiles(numClasses);

      // Loop through directory (CLASSES)
      for (int i = 0; i < numClasses; i++) {
        long offset = fm.getOffset();

        // 2 - Class Type Length
        nameLength = fm.readShort();
        FieldValidator.checkFilenameLength(nameLength);

        // X - Class Type (CBaseClass, CUnit, etc)
        String classType = fm.readString(nameLength);
        //System.out.println(offset + "\t" + classType);

        // 5 - Unknown
        // 4 - null
        fm.skip(9);

        // 2 - Filename Length
        nameLength = fm.readShort();
        FieldValidator.checkFilenameLength(nameLength);

        // X - Filename
        String filename = fm.readString(nameLength) + "." + classType;

        // 26 - null (can have some data in here, but usually null)
        // 4 - Unknown (16256)
        // 4 - Unknown (16256)
        // 4 - Unknown (16256)
        // X - File Data (complicated structure)
        //fm.skip(38);
        fm.skip(100);

        long currentOffset = fm.getOffset();
        while (currentOffset < arcSize) {
          fm.relativeSeek(currentOffset);
          if (fm.readInt() == 16256 && fm.readInt() == 16256 && fm.readInt() == 16256 && fm.readInt() == 0) {
            // found the next class, so go back a bit and try to find the class type
            long startOffset = currentOffset;
            currentOffset -= 80;
            fm.seek(currentOffset);
            for (int b = 0; b < 80; b++) {
              fm.relativeSeek(currentOffset);
              if (fm.readShort() < 20 && fm.readString(1).equals("C")) {
                // check that the next letter is also uppercase
                String nextLetter = fm.readString(1);
                if (nextLetter.equals(nextLetter.toUpperCase())) {
                  // yep, found the start of the class
                  fm.seek(currentOffset);
                  break;
                }
                else {
                  // nope, broke too early, keep looking
                }

              }
              currentOffset++;

            }
            if (currentOffset >= startOffset) {
              // didn't really find the next class
            }
            else {
              // yep, found it, and we hit the break; above
              break;
            }

          }
          currentOffset++;
        }

        long length = currentOffset - offset;

        //path,name,offset,length,decompLength,exporter
        resources[realNumFiles] = new Resource(path, filename, offset, length);
        realNumFiles++;

        TaskProgressManager.setValue(offset);

        if (currentOffset >= arcSize) {
          break; // end of archive 
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
