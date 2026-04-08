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

import org.watto.datatype.FileType;
import org.watto.datatype.Resource;
import org.watto.ge.helper.FieldValidator;
import org.watto.ge.plugin.ArchivePlugin;
import org.watto.io.FileManipulator;
import org.watto.task.TaskProgressManager;

/**
**********************************************************************************************

**********************************************************************************************
**/
public class Plugin_PPR_2RPX extends ArchivePlugin {

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public Plugin_PPR_2RPX() {

    super("PPR_2RPX", "PPR_2RPX");

    //         read write replace rename
    setProperties(true, false, false, false);

    setGames("Ms. Splosion Man");
    setExtensions("ppr"); // MUST BE LOWER CASE
    setPlatforms("PC");

    // MUST BE LOWER CASE !!!
    setFileTypes(new FileType("fubi", "Mesh Indices", FileType.TYPE_MODEL),
        new FileType("fubv", "Mesh Vertices", FileType.TYPE_OTHER));

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

      // Header
      if (fm.readString(4).equals("2RPX")) {
        rating += 50;
      }

      long arcSize = fm.getLength();

      // File Data Offset
      if (FieldValidator.checkOffset(fm.readInt(), arcSize)) {
        rating += 5;
      }

      // File Data Length
      if (FieldValidator.checkLength(fm.readInt(), arcSize)) {
        rating += 5;
      }

      // Nulls
      if (fm.readLong() == 0) {
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

      FileManipulator fm = new FileManipulator(path, false);

      long arcSize = fm.getLength();

      // 4 - Header (2RPX)
      fm.skip(4);

      // 4 - File Data Offset [+2048]
      int dataOffset = fm.readInt() + 2048;
      FieldValidator.checkOffset(dataOffset, arcSize);

      // 4 - File Data Length (including padding at the end)
      // 2036 - null Padding to a multiple of 2048 bytes
      fm.skip(2040);

      // 4 - Number Of Files
      int numFiles = fm.readInt();
      FieldValidator.checkNumFiles(numFiles);

      Resource[] resources = new Resource[numFiles];
      TaskProgressManager.setMaximum(numFiles);

      // Loop through directory
      String[] types = new String[numFiles];
      int[] offsets = new int[numFiles];
      int[] lengths = new int[numFiles];
      int[] filenameOffsets = new int[numFiles];
      for (int i = 0; i < numFiles; i++) {

        // 4 - File Type String (eg D2XT, RESU, ...)
        String type = fm.readString(4);
        types[i] = type;

        // 4 - File Entry Offset [+2048]
        int offset = fm.readInt() + 2048;
        FieldValidator.checkOffset(offset, arcSize);
        offsets[i] = offset;

        // 4 - File Entry Length
        int length = fm.readInt();
        FieldValidator.checkLength(length, arcSize);
        lengths[i] = length;

        // 4 - Filename Offset [+2048]
        int filenameOffset = fm.readInt() + 2048;
        FieldValidator.checkOffset(filenameOffset, arcSize);
        filenameOffsets[i] = filenameOffset;
      }

      String[] filenames = new String[numFiles];
      for (int i = 0; i < numFiles; i++) {
        fm.relativeSeek(filenameOffsets[i]);

        // X - Filename (null)
        String filename = fm.readNullString();
        FieldValidator.checkFilename(filename);
        filenames[i] = filename;
      }

      for (int i = 0; i < numFiles; i++) {
        String type = types[i];

        int offset = offsets[i];
        int length = lengths[i];
        String filename = filenames[i];

        if (type.equals("RESU")) {
          // file is stored at the offset/length as already retrieved
        }
        else {
          // file is stored in the File Data at block
          fm.relativeSeek(offset);

          // 4 - File Offset (relative to the File Data Offset)
          offset = fm.readInt() + dataOffset;
          FieldValidator.checkOffset(offset, arcSize);

          // 4 - File Length
          length = fm.readInt();
          FieldValidator.checkLength(length, arcSize);

          if (type.equals("D2XT")) {
            type += ".dds";
          }
          filename += "." + type;
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
