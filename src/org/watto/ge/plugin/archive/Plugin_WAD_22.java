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

import org.watto.datatype.Archive;
import org.watto.datatype.FileType;
import org.watto.datatype.Resource;
import org.watto.ge.helper.FieldValidator;
import org.watto.ge.plugin.ArchivePlugin;
import org.watto.io.FileManipulator;
import org.watto.io.converter.IntConverter;
import org.watto.io.converter.ShortConverter;
import org.watto.task.TaskProgressManager;

/**
**********************************************************************************************

**********************************************************************************************
**/
public class Plugin_WAD_22 extends ArchivePlugin {

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public Plugin_WAD_22() {

    super("WAD_22", "WAD_22");

    //         read write replace rename
    setProperties(true, false, false, false);

    setGames("God of War: Ascension");
    setExtensions("wad"); // MUST BE LOWER CASE
    setPlatforms("PS3");

    // MUST BE LOWER CASE !!!
    setFileTypes(new FileType("tx", "Texture Image", FileType.TYPE_IMAGE));

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
      if (fm.readInt() == 5376) {
        rating += 25; // root entry
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

      int numFiles = Archive.getMaxFiles();
      int realNumFiles = 0;

      Resource[] resources = new Resource[numFiles];
      TaskProgressManager.setMaximum(arcSize);

      // Loop through directory
      while (fm.getOffset() < arcSize) {

        // 2 - Block Type (21 = Root, 2 = GroupStart, 3 = GroupEnd, 22 = PopHeap, 1 = File, 7 = GoStreamInfo, 0 = Unknown (but ignore the block length))
        short blockType = ShortConverter.changeFormat(fm.readShort());

        // 2 - Unknown (0 = Unknown, 22 = File Data)
        short fileType = ShortConverter.changeFormat(fm.readShort());

        // 4 - Block Length
        int blockLength = IntConverter.changeFormat(fm.readInt());

        // 56 - Block Name or Filename (null terminated, filled with nulls)
        String blockName = fm.readNullString(56);

        // X - Block Data or File Data
        if (blockType != 0) {
          if (fileType != 0) {
            long offset = fm.getOffset();

            int length = blockLength;
            FieldValidator.checkLength(length, arcSize);

            String filename = blockName;

            if (filename.length() == 0) {
              filename = Resource.generateFilename(realNumFiles);
            }

            String fileTypeString = "." + fileType;
            if (fileType == 10) {
              fileTypeString = ".10"; // Reactive
            }
            else if (fileType == 12) {
              fileTypeString = ".RIB";
            }
            else if (fileType == 19) {
              fileTypeString = ".SCP";
            }
            else if (fileType == 20) {
              fileTypeString = ".SEM";
            }
            else if (fileType == 22) {
              fileTypeString = ".TX";
            }
            else if (fileType == 26) {
              fileTypeString = ".ENV"; // ENV or CDV
            }
            else if (fileType == 31) {
              fileTypeString = ".31"; // lite
            }
            else if (fileType == 45) {
              fileTypeString = ".45"; // scripts or something?
            }
            else if (fileType == 66) {
              fileTypeString = ".PEM"; // PEM or PDF or PTC
            }
            else if (fileType == 88) {
              fileTypeString = ".ANM";
            }
            else if (fileType == 117) {
              fileTypeString = ".MG";
            }
            else if (fileType == 119) {
              fileTypeString = ".MAT";
            }
            else if (fileType == 131) {
              fileTypeString = ".MDL";
            }
            else if (fileType == 181) {
              fileTypeString = ".181"; // not sure, filenames are HEX values
            }
            filename += fileTypeString;

            //path,name,offset,length,decompLength,exporter
            resources[realNumFiles] = new Resource(path, filename, offset, length);
            realNumFiles++;

            TaskProgressManager.setValue(offset);
          }

          fm.skip(blockLength);
        }

        // X - null Padding to a multiple of 16 bytes
        int padding = calculatePadding(fm.getOffset(), 16);
        fm.skip(padding);

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
