/*
 * Application:  Game Extractor
 * Author:       wattostudios
 * Website:      http://www.watto.org
 * Copyright:    Copyright (c) 2002-2023 wattostudios
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
import org.watto.datatype.FileType;
import org.watto.datatype.Resource;
import org.watto.ge.helper.FieldValidator;
import org.watto.ge.plugin.ArchivePlugin;
import org.watto.ge.plugin.ViewerPlugin;
import org.watto.io.FileManipulator;
import org.watto.io.converter.ShortConverter;
import org.watto.task.TaskProgressManager;

/**
**********************************************************************************************

**********************************************************************************************
**/
public class Plugin_WAD_19 extends ArchivePlugin {

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public Plugin_WAD_19() {

    super("WAD_19", "WAD_19");

    //         read write replace rename
    setProperties(true, false, false, false);

    setGames("MTV Music Generator 2");
    setExtensions("wad"); // MUST BE LOWER CASE
    setPlatforms("PS2");

    setFileTypes(new FileType("tim", "Playstation TIM Image", FileType.TYPE_IMAGE),
        new FileType("vag", "Playstation VAG Audio", FileType.TYPE_AUDIO));

    //setTextPreviewExtensions("colours", "rat", "screen", "styles"); // LOWER CASE

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

      try {
        getDirectoryFile(fm.getFile(), "IND");
        rating += 25;
      }
      catch (Throwable e) {
      }

      try {
        getDirectoryFile(fm.getFile(), "ind");
        rating += 25;
      }
      catch (Throwable e) {
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

      //ExporterPlugin exporter = Exporter_ZLib.getInstance();

      // RESETTING GLOBAL VARIABLES

      long arcSize = (int) path.length();

      File sourcePath = null;
      try {
        sourcePath = getDirectoryFile(path, "IND");
      }
      catch (Throwable t) {
        sourcePath = getDirectoryFile(path, "Ind");
      }

      FileManipulator fm = new FileManipulator(sourcePath, false);

      // 4 - Number Of Names
      int numNames = fm.readInt();
      FieldValidator.checkNumFiles(numNames / 4);

      // 4 - Number Of Files
      int numFiles = fm.readInt();
      FieldValidator.checkNumFiles(numFiles);

      Resource[] resources = new Resource[numFiles];
      TaskProgressManager.setMaximum(numFiles);

      // Loop through names directory
      String[] names = new String[numNames];
      String[] filenames = new String[numFiles];
      for (int i = 0; i < numNames; i++) {
        // 28 - Name (null terminated, filled with nulls)
        String filename = fm.readNullString(28);
        FieldValidator.checkFilename(filename);

        // 2 - Parent Directory ID (if the last bit is set, it means this is a directory entry, otherwise it is a file entry)
        int parentID = ShortConverter.unsign(fm.readShort());
        boolean dirEntry = false;
        if (parentID >= 32768) {
          parentID -= 32768;
          dirEntry = true;
        }

        parentID--;

        // 2 - Directory ID / File ID
        int fileID = fm.readShort() - 1; // indexes start at 1 for the file IDs

        //System.out.println(parentID + "\t" + fileID + "\t" + dirEntry + "\t" + filename);

        if (fileID == -1) {
          // end of directory - EMPTY entry
          continue;
        }

        if (dirEntry) {
          FieldValidator.checkRange(fileID, 0, numNames);

          filename += File.separatorChar;

          if (parentID == -1) {
            names[fileID] = filename;
          }
          else {
            names[fileID] = names[parentID] + filename;
          }
        }
        else {
          FieldValidator.checkRange(fileID, 0, numFiles - 1);

          filenames[fileID] = names[parentID] + filename;
        }
      }

      // Loop through files directory
      for (int i = 0; i < numFiles; i++) {

        // 4 - File Offset [*2048]
        long offset = fm.readInt() * 2048;
        FieldValidator.checkOffset(offset, arcSize);

        // 4 - File Length
        long length = fm.readInt();
        FieldValidator.checkLength(length, arcSize);

        String filename = filenames[i];

        //path,name,offset,length,decompLength,exporter
        resources[i] = new Resource(path, filename, offset, length);

        TaskProgressManager.setValue(i);
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
   Provide hints to the previewer so that certain document types are displayed appropriately
   **********************************************************************************************
   **/
  @Override
  public ViewerPlugin previewHint(Resource resource) {
    String extension = resource.getExtension();
    if (extension.equalsIgnoreCase("tim")) {
      return (ViewerPlugin) WSPluginManager.getPlugin("Viewer", "TIM");
    }
    return null;
  }

}
