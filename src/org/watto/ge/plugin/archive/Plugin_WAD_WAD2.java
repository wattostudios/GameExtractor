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
import org.watto.Language;
import org.watto.task.TaskProgressManager;
import org.watto.datatype.Resource;
import org.watto.ge.helper.FieldValidator;
import org.watto.ge.plugin.ArchivePlugin;
import org.watto.io.FileManipulator;
import org.watto.io.converter.ByteConverter;

/**
**********************************************************************************************

**********************************************************************************************
**/
public class Plugin_WAD_WAD2 extends ArchivePlugin {

  /**
  **********************************************************************************************

  **********************************************************************************************
  **/
  public Plugin_WAD_WAD2() {

    super("WAD_WAD2", "WAD_WAD2");

    //         read write replace rename
    setProperties(true, true, true, true);

    setGames("Eternal War: Shadows Of Light");
    setExtensions("wad"); // MUST BE LOWER CASE
    setPlatforms("PC");

    setFileTypes("lmp", "Paletted Image");

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
      if (fm.readString(4).equals("WAD2")) {
        rating += 50;
      }
      else {
        // Being that this is an image archive, if the header doesn't match, want to basically exclude this plugin from
        // being used, otherwise the generated thumbnail images will take a lot of time and be absolute rubbish!
        rating = 0;
      }

      // Number Of Files
      if (FieldValidator.checkNumFiles(fm.readInt())) {
        rating += 5;
      }

      long arcSize = fm.getLength();

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

      // RESETTING GLOBAL VARIABLES

      FileManipulator fm = new FileManipulator(path, false);

      long arcSize = fm.getLength();

      // 4 - Header (WAD2)
      fm.skip(4);

      // 4 - Number Of Files
      int numFiles = fm.readInt();
      FieldValidator.checkNumFiles(numFiles);

      // 4 - Directory Offset
      long dirOffset = fm.readInt();
      FieldValidator.checkOffset(dirOffset, arcSize);

      fm.seek(dirOffset);

      Resource[] resources = new Resource[numFiles];
      TaskProgressManager.setMaximum(numFiles);

      // Loop through directory
      for (int i = 0; i < numFiles; i++) {
        // 4 - File Offset
        long offset = fm.readInt();
        FieldValidator.checkOffset(offset, arcSize);

        // 4 - Uncompressed File Size
        long decompLength = fm.readInt();
        FieldValidator.checkLength(decompLength);

        // 4 - Compressed File Size
        long length = fm.readInt();
        FieldValidator.checkLength(length, arcSize);
        length = decompLength;

        // 1 - File Type (66=Paletted Image (*.lmp file), 68=Unknown)
        int fileType = ByteConverter.unsign(fm.readByte());

        // 1 - Compression Type
        // 2 - Padding
        fm.skip(3);

        // 16 - Filename (null)
        String filename = fm.readNullString(16);
        FieldValidator.checkFilename(filename);

        if (fileType == 66) {
          filename += ".lmp";
        }
        else {
          filename += "." + fileType;

        }

        // path,name,offset,length,decompLength,exporter
        resources[i] = new Resource(path, filename, offset, length, decompLength);

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

      // Calculations
      TaskProgressManager.setMessage(Language.get("Progress_PerformingCalculations"));

      long dirOffset = 12;
      for (int i = 0; i < numFiles; i++) {
        dirOffset += resources[i].getDecompressedLength();
      }

      // Write Header Data

      // 4 - Header (WAD2)
      fm.writeString("WAD2");

      // 4 - Number Of Files
      fm.writeInt(numFiles);

      // 4 - Directory Offset
      fm.writeInt((int) dirOffset);

      // Write Files
      TaskProgressManager.setMessage(Language.get("Progress_WritingFiles"));
      write(resources, fm);

      // Write Directory
      TaskProgressManager.setMessage(Language.get("Progress_WritingDirectory"));
      long offset = 12;
      for (int i = 0; i < numFiles; i++) {
        Resource resource = resources[i];
        long decompLength = resource.getDecompressedLength();

        // 4 - File Offset
        fm.writeInt((int) offset);

        // 4 - Uncompressed File Size
        fm.writeInt((int) decompLength);

        // 4 - Compressed File Size
        fm.writeInt((int) decompLength);

        String extension = resource.getExtension();
        int fileType = 66;
        if (extension.equals("lmp")) {
          fileType = 66;
        }
        else {
          try {
            fileType = Integer.parseInt(extension);
          }
          catch (Throwable t) {
            fileType = 66;
          }
        }

        // 1 - File Type (66=Paletted Image (*.lmp file), 68=Unknown)
        fm.writeByte(fileType);

        // 1 - Compression Type
        fm.writeByte(0);

        // 2 - Padding
        fm.writeShort((short) 0);

        // 16 - Filename (null)
        fm.writeNullString(resource.getFilename(), 16); // ONLY WANT THE NAME, NO EXTENSION ETC!

        offset += decompLength;
      }

      fm.close();

    }
    catch (Throwable t) {
      logError(t);
    }
  }

}
