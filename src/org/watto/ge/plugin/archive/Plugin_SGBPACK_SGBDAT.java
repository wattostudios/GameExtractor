/*
 * Application:  Game Extractor
 * Author:       wattostudios
 * Website:      http://www.watto.org
 * Copyright:    Copyright (c) 2002-2022 wattostudios
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
import org.watto.ge.plugin.exporter.Exporter_Custom_SGBPACK_SGBDAT_Default;
import org.watto.ge.plugin.exporter.Exporter_Custom_SGBPACK_SGBDAT_Deflate;
import org.watto.io.FileManipulator;
import org.watto.io.converter.ByteConverter;
import org.watto.io.converter.IntConverter;
import org.watto.io.converter.ShortConverter;
import org.watto.io.converter.StringConverter;
import org.watto.task.TaskProgressManager;

/**
**********************************************************************************************

**********************************************************************************************
**/
public class Plugin_SGBPACK_SGBDAT extends ArchivePlugin {

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public Plugin_SGBPACK_SGBDAT() {

    super("SGBPACK_SGBDAT", "SGBPACK_SGBDAT");

    //         read write replace rename
    setProperties(true, false, false, false);

    setGames("Corrupt Life");
    setExtensions("sgbpack"); // MUST BE LOWER CASE
    setPlatforms("PC");

    // MUST BE LOWER CASE !!!
    //setFileTypes(new FileType("txt", "Text Document", FileType.TYPE_DOCUMENT),
    //             new FileType("bmp", "Bitmap Image", FileType.TYPE_IMAGE)
    //             );

    setTextPreviewExtensions("cg", "def", "fpglsl", "ptcl", "vpglsl"); // LOWER CASE

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
      if (fm.readString(6).equals("SGBDAT")) {
        rating += 50;
      }

      // version
      if (ShortConverter.changeFormat(fm.readShort()) == 1) {
        rating += 5;
      }

      return rating;

    }
    catch (Throwable t) {
      return 0;
    }
  }

  int[] decryptionKey = new int[0];

  int decryptionPos = 0;

  /**
   **********************************************************************************************
  
   **********************************************************************************************
   **/
  private byte[] readBytes(FileManipulator fm, int byteCount) {
    byte[] bytes = fm.readBytes(byteCount);

    for (int b = 0; b < byteCount; b++) {
      int byteValue = ByteConverter.unsign(bytes[b]) + decryptionKey[decryptionPos];
      if (byteValue < 0) {
        byteValue = 256 + byteValue;
      }
      bytes[b] = (byte) byteValue;
      decryptionPos++;
      if (decryptionPos >= 16) {
        decryptionPos = 0;
      }
    }

    return bytes;
  }

  /**
   **********************************************************************************************
  
   **********************************************************************************************
   **/
  private void skip(FileManipulator fm, int skipSize) {
    fm.skip(skipSize);

    decryptionPos += skipSize;
    if (decryptionPos >= 16) {
      decryptionPos %= 16;
    }
  }

  /**
   **********************************************************************************************
  
   **********************************************************************************************
   **/
  private short readShort(FileManipulator fm) {
    byte[] bytes = readBytes(fm, 2);
    return ShortConverter.convertLittle(bytes);
  }

  /**
   **********************************************************************************************
  
   **********************************************************************************************
   **/
  private int readInt(FileManipulator fm) {
    byte[] bytes = readBytes(fm, 4);
    return IntConverter.convertLittle(bytes);
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

      int[] exporterKey = new int[] { -0, -14, -8, -30, -24, -55, -18, -0, -72, -135, -70, -11, -156, -104, -168, -75 };

      // RESETTING GLOBAL VARIABLES

      FileManipulator fm = new FileManipulator(path, false);

      decryptionKey = new int[] { -72, -9, -20, -154, -48, -169, -84, -225, -0, -8, -14, -9, -20, -60, -66, -70 };
      decryptionPos = 8;

      long arcSize = fm.getLength();

      int endOffset = (int) arcSize - 22;
      fm.seek(endOffset);

      decryptionPos += endOffset;
      decryptionPos %= 16;

      // 2 - PK Header
      // 2 - PK Version
      // 2 - Disk Number
      // 2 - Disk Start
      // 2 - Number of Directory Entries
      skip(fm, 10);

      // 2 - Number of Directory Entries
      short numFiles = readShort(fm);
      FieldValidator.checkNumFiles(numFiles);

      int realNumFiles = 0;

      // 4 - Directory Length
      skip(fm, 4);

      // 4 - Directory Offset
      int dirOffset = readInt(fm);
      FieldValidator.checkOffset(dirOffset, arcSize);

      // 2 - Comment Length (0)
      decryptionPos = 8;

      fm.seek(dirOffset);
      decryptionPos += dirOffset;
      decryptionPos %= 16;

      Resource[] resources = new Resource[numFiles];
      TaskProgressManager.setMaximum(numFiles);

      // Loop through directory
      for (int i = 0; i < numFiles; i++) {

        // 2 - PK Header
        // 2 - PK Version
        // 2 - Version Made
        // 2 - Version Needed
        // 2 - Flags
        skip(fm, 10);

        // 2 - Compression Method
        short compressionMethod = readShort(fm);

        // 2 - Time
        // 2 - Date
        // 4 - CRC
        skip(fm, 8);

        // 4 - Compressed Length
        int length = readInt(fm);
        FieldValidator.checkLength(length, arcSize);

        // 4 - Decompressed Length
        int decompLength = readInt(fm);
        FieldValidator.checkLength(decompLength);

        // 2 - Filename Length
        short filenameLength = readShort(fm);
        FieldValidator.checkFilenameLength(filenameLength);

        // 2 - Extra Length
        short extraLength = readShort(fm);

        // 2 - Comment Length
        short commentLength = readShort(fm);

        // 2 - Disk Number
        // 2 - Attributes (Internal)
        // 4 - Attributes (External)
        skip(fm, 8);

        // 4 - File Offset
        int offset = readInt(fm);
        FieldValidator.checkOffset(offset, arcSize);

        // X - Filename
        String filename = StringConverter.convertLittle(readBytes(fm, filenameLength));

        // X - Extra Data
        skip(fm, extraLength);

        // X - Comment
        skip(fm, commentLength);

        if (length != 0 && decompLength != 0) {

          offset += 30 + filenameLength + extraLength;

          //path,name,offset,length,decompLength,exporter
          //resources[realNumFiles] = new Resource(path, filename, offset, length, decompLength, exporter);
          int thisDecryptionPos = offset + 8;
          thisDecryptionPos %= 16;

          if (compressionMethod == 0) {
            // Not tested - do we have any files like this?
            //resources[realNumFiles] = new Resource(path, filename, offset, length, decompLength, new Exporter_ROT_RepeatingKey(decryptionKey));
            resources[realNumFiles] = new Resource(path, filename, offset, length, decompLength, new Exporter_Custom_SGBPACK_SGBDAT_Default(decryptionKey, thisDecryptionPos, exporterKey));
          }
          else {

            //resources[realNumFiles] = new Resource(path, filename, offset, length, decompLength, new Exporter_ROT_RepeatingKey(decryptionKey, thisDecryptionPos));
            resources[realNumFiles] = new Resource(path, filename, offset, length, decompLength, new Exporter_Custom_SGBPACK_SGBDAT_Deflate(decryptionKey, thisDecryptionPos, exporterKey));
          }

          realNumFiles++;
        }

        TaskProgressManager.setValue(i);
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
