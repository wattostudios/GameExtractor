/*
 * Application:  Game Extractor
 * Author:       wattostudios
 * Website:      http://www.watto.org
 * Copyright:    Copyright (c) 2002-2026 wattostudios
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

import org.watto.ErrorLogger;
import org.watto.TemporarySettings;
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
public class Plugin_XPK_2 extends ArchivePlugin {

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public Plugin_XPK_2() {

    super("XPK_2", "XPK_2");

    //         read write replace rename
    setProperties(true, false, false, false);

    setGames("WWE Raw");
    setExtensions("xpk"); // MUST BE LOWER CASE
    setPlatforms("PC");

    setFileTypes(new FileType("fpk", "FPK Archive", FileType.TYPE_ARCHIVE));

    setTextPreviewExtensions("fco", "fda", "hra", "mdf", "htt", "lst"); // LOWER CASE

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

      String baseFile = fm.getFilePath();
      int dotPos = baseFile.lastIndexOf('.');
      if (dotPos <= 0) {
        return 0;
      }
      baseFile = baseFile.substring(0, dotPos);

      if (new File(baseFile + "_D.BIN").exists()) {
        rating += 25;
      }
      else {
        return 0;
      }

      if (new File(baseFile + "_F.BIN").exists()) {
        rating += 25;
      }
      else {
        return 0;
      }

      return rating;

    }
    catch (Throwable t) {
      return 0;
    }
  }

  int realNumFiles = 0;

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

      long arcSize = path.length();

      String baseFile = path.getAbsolutePath();
      int dotPos = baseFile.lastIndexOf('.');
      if (dotPos <= 0) {
        return null;
      }
      baseFile = baseFile.substring(0, dotPos);

      File dFile = new File(baseFile + "_D.BIN");
      File fFile = new File(baseFile + "_F.BIN");

      if (!dFile.exists() || !fFile.exists()) {
        return null;
      }

      FileManipulator fm = new FileManipulator(fFile, false);
      // 4 - Unknown (591826)
      // 4 - Unknown (983059)
      // 4 - Unknown
      fm.skip(12);

      int numFiles = (int) (fFile.length() - 12) / 12;
      FieldValidator.checkNumFiles(numFiles);

      int[] offsets = new int[numFiles];
      int[] lengths = new int[numFiles];

      for (int i = 0; i < numFiles; i++) {
        // 4 - File Offset
        int offset = fm.readInt();
        FieldValidator.checkOffset(offset, arcSize);
        offsets[i] = offset;

        // 4 - File Length
        int length = fm.readInt();
        FieldValidator.checkLength(length, arcSize);
        lengths[i] = length;

        // 4 - Unknown
        fm.skip(4);
      }

      fm = new FileManipulator(dFile, false);

      // 4 - Unknown (591826)
      // 4 - Unknown (983059)
      // 4 - Unknown
      fm.skip(12);

      // 4 - Root Directory Offset
      int rootOffset = fm.readInt();
      FieldValidator.checkOffset(rootOffset, arcSize);

      fm.relativeSeek(rootOffset);

      Resource[] resources = new Resource[numFiles];
      TaskProgressManager.setMaximum(numFiles);

      realNumFiles = 0;
      readDirectory(path, fm, resources, "", offsets, lengths);

      fm.close();

      TemporarySettings.set("ForceVerticalImageFlip", true);

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
  public void readDirectory(File path, FileManipulator fm, Resource[] resources, String dirName, int[] offsets, int[] lengths) {
    try {

      long dirLength = fm.getLength();

      int numDirs = 0;
      int numFiles = resources.length;
      int[] dirOffsets = new int[numFiles];
      String[] dirNames = new String[numFiles];

      while (fm.getOffset() < dirLength) {

        // 2 - Entry Type (0=endOfFolder, 1=file, 2=folder)
        short entryType = fm.readShort();
        if (entryType == 0) {
          break; // end of directory
        }

        // 2 - Name Length (not including null terminator)
        short filenameLength = fm.readShort();
        FieldValidator.checkFilenameLength(filenameLength);

        // 4 - Offset to the Entries for this Folder / File ID
        int offset = fm.readInt();

        // X - Name
        // 1 - null Name Terminator
        String name = dirName + fm.readString(filenameLength);
        fm.skip(1);

        if (entryType == 1) {
          // file
          int fileID = offset;

          int length = lengths[fileID];
          offset = offsets[fileID];

          //path,name,offset,length,decompLength,exporter
          resources[fileID] = new Resource(path, name, offset, length);

          TaskProgressManager.setValue(realNumFiles);
          realNumFiles++;
        }
        else if (entryType == 2) {
          // folder
          dirOffsets[numDirs] = offset;
          dirNames[numDirs] = name + "\\";
          numDirs++;
        }
        else {
          // unknown
          ErrorLogger.log("[XPK_2] Unknown entry type: " + entryType);
          break;
        }

      }

      for (int i = 0; i < numDirs; i++) {
        fm.relativeSeek(dirOffsets[i]);

        readDirectory(path, fm, resources, dirNames[i], offsets, lengths);
      }

    }
    catch (Throwable t) {
      logError(t);
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
