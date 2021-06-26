/*
 * Application:  Game Extractor
 * Author:       wattostudios
 * Website:      http://www.watto.org
 * Copyright:    Copyright (c) 2002-2020 wattostudios
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
import org.watto.ge.plugin.ExporterPlugin;
import org.watto.ge.plugin.exporter.Exporter_ZLib_CompressedSizeOnly;
import org.watto.io.FileManipulator;
import org.watto.task.TaskProgressManager;

/**
**********************************************************************************************

**********************************************************************************************
**/
public class Plugin_BUNDLE_BND2 extends ArchivePlugin {

  /**
  **********************************************************************************************

  **********************************************************************************************
  **/
  public Plugin_BUNDLE_BND2() {

    super("BUNDLE_BND2", "BUNDLE_BND2");

    //         read write replace rename
    setProperties(true, false, false, false);

    setGames("Burnout Paradise");
    setExtensions("bundle"); // MUST BE LOWER CASE
    setPlatforms("PC");

    //setFileTypes(new FileType("txt", "Text Document", FileType.TYPE_DOCUMENT),
    //             new FileType("bmp", "Bitmap Image", FileType.TYPE_IMAGE)
    //             );

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

      // 4 - Header (bnd2)
      if (fm.readString(4).equals("bnd2")) {
        rating += 50;
      }

      // 4 - Number Of Entries
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

      ExporterPlugin exporter = Exporter_ZLib_CompressedSizeOnly.getInstance();

      // RESETTING GLOBAL VARIABLES

      FileManipulator fm = new FileManipulator(path, false);

      long arcSize = fm.getLength();

      // 4 - Header (bnd2)
      fm.skip(4);

      // 4 - Number of Entries
      int numEntries = fm.readInt();
      FieldValidator.checkNumFiles(numEntries);

      fm.skip(numEntries * 8);

      // 4 - File Data Offset
      int offset = fm.readInt();
      FieldValidator.checkOffset(offset, arcSize);

      // 4 - Archive Length
      // 4 - Archive Length
      // 4 - Unknown (7)
      // 8 - null
      // 4 - Unknown
      // 12 - null
      // 4 - Unknown (524288)
      // 8 - null
      fm.skip(48);

      // 4 - Compressed Data Length
      int length = fm.readInt();
      FieldValidator.checkLength(length, arcSize);

      Resource[] resources = new Resource[1];
      TaskProgressManager.setMaximum(1);

      String filename = "CompressedFile1.zlb";

      //path,name,offset,length,decompLength,exporter
      resources[0] = new Resource(path, filename, offset, length, length, exporter);

      fm.close();

      return resources;

    }
    catch (Throwable t) {
      logError(t);
      return null;
    }
  }

}
