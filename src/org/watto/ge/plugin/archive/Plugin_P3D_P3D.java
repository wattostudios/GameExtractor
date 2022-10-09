/*
 * Application:  Game Extractor
 * Author:       wattostudios
 * Website:      http://www.watto.org
 * Copyright:    Copyright (c) 2002-2022 wattostudios
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
import org.watto.io.converter.ByteConverter;
import org.watto.task.TaskProgressManager;

/**
**********************************************************************************************

**********************************************************************************************
**/
public class Plugin_P3D_P3D extends ArchivePlugin {

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public Plugin_P3D_P3D() {

    super("P3D_P3D", "P3D_P3D");

    //         read write replace rename
    setProperties(true, false, false, false);

    setGames("The Incredible Hulk: Ultimate Destruction");
    setExtensions("p3d"); // MUST BE LOWER CASE
    setPlatforms("PS2");

    // MUST BE LOWER CASE !!!
    //setFileTypes(new FileType("txt", "Text Document", FileType.TYPE_DOCUMENT),
    //             new FileType("bmp", "Bitmap Image", FileType.TYPE_IMAGE)
    //             );

    //setTextPreviewExtensions("colours", "rat", "screen", "styles"); // LOWER CASE

    setCanScanForFileTypes(true); // as the files will be "BMP", for example, but need to be "PNG"

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
      if (fm.readString(3).equals("P3D")) {
        rating += 45;
      }

      // 4 - Version (3)
      if (fm.readByte() == 3) {
        rating += 5;
      }

      fm.skip(4);

      long arcSize = fm.getLength();

      // Archive Size
      if (FieldValidator.checkEquals(fm.readInt(), arcSize)) {
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

      // 3 - Header (P3D)
      // 1 - Version (3)
      // 4 - Unknown
      // 4 - Archive Length
      fm.skip(12);

      int numFiles = Archive.getMaxFiles();

      Resource[] resources = new Resource[numFiles];
      TaskProgressManager.setMaximum(arcSize);

      // 4 - Type
      int realNumFiles = 0;
      while (fm.getOffset() < arcSize) {
        // 4 - Unknown (102400 = file)
        int type = fm.readInt();

        if (type == 102400) {
          // File

          // 4 - Unknown
          // 4 - Unknown
          fm.skip(8);

          // 1 - Filename Length (including null padding)
          int filenameLength = ByteConverter.unsign(fm.readByte());

          // X - Filename
          // 0-3 - null Padding to make the filename a multiple of 4 bytes
          String filename = fm.readNullString(filenameLength);
          FieldValidator.checkFilename(filename);

          // 4 - Unknown
          // 4 - Unknown (128/8)
          // 4 - Unknown (128/8)
          // 4 - Unknown (8)
          // 4 - null
          // 4 - Unknown (3/1)
          // 4 - Unknown (1)
          // 8 - null
          // 4 - Unknown
          // 4 - Unknown
          // 4 - Unknown
          fm.skip(48);

          // 1 - Filename Length (including null padding)
          filenameLength = ByteConverter.unsign(fm.readByte());

          // X - Filename
          // 0-3 - null Padding to make the filename a multiple of 4 bytes
          fm.skip(filenameLength);

          // 4 - Unknown
          // 4 - Unknown (128/8)
          // 4 - Unknown (128/8)
          // 4 - Unknown (8)
          // 4 - Unknown (1)
          // 4 - null
          // 4 - Unknown (1)
          // 4 - Unknown
          // 4 - Unknown
          // 4 - Unknown
          fm.skip(40);

          // 4 - File Length
          int length = fm.readInt();
          FieldValidator.checkLength(length);

          // X - File Data
          long offset = fm.getOffset();
          fm.skip(length);

          //path,name,offset,length,decompLength,exporter
          resources[realNumFiles] = new Resource(path, filename, offset, length);
          realNumFiles++;

          TaskProgressManager.setValue(offset);

        }
        else {
          // Text

          // 4 - Description Length (including these header fields)
          int length = fm.readInt() - 8;
          FieldValidator.checkLength(length, arcSize);

          fm.skip(length);
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
