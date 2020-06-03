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
import org.watto.datatype.ReplacableResource;
import org.watto.datatype.Resource;
import org.watto.ge.helper.FieldValidator;
import org.watto.ge.plugin.ArchivePlugin;
import org.watto.io.FileManipulator;
import org.watto.task.TaskProgressManager;

/**
**********************************************************************************************

**********************************************************************************************
**/
public class Plugin_GLI_GLIB extends ArchivePlugin {

  /**
  **********************************************************************************************

  **********************************************************************************************
  **/
  public Plugin_GLI_GLIB() {

    super("GLI_GLIB", "GLI_GLIB");

    //         read write replace rename
    setProperties(true, false, false, false);
    setCanImplicitReplace(true);

    setExtensions("gli", "glj");
    setGames("Airline Tycoon");
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

      // Header
      if (fm.readString(5).equals("GLIB2")) {
        rating += 50;
      }

      fm.skip(5);

      // Archive Size
      if (fm.readInt() == fm.getLength()) {
        rating += 5;
      }

      // null
      if (fm.readLong() == 0) {
        rating += 5;
      }

      fm.skip(12);

      // Number Of Files
      if (FieldValidator.checkNumFiles(fm.readInt())) {
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

      // 8 - Header (GLIB2) + 3 nulls
      // 2 - Unknown (3)
      // 4 - Archive Length
      // 8 - null
      // 4 - Unknown
      // 4 - Unknown
      // 4 - Unknown
      fm.skip(34);

      // 4 - numFiles [-1]
      int numFiles = fm.readInt() - 1;
      FieldValidator.checkNumFiles(numFiles);

      long arcSize = fm.getLength();

      Resource[] resources = new Resource[numFiles];
      TaskProgressManager.setMaximum(numFiles);

      // 4 - Unknown (54)
      // 4 - Unknown
      // 8 - null
      // 4 - Unknown (13)
      // 4 - Unknown
      // 1 - null
      // 4 - Data Length
      fm.skip(29);

      for (int i = 0; i < numFiles; i++) {

        // 4 - Type ID?
        // 1 - Unknown (1)
        fm.skip(5);

        // 8 - Filename (null)
        String filename = fm.readNullString(8);
        FieldValidator.checkFilename(filename);

        // 4 - Offset
        long offsetPointerLocation = fm.getOffset();
        long offsetPointerLength = 4;

        long offset = fm.readInt();
        FieldValidator.checkOffset(offset, arcSize);

        //path,id,name,offset,length,decompLength,exporter
        resources[i] = new ReplacableResource(path, filename, offset, offsetPointerLocation, offsetPointerLength);

        TaskProgressManager.setValue(i);
      }

      // Calculate File Sizes
      for (int j = 0; j < numFiles - 1; j++) {
        resources[j].setLength((int) (resources[j + 1].getOffset() - resources[j].getOffset()));
        FieldValidator.checkLength(resources[j].getLength(), arcSize);
      }
      resources[numFiles - 1].setLength((int) (arcSize - resources[numFiles - 1].getOffset()));

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

      TaskProgressManager.setMessage(Language.get("Progress_PerformingCalculations"));
      long arcSize = 0;
      for (int i = 0; i < numFiles; i++) {
        arcSize += resources[i].getDecompressedLength();
      }

      // 8 - Header (GLIB2) + 3 nulls
      // 2 - Unknown (3)
      fm.writeBytes(src.readBytes(10));

      // 4 - Archive Length
      fm.writeInt((int) arcSize + (17 * numFiles) + 67);
      src.skip(4);

      // 8 - null
      // 4 - Unknown
      // 4 - Unknown
      // 4 - Unknown
      // 4 - numFiles [-1]
      // 4 - Unknown (54)
      // 4 - Unknown
      // 8 - null
      // 4 - Unknown (13)
      // 4 - Unknown
      // 1 - null
      fm.writeBytes(src.readBytes(49));

      // 4 - Data Length
      fm.writeInt((int) arcSize);
      src.skip(4);

      long offset = 67 + (17 * numFiles);

      TaskProgressManager.setMessage(Language.get("Progress_WritingDirectory"));
      for (int i = 0; i < numFiles; i++) {
        long length = resources[i].getDecompressedLength();

        // 4 - Type ID?
        // 1 - Unknown (1)
        fm.writeBytes(src.readBytes(5));

        // 8 - Filename (null)
        // 4 - Offset
        src.skip(12);
        fm.writeNullString(resources[i].getName(), 8);
        fm.writeInt((int) offset);

        offset += length;
      }

      TaskProgressManager.setMessage(Language.get("Progress_WritingFiles"));
      write(resources, fm);

      src.close();

      fm.close();

    }
    catch (Throwable t) {
      logError(t);
    }
  }

}