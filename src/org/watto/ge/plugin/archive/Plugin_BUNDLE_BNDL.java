/*
 * Application:  Game Extractor
 * Author:       wattostudios
 * Website:      http://www.watto.org
 * Copyright:    Copyright (c) 2002-2022 wattostudios
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
import org.watto.datatype.Archive;
import org.watto.datatype.ReplacableResource;
import org.watto.datatype.Resource;
import org.watto.ge.helper.FieldValidator;
import org.watto.ge.plugin.ArchivePlugin;
import org.watto.ge.plugin.ExporterPlugin;
import org.watto.ge.plugin.exporter.Exporter_ZLib_CompressedSizeOnly;
import org.watto.io.FileManipulator;
import org.watto.io.converter.ByteConverter;
import org.watto.task.TaskProgressManager;

/**
**********************************************************************************************

**********************************************************************************************
**/
public class Plugin_BUNDLE_BNDL extends ArchivePlugin {

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public Plugin_BUNDLE_BNDL() {

    super("BUNDLE_BNDL", "BUNDLE_BNDL");

    //         read write replace rename
    setProperties(true, false, false, false);
    setCanImplicitReplace(true);

    setGames("Ghost Recon Advanced Warfighter",
        "Ghost Recon Advanced Warfighter 2",
        "Wanted: Weapons Of Fate",
        "Lead and Gold: Gangs of the Wild West");
    setExtensions("bundle");
    setPlatforms("PC");

    //setFileTypes("","",
    //             "",""
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

      // Header
      if (fm.readString(4).equals("BNDL")) {
        rating += 50;
      }

      // Version
      if (fm.readInt() == 2) {
        rating += 5;
      }

      long arcSize = fm.getLength();

      // First File Offset
      if (FieldValidator.checkOffset(fm.readInt(), arcSize)) {
        rating += 5;
      }

      // null
      if (fm.readInt() == 0) {
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

      ExporterPlugin exporter = Exporter_ZLib_CompressedSizeOnly.getInstance();

      // RESETTING GLOBAL VARIABLES

      FileManipulator fm = new FileManipulator(path, false);

      long arcSize = fm.getLength();

      // 4 - Header (BNDL)
      // 4 - Version (2)
      // 8 - First File Offset
      fm.skip(16);

      int numFiles = Archive.getMaxFiles();

      Resource[] resources = new Resource[numFiles];
      TaskProgressManager.setMaximum(numFiles);

      // Loop through directory
      String[] dirNames = new String[10];
      int numDirs = 0;

      int realNumFiles = 0;
      for (int i = 0; i < numFiles; i++) {

        // 1 - Entry Type
        int entryType = ByteConverter.unsign(fm.readByte());

        if (entryType == 0) {
          // end of directory - start of file data
          break;
        }
        if (entryType == 1) {
          // Start of SubFolder

          // 1 - Unknown (1)
          fm.skip(1);

          // X - Folder Name
          // 1 - null Folder Name Terminator
          String dirName = fm.readNullString();
          FieldValidator.checkFilename(dirName);

          if (numDirs < 0) {
            numDirs = 0;
          }
          dirNames[numDirs] = dirName;
          numDirs++;

        }
        else if (entryType == 2) {
          // File Entry

          // 8 - File Offset
          long offsetPointerLocation = fm.getOffset();
          int offsetPointerLength = 8;

          long offset = fm.readLong();

          // 4 - File Length
          long lengthPointerLocation = fm.getOffset();
          int lengthPointerLength = 4;

          long length = fm.readInt();
          FieldValidator.checkLength(length, arcSize);

          if (length != 0) {
            FieldValidator.checkOffset(offset, arcSize);
          }

          // 1 - Unknown (1)
          fm.skip(1);
          //System.out.println(fm.readByte());

          // X - Filename
          // 1 - null Filename Terminator
          String filename = fm.readNullString();
          FieldValidator.checkFilename(filename);

          // add the directory names to the start of the filename
          for (int n = numDirs - 1; n >= 0; n--) {
            filename = dirNames[n] + "\\" + filename;
          }

          //path,name,offset,length,decompLength,exporter
          resources[realNumFiles] = new ReplacableResource(path, filename, offset, offsetPointerLocation, offsetPointerLength, length, lengthPointerLocation, lengthPointerLength);
          realNumFiles++;

          TaskProgressManager.setValue(realNumFiles);

        }
        else if (entryType == 3) {
          // End of Current SubFolder
          numDirs--;
        }
        else {
          ErrorLogger.log("[BUNDLE_BNDL] Unknown entry type: " + entryType + " at " + fm.getOffset());
        }

      }

      resources = resizeResources(resources, realNumFiles);

      // now go through and see if the files are compressed
      fm.getBuffer().setBufferSize(1);
      fm.seek(0);

      for (int i = 0; i < realNumFiles; i++) {
        Resource resource = resources[i];
        fm.relativeSeek(resource.getOffset());

        if (fm.readString(1).equals("x")) {
          resource.setExporter(exporter);
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
