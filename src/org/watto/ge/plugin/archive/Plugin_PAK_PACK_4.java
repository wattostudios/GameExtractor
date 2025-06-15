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
import org.watto.ge.plugin.ExporterPlugin;
import org.watto.ge.plugin.exporter.Exporter_Default;
import org.watto.ge.plugin.exporter.Exporter_QuickBMS_DLL;
import org.watto.io.FileManipulator;
import org.watto.task.TaskProgressManager;

/**
**********************************************************************************************

**********************************************************************************************
**/
public class Plugin_PAK_PACK_4 extends ArchivePlugin {

  /**
  **********************************************************************************************
  
  **********************************************************************************************
  **/
  public Plugin_PAK_PACK_4() {

    super("PAK_PACK_4", "PAK_PACK_4");

    //         read write replace rename
    setProperties(true, false, true, false);

    setGames("Alpine Skiing 2006",
        "Alpine Ski Racing 2007",
        "Skispringen 2007");
    setExtensions("pak");
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

      // Header
      if (fm.readString(4).equals("PACK")) {
        rating += 50;
      }

      // Version
      if (fm.readInt() == 2) {
        rating += 5;
      }

      // Number Of Files
      if (FieldValidator.checkNumFiles(fm.readInt())) {
        rating += 5;
      }

      long arcSize = fm.getLength();

      // Directory Length
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

      // RESETTING THE GLOBAL VARIABLES

      // If QuickBMS is available, use it to perform the decompression
      ExporterPlugin exporter = Exporter_Default.getInstance();
      /*
      if (QuickBMSHelper.checkAndShowPopup() != null) {
        exporter = new Exporter_QuickBMS_Decompression("un49g");
      }
      */
      exporter = new Exporter_QuickBMS_DLL("un49g");

      FileManipulator fm = new FileManipulator(path, false);

      long arcSize = (int) fm.getLength();

      // 4 - Header (PACK)
      // 4 - Version (2)
      fm.skip(8);

      // 4 - Number Of Files
      int numFiles = fm.readInt();
      FieldValidator.checkNumFiles(numFiles);

      // 4 - Directory Length
      fm.skip(4);

      Resource[] resources = new Resource[numFiles];

      TaskProgressManager.setMaximum(numFiles);

      // Loop through directory
      int[] nameOffsets = new int[numFiles];
      for (int i = 0; i < numFiles; i++) {
        // 4 - Compressed File Size
        long length = fm.readInt();
        FieldValidator.checkLength(length, arcSize);

        // 4 - Decompressed File Size
        int decompLength = fm.readInt();
        FieldValidator.checkLength(decompLength);

        // 4 - File Offset
        long offset = fm.readInt();
        FieldValidator.checkOffset(offset, arcSize);

        // 4 - Filename Offset
        int filenameOffset = fm.readInt();
        FieldValidator.checkOffset(filenameOffset, arcSize);
        nameOffsets[i] = filenameOffset;

        // 4 - Hash?
        // 4 - Unknown (107)
        fm.skip(8);

        //path,id,name,offset,length,decompLength,exporter
        if (length == decompLength) {
          resources[i] = new Resource(path, "", offset, length);
        }
        else {
          resources[i] = new Resource(path, "", offset, length, decompLength, exporter);
        }

        TaskProgressManager.setValue(i);
      }

      for (int i = 0; i < numFiles; i++) {
        fm.seek(nameOffsets[i]);

        // X - Filename
        // 1 - null Filename Terminator
        String filename = fm.readNullString();
        FieldValidator.checkFilename(filename);

        Resource resource = resources[i];

        resource.setName(filename);
        resource.setOriginalName(filename);
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

      FileManipulator fm = new FileManipulator(path, true);
      FileManipulator src = new FileManipulator(new File(Settings.getString("CurrentArchive")), false);

      int numFiles = resources.length;
      TaskProgressManager.setMaximum(numFiles);

      // Calculations
      TaskProgressManager.setMessage(Language.get("Progress_PerformingCalculations"));

      long headerSize = 16;
      long directorySize = numFiles * 24;

      long filenameDirSize = 0;
      for (int i = 0; i < numFiles; i++) {
        filenameDirSize += resources[i].getNameLength() + 1;
      }

      long arcSize = headerSize + directorySize + filenameDirSize;
      int paddingSize = calculatePadding(arcSize, 32);

      // Write Header Data

      // 4 - Header (PACK)
      // 4 - Version (2)
      // 4 - Number Of Files
      // 4 - Directory Length
      fm.writeBytes(src.readBytes(16));

      // Write Directory
      TaskProgressManager.setMessage(Language.get("Progress_WritingDirectory"));
      long offset = arcSize + paddingSize;
      long nameOffset = headerSize + directorySize;
      for (int i = 0; i < numFiles; i++) {
        Resource resource = resources[i];
        long decompLength = resource.getDecompressedLength();
        long length = resource.getLength();

        if (!resource.isReplaced()) {
          // unchanged

          // 4 - Compressed File Size
          // 4 - Decompressed File Size
          fm.writeBytes(src.readBytes(8));

          // 4 - File Offset
          // 4 - Filename Offset
          fm.writeInt(offset);
          fm.writeInt(nameOffset);
          src.skip(8);

          // 4 - Hash?
          // 4 - Compression Type (107)
          fm.writeBytes(src.readBytes(8));
        }
        else {
          // replaced

          // 4 - Compressed File Size
          fm.writeInt(length);

          // 4 - Decompressed File Size
          fm.writeInt(decompLength);

          // 4 - File Offset
          fm.writeInt(offset);

          // 4 - Filename Offset
          fm.writeInt(nameOffset);

          src.skip(16);

          // 4 - Hash?
          fm.writeBytes(src.readBytes(4));

          // 4 - Compression Type (107)
          fm.writeInt(0);
          src.skip(4);
        }

        offset += length;

        int padding = calculatePadding(length, 32);
        offset += padding;

        nameOffset += resource.getNameLength() + 1;
      }

      for (int i = 0; i < numFiles; i++) {
        // X - Filename (null)
        fm.writeNullString(resources[i].getName());
      }

      for (int i = 0; i < paddingSize; i++) {
        fm.writeString("U");
      }

      // Write Files
      TaskProgressManager.setMessage(Language.get("Progress_WritingFiles"));
      ExporterPlugin exporterDefault = Exporter_Default.getInstance();
      for (int i = 0; i < numFiles; i++) {
        Resource resource = resources[i];

        // set the exporter to the default, so it's copies with compression (or copied raw if replaced)
        ExporterPlugin exporter = resource.getExporter();
        resource.setExporter(exporterDefault);

        // X - File Data
        write(resource, fm);

        resource.setExporter(exporter);

        // X - Padding (using (byte) 170) to a multiple of 32? bytes
        int padding = calculatePadding(resource.getLength(), 32);
        for (int p = 0; p < padding; p++) {
          fm.writeByte(170);
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
