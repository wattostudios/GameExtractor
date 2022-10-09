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
public class Plugin_000_SFDX extends ArchivePlugin {

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public Plugin_000_SFDX() {

    super("000_SFDX", "000_SFDX");

    //         read write replace rename
    setProperties(true, false, false, false);

    setGames("The Hobbit");
    setExtensions("000");
    setPlatforms("PC");

    // MUST BE LOWER CASE !!!
    setFileTypes(new FileType("xbmp", "XBMP Image", FileType.TYPE_IMAGE),
        new FileType("audiopkg", "Audio Package", FileType.TYPE_OTHER),
        new FileType("bin", "Binary File", FileType.TYPE_OTHER),
        new FileType("anim", "Animation", FileType.TYPE_OTHER),
        new FileType("charanim", "Character Animation", FileType.TYPE_OTHER));

    setTextPreviewExtensions("exportres", "info", "h", "matx"); // LOWER CASE

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

      // NOTE - Compressed files MUST know their DECOMPRESSED LENGTH
      //      - Uncompressed files MUST know their LENGTH

      addFileTypes();

      // RESETTING THE GLOBAL VARIABLES

      long arcSize = (int) path.length();

      File sourcePath = getDirectoryFile(path, "dfs");

      FileManipulator fm = new FileManipulator(sourcePath, false);

      // 4 - Header (SFDX)
      // 4 - Number Of Folders (1)
      // 4 - Padding Multiple (2048)
      // 4 - Unknown
      fm.skip(16);

      // 4 - Number Of Files
      int numFiles = fm.readInt();
      FieldValidator.checkNumFiles(numFiles);

      // 4 - Folder Name Offset [-1] (relative to the start of the name directory)
      // 4 - Unknown (512)
      // 4 - Unknown (40)
      fm.skip(12);

      // 4 - Directory Offset (44)
      int dirOffset = fm.readInt();
      FieldValidator.checkOffset(dirOffset, arcSize);

      // 4 - Filename Directory Offset
      int filenameDirOffset = fm.readInt();
      FieldValidator.checkOffset(filenameDirOffset);

      // 4 - Unknown
      fm.skip(4);

      int filenameDirLength = (int) (arcSize - filenameDirOffset);
      fm.seek(filenameDirOffset);
      byte[] filenameBytes = fm.readBytes(filenameDirLength);
      fm.seek(dirOffset);

      FileManipulator nameFM = new FileManipulator(new ByteBuffer(filenameBytes));

      Resource[] resources = new Resource[numFiles];

      TaskProgressManager.setMaximum(numFiles);

      // Loop through directory
      for (int i = 0; i < numFiles; i++) {
        // 4 - Filename Part 1 Offset (relative to the start of the name directory)
        int name1Offset = fm.readInt();
        FieldValidator.checkOffset(name1Offset, arcSize);

        // 4 - Filename Part 2 Offset (relative to the start of the name directory)
        int name2Offset = fm.readInt();
        FieldValidator.checkOffset(name2Offset, arcSize);

        // 4 - Directory Name Offset (relative to the start of the name directory)
        int dirNameOffset = fm.readInt();
        FieldValidator.checkOffset(dirNameOffset, arcSize);

        // 4 - File Extension Offset (relative to the start of the name directory)
        int extensionOffset = fm.readInt();
        FieldValidator.checkOffset(extensionOffset, arcSize);

        // 4 - File Offset
        long offset = fm.readInt();
        FieldValidator.checkOffset(offset, arcSize);

        // 4 - File Length
        long length = fm.readInt();
        FieldValidator.checkLength(length, arcSize);

        String filename = "";
        nameFM.seek(dirNameOffset);
        filename += nameFM.readNullString();
        nameFM.seek(name1Offset);
        filename += nameFM.readNullString();
        nameFM.seek(name2Offset);
        filename += nameFM.readNullString();
        nameFM.seek(extensionOffset);
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
