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
import org.watto.datatype.FileType;
import org.watto.datatype.Resource;
import org.watto.ge.helper.FieldValidator;
import org.watto.ge.plugin.ArchivePlugin;
import org.watto.ge.plugin.ExporterPlugin;
import org.watto.ge.plugin.exporter.Exporter_XOR;
import org.watto.io.FileManipulator;
import org.watto.io.buffer.XORBufferWrapper;
import org.watto.io.converter.ByteConverter;
import org.watto.task.TaskProgressManager;

/**
**********************************************************************************************

**********************************************************************************************
**/
public class Plugin_CUB_CUB10 extends ArchivePlugin {

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public Plugin_CUB_CUB10() {

    super("CUB_CUB10", "CUB_CUB10");

    //         read write replace rename
    setProperties(true, false, false, false);

    setGames("My Brother Rabbit",
        "Vampire Legends: The True Story Of Kisilova");
    setExtensions("cub"); // MUST BE LOWER CASE
    setPlatforms("PC");

    // MUST BE LOWER CASE !!!
    setFileTypes(new FileType("stex", "STEX Image", FileType.TYPE_IMAGE));

    setTextPreviewExtensions("snd", "cubefont", "cubeimage", "skcredits"); // LOWER CASE

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
      byte[] headerBytes = fm.readBytes(8);
      if (ByteConverter.unsign(headerBytes[0]) == 245 && ByteConverter.unsign(headerBytes[1]) == 227 && ByteConverter.unsign(headerBytes[2]) == 244 && headerBytes[3] == 0) {
        rating += 25;
      }
      if (ByteConverter.unsign(headerBytes[4]) == 167 && ByteConverter.unsign(headerBytes[5]) == 184 && ByteConverter.unsign(headerBytes[6]) == 166 && headerBytes[7] == 0) {
        rating += 25;
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

      ExporterPlugin exporter = new Exporter_XOR(150);

      // RESETTING GLOBAL VARIABLES

      FileManipulator fm = new FileManipulator(path, false);

      // The entire file is XOR with (byte)150
      fm.setBuffer(new XORBufferWrapper(fm.getBuffer(), 150));

      long arcSize = fm.getLength();

      // 4 - Header ("cub" + null)
      // 4 - Version ("1.0" + null)
      fm.skip(8);

      // 4 - Number of Files
      int numFiles = fm.readInt();
      FieldValidator.checkNumFiles(numFiles);

      // 256 - Description ("Paczka danych CUBE" + nulls to fill)
      fm.skip(256);

      Resource[] resources = new Resource[numFiles];
      TaskProgressManager.setMaximum(numFiles);

      // Loop through directory
      for (int i = 0; i < numFiles; i++) {
        // 256 - Filename
        String filename = fm.readNullString(256);
        FieldValidator.checkFilename(filename);

        // 4 - File Offset
        int offset = fm.readInt();
        FieldValidator.checkOffset(offset, arcSize);

        // 4 - File Length
        int length = fm.readInt();
        FieldValidator.checkLength(length, arcSize);

        //path,name,offset,length,decompLength,exporter
        resources[i] = new Resource(path, filename, offset, length, length, exporter);

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
