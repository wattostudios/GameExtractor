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
import org.watto.ge.plugin.ExporterPlugin;
import org.watto.ge.plugin.exporter.Exporter_Custom_WAV_RawAudio;
import org.watto.ge.plugin.resource.Resource_WAV_RawAudio;
import org.watto.io.FileManipulator;
import org.watto.task.TaskProgressManager;

/**
**********************************************************************************************

**********************************************************************************************
**/
public class Plugin_STR_IOISNDSTREAM_2 extends ArchivePlugin {

  /**
  **********************************************************************************************

  **********************************************************************************************
  **/
  public Plugin_STR_IOISNDSTREAM_2() {

    super("STR_IOISNDSTREAM_2", "STR_IOISNDSTREAM_2");

    //         read write replace rename
    setProperties(true, false, false, false);

    setGames("Mini Ninjas");
    setExtensions("str");
    setPlatforms("PC");

    //setFileTypes("","",
    //             "",""
    //             );

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
      if (FieldValidator.checkOffset(fm.readInt(), arcSize)) {
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

      ExporterPlugin exporter = Exporter_Custom_WAV_RawAudio.getInstance();

      // RESETTING GLOBAL VARIABLES

      FileManipulator fm = new FileManipulator(path, false);

      long arcSize = fm.getLength();

      // 12 - Header (IOISNDSTREAM)
      // 4 - Version? (9)
      // 4 - Unknown
      // 4 - null
      fm.skip(24);

      // 4 - Directory Offset
      long dirOffset = fm.readInt();
      FieldValidator.checkOffset(dirOffset, arcSize);

      // 4 - Number Of Files
      int numFiles = fm.readInt();
      FieldValidator.checkNumFiles(numFiles);

      // 4 - File Data Offset (256)
      // 4 - null
      // 4 - Unknown (1)
      // 4 - null
      // 4 - Header Length (56)
      // 4 - null
      // 200 - null Padding to Offset 256
      fm.seek(dirOffset);

      Resource_WAV_RawAudio[] resources = new Resource_WAV_RawAudio[numFiles];
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
        resources[i] = new Resource_WAV_RawAudio(path, "", offset, length, length, exporter);

        TaskProgressManager.setValue(i);
      }

      // now go and grab the filenames
      for (int i = 0; i < numFiles; i++) {
        fm.seek(nameOffsets[i]);
        // X - Filename
        // 1 - null Filename Terminator
        resources[i].setName(fm.readString(nameLengths[i]));
      }

      // now go and grab the audio details
      for (int i = 0; i < numFiles; i++) {
        fm.seek(audioInfoOffsets[i]);

        // 4 - Audio Codec? (2/3)
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

        // 4 - Bitrate? (4/16)
        short bitrate = (short) fm.readInt();
        if (bitrate < 0 || bitrate > 64) {
          bitrate = 16;
        }

        resources[i].setAudioProperties(frequency, bitrate, channels);
      }

      fm.close();

      return resources;

    }
    catch (Throwable t) {
      logError(t);
      return null;
    }
  }

}
