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
import org.watto.io.converter.ByteConverter;
import org.watto.io.converter.IntConverter;
import org.watto.task.TaskProgressManager;

/**
**********************************************************************************************

**********************************************************************************************
**/
public class Plugin_SOU_SOU extends ArchivePlugin {

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public Plugin_SOU_SOU() {

    super("SOU_SOU", "SOU_SOU");

    //         read write replace rename
    setProperties(true, false, false, false);

    setGames("Indiana Jones and the Fate of Atlantis");
    setExtensions("sou"); // MUST BE LOWER CASE
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
      if (fm.readString(4).equals("SOU ")) {
        rating += 50;
      }

      // 4 - null
      if (fm.readInt() == 0) {
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

      FileManipulator fm = new FileManipulator(path, false, 48);// small quick reads

      long arcSize = fm.getLength();

      // 4 - Header ("SOU ")
      // 4 - null
      fm.skip(8);

      int numFiles = Archive.getMaxFiles();

      Resource[] resources = new Resource[numFiles];
      TaskProgressManager.setMaximum(arcSize);

      // Loop through directory
      int realNumFiles = 0;
      while (fm.getOffset() < arcSize) {
        // 4 - Header (VCTL)
        fm.skip(4);

        // 4 - Header Length (18) (including these 2 header fields)
        int headerLength = IntConverter.changeFormat(fm.readInt()) - 8;
        FieldValidator.checkLength(headerLength, arcSize);

        // 2 - Unknown
        // 2 - Unknown
        // 2 - Unknown
        // 2 - Unknown
        // 2 - Unknown
        fm.skip(headerLength);

        // X - VOC Audio File {
        long offset = fm.getOffset();

        //   19 - Header (Creative Voice File)
        //   7 - Unknown
        //   1 - Audio Format (1)
        fm.skip(27);

        //   3 - File Data Length (byte0 + byte1*0x100 + byte2*0x10000)
        int length = ByteConverter.unsign(fm.readByte()) + (ByteConverter.unsign(fm.readByte()) << 8) + (ByteConverter.unsign(fm.readByte()) << 16);
        FieldValidator.checkLength(length, arcSize);

        //   X - File Data
        //   1 - null End Of File Marker
        //   }
        fm.skip(length + 1);

        length += 30 + 1; // add the file header and null terminator to the length

        String filename = Resource.generateFilename(realNumFiles) + ".voc";

        //path,name,offset,length,decompLength,exporter
        resources[realNumFiles] = new Resource(path, filename, offset, length);
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

}
