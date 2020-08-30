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
import org.watto.component.WSPluginException;
import org.watto.component.WSPluginManager;
import org.watto.datatype.Archive;
import org.watto.datatype.Resource;
import org.watto.ge.helper.FieldValidator;
import org.watto.ge.plugin.ArchivePlugin;
import org.watto.ge.plugin.ViewerPlugin;
import org.watto.io.FileManipulator;
import org.watto.io.converter.StringConverter;
import org.watto.task.TaskProgressManager;

/**
**********************************************************************************************

**********************************************************************************************
**/
public class Plugin_REZ_REZMGR extends ArchivePlugin {

  int i = 0;

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public Plugin_REZ_REZMGR() {

    super("REZ_REZMGR", "Monolith Studios REZ");

    //         read write replace rename
    setProperties(true, false, false, false);

    setExtensions("rez");
    setGames("Alien Vs Predator 2",
        "Blood 2",
        "Contract JACK",
        "Die Hard Nakatomi Plaza",
        "Global Operations",
        "I, The Gangster",
        "KISS Psycho Circus: The Nightmare Child",
        "Marine Sharpshooter",
        "Marine Sharpshooter 2: Jungle Warfare",
        "Noone Lives Forever",
        "Noone Lives Forever 2",
        "Purge",
        "Sanity Aiken's Artifact",
        "Sentinel",
        "Shogo: Mobile Armor Division",
        "TerraWars: New York Invasion",
        "Tron 2.0",
        "World War 2 Normandy",
        "World War 2 Sniper - Call To Victory");
    setPlatforms("PC");

  }

  @SuppressWarnings("unused")
  public void analyseDirectory(FileManipulator fm, File path, Resource[] resources, long offset, long size, String directoryName) throws Exception {

    long currentPos = offset;
    long endPos = offset + size;
    int readLength = 0;
    while (currentPos < endPos) {
      fm.seek(currentPos);

      // 4 - EntryType
      int entryType = fm.readInt();

      // 4 - Data Offset
      offset = fm.readInt();

      // 4 - File Length
      long length = fm.readInt();

      // 4 - DateTime
      fm.skip(4);

      if (entryType == 1) {

        // X - filename (null)
        String dirFilename = fm.readNullString();
        FieldValidator.checkFilename(dirFilename);

        currentPos = (int) fm.getOffset();

        if (length > 0) {
          analyseDirectory(fm, path, resources, offset, length, directoryName + File.separator + dirFilename);
        }

      }
      else if (entryType == 0) {
        // 4 - ID number
        int fileID = fm.readInt();

        // 4 - Extension (reversed)
        String ext = StringConverter.reverse(fm.readNullString(4));

        // 4 - null
        fm.skip(4);

        // X - filename (null)
        String filename = directoryName + File.separator + fm.readNullString();

        currentPos = (int) fm.getOffset() + 1;

        if (!ext.equals("")) {
          filename += "." + ext;
        }

        if (length > 0) {
          //path,id,name,offset,length,decompLength,exporter
          resources[i] = new Resource(path, filename, offset, length);

          TaskProgressManager.setValue(readLength);
          readLength += length;
          i++;
        }
      }
      else {
        throw new WSPluginException("Invalid entry type");
      }

    }

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
      if (fm.readString(50).equals((char) 13 + (char) 10 + "RezMgr Version 1 Copyright (C) 1995 MONOLITH INC")) {
        rating += 50;
      }

      fm.skip(77);

      // Version (1)
      if (fm.readInt() == 1) {
        rating += 5;
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
    if (extension.equalsIgnoreCase("msc") || extension.equalsIgnoreCase("sc") || extension.equalsIgnoreCase("vsh") || extension.equalsIgnoreCase("fcf") || extension.equalsIgnoreCase("fxf") || extension.equalsIgnoreCase("scr") || extension.equalsIgnoreCase("uif")) {
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

      // RESETTING THE GLOBAL VARIABLES
      i = 0;

      FileManipulator fm = new FileManipulator(path, false);
      long arcSize = fm.getLength();

      // 127 - Header (13 10 'RezMgr Version 1 Copyright (C) 1995 MONOLITH INC.           ' 13 10 'LithTech Resource File                                      ' 13 10 26)
      //           | (13 10 'RezMgr Version 1 Copyright (C) 1995 MONOLITH INC.           ' 13 10 '                                                            ' 13 10 26)
      // 4 - Version (1)
      fm.skip(131);

      // 4 - dirOffset
      long dirOffset = fm.readInt();
      FieldValidator.checkOffset(dirOffset, arcSize);

      // 4 - dirLength
      int dirLength = fm.readInt();
      FieldValidator.checkLength(dirLength, arcSize);

      // 4 - Empty
      // 4 - IdxOffset
      // 4 - DateTime
      // 4 - Empty
      // 4 - LongestFoldernameLength
      // 4 - LongestFilenameLength
      fm.skip(24);

      int numFiles = Archive.getMaxFiles(4);//guess

      Resource[] resources = new Resource[numFiles];
      TaskProgressManager.setMaximum(arcSize);

      analyseDirectory(fm, path, resources, dirOffset, dirLength, "");
      resources = resizeResources(resources, i);

      fm.close();

      return resources;

    }
    catch (Throwable t) {
      logError(t);
      return null;
    }
  }

}