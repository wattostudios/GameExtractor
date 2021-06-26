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
import org.watto.ge.plugin.exporter.Exporter_GZip;
import org.watto.io.FileManipulator;
import org.watto.io.converter.IntConverter;
import org.watto.task.TaskProgressManager;

/**
**********************************************************************************************

**********************************************************************************************
**/
public class Plugin_PKG_FTYPODCF extends ArchivePlugin {

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public Plugin_PKG_FTYPODCF() {

    super("PKG_FTYPODCF", "PKG_FTYPODCF");

    //         read write replace rename
    setProperties(true, false, false, false);

    setGames("Snakes Subsonic");
    setExtensions("pkg"); // MUST BE LOWER CASE
    setPlatforms("PC");

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

      // 4 - Block Length (20) BIG ENDIAN
      if (FieldValidator.checkEquals(IntConverter.changeFormat(fm.readInt()), 20)) {
        rating += 5;
      }

      // 8 - Header (ftypodcf)
      if (fm.readString(8).equals("ftypodcf")) {
        rating += 50;
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
    if (extension.equalsIgnoreCase("gui") || extension.equalsIgnoreCase("str")) {
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

      ExporterPlugin exporter = Exporter_GZip.getInstance();

      // RESETTING GLOBAL VARIABLES

      FileManipulator fm = new FileManipulator(path, false);

      long arcSize = fm.getLength();

      //
      // FOR SIMPLICITY WITH ALL THE HEADER FIELDS, WE JUST...
      // READ THE FIRST 512 BYTES AND LOOK FOR THE HEADER "pkg" + 5 nulls
      //
      boolean found = false;
      while (fm.getOffset() < 512) {
        // 4 - Header ("pkg" + null)
        // 4 - null
        if (fm.readByte() == 112 && fm.readByte() == 107 && fm.readByte() == 103 && fm.readByte() == 0 && fm.readByte() == 0 && fm.readByte() == 0 && fm.readByte() == 0 && fm.readByte() == 0) {
          found = true;
          break;
        }
      }
      if (!found) {
        fm.close();
        return null;
      }

      long relativeOffset = fm.getOffset() - 8;

      // 4 - Number of Files
      int numFiles = fm.readInt();
      FieldValidator.checkNumFiles(numFiles);

      // 4 - Unknown (1)
      // 4 - Unknown
      // 4 - File Data Length
      // 4 - Unknown
      // 4 - Directory Length [- ~100]
      // 4 - Unknown (3)
      // 12 - null
      fm.skip(36);

      Resource[] resources = new Resource[numFiles];
      TaskProgressManager.setMaximum(numFiles);

      // Loop through directory
      for (int i = 0; i < numFiles; i++) {

        // 32 - Filename (null terminated)
        String filename = fm.readNullString(32);
        FieldValidator.checkFilename(filename);

        // 2 - File ID (incremental from 0)
        fm.skip(2);

        // 2 - Compression Flag (1=Compressed, 0=Uncompressed)
        short compression = fm.readShort();

        // 4 - Decompressed Length
        int decompLength = fm.readInt();
        FieldValidator.checkLength(decompLength);

        // 4 - Compressed Length
        int length = fm.readInt();
        FieldValidator.checkLength(length, arcSize);

        // 4 - File Offset (relative to the start of the FILES DIRECTORY) [+4]
        long offset = fm.readInt() + relativeOffset;
        FieldValidator.checkOffset(offset, arcSize);

        // 4 - null
        fm.skip(4);

        if (compression == 0) {
          //path,name,offset,length,decompLength,exporter
          resources[i] = new Resource(path, filename, offset, length);
        }
        else {
          //path,name,offset,length,decompLength,exporter
          resources[i] = new Resource(path, filename, offset, length, decompLength, exporter);
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
}
