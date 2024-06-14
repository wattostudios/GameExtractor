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
import java.util.HashMap;
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
public class Plugin_ISO extends ArchivePlugin {

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public Plugin_ISO() {

    super("ISO", "CD ISO Disk");

    //         read write replace rename
    setProperties(true, false, false, false);

    setGames("CD ISO");
    setExtensions("iso"); // MUST BE LOWER CASE
    setPlatforms("PC");

    // MUST BE LOWER CASE !!!
    //setFileTypes(new FileType("txt", "Text Document", FileType.TYPE_DOCUMENT),
    //             new FileType("bmp", "Bitmap Image", FileType.TYPE_IMAGE)
    //             );

    //setTextPreviewExtensions("colours", "rat", "screen", "styles"); // LOWER CASE

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

      // 32768 - null
      for (int i = 0; i < 256; i++) {
        if (fm.readByte() != 0) {
          return 0;
        }
      }
      rating += 25;

      return rating;

    }
    catch (Throwable t) {
      return 0;
    }
  }

  int realNumFiles = 0;

  HashMap<Long, Long> readOffsets = null;

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
      realNumFiles = 0;

      FileManipulator fm = new FileManipulator(path, false, 2048); // blocks are size 2048 bytes

      long arcSize = fm.getLength();

      // 32768 - null
      fm.skip(32768);

      // 1 - Type (1=Primary, 255=Terminator)
      int type = ByteConverter.unsign(fm.readByte());
      while (type <= 3) {
        if (type == 1) { // only want the primary partition
          break;
        }
        else {
          fm.skip(2047);
          type = ByteConverter.unsign(fm.readByte());
        }
      }

      if (type == 1) {
        // found the primary partition
      }
      else {
        return null;
      }

      // 5 - Header (CD001)
      // 1 - Version (1)
      // 1 - null
      // 32 - System Identifier Name (padded with spaces)
      // 32 - Volume Identifier Name (padded with spaces)
      // 8 - null
      // 4 - Number of Blocks for all the Volume Data
      // 4 - Number of Blocks for all the Volume Data (BIG)
      // 32 - null
      // 2 - Number of Disks in this Volume
      // 2 - Number of Disks in this Volume (BIG)
      // 2 - This Disk Number
      // 2 - This Disk Number (BIG)
      // 2 - Logical Block Size (2048)
      // 2 - Logical Block Size (BIG) (2048)
      // 4 - Path Table Size
      // 4 - Path Table Size (BIG)
      // 4 - Little-Endian Path Table Offset [*2048]
      // 4 - null
      // 4 - Big-Endian Path Table Offset (BIG) [*2048]
      // 4 - null
      fm.skip(155);

      int numFiles = Archive.getMaxFiles();

      Resource[] resources = new Resource[numFiles];
      TaskProgressManager.setMaximum(arcSize);

      readOffsets = new HashMap<Long, Long>();

      // 34 - Root Directory Entry
      readDirectory(path, fm, resources, "", 34, arcSize);

      resources = resizeResources(resources, realNumFiles);

      fm.close();

      return resources;

    }
    catch (Throwable t) {
      logError(t);
      return null;
    }
  }

  /**
   **********************************************************************************************
   * 
   **********************************************************************************************
   **/
  public void readDirectory(File path, FileManipulator fm, Resource[] resources, String dirName, int dirLength, long arcSize) {
    try {

      long startOffset = fm.getOffset();
      long endOffset = startOffset + dirLength;

      long[] dirOffsets = new long[1000]; // guess
      long[] dirLengths = new long[1000]; // guess
      String[] dirNames = new String[1000]; //guess
      int numDirs = 0;

      // remember this offset, so we don't process this directory again.
      readOffsets.put(startOffset, startOffset);

      while (fm.getOffset() < endOffset) {
        long startPos = fm.getOffset();

        // 1 - Entry Length
        int entryLength = fm.readByte();
        if (entryLength == 0) {
          // skip to the next 2048-byte multiple
          startPos += calculatePadding(startPos, 2048);
          fm.seek(startPos);
          continue;
        }

        // 1 - null
        fm.skip(1);

        // 4 - Extent Offset [*2048]
        long offset = fm.readInt() * 2048;
        FieldValidator.checkOffset(offset, arcSize);

        // 4 - Extent Offset (BIG) [*2048]
        fm.skip(4);

        // 4 - Extent Length
        int length = fm.readInt();
        FieldValidator.checkLength(length, arcSize);

        // 4 - Extent Length (BIG)
        fm.skip(4);

        // 7 - Date
        fm.skip(7);

        // 1 - File Flags (bit 1 means it points to a directory, rather than a file)
        int fileFlags = ByteConverter.unsign(fm.readByte());

        // 1 - File Unit
        // 1 - Interleave Gap Size
        // 2 - Volume Sequence Number
        // 2 - Volume Sequence Number (BIG)
        fm.skip(6);

        // 1 - Filename Length (including terminator)
        int filenameLength = ByteConverter.unsign(fm.readByte());

        // X - Filename
        // 2 - Filename Terminator (";1")
        String filename = fm.readString(filenameLength);
        if (filename.endsWith(";1")) {
          filename = filename.substring(0, filenameLength - 2);
        }

        // 0-1 - Padding to a multiple of 2 bytes
        if (filenameLength % 2 == 0) {
          fm.skip(1);
        }

        if ((fileFlags & 2) == 2) {
          // a directory

          // check that we haven't processed this directory before
          Long offsetCheck = readOffsets.get(offset);
          if (offsetCheck != null) {
            // processed before - skip
            continue;
          }

          if (filename.length() == 1 && filename.getBytes()[0] == 0) {
            filename = "";
          }
          else {
            filename = dirName + filename + File.separatorChar;
          }

          dirOffsets[numDirs] = offset;
          dirLengths[numDirs] = length;
          dirNames[numDirs] = filename;
          numDirs++;
        }
        else {
          // a file

          filename = dirName + filename;

          //path,name,offset,length,decompLength,exporter
          resources[realNumFiles] = new Resource(path, filename, offset, length);
          realNumFiles++;

          TaskProgressManager.setValue(offset);
        }

      }

      // now read all the subdirectories
      for (int i = 0; i < numDirs; i++) {
        fm.seek(dirOffsets[i]);

        readDirectory(path, fm, resources, dirNames[i], (int) dirLengths[i], arcSize);
      }

    }
    catch (Throwable t) {
      logError(t);
    }
  }

  /**
  **********************************************************************************************
  If an archive doesn't have filenames stored in it, the scanner can come here to try to work out
  what kind of file a Resource is. This method allows the plugin to provide additional plugin-specific
  extensions, which will be tried before any standard extensions.
  @return null if no extension can be determined, or the extension if one can be found
  **********************************************************************************************
  **/
  @Override
  public String guessFileExtension(Resource resource, byte[] headerBytes, int headerInt1, int headerInt2, int headerInt3, short headerShort1, short headerShort2, short headerShort3, short headerShort4, short headerShort5, short headerShort6) {

    /*
    if (headerInt1 == 2037149520) {
      return "js";
    }
    */

    return null;
  }

}
