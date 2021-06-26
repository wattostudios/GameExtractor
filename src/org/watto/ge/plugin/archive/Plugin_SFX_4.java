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
import org.watto.Language;
import org.watto.datatype.Archive;
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
public class Plugin_SFX_4 extends ArchivePlugin {

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public Plugin_SFX_4() {

    super("SFX_4", "SFX_4");

    //         read write replace rename
    setProperties(true, true, true, false);

    setExtensions("sfx");
    setGames("Worms");
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

      int sizePos = (250 * 4) + 24;
      int fileJump = sizePos + sizePos;

      int numFiles = Archive.getMaxFiles(4);//guess

      long arcSize = fm.getLength();

      Resource[] resources = new Resource[numFiles];
      TaskProgressManager.setMaximum(arcSize);

      long currentPos = 0;
      int directoryPos = 0;
      int i = 0;
      int readLength = 0;
      while (currentPos < fm.getLength()) {
        // 4 - offset
        long offset = fm.readInt();
        FieldValidator.checkOffset(offset, arcSize);

        directoryPos = (int) fm.getOffset();
        fm.seek(sizePos);

        // 4 - fileLength - offset
        long length = fm.readInt() - offset;
        FieldValidator.checkLength(length, arcSize);

        sizePos = (int) fm.getOffset();

        offset += fileJump;

        currentPos = offset + length;
        if (currentPos >= fm.getLength()) {
          length = (int) fm.getLength() - offset;
        }

        fm.seek(directoryPos);

        String filename = Resource.generateFilename(i) + ".wav";

        //path,id,name,offset,length,decompLength,exporter
        Resource_WAV_RawAudio resource = new Resource_WAV_RawAudio(path, filename, offset, length);
        resource.setAudioProperties(11025, (short) 8, (short) 1, false);
        resources[i] = resource;

        TaskProgressManager.setValue(readLength);
        readLength += length;
        i++;
      }

      resources = resizeResources(resources, i);

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
  
  **********************************************************************************************
  **/
  @Override
  public void write(Resource[] resources, File path) {
    try {

      FileManipulator fm = new FileManipulator(path, true);

      int numFiles = resources.length;
      TaskProgressManager.setMaximum(numFiles);

      // Loop 1 - Directory
      long offset = (4 * numFiles);

      TaskProgressManager.setMessage(Language.get("Progress_WritingDirectory"));
      for (int i = 0; i < numFiles; i++) {
        Resource fd = resources[i];
        long length = fd.getDecompressedLength();

        // 4 - offset
        fm.writeInt((int) offset);

        offset += length;
      }

      TaskProgressManager.setMessage(Language.get("Progress_WritingFiles"));
      write(resources, fm);

      fm.close();

    }
    catch (Throwable t) {
      logError(t);
    }
  }

}