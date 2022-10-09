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
import org.watto.Language;
import org.watto.Settings;
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
public class Plugin_BIG_JPAK extends ArchivePlugin {

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public Plugin_BIG_JPAK() {

    super("BIG_JPAK", "BIG_JPAK");

    //         read write replace rename
    setProperties(true, false, true, true);

    setGames("Toca Race Driver 3");
    setExtensions("big"); // MUST BE LOWER CASE
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
      if (fm.readString(4).equals("JPAK")) {
        rating += 50;
      }

      fm.skip(4);

      // Number Of Files
      if (FieldValidator.checkNumFiles(fm.readInt())) {
        rating += 5;
      }

      if (fm.readInt() == 64) {
        rating += 5;
      }

      fm.skip(4);

      long arcSize = fm.getLength();

      // 4 - Filename Directory Offset
      if (FieldValidator.checkOffset(fm.readInt(), arcSize)) {
        rating += 5;
      }

      // 4 - Unknown (29758731)
      if (fm.readInt() == 29758731) {
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

      // 4 - Header (JPAK)
      // 4 - null
      fm.skip(8);

      // 4 - Number Of Files
      int numFiles = fm.readInt();
      FieldValidator.checkNumFiles(numFiles);

      // 4 - Padding Multiple (64)
      // 4 - null
      fm.skip(8);

      // 4 - Filename Directory Offset
      int filenameDirOffset = fm.readInt();
      FieldValidator.checkOffset(filenameDirOffset, arcSize);

      // 4 - Unknown (29758731)
      // 4 - Hash?
      fm.skip(8);

      // 4 - First Filename Offset
      // 4 - First File Length
      fm.skip(8);

      // 4 - First File Offset
      int filenameDirLength = fm.readInt() - filenameDirOffset;
      FieldValidator.checkLength(filenameDirLength, arcSize);

      fm.relativeSeek(filenameDirOffset);
      byte[] nameBytes = fm.readBytes(filenameDirLength);

      FileManipulator nameFM = new FileManipulator(new ByteBuffer(nameBytes));

      fm.seek(32);

      Resource[] resources = new Resource[numFiles];
      TaskProgressManager.setMaximum(numFiles);

      // Loop through directory
      for (int i = 0; i < numFiles; i++) {
        // 4 - Filename Offset
        int filenameOffset = fm.readInt() - filenameDirOffset;
        FieldValidator.checkOffset(filenameOffset, filenameDirLength);

        nameFM.seek(filenameOffset);

        // X - Filename (null)
        String filename = nameFM.readNullString();
        FieldValidator.checkFilename(filename);

        // 4 - File Length
        fm.skip(4);

        // 4 - File Offset
        long offset = fm.readInt();
        FieldValidator.checkOffset(offset, arcSize);

        // 4 - File Length
        long length = fm.readInt();
        FieldValidator.checkLength(length, arcSize);

        // 4 - Unknown (29758731)
        // 4 - Hash?
        // 8 - null
        fm.skip(16);

        //path,name,offset,length,decompLength,exporter
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

  /**
   **********************************************************************************************
   * Writes an [archive] File with the contents of the Resources. The archive is written using
   * data from the initial archive - it isn't written from scratch.
   **********************************************************************************************
   **/
  @Override
  public void replace(Resource[] resources, File path) {
    try {

      FileManipulator fm = new FileManipulator(path, true);
      FileManipulator src = new FileManipulator(new File(Settings.getString("CurrentArchive")), false);

      int numFiles = resources.length;
      TaskProgressManager.setMaximum(numFiles);

      // Calculations
      TaskProgressManager.setMessage(Language.get("Progress_PerformingCalculations"));

      long filenameDirOffset = 32 + (numFiles * 32);
      filenameDirOffset += calculatePadding(filenameDirOffset, 64);

      // Write Header Data

      // 4 - Header (JPAK)
      // 4 - null
      // 4 - Number Of Files
      // 4 - Padding Multiple (64)
      // 4 - null
      // 4 - Filename Directory Offset
      // 4 - Unknown (29758731)
      // 4 - Hash?
      fm.writeBytes(src.readBytes(32));

      long offset = filenameDirOffset;
      for (int i = 0; i < numFiles; i++) {
        offset += resources[i].getNameLength() + 1;
      }

      int authorNameLength = 0;

      TaskProgressManager.setMessage(Language.get("Progress_WritingDirectory"));
      for (int i = 0; i < numFiles; i++) {
        Resource resource = resources[i];
        long length = resource.getDecompressedLength();

        // 4 - Filename Offset
        int filenameOffset = src.readInt();

        if (i == 0) {
          authorNameLength = (int) (filenameOffset - filenameDirOffset);
          if (authorNameLength < 0) {
            authorNameLength = 0;
          }

          offset += authorNameLength;
          offset += calculatePadding(offset, 64);

          filenameDirOffset += authorNameLength;
        }

        fm.writeInt(filenameDirOffset);

        // 4 - File Length
        // 4 - File Offset
        // 4 - File Length
        fm.writeInt(length);
        fm.writeInt(offset);
        fm.writeInt(length);

        src.skip(12);

        // 4 - Unknown (29758731)
        // 4 - Hash?
        // 8 - null
        fm.writeBytes(src.readBytes(16));

        filenameDirOffset += resource.getNameLength() + 1;

        offset += length;
        offset += calculatePadding(offset, 64);
      }

      // X - Author Name
      // 1 - null Author Name Terminator
      fm.writeBytes(src.readBytes(authorNameLength));

      // Write Directory
      for (int i = 0; i < numFiles; i++) {
        Resource resource = resources[i];

        // X - Filename
        fm.writeString(resource.getName());

        // 1 - null Filename Terminator
        fm.writeByte(0);
      }

      // 0-63 - null Padding to a multiple of 64 bytes
      int dirPaddingLength = calculatePadding(fm.getOffset(), 64);
      for (int p = 0; p < dirPaddingLength; p++) {
        fm.writeByte(0);
      }

      // Write Files
      TaskProgressManager.setMessage(Language.get("Progress_WritingFiles"));
      for (int i = 0; i < numFiles; i++) {
        Resource resource = resources[i];

        // X - File Data
        write(resource, fm);

        // 0-63 - null Padding to a multiple of 64 bytes
        int paddingSize = calculatePadding(fm.getOffset(), 64);
        for (int p = 0; p < paddingSize; p++) {
          fm.writeByte(0);
        }

        TaskProgressManager.setValue(i);
      }

      //ExporterPlugin exporter = new Exporter_ZLib();
      //long[] compressedLengths = write(exporter,resources,fm);

      src.close();
      fm.close();

    }
    catch (Throwable t) {
      logError(t);
    }
  }

}
