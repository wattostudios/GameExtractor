/*
 * Application:  Game Extractor
 * Author:       wattostudios
 * Website:      http://www.watto.org
 * Copyright:    Copyright (c) 2002-2026 wattostudios
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
import org.watto.io.buffer.ByteBuffer;
import org.watto.io.converter.ByteConverter;
import org.watto.task.TaskProgressManager;

/**
**********************************************************************************************

**********************************************************************************************
**/
public class Plugin_RAS_RAS extends ArchivePlugin {

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public Plugin_RAS_RAS() {

    super("RAS_RAS", "RAS_RAS");

    //         read write replace rename
    setProperties(true, false, false, false);

    setGames("Max Payne",
        "Max Payne 2");
    setExtensions("ras"); // MUST BE LOWER CASE
    setPlatforms("PC");

    // MUST BE LOWER CASE !!!
    //setFileTypes(new FileType("txt", "Text Document", FileType.TYPE_DOCUMENT),
    //             new FileType("bmp", "Bitmap Image", FileType.TYPE_IMAGE)
    //             );

    setTextPreviewExtensions("h"); // LOWER CASE

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

      // Header ("RAS" + null)
      if (fm.readInt() == 5456210) {
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
  @SuppressWarnings("unused")
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

      // 4 - Header ("RAS" + null)
      fm.skip(4);

      // 4 - Encryption Key
      int key = fm.readInt();

      // 36 - Encrypted Metadata
      byte[] metadata = fm.readBytes(36);
      metadata = decrypt(metadata, key);

      FileManipulator metadataFM = new FileManipulator(new ByteBuffer(metadata));

      // 4 - Number of Files
      int numFiles = metadataFM.readInt();
      FieldValidator.checkNumFiles(numFiles);

      // 4 - Number of Directories
      int numDirectories = metadataFM.readInt();
      FieldValidator.checkNumFiles(numDirectories);

      // 4 - File List Length
      int fileListLength = metadataFM.readInt();
      FieldValidator.checkLength(fileListLength, arcSize);

      // 4 - Directory List Length
      int directoryListLength = metadataFM.readInt();
      FieldValidator.checkLength(directoryListLength, arcSize);

      // 4 - Version (Float)
      float version = metadataFM.readFloat();

      // 4 - Unknown
      // 4 - Unknown
      // 4 - Unknown
      // 4 - Compatibility Flag
      metadataFM.close();

      // X - Encrypted File List
      byte[] fileListBytes = fm.readBytes(fileListLength);
      fileListBytes = decrypt(fileListBytes, key);

      // X - Encrypted Directory List
      byte[] directoryListBytes = fm.readBytes(directoryListLength);
      directoryListBytes = decrypt(directoryListBytes, key);

      // read the directories
      fm.close();
      fm = new FileManipulator(new ByteBuffer(directoryListBytes));

      String[] dirNames = new String[numDirectories];
      for (int i = 0; i < numDirectories; i++) {
        // X - Directory Name
        // 1 - null Directory Name Terminator
        String dirName = fm.readNullString();
        FieldValidator.checkFilename(dirName);

        if (dirName.startsWith("\\")) {
          dirName = dirName.substring(1);
        }

        // 16 - Timestamp
        fm.skip(16);

        dirNames[i] = dirName;
      }

      // read the files
      fm.close();
      fm = new FileManipulator(new ByteBuffer(fileListBytes));

      Resource[] resources = new Resource[numFiles];
      TaskProgressManager.setMaximum(numFiles);

      long offset = 8 + 36 + fileListLength + directoryListLength;
      for (int i = 0; i < numFiles; i++) {
        // X - Filename
        // 1 - null Filename Terminator
        String filename = fm.readNullString();
        FieldValidator.checkFilename(filename);

        // 4 - Decompressed File Length
        int decompLength = fm.readInt();
        FieldValidator.checkLength(decompLength);

        // 4 - Compressed File Length
        int length = fm.readInt();
        FieldValidator.checkLength(length, arcSize);

        // 4 - Unknown
        fm.skip(4);

        // 4 - Directory Index
        int dirIndex = fm.readInt();
        FieldValidator.checkRange(dirIndex, 0, numDirectories);

        filename = dirNames[dirIndex] + filename;

        // 4 - Unknown
        // 4 - Compression Type (1=lzss0, 3=uncompressed)
        // 16 - Timestamp
        fm.skip(24);

        if (length != decompLength) {
          // compressed

          //path,name,offset,length,decompLength,exporter
          resources[i] = new Resource(path, filename, offset, length, decompLength);
        }
        else {
          // not compressed

          //path,name,offset,length,decompLength,exporter
          resources[i] = new Resource(path, filename, offset, length);
        }

        TaskProgressManager.setValue(i);

        offset += length;
      }

      fm.close();

      return resources;

    }
    catch (

    Throwable t) {
      logError(t);
      return null;
    }
  }

  /**
   **********************************************************************************************
  
   **********************************************************************************************
   **/
  public byte[] decrypt(byte[] bytes, int key) {
    int length = bytes.length;
    int i;

    if (key == 0) {
      key = 1;
    }
    for (i = 0; i < length; i++) {
      int a = ByteConverter.unsign(bytes[i]);
      int b = (i % 5) & 7;
      bytes[i] = (byte) rotateLeftByte(a, b);
      key = key * 171 + (key / 177) * -30269;
      bytes[i] = (byte) (((((((byte) i) + 3) * 6) ^ bytes[i]) + ((byte) key)));
    }

    return bytes;
  }

  /**
   **********************************************************************************************
  
   **********************************************************************************************
   **/
  public int rotateLeftByte(int a, int b) {
    return ((a << b) | (a >> (8 - b)));
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
