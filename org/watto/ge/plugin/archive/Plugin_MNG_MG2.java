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
import org.watto.datatype.Resource;
import org.watto.ge.helper.FieldValidator;
import org.watto.ge.plugin.ArchivePlugin;
import org.watto.io.FileManipulator;
import org.watto.task.TaskProgressManager;

/**
**********************************************************************************************

**********************************************************************************************
**/
public class Plugin_MNG_MG2 extends ArchivePlugin {

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public Plugin_MNG_MG2() {

    super("MNG_MG2", "MNG_MG2");

    //         read write replace rename
    setProperties(true, true, true, true);

    setGames("LMA Manager 2007");
    setExtensions("mng"); // MUST BE LOWER CASE
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

      // 4 - Header ("MG2" + (byte)0)
      String header = fm.readString(3);
      int headerByte = fm.readByte();
      if (header.equals("MG2") && headerByte == 0) {
        rating += 50;
      }

      // 4 - Version? (20)
      if (FieldValidator.checkEquals(fm.readInt(), 20)) {
        rating += 5;
      }

      long arcSize = fm.getLength();

      // 4 - Files Directory Length
      if (FieldValidator.checkLength(fm.readInt(), arcSize)) {
        rating += 5;
      }

      // 4 - First File Offset
      if (FieldValidator.checkOffset(fm.readInt(), arcSize)) {
        rating += 5;
      }

      // 4 - File Data Length
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

      // 4 - Header ("MG2" + (byte)0)
      // 4 - Version? (20)
      fm.skip(8);

      // 4 - Files Directory Length
      long dirLength = fm.readInt();
      FieldValidator.checkOffset(dirLength, arcSize);

      int numFiles = (int) (dirLength / 264);
      FieldValidator.checkNumFiles(numFiles);

      // 4 - First File Offset
      long relativeOffset = fm.readInt();
      FieldValidator.checkOffset(relativeOffset, arcSize);

      // 4 - File Data Length
      fm.skip(4);

      Resource[] resources = new Resource[numFiles];
      TaskProgressManager.setMaximum(numFiles);

      // Loop through directory
      for (int i = 0; i < numFiles; i++) {

        // 256 - Filename (padded with null bytes)
        String filename = fm.readNullString(256);
        FieldValidator.checkFilename(filename);

        // 4 - File Offset (relative to the start of the first file)
        long offset = fm.readInt() + relativeOffset;
        FieldValidator.checkOffset(offset, arcSize);

        // 4 - File Length
        long length = fm.readInt();
        FieldValidator.checkLength(length, arcSize);

        //path,name,offset,length,decompLength,exporter
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

      // Calculations
      TaskProgressManager.setMessage(Language.get("Progress_PerformingCalculations"));

      long directorySize = numFiles * 264;
      long filesSize = 0;
      for (int i = 0; i < numFiles; i++) {
        long fileLength = resources[i].getDecompressedLength();
        filesSize += fileLength + calculatePadding(fileLength, 2048);
      }
      long firstFileOffset = 20 + directorySize;
      int dirPaddingSize = calculatePadding(firstFileOffset, 2048);
      firstFileOffset += dirPaddingSize;

      // Write Header Data

      // 4 - Header ("MG2" + (byte)0)
      fm.writeString("MG2");
      fm.writeByte(0);

      // 4 - Version? (20)
      fm.writeInt(20);

      // 4 - Files Directory Length
      fm.writeInt((int) directorySize);

      // 4 - First File Offset
      fm.writeInt((int) firstFileOffset);

      // 4 - File Data Length
      fm.writeInt((int) filesSize);

      // Write Directory
      TaskProgressManager.setMessage(Language.get("Progress_WritingDirectory"));
      long offset = 0;
      for (int i = 0; i < numFiles; i++) {
        Resource resource = resources[i];
        long decompLength = resource.getDecompressedLength();

        // 256 - Filename (padded with null bytes)
        fm.writeNullString(resource.getName(), 256);

        // 4 - File Offset (relative to the start of the first file)
        fm.writeInt((int) offset);

        // 4 - File Length
        fm.writeInt((int) decompLength);

        offset += decompLength + calculatePadding(decompLength, 2048);

      }

      // 0-2047 - null Padding to a multiple of 2048 bytes
      for (int p = 0; p < dirPaddingSize; p++) {
        fm.writeByte(0);
      }

      // Write Files
      TaskProgressManager.setMessage(Language.get("Progress_WritingFiles"));
      for (int i = 0; i < numFiles; i++) {
        // X - File Data
        write(resources[i], fm);

        // 0-2047 - null Padding to a multiple of 2048 bytes
        int paddingSize = calculatePadding(resources[i].getDecompressedLength(), 2048);
        for (int p = 0; p < paddingSize; p++) {
          fm.writeByte(0);
        }

        TaskProgressManager.setValue(i);
      }

      fm.close();

    }
    catch (Throwable t) {
      logError(t);
    }
  }

}
