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
import org.watto.io.FilenameSplitter;
import org.watto.io.converter.ByteConverter;
import org.watto.io.converter.StringConverter;
import org.watto.task.TaskProgressManager;

/**
**********************************************************************************************

**********************************************************************************************
**/
public class Plugin_DAT_2 extends ArchivePlugin {

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public Plugin_DAT_2() {

    super("DAT_2", "DAT_2");

    //         read write replace rename
    setProperties(true, false, false, false);

    setExtensions("mbx");
    setGames("Hostile Waters");
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

      getDirectoryFile(fm.getFile(), "dat");
      rating += 25;

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

      long arcSize = (int) path.length();

      File sourcePath = getDirectoryFile(path, "dat");

      FileManipulator fm = new FileManipulator(sourcePath, false);

      // 4 - Number Of Files
      int numFiles = fm.readInt();
      FieldValidator.checkNumFiles(numFiles);

      Resource[] resources = new Resource[numFiles];
      TaskProgressManager.setMaximum(numFiles);

      String arcName = FilenameSplitter.getFilename(path.getName()).toLowerCase();

      boolean moviesDirectory = false;
      try {
        moviesDirectory = path.getParentFile().getParentFile().getName().equalsIgnoreCase("movies");
      }
      catch (Throwable t) {
      }

      for (int i = 0; i < numFiles; i++) {

        String filename = Resource.generateFilename(i) + ".wav";

        // 12 - Unknown
        byte[] filenameBytes = fm.readBytes(12);
        if (ByteConverter.unsign(filenameBytes[0]) == 128) {
          // no filename
        }
        else {
          int nullPos = 0;
          for (int p = 0; p < 12; p++) {
            if (filenameBytes[p] == 0) {
              nullPos = p;
              break;
            }
          }
          if (nullPos != 0) {
            filename = StringConverter.convertLittle(filenameBytes).substring(0, nullPos) + ".wav";
          }

        }

        // 4 - File Length
        long length = fm.readInt();
        //FieldValidator.checkLength(length,arcSize);

        // 4 - File Offset
        long offset = fm.readInt();
        FieldValidator.checkOffset(offset, arcSize);

        //path,id,name,offset,length,decompLength,exporter
        Resource_WAV_RawAudio resource = new Resource_WAV_RawAudio(path, filename, offset, length);
        if (moviesDirectory) {
          resource.setAudioProperties(11025, (short) 32, (short) 2, true);
        }
        else if (arcName.equals("tmungenglish") || arcName.equals("igbrief")) {
          resource.setAudioProperties(11025, (short) 8, (short) 1, true);
        }
        else {
          resource.setAudioProperties(11025, (short) 16, (short) 2, true);
        }

        resources[i] = resource;
        //resources[i] = new Resource(path, filename, offset, length);

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