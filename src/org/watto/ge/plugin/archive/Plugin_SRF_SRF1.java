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
import org.watto.datatype.Archive;
import org.watto.datatype.ReplacableResource;
import org.watto.datatype.Resource;
import org.watto.ge.helper.FieldValidator;
import org.watto.ge.plugin.ArchivePlugin;
import org.watto.io.FileManipulator;
import org.watto.io.StringHelper;
import org.watto.io.converter.IntConverter;
import org.watto.task.TaskProgressManager;

/**
**********************************************************************************************

**********************************************************************************************
**/
public class Plugin_SRF_SRF1 extends ArchivePlugin {

  /**
  **********************************************************************************************

  **********************************************************************************************
  **/
  public Plugin_SRF_SRF1() {

    super("SRF_SRF1", "SRF_SRF1");

    //         read write replace rename
    setProperties(true, false, false, false);
    setCanImplicitReplace(false);

    setGames("Austin Powers: Operation Trivia",
        "HeadRush",
        "You Don't Know Jack!",
        "You Don't Know Jack! Die Abwarts",
        "You Don't Know Jack! Germany",
        "You Don't Know Jack! Japan",
        "You Don't Know Jack! OFFLINE",
        "You Don't Know Jack! The Ride",
        "You Don't Know Jack! UK",
        "You Don't Know Jack! Volume 1 XL",
        "You Don't Know Jack! Volume 2",
        "You Don't Know Jack! Volume 3");
    setExtensions("srf"); // MUST BE LOWER CASE
    setPlatforms("PC");

    //setFileTypes("","",
    //             "",""
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

      // Header
      if (fm.readString(4).equals("srf1")) {
        rating += 50;
      }

      long arcSize = fm.getLength();

      // Archive Size
      if (IntConverter.changeFormat(fm.readInt()) == arcSize) {
        rating += 5;
      }

      // Directory Length
      if (FieldValidator.checkLength(IntConverter.changeFormat(fm.readInt()), arcSize)) {
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

      // 4 - Header (srf1)
      // 4 - Archive Size
      fm.skip(8);

      // 4 - Directory Length
      int dirLength = IntConverter.changeFormat(fm.readInt());
      FieldValidator.checkLength(dirLength, arcSize);

      int numFiles = Archive.getMaxFiles();

      Resource[] resources = new Resource[numFiles];
      TaskProgressManager.setMaximum(dirLength);

      // Loop through directory
      int realNumFiles = 0;
      while (fm.getOffset() < dirLength) {

        // 4 - File Type/Extension (32 terminated)
        String type = StringHelper.readTerminatedString(fm.getBuffer(), (byte) 32, 4);

        // 4 - Number Of Pieces
        int numPieces = IntConverter.changeFormat(fm.readInt());
        FieldValidator.checkNumFiles(numPieces);

        // for each piece
        for (int p = 0; p < numPieces; p++) {
          // 4 - File ID
          String filename = IntConverter.changeFormat(fm.readInt()) + "." + type;

          // 4 - File Offset
          long offsetPointerLocation = fm.getOffset();
          int offsetPointerLength = 4;

          int offset = IntConverter.changeFormat(fm.readInt());
          FieldValidator.checkOffset(offset, arcSize);

          // 4 - File Length
          long lengthPointerLocation = fm.getOffset();
          int lengthPointerLength = 4;

          int length = IntConverter.changeFormat(fm.readInt());
          FieldValidator.checkLength(length, arcSize);

          //path,name,offset,length,decompLength,exporter
          resources[realNumFiles] = new ReplacableResource(path, filename, offset, offsetPointerLocation, offsetPointerLength, length, lengthPointerLocation, lengthPointerLength);

          TaskProgressManager.setValue(fm.getOffset());
          realNumFiles++;
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

}
