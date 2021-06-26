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
import org.watto.ge.plugin.ExporterPlugin;
import org.watto.ge.plugin.exporter.Exporter_ZLib;
import org.watto.io.FileManipulator;
import org.watto.task.TaskProgressManager;

/**
**********************************************************************************************

**********************************************************************************************
**/
public class Plugin_RPACK_RP5L extends ArchivePlugin {

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public Plugin_RPACK_RP5L() {

    super("RPACK_RP5L", "RPACK_RP5L");

    //         read write replace rename
    setProperties(true, false, false, false);

    setGames("Call of Juarez: Bound in Blood",
        "Sniper: Ghost Warrior");
    setExtensions("rpack"); // MUST BE LOWER CASE
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

      // 4 - Header (RP5L)
      if (fm.readString(4).equals("RP5L")) {
        rating += 50;
      }

      long arcSize = fm.getLength();

      // 4 - Unknown (14)
      if (fm.readInt() == 14) {
        rating += 5;
      }

      // 4 - null
      if (fm.readInt() == 0) {
        rating += 5;
      }

      // 4 - Number of Files in the Files Directory
      if (FieldValidator.checkNumFiles(fm.readInt())) {
        rating += 5;
      }

      fm.skip(8);

      // 4 - Filename Directory Length
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

      ExporterPlugin exporter = Exporter_ZLib.getInstance();

      // RESETTING GLOBAL VARIABLES

      FileManipulator fm = new FileManipulator(path, false);

      long arcSize = fm.getLength();

      // 4 - Header (RP5L)
      // 4 - Unknown (14)
      // 4 - null
      fm.skip(12);

      // 4 - Number of Files in the Files Directory
      int numFiles = fm.readInt();
      FieldValidator.checkNumFiles(numFiles);

      // 4 - Number of Blocks
      int numBlocks = fm.readInt();
      FieldValidator.checkNumFiles(numBlocks);

      // 4 - Number of Filenames
      int numUnknown1 = fm.readInt();
      FieldValidator.checkNumFiles(numUnknown1);

      // 4 - Filename Directory Length
      int filenameDirLength = fm.readInt();
      FieldValidator.checkLength(filenameDirLength, arcSize);

      // 4 - Number of Filenames
      int numFilenames = fm.readInt();
      FieldValidator.checkNumFiles(numFilenames);

      // 4 - Padding Multiple (2048)
      int paddingMultiple = fm.readInt();
      FieldValidator.checkNumFiles(paddingMultiple);

      // Read the block offsets
      int[] blockOffsets = new int[numBlocks];
      for (int i = 0; i < numBlocks; i++) {
        // 2 - Unknown
        // 2 - Unknown
        fm.skip(4);

        // 4 - Offset to Block Data
        int blockOffset = fm.readInt();
        FieldValidator.checkOffset(blockOffset, arcSize);
        blockOffsets[i] = blockOffset;

        // 4 - Decompressed Block Length?
        // 4 - Compressed Block Length
        // 4 - Number of Entries in this Block
        fm.skip(12);
      }

      Resource[] resources = new Resource[numFiles];
      TaskProgressManager.setMaximum(numFiles);

      // skip to the filenames
      fm.seek(36 + numBlocks * 20 + numFiles * 16 + numUnknown1 * 12 + numFilenames * 8);

      // read the Filename Directory
      String[] filenames = new String[numFiles];
      long endOfDirectory = fm.getOffset() + filenameDirLength;
      for (int i = 0; i < numFilenames; i++) {
        // X - Filename
        // 1 - null Filename Terminator
        String filename = fm.readNullString();
        FieldValidator.checkFilename(filename);

        // 0-3 - null Padding to a multiple of 4 bytes
        fm.skip(calculatePadding(filename.length() + 1, 4));

        filenames[i] = filename;
        if (fm.getOffset() >= endOfDirectory) {
          break; // finished reading all the filenames
        }
      }

      // Go back to the Files Directory
      fm.seek(36 + numBlocks * 20);

      // Loop through the Files Directory
      for (int i = 0; i < numFiles; i++) {

        // 2 - Block ID
        short blockID = fm.readShort();
        FieldValidator.checkRange(blockID, 0, numBlocks);

        // 2 - File Name ID
        short filenameID = fm.readShort();
        FieldValidator.checkRange(filenameID, 0, numFilenames);

        // 4 - File Offset (relative to the start of the first file)
        int offset = fm.readInt() + blockOffsets[blockID];
        FieldValidator.checkOffset(offset, arcSize);

        // 4 - Decompressed File Length?
        int decompLength = fm.readInt();
        FieldValidator.checkLength(decompLength);

        // 4 - Compressed File Length
        int length = fm.readInt();
        FieldValidator.checkLength(length, arcSize);

        String filename = filenames[filenameID] + "." + blockID;

        //path,name,offset,length,decompLength,exporter
        if (length == 0) { // empty files should use the default exporter (because they're empty)
          resources[i] = new Resource(path, filename, offset, decompLength);
        }
        else { // only want to use the ZLib exporter for real files
          resources[i] = new Resource(path, filename, offset, length, decompLength, exporter);
        }

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
