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
import org.watto.task.TaskProgressManager;

/**
**********************************************************************************************

**********************************************************************************************
**/
public class Plugin_BIG_3 extends ArchivePlugin {

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public Plugin_BIG_3() {

    super("BIG_3", "BIG_3");

    //         read write replace rename
    setProperties(true, false, false, false);

    setGames("Armed and Dangerous");
    setExtensions("big"); // MUST BE LOWER CASE
    setPlatforms("PC");

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
        rating += 24;
      }

      // only applies to files with the name *aud.big
      if (fm.getFile().getName().indexOf("aud.big") > 0) {
        rating += 50;
      }

      long arcSize = fm.getLength();

      // 4 - Unknown
      fm.skip(4);

      // 4 - Archive Length [+8]
      if (FieldValidator.checkEquals(fm.readInt() + 8, arcSize)) {
        rating += 5;
      }

      // 4 - ID Directory Length
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

      // 4 - Unknown
      // 4 - Archive Length [+8]
      fm.skip(8);

      // 4 - ID Directory Length
      int dirLength = fm.readInt();
      FieldValidator.checkLength(dirLength, arcSize);
      fm.skip(dirLength);

      // 4 - Number Of Files
      int numFiles = fm.readInt();
      FieldValidator.checkNumFiles(numFiles);

      Resource_WAV_RawAudio[] resources = new Resource_WAV_RawAudio[numFiles];
      TaskProgressManager.setMaximum(numFiles);

      // Loop through directory
      int realNumFiles = 0;
      for (int i = 0; i < numFiles; i++) {
        // 4 - Unknown (a little bit bigger than the archive length)
        fm.skip(4);

        // 32 - Filename (null terminated, filled with nulls)
        String filename = fm.readNullString(32);
        FieldValidator.checkFilename(filename);
        filename += ".wav";

        // 4 - Unknown
        // 4 - Unknown
        // 10 - null
        // 4 - Unknown
        // 4 - Unknown
        // 164 - null
        fm.skip(190);

        // 2 - Number of Channels? (1)
        short channels = fm.readShort();
        FieldValidator.checkRange(channels, 0, 3);

        // 4 - Frequency 1 (22050)
        int frequency = fm.readInt();
        FieldValidator.checkRange(frequency, 0, 45000);

        // 4 - Frequency 2 (44100)
        fm.skip(4);

        // 2 - Number of Channels? (2)
        fm.skip(2);

        // 2 - Bitrate? (16)
        short bitrate = fm.readShort();
        FieldValidator.checkRange(bitrate, 0, 65);

        // 4 - null
        fm.skip(4);

        // 4 - File Length
        int length = fm.readInt();
        FieldValidator.checkLength(length, arcSize);

        // 4 - File Length [*2]
        fm.skip(4);

        // X - Raw Audio Data
        long offset = fm.getOffset();

        if (offset + length > arcSize) {
          break; // end of file, ignore the last file which is kinda a directory
        }

        fm.skip(length);

        //path,name,offset,length,decompLength,exporter
        resources[i] = new Resource_WAV_RawAudio(path, filename, offset, length);
        resources[i].setAudioProperties(frequency, bitrate, channels);

        TaskProgressManager.setValue(i);
        realNumFiles++;
      }

      Resource[] resizedResources = resizeResources(resources, realNumFiles);

      fm.close();

      return resizedResources;

    }
    catch (Throwable t) {
      logError(t);
      return null;
    }
  }

}
