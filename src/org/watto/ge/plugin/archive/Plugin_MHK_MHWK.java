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
import org.watto.datatype.Resource;
import org.watto.ge.helper.FieldValidator;
import org.watto.ge.plugin.ArchivePlugin;
import org.watto.ge.plugin.ExporterPlugin;
import org.watto.ge.plugin.exporter.Exporter_Custom_MHK_MHWK_WAV;
import org.watto.io.FileManipulator;
import org.watto.io.converter.IntConverter;
import org.watto.io.converter.ShortConverter;
import org.watto.task.TaskProgressManager;

/**
**********************************************************************************************

**********************************************************************************************
**/
public class Plugin_MHK_MHWK extends ArchivePlugin {

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public Plugin_MHK_MHWK() {

    super("MHK_MHWK", "MHK_MHWK");

    //         read write replace rename
    setProperties(true, false, false, false);

    setGames("Carmen Sandiego: Word Detective",
        "Koala Lumpur: Journey To The Edge",
        "Myst",
        "Green Eggs and Ham",
        "Riven",
        "The Cat In The Hat by Dr. Seuss");
    setExtensions("mhk");
    setPlatforms("PC");

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
      if (fm.readString(4).equals("MHWK")) {
        rating += 50;
      }

      long arcSize = fm.getLength();

      // Archive Size
      if (IntConverter.changeFormat(fm.readInt()) + 8 == arcSize) {
        rating += 5;
      }

      // Directory Header
      if (fm.readString(4).equals("RSRC")) {
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

      // NOTE - Compressed file MUST know their DECOMPRESSED LENGTH
      //      - Uncompressed files MUST know their LENGTH

      addFileTypes();

      // RESETTING THE GLOBAL VARIABLES

      ExporterPlugin wavExporter = Exporter_Custom_MHK_MHWK_WAV.getInstance();

      FileManipulator fm = new FileManipulator(path, false);

      long arcSize = (int) fm.getLength();

      // 4 - Header (MHWK)
      // 4 - Archive Size [+8]
      // 4 - Directory Header (RSRC)
      // 4 - Length Of All Directories
      // 4 - Archive Size
      fm.skip(20);

      // 4 - Type Directory Offset
      int typeDirOffset = IntConverter.changeFormat(fm.readInt());
      FieldValidator.checkOffset(typeDirOffset, arcSize);

      // 2 - Entry Directory Offset [+typeDirectoryOffset]
      long dirOffset = typeDirOffset + ShortConverter.changeFormat(fm.readShort());
      FieldValidator.checkOffset(dirOffset, arcSize);

      // 2 - Length of the Entry Directory

      // 2 - Filename Directory Offset
      fm.seek(typeDirOffset + 2);

      // 2 - Number Of Types
      short numTypes = ShortConverter.changeFormat(fm.readShort());
      FieldValidator.checkNumFiles(numTypes);

      String[] types = new String[numTypes];
      int[] numFilesOfType = new int[numTypes];
      // Loop through type directory 1
      for (int i = 0; i < numTypes; i++) {
        // 4 - Type Code String
        String type = fm.readString(4);
        if (type.charAt(0) == 't') {
          type = type.substring(1);
        }
        types[i] = type;

        // 2 - Offset To This Type Details
        // 2 - Unknown
        fm.skip(4);
      }

      // Loop through type directory 2
      long[] typeOffsets = new long[numTypes];
      for (int i = 0; i < numTypes; i++) {
        // 2 - NumFiles of this type
        int numOfType = ShortConverter.changeFormat(fm.readShort());
        FieldValidator.checkNumFiles(numOfType);
        numFilesOfType[i] = numOfType;
        typeOffsets[i] = fm.getOffset();

        fm.skip(numOfType * 4 + 2);
      }

      fm.seek(dirOffset);

      // 4 - Number Of Files (from dirOffset)
      int numFiles = IntConverter.changeFormat(fm.readInt());
      FieldValidator.checkNumFiles(numFiles);

      //if (numFiles < numFilesTotal) {
      //  numFiles = numFilesTotal;
      //}

      Resource[] resources = new Resource[numFiles];

      TaskProgressManager.setMaximum(numFiles);

      // Loop through directory
      int realNumFiles = 0;
      //for (int t = 0; t < numTypes; t++) {
      //  for (int i = 0; i < numFilesOfType[t]; i++) {
      for (int i = 0; i < numFiles; i++) {
        // 4 - File Offset
        long offset = IntConverter.changeFormat(fm.readInt());
        FieldValidator.checkOffset(offset, arcSize);

        // 3 - File Length
        // 1 - Flags
        // 2 - Unknown
        fm.skip(6);

        //String type = types[t];
        String filename = Resource.generateFilename(realNumFiles);

        //path,id,name,offset,length,decompLength,exporter
        resources[realNumFiles] = new Resource(path, filename, offset);

        TaskProgressManager.setValue(realNumFiles);
        realNumFiles++;
      }
      //}

      for (int t = 0; t < numTypes; t++) {
        fm.seek(typeOffsets[t]);
        String type = types[t];

        for (int i = 0; i < numFilesOfType[t]; i++) {
          // 2 - Unknown
          fm.skip(2);

          // 2 - File ID (starting at 1)
          int fileID = ShortConverter.changeFormat(fm.readShort()) - 1;

          if (fileID >= 0 && fileID < numFiles) {

            Resource resource = resources[fileID];
            String extension = resource.getExtension();
            if (extension == null || extension.equals("")) {
              String filename = resource.getName() + "." + type;
              resource.setName(filename);
              resource.setOriginalName(filename);

              if (type.equals("WAV")) {
                resource.setExporter(wavExporter);
              }
            }
          }
        }
      }

      if (typeDirOffset == 28) {
        // directory at the start of the archive
        calculateFileSizes(resources, arcSize);
      }
      else {
        // directory at the end of the archive
        calculateFileSizes(resources, typeDirOffset);
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
