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
import org.watto.datatype.Resource;
import org.watto.ge.helper.FieldValidator;
import org.watto.ge.plugin.ArchivePlugin;
import org.watto.io.FileManipulator;
import org.watto.io.converter.ByteConverter;
import org.watto.io.converter.IntConverter;
import org.watto.task.TaskProgressManager;

/**
**********************************************************************************************

**********************************************************************************************
**/
public class Plugin_CAT_4 extends ArchivePlugin {

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public Plugin_CAT_4() {

    super("CAT_4", "CAT_4");

    //         read write replace rename
    setProperties(true, false, false, false);

    setExtensions("cat");
    setGames("OpenTTD");
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

      long arcSize = fm.getLength();

      // First File Offset
      byte[] firstOffsetBytes = fm.readBytes(4);
      firstOffsetBytes[3] = 0;
      if (FieldValidator.checkOffset(IntConverter.convertLittle(firstOffsetBytes), arcSize)) {
        rating += 5;
      }

      // First File Length
      if (FieldValidator.checkLength(fm.readInt(), arcSize)) {
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

      // NumFiles
      byte[] firstOffsetBytes = fm.readBytes(4);
      firstOffsetBytes[3] = 0;
      int numFiles = IntConverter.convertLittle(firstOffsetBytes) / 8;
      FieldValidator.checkNumFiles(numFiles);

      fm.relativeSeek(0);

      long arcSize = fm.getLength();

      Resource[] resources = new Resource[numFiles];
      TaskProgressManager.setMaximum(numFiles);

      int[] offsets = new int[numFiles];
      int[] lengths = new int[numFiles];
      for (int i = 0; i < numFiles; i++) {
        // 4 - File Offset
        byte[] offsetBytes = fm.readBytes(4);
        offsetBytes[3] = 0;
        int offset = IntConverter.convertLittle(offsetBytes);
        FieldValidator.checkOffset(offset, arcSize);

        offsets[i] = offset;

        // 4 - File Length
        int length = fm.readInt();
        FieldValidator.checkLength(length, arcSize);

        lengths[i] = length;
        TaskProgressManager.setValue(i);
      }

      fm.getBuffer().setBufferSize(128); // small quick reads
      fm.seek(1);

      for (int i = 0; i < numFiles; i++) {
        int offset = offsets[i];
        int length = lengths[i];

        fm.seek(offset);

        // 1 - Description Length
        int descriptionLength = ByteConverter.unsign(fm.readByte());

        // X - Description
        // 1 - null Description Terminator
        fm.skip(descriptionLength);

        // 4 - RIFF Header
        offset = (int) fm.getOffset();
        fm.skip(4);

        // 4 - RIFF length
        int riffLength = fm.readInt();
        FieldValidator.checkLength(riffLength, length);
        fm.skip(riffLength);

        length = riffLength + 8;

        // 1 - null
        fm.skip(1);

        // 1 - Filename Length
        int filenameLength = ByteConverter.unsign(fm.readByte());

        // X - Filename
        // 1 - null Filename Terminator
        String filename = fm.readNullString(filenameLength);
        FieldValidator.checkFilename(filename);

        //path,id,name,offset,length,decompLength,exporter
        resources[i] = new Resource(path, filename, offset, length);

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