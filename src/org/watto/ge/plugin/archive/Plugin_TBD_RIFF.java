/*
 * Application:  Game Extractor
 * Author:       wattostudios
 * Website:      http://www.watto.org
 * Copyright:    Copyright (c) 2002-2021 wattostudios
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
import org.watto.ErrorLogger;
import org.watto.datatype.Archive;
import org.watto.datatype.Resource;
import org.watto.ge.helper.FieldValidator;
import org.watto.ge.helper.ResourceSorter_Offset;
import org.watto.ge.plugin.ArchivePlugin;
import org.watto.io.FileManipulator;
import org.watto.task.TaskProgressManager;

/**
**********************************************************************************************

**********************************************************************************************
**/
public class Plugin_TBD_RIFF extends ArchivePlugin {

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public Plugin_TBD_RIFF() {

    super("TBD_RIFF", "TBD_RIFF");

    //         read write replace rename
    setProperties(true, false, false, false);

    setGames("Metal Fatigue");
    setExtensions("tbd"); // MUST BE LOWER CASE
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

      // Header
      if (fm.readString(4).equals("RIFF")) {
        rating += 50;
      }

      long arcSize = fm.getLength();

      // Archive Size
      if (FieldValidator.checkEquals(fm.readInt() + 8, arcSize)) {
        rating += 5;
      }

      // Header
      if (fm.readString(4).equals("TBDF")) {
        rating += 5;
      }
      else {
        rating = 0;
      }

      // Header
      if (fm.readString(4).equals("TYPE")) {
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

      // 4 - Header (RIFF)
      // 4 - Archive Length [+8]
      // 4 - Header (TBDF)
      fm.skip(12);

      int numFiles = Archive.getMaxFiles();

      Resource[] resources = null;
      TaskProgressManager.setMaximum(arcSize);

      int realNumFiles = 0;
      int[] offsets = new int[numFiles];
      String[] types = new String[numFiles];
      while (fm.getOffset() < arcSize - 32) {

        // 4 - Header
        String header = fm.readString(4);

        // 4 - Block Length (not including these 2 header fields)
        int blockLength = fm.readInt();
        FieldValidator.checkLength(blockLength);

        if (header.equals("TYPE") || header.equals("IMPT") || header.equals("EXPT")) {
          // 4 - Number of Entries
          int numEntries = fm.readInt();
          FieldValidator.checkNumFiles(numEntries + 1); // +1 to allow 0 entries

          for (int i = 0; i < numEntries; i++) {
            // 4 - Hash?
            fm.skip(4);

            // 4 - File Offset (relative to the start of the File Data)
            int offset = fm.readInt();
            offsets[realNumFiles] = offset;
            types[realNumFiles] = header;

            realNumFiles++;
          }

          // 32 - null Padding
          fm.skip(32);
        }
        else if (header.equals("OFFS")) {
          // 4 - Number of Entries
          int numEntries = fm.readInt();
          FieldValidator.checkNumFiles(numEntries + 1); // +1 to allow 0 entries

          for (int i = 0; i < numEntries; i++) {
            // 4 - File Offset (relative to the start of the File Data)
            int offset = fm.readInt();
            offsets[realNumFiles] = offset;
            types[realNumFiles] = header;

            realNumFiles++;
          }

          // 32 - null Padding
          fm.skip(32);
        }
        else if (header.equals("DATA")) {
          int relOffset = (int) fm.getOffset();
          fm.skip(blockLength);

          numFiles = realNumFiles;
          TaskProgressManager.setMaximum(numFiles);

          resources = new Resource[numFiles];
          ResourceSorter_Offset[] sorter = new ResourceSorter_Offset[numFiles];

          // go through and add all the files
          for (int i = 0; i < numFiles; i++) {
            int offset = offsets[i] + relOffset;
            FieldValidator.checkOffset(offset, arcSize);

            String filename = Resource.generateFilename(i) + "." + types[i];

            //path,name,offset,length,decompLength,exporter
            Resource resource = new Resource(path, filename, offset);
            resources[i] = resource;

            sorter[i] = new ResourceSorter_Offset(resource);

            TaskProgressManager.setValue(i);
          }

          // Sort the Resources by their offsets
          Arrays.sort(sorter);

          // Put the resources back into the array based on their offset
          for (int i = 0; i < numFiles; i++) {
            resources[i] = sorter[i].getResource();
          }

        }
        else {
          ErrorLogger.log("[TBD_RIFF] Unknown block type: " + header);
          fm.skip(blockLength);
        }
      }

      if (resources == null) {
        return null;
      }
      calculateFileSizes(resources, arcSize);

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
