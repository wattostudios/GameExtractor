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
import org.watto.datatype.Resource;
import org.watto.ge.helper.FieldValidator;
import org.watto.ge.plugin.ArchivePlugin;
import org.watto.io.FileManipulator;
import org.watto.io.converter.IntConverter;
import org.watto.task.TaskProgressManager;

/**
**********************************************************************************************

**********************************************************************************************
**/
public class Plugin_RPK_EVOSBIG extends ArchivePlugin {

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public Plugin_RPK_EVOSBIG() {

    super("RPK_EVOSBIG", "RPK_EVOSBIG");

    //         read write replace rename
    setProperties(true, false, false, false);

    setGames("Motorstorm Apocalypse");
    setExtensions("rpk"); // MUST BE LOWER CASE
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
      if (fm.readString(8).equals("EVOSBIG ")) {
        rating += 50;
      }

      // 4 - Description Length (18)
      if (IntConverter.changeFormat(fm.readInt()) == 18) {
        rating += 5;
      }

      if (fm.readString(18).equals("Resource PacK file")) {
        rating += 15;
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

      // 8 - Header ("EVOSBIG ")
      // 4 - Description Length (18)
      // 18 - Description ("Resource PacK file")
      // 4 - Unknown
      // 4 - File Data Offset (38)
      fm.seek(38);

      int numFiles = Archive.getMaxFiles();

      TaskProgressManager.setMaximum(arcSize);

      // Loop through directory
      int realNumFiles = 0;
      long[] offsets = new long[numFiles];
      long offset = fm.getOffset();
      while (offset < arcSize) {

        // 8 - Header ("EVOSBIG ")
        if (fm.readByte() == 69) {
          if (fm.readByte() == 86) {
            if (fm.readByte() == 79) {
              if (fm.readByte() == 83) {
                if (fm.readByte() == 66) {
                  if (fm.readByte() == 73) {
                    if (fm.readByte() == 71) {
                      if (fm.readByte() == 32) {
                        // found a file
                        offsets[realNumFiles] = offset;
                        realNumFiles++;
                      }
                    }
                  }
                }
              }
            }
          }
        }

        offset++;
        fm.relativeSeek(offset);

        TaskProgressManager.setValue(offset);
      }

      numFiles = realNumFiles;

      offsets[realNumFiles] = arcSize;

      /*
      Resource[] resources = new Resource[numFiles - 1]; // -1 because the first file is just the directory
      
      // read the first file which has the filenames in it
      fm.seek(offsets[0]);
      
      
      //fm.skip(36);
      //long filenameOffset = IntConverter.changeFormat(fm.readInt()) + fm.getOffset() + 12;
      //FieldValidator.checkOffset(filenameOffset);
      //fm.seek(filenameOffset);
      
      fm.skip(52);
      
      String[] names = new String[numFiles];
      for (int i = 0; i < numFiles; i++) {
        // X - Filename
        // 1 - null Filename Terminator
        names[i] = fm.readNullString();
      }
      */

      Resource[] resources = new Resource[numFiles];
      for (int i = 0; i < numFiles; i++) {
        offset = offsets[i];
        long length = offsets[i + 1] - offset;
        String filename = Resource.generateFilename(i);

        //path,name,offset,length,decompLength,exporter
        resources[i] = new Resource(path, filename, offset, length);
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
