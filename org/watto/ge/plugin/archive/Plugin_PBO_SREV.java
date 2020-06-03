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
import org.watto.datatype.Archive;
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
public class Plugin_PBO_SREV extends ArchivePlugin {

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public Plugin_PBO_SREV() {

    super("PBO_SREV", "PBO_SREV");

    //         read write replace rename
    setProperties(true, false, false, false);

    setGames("ArmA",
        "ArmA 2",
        "Arma 3",
        "Argo");
    setExtensions("pbo");
    setPlatforms("PC");

    //setFileTypes("","",
    //             "",""
    //             );

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

      // 1 - null
      if (fm.readByte() == 0) {
        rating += 5;
      }

      // 4 - Header (sreV)
      if (fm.readString(4).equals("sreV")) {
        rating += 50;
      }

      // 16 - null
      if (fm.readLong() == 0) {
        rating += 5;
      }
      if (fm.readLong() == 0) {
        rating += 5;
      }

      // 6 - Prefix Descriptor (Prefix)
      if (fm.readString(6).equalsIgnoreCase("prefix")) {
        rating += 5;
      }

      // 1 - null
      if (fm.readByte() == 0) {
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
    if (extension.equalsIgnoreCase("inc") || extension.equalsIgnoreCase("sqf") || extension.equalsIgnoreCase("sqs") || extension.equalsIgnoreCase("fsm") || extension.equalsIgnoreCase("prj") || extension.equalsIgnoreCase("hpp") || extension.equalsIgnoreCase("bikb") || extension.equalsIgnoreCase("bisurf")) {
      return (ViewerPlugin) WSPluginManager.getPlugin("Viewer", "TXT");
    }
    return null;
  }

  /**
   **********************************************************************************************
   * Reads an [archive] File into the Resources
   **********************************************************************************************
   **/
  @Override
  public Resource[] read(File path) {
    try {

      // NOTE - Compressed files MUST know their DECOMPRESSED LENGTH
      //      - Uncompressed files MUST know their LENGTH

      addFileTypes();

      //ExporterPlugin exporter = Exporter_ZLib.getInstance();

      // RESETTING GLOBAL VARIABLES

      FileManipulator fm = new FileManipulator(path, false);

      long arcSize = fm.getLength();

      // 1 - null
      // 4 - Header (sreV)
      // 16 - null
      fm.skip(21);

      // X - Header (prefix/product/version)
      String header = fm.readNullString();
      while (header.length() > 0) {
        // X - Value
        fm.readNullString();

        // X - read the next Header
        header = fm.readNullString();
      }

      int numFiles = Archive.getMaxFiles();

      Resource[] resources = new Resource[numFiles];
      TaskProgressManager.setMaximum(numFiles);

      // Loop through directory
      int realNumFiles = 0;
      while (fm.getOffset() < arcSize) {
        // X - Filename
        // 1 - null Filename Terminator
        String filename = fm.readNullString();
        if (filename.length() == 0) {
          fm.skip(20);
          break; // end of directory
        }
        FieldValidator.checkFilename(filename);

        // 12 - null
        // 4 - Unknown
        fm.skip(16);

        // 4 - File Length
        long length = fm.readInt();
        FieldValidator.checkLength(length, arcSize);

        //path,name,offset,length,decompLength,exporter
        resources[realNumFiles] = new Resource(path, filename, 0, length);

        TaskProgressManager.setValue(realNumFiles);
        realNumFiles++;
      }

      // go back and set the offsets
      long offset = fm.getOffset();
      for (int i = 0; i < realNumFiles; i++) {
        Resource resource = resources[i];
        resource.setOffset(offset);
        offset += resource.getDecompressedLength();
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
