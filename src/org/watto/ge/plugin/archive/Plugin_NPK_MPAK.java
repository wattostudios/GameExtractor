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
import org.watto.ge.plugin.exporter.Exporter_ZLib;
import org.watto.io.FileManipulator;
import org.watto.io.buffer.ByteBuffer;
import org.watto.io.converter.IntConverter;
import org.watto.task.TaskProgressManager;

/**
**********************************************************************************************

**********************************************************************************************
**/
public class Plugin_NPK_MPAK extends ArchivePlugin {

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public Plugin_NPK_MPAK() {

    super("NPK_MPAK", "NPK_MPAK");

    //         read write replace rename
    setProperties(true, false, false, false);

    setGames("Dark Age of Camelot");
    setExtensions("npk", "mpk"); // MUST BE LOWER CASE
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
      if (fm.readString(4).equals("MPAK")) {
        rating += 50;
      }

      if (fm.readByte() == 2) {
        rating += 5;
      }

      fm.skip(16);

      if (fm.readByte() == 120) {
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

      // RESETTING GLOBAL VARIABLES

      FileManipulator fm = new FileManipulator(path, false);

      long arcSize = fm.getLength();

      // 4 - Header (MPAK)
      // 1 - Version? (2)
      fm.skip(5);

      byte[] encryptedHeader = fm.readBytes(16);
      for (int b = 0; b < 16; b++) {
        encryptedHeader[b] ^= b;
      }

      // 4 - CRC
      // don't care about this field, ignore it

      // 4 - Compressed Block 2 Length
      int block2Length = IntConverter.convertLittle(new byte[] { encryptedHeader[4], encryptedHeader[5], encryptedHeader[6], encryptedHeader[7] });
      FieldValidator.checkLength(block2Length, arcSize);

      // 4 - Compressed Block 1 Length
      int block1Length = IntConverter.convertLittle(new byte[] { encryptedHeader[8], encryptedHeader[9], encryptedHeader[10], encryptedHeader[11] });
      FieldValidator.checkLength(block1Length, arcSize);

      // 4 - Number of Files
      int numFiles = IntConverter.convertLittle(new byte[] { encryptedHeader[12], encryptedHeader[13], encryptedHeader[14], encryptedHeader[15] });
      FieldValidator.checkNumFiles(numFiles);

      // COMPRESSED BLOCK 1
      fm.skip(block1Length);

      // COMPRESSED BLOCK 2
      int block2DecompLength = (numFiles * 284);
      FieldValidator.checkLength(block2DecompLength);

      byte[] dirBytes = new byte[block2DecompLength];
      int decompWritePos = 0;
      Exporter_ZLib exporter = Exporter_ZLib.getInstance();
      exporter.open(fm, block2Length, block2DecompLength);

      for (int b = 0; b < block2DecompLength; b++) {
        if (exporter.available()) { // make sure we read the next bit of data, if required
          dirBytes[decompWritePos++] = (byte) exporter.read();
        }
      }

      // open the decompressed data for processing
      fm.close();
      fm = new FileManipulator(new ByteBuffer(dirBytes));

      Resource[] resources = new Resource[numFiles];
      TaskProgressManager.setMaximum(numFiles);

      int dataOffset = 21 + block1Length + block2Length;

      // Loop through directory
      for (int i = 0; i < numFiles; i++) {
        // 256 - Filename of Uncompressed File (null terminated, filled with nulls)
        String filename = fm.readNullString(256);
        FieldValidator.checkFilename(filename);

        // 4 - Timestamp
        // 8 - Unknown (0/4)
        fm.skip(12);

        // 4 - Decompressed Length
        int decompLength = fm.readInt();
        FieldValidator.checkLength(decompLength);

        // 4 - File Offset (relative to the start of Compressed Block 3)
        int offset = fm.readInt() + dataOffset;
        FieldValidator.checkOffset(offset, arcSize);

        // 4 - Compressed Length
        int length = fm.readInt();
        FieldValidator.checkLength(length, arcSize);

        // 4 - CRC
        fm.skip(4);

        //path,name,offset,length,decompLength,exporter
        resources[i] = new Resource(path, filename, offset, length, decompLength, exporter);

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
