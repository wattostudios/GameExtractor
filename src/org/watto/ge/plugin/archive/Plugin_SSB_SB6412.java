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
import org.watto.ge.plugin.exporter.Exporter_Custom_WAV_RawAudio;
import org.watto.io.FileManipulator;
import org.watto.task.TaskProgressManager;

/**
**********************************************************************************************

**********************************************************************************************
**/
public class Plugin_SSB_SB6412 extends ArchivePlugin {

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public Plugin_SSB_SB6412() {

    super("SSB_SB6412", "SSB_SB6412");

    //         read write replace rename
    setProperties(true, false, false, false);

    setGames("Captain Blood");
    setExtensions("ssb"); // MUST BE LOWER CASE
    setPlatforms("PC");

    // MUST BE LOWER CASE !!!
    //setFileTypes(new FileType("txt", "Text Document", FileType.TYPE_DOCUMENT),
    //             new FileType("bmp", "Bitmap Image", FileType.TYPE_IMAGE)
    //             );

    //setTextPreviewExtensions("colours", "rat", "screen", "styles"); // LOWER CASE

    setCanScanForFileTypes(true);

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
      if (fm.readString(8).equals("SB641.2 ")) {
        rating += 50;
      }

      fm.skip(4);

      if (fm.readInt() == 16) {
        rating += 5;
      }

      fm.skip(16);

      if (fm.readLong() == 112) {
        rating += 5;
      }

      // Number Of Files
      if (FieldValidator.checkNumFiles((int) fm.readLong())) {
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

      // 8 - Header ("SB641.2 ")
      // 4 - Unknown (33,178,161,194)
      // 4 - Block Size? (16)
      // 16 - CRC?
      fm.skip(32);

      // 8 - Block 1 Offset (112) [+16]
      long block1Offset = fm.readLong() + 16;
      FieldValidator.checkOffset(block1Offset, arcSize);

      // 8 - Number of Entries in Block 1
      int numBlock1Entries = (int) fm.readLong();
      FieldValidator.checkNumFiles(numBlock1Entries);

      // 8 - Block 2 Offset [+16]
      long block2Offset = fm.readLong() + 16;
      FieldValidator.checkOffset(block2Offset, arcSize);

      // 8 - Number of Entries in Block 2
      int numBlock2Entries = (int) fm.readLong();
      FieldValidator.checkNumFiles(numBlock2Entries);

      // 8 - Block 3 (Details Directory) Offset [+16]
      long block3Offset = fm.readLong() + 16;
      FieldValidator.checkOffset(block3Offset, arcSize);

      // 8 - Number of Entries in Block 3
      int numBlock3Entries = (int) fm.readLong();
      FieldValidator.checkNumFiles(numBlock3Entries);

      // 8 - Block 4 Offset [+16]
      // 8 - Number of Entries in Block 4

      // 8 - Footer Offset [+16]
      // 8 - Number of Entries in the Footer

      // 8 - Block 5 Offset [+16]
      // 8 - Number of Entries in Block 5 and Block 6
      fm.seek(block1Offset);

      int numFiles = numBlock1Entries;

      Resource[] resources = new Resource[numFiles];
      TaskProgressManager.setMaximum(numFiles);

      // Loop through Block 1
      long[] offsets = new long[numFiles];
      long[] filenameOffsets = new long[numFiles];
      for (int i = 0; i < numFiles; i++) {
        // 8 - Offset to an entry in Block 2 [+16]
        long offset = fm.readLong() + 16;
        FieldValidator.checkOffset(offset, arcSize);
        offsets[i] = offset;

        // 4 - Unknown (1)
        // 20 - null
        fm.skip(24);

        // 8 - Filename Offset [+16]
        long filenameOffset = fm.readLong() + 16;
        FieldValidator.checkOffset(filenameOffset, arcSize);
        filenameOffsets[i] = filenameOffset;

        // 4 - Hash?
        // 4 - Filename Length (not including null)
        // 4 - Unknown
        // 16 - null
        // 4 - Unknown (16)
        // 4 - Unknown
        // 16 - null
        // 4 - Unknown Float
        // 4 - Unknown Float
        // 4 - Unknown
        fm.skip(64);

        TaskProgressManager.setValue(i);
      }

      // Loop through Block 2
      for (int i = 0; i < numFiles; i++) {
        fm.relativeSeek(offsets[i]);

        // 4 - Unknown Float
        // 4 - Unknown Float
        // 4 - Unknown Float
        // 4 - Unknown Float
        fm.skip(16);

        // 8 - Offset to an entry in Block 3 [+16]
        long offset = fm.readLong() + 16;
        FieldValidator.checkOffset(offset, arcSize);
        offsets[i] = offset;

        TaskProgressManager.setValue(i);
      }

      // Loop through Block 3
      int[] lengths = new int[numFiles];
      for (int i = 0; i < numFiles; i++) {
        long offset = offsets[i];
        if (offset == 16) {
          // empty file?
          offsets[i] = 0;
          lengths[i] = 0;
        }
        else {
          fm.relativeSeek(offset);

          // 8 - File Offset [+16]
          offset = fm.readLong() + 16;
          FieldValidator.checkOffset(offset, arcSize);
          offsets[i] = offset;

          // 4 - File Length
          int length = fm.readInt();
          FieldValidator.checkLength(length, arcSize);
          lengths[i] = length;

          // 4 - Unknown
          // 4 - Unknown
          // 4 - Unknown Float
          // 4 - Unknown Float
          // 4 - null
          // 4 - Unknown
          // 4 - null
          // 8 - Offset to an entry in Block 4 [+16]
          // 16 - null
          fm.skip(44);

          TaskProgressManager.setValue(i);
        }
      }

      // Loop through the Filenames, and create the Resources
      for (int i = 0; i < numFiles; i++) {
        fm.relativeSeek(filenameOffsets[i]);

        // X - Filename
        // 1 - null Filename Terminator
        String filename = fm.readNullString();
        FieldValidator.checkFilename(filename);

        long offset = offsets[i];
        int length = lengths[i];

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

    if (headerInt1 == 1399285583) {
      return "ogg";
    }
    else {
      // Raw audio
      resource.addProperty("AudioFrequency", 44100);
      resource.addProperty("AudioBitRate", 16);
      resource.addProperty("AudioChannels", 1);
      resource.addProperty("AudioSigned", true);
      resource.setExporter(Exporter_Custom_WAV_RawAudio.getInstance());
      return "wav";
    }

    //return null;
  }

}
