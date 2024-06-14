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
import org.watto.Language;
import org.watto.datatype.FileType;
import org.watto.datatype.Resource;
import org.watto.ge.helper.FieldValidator;
import org.watto.ge.plugin.ArchivePlugin;
import org.watto.io.FileManipulator;
import org.watto.io.converter.IntConverter;
import org.watto.task.TaskProgressManager;

/**
**********************************************************************************************

**********************************************************************************************
**/
public class Plugin_WAD_5 extends ArchivePlugin {

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public Plugin_WAD_5() {

    super("WAD_5", "WAD_5");

    //         read write replace rename
    setProperties(true, true, false, false);

    setGames("Fighting Force");
    setExtensions("wad");
    setPlatforms("PC");

    setFileTypes(new FileType("pdt", "Image Archive", FileType.TYPE_ARCHIVE),
        new FileType("pbl", "PBL Image", FileType.TYPE_IMAGE));

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

      // Number Of Files
      if (FieldValidator.checkNumFiles(IntConverter.changeFormat(fm.readInt()))) {
        rating += 5;
      }

      fm.skip(12);

      long arcSize = fm.getLength();

      // First File Length
      if (FieldValidator.checkLength(IntConverter.changeFormat(fm.readInt()), arcSize)) {
        rating += 5;
      }

      // First File Offset
      if (FieldValidator.checkOffset(IntConverter.changeFormat(fm.readInt()), arcSize)) {
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

      // RESETTING THE GLOBAL VARIABLES

      FileManipulator fm = new FileManipulator(path, false);

      long arcSize = (int) fm.getLength();

      // 4 - Number Of Files
      int numFiles = IntConverter.changeFormat(fm.readInt());
      FieldValidator.checkNumFiles(numFiles);

      Resource[] resources = new Resource[numFiles];

      TaskProgressManager.setMaximum(numFiles);

      // Loop through directory
      for (int i = 0; i < numFiles; i++) {
        // 12 - Filename (null)
        String filename = fm.readNullString(12);
        FieldValidator.checkFilename(filename);

        // 4 - File Length
        int length = IntConverter.changeFormat(fm.readInt());
        FieldValidator.checkLength(length, arcSize);

        // 4 - File Offset
        int offset = IntConverter.changeFormat(fm.readInt());
        FieldValidator.checkOffset(offset, arcSize);

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

  /**
   **********************************************************************************************
   * Writes an [archive] File with the contents of the Resources
   **********************************************************************************************
   **/
  @Override
  public void write(Resource[] resources, File path) {
    try {

      FileManipulator fm = new FileManipulator(path, true);
      int numFiles = resources.length;
      TaskProgressManager.setMaximum(numFiles);

      // Write Header Data

      // 4 - Number Of Files
      fm.writeInt(IntConverter.changeFormat(numFiles));

      // Write Directory
      TaskProgressManager.setMessage(Language.get("Progress_WritingDirectory"));
      long offset = 4 + (numFiles * 20);
      for (int i = 0; i < numFiles; i++) {
        Resource resource = resources[i];
        long decompLength = resource.getDecompressedLength();

        // 12 - Filename (null terminated, filled with nulls)
        String filename = resource.getName();
        if (filename.length() > 12) {
          filename = filename.substring(0, 12);
        }
        fm.writeString(filename);

        int paddingLength = 12 - filename.length();
        for (int p = 0; p < paddingLength; p++) {
          fm.writeByte(0);
        }

        // 4 - File Length
        fm.writeInt(IntConverter.changeFormat((int) decompLength));

        // 4 - File Offset
        fm.writeInt(IntConverter.changeFormat((int) offset));

        offset += decompLength;
      }

      // Write Files
      TaskProgressManager.setMessage(Language.get("Progress_WritingFiles"));
      write(resources, fm);

      fm.close();

    }
    catch (Throwable t) {
      logError(t);
    }
  }

}
