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
public class Plugin_TEX_7 extends ArchivePlugin {

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public Plugin_TEX_7() {

    super("TEX_7", "TEX_7");

    //         read write replace rename
    setProperties(true, false, false, false);

    setGames("Shrek Smash n' Crash Racing");
    setExtensions("tex"); // MUST BE LOWER CASE
    setPlatforms("GameCube");

    // MUST BE LOWER CASE !!!
    setFileTypes(new FileType("tex_tex", "Texture Image", FileType.TYPE_IMAGE));

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

      if (fm.readInt() == 22) {
        rating += 5;
      }

      long arcSize = fm.getLength();

      // Archive Size
      if (FieldValidator.checkEquals(fm.readInt() + 12, arcSize)) {
        rating += 5;
      }

      fm.skip(16);

      // Number Of Files
      if (FieldValidator.checkNumFiles(fm.readShort())) {
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

      FileManipulator fm = new FileManipulator(path, false, 256); // small quick reads

      long arcSize = fm.getLength();

      // 4 - Unknown (22)
      // 4 - Archive Length [+12]
      // 4 - Unknown

      // 4 - Unknown (1)
      // 4 - Unknown (4)
      // 4 - Unknown
      fm.skip(24);

      // 2 - Number of Images
      short numFiles = fm.readShort();
      FieldValidator.checkNumFiles(numFiles);

      // 2 - Unknown (3)
      fm.skip(2);

      Resource[] resources = new Resource[numFiles];
      TaskProgressManager.setMaximum(numFiles);

      // Loop through directory
      for (int i = 0; i < numFiles; i++) {
        // 4 - Unknown (21)
        // 4 - Image Data Length [-24]
        // 4 - Unknown

        // 4 - Unknown (1)
        fm.skip(16);

        // 4 - Image Data Length
        int length = fm.readInt();
        FieldValidator.checkLength(length, arcSize);

        // 4 - Unknown
        fm.skip(4);

        long offset = fm.getOffset();

        // 4 - Unknown (6) (BIG)
        // 4 - Unknown (4354) (BIG)
        // 4 - null

        // 4 - Unknown (1) (BIG)
        // 4 - Unknown (1) (BIG)
        // 4 - null
        fm.skip(24);

        // 32 - Filename (null terminated, filled with nulls)
        // 32 - Junk
        String filename = fm.readNullString(64) + ".tex_tex";
        //System.out.println(offset + "\t" + filename);

        // 4 - Unknown (BIG)
        // 2 - Image Width (BIG)
        // 2 - Image Height (BIG)
        // 4 - Unknown
        // 4 - Unknown (0/1) (BIG)
        // 4 - Unknown
        fm.skip(20);

        // X - Image Data (CMPR)
        fm.relativeSeek(offset + length);

        // 4 - Unknown (3)
        fm.skip(4);

        // 4 - Extra Data Length (can be null)
        int extraLength = fm.readInt();
        FieldValidator.checkLength(extraLength, arcSize);

        // X - Extra Data
        fm.skip(extraLength);

        // 4 - Unknown
        fm.skip(4);

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
