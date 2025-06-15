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

import org.watto.component.WSPluginManager;
import org.watto.datatype.Resource;
import org.watto.ge.helper.FieldValidator;
import org.watto.ge.plugin.ArchivePlugin;
import org.watto.ge.plugin.ViewerPlugin;
import org.watto.ge.plugin.resource.Resource_WAV_RawAudio;
import org.watto.io.FileManipulator;
import org.watto.task.TaskProgressManager;

/**
**********************************************************************************************

**********************************************************************************************
**/
public class Plugin_XWB_WBND extends ArchivePlugin {

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public Plugin_XWB_WBND() {

    super("XWB_WBND", "XWB_WBND");

    //         read write replace rename
    setProperties(true, false, false, false);

    setGames("Full Spectrum Warrior",
        "Return To Castle Wolfenstein: Tide Of War",
        "Star Trek Shattered Universe",
        "Unreal Championship 2: The Liandri Conflict",
        "Powerdrome");
    setExtensions("xwb");
    setPlatforms("PC", "XBox");

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

      // Version (3)
      if (fm.readInt() == 3) {
        rating += 5;
      }

      // Size Of Header 1 (40)
      if (fm.readInt() == 40) {
        rating += 5;
      }

      // Size Of Header 2 (40)
      if (fm.readInt() == 40) {
        rating += 5;
      }

      // Directory Offset (80)
      if (fm.readInt() == 80) {
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

      // 4 - Header (WBND)
      // 4 - Version (3)
      // 4 - Size Of Header 1 (40)
      // 4 - Size Of Header 2 (40)
      fm.skip(16);

      // 4 - Offset To Details Directory (80)
      long dirOffset = fm.readInt();
      FieldValidator.checkOffset(dirOffset, arcSize);

      // 4 - Length Of Details Directory
      fm.skip(4);

      // 4 - Offset To Filename Directory
      int filenameDirOffset = fm.readInt();
      FieldValidator.checkOffset(filenameDirOffset, arcSize);

      // 4 - Length Of Filename Directory
      fm.skip(4);

      // 4 - First File Offset (8192) [+8]
      int firstFileOffset = fm.readInt() + 8;
      FieldValidator.checkOffset(firstFileOffset, arcSize);

      // 4 - Length Of File Data
      // 2 - Unknown (1)
      // 2 - Unknown (1)
      fm.skip(8);

      // 4 - Number Of Files
      int numFiles = fm.readInt();
      FieldValidator.checkNumFiles(numFiles);

      // 16 - Archive Filename (null) (without extension)
      // 4 - Length Of Each Details Entry (24)
      // 4 - Length Of Each Filename Entry (64)
      // 4 - Max padding size between each file (2048)
      // 4 - null

      Resource[] resources = new Resource[numFiles];
      TaskProgressManager.setMaximum(numFiles);

      fm.seek(filenameDirOffset);

      // FILENAMES DIRECTORY
      String[] names = new String[numFiles];
      if (filenameDirOffset > 0) {
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
        // 2 - Number of Channels (0/2)
        // 2 - Format (0=normal pcm) (1=xbox adpcm)
        fm.skip(4);

        // 4 - Flags
        int flags = fm.readInt();

        // 4 - File Offset (relative to the start of the file data)
        long offset = fm.readInt() + firstFileOffset;
        FieldValidator.checkOffset(offset, arcSize);

        // 4 - File Length
        long length = fm.readInt();
        FieldValidator.checkLength(length, arcSize);

        FieldValidator.checkOffset(offset + length, arcSize + 100); // help to rule out some different formats (eg where the files are stored as OGG)

        // 8 - null
        fm.skip(8);

        String filename = names[i] + ".wav";

        //path,id,name,offset,length,decompLength,exporter
        Resource_WAV_RawAudio resource = new Resource_WAV_RawAudio(path, filename, offset, length);
        resources[i] = resource;

        short codec = (short) ((flags) & ((1 << 2) - 1));
        short channels = (short) ((flags >> (2)) & ((1 << 3) - 1));
        int frequency = ((flags >> (2 + 3)) & ((1 << 18) - 1));
        short blockAlign = (short) ((flags >> (2 + 3 + 18)) & ((1 << 8) - 1));
        short bitrate = (short) ((flags >> (2 + 3 + 18 + 8)) & ((1 << 1) - 1));

        if (bitrate == 0) {
          bitrate = 8;
        }
        else if (bitrate == 1) {
          bitrate = 16;
        }

        if (codec == 1) {
          codec = 0x0069; // XBox ADPCM
        }
        else if (codec == 0) {
          codec = 0x0001; // PCM
        }

        resource.setAudioProperties(frequency, bitrate, channels);
        resource.setBlockAlign(blockAlign);
        resource.setCodec(codec);

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
   Provide hints to the previewer so that certain document types are displayed appropriately
   **********************************************************************************************
   **/
  @Override
  public ViewerPlugin previewHint(Resource resource) {
    String extension = resource.getExtension();
    if (extension.equalsIgnoreCase("wav")) {
      return (ViewerPlugin) WSPluginManager.getPlugin("Viewer", "VGMSTREAM_Audio_WAV_RIFF");
    }
    return null;
  }

}
