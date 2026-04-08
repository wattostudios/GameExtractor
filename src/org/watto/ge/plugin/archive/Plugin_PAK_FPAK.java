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
import org.watto.ge.plugin.ExporterPlugin;
import org.watto.ge.plugin.exporter.Exporter_ByteMapping;
import org.watto.io.FileManipulator;
import org.watto.io.buffer.ByteBuffer;
import org.watto.task.TaskProgressManager;

/**
**********************************************************************************************

**********************************************************************************************
**/
public class Plugin_PAK_FPAK extends ArchivePlugin {

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public Plugin_PAK_FPAK() {

    super("PAK_FPAK", "PAK_FPAK");

    //         read write replace rename
    setProperties(true, false, false, false);

    setGames("Ashley Jones and the Heart of Egypt");
    setExtensions("pak"); // MUST BE LOWER CASE
    setPlatforms("PC");

    // MUST BE LOWER CASE !!!
    //setFileTypes(new FileType("txt", "Text Document", FileType.TYPE_DOCUMENT),
    //             new FileType("bmp", "Bitmap Image", FileType.TYPE_IMAGE)
    //             );

    setTextPreviewExtensions("bat"); // LOWER CASE

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
      if (fm.readString(4).equals("FPAK")) {
        rating += 50;
      }

      fm.skip(8);

      // Number Of Files
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

      int[] byteMap = new int[] { 123, 71, 133, 240, 145, 139, 44, 148, 167, 85, 58, 246, 23, 177, 194, 6, 107, 100, 40, 138, 134, 9, 206, 162, 159, 88, 222, 70, 174, 36, 99, 116, 105, 157, 251, 253, 110, 80, 224, 18, 87, 28, 8, 188, 27, 208, 217, 129, 1, 228, 199, 7, 136, 22, 118, 73, 35, 5, 173, 81, 56, 120, 69, 137, 93, 207, 46, 32, 38, 182, 59, 195, 235, 239, 34, 160, 25, 198, 106, 187, 161, 65, 245, 203, 115, 237, 234, 155, 144, 169, 45, 170, 21, 39, 218, 55, 249, 64, 164, 232, 103, 191, 30, 184, 172, 96, 83, 210, 84, 29, 104, 247, 79, 0, 196, 94, 51, 41, 20, 12, 63, 98, 252, 147, 193, 176, 180, 156, 31, 114, 171, 17, 24, 135, 181, 233, 241, 89, 248, 54, 92, 163, 227, 121, 140, 47, 10, 186, 68, 216, 200, 166, 42, 3, 202, 26, 16, 53, 243, 119, 215, 49, 185, 151, 67, 242, 61, 223, 33, 82, 101, 178, 52, 111, 127, 211, 43, 192, 204, 109, 117, 221, 13, 19, 219, 229, 2, 238, 179, 244, 201, 254, 102, 91, 236, 125, 131, 95, 205, 142, 230, 132, 212, 190, 50, 209, 165, 86, 197, 112, 158,
          11, 154, 128, 108, 90, 231, 122, 124, 183, 126, 250, 220, 15, 225, 75, 76, 143, 74, 4, 78, 168, 255, 60, 214, 14, 149, 57, 130, 66, 62, 97, 150, 72, 141, 175, 48, 226, 113, 77, 213, 37, 146, 189, 153, 152 };
      ExporterPlugin exporter = new Exporter_ByteMapping(byteMap);

      // RESETTING GLOBAL VARIABLES

      FileManipulator fm = new FileManipulator(path, false);

      long arcSize = fm.getLength();

      // 4 - Header (FPAK)
      // 4 - Unknown (65536)
      // 4 - Unknown (3)
      fm.skip(12);

      // 4 - Number of Files
      int numFiles = fm.readInt();
      FieldValidator.checkNumFiles(numFiles);

      // X - Encrypted Directory
      int dirLength = numFiles * 73;
      byte[] dirBytes = fm.readBytes(dirLength);

      int VAR_A = (numFiles * 73) - 39;
      int VAR_C = 173;
      int VAR_D = 111;

      for (int i = 0; i < dirLength; i++) {
        VAR_A += VAR_C;
        VAR_C += VAR_A;
        VAR_D += VAR_C;
        VAR_D ^= VAR_A;

        VAR_A &= 0xff;
        VAR_C &= 0xff;
        VAR_D &= 0xff;

        dirBytes[i] ^= VAR_D;
      }

      fm.close();
      fm = new FileManipulator(new ByteBuffer(dirBytes));

      Resource[] resources = new Resource[numFiles];
      TaskProgressManager.setMaximum(numFiles);

      // Loop through directory
      for (int i = 0; i < numFiles; i++) {

        // 65 - Filename (null terminated, filled with nulls)
        String filename = fm.readNullString(65);
        FieldValidator.checkFilename(filename);

        // 4 - File Offset
        int offset = fm.readInt();
        FieldValidator.checkOffset(offset, arcSize);

        // 4 - File Length
        int length = fm.readInt();
        FieldValidator.checkLength(length, arcSize);

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
