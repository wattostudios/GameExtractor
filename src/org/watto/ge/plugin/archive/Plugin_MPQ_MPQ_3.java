/*
 * Application:  Game Extractor
 * Author:       wattostudios
 * Website:      http://www.watto.org
 * Copyright:    Copyright (c) 2002-2021 wattostudios
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
import org.watto.task.TaskProgressManager;
import systems.crigges.jmpq3.security.MPQEncryption;

/**
**********************************************************************************************

**********************************************************************************************
**/
public class Plugin_MPQ_MPQ_3 extends ArchivePlugin {

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public Plugin_MPQ_MPQ_3() {

    super("MPQ_MPQ_3", "MPQ_MPQ_3");

    //         read write replace rename
    setProperties(true, false, false, false);

    setGames("Dr Brain Thinking Games: IQ  Adventure");
    setExtensions("iqm"); // MUST BE LOWER CASE
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

      // 4 - Header ("MPQ" + (byte)3)
      String header = fm.readString(3);
      int version = fm.readByte();
      if (header.equals("MPQ") && version == 3) {
        rating += 50;
      }

      // 4 - Header Length (32)
      if (fm.readInt() == 32) {
        rating += 5;
      }

      long arcSize = fm.getLength();

      // 4 - Archive Size
      if (fm.readInt() == arcSize) {
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

      //Exporter_Custom_MPQ exporter = Exporter_Custom_MPQ.getInstance();

      // RESETTING GLOBAL VARIABLES

      FileManipulator fm = new FileManipulator(path, false);

      long arcSize = fm.getLength();

      // 4 - Header ("MPQ" + (byte)3)
      // 4 - Header Length (32)
      // 4 - Archive Size
      // 2 - Format Version (0)
      fm.skip(14);

      // 2 - Sector Size Shift (3)
      /*
      int sectorSizeShift = fm.readShort();
      int discBlockSize = 512 * (1 << sectorSizeShift);
      exporter.setBlockSize(discBlockSize);
      */
      fm.skip(2);

      // 4 - Hash Table Offset
      fm.skip(4);

      // 4 - Block Table Offset
      int dirOffset = fm.readInt();
      FieldValidator.checkOffset(dirOffset, arcSize);

      // 4 - Hash Table Size (1024)
      fm.skip(4);

      // 4 - Block Table Entries (Number of Files)
      int numFiles = fm.readInt();
      FieldValidator.checkNumFiles(numFiles);

      // Decrypt the directory
      fm.seek(dirOffset);

      int dirLength = numFiles * 16;
      byte[] rawBytes = fm.readBytes(dirLength);
      byte[] decryptedBytes = new byte[dirLength];

      FileManipulator rawFM = new FileManipulator(new ByteBuffer(rawBytes));
      FileManipulator decryptedFM = new FileManipulator(new ByteBuffer(decryptedBytes));

      new MPQEncryption(-326913117, true).decrypt(rawFM, decryptedFM);

      fm.close();
      fm = decryptedFM;
      decryptedFM.seek(0);

      Resource[] resources = new Resource[numFiles];
      TaskProgressManager.setMaximum(numFiles);

      // Loop through directory
      for (int i = 0; i < numFiles; i++) {

        // 4 - File Offset
        int offset = fm.readInt();
        FieldValidator.checkOffset(offset, arcSize);

        // 4 - Compressed File Length
        int length = fm.readInt();
        FieldValidator.checkLength(length, arcSize);

        // 4 - Decompressed File Length
        int decompLength = fm.readInt();
        FieldValidator.checkLength(decompLength);

        // 4 - Flags
        int flags = fm.readInt();

        String filename = Resource.generateFilename(i);

        //path,name,offset,length,decompLength,exporter
        Resource resource = new Resource(path, filename, offset, length, decompLength);

        boolean compressed = false;
        if ((flags & 0x00000100) == 0x00000100) {
          // Implode Compression
          resource.addProperty("MPQ_FILE_IMPLODE", true);
          compressed = true;
        }
        if ((flags & 0x00000200) == 0x00000200) {
          // Various MPQ Compressions
          resource.addProperty("MPQ_FILE_COMPRESS", true);
          compressed = true;
        }
        if ((flags & 0x00010000) == 0x00010000) {
          // Encryption
          resource.addProperty("MPQ_FILE_ENCRYPTED", true);
          compressed = true;
        }
        if ((flags & 0x00020000) == 0x00020000) {
          // Encryption - decryption key for the file is altered according to the position of the file in the archive
          resource.addProperty("MPQ_FILE_FIX_KEY", true);
        }
        if ((flags & 0x01000000) == 0x01000000) {
          // Instead of being divided to 0x1000-bytes blocks, the file is stored as single unit
          resource.addProperty("MPQ_FILE_SINGLE_UNIT", true);
        }

        // Compression can be supported, but not decryption, so don't worry about it
        // Ref for compression: https://github.com/inwc3/JMPQ3
        if (compressed) {
          //resource.setExporter(exporter);
        }

        resources[i] = resource;

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
