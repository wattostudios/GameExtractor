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

import org.watto.Settings;
import org.watto.datatype.Archive;
import org.watto.datatype.FileType;
import org.watto.datatype.Resource;
import org.watto.ge.helper.FieldValidator;
import org.watto.ge.plugin.ArchivePlugin;
import org.watto.ge.plugin.ExporterPlugin;
import org.watto.ge.plugin.exporter.Exporter_Custom_FSB_Audio;
import org.watto.ge.plugin.resource.Resource_FSB_Audio;
import org.watto.io.FileManipulator;
import org.watto.task.TaskProgressManager;

/**
**********************************************************************************************

**********************************************************************************************
**/
public class Plugin_FSB_FSB3 extends ArchivePlugin {

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public Plugin_FSB_FSB3() {

    super("FSB_FSB3", "FSB_FSB3");

    //         read write replace rename
    setProperties(true, false, false, false);

    setExtensions("fsb");
    setGames("American Chopper 2: Full Throttle",
        "Battlestations: Midway",
        "Battlestations: Pacific",
        "BioShock",
        "Cold Fear",
        "Evil Dead: Regeneration",
        "Heroes Of The Pacific",
        "Kung Fu Panda",
        "NRL 2003",
        "Rainbow Six: Lockdown",
        "Spider-Man 3",
        "TimeShift",
        "Warhammer 40k: Fire Warrior");
    setPlatforms("PC", "PS2", "PS3", "GameCube");

    setFileTypes(new FileType("genh", "GameCube Audio", FileType.TYPE_AUDIO));

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
      if (fm.readString(4).equals("FSB3")) {
        rating += 50;
      }

      // Number Of Files
      if (FieldValidator.checkNumFiles(fm.readInt())) {
        rating += 5;
      }

      long arcSize = fm.getLength();

      // Directory Length
      if (FieldValidator.checkLength(fm.readInt(), arcSize)) {
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

      FileManipulator fm = new FileManipulator(path, false);

      boolean debug = Settings.getBoolean("DebugMode");

      long arcSize = fm.getLength();

      int realNumFiles = 0;
      int maxNumFiles = Archive.getMaxFiles();

      Resource[] resources = new Resource[maxNumFiles];
      TaskProgressManager.setMaximum(arcSize);

      while (fm.getOffset() < arcSize - 32 && realNumFiles < maxNumFiles) { // This loop is to support PS3 archives that list multiple FSB3's in a single file
        long startOffset = fm.getOffset();

        System.out.println(startOffset);

        // 4 - Header (FSB3)
        if (fm.readString(4).equals("FSB3")) {
          // good
        }
        else {
          // skip 32 bytes of padding, then try again
          fm.skip(calculatePadding(startOffset + 4, 32));
          continue;
        }

        // 4 - Number Of Files
        int numFiles = fm.readInt();
        FieldValidator.checkNumFiles(numFiles);

        // 4 - Details Directory Length
        long offset = startOffset + fm.readInt() + 24;
        FieldValidator.checkOffset(offset, arcSize);

        // 4 - File Data Length
        int dataLength = fm.readInt();
        FieldValidator.checkLength(dataLength);

        // 2 - Version Major
        // 2 - Version Minor
        fm.skip(4);

        // 4 - Mode
        int mode = fm.readInt();

        boolean paddedFiles = false;
        if ((mode & 0x40) == 0x40) {
          paddedFiles = true;
        }

        boolean basicHeaders = false;
        if ((mode & 0x00000002) == 0x00000002) {
          basicHeaders = true;
        }

        // these are outside the loop, so that if we're using basic headers, we will remember the values for the next loop
        int flags = 0;
        int codec = 0;
        short bits = 0;
        int frequency = 0;
        short channels = 0;
        int samplesLength = 0;
        int compLength = 0;
        String filename = "";
        byte[] extraData = null;

        // Get the FSB Audio exporter
        ExporterPlugin exporter = Exporter_Custom_FSB_Audio.getInstance();

        // Now go through the archive
        for (int i = 0; i < numFiles; i++) {
          if (basicHeaders && i != 0) {
            // 4 - Samples Length
            samplesLength = fm.readInt();

            // 4 - Compressed File Length (length of the whole file, as stored in the archive)
            compLength = fm.readInt();
            FieldValidator.checkLength(compLength, arcSize);

            filename = Resource.generateFilename(i);
          }
          else {
            // 2 - File Entry Length (64/80)
            int entrySize = fm.readShort();

            // 30 - Filename
            filename = fm.readNullString(30);
            if (filename.length() <= 0 || basicHeaders) {
              filename = Resource.generateFilename(i);
            }

            //FieldValidator.checkFilename(filename);

            // 4 - Samples Length
            samplesLength = fm.readInt();

            // 4 - Compressed File Length (length of the whole file, as stored in the archive)
            compLength = fm.readInt();
            FieldValidator.checkLength(compLength, arcSize);

            if (realNumFiles != 0 && resources[realNumFiles - 1].getName().startsWith(filename) && resources[realNumFiles - 1].getLength() == compLength) {
              // a footer or something, for the same file that we've already, so skip it
              fm.seek(startOffset + 4 + calculatePadding(startOffset + 4, 32));
              continue;
            }

            // 4 - Loop Start
            // 4 - Loop End
            fm.skip(8);

            // 4 - Flags
            flags = fm.readInt();

            // Work out the audio codec from the Flags
            codec = Resource_FSB_Audio.CODEC_PCM16;

            if ((flags & 0x00000200) == 0x00000200) {
              codec = Resource_FSB_Audio.CODEC_MPEG;
            }
            else if ((flags & 0x00000400) == 0x00000400) {
              codec = Resource_FSB_Audio.CODEC_IT214;
            }
            else if ((flags & 0x00000800) == 0x00000800) {
              codec = Resource_FSB_Audio.CODEC_IT215;
            }
            else if ((flags & 0x00400000) == 0x00400000) {
              codec = Resource_FSB_Audio.CODEC_IMAADPCM;
            }
            else if ((flags & 0x00800000) == 0x00800000) {
              codec = Resource_FSB_Audio.CODEC_VAG;
            }
            else if ((flags & 0x02000000) == 0x02000000) {
              codec = Resource_FSB_Audio.CODEC_GCADPCM;
            }
            else if ((flags & 0x01000000) == 0x01000000) {
              codec = Resource_FSB_Audio.CODEC_XMA;
            }
            else if ((flags & 0x08000000) == 0x08000000) {
              codec = Resource_FSB_Audio.CODEC_VORBIS;
            }
            else if ((flags & 0x00000008) == 0x00000008) {
              if (codec == Resource_FSB_Audio.CODEC_PCM16) {
                codec = Resource_FSB_Audio.CODEC_PCM8;
              }
            }

            if (debug) {
              System.out.println("[Plugin_FSB_FSB3] Codec is " + codec);
            }

            // work out the bitrate
            bits = 16;
            if ((flags & 0x00000008) == 0x00000008) {
              bits = 8;
            }
            else if ((flags & 0x00000010) == 0x00000010) {
              bits = 16;
            }

            // 4 - Frequency / Sound Sample Rate (32000/22050/44100)
            frequency = fm.readInt();

            // 2 - Volume
            // 2 - Pan
            // 2 - Pri
            fm.skip(6);

            // 2 - Channels - Stereo/Mono (2)
            channels = fm.readShort();

            if (entrySize >= 80) {
              // 4 - Minimum Distance
              // 4 - Maximum Distance
              // 4 - Variable Frequency
              // 2 - Variable Volume
              // 2 - Variable Pan
              fm.skip(16);
            }

            if (entrySize > 80) {
              // for GameCube audio
              extraData = fm.readBytes(entrySize - 80);
            }

          }

          //path,id,name,offset,length,decompLength,exporter
          Resource_FSB_Audio resource = new Resource_FSB_Audio(path, filename, offset, compLength, compLength, exporter);
          resource.setCodec(codec);
          resource.addExtensionForCodec(); // adds an appropriate file extension to the filename, based on the codec
          resource.setFrequency(frequency);
          resource.setChannels(channels);
          resource.setBits(bits);
          resource.setSamplesLength(samplesLength);
          resource.setExtraData(extraData);
          resources[realNumFiles] = resource;
          realNumFiles++;

          TaskProgressManager.setValue(offset);
          offset += compLength;

          // if the files are padded to 32-bytes, add the padding here
          if (paddedFiles) {
            offset += calculatePadding(compLength, 32);
          }
        }

        fm.seek(offset); // move to the start of the next file (offset value is already updated from the calculations above)
      }

      fm.close();

      resources = resizeResources(resources, realNumFiles);

      return resources;

    }
    catch (Throwable t) {
      logError(t);
      return null;
    }
  }

}