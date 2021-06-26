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
import org.watto.ge.plugin.exporter.Exporter_ZLib;
import org.watto.io.FileManipulator;
import org.watto.io.buffer.ByteBuffer;
import org.watto.task.TaskProgressManager;

/**
**********************************************************************************************

**********************************************************************************************
**/
public class Plugin_SGA_ARCHIVE extends ArchivePlugin {

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public Plugin_SGA_ARCHIVE() {

    super("SGA_ARCHIVE", "SGA_ARCHIVE");

    //         read write replace rename
    setProperties(true, false, false, false);

    setGames("Warhammer 40k: Dawn Of War",
        "Warhammer 40k: Dawn of War: Winter Assault");
    setExtensions("sga");
    setPlatforms("PC");

    setTextPreviewExtensions("colours", "rat", "screen", "styles", "turn", "ai", "camp", "nis", "scar", "teamcolour"); // LOWER CASE

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
      if (fm.readString(8).equals("_ARCHIVE")) {
        rating += 50;
      }

      // Version
      if (FieldValidator.checkEquals(fm.readInt(), 2)) {
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
  
  **********************************************************************************************
  **/
  @Override
  @SuppressWarnings("unused")
  public Resource[] read(File path) {
    try {

      ExporterPlugin exporter = Exporter_ZLib.getInstance();
      addFileTypes();

      FileManipulator fm = new FileManipulator(path, false);

      long arcSize = fm.getLength();

      // 8 - Header (_ARCHIVE)
      // 4 - Version (2)
      // 4 - Unknown
      // 4 - Unknown
      // 4 - Unknown
      // 4 - Unknown
      // 128 - Archive Name (unicode)
      // 4 - Unknown
      // 4 - Unknown
      // 4 - Unknown
      // 4 - Unknown
      fm.skip(172);

      // 4 - Directories Length
      int directoriesLength = fm.readInt();
      FieldValidator.checkLength(directoriesLength, arcSize);

      // 4 - File Data Offset
      int fileDataOffset = fm.readInt();
      FieldValidator.checkOffset(fileDataOffset, arcSize);

      // 4 - Descriptions Directory Offset (24) [+180]
      // 2 - Number Of Descriptions (1)
      fm.skip(6);

      // 4 - Folders Directory Offset [+180]
      int folderDirOffset = fm.readInt() + 180;
      FieldValidator.checkOffset(folderDirOffset, arcSize);

      // 2 - Number Of Folders
      int numFolders = fm.readShort();

      // 4 - Files Directory Offset [+180]
      long dirOffset = fm.readInt() + 180;
      FieldValidator.checkOffset(dirOffset, arcSize);

      // 2 - Number Of Files
      int numFiles = fm.readShort();

      // 4 - Filename Directory Offset [+180]
      int filenameDirOffset = fm.readInt() + 180;
      FieldValidator.checkOffset(filenameDirOffset, arcSize);

      // 2 - Number Of Filenames
      int numNames = fm.readShort();

      Resource[] resources = new Resource[numFiles];
      TaskProgressManager.setMaximum(numFiles);

      // Read in the filename directory, for quick access later on
      fm.seek(filenameDirOffset);

      int filenameDirLength = directoriesLength - filenameDirOffset + 180;
      byte[] nameDirBytes = fm.readBytes(filenameDirLength);
      FileManipulator nameFM = new FileManipulator(new ByteBuffer(nameDirBytes));

      // Files Directory
      fm.seek(dirOffset);
      for (int i = 0; i < numFiles; i++) {
        // 4 - Filename Offset (relative to filename directory offset)
        int filenameOffset = fm.readInt();
        FieldValidator.checkOffset(filenameOffset, filenameDirLength);

        nameFM.seek(filenameOffset);
        // X - Filename
        String filename = nameFM.readNullString();

        // 4 - Unknown
        fm.skip(4);

        // 4 - File Offset (relative to start of the file data)
        long offset = fm.readInt() + fileDataOffset;
        FieldValidator.checkOffset(offset, arcSize);

        // 4 - Compressed File Size
        long length = fm.readInt();
        FieldValidator.checkLength(length, arcSize);

        // 4 - Decompressed File Size
        int decompLength = fm.readInt();
        FieldValidator.checkLength(decompLength);

        //path,id,name,offset,length,decompLength,exporter
        if (length != decompLength) {
          resources[i] = new Resource(path, filename, offset, length, decompLength, exporter);
        }
        else {
          resources[i] = new Resource(path, filename, offset, length, decompLength);
        }

        TaskProgressManager.setValue(i);
      }

      // Folders Directory
      fm.seek(folderDirOffset);
      for (int i = 0; i < numFolders; i++) {
        // 4 - Folder Name Offset (relative to filename directory offset)
        int filenameOffset = fm.readInt();

        nameFM.seek(filenameOffset);
        // X - Filename
        String filename = nameFM.readNullString();

        // 2 - First Sub-Folder Number
        // 2 - Last Sub-Folder Number
        // NOTE - IF THE 2 FIELDS ABOVE =36, THEY CONTAIN NO SUB-FOLDERS
        fm.skip(4);

        // 2 - First Filename Number
        int firstFilename = fm.readShort();

        // 2 - Last Filename Number
        int lastFilename = fm.readShort();

        if (filename.length() > 0) {
          // assign names to the files
          for (int j = firstFilename; j < lastFilename; j++) {
            //String filename = names[i] + names[numFolders+j];
            Resource resource = resources[j];
            String name = filename + "\\" + resource.getName();
            resource.setName(name);
            resource.setOriginalName(name);
          }
        }

      }

      nameFM.close();
      fm.close();

      return resources;

    }
    catch (Throwable t) {
      logError(t);
      return null;
    }
  }

}
