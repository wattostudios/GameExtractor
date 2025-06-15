/*
 * Application:  Game Extractor
 * Author:       wattostudios
 * Website:      http://www.watto.org
 * Copyright:    Copyright (c) 2002-2025 wattostudios
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
import org.watto.ge.plugin.exporter.Exporter_ZLib;
import org.watto.io.FileManipulator;
import org.watto.task.TaskProgressManager;

/**
**********************************************************************************************

**********************************************************************************************
**/
public class Plugin_ASR_ASURAZLB extends ArchivePlugin {

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public Plugin_ASR_ASURAZLB() {

    super("ASR_ASURAZLB", "ASR_ASURAZLB");

    //         read write replace rename
    setProperties(true, false, false, false);

    setGames("Alien vs Predator (2010)",
        "Atomfall");
    setExtensions("asr"); // MUST BE LOWER CASE
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

      // 8 - Header (AsuraZlb)
      if (fm.readString(8).equals("AsuraZlb")) {
        rating += 50;
      }

      fm.skip(4);

      long arcSize = fm.getLength();

      // Compressed Length
      if (FieldValidator.checkLength(fm.readInt(), arcSize)) {
        rating += 5;
      }

      // Decompressed Length
      if (FieldValidator.checkLength(fm.readInt())) {
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

      ExporterPlugin exporter = Exporter_ZLib.getInstance();

      // RESETTING GLOBAL VARIABLES

      FileManipulator fm = new FileManipulator(path, false);

      long arcSize = fm.getLength();

      // 8 - Header (AsuraZlb)
      // 4 - null
      fm.skip(12);

      // 4 - Compressed Length
      int length = fm.readInt();
      FieldValidator.checkLength(length, arcSize);

      // 4 - Decompressed Length
      int decompLength = fm.readInt();
      FieldValidator.checkLength(decompLength);

      // X - Compressed File Data
      long offset = fm.getOffset();

      Resource[] resources = new Resource[1];
      TaskProgressManager.setMaximum(1);

      String archiveName = path.getName();
      int dotPos = archiveName.lastIndexOf('.');
      String extension = archiveName.substring(dotPos);
      archiveName = archiveName.substring(0, dotPos);

      String filename = archiveName + ".decompressed" + extension;

      //path,name,offset,length,decompLength,exporter
      resources[0] = new Resource(path, filename, offset, length, decompLength, exporter);

      fm.close();

      return resources;

    }
    catch (Throwable t) {
      logError(t);
      return null;
    }
  }

}
