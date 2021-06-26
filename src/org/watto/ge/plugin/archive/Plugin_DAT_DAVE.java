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
import org.watto.datatype.FileType;
import org.watto.datatype.Resource;
import org.watto.ge.helper.FieldValidator;
import org.watto.ge.plugin.ArchivePlugin;
import org.watto.ge.plugin.ExporterPlugin;
import org.watto.ge.plugin.exporter.Exporter_Deflate;
import org.watto.io.FileManipulator;
import org.watto.task.TaskProgressManager;

/**
**********************************************************************************************

**********************************************************************************************
**/
public class Plugin_DAT_DAVE extends ArchivePlugin {

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public Plugin_DAT_DAVE() {

    super("DAT_DAVE", "DAT_DAVE");

    //         read write replace rename
    setProperties(true, false, false, false);

    setGames("Midnight Club 2",
        "Smugglers Run",
        "Midnight Club",
        "Red Dead Revolver");
    setExtensions("dat");
    setPlatforms("PC", "XBox");

    setFileTypes(new FileType("tex", "Texture Image", FileType.TYPE_IMAGE));

    setTextPreviewExtensions("script"); // LOWER CASE

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

      // Header
      String header = fm.readString(4);
      if (header.equals("DAVE") || header.equals("Dave")) {
        rating += 50;
      }

      // Number Of Files
      if (FieldValidator.checkNumFiles(fm.readInt())) {
        rating += 5;
      }

      long arcSize = fm.getLength();

      // Filename Directory Offset
      if (FieldValidator.checkOffset(fm.readInt(), arcSize)) {
        rating += 5;
      }

      // First File Offset
      if (FieldValidator.checkOffset(fm.readInt(), arcSize)) {
        rating += 5;
      }

      // null
      if (fm.readLong() == 0) {
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

      ExporterPlugin exporter = Exporter_Deflate.getInstance();

      addFileTypes();

      // RESETTING THE GLOBAL VARIABLES

      FileManipulator fm = new FileManipulator(path, false);

      long arcSize = (int) fm.getLength();

      // 4 - Header ("Dave" or "DAVE")
      String header = fm.readString(4);

      // 4 - Number Of Files?
      int numFiles = fm.readInt();
      FieldValidator.checkNumFiles(numFiles);

      // 4 - Filename Directory Offset [+2048] (ie relative to the start of the Directory)
      int filenameDirOffset = fm.readInt() + 2048;
      FieldValidator.checkOffset(filenameDirOffset, arcSize);

      // 4 - First File Data Offset [+2048+FilenameDirOffset] (ie relative to the start of the FilenameDir)
      // 2032 - null Padding to offset 2048
      fm.seek(2048);

      Resource[] resources = new Resource[numFiles];

      TaskProgressManager.setMaximum(numFiles);

      // Loop through directory
      int[] nameOffsets = new int[numFiles];
      for (int i = 0; i < numFiles; i++) {
        // 4 - Filename Offset (relative to the start of the filename directory)
        int filenameOffset = fm.readInt() + filenameDirOffset;
        FieldValidator.checkOffset(filenameOffset, arcSize);
        nameOffsets[i] = filenameOffset;

        // 4 - File Offset
        long offset = fm.readInt();
        FieldValidator.checkOffset(offset, arcSize);

        // 4 - Decompressed File Length
        int decompLength = fm.readInt();
        FieldValidator.checkLength(decompLength);

        // 4 - Compressed File Length
        long length = fm.readInt();
        FieldValidator.checkLength(length, arcSize);

        String filename = Resource.generateFilename(i);

        //path,id,name,offset,length,decompLength,exporter
        Resource resource = new Resource(path, filename, offset, length, decompLength);
        if (length != decompLength) {
          resource.setExporter(exporter);
        }
        resources[i] = resource;

        TaskProgressManager.setValue(i);
      }

      if (header.equals("DAVE")) {
        // non-encrypted filenames
        for (int i = 0; i < numFiles; i++) {
          fm.seek(nameOffsets[i]);

          // X - Filename
          // 1 - null Filename Terminator
          String filename = fm.readNullString();
          FieldValidator.checkFilename(filename);

          Resource resource = resources[i];
          resource.setName(filename);
          resource.setOriginalName(filename);
        }
        setCanScanForFileTypes(false);
      }
      else {
        setCanScanForFileTypes(true);
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

    if (headerInt1 == 809584979) {
      return "sia";
    }
    else if (headerShort1 == 30030) {
      return "num";
    }
    else if (headerInt1 == 1113878099) {
      return "snd";
    }
    else if (headerInt1 == 1396917577) {
      return "iecs";
    }
    else if (headerInt1 == 1145591873) {
      return "axhd";
    }
    else if (headerInt1 == 1180648532) {
      return "td";
    }
    else if (headerInt2 == 1701667171) {
      return "cam";
    }
    else if (headerInt1 == 1766541634) {
      return "bik";
    }
    else if (headerInt1 == 1313818172 || headerInt1 == 1178339853) {
      return "font";
    }
    else if (headerInt1 == 1095914556 || headerInt1 == 1128350268 || headerInt1 == 1163148347 || headerInt1 == 1397706812 || headerInt1 == 1480938556 || headerInt1 == 1667327314 || headerInt1 == 1667327346 || headerInt1 == 1701869940 || headerInt1 == 1702260589 || headerInt1 == 1835365449 || headerInt1 == 1867915299) {
      return "script";
    }
    else if (headerInt1 == 1918989395 || headerInt1 == 1936291905) {
      return "anim";
    }
    else if (headerInt1 == 1936876886) {
      return "skel";
    }
    else if (headerInt1 == 1953527667) {
      return "swpt";
    }
    else if (headerInt1 == 3304803) {
      return "cm2";
    }
    else if (headerInt1 == 7368035) {
      return "cmp";
    }
    else if (headerInt1 == 809718610) {
      return "rsc";
    }
    else if (headerInt1 == 810831442) {
      return "rnt";
    }
    else if (headerInt1 == 1144148816) {
      return "ps2d";
    }
    else if (headerInt1 == 1145979479) {
      return "wbnd";
    }
    else if (headerInt1 == 1146047804) {
      return "model";
    }
    else if (headerInt1 == 1836413033) {
      return "ipum";
    }
    else if (headerInt1 == 1262634067) {
      return "sdbk";
    }
    else if (headerShort1 == 25454) {
      return "ncb";
    }
    else if (headerShort1 == 30316) {
      return "lvl";
    }
    else if ((headerShort1 == 32 || headerShort1 == 64 || headerShort1 == 128 || headerShort1 == 256 || headerShort1 == 512) && (headerShort2 == 32 || headerShort2 == 64 || headerShort2 == 128 || headerShort2 == 256 || headerShort2 == 512)) {
      return "tex";
    }

    return null;
  }

}
