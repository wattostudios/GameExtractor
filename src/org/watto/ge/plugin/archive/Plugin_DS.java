/*
 * Application:  Game Extractor
 * Author:       wattostudios
 * Website:      http://www.watto.org
 * Copyright:    Copyright (c) 2002-2022 wattostudios
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
public class Plugin_DS extends ArchivePlugin {

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public Plugin_DS() {

    super("DS", "DS");

    //         read write replace rename
    setProperties(true, false, false, false);

    setGames("Warhammer 40,000: Chaos Gate");
    setExtensions("ds"); // MUST BE LOWER CASE
    setPlatforms("PC");

    // MUST BE LOWER CASE !!!
    //setFileTypes(new FileType("txt", "Text Document", FileType.TYPE_DOCUMENT),
    //             new FileType("bmp", "Bitmap Image", FileType.TYPE_IMAGE)
    //             );

    //setTextPreviewExtensions("colours", "rat", "screen", "styles"); // LOWER CASE

    //setCanScanForFileTypes(true);

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

      if (fm.readInt() == -1) {
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

      int numFiles = 2048; // max number of files in this archive format

      Resource[] resources = new Resource[numFiles];
      TaskProgressManager.setMaximum(arcSize);

      // Loop through directory
      int realNumFiles = 0;
      for (int i = 0; i < numFiles; i++) {
        // 4 - File Length (or -1)
        int length = fm.readInt();

        if (length == -1) {
          // file that no longer exists in the archive
          fm.skip(200);
          continue;
        }

        FieldValidator.checkLength(length, arcSize);

        // 4 - File Offset
        int offset = fm.readInt();
        FieldValidator.checkOffset(offset, arcSize);

        if (length == 0 && offset == 0) {
          // end of directory
          break;
        }

        // 128 - Filename (null terminated, filled with nulls)
        String filename = fm.readNullString(128);
        FieldValidator.checkFilename(filename);

        // 4 - Unknown (0/1)
        // 4 - Unknown (72/66)
        // 4 - Unknown (9041132)
        // 4 - null
        fm.skip(16);

        // 2 - Codec (1/2)
        short codec = fm.readShort();

        // 2 - Channels (1)
        short channels = fm.readShort();

        // 4 - Audio Frequency (22050)
        int frequency = fm.readInt();

        // 4 - Audio Frequency Average (44100/11155)
        fm.skip(4);

        // 2 - Block Alignment (2/512)
        short blockAlign = fm.readShort();

        // 2 - Bitrate (4/16)
        short bitrate = fm.readShort();

        // Extra Data {
        //   2 - Unknown (32/0)
        //   2- Unknown (1012/0)
        //   2 - Unknown (7/0)
        //   4 - Unknown (256/0)
        //   2 - Unknown (512/0)
        //   2 - Unknown (-256/0)
        //   4 - null
        //   2 - Unknown (192/0)
        //   2 - Unknown (64/0)
        //   4 - Unknown (240/0)
        //   8 - Unknown (or null)
        // }
        byte[] extraData = fm.readBytes(34);

        // 2 - null
        fm.skip(2);

        //path,name,offset,length,decompLength,exporter
        Resource_WAV_RawAudio resource = new Resource_WAV_RawAudio(path, filename, offset, length);
        resource.setAudioProperties(frequency, bitrate, channels);
        resource.setCodec(codec);
        resource.setBlockAlign(blockAlign);
        resource.setExtraData(extraData);

        resources[realNumFiles] = resource;

        realNumFiles++;

        TaskProgressManager.setValue(offset);
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

    /*
    if (headerInt1 == 2037149520) {
      return "js";
    }
    */

    return null;
  }

}
