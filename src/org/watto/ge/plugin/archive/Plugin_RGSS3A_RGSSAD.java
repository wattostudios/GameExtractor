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
import org.watto.ge.plugin.ExporterPlugin;
import org.watto.ge.plugin.exporter.Exporter_Custom_RGSSAD_RGSSAD;
import org.watto.io.FileManipulator;
import org.watto.io.converter.StringConverter;
import org.watto.task.TaskProgressManager;

/**
**********************************************************************************************

**********************************************************************************************
**/
public class Plugin_RGSS3A_RGSSAD extends ArchivePlugin {

  int decryptionKey = 0xdeadcafe;

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public Plugin_RGSS3A_RGSSAD() {

    super("RGSS3A_RGSSAD", "RGSS3A_RGSSAD");

    //         read write replace rename
    setProperties(true, false, false, false);

    setGames("Arcane preRaise",
        "Arcane RERaise",
        "Chosen 2",
        "DarkEnd",
        "Gladiator Trainer",
        "Greyfox",
        "Hunter's Grimm",
        "Labyronia RPG",
        "Labyronia RPG 2",
        "Legend of Mysteria",
        "Please Close The Doors",
        "Sins Of The Demon",
        "Story: Heaven & Hell",
        "Story Of The Survivor",
        "The Dark Stone from Mebara",
        "The Deed: Dynasty");
    setExtensions("rgss3a"); // MUST BE LOWER CASE
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

      // 7 - Header ("RGSSAD" + null)
      if (fm.readString(6).equals("RGSSAD")) {
        rating += 50;
      }

      fm.skip(1);

      // 1 - Version (3)
      if (fm.readByte() == 3) {
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
      decryptionKey = 0xdeadcafe;

      FileManipulator fm = new FileManipulator(path, false);

      long arcSize = fm.getLength();

      // 7 - Header ("RGSSAD" + null)
      // 1 - Version (3)
      fm.skip(8);

      // 4 - Decryption Key
      decryptionKey = fm.readInt();
      decryptionKey *= 9;
      decryptionKey += 3;

      int numFiles = Archive.getMaxFiles();
      int realNumFiles = 0;

      Resource[] resources = new Resource[numFiles];
      TaskProgressManager.setMaximum(arcSize);

      // Loop through directory
      while (fm.getOffset() < arcSize) {

        //
        // THE DIRECTORY IS ENCRYPTED!!!
        //

        // 4 - File Offset
        int offset = fm.readInt() ^ decryptionKey;
        FieldValidator.checkOffset(offset, arcSize);

        if (offset == 0) {
          break;
        }

        // 4 - File Length
        int length = fm.readInt() ^ decryptionKey;
        FieldValidator.checkLength(length, arcSize);

        // 4 - File Decryption Key
        int fileDecryptionKey = fm.readInt() ^ decryptionKey;

        // 4 - Filename Length
        int filenameLength = fm.readInt() ^ decryptionKey;
        FieldValidator.checkFilenameLength(filenameLength);

        // X - Filename
        byte[] filenameBytes = fm.readBytes(filenameLength);

        int filenameDecryptionKey = decryptionKey;
        for (int f = 0; f < filenameLength; f++) {
          filenameBytes[f] = filenameBytes[f] ^= filenameDecryptionKey;
          //filenameDecryptionKey >>>= 8;
          filenameDecryptionKey = Integer.rotateRight(filenameDecryptionKey, 8);
        }
        String filename = StringConverter.convertLittle(filenameBytes);

        ExporterPlugin exporter = new Exporter_Custom_RGSSAD_RGSSAD(fileDecryptionKey);

        //path,name,offset,length,decompLength,exporter
        resources[realNumFiles] = new Resource(path, filename, offset, length, length, exporter);

        TaskProgressManager.setValue(offset);
        realNumFiles++;
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
