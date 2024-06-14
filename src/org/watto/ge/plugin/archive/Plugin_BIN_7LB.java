/*
 * Application:  Game Extractor
 * Author:       wattostudios
 * Website:      http://www.watto.org
 * Copyright:    Copyright (c) 2002-2024 wattostudios
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
import org.watto.io.FileManipulator;
import org.watto.task.TaskProgressManager;

/**
**********************************************************************************************

**********************************************************************************************
**/
public class Plugin_BIN_7LB extends ArchivePlugin {

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public Plugin_BIN_7LB() {

    super("BIN_7LB", "7th Level Engine - Format B");

    //         read write replace rename
    setProperties(true, false, false, false);

    setGames("Battle Beast",
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
      if (fm.readInt() == 21122103) { // "7LB" + (byte)1
        rating += 50;
      }

      // Version
      if (fm.readInt() == 2) {
        rating += 5;
      }

      fm.skip(74);

      // null
      if (fm.readInt() == 0) {
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

      // 4 - Header (7LB + (byte)1)
      // 4 - Version (2)
      // 78 - Description (null terminated, filled with nulls)
      // 4 - Unknown (2)
      // 8 - null
      // 4 - Unknown
      // 4 - Unknown
      // 88 - null
      // 2 - Unknown (1/2)
      // 4 - Unknown (236)
      // 2 - Unknown (200)
      // 2 - Unknown (14)
      // 2 - Unknown (213)
      // 2 - Unknown (1)
      // 4 - Unknown
      // 14 - null
      fm.skip(226);

      // 4 - Details Directory Offset
      int dirOffset = fm.readInt();
      FieldValidator.checkOffset(dirOffset, arcSize);

      // 4 - Details Directory Length
      int numFiles = (fm.readInt() / 10);
      FieldValidator.checkNumFiles(numFiles);

      Resource[] resources = new Resource[numFiles + 4]; // +4 for each of the remaining directories (palettes, texts, etc)
      TaskProgressManager.setMaximum(numFiles);

      int realNumFiles = 0;

      // 16 - null
      fm.skip(16);

      // 4 - Constants Directory Offset
      int constDirOffset = fm.readInt();
      FieldValidator.checkOffset(constDirOffset, arcSize);

      // 4 - Constants Directory Length
      int constDirLength = fm.readInt();
      FieldValidator.checkLength(constDirLength, arcSize);

      //path,id,name,offset,length,decompLength,exporter
      resources[realNumFiles] = new Resource(path, "constants.dat", constDirOffset, constDirLength);
      realNumFiles++;

      // 4 - Texts Directory Offset
      int textsDirOffset = fm.readInt();
      FieldValidator.checkOffset(textsDirOffset, arcSize);

      // 4 - Texts Directory Length
      int textsDirLength = fm.readInt();
      FieldValidator.checkLength(textsDirLength, arcSize);

      //path,id,name,offset,length,decompLength,exporter
      resources[realNumFiles] = new Resource(path, "texts.dat", textsDirOffset, textsDirLength);
      realNumFiles++;

      // 4 - Code Section Offset
      int codeOffset = fm.readInt();
      FieldValidator.checkOffset(codeOffset, arcSize);

      // 4 - Code Section Length
      int codeLength = fm.readInt();
      FieldValidator.checkLength(codeLength, arcSize);

      //path,id,name,offset,length,decompLength,exporter
      resources[realNumFiles] = new Resource(path, "code.dat", codeOffset, codeLength);
      realNumFiles++;

      // 4 - Palette Section Offset
      int paletteOffset = fm.readInt();
      FieldValidator.checkOffset(paletteOffset, arcSize);

      // 4 - Palette Section Length
      int paletteLength = fm.readInt();
      FieldValidator.checkLength(paletteLength, arcSize);

      //path,id,name,offset,length,decompLength,exporter
      resources[realNumFiles] = new Resource(path, "palette.dat", paletteOffset, paletteLength);
      realNumFiles++;

      // 40 - null
      fm.seek(dirOffset);

      // Loop through directory

      for (int i = 0; i < numFiles; i++) {
        // 2 - Flags (1=Graphics, 4=Music, 7=Sound) (8-16=Alias, 0-7=Resource)
        short flags = fm.readShort();

        // 4 - File Offset
        int offset = fm.readInt();
        FieldValidator.checkOffset(offset, arcSize);

        // 4 - File Length
        int length = fm.readInt();
        FieldValidator.checkLength(length, arcSize);

        if (flags > 9) {
          // an alias - skip t
          continue;
        }

        String extension;
        if (flags == 1) {
          extension = ".tex";
        }
        else if (flags == 4) {
          extension = ".music";
        }
        else if (flags == 7) {
          extension = ".audio";
        }
        else {
          extension = "." + flags;
        }

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
