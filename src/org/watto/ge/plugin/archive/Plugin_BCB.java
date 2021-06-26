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
import org.watto.datatype.Resource;
import org.watto.ge.helper.FieldValidator;
import org.watto.ge.plugin.ArchivePlugin;
import org.watto.io.FileManipulator;
import org.watto.io.converter.ByteConverter;
import org.watto.task.TaskProgressManager;

/**
**********************************************************************************************

**********************************************************************************************
**/
public class Plugin_BCB extends ArchivePlugin {

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public Plugin_BCB() {

    super("BCB", "BCB");

    //         read write replace rename
    setProperties(true, true, true, true);

    setGames("Stolen",
        "Peter Pan: The Legend of Neverland");
    setExtensions("bcb");
    setPlatforms("PC", "PS2");

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

      // Number Of Files
      if (FieldValidator.checkNumFiles(fm.readInt())) {
        rating += 5;
      }

      long arcSize = fm.getLength();

      // First File Length
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

      // RESETTING THE GLOBAL VARIABLES

      FileManipulator fm = new FileManipulator(path, false);

      // 4 - Number Of Files
      int numFiles = fm.readInt();
      FieldValidator.checkNumFiles(numFiles);

      long arcSize = fm.getLength();

      Resource[] resources = new Resource[numFiles];
      TaskProgressManager.setMaximum(numFiles);

      fm.skip(numFiles * 4);

      // FILENAMES DIRECTORY
      String[] names = new String[numFiles];
      for (int i = 0; i < numFiles; i++) {
        // 1 - Filename Length
        // X - Filename
        names[i] = fm.readString(ByteConverter.unsign(fm.readByte()));
      }

      long offset = (int) fm.getOffset();
      long paddingSize = 2048 - (offset % 2048);
      if (paddingSize < 2048) {
        offset += paddingSize;
      }

      fm.seek(4);

      // Loop through directory
      for (int i = 0; i < numFiles; i++) {
        // 4 - File Size
        long length = fm.readInt();
        FieldValidator.checkLength(length, arcSize);

        String filename = names[i];

        //path,id,name,offset,length,decompLength,exporter
        resources[i] = new Resource(path, filename, offset, length);

        TaskProgressManager.setValue(i);
        offset += length;

        offset += calculatePadding(offset, 2048);

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
  
  **********************************************************************************************
  **/
  @Override
  public void write(Resource[] resources, File path) {
    try {

      FileManipulator fm = new FileManipulator(path, true);
      int numFiles = resources.length;
      TaskProgressManager.setMaximum(numFiles);

      // Write Header Data

      // 4 - Number Of Files
      fm.writeInt(numFiles);

      // Write Directory
      TaskProgressManager.setMessage(Language.get("Progress_WritingDirectory"));
      for (int i = 0; i < numFiles; i++) {
        // 4 - File Size
        fm.writeInt((int) resources[i].getDecompressedLength());
      }

      for (int i = 0; i < numFiles; i++) {
        String filename = resources[i].getName();

        // 1 - Filename Length
        fm.writeInt(filename.length());

        // X - Filename
        fm.writeString(filename);
      }

      // Padding
      int paddingSize = calculatePadding(fm.getOffset(), 2048);
      for (int p = 0; p < paddingSize; p++) {
        fm.writeByte(0);
      }

      // Write Files
      TaskProgressManager.setMessage(Language.get("Progress_WritingFiles"));
      for (int i = 0; i < numFiles; i++) {
        Resource resource = resources[i];
        write(resource, fm);

        // Padding
        paddingSize = calculatePadding(resource.getDecompressedLength(), 2048);
        for (int p = 0; p < paddingSize; p++) {
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
