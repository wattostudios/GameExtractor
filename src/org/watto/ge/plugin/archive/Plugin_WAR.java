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
import org.watto.Settings;
import org.watto.datatype.Resource;
import org.watto.ge.helper.FieldValidator;
import org.watto.ge.plugin.ArchivePlugin;
import org.watto.io.FileManipulator;
import org.watto.task.TaskProgressManager;

/**
**********************************************************************************************

**********************************************************************************************
**/
public class Plugin_WAR extends ArchivePlugin {

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public Plugin_WAR() {

    super("WAR", "Warcraft WAR");

    //         read write replace rename
    setProperties(true, false, true, false);

    setExtensions("war", "sud");
    setGames("Warcraft",
        "Warcraft 2");
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

      fm.skip(4);

      // Number Of Files
      if (FieldValidator.checkNumFiles(fm.readShort())) {
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
      // 4 - Unknown
      fm.skip(4);

      // 2 - numFiles
      int numFiles = fm.readShort();
      FieldValidator.checkNumFiles(numFiles);

      long arcSize = fm.getLength();

      Resource[] resources = new Resource[numFiles];
      TaskProgressManager.setMaximum(numFiles);

      // 2 - Unknown
      fm.skip(2);

      for (int i = 0; i < numFiles; i++) {

        // 4 - offset
        long offset = fm.readInt();
        FieldValidator.checkOffset(offset, arcSize);

        String filename = Resource.generateFilename(i);

        //path,id,name,offset,length,decompLength,exporter
        resources[i] = new Resource(path, filename, offset);

        TaskProgressManager.setValue(i);
      }

      // Calculate File Sizes
      for (int i = 0; i < numFiles - 1; i++) {
        Resource resource = resources[i];

        long thisOffset = resource.getOffset();
        long nextOffset = resources[i + 1].getOffset();
        long length = nextOffset - thisOffset;

        if (length > 4) {
          thisOffset += 4;
          length -= 4;
        }

        FieldValidator.checkLength(length, arcSize);

        resource.setLength(length);
        resource.setDecompressedLength(length);
        resource.setOffset(thisOffset);
      }
      //resources[numFiles - 1].setLength((int) (arcSize - resources[numFiles - 1].getOffset() - 4));
      long lastOffset = resources[numFiles - 1].getOffset();
      long lastLength = arcSize - lastOffset;

      if (lastLength > 4) {
        lastOffset += 4;
        lastLength -= 4;
      }

      resources[numFiles - 1].setLength(lastLength);
      resources[numFiles - 1].setDecompressedLength(lastLength);
      resources[numFiles - 1].setOffset(lastOffset);

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

      int numFiles = resources.length;
      TaskProgressManager.setMaximum(numFiles);

      FileManipulator fm = new FileManipulator(path, true);
      FileManipulator src = new FileManipulator(new File(Settings.getString("CurrentArchive")), false);

      // 4 - Unknown
      // 2 - numFiles
      // 2 - Unknown
      fm.writeBytes(src.readBytes(8));

      src.close();

      TaskProgressManager.setMessage(Language.get("Progress_WritingDirectory"));
      long offset = 8 + (numFiles * 4);
      for (int i = 0; i < numFiles; i++) {
        Resource fd = resources[i];
        long length = fd.getDecompressedLength() + 4;

        // 4 - fileOffset
        fm.writeInt((int) offset);

        offset += length;
      }

      TaskProgressManager.setMessage(Language.get("Progress_WritingFiles"));
      for (int i = 0; i < numFiles; i++) {
        long length = resources[i].getDecompressedLength();
        if (length + 4 > 1) {
          // 4 - File Length
          fm.writeInt((int) length);

          // X - File Data
          write(resources[i], fm);
          TaskProgressManager.setValue(i);
        }
        else if (length + 4 == 1) {
          fm.writeByte(0);
        }
      }

      fm.close();

    }
    catch (Throwable t) {
      logError(t);
    }
  }

}