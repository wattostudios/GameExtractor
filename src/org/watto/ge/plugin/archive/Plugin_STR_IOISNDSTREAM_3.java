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
public class Plugin_STR_IOISNDSTREAM_3 extends ArchivePlugin {

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public Plugin_STR_IOISNDSTREAM_3() {

    super("STR_IOISNDSTREAM_3", "STR_IOISNDSTREAM_3");

    //         read write replace rename
    setProperties(true, false, false, false);

    setGames("Kane and Lynch 2: Dog Days");
    setExtensions("str");
    setPlatforms("PC");

    //setFileTypes("","",
    //             "",""
    //             );

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
      if (fm.readString(12).equals("IOISNDSTREAM")) {
        rating += 50;
      }

      fm.skip(12);

      long arcSize = fm.getLength();

      // Directory Offset
      if (FieldValidator.checkOffset(fm.readLong(), arcSize)) {
        rating += 5;
      }

      // Number Of Files
      if (FieldValidator.checkNumFiles(fm.readInt())) {
        rating += 5;
      }

      // Unknown (256)
      if (fm.readInt() == 256) {
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

      //ExporterPlugin exporter = Exporter_Custom_WAV_RawAudio.getInstance();

      // RESETTING GLOBAL VARIABLES

      FileManipulator fm = new FileManipulator(path, false);

      long arcSize = fm.getLength();

      // 12 - Header (IOISNDSTREAM)
      // 4 - Version? (9)
      // 8 - Unknown
      fm.skip(24);

      // 8 - Directory Offset
      long dirOffset = fm.readLong();
      FieldValidator.checkOffset(dirOffset, arcSize);

      // 4 - Number Of Files
      int numFiles = fm.readInt();
      FieldValidator.checkNumFiles(numFiles);

      // 8 - File Data Offset (256)
      // 4 - Unknown (1)
      // 4 - null
      // 2 - Header Length (56)
      // 2 - Unknown
      // 200 - null Padding to Offset 256
      fm.seek(dirOffset);

      //Resource_WAV_RawAudio[] resources = new Resource_WAV_RawAudio[numFiles];
      Resource[] resources = new Resource[numFiles];
      TaskProgressManager.setMaximum(numFiles);

      // Loop through directory
      long[] nameOffsets = new long[numFiles];
      int[] nameLengths = new int[numFiles];
      long[] audioInfoOffsets = new long[numFiles];
      for (int i = 0; i < numFiles; i++) {
        // 8 - File ID?
        fm.skip(8);

        // 8 - File Offset
        long offset = fm.readLong();
        FieldValidator.checkOffset(offset, arcSize);

        // 8 - File Length (including Padding)
        long length = fm.readLong();
        FieldValidator.checkLength(length, arcSize);

        // 8 - Sub-Directory Offset
        long audioInfoOffset = fm.readLong();
        FieldValidator.checkOffset(audioInfoOffset, arcSize);
        audioInfoOffsets[i] = audioInfoOffset;

        // 4 - Sub-Directory Entry Size (28)
        // 4 - Unknown
        fm.skip(8);

        // 8 - Filename Length (not including null)
        int filenameLength = (int) fm.readLong();
        FieldValidator.checkFilenameLength(filenameLength);
        nameLengths[i] = filenameLength;

        // 8 - Filename Offset
        long filenameOffset = fm.readLong();
        FieldValidator.checkOffset(filenameOffset, arcSize);
        nameOffsets[i] = filenameOffset;

        // 16 - null
        fm.skip(16);

        //path,name,offset,length,decompLength,exporter
        //resources[i] = new Resource_WAV_RawAudio(path, "", offset, length, length, exporter);
        resources[i] = new Resource(path, "", offset, length);

        TaskProgressManager.setValue(i);
      }

      // now go and grab the filenames
      for (int i = 0; i < numFiles; i++) {
        fm.seek(nameOffsets[i]);
        // X - Filename
        // 1 - null Filename Terminator
        String filename = fm.readString(nameLengths[i]);
        FieldValidator.checkFilename(filename);

        Resource resource = resources[i];
        resource.setName(filename);
        resource.setOriginalName(filename);
      }

      /*
      // now go and grab the audio details
      for (int i = 0; i < numFiles; i++) {
        fm.seek(audioInfoOffsets[i]);
      
        // 4 - Audio Codec? (4)
        // 4 - Unknown
        fm.skip(8);
      
        // 4 - Number of Channels (1)
        short channels = (short) fm.readInt();
        if (channels < 0 || channels > 2) {
          channels = 1;
        }
      
        // 4 - Frequency (44100)
        int frequency = fm.readInt();
        if (frequency < 0 || frequency > 45000) {
          frequency = 44100;
        }
      
        // 4 - null
        // 8 - Partial Filename
      
        short bitrate = 16;
      
        Resource_WAV_RawAudio resource = resources[i];
        resource.setAudioProperties(frequency, bitrate, channels);
        //resource.setCodec((short) audioCodec);
      }
      */

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

    if (headerInt1 == 542132556) {
      return "lip";
    }

    return null;
  }

}
