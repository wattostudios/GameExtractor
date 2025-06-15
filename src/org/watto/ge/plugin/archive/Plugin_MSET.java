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

import org.watto.datatype.Archive;
import org.watto.datatype.Resource;
import org.watto.ge.helper.FieldValidator;
import org.watto.ge.plugin.ArchivePlugin;
import org.watto.ge.plugin.ExporterPlugin;
import org.watto.ge.plugin.exporter.Exporter_ZLib;
import org.watto.io.FileManipulator;
import org.watto.io.converter.IntConverter;
import org.watto.io.converter.ShortConverter;
import org.watto.task.TaskProgressManager;

/**
**********************************************************************************************

**********************************************************************************************
**/
public class Plugin_MSET extends ArchivePlugin {

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public Plugin_MSET() {

    super("MSET", "MSET");

    //         read write replace rename
    setProperties(true, false, false, false);

    setGames("Neverwinter Online");
    setExtensions("mset"); // MUST BE LOWER CASE
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

      fm.skip(4);

      if (IntConverter.changeFormat(fm.readInt()) == 14) {
        rating += 5;
      }

      fm.skip(4);

      // 2 - Model Definition Count
      if (FieldValidator.checkRange(ShortConverter.changeFormat(fm.readShort()), 0, 500)) { // guess
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

      ExporterPlugin exporter = Exporter_ZLib.getInstance();

      // RESETTING GLOBAL VARIABLES

      FileManipulator fm = new FileManipulator(path, false);

      long arcSize = fm.getLength();

      int numFiles = Archive.getMaxFiles();
      int realNumFiles = 0;

      // 4 - Header (82) (LITTLE)
      // 4 - Version (14)
      // 4 - CRC
      fm.skip(12);

      // 2 - Model Definition Count
      short numModels = ShortConverter.changeFormat(fm.readShort());
      FieldValidator.checkRange(numModels, 0, 500);//guess

      int[][] offsets = new int[numModels][0];
      int[][] lengths = new int[numModels][0];
      String[] names = new String[numModels];
      for (int m = 0; m < numModels; m++) {

        // 2 - Name Length
        short nameLength = ShortConverter.changeFormat(fm.readShort());
        FieldValidator.checkFilenameLength(nameLength);

        // X - Name
        String name = fm.readString(nameLength);
        names[m] = name;

        // 2 - Number of Offsets
        short numOffsets = ShortConverter.changeFormat(fm.readShort());
        FieldValidator.checkNumFiles(numOffsets);

        int[] localOffsets = new int[numOffsets];
        int[] localLengths = new int[numOffsets];

        for (int o = 0; o < numOffsets; o++) {
          // 4 - Model Offset
          int offset = IntConverter.changeFormat(fm.readInt());
          FieldValidator.checkOffset(offset, arcSize);
          localOffsets[o] = offset;

          // 4 - Model Length
          int length = IntConverter.changeFormat(fm.readInt());
          FieldValidator.checkLength(length, arcSize);
          localLengths[o] = length;
        }

        offsets[m] = localOffsets;
        lengths[m] = localLengths;

        // 4 - Unknown (null)
        // 4 - Unknown (null)
        // 4 - Unknown (null)
        fm.skip(12);
      }

      Resource[] resources = new Resource[numFiles];
      TaskProgressManager.setMaximum(arcSize);

      String[] extensions = new String[] { "Triangles", "Vertices", "Normals", "Binormals", "Tangents", "TexCoord", "TexCoord3", "Colors", "Weights", "MaterialIndexes", "Vertices2", "Normals2" };

      for (int m = 0; m < numModels; m++) {
        int[] localOffsets = offsets[m];
        //int[] localLengths = lengths[m];
        String name = names[m];

        for (int o = 0; o < localOffsets.length; o++) {
          int localOffset = localOffsets[o];
          fm.relativeSeek(localOffset);

          // 4 - Data Size
          // 4 - Number of Vertices
          // 4 - Number of Faces
          // 4 - Number of Textures
          // 4 - Average Texel Density (Float)
          // 4 - Standard Deviation Texel Density (Float)
          // 4 - Process Time Flags
          // 4 - Unknown
          fm.skip(32);

          for (int i = 0; i < 12; i++) {
            // 4 - Model Data Compressed Length
            int length = IntConverter.changeFormat(fm.readInt());
            FieldValidator.checkLength(length, arcSize);

            // 4 - Model Data Decompressed Length
            int decompLength = IntConverter.changeFormat(fm.readInt());
            if (decompLength < 0) {
              decompLength = 0 - decompLength;
            }
            FieldValidator.checkLength(decompLength);

            // 4 - Model Data Offset (relative to the start of this offset)
            int offset = IntConverter.changeFormat(fm.readInt()) + localOffset;
            FieldValidator.checkOffset(offset, arcSize);

            String filename = name + "_Part" + (m + 1) + "_Group" + (o + 1) + "." + extensions[i];

            resources[realNumFiles] = new Resource(path, filename, offset, length, decompLength, exporter);
            realNumFiles++;

            TaskProgressManager.setValue(offset);
          }
        }
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
