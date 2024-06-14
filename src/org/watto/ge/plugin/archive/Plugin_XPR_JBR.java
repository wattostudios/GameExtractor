/*
 * Application:  Game Extractor
 * Author:       wattostudios
 * Website:      http://www.watto.org
 * Copyright:    Copyright (c) 2002-2024 wattostudios
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
import org.watto.datatype.Resource;
import org.watto.ge.helper.FieldValidator;
import org.watto.ge.plugin.ArchivePlugin;
import org.watto.io.FileManipulator;
import org.watto.task.TaskProgressManager;

/**
**********************************************************************************************

**********************************************************************************************
**/
public class Plugin_XPR_JBR extends ArchivePlugin {

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public Plugin_XPR_JBR() {

    super("XPR_JBR", "XPR_JBR");

    //         read write replace rename
    setProperties(true, false, false, false);

    setGames("Xenophage: Alien Bloodsport");
    setExtensions("xpr"); // MUST BE LOWER CASE
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

      // Header
      if (fm.readString(4).equals("JBR" + (char) 0)) {
        rating += 50;
      }

      if (fm.readShort() == 1) {
        rating += 5;
      }

      long arcSize = fm.getLength();

      // Footer Offset
      if (FieldValidator.checkOffset(fm.readInt(), arcSize)) {
        rating += 5;
      }

      // Directory Offset
      if (FieldValidator.checkOffset(fm.readInt(), arcSize)) {
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

      // 4 - Header ("JBR" + null)
      // 2 - Version? (1)
      fm.skip(6);

      // 4 - Footer Offset
      long footerOffset = fm.readInt();
      FieldValidator.checkOffset(footerOffset, arcSize);

      // 4 - Filename Directory Offset
      long filenameDirOffset = fm.readInt();
      FieldValidator.checkOffset(filenameDirOffset, arcSize);

      // Read the filename directory
      fm.seek(filenameDirOffset);

      // 4 - Number of Files
      int numNames = fm.readInt();
      FieldValidator.checkNumFiles(numNames);

      // 4 - Number of Files [-1]
      fm.skip(4);

      String[] names = new String[numNames];
      for (int i = 0; i < numNames; i++) {
        // 4 - File ID (incremental from 1)
        // 2 - Unknown (1)
        // 4 - Unknown
        fm.skip(10);

        // 128 - Filename (null terminated, filled with nulls)
        String filename = fm.readNullString(128);
        FieldValidator.checkFilename(filename);
        names[i] = filename;

        TaskProgressManager.setValue(i);
      }

      // go to the footer to find all the type diectories
      fm.seek(footerOffset);

      // 2 - null
      // 4 - Unknown (321/322)
      fm.skip(6);

      // 4 - Number of Type Directories
      int numTypes = fm.readInt();
      FieldValidator.checkNumFiles(numTypes);

      int[] offsets = new int[numTypes];
      for (int i = 0; i < numTypes; i++) {
        // 4 - Type Directory Offset
        int typeDirOffset = fm.readInt();
        FieldValidator.checkOffset(typeDirOffset, arcSize);
        offsets[i] = typeDirOffset;
      }

      int numFiles = Archive.getMaxFiles();

      Resource[] resources = new Resource[numFiles];
      TaskProgressManager.setMaximum(arcSize);

      // read each type directory
      int realNumFiles = 0;
      for (int t = 0; t < numTypes; t++) {
        fm.relativeSeek(offsets[t]);

        // 4 - Type Header (eg WAVE)
        String type = fm.readNullString(4);

        // 4 - Unknown (-1)
        fm.skip(4);

        // 4 - Number of Files of this Type
        int numFilesOfType = fm.readInt();
        FieldValidator.checkNumFiles(numFilesOfType);

        for (int i = 0; i < numFilesOfType; i++) {
          // 2 - null
          fm.skip(2);

          // 4 - File Offset
          long offset = fm.readInt();
          FieldValidator.checkOffset(offset, arcSize);

          // 4 - File Length
          long length = fm.readInt();
          FieldValidator.checkLength(length, arcSize);

          // 18 - null
          fm.skip(18);

          // 4 - File ID (matches to an ID in the Filename Directory)
          int fileID = fm.readInt() - 1;

          // 4 - Unknown ID
          // 4 - Unknown
          fm.skip(8);

          // 32 - Friendly Name (null terminated, filled with nulls)
          String friendlyName = fm.readNullString(32);

          String filename = null;
          if (fileID >= 0 && fileID < numNames) {
            filename = names[fileID];
          }
          else {
            // see if we can use the friendly name
            if (friendlyName != null && friendlyName.length() > 0) {
              filename = friendlyName + "." + type;
            }
            else {
              filename = Resource.generateFilename(realNumFiles);
            }
          }

          //path,name,offset,length,decompLength,exporter
          resources[realNumFiles] = new Resource(path, filename, offset, length);
          realNumFiles++;

          TaskProgressManager.setValue(offset);
        }
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
