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
public class Plugin_DAT_105 extends ArchivePlugin {

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public Plugin_DAT_105() {

    super("DAT_105", "DAT_105");

    //         read write replace rename
    setProperties(true, false, false, false);

    setGames("Eternal Poison");
    setExtensions("dat"); // MUST BE LOWER CASE
    setPlatforms("PS2");

    setFileTypes(new FileType("tm2", "Playstation TIM Image", FileType.TYPE_IMAGE));

    setTextPreviewExtensions("h"); // LOWER CASE

    //setCanScanForFileTypes(true);

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

      getDirectoryFile(fm.getFile(), "hed");
      rating += 25;

      return rating;

    }
    catch (Throwable t) {
      return 0;
    }
  }

  int[] firstValues = null;
  int[] secondValues = null;
  String[] names = null;
  int[] flags = null;

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

      long arcSize = (int) path.length();

      File sourcePath = getDirectoryFile(path, "hed");
      FileManipulator fm = new FileManipulator(sourcePath, false);

      int numFiles = (int) (sourcePath.length() / 44);
      FieldValidator.checkNumFiles(numFiles);

      Resource[] resources = new Resource[numFiles];
      TaskProgressManager.setMaximum(numFiles);

      // Loop through directory
      int realNumFiles = 0;

      firstValues = new int[numFiles];
      secondValues = new int[numFiles];
      names = new String[numFiles];
      flags = new int[numFiles];

      // First, read in all the content
      for (int i = 0; i < numFiles; i++) {
        // 4 - File Offset || ID of the First Entry in this sub-directory
        int firstValue = fm.readInt();
        firstValues[i] = firstValue;

        // 4 - File Length || Number of Entries in this sub-directory (including the DirEnd entry)
        int secondValue = fm.readInt();
        secondValues[i] = secondValue;

        // 32 - Name (null terminated, filled with nulls)
        String filename = fm.readNullString(32);
        FieldValidator.checkFilename(filename);
        names[i] = filename;

        // 4 - Flags
        int flag = fm.readInt();
        flags[i] = flag;
      }

      // Second, iterate through the directories and set the directory names
      // read the root directory
      int firstID = firstValues[0];
      int numEntries = secondValues[0];

      processDirectory(firstID, numEntries, names[0] + "\\");

      // Third, go through, find the files, and add them as real files
      for (int i = 0; i < numFiles; i++) {
        int firstValue = firstValues[i];
        int secondValue = secondValues[i];
        String name = names[i];
        int flag = flags[i];

        if (flag != 0) {
          if (name.equals("DI")) {
            // ignore
          }
          else {
            // a real file
            int offset = firstValue;
            FieldValidator.checkOffset(offset, arcSize);

            int length = secondValue;
            FieldValidator.checkLength(length, arcSize);

            String filename = name;

            //path,name,offset,length,decompLength,exporter
            resources[realNumFiles] = new Resource(path, filename, offset, length);
            realNumFiles++;

            TaskProgressManager.setValue(i);
          }
        }
      }

      resources = resizeResources(resources, realNumFiles);

      fm.close();

      return resources;

    }
    catch (

    Throwable t) {
      logError(t);
      return null;
    }
  }

  /**
   **********************************************************************************************
   * Adds the directory names to the files
   **********************************************************************************************
   **/
  public void processDirectory(int firstID, int numEntries, String dirName) {
    int lastID = firstID + numEntries;
    for (int i = firstID; i < lastID; i++) {
      int firstValue = firstValues[i];
      int secondValue = secondValues[i];
      String name = names[i];
      int flag = flags[i];

      if (flag == 0) {
        if (name.equals("..")) {
          // ignore
        }
        else if (name.equals("--DirEnd--")) {
          // ignore
        }
        else {
          // a sub-directory
          name = dirName + name + "\\";
          processDirectory(firstValue, secondValue, name);
        }
      }
      else {
        if (name.equals("DI")) {
          // ignore
        }
        else {
          // a file
          name = dirName + name;
          names[i] = name;
        }
      }
    }
  }

  /**
  **********************************************************************************************
  If an archive doesn't have filenames stored in it, the scanner can come here to try to work out
  what kind of file a Resource is. This method allows the plugin to provide additional plugin-specific
  extensions, which will be tried before any standard extensions.
  @return null if no extension can be determined, or the extension if one can be found
  **********************************************************************************************
  **/
  @Override
  public String guessFileExtension(Resource resource, byte[] headerBytes, int headerInt1, int headerInt2, int headerInt3, short headerShort1, short headerShort2, short headerShort3, short headerShort4, short headerShort5, short headerShort6) {

    /*
    if (headerInt1 == 2037149520) {
      return "js";
    }
    */

    return null;
  }

}
