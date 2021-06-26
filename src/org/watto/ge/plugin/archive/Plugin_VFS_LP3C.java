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
import org.watto.io.converter.ByteConverter;
import org.watto.task.TaskProgressManager;

/**
**********************************************************************************************

**********************************************************************************************
**/
public class Plugin_VFS_LP3C extends ArchivePlugin {

  Resource[] resources = null;

  int realNumFiles = 0;

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public Plugin_VFS_LP3C() {

    super("VFS_LP3C", "VFS_LP3C");

    //         read write replace rename
    setProperties(true, false, false, false);

    setGames("Cargo: The Quest for Gravity");
    setExtensions("vfs"); // MUST BE LOWER CASE
    setPlatforms("PC");

    // MUST BE LOWER CASE !!!
    //setFileTypes(new FileType("txt", "Text Document", FileType.TYPE_DOCUMENT),
    //             new FileType("bmp", "Bitmap Image", FileType.TYPE_IMAGE)
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

      // 4 - Header (LP3C)
      if (fm.readString(4).equals("LP3C")) {
        rating += 50;
      }

      // 4 - null
      if (fm.readInt() == 0) {
        rating += 5;
      }

      // 4 - Number of Files?
      if (FieldValidator.checkNumFiles(fm.readInt() + 1)) {
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
    if (extension.equalsIgnoreCase("prc") || extension.equalsIgnoreCase("ics") || extension.equalsIgnoreCase("psh") || extension.equalsIgnoreCase("pss") || extension.equalsIgnoreCase("vsh") || extension.equalsIgnoreCase("mus") || extension.equalsIgnoreCase("rd")) {
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

      // 4 - Header (LP3C)
      fm.skip(4);

      int numFiles = Archive.getMaxFiles();
      realNumFiles = 0;

      resources = new Resource[numFiles];
      TaskProgressManager.setMaximum(arcSize);

      readDirectory(path, fm, "");

      resources = resizeResources(resources, realNumFiles);

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

  public void readDirectory(File path, FileManipulator fm, String dirName) {
    try {

      long arcSize = fm.getLength();

      // 4 - Number of Directories
      int numDirsInDir = fm.readInt();
      FieldValidator.checkNumFiles(numDirsInDir + 1); // +1 to allow "0" directories

      // 4 - Number Of Files
      int numFilesInDir = fm.readInt();
      FieldValidator.checkNumFiles(numFilesInDir + 1); // +1 to allow "0" files in this directory

      // Loop through directory
      for (int i = 0; i < numFilesInDir; i++) {
        // 1 - Filename Length
        int filenameLength = ByteConverter.unsign(fm.readByte());

        // X - Filename
        String filename = dirName + fm.readString(filenameLength);

        // 4 - File Offset
        int offset = fm.readInt();
        FieldValidator.checkOffset(offset, arcSize);

        // 4 - null
        fm.skip(4);

        // 4 - File Length
        int length = fm.readInt();
        FieldValidator.checkLength(length, arcSize);

        //path,name,offset,length,decompLength,exporter
        resources[realNumFiles] = new Resource(path, filename, offset, length);

        TaskProgressManager.setValue(offset);
        realNumFiles++;
      }

      // read each directory
      for (int d = 0; d < numDirsInDir; d++) {
        // 1 - Directory Name Length
        int dirNameLength = ByteConverter.unsign(fm.readByte());

        // X - Directory Name
        String thisDirName = fm.readString(dirNameLength);

        readDirectory(path, fm, dirName + thisDirName + "\\");
      }

    }
    catch (Throwable t) {
      logError(t);
    }
  }

}
