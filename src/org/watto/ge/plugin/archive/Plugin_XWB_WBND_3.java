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
import org.watto.ge.plugin.resource.Resource_WAV_RawAudio;
import org.watto.io.FileManipulator;
import org.watto.io.converter.IntConverter;
import org.watto.task.TaskProgressManager;

/**
**********************************************************************************************

**********************************************************************************************
**/
public class Plugin_XWB_WBND_3 extends ArchivePlugin {

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public Plugin_XWB_WBND_3() {

    super("XWB_WBND_3", "XWB_WBND_3");

    //         read write replace rename
    setProperties(true, false, false, false);

    setGames("One Finger Death Punch",
        "Towerfall Ascension",
        "Two Worlds");
    setExtensions("xwb");
    setPlatforms("XBox");

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
      if (fm.readString(4).equals("WBND")) {
        rating += 50;
      }

      // Version (42)
      if (fm.readInt() == 42) {
        rating += 5;
      }

      // Version (42)
      if (fm.readInt() == 42) {
        rating += 5;
      }

      // Size Of Header 1 (52)
      if (fm.readInt() == 52) {
        rating += 5;
      }

      // Size Of Header 2 (96)
      if (fm.readInt() == 96) {
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
  
  **********************************************************************************************
  **/
  @Override
  public Resource[] read(File path) {
    try {

      addFileTypes();

      // RESETTING THE GLOBAL VARIABLES
      //ExporterPlugin exporter = Exporter_Custom_WAV_RawAudio.getInstance();

      FileManipulator fm = new FileManipulator(path, false);

      long arcSize = fm.getLength();

      // 4 - Header (WBND)
      // 4 - Unknown (42)
      // 4 - Unknown (42)
      // 4 - Size Of Header 1 (52)
      // 4 - Size Of Header 2 (96)
      fm.skip(20);

      // 4 - Offset To Details Directory
      long dirOffset = fm.readInt();
      FieldValidator.checkOffset(dirOffset, arcSize);

      // 4 - Length Of Details Directory
      fm.skip(4);

      // 4 - Offset To Filename Directory
      int filenameDirOffset = fm.readInt();
      FieldValidator.checkOffset(filenameDirOffset, arcSize);

      // 4 - Length Of Filename Directory (null)
      int filenameDirLength = fm.readInt();
      FieldValidator.checkLength(filenameDirLength, arcSize);

      // 8 - null
      int nextField1 = fm.readInt();
      int nextField2 = fm.readInt();

      if (filenameDirLength == 0 && filenameDirOffset == nextField1) {
        filenameDirLength = nextField2;
        FieldValidator.checkLength(filenameDirLength, arcSize);
      }

      // 4 - First File Offset
      int firstFileOffset = fm.readInt();
      FieldValidator.checkOffset(firstFileOffset, arcSize);

      // 4 - File Data Length
      // 2 - Unknown (1)
      // 2 - Unknown (8)
      fm.skip(8);

      // 4 - Number Of Files
      int numFiles = fm.readInt();
      FieldValidator.checkNumFiles(numFiles);

      // 64 - Archive Filename (null) (without extension)
      // 4 - Length Of Each Details Entry (24)
      // 4 - Length Of Each Filename Entry (64)
      // 4 - Max padding size between each file (2048)
      // 4 - null
      // 8 - CRC?
      fm.skip(88);

      Resource[] resources = new Resource[numFiles];
      TaskProgressManager.setMaximum(numFiles);

      fm.seek(filenameDirOffset);

      // FILENAMES DIRECTORY
      String[] names = new String[numFiles];
      if (filenameDirOffset > 0 && filenameDirLength > 0) {
        for (int i = 0; i < numFiles; i++) {
          // 64 - Filename (null) (without extension)
          String filename = fm.readNullString(64);
          FieldValidator.checkFilename(filename);
          names[i] = filename;
        }
      }
      else {
        for (int i = 0; i < numFiles; i++) {
          names[i] = Resource.generateFilename(i);
        }
      }

      fm.seek(dirOffset);

      // Loop through directory
      for (int i = 0; i < numFiles; i++) {
        // 4 - Flags
        fm.skip(4);

        // 4 - Audio Details
        long audioDetails = IntConverter.unsign(fm.readInt());

        // 0 00000000 000111110100000000 010 01
        // | |        |                  |   |
        // | |        |                  |   wFormatTag
        // | |        |                  nChannels
        // | |        nSamplesPerSec
        // | wBlockAlign
        // wBitsPerSample

        int codec = (int) ((audioDetails) & ((1 << 2) - 1));
        int channels = (int) ((audioDetails >> (2)) & ((1 << 3) - 1));
        int frequency = (int) ((audioDetails >> (2 + 3)) & ((1 << 18) - 1));
        int align = (int) ((audioDetails >> (2 + 3 + 18)) & ((1 << 8) - 1));
        //int bits = (int) ((audioDetails >> (2 + 3 + 18 + 8)) & ((1 << 1) - 1));

        // 4 - File Offset (relative to the start of the file data)
        long offset = fm.readInt() + firstFileOffset;
        FieldValidator.checkOffset(offset, arcSize);

        // 4 - File Length
        long length = fm.readInt();
        FieldValidator.checkLength(length, arcSize);

        // 4 - null
        // 4 - Unknown
        fm.skip(8);

        String filename = names[i];

        //path,id,name,offset,length,decompLength,exporter

        short bitrate = 8;

        String extension = ".wav";
        if (codec == 0) { // WAVEBANKMINIFORMAT_TAG_PCM      0x0     // PCM data
          codec = 0x0001;
          bitrate = 8;
          align = (bitrate / 8) * channels;
        }
        else if (codec == 1) { // WAVEBANKMINIFORMAT_TAG_XMA      0x1     // XMA data
          extension = ".xma";
        }
        else if (codec == 2) { // WAVEBANKMINIFORMAT_TAG_ADPCM    0x2     // ADPCM data
          codec = 0x0002;
          bitrate = 4;
          align = (align + 22) * channels;
        }
        else if (codec == 3) { // WAVEBANKMINIFORMAT_TAG_WMA      0x3     // WMA data
          extension = ".wma";
        }

        filename += extension;

        if (extension.equals(".wav")) {
          // WAV format
          Resource_WAV_RawAudio resource = new Resource_WAV_RawAudio(path, filename, offset, length);
          resource.setAudioProperties(frequency, bitrate, (short) channels, true);
          resource.setCodec((short) codec);
          resource.setBlockAlign((short) align);
          resources[i] = resource;
        }
        else {
          // something else
          Resource resource = new Resource(path, filename, offset, length);
          resources[i] = resource;
        }

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

}
