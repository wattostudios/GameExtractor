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
public class Plugin_LP_KAPA extends ArchivePlugin {

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public Plugin_LP_KAPA() {

    super("LP_KAPA", "LP_KAPA");

    //         read write replace rename
    setProperties(true, false, true, false);

    setGames("Star Wars: The Force Unleashed");
    setExtensions("lp"); // MUST BE LOWER CASE
    setPlatforms("PC");

    // MUST BE LOWER CASE !!!
    //setFileTypes(new FileType("txt", "Text Document", FileType.TYPE_DOCUMENT),
    //             new FileType("bmp", "Bitmap Image", FileType.TYPE_IMAGE)
    //             );

    setTextPreviewExtensions("utf16"); // LOWER CASE

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
      if (fm.readString(4).equals("kaPA")) {
        rating += 50;
      }

      fm.skip(4);

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

      // 4 - Header (kaPA)
      // 4 - Unknown (5)
      fm.skip(8);

      // 4 - Number of Files
      int numFiles = fm.readInt();
      FieldValidator.checkNumFiles(numFiles);

      // 8 - null
      // 4 - Unknown (-1)
      fm.skip(12);

      // 4 - Extra Header Length (can be null)
      int extraHeaderLength = fm.readInt();
      FieldValidator.checkLength(extraHeaderLength, arcSize);

      // 4 - Filename Directory Length (including the header/footer and padding)
      int filenameDirLength = fm.readInt();
      FieldValidator.checkLength(filenameDirLength, arcSize);

      // 4 - Details Directory 2 Offset
      int dir2Offset = fm.readInt();
      FieldValidator.checkOffset(dir2Offset, arcSize);

      // 4 - File Data Offset
      int dataOffset = fm.readInt();
      FieldValidator.checkOffset(dataOffset, arcSize);

      // 4 - File Data Length
      // 4 - Number of Files
      // 4 - File Data Offset
      // 4 - Archive Length
      // 12 - null
      // 4 - File Data Offset
      // 4 - Archive Length
      // 4 - null
      fm.skip(40);

      // X - Extra Header
      fm.skip(extraHeaderLength);

      long dir1Offset = fm.getOffset() + filenameDirLength;
      FieldValidator.checkOffset(dir1Offset, arcSize);

      byte[] nameBytes = fm.readBytes(filenameDirLength);
      FileManipulator nameFM = new FileManipulator(new ByteBuffer(nameBytes));

      fm.relativeSeek(dir2Offset);

      // Loop through directory
      int[] offsets = new int[numFiles];
      for (int i = 0; i < numFiles; i++) {
        // 4 - File ID (incremental from 0)
        fm.skip(4);

        // 4 - File Offset (relative to the start of the File Data)
        int offset = fm.readInt() + dataOffset;
        FieldValidator.checkOffset(offset, arcSize);
        offsets[i] = offset;

        // 8 - null
        fm.skip(8);
      }

      fm.seek(dir1Offset);

      Resource[] resources = new Resource[numFiles];
      TaskProgressManager.setMaximum(numFiles);

      // Loop through directory
      for (int i = 0; i < numFiles; i++) {

        // 8 - Unknown
        // 4 - null
        // 4 - Junk
        // 4 - Unknown
        fm.skip(20);

        // 4 - Filename Offset (relative to the start of the Filename Directory)
        int filenameOffset = fm.readInt();
        FieldValidator.checkOffset(filenameOffset, filenameDirLength);

        nameFM.seek(filenameOffset);
        String filename = nameFM.readNullString();
        FieldValidator.checkFilename(filename);

        // 4 - Unknown
        // 12 - null
        fm.skip(16);

        // 4 - File Length
        int length = fm.readInt();
        FieldValidator.checkLength(length, arcSize);

        // 4 - File Length
        // 4 - null
        // 4 - Unknown
        // 8 - null
        fm.skip(20);

        long offset = offsets[i];

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

      // Write Header Data

      // 4 - Header (kaPA)
      // 4 - Unknown (5)
      // 4 - Number of Files
      // 8 - null
      // 4 - Unknown (-1)
      fm.writeBytes(src.readBytes(24));

      // 4 - Extra Header Length (can be null)
      int srcExtraHeaderLength = src.readInt();
      fm.writeInt(srcExtraHeaderLength);

      // 4 - Filename Directory Length (including the header/footer and padding)
      int srcFilenameDirLength = src.readInt();
      fm.writeInt(srcFilenameDirLength);

      // 4 - Details Directory 2 Offset
      // 4 - File Data Offset
      fm.writeBytes(src.readBytes(8));

      int fileDataLength = 0;
      for (int i = 0; i < numFiles; i++) {
        int length = (int) resources[i].getDecompressedLength();
        length += calculatePadding(length, 16);
        fileDataLength += length;
      }

      // 4 - File Data Length
      src.skip(4);
      fm.writeInt(fileDataLength);

      // 4 - Number of Files
      fm.writeBytes(src.readBytes(4));

      // 4 - File Data Offset
      int srcFileDataOffset = src.readInt();
      fm.writeInt(srcFileDataOffset);

      int archiveLength = srcFileDataOffset + fileDataLength;

      // 4 - Archive Length
      src.skip(4);
      fm.writeInt(archiveLength);

      // 12 - null
      // 4 - File Data Offset
      fm.writeBytes(src.readBytes(16));

      // 4 - Archive Length
      src.skip(4);
      fm.writeInt(archiveLength);

      // 4 - null
      fm.writeBytes(src.readBytes(4));

      // EXTRA HEADER (optional)
      fm.writeBytes(src.readBytes(srcExtraHeaderLength));

      // FILENAME DIRECTORY
      fm.writeBytes(src.readBytes(srcFilenameDirLength));

      TaskProgressManager.setMessage(Language.get("Progress_WritingDirectory"));

      // DETAILS DIRECTORY 1
      for (int i = 0; i < numFiles; i++) {
        Resource resource = resources[i];
        long length = resource.getDecompressedLength();

        // 8 - Unknown
        // 4 - null
        // 4 - Junk
        // 4 - Unknown
        // 4 - Filename Offset (relative to the start of the Filename Directory)
        // 4 - Unknown
        // 12 - null
        fm.writeBytes(src.readBytes(40));

        // 4 - File Length
        // 4 - File Length
        src.skip(8);
        fm.writeInt(length);
        fm.writeInt(length);

        // 4 - null
        // 4 - Unknown
        // 8 - null
        fm.writeBytes(src.readBytes(16));
      }

      // DETAILS DIRECTORY 2
      long offset = 0;
      for (int i = 0; i < numFiles; i++) {
        Resource resource = resources[i];
        long length = resource.getDecompressedLength();

        // 4 - File ID (incremental from 0)
        fm.writeBytes(src.readBytes(4));

        // 4 - File Offset (relative to the start of the File Data)
        src.skip(4);
        fm.writeInt(offset);

        // 8 - null
        fm.writeBytes(src.readBytes(8));

        offset += length;
        offset += calculatePadding(length, 16); // file data offset to multiples of 16 bytes
      }

      // FILE DATA
      TaskProgressManager.setMessage(Language.get("Progress_WritingFiles"));
      for (int i = 0; i < numFiles; i++) {
        Resource resource = resources[i];

        // X - File Data
        write(resource, fm);

        // X - null Padding to a multiple of 16? bytes
        int padding = calculatePadding(resource.getDecompressedLength(), 16);
        for (int p = 0; p < padding; p++) {
          fm.writeByte(0);
        }

        TaskProgressManager.setValue(i);
      }

      src.close();
      fm.close();

    }
    catch (Throwable t) {
      logError(t);
    }
  }

}
