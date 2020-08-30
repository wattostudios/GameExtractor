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
import org.watto.Settings;
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
public class Plugin_FSB_FSB1 extends ArchivePlugin {

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public Plugin_FSB_FSB1() {

    super("FSB_FSB1", "FSB_FSB1");

    //         read write replace rename
    setProperties(true, false, false, false);

    setExtensions("fsb");
    setGames("FMOD");
    setPlatforms("PC");

    //setFileTypes("spr", "Object Sprite");

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
      if (fm.readString(4).equals("FSB1")) {
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

      // 4 - Header (FSB1)
      fm.skip(4);

      // 4 - Number Of Files
      int numFiles = fm.readInt();
      FieldValidator.checkNumFiles(numFiles);

      long arcSize = fm.getLength();

      Resource[] resources = new Resource[numFiles];
      TaskProgressManager.setMaximum(numFiles);

      // 4 - Details Directory Length
      long offset = fm.readInt() + 24;
      FieldValidator.checkOffset(offset, arcSize);

      // 4 - File Data Length
      fm.skip(4);

      // Get the FSB Audio exporter
      ExporterPlugin exporter = Exporter_Custom_FSB_Audio.getInstance();

      // Now go through the archive
      for (int i = 0; i < numFiles; i++) {

        // 4 - Samples Length
        int samplesLength = fm.readInt();

        // 4 - Compressed File Length (length of the whole file, as stored in the archive)
        int compLength = fm.readInt();
        FieldValidator.checkLength(compLength, arcSize);

        // 4 - Frequency / Sound Sample Rate (32000/22050/44100)
        int frequency = fm.readInt();

        // 2 - Pri
        fm.skip(2);

        // 2 - Channels - Stereo/Mono (2)
        short channels = fm.readShort();

        // 2 - Volume (255)
        // 2 - Pan (128)
        fm.skip(4);

        // 4 - Flags
        int flags = fm.readInt();

        // Work out the audio codec from the Flags
        int codec = Resource_FSB_Audio.CODEC_PCM16;

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
          System.out.println("[Plugin_FSB_FSB1] Codec is " + codec);
        }

        // work out the bitrate
        short bits = 16;
        if ((flags & 0x00000008) == 0x00000008) {
          bits = 8;
        }
        else if ((flags & 0x00000010) == 0x00000010) {
          bits = 16;
        }

        // 4 - Loop Start
        // 4 - Loop End
        fm.skip(8);

        String filename = Resource.generateFilename(i);

        //path,id,name,offset,length,decompLength,exporter
        Resource_FSB_Audio resource = new Resource_FSB_Audio(path, filename, offset, compLength, compLength, exporter);
        resource.setCodec(codec);
        resource.addExtensionForCodec(); // adds an appropriate file extension to the filename, based on the codec
        resource.setFrequency(frequency);
        resource.setChannels(channels);
        resource.setBits(bits);
        resource.setSamplesLength(samplesLength);
        resources[i] = resource;

        TaskProgressManager.setValue(i);
        offset += compLength;
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