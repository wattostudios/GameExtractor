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
public class Plugin_PKG_PKGFILE extends ArchivePlugin {

  /**
  **********************************************************************************************

  **********************************************************************************************
  **/
  public Plugin_PKG_PKGFILE() {

    super("PKG_PKGFILE", "PKG_PKGFILE");

    //         read write replace rename
    setProperties(true, false, false, false);

    setGames("Order of War");
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

      // 42 - Header (Unicode String "PKG_FILE_VERSION:0004")
      if (fm.readUnicodeString(42).equals("PKG_FILE_VERSION:0004")) {
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
    if (extension.equalsIgnoreCase("bat") || extension.equalsIgnoreCase("lua") || extension.equalsIgnoreCase("fx") || extension.equalsIgnoreCase("inc") || extension.equalsIgnoreCase("msd") || extension.equalsIgnoreCase("sdr") || extension.equalsIgnoreCase("xyz")) {
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

      // skip to the end and read the footer
      fm.seek(arcSize - 8);

      // 4 - Directory Length
      int dirLength = fm.readInt() - 8;
      FieldValidator.checkLength(dirLength, arcSize);

      // 4 - Directory Offset
      int dirOffset = fm.readInt();
      FieldValidator.checkOffset(dirOffset, arcSize);

      // skip to the directory
      fm.seek(dirOffset);

      int numFiles = Archive.getMaxFiles();

      Resource[] resources = new Resource[numFiles];
      TaskProgressManager.setMaximum(dirLength);

      // Loop through directory
      int realNumFiles = 0;
      int dirPos = 0;
      while (dirPos < dirLength) {

        // 4 - Entry Length
        int entryLength = fm.readInt();
        FieldValidator.checkLength(entryLength, arcSize);

        int filenameLength = (entryLength - 16) / 2;
        FieldValidator.checkFilenameLength(filenameLength);

        // 4 - File Offset
        int offset = fm.readInt();

        // 4 - File Length
        int length = fm.readInt();

        if (offset == -1 && length == -1) {
          // empty file/directory/something
          fm.skip(entryLength - 12);

          dirPos += entryLength;
          continue;
        }
        FieldValidator.checkOffset(offset, arcSize);
        FieldValidator.checkLength(length, arcSize);

        // 4 - Hash?
        fm.skip(4);

        // X - Filename (Unicode String --> length is EntryLength - 16)
        String filename = fm.readUnicodeString(filenameLength);
        FieldValidator.checkFilename(filename);

        //path,name,offset,length,decompLength,exporter
        resources[realNumFiles] = new Resource(path, filename, offset, length);
        realNumFiles++;

        TaskProgressManager.setValue(dirPos);
        dirPos += entryLength;
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
