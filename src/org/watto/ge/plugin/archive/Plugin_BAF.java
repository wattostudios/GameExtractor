/*
 * Application:  Game Extractor
 * Author:       wattostudios
 * Website:      http://www.watto.org
 * Copyright:    Copyright (c) 2002-2021 wattostudios
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
import org.watto.datatype.Resource;
import org.watto.ge.helper.FieldValidator;
import org.watto.ge.plugin.ArchivePlugin;
import org.watto.io.FileManipulator;
import org.watto.task.TaskProgressManager;

/**
**********************************************************************************************

**********************************************************************************************
**/
public class Plugin_BAF extends ArchivePlugin {

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public Plugin_BAF() {

    super("BAF", "BAF");

    //         read write replace rename
    setProperties(true, false, false, false);

    setGames("Goblin Commander: Unleash the Horde");
    setExtensions("baf"); // MUST BE LOWER CASE
    setPlatforms("XBox");

    // MUST BE LOWER CASE !!!
    //setFileTypes(new FileType("txt", "Text Document", FileType.TYPE_DOCUMENT),
    //             new FileType("bmp", "Bitmap Image", FileType.TYPE_IMAGE)
    //             );

    setTextPreviewExtensions("script", "material", "arb", "vp", "xvs", "prop", "xbox_shader", "ma", "nvparse", "make"); // LOWER CASE

    setCanScanForFileTypes(true);

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

      // 4 - Number Of Files
      int numFiles = fm.readInt();
      FieldValidator.checkNumFiles(numFiles);

      Resource[] resources = new Resource[numFiles];
      TaskProgressManager.setMaximum(numFiles);

      // Loop through directory
      for (int i = 0; i < numFiles; i++) {

        // 12 - Unknown
        fm.skip(12);

        // 4 - File Length
        int length = fm.readInt();
        FieldValidator.checkLength(length, arcSize);

        // 4 - File Offset
        int offset = fm.readInt();
        FieldValidator.checkOffset(offset, arcSize);

        String filename = Resource.generateFilename(i);

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
  If an archive doesn't have filenames stored in it, the scanner can come here to try to work out
  what kind of file a Resource is. This method allows the plugin to provide additional plugin-specific
  extensions, which will be tried before any standard extensions.
  @return null if no extension can be determined, or the extension if one can be found
  **********************************************************************************************
  **/
  @Override
  public String guessFileExtension(Resource resource, byte[] headerBytes, int headerInt1, int headerInt2, int headerInt3, short headerShort1, short headerShort2, short headerShort3, short headerShort4, short headerShort5, short headerShort6) {

    if (headerInt3 == 1480864580 || headerInt1 == 1344286511 || headerInt1 == 168636207) {
      return "shader";
    }
    else if (headerInt1 == 1344289595) {
      return "prop";
    }
    else if (headerInt1 == 1129455905 || headerInt1 == 1347241300 || headerInt1 == 1702043707 || headerInt1 == 1852727619 || headerInt1 == 543449410 || headerInt1 == 589505315 || headerInt1 == 791621423) {
      return "script";
    }
    else if (headerInt1 == 1347821857) {
      return "vp";
    }
    else if (headerInt1 == 1380000033) {
      return "arb";
    }
    else if (headerInt1 == 1478504239) {
      return "xbox_shader";
    }
    else if (headerInt1 == 1632438797 || headerInt1 == 1702125901) {
      return "material";
    }
    else if (headerInt1 == 1632448303) {
      return "ma"; // MAYA
    }
    else if (headerInt1 == 827148120) {
      return "xgm1";
    }
    else if (headerInt1 == 877809478) {
      return "for4";
    }
    else if (headerInt1 == 9460301) {
      return "exe";
    }
    else if (headerInt1 == 1632444451) {
      return "make";
    }
    else if (headerInt1 == 1847602991) {
      return "nvparse";
    }
    else if (headerInt1 == 538976288) {
      return "xvs";
    }

    return null;
  }

}
