/*
 * Application:  Game Extractor
 * Author:       wattostudios
 * Website:      http://www.watto.org
 * Copyright:    Copyright (c) 2002-2024 wattostudios
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
public class Plugin_BIN_37 extends ArchivePlugin {

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public Plugin_BIN_37() {

    super("BIN_37", "BIN_37");

    //         read write replace rename
    setProperties(true, false, false, false);

    setGames("Michael Jackson: The Experience");
    setExtensions("bin"); // MUST BE LOWER CASE
    setPlatforms("Wii");

    // MUST BE LOWER CASE !!!
    setFileTypes(new FileType("bin_tex", "Texture Image", FileType.TYPE_IMAGE));

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

      if (FieldValidator.checkRange(fm.readShort(), 1, 1000)) { // guess max num images in the archive
        rating += 5;
      }

      fm.skip(12);

      long arcSize = fm.getLength();

      // File Length
      if (FieldValidator.checkLength(fm.readInt(), arcSize)) {
        rating += 5;
      }

      // File Length
      if (FieldValidator.checkLength(fm.readInt(), arcSize)) {
        rating += 4;
      }

      fm.skip(8);

      if (FieldValidator.checkWidth(fm.readShort())) {
        rating += 5;
      }

      if (FieldValidator.checkWidth(fm.readShort())) {
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

      FileManipulator fm = new FileManipulator(path, false, 32); // small quick reads

      long arcSize = fm.getLength();

      int numFiles = Archive.getMaxFiles();

      Resource[] resources = new Resource[numFiles];
      TaskProgressManager.setMaximum(arcSize);

      // Loop through directory
      int realNumFiles = 0;
      while (fm.getOffset() < arcSize - 8) { // to allow for the 2-byte footer, so leave a little buffer here to detect the end
        // 2 - Number of Images (or null for all subsequent images)
        // 2 - Unknown
        // 4 - Unknown
        // 4 - Unknown
        // 2 - Unknown
        fm.skip(14);

        // 4 - Image Data Length [+16] (including all these fields)
        int length = fm.readInt() + 16 - 34 + 4;
        FieldValidator.checkLength(length, arcSize);

        // 4 - Image Data Length [+16] (including all these fields)
        // 4 - Unknown (2)
        // 4 - Unknown
        fm.skip(12);

        long offset = fm.getOffset();
        // 2 - Image Width
        // 2 - Image Height
        // X - Image Data (CMPR format)
        fm.skip(length);

        String filename = Resource.generateFilename(realNumFiles) + ".bin_tex";

        //path,name,offset,length,decompLength,exporter
        resources[realNumFiles] = new Resource(path, filename, offset, length);

        TaskProgressManager.setValue(offset);
        realNumFiles++;
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
