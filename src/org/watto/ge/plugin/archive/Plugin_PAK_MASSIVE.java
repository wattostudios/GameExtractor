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

import org.watto.datatype.Resource;
import org.watto.ge.helper.FieldValidator;
import org.watto.ge.plugin.ArchivePlugin;
import org.watto.io.FileManipulator;
import org.watto.io.buffer.ByteBuffer;
import org.watto.io.converter.IntConverter;
import org.watto.io.converter.StringConverter;
import org.watto.task.TaskProgressManager;

/**
**********************************************************************************************

**********************************************************************************************
**/
public class Plugin_PAK_MASSIVE extends ArchivePlugin {

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public Plugin_PAK_MASSIVE() {

    super("PAK_MASSIVE", "PAK_MASSIVE");

    //         read write replace rename
    setProperties(true, false, false, false);

    setExtensions("pak");
    setGames("SpellForce");
    setPlatforms("PC");

    setTextPreviewExtensions("bor", "des", "msh"); // LOWER CASE

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

      // Version
      if (fm.readInt() == 4) {
        rating += 5;
      }

      // Header
      if (fm.readString(21).equals("MASSIVE PAKFILE V 4.0")) {
        rating += 50;
      }

      fm.skip(51);

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
  
  **********************************************************************************************
  **/
  @Override
  public Resource[] read(File path) {
    try {

      addFileTypes();

      FileManipulator fm = new FileManipulator(path, false);

      // 4 - Version (4)
      // 24 - header ("MASSIVE PAKFILE V 4.0" 13 10 null)
      // 44 - Unknown
      // 4 - Unknown
      fm.skip(76);

      // 4 - Number Of Files
      int numFiles = fm.readInt();
      FieldValidator.checkNumFiles(numFiles);

      long arcSize = fm.getLength();

      Resource[] resources = new Resource[numFiles];
      TaskProgressManager.setMaximum(numFiles);

      // 4 - Unknown
      fm.skip(4);

      // 4 - File Data Offset
      int dataOffset = fm.readInt();
      FieldValidator.checkOffset(dataOffset, arcSize);

      // 4 - Archive Length
      fm.skip(4);

      // read the filename directory
      int filenameDirOffset = 92 + (numFiles * 16);
      int filenameDirLength = dataOffset - filenameDirOffset;

      fm.seek(filenameDirOffset);
      byte[] nameBytes = fm.readBytes(filenameDirLength);

      FileManipulator nameFM = new FileManipulator(new ByteBuffer(nameBytes));

      fm.seek(92);

      for (int i = 0; i < numFiles; i++) {
        // 4 - File Length
        int length = fm.readInt();
        FieldValidator.checkLength(length, arcSize);

        // 4 - File Offset (relative to the start of the file data)
        int offset = fm.readInt() + dataOffset;
        FieldValidator.checkOffset(offset, arcSize);

        // 3 - Filename Offset (relative to the start of the filename directory)
        // 1 - Unknown
        byte[] filenameBytes = fm.readBytes(4);
        //System.out.println(filenameBytes[3]);
        filenameBytes[3] = 0;
        int filenameOffset = IntConverter.convertLittle(filenameBytes);
        FieldValidator.checkOffset(filenameOffset, filenameDirLength);

        // 3 - Directory Name Offset (relative to the start of the filename directory)
        // 1 - Unknown
        byte[] dirNameBytes = fm.readBytes(4);
        //System.out.println(dirNameBytes[3]);
        dirNameBytes[3] = 0;
        int dirNameOffset = IntConverter.convertLittle(dirNameBytes);
        FieldValidator.checkOffset(dirNameOffset, filenameDirLength);

        nameFM.seek(dirNameOffset);
        String dirName = nameFM.readNullString();
        dirName = StringConverter.reverse(dirName) + "\\";

        nameFM.seek(filenameOffset);
        nameFM.skip(2);
        String filename = nameFM.readNullString();
        filename = dirName + StringConverter.reverse(filename);

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