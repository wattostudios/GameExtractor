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
import org.watto.io.FileManipulator;
import org.watto.io.buffer.ByteBuffer;
import org.watto.task.TaskProgressManager;

/**
**********************************************************************************************

**********************************************************************************************
**/
public class Plugin_DAT_SFDC extends ArchivePlugin {

  /**
  **********************************************************************************************

  **********************************************************************************************
  **/
  public Plugin_DAT_SFDC() {

    super("DAT_SFDC", "DAT_SFDC");

    //         read write replace rename
    setProperties(true, false, false, false);

    setGames("Tribes: Aerial Assault");
    setExtensions("dat"); // MUST BE LOWER CASE
    setPlatforms("PC");

    // MUST BE LOWER CASE !!!
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
      if (fm.readString(4).equals("SFDC")) {
        rating += 50;
      }

      // 4 - Version? (1)
      if (fm.readInt() == 1) {
        rating += 5;
      }

      // 4 - Padding Multiple (2048)
      if (fm.readInt() == 2048) {
        rating += 5;
      }

      fm.skip(4);

      long arcSize = fm.getLength();

      // 4 - First File Offset
      if (FieldValidator.checkOffset(fm.readInt(), arcSize)) {
        rating += 5;
      }

      fm.skip(8);

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

      // 4 - Header (SFDC)
      // 4 - Version? (1)
      fm.skip(8);

      // 4 - Padding Multiple (2048)
      int paddingMultiple = fm.readInt();
      FieldValidator.checkRange(paddingMultiple, 128, 4096);

      // 4 - Unknown
      fm.skip(4);

      // 4 - First File Offset
      int fileDataOffset = fm.readInt();
      FieldValidator.checkOffset(fileDataOffset, arcSize);

      // 4 - Unknown
      // 4 - Directory Length
      fm.skip(8);

      // 4 - Number of Files
      int numFiles = fm.readInt();
      FieldValidator.checkNumFiles(numFiles);

      // 4 - Filename Directory Length
      int filenameDirLength = fm.readInt();
      FieldValidator.checkLength(filenameDirLength, arcSize);

      Resource[] resources = new Resource[numFiles];
      TaskProgressManager.setMaximum(numFiles);

      // Loop through directory
      int[] filenameOffsets = new int[numFiles];
      int[] dirNameOffsets = new int[numFiles];
      for (int i = 0; i < numFiles; i++) {

        // 4 - File Length
        int length = fm.readInt();
        FieldValidator.checkLength(length, arcSize);

        // 4 - Filename Offset (relative to the start of the Filename Directory)
        int filenameOffset = fm.readInt();
        FieldValidator.checkOffset(filenameOffset, arcSize);
        filenameOffsets[i] = filenameOffset;

        // 4 - Directory Name (relative to the start of the Filename Directory)
        int dirNameOffset = fm.readInt();
        FieldValidator.checkOffset(dirNameOffset, arcSize);
        dirNameOffsets[i] = dirNameOffset;

        // 4 - File Offset [*2048] (relative to the start of the File Data)
        int offset = fm.readInt() * 2048 + fileDataOffset;
        FieldValidator.checkOffset(offset, arcSize);

        String filename = "";

        //path,name,offset,length,decompLength,exporter
        resources[i] = new Resource(path, filename, offset, length);

        TaskProgressManager.setValue(i);
      }

      // 4 - Unknown
      fm.skip(4);

      // get the full filename directory for quick reading/seeking
      byte[] filenameDirectoryBytes = fm.readBytes(filenameDirLength);
      fm.close();
      fm = new FileManipulator(new ByteBuffer(filenameDirectoryBytes));

      for (int i = 0; i < numFiles; i++) {
        // get the directory name
        fm.seek(dirNameOffsets[i]);
        String dirName = fm.readNullString();

        int dotPos = dirName.indexOf("C:\\");
        if (dotPos < 0) {
          dotPos = dirName.indexOf("..\\");
        }
        if (dotPos >= 0) {
          dirName = dirName.substring(3);
        }

        // get the filename
        fm.seek(filenameOffsets[i]);
        String filename = fm.readNullString();

        filename = dirName + "\\" + filename;

        Resource resource = resources[i];
        resource.setName(filename);
        resource.setOriginalName(filename);
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
