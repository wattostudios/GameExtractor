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
import org.watto.io.FileManipulator;
import org.watto.io.converter.IntConverter;
import org.watto.io.converter.ShortConverter;
import org.watto.task.TaskProgressManager;

/**
**********************************************************************************************

**********************************************************************************************
**/
public class Plugin_ARC_SARC extends ArchivePlugin {

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public Plugin_ARC_SARC() {

    super("ARC_SARC", "ARC_SARC");

    //         read write replace rename
    setProperties(true, false, false, false);

    setGames("Mii Maker");
    setExtensions("arc"); // MUST BE LOWER CASE
    setPlatforms("Wii U");

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

      // 4 - Header (SARC)
      if (fm.readString(4).equals("SARC")) {
        rating += 50;
      }

      fm.skip(4);

      long arcSize = fm.getLength();

      // 4 - Archive Size
      if (FieldValidator.checkEquals(IntConverter.changeFormat(fm.readInt()), arcSize)) {
        rating += 5;
      }

      // 4 - File Data Offset (8192)
      if (FieldValidator.checkOffset(IntConverter.changeFormat(fm.readInt()), arcSize)) {
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

      // 4 - Header (SARC)
      // 4 - Unknown
      // 4 - Archive Length
      fm.skip(12);

      // 4 - File Data Offset (8192)
      int fileDataOffset = IntConverter.changeFormat(fm.readInt());
      FieldValidator.checkOffset(fileDataOffset, arcSize);

      // 4 - Unknown
      // 4 - Directory Header (SFAT)
      // 2 - Unknown (12)
      fm.skip(10);

      // 2 - Number of Files
      short numFiles = ShortConverter.changeFormat(fm.readShort());
      FieldValidator.checkNumFiles(numFiles);

      // 4 - Unknown
      fm.skip(4);

      Resource[] resources = new Resource[numFiles];
      TaskProgressManager.setMaximum(numFiles);

      // Loop through directory
      for (int i = 0; i < numFiles; i++) {

        // 4 - Hash?
        // 4 - Unknown
        fm.skip(8);

        // 4 - File Offset (relative to the start of the File Data)
        int offset = IntConverter.changeFormat(fm.readInt()) + fileDataOffset;
        FieldValidator.checkOffset(offset, arcSize);

        // 4 - Offset to the End of the File Data for this File (relative to the start of the File Data) (not including padding)
        int endOffset = IntConverter.changeFormat(fm.readInt()) + fileDataOffset;
        FieldValidator.checkOffset(offset, arcSize);

        int length = endOffset - offset;
        FieldValidator.checkLength(length, arcSize);

        //path,name,offset,length,decompLength,exporter
        resources[i] = new Resource(path, "", offset, length);

        TaskProgressManager.setValue(i);
      }

      // 4 - Directory Header (SFNT)
      // 2 - Unknown (8)
      // 2 - null
      fm.skip(8);

      for (int i = 0; i < numFiles; i++) {

        // X - Filename
        // 1 - null Filename Terminator
        String filename = fm.readNullString();
        FieldValidator.checkFilename(filename);

        // 0-3 - null Padding to a multiple of 4 bytes
        int filenameLength = filename.length() + 1;
        fm.skip(calculatePadding(filenameLength, 4));

        resources[i].setName(filename);
        resources[i].setOriginalName(filename); // so it doesn't think the filename has changed

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

}
