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
import org.watto.datatype.Archive;
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
public class Plugin_WGA extends ArchivePlugin {

  int realNumFiles = 0;

  /**
  **********************************************************************************************

  **********************************************************************************************
  **/
  public Plugin_WGA() {

    super("WGA", "WGA");

    //         read write replace rename
    setProperties(true, false, false, false);
    setCanImplicitReplace(true);

    setGames("Pilot Down: Behind Enemy Lines",
        "Prisoner Of War");
    setExtensions("wga");
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

      long arcSize = fm.getLength();

      // Directory Length
      if (FieldValidator.checkLength(fm.readInt(), arcSize)) {
        rating += 5;
      }

      // Version
      if (fm.readInt() == 1) {
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
   * Reads an [archive] File into the Resources
   **********************************************************************************************
   **/
  @Override
  public Resource[] read(File path) {
    try {

      // NOTE - Compressed files MUST know their DECOMPRESSED LENGTH
      //      - Uncompressed files MUST know their LENGTH

      addFileTypes();

      // RESETTING THE GLOBAL VARIABLES
      realNumFiles = 0;

      FileManipulator fm = new FileManipulator(path, false);

      long arcSize = (int) fm.getLength();

      // 4 - Directory Length?
      // 4 - Version (1)?
      fm.skip(8);

      int numFiles = Archive.getMaxFiles();

      Resource[] resources = new Resource[numFiles];

      TaskProgressManager.setMaximum(arcSize);

      readDirectory(fm, path, resources, "");

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
  public void readDirectory(FileManipulator fm, File path, Resource[] resources, String dirName) throws Exception {

    long arcSize = fm.getLength();

    // 4 - Directory Name Length
    //System.out.println(fm.getOffset());
    int dirNameLength = fm.readInt();
    FieldValidator.checkFilenameLength(dirNameLength + 1);

    // X - Directory Name
    dirName += fm.readString(dirNameLength) + "\\";
    //System.out.println(dirName);

    // 4 - Number Of Files In This Directory
    int numFiles = fm.readInt();
    FieldValidator.checkNumFiles(numFiles + 1);

    // Loop through directory
    for (int i = 0; i < numFiles; i++) {
      // 4 - Filename Length
      int filenameLength = fm.readInt();
      FieldValidator.checkFilenameLength(filenameLength);

      // X - Filename
      String filename = dirName + fm.readString(filenameLength);
      //System.out.println(filename);

      // 4 - File Offset?
      long offsetPointerLocation = fm.getOffset();
      long offsetPointerLength = 4;

      long offset = fm.readInt();
      FieldValidator.checkOffset(offset, arcSize);

      // 4 - File Length?
      long lengthPointerLocation = fm.getOffset();
      long lengthPointerLength = 4;

      long length = fm.readInt();
      FieldValidator.checkLength(length, arcSize);

      //path,id,name,offset,length,decompLength,exporter
      resources[realNumFiles] = new ReplacableResource(path, filename, offset, offsetPointerLocation, offsetPointerLength, length, lengthPointerLocation, lengthPointerLength);

      TaskProgressManager.setValue(offset);
      realNumFiles++;
    }

    // 4 - Number Of Sub-Directory in this directory
    int numDirs = fm.readInt();
    FieldValidator.checkNumFiles(numDirs + 1);

    // Loop through directory
    for (int i = 0; i < numDirs; i++) {
      readDirectory(fm, path, resources, dirName);
    }

  }

}
