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
import org.watto.Settings;
import org.watto.datatype.Resource;
import org.watto.ge.helper.FieldValidator;
import org.watto.ge.plugin.ArchivePlugin;
import org.watto.io.FileManipulator;
import org.watto.task.TaskProgressManager;

/**
**********************************************************************************************

**********************************************************************************************
**/
public class Plugin_BNK_BKHD extends ArchivePlugin {

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public Plugin_BNK_BKHD() {

    super("BNK_BKHD", "BNK_BKHD");

    //         read write replace rename
    setProperties(true, false, true, false);

    setGames("ABZU",
        "APB Reloaded",
        "Assassins Creed: Syndicate",
        "Batman: Arkham City",
        "Bioshock Infinite",
        "Block Sport",
        "Borderlands: The Pre-Sequel",
        "Dauntless",
        "Divinity 2: Ego Draconis",
        "Dungeon Siege 3",
        "Enter The Gungeon",
        "F1 2015",
        "F1 2018",
        "For The King",
        "Inside",
        "Tom Clancy's Ghost Recon Wildlands",
        "Resident Evil 2",
        "Steep",
        "Sure Footing",
        "The Alto Collection",
        "Warhammer 40,000 Space Marine",
        "Warhammer: End Times: Vermintide",
        "Yooka Laylee");
    setExtensions("bnk"); // MUST BE LOWER CASE
    setPlatforms("PC");

    setCanScanForFileTypes(true);

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
      if (fm.readString(4).equals("BKHD")) {
        rating += 50;
      }

      // 4 - Header Size (not including these 2 header fields) (20/28)
      if (FieldValidator.checkRange(fm.readInt(), 0, 128)) {
        rating += 5;
      }

      fm.skip(20);

      // Header
      if (fm.readString(4).equals("DIDX")) {
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

      // 4 - Header (BKHD)
      fm.skip(4);

      // 4 - Header Size (not including these 2 header fields) (20)
      int headerSize = fm.readInt();
      FieldValidator.checkRange(headerSize, 0, 128);

      // 4 - Unknown (72)
      // 4 - Hash?
      // 12 - null
      // X - optional other fields
      fm.skip(headerSize);

      // 4 - Header (DIDX)
      fm.skip(4);

      // 4 - Directory Length (not including these 2 header fields)
      int dirLength = fm.readInt();
      FieldValidator.checkLength(dirLength, arcSize);

      int fileDataOffset = (int) (fm.getOffset() + dirLength + 8);
      FieldValidator.checkOffset(fileDataOffset, arcSize);

      int numFiles = dirLength / 12;
      FieldValidator.checkNumFiles(numFiles);

      Resource[] resources = new Resource[numFiles];
      TaskProgressManager.setMaximum(numFiles);

      // Loop through directory
      for (int i = 0; i < numFiles; i++) {

        // 4 - Hash?
        fm.skip(4);

        // 4 - File Offset (relative to the start of the FILE DATA) [+8]
        int offset = fm.readInt() + fileDataOffset;
        FieldValidator.checkOffset(offset, arcSize);

        // 4 - File Length
        int length = fm.readInt();
        FieldValidator.checkLength(length, arcSize);

        String filename = Resource.generateFilename(i);

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
   * Writes an [archive] File with the contents of the Resources. The archive is written using
   * data from the initial archive - it isn't written from scratch.
   **********************************************************************************************
   **/
  @Override
  public void replace(Resource[] resources, File path) {
    try {

      int paddingMultiple = 16;

      FileManipulator fm = new FileManipulator(path, true);
      FileManipulator src = new FileManipulator(new File(Settings.getString("CurrentArchive")), false);

      int numFiles = resources.length;
      TaskProgressManager.setMaximum(numFiles);

      // Calculations
      TaskProgressManager.setMessage(Language.get("Progress_PerformingCalculations"));

      long fileDataLength = 0;
      for (int i = 0; i < numFiles; i++) {
        fileDataLength += resources[i].getDecompressedLength();
        if (i != numFiles - 1) {
          fileDataLength += calculatePadding(fileDataLength, paddingMultiple); // padding to 16 bytes
        }
      }

      // Write Header Data

      // 4 - Header (BKHD)
      fm.writeBytes(src.readBytes(4));

      // 4 - Header Size (not including these 2 header fields) (20)
      int headerSize = src.readInt();
      fm.writeInt(headerSize);

      // 4 - Unknown (72)
      // 4 - Hash?
      // 12 - null
      // X - [optional] other fields
      fm.writeBytes(src.readBytes(headerSize));

      // 4 - Header (DIDX)
      // 4 - Directory Length (not including these 2 header fields)
      fm.writeBytes(src.readBytes(8));

      // Write Directory
      TaskProgressManager.setMessage(Language.get("Progress_WritingDirectory"));
      long offset = 0;
      for (int i = 0; i < numFiles; i++) {
        Resource fd = resources[i];
        long length = fd.getDecompressedLength();

        // 4 - Hash?
        fm.writeBytes(src.readBytes(4));

        // 4 - File Offset (relative to the start of the FILE DATA) [+8]
        fm.writeInt(offset);
        src.skip(4);

        // 4 - File Length
        fm.writeInt(length);
        src.skip(4);

        offset += length;
        if (i != numFiles - 1) {
          offset += calculatePadding(length, paddingMultiple); // padding to 16 bytes
        }

      }

      // 4 - Header (DATA)
      fm.writeBytes(src.readBytes(4));

      // 4 - File Data Length (not including these 2 header fields)
      fm.writeInt(fileDataLength);
      int srcFileDataLength = src.readInt();

      // Write Files
      TaskProgressManager.setMessage(Language.get("Progress_WritingFiles"));
      for (int i = 0; i < numFiles; i++) {
        Resource resource = resources[i];
        write(resource, fm);

        if (i != numFiles - 1) {
          int paddingSize = calculatePadding(resource.getDecompressedLength(), paddingMultiple); // padding to 16 bytes
          for (int p = 0; p < paddingSize; p++) {
            fm.writeByte(0);
          }
        }

        TaskProgressManager.setValue(i);
      }
      src.skip(srcFileDataLength);

      // Footer
      if (src.getOffset() < src.getLength()) {
        // 4 - Header (HIRC)
        fm.writeBytes(src.readBytes(4));

        // 4 - Footer Size (not including these 2 header fields)
        int footerLength = src.readInt();
        fm.writeInt(footerLength);

        // X - Unknown
        for (int f = 0; f < footerLength; f++) {
          fm.writeByte(src.readByte());
        }
      }

      src.close();
      fm.close();

    }
    catch (Throwable t) {
      logError(t);
    }
  }

}
