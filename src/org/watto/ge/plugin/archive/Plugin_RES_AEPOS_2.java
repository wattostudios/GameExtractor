/*
 * Application:  Game Extractor
 * Author:       wattostudios
 * Website:      http://www.watto.org
 * Copyright:    Copyright (c) 2002-2026 wattostudios
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
import java.util.Arrays;

import org.watto.datatype.Resource;
import org.watto.ge.helper.FieldValidator;
import org.watto.ge.plugin.ArchivePlugin;
import org.watto.io.FileManipulator;
import org.watto.task.TaskProgressManager;

/**
**********************************************************************************************

**********************************************************************************************
**/
public class Plugin_RES_AEPOS_2 extends ArchivePlugin {

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public Plugin_RES_AEPOS_2() {

    super("RES_AEPOS_2", "RES_AEPOS_2");

    //         read write replace rename
    setProperties(true, false, false, false);

    setGames("Dungeon Hack");
    setExtensions("res"); // MUST BE LOWER CASE
    setPlatforms("PC");

    // MUST BE LOWER CASE !!!
    //setFileTypes(new FileType("txt", "Text Document", FileType.TYPE_DOCUMENT),
    //             new FileType("bmp", "Bitmap Image", FileType.TYPE_IMAGE)
    //             );

    //setTextPreviewExtensions("colours", "rat", "screen", "styles"); // LOWER CASE

    setCanScanForFileTypes(true);

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
      if (fm.readString(14).equals("AESOP/16 V1.00")) {
        rating += 50;
      }

      fm.skip(1);
      if (fm.readByte() == 104) {
        rating += 5;
      }

      long arcSize = fm.getLength();

      // Archive Size
      if (FieldValidator.checkEquals(fm.readInt(), arcSize)) {
        rating += 5;
      }

      getDirectoryFile(fm.getFile(), "tbl");
      rating += 15;

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

      /*
      FileManipulator fm = new FileManipulator(path, false);
      
      long arcSize = fm.getLength();
      
      // 14 - Header ("AESOP/16 V1.00")
      // 1 - null
      // 1 - Version (104)
      // 4 - Archive Length
      // 4 - Unknown
      // 4 - First Block Offset (36)
      // 8 - Unknown
      fm.seek(36);
      
      // 4 - Next Block Offset
      int nextBlockOffset = fm.readInt();
      FieldValidator.checkOffset(nextBlockOffset, arcSize);
      
      int numFiles = Archive.getMaxFiles();
      
      Resource[] resources = new Resource[numFiles];
      TaskProgressManager.setMaximum(arcSize);
      
      fm.getBuffer().setBufferSize(12); // small quick reads
      fm.seek(1);
      fm.seek(680);
      
      int realNumFiles = 0;
      while (fm.getOffset() < arcSize) {
        if (fm.getOffset() == nextBlockOffset) {
          // 4 - Next Block Offset
          nextBlockOffset = fm.readInt();
          FieldValidator.checkOffset(nextBlockOffset, arcSize);
      
          // 128 - null
          // 512 - Entries
          fm.skip(640);
        }
      
        // 4 - Unknown
        // 4 - null
        fm.skip(8);
      
        // 4 - File Length
        int length = fm.readInt();
        FieldValidator.checkLength(length, arcSize);
      
        // X - File Data
        long offset = fm.getOffset();
        fm.skip(length);
      
        if (length != 0) {
          String filename = Resource.generateFilename(realNumFiles);
      
          //path,name,offset,length,decompLength,exporter
          resources[realNumFiles] = new Resource(path, filename, offset, length);
      
          TaskProgressManager.setValue(offset);
          realNumFiles++;
        }
      }
      
      resources = resizeResources(resources, realNumFiles);
      
      fm.close();
      */

      long arcSize = (int) path.length();

      File sourcePath = getDirectoryFile(path, "tbl");
      FileManipulator fm = new FileManipulator(sourcePath, false);

      int numFiles = (int) sourcePath.length() / 4;

      long[] offsets = new long[numFiles];

      // Loop through directory
      for (int i = 0; i < numFiles; i++) {

        // 4 - File Offset
        long offset = fm.readInt();

        if (offset == 0) { // padding at the end of the directory
          numFiles = i; // the real numFiles
          break;
        }

        FieldValidator.checkOffset(offset, arcSize);
        offsets[i] = offset;
      }

      offsets[numFiles] = arcSize; // so we can calculate file sizes easily

      Arrays.sort(offsets, 0, numFiles + 1);

      Resource[] resources = new Resource[numFiles];
      TaskProgressManager.setMaximum(numFiles);

      // Loop through directory
      for (int i = 0; i < numFiles; i++) {
        long offset = offsets[i] + 12; // 12-byte header
        long length = offsets[i + 1] - offset;

        String filename = Resource.generateFilename(i);

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

    if (headerInt1 == 1297239878) {
      return "xmi";
    }
    else if (headerShort1 == 14931) {
      return "s";
    }
    else if (headerInt1 == 393217) {
      return "o";
    }

    return null;
  }

}
