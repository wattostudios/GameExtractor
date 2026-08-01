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

import org.watto.datatype.FileType;
import org.watto.datatype.Resource;
import org.watto.ge.helper.FieldValidator;
import org.watto.ge.plugin.ArchivePlugin;
import org.watto.io.FileManipulator;
import org.watto.io.buffer.ByteBuffer;
import org.watto.task.TaskProgressManager;

/**
**********************************************************************************************

**********************************************************************************************
**/
public class Plugin_DFS_SFDX extends ArchivePlugin {

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public Plugin_DFS_SFDX() {

    super("DFS_SFDX", "DFS_SFDX");

    //         read write replace rename
    setProperties(true, false, false, false);

    setGames("Area 51");
    setExtensions("000");
    setPlatforms("PC");

    // MUST BE LOWER CASE !!!
    setFileTypes(new FileType("xbmp", "Texture Image", FileType.TYPE_IMAGE));

    setTextPreviewExtensions("h", "psh", "vsh"); // LOWER CASE

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

      getDirectoryFile(fm.getFile(), "dfs");
      rating += 25;

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

      long arcSize = path.length();

      File sourcePath = getDirectoryFile(path, "dfs");
      FileManipulator fm = new FileManipulator(sourcePath, false);

      long dirSize = sourcePath.length();

      // 4 - Header (SFDX)
      // 4 - Version (3)
      // 4 - Unknown
      // 4 - Padding Size (2048)
      // 4 - Unknown (1000000000)
      fm.skip(20);

      // 4 - Number Of Files
      int numFiles = fm.readInt();
      FieldValidator.checkNumFiles(numFiles);

      // 4 - Unknown (1)
      fm.skip(4);

      // 4 - Name Directory Length
      int nameDirLength = fm.readInt();
      FieldValidator.checkLength(nameDirLength, dirSize);

      // 4 - Unknown (48)
      fm.skip(4);

      // 4 - Header Length (56)
      int dirOffset = fm.readInt();
      FieldValidator.checkOffset(dirOffset, dirSize);

      // 4 - null
      fm.skip(4);

      // 4 - Name Directory Offset
      int nameDirOffset = fm.readInt();
      FieldValidator.checkOffset(nameDirOffset, dirSize);

      // 4 - 000 File Length
      // 4 - null
      fm.seek(nameDirOffset);

      byte[] nameBytes = fm.readBytes(nameDirLength);
      FileManipulator nameFM = new FileManipulator(new ByteBuffer(nameBytes));

      Resource[] resources = new Resource[numFiles];

      TaskProgressManager.setMaximum(numFiles);

      // skip back to the directory
      fm.relativeSeek(dirOffset);

      // Loop through directory
      for (int i = 0; i < numFiles; i++) {
        // 4 - Directory Name Offset (relative to the start of the Filename Directory)
        int dirNameOffset = fm.readInt();
        FieldValidator.checkOffset(dirNameOffset, nameDirLength);

        // 4 - Filename Offset (relative to the start of the Filename Directory)
        int filenameOffset = fm.readInt();
        FieldValidator.checkOffset(filenameOffset, nameDirLength);

        // 4 - null
        fm.skip(4);

        // 4 - File Extension Offset (relative to the start of the Filename Directory)
        int extensionNameOffset = fm.readInt();
        FieldValidator.checkOffset(extensionNameOffset, nameDirLength);

        // 4 - File Offset (offset in the 000 file)
        int offset = fm.readInt();
        FieldValidator.checkOffset(offset, arcSize);

        // 4 - File Length
        int length = fm.readInt();
        FieldValidator.checkLength(length, arcSize);

        //String filename = names[i];
        String filename = "";

        nameFM.seek(dirNameOffset);
        filename += nameFM.readNullString();

        nameFM.seek(filenameOffset);
        filename += nameFM.readNullString();

        nameFM.seek(extensionNameOffset);
        filename += nameFM.readNullString();

        //path,id,name,offset,length,decompLength,exporter
        resources[i] = new Resource(path, filename, offset, length);

        TaskProgressManager.setValue(i);
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
