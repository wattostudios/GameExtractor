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
public class Plugin_DAT_67 extends ArchivePlugin {

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public Plugin_DAT_67() {

    super("DAT_67", "DAT_67");

    //         read write replace rename
    setProperties(true, false, false, false);

    setGames("The Oregon Trail: 5th Edition");
    setExtensions("dat"); // MUST BE LOWER CASE
    setPlatforms("PC");

    // MUST BE LOWER CASE !!!
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

      long arcSize = fm.getLength();

      // Directory Offset
      if (FieldValidator.checkOffset(ShortConverter.changeFormat(fm.readShort()), arcSize)) {
        rating += 5;
      }

      // Directory Offset
      if (FieldValidator.checkOffset(IntConverter.changeFormat(fm.readInt()), arcSize)) {
        rating += 5;
      }

      // Number Of Files
      if (FieldValidator.checkNumFiles(ShortConverter.changeFormat(fm.readShort()))) {
        rating += 5;
      }

      // Unknown (1)
      if (ShortConverter.changeFormat(fm.readShort()) == 1) {
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

      // 2 - Extension Directory Offset
      short dirOffset = ShortConverter.changeFormat(fm.readShort());
      FieldValidator.checkOffset(dirOffset, arcSize);

      // 4 - File Data Offset [+60]
      fm.skip(4);

      // 2 - Number of Extensions
      int numTypes = ShortConverter.changeFormat(fm.readShort());
      FieldValidator.checkNumFiles(numTypes);

      // 2 - Unknown (1)
      // 320 - Padding to the Extension Directory Offset
      fm.seek(dirOffset);

      int numFiles = Archive.getMaxFiles();

      Resource[] resources = new Resource[numFiles];
      TaskProgressManager.setMaximum(arcSize);

      // Loop through directory
      String[] types = new String[numTypes];
      int[] typeCount = new int[numTypes];
      int[] typeOffset = new int[numTypes];

      for (int i = 0; i < numTypes; i++) {
        // 8 - File Extension (null terminated)
        String extension = fm.readNullString(8);
        types[i] = extension;

        // 2 - Number of Files of this type
        short numOfType = ShortConverter.changeFormat(fm.readShort());
        FieldValidator.checkNumFiles(numOfType);
        typeCount[i] = numOfType;

        // 4 - Offset to the Details Directory for this Extension
        int offset = IntConverter.changeFormat(fm.readInt());
        FieldValidator.checkOffset(offset, arcSize);
        typeOffset[i] = offset;
      }

      // Loop through directory
      int realNumFiles = 0;
      for (int i = 0; i < numTypes; i++) {
        fm.seek(typeOffset[i]);

        String extension = types[i];
        int count = typeCount[i];

        for (int c = 0; c < count; c++) {

          // 2 - Unknown
          // 2 - Unknown
          fm.skip(4);

          // 4 - File Length
          int length = IntConverter.changeFormat(fm.readInt());
          FieldValidator.checkLength(length, arcSize);

          // 4 - File Offset
          int offset = IntConverter.changeFormat(fm.readInt());
          FieldValidator.checkOffset(offset, arcSize);

          // X - Filename (null)
          String filename = Resource.generateFilename(c) + "." + extension;

          //path,name,offset,length,decompLength,exporter
          resources[realNumFiles] = new Resource(path, filename, offset, length);
          realNumFiles++;
          TaskProgressManager.setValue(offset);
        }
      }

      resources = resizeResources(resources, realNumFiles);
      calculateFileSizes(resources, arcSize);

      fm.close();

      return resources;

    }
    catch (Throwable t) {
      logError(t);
      return null;
    }
  }

}
