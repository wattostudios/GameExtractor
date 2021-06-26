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
import org.watto.datatype.FileType;
import org.watto.datatype.Resource;
import org.watto.ge.helper.FieldValidator;
import org.watto.ge.plugin.ArchivePlugin;
import org.watto.ge.plugin.ExporterPlugin;
import org.watto.ge.plugin.ViewerPlugin;
import org.watto.ge.plugin.exporter.Exporter_ZLib;
import org.watto.io.FileManipulator;
import org.watto.task.TaskProgressManager;

/**
**********************************************************************************************

**********************************************************************************************
**/
public class Plugin_PKDWIN extends ArchivePlugin {

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public Plugin_PKDWIN() {

    super("PKDWIN", "PKDWIN");

    //         read write replace rename
    setProperties(true, false, false, false);

    setGames("Frozen Synapse Prime",
        "PixelJunk Shooter",
        "Super Cloudbuilt");
    setExtensions("pkdwin"); // MUST BE LOWER CASE
    setPlatforms("PC");

    setCanScanForFileTypes(true);

    setFileTypes(new FileType("script", "Script", FileType.TYPE_DOCUMENT));
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

      getDirectoryFile(fm.getFile(), "pkiwin");
      rating += 25;

      // 4 - Version (3)
      if (fm.readInt() == 3) {
        rating += 5;
      }

      fm.skip(4);

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
  If an archive doesn't have filenames stored in it, the scanner can come here to try to work out
  what kind of file a Resource is. This method allows the plugin to provide additional plugin-specific
  extensions, which will be tried before any standard extensions.
  @return null if no extension can be determined, or the extension if one can be found
  **********************************************************************************************
  **/
  @Override
  public String guessFileExtension(Resource resource, byte[] headerBytes, int headerInt1, int headerInt2, int headerInt3, short headerShort1, short headerShort2, short headerShort3, short headerShort4, short headerShort5, short headerShort6) {

    if (headerInt1 == 1480995140) {
      return "d1fx";
    }
    else if (headerInt1 == 774975073 || headerInt1 == 774975041 || headerInt1 == 805965105 || headerInt1 == 791621423) {
      return "script";
    }
    else if (headerInt1 == 1868983913) {
      return "font";
    }
    else if (headerInt1 == 1295069508) {
      return "d11mesh";
    }
    else if (headerInt1 == 1414285642) {
      return "jilt";
    }
    else if (headerInt1 == 305419896) {
      return "xv4";
    }
    else if (headerInt3 == 1111577667) {
      return "ctab";
    }

    if (headerShort1 == 30792) {
      return "hx";
    }
    else if (headerShort1 == 1026) {
      return "model";
    }
    else if (headerShort1 == 257) {
      return "anim";
    }
    else if (headerShort1 == 514) {
      return "editent";
    }
    else if (headerShort1 == 512) {
      return "tree";
    }

    if (headerBytes[0] == 91) {
      return "txt";
    }

    return null;
  }

  /**
   **********************************************************************************************
   Provide hints to the previewer so that certain document types are displayed appropriately
   **********************************************************************************************
   **/
  @Override
  public ViewerPlugin previewHint(Resource resource) {
    String extension = resource.getExtension();
    if (extension.equalsIgnoreCase("script") || extension.equalsIgnoreCase("font")) {
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

      ExporterPlugin exporter = Exporter_ZLib.getInstance();

      // RESETTING GLOBAL VARIABLES

      long arcSize = (int) path.length();

      File sourcePath = getDirectoryFile(path, "pkiwin");
      FileManipulator fm = new FileManipulator(sourcePath, false);

      // 4 - Version (3)
      // 4 - null
      fm.skip(8);

      // 4 - Number of Files
      int numFiles = fm.readInt();
      FieldValidator.checkNumFiles(numFiles);

      Resource[] resources = new Resource[numFiles];
      TaskProgressManager.setMaximum(numFiles);

      try {
        // FIRST TRY TO READ AS 16-BYTE ENTRIES (with compression - "PixelJunk Shooter" and "Super Cloudbuilt")

        // Loop through directory
        for (int i = 0; i < numFiles; i++) {

          // 4 - Decompressed File Length
          long decompLength = fm.readInt();
          FieldValidator.checkLength(decompLength);

          // 4 - Compressed File Length
          long length = fm.readInt();
          FieldValidator.checkLength(length, arcSize);

          // 4 - File Offset
          long offset = fm.readInt();
          FieldValidator.checkOffset(offset, arcSize);

          // 4 - Hash?
          fm.skip(4);

          String filename = Resource.generateFilename(i);

          //path,name,offset,length,decompLength,exporter
          if (length == 0) {
            // uncompressed
            resources[i] = new Resource(path, filename, offset, decompLength);
          }
          else {
            // compressed
            resources[i] = new Resource(path, filename, offset, length, decompLength, exporter);
          }

          TaskProgressManager.setValue(i);
        }

      }
      catch (Throwable t) {
        // IF THAT FAILS, TRY TO READ AS 12-BYTE ENTRIES (without compression - "Frozen Synapse Prime")
        fm.seek(12);

        // Loop through directory
        for (int i = 0; i < numFiles; i++) {

          // 4 - File Length
          long length = fm.readInt();
          FieldValidator.checkLength(length, arcSize);

          // 4 - File Offset
          long offset = fm.readInt();
          FieldValidator.checkOffset(offset, arcSize);

          // 4 - Hash?
          fm.skip(4);

          String filename = Resource.generateFilename(i);

          //path,name,offset,length,decompLength,exporter
          resources[i] = new Resource(path, filename, offset, length);

          TaskProgressManager.setValue(i);
        }

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
