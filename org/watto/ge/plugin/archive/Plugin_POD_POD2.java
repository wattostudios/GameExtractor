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
import org.watto.component.WSPluginManager;
import org.watto.datatype.ReplacableResource;
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
public class Plugin_POD_POD2 extends ArchivePlugin {

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public Plugin_POD_POD2() {

    super("POD_POD2", "POD_POD2");

    //         read write replace rename
    setProperties(true, false, false, false);
    setCanImplicitReplace(true);

    setExtensions("pod");
    setGames("4x4 Evolution",
        "4x4 Evolution 2",
        "Blair Witch Project",
        "Nocturne");
    setPlatforms("PC");

    setFileTypes("lst", "File List",
        "opa", "Encrypted Image?",
        "act", "Color Palette",
        "smf", "3D Model",
        "rpl", "Replay File",
        "tex", "Texture Listing",
        "clr", "Clear Image?",
        "rtd", "Unknown File",
        "sdw", "Unknown File",
        "lvl", "Level Descriptor",
        "sit", "Race Settings",
        "trk", "Truck Information",
        "bin", "Binary Data",
        "lte", "Unknown File",
        "map", "Map File",
        "pbm", "PBM Image",
        "sfx", "Sound Settings",
        "loc", "Text Description",
        "vox", "Configuration List",
        "rsp", "File List");

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
      if (fm.readString(4).equals("POD2")) {
        rating += 50;
      }

      fm.skip(84);

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
   Provide hints to the previewer so that certain document types are displayed appropriately
   **********************************************************************************************
   **/
  @Override
  public ViewerPlugin previewHint(Resource resource) {
    String extension = resource.getExtension();
    if (extension.equalsIgnoreCase("lvl") || extension.equalsIgnoreCase("sit") || extension.equalsIgnoreCase("smf") || extension.equalsIgnoreCase("tex") || extension.equalsIgnoreCase("lst")) {
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

      // 4 - Header
      // 4 - CRC Checksum?
      // 80 - Archive Name
      fm.skip(88);

      // 4 - Number Of Files
      int numFiles = fm.readInt();
      FieldValidator.checkNumFiles(numFiles);

      long arcSize = fm.getLength();

      Resource[] resources = new Resource[numFiles];
      TaskProgressManager.setMaximum(numFiles);

      // 4 - Audit Entry File Count
      fm.skip(4);

      long offsetToNameList = (numFiles * 20) + 96;

      int readLength = 0;
      for (int i = 0; i < numFiles; i++) {
        // 4 - Filename Offset
        long filenameOffset = offsetToNameList + fm.readInt();
        FieldValidator.checkOffset(filenameOffset, arcSize);

        int position = (int) fm.getOffset();
        fm.seek(filenameOffset);

        String filename = fm.readNullString();

        fm.seek(position);

        // 4 - File Length
        long lengthPointerLocation = fm.getOffset();
        long lengthPointerLength = 4;

        long length = fm.readInt();
        FieldValidator.checkLength(length, arcSize);

        // 4 - Data Offset
        long offsetPointerLocation = fm.getOffset();
        long offsetPointerLength = 4;

        long offset = fm.readInt();
        FieldValidator.checkOffset(offset, arcSize);

        // 4 - Group ID
        fm.skip(8);

        // 4 - Unknown

        //path,id,name,offset,length,decompLength,exporter
        resources[i] = new ReplacableResource(path, filename, offset, offsetPointerLocation, offsetPointerLength, length, lengthPointerLocation, lengthPointerLength);

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

}