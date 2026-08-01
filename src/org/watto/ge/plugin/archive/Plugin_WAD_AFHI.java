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
import org.watto.ge.plugin.resource.Resource_WAV_RawAudio;
import org.watto.io.FileManipulator;
import org.watto.task.TaskProgressManager;

/**
**********************************************************************************************

**********************************************************************************************
**/
public class Plugin_WAD_AFHI extends ArchivePlugin {

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public Plugin_WAD_AFHI() {

    super("WAD_AFHI", "WAD_AFHI");

    //         read write replace rename
    setProperties(true, false, false, false);

    setGames("Starsky And Hutch (2003)");
    setExtensions("wad", "wd");
    setPlatforms("PC");

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
      if (fm.readString(4).equals("AFHI")) {
        rating += 50;
      }

      // Number Of Files
      if (FieldValidator.checkNumFiles(fm.readInt())) {
        rating += 5;
      }

      fm.skip(4);

      // null
      if (fm.readInt() == 0) {
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

      FileManipulator fm = new FileManipulator(path, false);

      long arcSize = fm.getLength();

      // 4 - Header (AFHI)
      fm.skip(4);

      // 4 - Number Of Files
      int numFiles = fm.readInt();
      FieldValidator.checkNumFiles(numFiles);

      // 4 - Number Of Files
      // 4 - null
      // 4 - Version (1)
      // 4 - Version (1)
      // 4 - null
      // 4 - File Data Length? [+1140]
      // 4 - File Data Length? [+1140]
      // 4 - null
      fm.skip(32);

      // 4 - Directory Offset (112)
      long dirOffset = fm.readInt();
      FieldValidator.checkOffset(dirOffset, arcSize);

      // 4 - Directory Length (numFiles*48)
      // 4 - Length of the Directory Entries (48)
      fm.skip(8);

      // 4 - Filename Directory Offset
      int filenameDirOffset = fm.readInt();
      FieldValidator.checkOffset(filenameDirOffset, arcSize);

      // 4 - Length Of Filename Directory
      fm.skip(4);

      // 4 - First File Offset
      int firstFileOffset = fm.readInt();
      FieldValidator.checkOffset(firstFileOffset, arcSize);

      // 4 - Length Of File Data
      fm.skip(4);

      // 4 - Sound Quality Directory Offset
      int audioDirOffset = fm.readInt();
      FieldValidator.checkOffset(audioDirOffset, arcSize);

      // 4 - Length Of Sound Quality Directory
      // 4 - ID Directory Offset
      // 4 - Length Of ID Directory
      // 4 - Unknown (65536)
      // 4 - Unknown (32768)
      // 4 - Unknown (14)
      // 4 - Unknown
      // 12 - null

      fm.seek(filenameDirOffset);

      // Loop through the filename directory
      String[] names = new String[numFiles];
      for (int i = 0; i < numFiles; i++) {
        // X - Filename
        // 1 - null Filename Terminator
        names[i] = fm.readNullString();
        FieldValidator.checkFilename(names[i]);
      }

      // loop through the audio data
      fm.seek(audioDirOffset);

      int[] channels = new int[numFiles];
      int[] frequency = new int[numFiles];
      int[] bitrate = new int[numFiles];
      for (int i = 0; i < numFiles; i++) {
        // 2 - Unknown (1)
        fm.skip(2);

        // 2 - Channels (1/2)
        channels[i] = fm.readShort();

        // 4 - Sound Quality (11025/22050)
        frequency[i] = fm.readInt();

        // 4 - Unknown
        // 2 - Unknown
        fm.skip(6);

        // 4 - Bitrate? (16)
        bitrate[i] = fm.readShort();
        fm.skip(2);
      }

      Resource[] resources = new Resource[numFiles];
      TaskProgressManager.setMaximum(numFiles);

      fm.seek(dirOffset);

      // Loop through directory
      for (int i = 0; i < numFiles; i++) {
        // 4 - File ID?
        // 4 - Unknown
        // 4 - Unknown
        // 4 - Unknown
        // 4 - File ID?
        fm.skip(20);

        // 4 - File Offset [+FirstFileOffset]
        long offset = fm.readInt() + firstFileOffset;
        FieldValidator.checkOffset(offset, arcSize);

        // 4 - File Length
        long length = fm.readInt();
        FieldValidator.checkLength(length, arcSize);

        // 4 - File ID?
        // 4 - Unknown (18)
        // 2 - File Type ID?
        // 2 - File Type ID?
        // 8 - null
        fm.skip(20);

        String filename = names[i] + ".wav";

        //path,id,name,offset,length,decompLength,exporter
        Resource_WAV_RawAudio resource = new Resource_WAV_RawAudio(path, filename, offset, length);
        resource.setAudioProperties(frequency[i], bitrate[i], channels[i]);
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

}
