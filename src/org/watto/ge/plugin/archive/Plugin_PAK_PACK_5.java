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
import org.watto.ge.plugin.exporter.Exporter_XOR_RepeatingKey;
import org.watto.io.FileManipulator;
import org.watto.io.buffer.XORRepeatingKeyBufferWrapper;
import org.watto.task.TaskProgressManager;

/**
**********************************************************************************************

**********************************************************************************************
**/
public class Plugin_PAK_PACK_5 extends ArchivePlugin {

  /**
  **********************************************************************************************

  **********************************************************************************************
  **/
  public Plugin_PAK_PACK_5() {

    super("PAK_PACK_5", "PAK_PACK_5");

    //         read write replace rename
    setProperties(true, false, false, false);

    setGames("Eador: Masters of the Broken World");
    setExtensions("pak"); // MUST BE LOWER CASE
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

      // 4 - Header (PACK)
      if (fm.readString(4).equals("PACK")) {
        rating += 50;
      }

      // 4 - Version (1)
      if (fm.readInt() == 1) {
        rating += 5;
      }

      long arcSize = fm.getLength();

      // 8 - Directory Offset
      if (FieldValidator.checkOffset(fm.readLong(), arcSize)) {
        rating += 5;
      }

      // 4 - Number of Files?
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

      //ExporterPlugin exporter = Exporter_ZLib.getInstance();

      // RESETTING GLOBAL VARIABLES

      FileManipulator fm = new FileManipulator(path, false);

      long arcSize = fm.getLength();

      // 4 - Header (PACK)
      // 4 - Version (1)
      fm.skip(8);

      // 8 - Directory Offset
      long dirOffset = fm.readLong();
      FieldValidator.checkOffset(dirOffset, arcSize);

      // 4 - Number of Files?
      int numFiles = fm.readInt();
      FieldValidator.checkNumFiles(numFiles);

      // 4 - null

      // Set the XOR decryption
      int[] xorKey = new int[] { 152, 131, 132, 142, 130, 159, 131, 146, 158, 131, 130, 154, 143, 132, 159, 137 };
      fm.setBuffer(new XORRepeatingKeyBufferWrapper(fm.getBuffer(), xorKey));

      // go to the directory
      fm.seek(dirOffset);

      // configure the exporter for the file data
      ExporterPlugin exporter = new Exporter_XOR_RepeatingKey(xorKey);

      Resource[] resources = new Resource[numFiles];
      TaskProgressManager.setMaximum(numFiles);

      // Loop through directory
      for (int i = 0; i < numFiles; i++) {

        // 8 - File Offset
        int offset = fm.readInt();
        FieldValidator.checkOffset(offset, arcSize);
        fm.skip(4);

        // 8 - File Length
        int length = fm.readInt();
        FieldValidator.checkLength(length, arcSize);
        fm.skip(4);

        // 4 - Filename Length
        int filenameLength = fm.readInt();
        FieldValidator.checkFilenameLength(filenameLength);

        // 4 - Unknown (129)
        fm.skip(4);

        // X - Filename
        String filename = fm.readString(filenameLength);
        FieldValidator.checkFilename(filename);

        //path,name,offset,length,decompLength,exporter
        resources[i] = new Resource(path, filename, offset, length, length, exporter);

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
