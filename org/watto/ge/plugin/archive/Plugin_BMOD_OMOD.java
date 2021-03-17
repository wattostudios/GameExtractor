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
import org.watto.datatype.Archive;
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
public class Plugin_BMOD_OMOD extends ArchivePlugin {

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public Plugin_BMOD_OMOD() {

    super("BMOD_OMOD", "BMOD_OMOD");

    //         read write replace rename
    setProperties(true, false, false, false);

    setGames("Platoon");
    setExtensions("bmod"); // MUST BE LOWER CASE
    setPlatforms("PC");

    // MUST BE LOWER CASE !!!
    setFileTypes(new FileType("time", "Timestamp", FileType.TYPE_DOCUMENT),
        new FileType("mate", "Material Reference", FileType.TYPE_OTHER),
        new FileType("text", "Texture Reference", FileType.TYPE_DOCUMENT),
        new FileType("obst", "Object Model", FileType.TYPE_MODEL));

    setTextPreviewExtensions("text", "time"); // LOWER CASE

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
      if (fm.readString(4).equals("OMOD")) {
        rating += 50;
      }

      long arcSize = fm.getLength();

      // 4 - File Length [+32]
      int length = fm.readInt();
      if (length == arcSize || length + 32 == arcSize) {
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

      FileManipulator fm = new FileManipulator(path, false, 24); // 24 for quick reading

      long arcSize = fm.getLength();

      // 4 - Header (OMOD)
      // 4 - File Length [+32]
      // 2 - Unknown (13)
      // 2 - Unknown (1)
      fm.skip(12);

      int numFiles = Archive.getMaxFiles();

      Resource[] resources = new Resource[numFiles];
      TaskProgressManager.setMaximum(arcSize);

      // Loop through directory
      int realNumFiles = 0;
      while (fm.getOffset() < arcSize) {
        // 4 - Block Name
        String blockType = fm.readString(4);

        // 4 - Block Length (including these 2 header fields)
        int blockLength = fm.readInt() - 8; // -8 to skip the 2 header fields

        if (blockType.equals("DUMY")) {
          // 4 - Object Number (incremental from 0)
          fm.skip(4);

          // 4 - Name Length
          int nameLength = fm.readInt();
          FieldValidator.checkFilenameLength(nameLength);

          // X - Name
          fm.skip(nameLength);

          // 68 - Unknown
          fm.skip(68);
        }
        else if (blockType.equals("OBST")) {
          long offset = fm.getOffset() - 8;
          int length = blockLength + 8;

          // 4 - Object Number (incremental from 0)
          fm.skip(4);

          // 4 - Name Length
          int nameLength = fm.readInt();
          FieldValidator.checkFilenameLength(nameLength);

          // X - Name
          String filename = fm.readString(nameLength) + "." + blockType;

          // 68 - Unknown
          fm.skip(68);

          int remainingLength = blockLength - 68 - 4 - 4 - nameLength;
          FieldValidator.checkLength(remainingLength, arcSize);

          fm.skip(remainingLength);

          //path,name,offset,length,decompLength,exporter
          resources[realNumFiles] = new Resource(path, filename, offset, length);
          realNumFiles++;

          TaskProgressManager.setValue(offset);
        }
        else {
          //ErrorLogger.log("[BMOD_OMOD] Unknown block type: " + blockType);
          //fm.skip(blockLength);

          long offset = fm.getOffset();

          int length = blockLength;
          FieldValidator.checkLength(length, arcSize);

          fm.skip(length);

          String filename = Resource.generateFilename(realNumFiles) + "." + blockType;

          //path,name,offset,length,decompLength,exporter
          resources[realNumFiles] = new Resource(path, filename, offset, length);
          realNumFiles++;

          TaskProgressManager.setValue(offset);

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
