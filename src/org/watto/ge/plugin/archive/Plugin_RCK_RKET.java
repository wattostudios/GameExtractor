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
import org.watto.datatype.Archive;
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
public class Plugin_RCK_RKET extends ArchivePlugin {

  /**
  **********************************************************************************************

  **********************************************************************************************
  **/
  public Plugin_RCK_RKET() {

    super("RCK_RKET", "RCK_RKET");

    //         read write replace rename
    setProperties(true, false, false, false);

    setGames("How To Survive",
        "How To Survive 2");
    setExtensions("rck"); // MUST BE LOWER CASE
    setPlatforms("PC");

    //setCanScanForFileTypes(true);

    // MUST BE LOWER CASE !!!
    //setFileTypes(new FileType("txt", "Text Document", FileType.TYPE_DOCUMENT),
    //             new FileType("bmp", "Bitmap Image", FileType.TYPE_IMAGE)
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
      if (fm.readString(4).equals("RKET")) {
        rating += 50;
      }

      return rating;

    }
    catch (Throwable t) {
      return 0;
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
  /*
  @Override
  public String guessFileExtension(Resource resource, byte[] headerBytes, int headerInt1, int headerInt2, int headerInt3, short headerShort1, short headerShort2, short headerShort3, short headerShort4, short headerShort5, short headerShort6) {
  
    if (headerInt2 == 44100 || headerInt2 == 22050 || headerInt2 == 48000 || headerInt2 == 11025 || headerInt2 == 32000) {
      return "audio";
    }
    else if (headerInt2 == 32 || headerInt2 == 24) {
      return "image";
    }
  
    return null;
  }
  */

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
      // 4 - Header (RKET)
      // 4 - null
      // 4 - Unknown
      // 4 - Unknown
      // 4 - null
      // 4 - Unknown
      // 4 - Unknown
      // 4 - Unknown
      // 4 - Unknown
      // 4 - Unknown
      // 4 - Unknown
      // 4 - Unknown
      // 4 - Unknown
      // 4 - Unknown
      fm.skip(56);

      int numFiles = Archive.getMaxFiles();

      Resource[] resources = new Resource[numFiles];
      TaskProgressManager.setMaximum(arcSize);

      // Loop through directory
      int realNumFiles = 0;
      while (fm.getOffset() < arcSize) {

        // 4 - File Length
        int length = fm.readInt();
        FieldValidator.checkLength(length, arcSize);

        // 4 - null
        if (fm.readString(4).equals("DDSX")) {
          // 8 - Unknown
          fm.skip(8);

          // 4 - File Length
          length = fm.readInt();
          FieldValidator.checkLength(length, arcSize);

          // X - File Data
          long offset = fm.getOffset();
          fm.skip(length);

          String filename = Resource.generateFilename(realNumFiles) + ".dds";

          //path,name,offset,length,decompLength,exporter
          resources[realNumFiles] = new Resource(path, filename, offset, length);
          realNumFiles++;

          TaskProgressManager.setValue(offset);

        }
        else if (length <= 16) {
          long offset = fm.getOffset();

          // X - File Data
          fm.skip(length);

          String filename = Resource.generateFilename(realNumFiles);

          //path,name,offset,length,decompLength,exporter
          resources[realNumFiles] = new Resource(path, filename, offset, length);
          realNumFiles++;

          TaskProgressManager.setValue(offset);

        }
        else {
          long offset = fm.getOffset();

          // 4 - File Length
          fm.skip(4);

          // 4 - Audio Quality (44100)
          int audioQuality = fm.readInt();

          // 4 - Audio Bitrate (16)
          int audioBitrate = fm.readInt();

          // 4 - Audio Channels? (1)
          int audioChannels = fm.readInt();

          // X - File Data
          fm.skip(length - 16);

          String filename = Resource.generateFilename(realNumFiles);

          if (audioQuality == 44100 || audioQuality == 22050 || audioQuality == 48000 || audioQuality == 11025 || audioQuality == 32000) {
            filename += ".wav";

            length -= 16;
            offset += 16;

            //path,name,offset,length,decompLength,exporter
            Resource_WAV_RawAudio resource = new Resource_WAV_RawAudio(path, filename, offset, length, length, exporter);
            resource.setAudioProperties(audioQuality, (short) audioBitrate, (short) audioChannels);
            resources[realNumFiles] = resource;
            realNumFiles++;
          }
          else if (audioQuality == 32 || audioQuality == 24) {
            filename += ".image";

            //path,name,offset,length,decompLength,exporter
            resources[realNumFiles] = new Resource(path, filename, offset, length);
            realNumFiles++;
          }
          else {
            //path,name,offset,length,decompLength,exporter
            resources[realNumFiles] = new Resource(path, filename, offset, length);
            realNumFiles++;
          }

          TaskProgressManager.setValue(offset);

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

}
