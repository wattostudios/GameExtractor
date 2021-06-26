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
public class Plugin_HGO_HGOF extends ArchivePlugin {

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public Plugin_HGO_HGOF() {

    super("HGO_HGOF", "HGO_HGOF");

    //         read write replace rename
    setProperties(true, false, false, false);

    setGames("Crash Bandicoot: The Wrath of Cortex");
    setExtensions("hgo"); // MUST BE LOWER CASE
    setPlatforms("xbox");

    // MUST BE LOWER CASE !!!
    setFileTypes(new FileType("txm0", "Texture Image", FileType.TYPE_IMAGE));

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
      if (fm.readString(4).equals("HGOF")) {
        rating += 50;
      }

      fm.skip(4);

      // Header
      if (fm.readString(4).equals("NTBL")) {
        rating += 5;
      }

      long arcSize = fm.getLength();

      // Directory Length
      if (FieldValidator.checkLength(fm.readInt(), arcSize)) {
        rating += 5;
      }

      // Directory Length
      if (FieldValidator.checkLength(fm.readInt(), arcSize)) {
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

      // 4 - Header (HGOF)
      // 4 - File Length (slightly off by 0-3 bytes, presumably due to padding)
      fm.skip(8);

      int numFiles = Archive.getMaxFiles();

      Resource[] resources = new Resource[numFiles];
      TaskProgressManager.setMaximum(arcSize);

      int realNumFiles = 0;
      while (fm.getOffset() < arcSize) {
        // 4 - Block Header
        String blockHeader = fm.readString(4);

        //System.out.println(blockHeader + " at " + (fm.getOffset() - 4));

        // 4 - Block Length (including these 2 header fields)
        int blockLength = fm.readInt() - 8;

        if (!blockHeader.equals("TST0")) { // this block has a bad length, for some reason
          FieldValidator.checkLength(blockLength, arcSize);
        }

        if (blockHeader.equals("TST0")) {
          // Texture Block

          // 4 - Header (TSH0)
          // 4 - Block Length (including these 2 header fields) (12)
          fm.skip(8);

          // 4 - Number of Texture Images
          int numImages = fm.readInt();
          FieldValidator.checkNumFiles(numImages);

          // Loop through directory
          for (int i = 0; i < numImages; i++) {

            long offset = fm.getOffset();
            //System.out.println("Texture at " + offset);

            // 4 - Header (TXM0)
            fm.skip(4);

            // 4 - Block Length (including these 2 header fields)
            int length = fm.readInt();
            FieldValidator.checkLength(length, arcSize);
            fm.skip(length - 8);

            String filename = Resource.generateFilename(realNumFiles) + ".txm0";

            //path,name,offset,length,decompLength,exporter
            resources[realNumFiles] = new Resource(path, filename, offset, length);
            realNumFiles++;

            TaskProgressManager.setValue(offset);
          }

        }
        else {
          // Something else - skip it
          long offset = fm.getOffset() - 8;
          int length = blockLength + 8;
          String filename = Resource.generateFilename(realNumFiles) + "." + blockHeader.toLowerCase();

          //path,name,offset,length,decompLength,exporter
          resources[realNumFiles] = new Resource(path, filename, offset, length);
          realNumFiles++;

          TaskProgressManager.setValue(offset);

          fm.skip(blockLength);
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
