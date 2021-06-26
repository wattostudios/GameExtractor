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
import org.watto.datatype.Resource;
import org.watto.ge.helper.FieldValidator;
import org.watto.ge.plugin.ArchivePlugin;
import org.watto.io.FileManipulator;
import org.watto.task.TaskProgressManager;

/**
**********************************************************************************************

**********************************************************************************************
**/
public class Plugin_DAT_87 extends ArchivePlugin {

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public Plugin_DAT_87() {

    super("DAT_87", "DAT_87");

    //         read write replace rename
    setProperties(true, false, false, false);

    setGames("Syndicate Wars");
    setExtensions("dat"); // MUST BE LOWER CASE
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
        rating += 10;
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

      fm.seek(arcSize - 60);

      // 4 - Directory 1 Offset
      long dir1Offset = fm.readInt();
      FieldValidator.checkOffset(dir1Offset, arcSize);

      // 4 - Directory 1 Length
      int dir1Length = fm.readInt();
      int num1Files = (dir1Length - 32) / 32;
      FieldValidator.checkNumFiles(num1Files);

      long endOfDir1 = dir1Offset + dir1Length;

      // 24 - null
      fm.skip(24);

      // 4 - Directory 2 Offset (relative to the end of Directory 1)
      long dir2Offset = fm.readInt() + endOfDir1;
      FieldValidator.checkOffset(dir2Offset, arcSize);

      // 4 - Directory 2 Length
      int num2Files = (fm.readInt() - 32) / 32;
      FieldValidator.checkNumFiles(num2Files);

      // 16 - null
      // 4 - Footer Offset

      int numFiles = num1Files + num2Files;

      Resource[] resources = new Resource[numFiles];
      TaskProgressManager.setMaximum(numFiles);

      fm.seek(dir1Offset + 32);

      // Loop through directory
      for (int i = 0; i < num1Files; i++) {
        // 18 - Filename (null terminated, filled with nulls)
        String filename = fm.readNullString(18);
        FieldValidator.checkFilename(filename);

        int dotPos = filename.lastIndexOf('.');
        if (dotPos > 0) {
          filename = filename.substring(0, dotPos) + "_HIGH" + filename.substring(dotPos);
        }

        // 8 - File Offset
        int offset = fm.readInt();
        FieldValidator.checkOffset(offset, arcSize);
        fm.skip(4);

        // 4 - File Length
        int length = fm.readInt();
        FieldValidator.checkLength(length, arcSize);

        // 2 - Unknown (90)
        fm.skip(2);

        //path,name,offset,length,decompLength,exporter
        resources[i] = new Resource(path, filename, offset, length);

        TaskProgressManager.setValue(i);
      }

      fm.seek(dir2Offset + 32);

      // Loop through directory
      for (int i = num1Files; i < numFiles; i++) {
        // 18 - Filename (null terminated, filled with nulls)
        String filename = fm.readNullString(18);
        FieldValidator.checkFilename(filename);

        int dotPos = filename.lastIndexOf('.');
        if (dotPos > 0) {
          filename = filename.substring(0, dotPos) + "_LOW" + filename.substring(dotPos);
        }

        // 8 - File Offset (relative to the end of Directory 1)
        int offset = (int) (fm.readInt() + endOfDir1);
        FieldValidator.checkOffset(offset, arcSize);
        fm.skip(4);

        // 4 - File Length
        int length = fm.readInt();
        FieldValidator.checkLength(length, arcSize);

        // 2 - Unknown (90)
        fm.skip(2);

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
