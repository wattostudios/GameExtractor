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
import org.watto.task.TaskProgressManager;

/**
**********************************************************************************************

**********************************************************************************************
**/
public class Plugin_GSC_NU20 extends ArchivePlugin {

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public Plugin_GSC_NU20() {

    super("GSC_NU20", "GSC_NU20");

    //         read write replace rename
    setProperties(true, false, false, false);

    setGames("LEGO Batman");
    setExtensions("gsc"); // MUST BE LOWER CASE
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
      if (fm.readString(4).equals("NU20")) {
        rating += 50;
      }

      fm.skip(4);

      // 4 - Unknown (3)
      if (fm.readInt() == 3) {
        rating += 5;
      }

      fm.skip(4);

      // 4 - Header (HEAD)
      if (fm.readString(4).equals("HEAD")) {
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

      // 4 - Header (NU20)
      // 4 - Unknown
      // 4 - Unknown (3)
      // 4 - Unknown (-1)
      fm.skip(16);

      // 4 - Header (HEAD)
      // 4 - Block Length (including these header fields) (16)
      fm.skip(8);

      // 4 - Offset to PNTR [+16]
      int pointerOffset = fm.readInt() + 16;
      FieldValidator.checkOffset(pointerOffset, arcSize);

      // 4 - Offset to GSNH [+20]
      fm.skip(4);

      // read through the blocks, trying to find the filenames and image details
      int numNames = 0;
      String[] names = null;

      int numLengths = 0;
      int[] lengths = null;

      while (fm.getOffset() < pointerOffset) {
        long blockOffset = fm.getOffset();

        // 4 - Block Header
        String header = fm.readString(4);

        // 4 - Block Length
        int blockLength = fm.readInt();
        FieldValidator.checkLength(blockLength, arcSize);

        if (header.equals("NTBL")) {
          // NAME TABLE

          // 4 - Name Table Length (not including these header fields or the padding)
          int nameTableLength = fm.readInt();
          FieldValidator.checkLength(nameTableLength, blockLength);

          int maxNames = nameTableLength / 4;
          names = new String[maxNames];

          long endOffset = fm.getOffset() + nameTableLength;
          while (fm.getOffset() < endOffset) {
            // X - Filename
            // 1 - null Filename Terminator
            String name = fm.readNullString();
            names[numNames] = name;
            numNames++;
          }

          // 0-15 - null Padding to a multiple of 16 bytes
          fm.seek(blockOffset + blockLength);
        }
        else if (header.equals("TST0")) {
          // IMAGE DETAILS TABLE
          long endOffset = blockOffset + blockLength;

          int maxLengths = (blockLength - 8) / 128;
          lengths = new int[maxLengths];

          while (fm.getOffset() < endOffset) {
            // 4 - Image Width
            // 4 - Image Height
            // 16 - CRC?
            // 20 - null
            // 4 - Unknown (16)
            // 8 - null
            // 4 - Unknown (6)
            // 4 - null
            // 4 - Mipmap Count?
            fm.skip(68);

            // 4 - Image Data Length
            int length = fm.readInt();
            FieldValidator.checkLength(length, arcSize);

            if (length == 0) {
              break;
            }
            else {
              lengths[numLengths] = length;
              numLengths++;
            }

            // 8 - null
            // 4 - Unknown (3)
            // 4 - Unknown (1)
            // 40 - null
            fm.skip(56);
          }

          fm.seek(blockOffset + blockLength);
        }
        else {
          // skip over all other blocks
          fm.seek(blockOffset + blockLength);
        }
      }

      fm.seek(pointerOffset); // just in case

      // 4 - Header (PNTR)
      fm.skip(4);

      // 4 - Block Length (including these header fields)
      int blockLength = fm.readInt() - 8;
      FieldValidator.checkLength(blockLength, arcSize);

      // X - PNTR Data
      fm.skip(blockLength);

      // 4 - Unknown
      fm.skip(4);

      //
      // NOW WE'RE AT THE START OF THE FILE DATA, SO STORE THE FILES AND WORK OUT THE OFFSETS
      //
      long offset = fm.getOffset();

      int numFiles = numLengths;
      FieldValidator.checkNumFiles(numFiles);

      Resource[] resources = new Resource[numFiles];
      TaskProgressManager.setMaximum(numFiles);

      // Loop through directory
      for (int i = 0; i < numFiles; i++) {
        int length = lengths[i];
        //String filename = names[i] + ".dds";
        String filename = Resource.generateFilename(i) + ".dds";

        //path,name,offset,length,decompLength,exporter
        resources[i] = new Resource(path, filename, offset, length);

        offset += length;

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
