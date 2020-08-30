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
import org.watto.datatype.FileType;
import org.watto.datatype.Resource;
import org.watto.ge.helper.FieldValidator;
import org.watto.ge.plugin.ArchivePlugin;
import org.watto.io.FileManipulator;
import org.watto.task.TaskProgressManager;

/**
**********************************************************************************************

**********************************************************************************************
**/
public class Plugin_BIN_7L extends ArchivePlugin {

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public Plugin_BIN_7L() {

    super("BIN_7L", "BIN_7L");

    //         read write replace rename
    setProperties(true, false, false, false);

    setGames("Battle Beast",
        "Take Your Best Shot",
        "Arcade America",
        "Monty Python's Complete Waste Of Time",
        "Monty Python And The Quest For The Holy Grail",
        "Monty Pythons Meaning Of Life",
        "Krondor",
        "G-Nome",
        "Tuneland",
        "The Great Word Adventure",
        "The Universe According To Virgil");
    setExtensions("bin");
    setPlatforms("PC");

    // MUST BE LOWER CASE !!!
    setFileTypes(new FileType("7lg", "Graphics File", FileType.TYPE_IMAGE),
        new FileType("7lm", "MIDI File", FileType.TYPE_AUDIO),
        new FileType("pal", "Color Palette", FileType.TYPE_OTHER),
        new FileType("7la", "Audio File", FileType.TYPE_AUDIO));

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
      if (fm.readString(2).equals("7L")) {
        rating += 50;
      }

      // Number of Descriptions
      if (FieldValidator.checkNumFiles(fm.readShort())) {
        rating += 5;
      }

      fm.skip(82);

      // Number Of Files
      int numFiles = fm.readShort();
      if (FieldValidator.checkNumFiles(numFiles)) {
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

      long arcSize = fm.getLength();

      // 2 - Header (7L)
      // 2 - Version (1)
      // 1 - Description Length
      // 79 - Description (null)
      fm.skip(84);

      // 2 - Number Of Files
      int numFiles = fm.readShort();
      while (numFiles == 0) {
        numFiles = fm.readShort();
      }
      FieldValidator.checkNumFiles(numFiles);

      // 2 - Unknown
      fm.skip(2);

      // 2 - Number of IDs?
      int numIDs = fm.readShort();
      FieldValidator.checkNumFiles(numIDs + 1);
      fm.skip(numIDs * 2);

      // 2 - null
      // 2 - Unknown
      // 2 - Unknown
      fm.skip(6);

      // 2 - Number of Unknown Entries
      int numUnknown = fm.readShort();
      FieldValidator.checkNumFiles(numUnknown + 1);

      // 2 - null
      // 2 - Unknown
      fm.skip(4);

      // 2 - Name Directory Length
      int nameDirLength = fm.readShort();
      FieldValidator.checkLength(nameDirLength, arcSize);

      // 4 - Unknown
      // 4 - Unknown
      // 8 - null
      fm.skip(16);

      Resource[] resources = new Resource[numFiles];
      TaskProgressManager.setMaximum(numFiles);

      // Loop through directory
      int realNumFiles = 0;
      for (int i = 0; i < numFiles; i++) {
        // 2 - File Type ID? (1,3,5,11,12)
        int type = fm.readShort();

        // 4 - Offset (relative to the end of the directory)
        long offset = fm.readInt();// + dataOffset;
        FieldValidator.checkOffset(offset, arcSize);

        // 4 - Length
        long length = fm.readInt();
        FieldValidator.checkLength(length, arcSize);

        if (type != 1 && type != 4 && type != 6 && type != 7) {
          continue;
        }

        String extension = "";
        if (type == 1) {
          extension = ".7lg";
        }
        else if (type == 4) {
          extension = ".7lm";
        }
        else if (type == 6) {
          extension = ".pal";
        }
        else if (type == 7) {
          extension = ".7la";
        }

        //String filename = names[i] + ext;
        String filename = Resource.generateFilename(realNumFiles) + extension;

        //path,id,name,offset,length,decompLength,exporter
        resources[realNumFiles] = new Resource(path, filename, offset, length);
        realNumFiles++;

        TaskProgressManager.setValue(i);
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
