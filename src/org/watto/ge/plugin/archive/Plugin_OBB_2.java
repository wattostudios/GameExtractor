/*
 * Application:  Game Extractor
 * Author:       wattostudios
 * Website:      http://www.watto.org
 * Copyright:    Copyright (c) 2002-2025 wattostudios
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
import org.watto.datatype.FileType;
import org.watto.datatype.Resource;
import org.watto.ge.helper.FieldValidator;
import org.watto.ge.plugin.ArchivePlugin;
import org.watto.io.FileManipulator;
import org.watto.task.Task_ScanArchive;

/**
**********************************************************************************************

**********************************************************************************************
**/
public class Plugin_OBB_2 extends ArchivePlugin {

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public Plugin_OBB_2() {

    super("OBB_2", "OBB_2");

    //
    //
    // DISABLED (replaced by OBB_3 with proper handling of this archive, but kept here as a reference for scanner-based plugins)
    //
    //
    setEnabled(false);

    //         read write replace rename
    setProperties(true, false, false, false);

    setGames("Littlest Pet Shop");
    setExtensions("obb"); // MUST BE LOWER CASE
    setPlatforms("Android");

    // MUST BE LOWER CASE !!!
    setFileTypes(new FileType("pvr", "PVR Image", FileType.TYPE_IMAGE),
        new FileType("rk", "RK Mesh", FileType.TYPE_MODEL));

    setTextPreviewExtensions("event"); // LOWER CASE

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

      long arcSize = fm.getLength();

      // Archive Size
      if (FieldValidator.checkLength(fm.readInt(), arcSize)) {
        rating += 5;
      }

      if (fm.readInt() == 1) {
        rating += 5;
      }

      if (fm.readByte() == 120) {
        rating += 5;
      }

      // THIS IS A WRAPPER AROUND A SCANNER, SO WE REALLY NEED THIS TO BE TRIED AFTER EVERYTHING ELSE!
      if (rating == 45) {
        rating = 25;
      }
      else {
        rating = 0; // don't allow anything other than perfect match!
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

      // RUN THE SCANNER
      new Task_ScanArchive(path, true).redo();

      // RETURN THE FILES THAT WERE FOUND
      return Archive.getResources();

      // We also have setCanScanForFileTypes(true), so as long as we return the files, they will trigger the scan afterwards.

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

    if (headerInt1 == 55727696) {
      return "pvr";
    }
    else if (headerInt1 == 1330006866 && headerInt2 == 1413565778) {
      return "rk";
    }
    else if (headerInt1 == 1702249788) {
      return "event";
    }
    else if (headerInt1 == 54938946) {
      return "bmf";
    }
    else if (headerInt1 == 1094863182) {
      return "nib";
    }
    else if (headerInt1 == 1316515670) {
      return "vox";
    }
    else if (headerInt1 == 1329865020) {
      return "html";
    }
    else if (headerShort1 == 12079 || headerShort1 == 3451 || headerInt1 == 1835091772) {
      return "txt"; // a script of some type
    }
    else if (headerShort1 == 19280) {
      return "zip";
    }
    else {
      int length = (int) resource.getLength(); // they all seem to compress to the same basic size!
      if (length == 512 || length == 1024 || length == 1536 || length == 2048) {
        return "txt"; // a script of some type
      }
    }

    return null;
  }

}
