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
import org.watto.io.converter.ByteConverter;
import org.watto.io.converter.IntConverter;
import org.watto.task.TaskProgressManager;

/**
**********************************************************************************************

**********************************************************************************************
**/
public class Plugin_PVD extends ArchivePlugin {

  /**
  **********************************************************************************************

  **********************************************************************************************
  **/
  public Plugin_PVD() {

    super("PVD", "PVD");

    //         read write replace rename
    setProperties(true, false, false, false);

    setGames("Aura: Fate of the Ages");
    setExtensions("pvd", "psp"); // MUST BE LOWER CASE
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

      // 4 - Header and XOR Key (bytes 239,110,109,85)
      if (ByteConverter.unsign(fm.readByte()) == 239 && ByteConverter.unsign(fm.readByte()) == 110 && ByteConverter.unsign(fm.readByte()) == 109 && ByteConverter.unsign(fm.readByte()) == 85) {
        rating += 50;
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

      // 4 - Number of Files (XOR with the XOR Key)
      byte[] numFilesBytes = fm.readBytes(4);
      numFilesBytes[0] ^= (byte) 236;
      numFilesBytes[1] ^= (byte) 110;
      numFilesBytes[2] ^= (byte) 109;
      numFilesBytes[3] ^= (byte) 85;
      int numFiles = IntConverter.convertLittle(numFilesBytes);

      // Filenames directory
      String[] filenames = new String[numFiles];
      for (int i = 0; i < numFiles; i++) {
        // 128 - Filename (XOR with the XOR Key) (null-terminated after XORing)
        byte[] filenameBytes = fm.readBytes(128);
        int filenameLength = 0;
        for (int b = 0; b < 128; b += 4) {
          filenameBytes[b + 0] ^= (byte) 236;
          if (filenameBytes[b + 0] == 0) {
            filenameLength = b + 0;
            break;
          }
          filenameBytes[b + 1] ^= (byte) 110;
          if (filenameBytes[b + 1] == 0) {
            filenameLength = b + 1;
            break;
          }
          filenameBytes[b + 2] ^= (byte) 109;
          if (filenameBytes[b + 2] == 0) {
            filenameLength = b + 2;
            break;
          }
          filenameBytes[b + 3] ^= (byte) 85;
          if (filenameBytes[b + 3] == 0) {
            filenameLength = b + 3;
            break;
          }
        }

        byte[] shortFilenameBytes = new byte[filenameLength];
        System.arraycopy(filenameBytes, 0, shortFilenameBytes, 0, filenameLength);
        filenames[i] = new String(shortFilenameBytes);
      }

      Resource[] resources = new Resource[numFiles];
      TaskProgressManager.setMaximum(numFiles);

      // Loop through directory
      for (int i = 0; i < numFiles; i++) {

        // 4 - File Length (XOR with the XOR Key)
        byte[] lengthBytes = fm.readBytes(4);
        lengthBytes[0] ^= (byte) 236;
        lengthBytes[1] ^= (byte) 110;
        lengthBytes[2] ^= (byte) 109;
        lengthBytes[3] ^= (byte) 85;
        int length = IntConverter.convertLittle(lengthBytes);
        FieldValidator.checkLength(length, arcSize);

        // X - File Data
        long offset = fm.getOffset();

        fm.skip(length);

        String filename = filenames[i];

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

}
