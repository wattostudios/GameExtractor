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
import org.watto.component.WSPluginManager;
import org.watto.datatype.Resource;
import org.watto.ge.helper.FieldValidator;
import org.watto.ge.plugin.ArchivePlugin;
import org.watto.ge.plugin.ViewerPlugin;
import org.watto.io.FileManipulator;
import org.watto.task.TaskProgressManager;

/**
**********************************************************************************************

**********************************************************************************************
**/
public class Plugin_PAK_PACK extends ArchivePlugin {

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public Plugin_PAK_PACK() {

    super("PAK_PACK", "Generic PAK Archive [PAK_PACK]");

    //         read write replace rename
    setProperties(true, true, true, true);

    setExtensions("pak");
    setGames("Conquest Of The New World",
        "Gunman Chronicles",
        "Half-Life",
        "Hands Of Fate",
        "Heretic 2",
        "Hexen 2",
        "Kingpin: Life of Crime",
        "Quake",
        "Quake 2",
        "Search And Rescue 2",
        "Search And Rescue 3",
        "Skisprung Winter Cup 2005",
        "Sin",
        "Soldier of Fortune",
        "Trickstyle");
    setPlatforms("PC");

    setFileTypes("spr", "Object Sprite",
        "bsp", "Level Map",
        "nod", "Map Graph",
        "mdl", "Object Model",
        "lst", "File List",
        "lmp", "Color Palette",
        "nrp", "Map Graph Report",
        "wad", "Texture Archive");

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
      if (fm.readString(4).equals("PACK")) {
        rating += 50;
      }

      long arcSize = fm.getLength();

      // Directory Offset
      if (FieldValidator.checkOffset(fm.readInt(), arcSize)) {
        rating += 5;
      }

      // Directory Length
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
   Provide hints to the previewer so that certain document types are displayed appropriately
   **********************************************************************************************
   **/
  @Override
  public ViewerPlugin previewHint(Resource resource) {
    String extension = resource.getExtension();
    if (extension.equalsIgnoreCase("wrs") || extension.equalsIgnoreCase("sp") || extension.equalsIgnoreCase("lst") || extension.equalsIgnoreCase("ifl") || extension.equalsIgnoreCase("gpm") || extension.equalsIgnoreCase("gpl") || extension.equalsIgnoreCase("gbm") || extension.equalsIgnoreCase("bpf") || extension.equalsIgnoreCase("nrp")) {
      return (ViewerPlugin) WSPluginManager.getPlugin("Viewer", "TXT");
    }
    return null;
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

      // 4 - Header
      fm.skip(4);

      // 4 - Directory Offset
      long dirOffset = fm.readInt();
      FieldValidator.checkOffset(dirOffset, arcSize);

      // 4 - Directory Length
      int dirLength = fm.readInt();
      FieldValidator.checkLength(dirLength, arcSize);

      int numFiles = dirLength / 64;
      FieldValidator.checkNumFiles(numFiles);

      Resource[] resources = new Resource[numFiles];
      TaskProgressManager.setMaximum(numFiles);

      fm.seek(dirOffset);
      int readLength = 0;
      for (int i = 0; i < numFiles; i++) {
        // 56 - Filename
        String filename = fm.readNullString(56);
        FieldValidator.checkFilename(filename);

        // 4 - Data Offset
        long offset = fm.readInt();
        FieldValidator.checkOffset(offset, arcSize);

        // 4 - File Length
        long length = fm.readInt();
        FieldValidator.checkLength(length, arcSize);

        //path,id,name,offset,length,decompLength,exporter
        resources[i] = new Resource(path, filename, offset, length);

        TaskProgressManager.setValue(readLength);
        readLength += length;
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

      //Header-4
      fm.writeString("PACK");

      TaskProgressManager.setMessage(Language.get("Progress_PerformingCalculations"));
      int totalLengthOfData = 0;
      int totalLengthOfHeader = 12;
      for (int i = 0; i < numFiles; i++) {
        if (resources[i].getLength() >= 0) {
          totalLengthOfData += resources[i].getDecompressedLength();
        }
      }

      //DirOffset-4
      fm.writeInt(totalLengthOfData + totalLengthOfHeader);

      //DirLength-4
      fm.writeInt(numFiles * 64);

      TaskProgressManager.setMessage(Language.get("Progress_WritingFiles"));
      write(resources, fm);

      TaskProgressManager.setMessage(Language.get("Progress_WritingDirectory"));
      int currentPos = totalLengthOfHeader;
      for (int i = 0; i < numFiles; i++) {
        String name = resources[i].getName();
        long length = resources[i].getDecompressedLength();

        //FileName-56
        fm.writeNullString(name, 56);

        //Data Offset-4
        fm.writeInt(currentPos);

        //Length-4
        fm.writeInt((int) length);

        currentPos += length;
      }

      fm.close();

    }
    catch (Throwable t) {
      logError(t);
    }
  }

}