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
import org.watto.datatype.Resource;
import org.watto.ge.helper.FieldValidator;
import org.watto.ge.plugin.ArchivePlugin;
import org.watto.ge.plugin.ExporterPlugin;
import org.watto.ge.plugin.ViewerPlugin;
import org.watto.ge.plugin.exporter.Exporter_LZ77WII;
import org.watto.io.FileManipulator;
import org.watto.io.FilenameSplitter;
import org.watto.task.TaskProgressManager;

/**
**********************************************************************************************

**********************************************************************************************
**/
public class Plugin_FUN extends ArchivePlugin {

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public Plugin_FUN() {

    super("FUN", "FUN");

    //         read write replace rename
    setProperties(true, false, false, false);

    setGames("Club Penguin: Elite Penguin Force",
        "Club Penguin: Herberts Revenge");
    setExtensions("fun"); // MUST BE LOWER CASE
    setPlatforms("Nintendo DS");

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

      long arcSize = fm.getLength();

      // 4 - Filename Directory Offset
      if (FieldValidator.checkOffset(fm.readInt(), arcSize)) {
        rating += 5;
      }

      // 4 - Filename Directory Length (not including padding)
      if (FieldValidator.checkLength(fm.readInt(), arcSize)) {
        rating += 5;
      }

      // 4 - Details Directory Offset
      if (FieldValidator.checkOffset(fm.readInt(), arcSize)) {
        rating += 5;
      }

      // 4 - Details Directroy Length (not including padding)
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
   * Reads an [archive] File into the Resources
   **********************************************************************************************
   **/
  @Override
  public Resource[] read(File path) {
    try {

      // NOTE - Compressed files MUST know their DECOMPRESSED LENGTH
      //      - Uncompressed files MUST know their LENGTH

      addFileTypes();

      ExporterPlugin exporter = Exporter_LZ77WII.getInstance();

      // RESETTING GLOBAL VARIABLES

      FileManipulator fm = new FileManipulator(path, false);

      long arcSize = fm.getLength();

      // 4 - Filename Directory Offset
      int filenameDirOffset = fm.readInt();
      FieldValidator.checkOffset(filenameDirOffset, arcSize);

      // 4 - Filename Directory Length (not including padding)
      fm.skip(4);

      // 4 - Details Directory Offset
      int dirOffset = fm.readInt();
      FieldValidator.checkOffset(dirOffset, arcSize);

      // 4 - Details Directory Length (not including padding)
      int numFiles = fm.readInt() / 8;
      FieldValidator.checkNumFiles(numFiles);

      // 496 - Padding (byte 255)
      fm.seek(filenameDirOffset);

      Resource[] resources = new Resource[numFiles];
      TaskProgressManager.setMaximum(numFiles);

      // 4 - Heading Block Length 1 (24)
      int headerLength = fm.readInt() - 4;
      FieldValidator.checkLength(headerLength, arcSize);

      // 2 - Unknown
      // 2 - Unknown
      // 4 - Heading Block Length 2 (34)
      // 2 - Unknown
      // 2 - Unknown
      // 4 - Version String ("v4" + 2x nulls)
      // 4 - Unknown
      fm.skip(headerLength);

      String dirName = "";

      String[] names = new String[numFiles];

      // Loop through filename directory
      for (int i = 0; i < numFiles; i++) {
        // 1 - Filename Length
        int filenameLength = fm.readByte();

        if (filenameLength < 0) {
          // a directory name
          filenameLength &= 127;
          dirName = fm.readString(filenameLength) + "\\";

          // 3 - Unknown
          fm.skip(3);

          i--; // don't want to include this name as one of the filenames, this moves the 
              // pointer back one ready for the next iteration of the loop
          continue;
        }

        // X - Filename
        String filename = dirName + fm.readString(filenameLength);

        names[i] = filename;

        TaskProgressManager.setValue(i);
      }

      fm.seek(dirOffset);

      // Loop through directory
      for (int i = 0; i < numFiles; i++) {

        // 4 - File Offset
        int offset = fm.readInt();
        FieldValidator.checkOffset(offset, arcSize);

        // 4 - File End Offset
        int length = fm.readInt() - offset;
        FieldValidator.checkLength(length, arcSize);

        String filename = names[i];

        //path,name,offset,length,decompLength,exporter

        if (FilenameSplitter.getExtension(filename).equals("lzc")) {
          // Compressed
          filename = filename.substring(0, filename.length() - 4); // remove the .lzc from the end of the filename
          resources[i] = new Resource(path, filename, offset, length, length, exporter);
        }
        else {
          // Uncompressed
          resources[i] = new Resource(path, filename, offset, length);
        }

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
    if (extension.equalsIgnoreCase("info") || extension.equalsIgnoreCase("19") || extension.equalsIgnoreCase("gsc")) {
      return (ViewerPlugin) WSPluginManager.getPlugin("Viewer", "TXT");
    }
    return null;
  }

}
