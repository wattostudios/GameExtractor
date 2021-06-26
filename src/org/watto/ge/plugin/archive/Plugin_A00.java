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
import org.watto.ge.plugin.ExporterPlugin;
import org.watto.ge.plugin.exporter.Exporter_LH6;
import org.watto.io.FileManipulator;
import org.watto.task.TaskProgressManager;

/**
**********************************************************************************************

**********************************************************************************************
**/
public class Plugin_A00 extends ArchivePlugin {

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public Plugin_A00() {

    super("A00", "A00");

    //         read write replace rename
    setProperties(true, false, false, false);

    setGames("Steel Panthers 2",
        "Steel Panthers 3");
    setExtensions("a00"); // MUST BE LOWER CASE
    setPlatforms("PC");

    // MUST BE LOWER CASE !!!
    setFileTypes(new FileType("shp", "SHP Image Archive", FileType.TYPE_ARCHIVE),
        new FileType("mel", "MEL Audio File", FileType.TYPE_AUDIO));

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
      if (fm.readShort() == -5536) {
        rating += 50;
      }

      if (fm.readShort() == 40) {
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

      ExporterPlugin exporter = Exporter_LH6.getInstance();

      // RESETTING GLOBAL VARIABLES

      FileManipulator fm = new FileManipulator(path, false);

      long arcSize = fm.getLength();

      // 2 - Header ((bytes)96,234)
      fm.skip(2);

      // 2 - Basic Header Size (40) [+10]
      int headerSize = fm.readShort() + 10;
      FieldValidator.checkPositive(headerSize);

      // 4 - Unknown
      // 4 - Unknown
      // 4 - Unknown
      // 18 - null
      // X - Archive Filename
      // 2 - null
      // 2 - Unknown
      // 4 - Unknown
      fm.skip(headerSize - 4); // we've already read 4 bytes of it

      int numFiles = Archive.getMaxFiles();

      Resource[] resources = new Resource[numFiles];
      TaskProgressManager.setMaximum(arcSize);

      // Loop through directory
      int realNumFiles = 0;
      while (fm.getOffset() < arcSize - 8) { // ultimately only needs to be -4, to skip the archive footer
        // 2 - Header ((bytes)96,234)
        fm.skip(2);

        // 2 - Basic Header Size [+10]
        headerSize = fm.readShort() + 10;
        FieldValidator.checkPositive(headerSize);

        // 1 - First Header Size
        // 1 - Archiver Version Number
        // 1 - Minimum Version Required to Extract
        // 1 - Host OS
        // 1 - Flags
        fm.skip(5);

        // 1 - Compression Type (0=uncompressed, 1-4 = compression factor)
        int compression = fm.readByte();

        // 1 - File Type (eg 0=Binary)
        // 1 - Reserved
        // 4 - Modified Timestamp
        fm.skip(6);

        // 4 - Compressed Length
        int length = fm.readInt();
        FieldValidator.checkLength(length, arcSize);

        // 4 - Decompressed Length
        int decompLength = fm.readInt();
        FieldValidator.checkLength(decompLength);

        // 4 - CRC
        // 2 - null
        // 4 - Unknown (32)
        fm.skip(10);

        // X - Filename
        int filenameLength = headerSize - 42;
        FieldValidator.checkFilenameLength(filenameLength);

        String filename = fm.readString(filenameLength);

        // 2 - null
        // 2 - Unknown
        // 4 - Unknown
        fm.skip(8);

        // X - File Data (Compressed)
        long offset = fm.getOffset();
        fm.skip(length);

        //path,name,offset,length,decompLength,exporter
        Resource resource = new Resource(path, filename, offset, length, decompLength);
        if (compression == 1 || compression == 2 || compression == 3) {
          resource.setExporter(exporter);
        }

        resources[realNumFiles] = resource;
        realNumFiles++;

        TaskProgressManager.setValue(offset);
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
