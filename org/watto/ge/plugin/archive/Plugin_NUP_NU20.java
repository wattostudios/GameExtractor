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
import java.util.Arrays;
import org.watto.datatype.Resource;
import org.watto.ge.helper.FieldValidator;
import org.watto.ge.plugin.ArchivePlugin;
import org.watto.io.FileManipulator;
import org.watto.task.TaskProgressManager;

/**
**********************************************************************************************

**********************************************************************************************
**/
public class Plugin_NUP_NU20 extends ArchivePlugin {

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public Plugin_NUP_NU20() {

    super("NUP_NU20", "NUP_NU20");

    //         read write replace rename
    setProperties(true, false, false, false);

    setGames("LEGO Star Wars 2: The Original Trilogy");
    setExtensions("nup"); // MUST BE LOWER CASE
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

      // 4 - Unknown (1)
      if (fm.readInt() == 1) {
        rating += 5;
      }

      fm.skip(4);

      // 4 - Header (NTBL)
      if (fm.readString(4).equals("NTBL")) {
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
      // 4 - Unknown (1)
      // 4 - null
      fm.skip(16);

      Resource[] resources = null;

      while (fm.getOffset() < arcSize) {
        long blockOffset = fm.getOffset();

        // 4 - Block Header
        String header = fm.readString(4);

        // 4 - Block Length
        int blockLength = fm.readInt();
        FieldValidator.checkLength(blockLength, arcSize);

        if (header.equals("TST0")) {
          // IMAGE DETAILS TABLE

          // 4 - Number of Entries
          int numFiles = fm.readInt();
          FieldValidator.checkNumFiles(numFiles);

          // 4 - Unknown
          // 4 - Unknown
          fm.skip(8);

          // 4 - Image Data Offset (relative to the start of the TST0 BLOCK) [+8]
          int dataOffset = (int) (blockOffset + 8 + fm.readInt());
          FieldValidator.checkOffset(dataOffset, arcSize);

          // 4 - Image Data Length
          int dataLength = fm.readInt();
          FieldValidator.checkLength(dataLength, arcSize);

          // 4 - null
          fm.skip(4);

          resources = new Resource[numFiles];
          TaskProgressManager.setMaximum(numFiles);

          // Loop through directory
          int[] offsets = new int[numFiles];
          for (int i = 0; i < numFiles; i++) {
            // 4 - Image Width
            // 4 - Image Height
            // 4 - Mipmap Count? (1)
            // 4 - Unknown
            fm.skip(16);

            // 4 - Image Data Offset (relative to the start of the Image Data)
            int offset = fm.readInt() + dataOffset;
            FieldValidator.checkOffset(offset, arcSize);
            offsets[i] = offset;

            TaskProgressManager.setValue(i);
          }

          // now work out the file lengths
          Arrays.sort(offsets);

          int[] lengths = new int[numFiles];
          int totalLength = 0;
          for (int i = 0; i < numFiles - 1; i++) {
            int length = offsets[i + 1] - offsets[i];
            FieldValidator.checkLength(length);
            lengths[i] = length;
            totalLength += length;
          }
          int length = dataLength - totalLength;
          FieldValidator.checkLength(length);
          lengths[numFiles - 1] = length;

          // create the Resources
          for (int i = 0; i < numFiles; i++) {
            int offset = offsets[i];
            length = lengths[i];

            //String filename = names[i] + ".dds";
            String filename = Resource.generateFilename(i) + ".dds";

            //path,name,offset,length,decompLength,exporter
            resources[i] = new Resource(path, filename, offset, length);

            TaskProgressManager.setValue(i);
          }

          // found all the files - break now
          break;
        }
        else {
          // skip over all other blocks
          fm.seek(blockOffset + blockLength);
        }
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
