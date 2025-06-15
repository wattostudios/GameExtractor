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

import org.watto.datatype.Resource;
import org.watto.ge.helper.FieldValidator;
import org.watto.ge.plugin.ArchivePlugin;
import org.watto.ge.plugin.ExporterPlugin;
import org.watto.ge.plugin.exporter.Exporter_Custom_VAG_Audio;
import org.watto.ge.plugin.resource.Resource_WAV_RawAudio;
import org.watto.io.FileManipulator;
import org.watto.task.TaskProgressManager;

/**
**********************************************************************************************

**********************************************************************************************
**/
public class Plugin_ZSD_ZSNDWIN extends ArchivePlugin {

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public Plugin_ZSD_ZSNDWIN() {

    super("ZSD_ZSNDWIN", "ZSD_ZSNDWIN");

    //         read write replace rename
    setProperties(true, false, false, false);

    setGames("Dave Mirra Freestyle BMX",
        "Space Invaders");
    setExtensions("zsd");
    setPlatforms("PC", "PSX");

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
      if (fm.readString(8).equals("ZSNDWIN ")) {
        rating += 25;
      }

      fm.skip(4);

      long arcSize = fm.getLength();

      // Directory Length
      if (FieldValidator.checkLength(fm.readInt(), arcSize)) {
        rating += 5;
      }

      fm.skip(16);

      // Number Of Files
      if (FieldValidator.checkNumFiles(fm.readInt())) {
        rating += 5;
      }

      // Dir Offset 3
      if (FieldValidator.checkOffset(fm.readInt(), arcSize)) {
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

      // NOTE - Compressed file MUST know their DECOMPRESSED LENGTH
      //      - Uncompressed files MUST know their LENGTH

      ExporterPlugin vagExporter = Exporter_Custom_VAG_Audio.getInstance();

      addFileTypes();

      // RESETTING THE GLOBAL VARIABLES

      FileManipulator fm = new FileManipulator(path, false);

      long arcSize = (int) fm.getLength();

      // 8 - Header (ZSNDWIN )
      // 4 - Archive Size
      // 4 - File Data Offset (152)
      // 4 - Number Of Entries In Directory 1
      // 4 - Offset To Directory 1
      fm.skip(24);

      // 4 - Number Of Entries In Directory 2
      int numSoundEntries = fm.readInt();
      FieldValidator.checkNumFiles(numSoundEntries + 1);

      // 4 - Offset To Directory Data 2 (Sound Details)
      int soundDirOffset = fm.readInt();
      FieldValidator.checkOffset(soundDirOffset, arcSize);

      // 4 - Number Of Entries In Directory 3
      int numFiles = fm.readInt();
      FieldValidator.checkNumFiles(numFiles);

      // 4 - Offset To Directory Data 3 (Offsets and Lengths)
      long dirOffset = fm.readInt();
      FieldValidator.checkOffset(dirOffset, arcSize);

      int[] soundQuality = new int[numFiles];
      if (numSoundEntries > 0) {
        // read the sound details

        fm.seek(soundDirOffset);

        for (int i = 0; i < numSoundEntries; i++) {
          // 2 - Unknown
          int fileID = fm.readShort();
          if (fileID < 0 || fileID >= numFiles) {
            fm.skip(22);
            continue;
          }

          // 2 - Unknown
          fm.skip(2);

          // 4 - Sound Quality (22050)
          soundQuality[fileID] = fm.readInt();

          // 4 - null
          // 4 - Unknown
          // 4 - Unknown
          // 4 - null
          fm.skip(16);
        }
      }

      fm.seek(dirOffset);

      Resource[] resources = new Resource[numFiles];

      TaskProgressManager.setMaximum(numFiles);

      // Loop through directory
      for (int i = 0; i < numFiles; i++) {
        // 4 - File Offset
        long offset = fm.readInt();
        FieldValidator.checkOffset(offset, arcSize);

        // 4 - File Length
        long length = fm.readInt();
        FieldValidator.checkLength(length, arcSize);

        String filename = Resource.generateFilename(i);

        if (numSoundEntries > 0) {
          // audio file

          filename += ".wav";

          //path,id,name,offset,length,decompLength,exporter
          Resource_WAV_RawAudio resource = new Resource_WAV_RawAudio(path, filename, offset, length);

          int frequency = soundQuality[i];
          if (frequency >= 0 && frequency < 4000) {
            // VAG audio
            resource.setExporter(vagExporter);
          }
          else {
            // WAV audio (maybe)
            resource.setAudioProperties(soundQuality[i], (short) 16, (short) 1);
          }

          resources[i] = resource;
        }
        else {
          // anything else

          if (length % 32 == 0) {
            // maybe a VAG file (PSX)
            filename += ".wav";

            //path,id,name,offset,length,decompLength,exporter
            //resources[i] = new Resource(path, filename, offset, length);
            resources[i] = new Resource(path, filename, offset, length, length, vagExporter);
          }
          else {
            /*
            filename += ".wav";
            // maybe a WAV file? Dave Mirra Freestyle BMX might have these Music files encrypted?
            //path,id,name,offset,length,decompLength,exporter
            Resource_WAV_RawAudio resource = new Resource_WAV_RawAudio(path, filename, offset, length);
            resource.setAudioProperties(32000, (short) 16, (short) 1);
            resources[i] = resource;
            */
            resources[i] = new Resource(path, filename, offset, length);
          }

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
