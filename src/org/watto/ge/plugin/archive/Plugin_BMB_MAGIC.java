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
public class Plugin_BMB_MAGIC extends ArchivePlugin {

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public Plugin_BMB_MAGIC() {
    super("BMB_MAGIC", "BMB_MAGIC");

    //         read write replace rename
    setProperties(true, false, false, false);

    setGames("Armobiles");
    setExtensions("bmb");
    setPlatforms("PC");

    // MUST BE LOWER CASE !!!
    setFileTypes(new FileType("bmb_tex", "Texture Image", FileType.TYPE_IMAGE));

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
      if (fm.readString(7).equals("magic  ")) {
        rating += 50;
      }
      fm.skip(1);

      fm.skip(16);

      // Number Of Files
      if (FieldValidator.checkNumFiles(fm.readInt())) {
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

      // RESETTING THE GLOBAL VARIABLES

      FileManipulator fm = new FileManipulator(path, false);

      long arcSize = (int) fm.getLength();

      // 8 - Header ("magic  " + null)
      // 4 - Unknown
      // 4 - Hash?
      // 4 - null
      // 4 - Unknown (4)
      fm.skip(24);

      // 4 - Number Of Files (including blank files)
      int numFiles = fm.readInt();
      FieldValidator.checkNumFiles(numFiles);

      // 4 - null
      // 4 - Unknown
      // 12 - null
      // 4 - Unknown (1)
      // 4 - File ID Starting Point [+1] (2199)
      // 4 - File ID Starting Point [+1] (2199)
      // 4 - Unknown (201)
      // 4 - File ID Starting Point [+1] (2199)
      // 4 - Unknown (219)
      // 4 - Unknown (-1)
      // 4 - null
      // 4 - Unknown (24)
      fm.skip(56);

      // 4 - Files Directory Offset (156)
      long dirOffset = fm.readInt();
      FieldValidator.checkOffset(dirOffset, arcSize);

      // 4 - null
      // 4 - File ID Starting Point [-1] (2201)
      // 4 - null
      // 4 - Unknown (16)
      // 4 - Files Directory Offset (156)
      // 4 - Archive Length [+156 for the Archive Header]
      // 8 - null
      // 4 - Unknown (15)
      // 4 - Archive Length
      // 4 - null
      // 4 - File ID Starting Point [-1] (2201)
      // 4 - null
      // 4 - Unknown (32)
      // 4 - Archive Length
      // 4 - null
      // 4 - File ID Starting Point [-1] (2201)
      fm.seek(dirOffset);

      Resource[] resources = new Resource[numFiles];

      TaskProgressManager.setMaximum(numFiles);

      // Loop through directory
      int realNumFiles = 0;
      for (int i = 0; i < numFiles; i++) {
        // 4 - File Offset
        long offset = fm.readInt();
        FieldValidator.checkOffset(offset, arcSize);

        // 4 - File Length
        long length = fm.readInt();
        FieldValidator.checkLength(length, arcSize);

        // 4 - Unknown
        // 4 - Unknown
        // 4 - File ID (incremental from 2200)
        // 4 - File ID (incremental from 2200)
        // 4 - File ID (incremental from 2200)
        // 4 - File/Blank ID (0=blank file, 1=file)
        // 4 - File/Blank ID (0=blank file, 1=file)
        // 4 - null
        // 4 - File/Blank ID (0=blank file, 1=file)
        // 4 - null
        fm.skip(40);

        if (length == 0) {
          // blank file
        }
        else {

          String filename = Resource.generateFilename(realNumFiles);

          //path,id,name,offset,length,decompLength,exporter
          resources[realNumFiles] = new Resource(path, filename, offset, length);

          TaskProgressManager.setValue(i);
          realNumFiles++;
        }
      }

      if (realNumFiles < numFiles) {
        resources = resizeResources(resources, realNumFiles);
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

    if (headerInt1 == 65538) {
      return "bmb_tex";
    }

    return null;
  }

}
